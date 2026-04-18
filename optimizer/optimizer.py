
"""
MiniPostgres RL Query Optimizer — FastAPI Service

Industry-grade RL-based query optimizer that learns which scan strategy
(SeqScan vs IndexScan) performs best for different query patterns.

Endpoints:
  POST /predict  — Get the recommended action for a query state
  POST /update   — Report execution feedback for learning
  GET  /health   — Readiness probe
  GET  /metrics  — Learning statistics and performance dashboard
"""

import logging
import os
import signal
import sys
import uuid
from contextlib import asynccontextmanager
from typing import Optional

from fastapi import FastAPI, Header, HTTPException, Request, Response
from pydantic import BaseModel, Field, field_validator

from agent import RLAgent
from config import Config
from experience_store import ExperienceStore
from idempotency import IdempotencyCache
from q_store import QStore
from grpc_server import start_grpc_server

# ── Logging Setup ───────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger("rl_optimizer")

# ── Global Components ───────────────────────────────────────────────────
q_store = QStore()
exp_store = ExperienceStore()
agent: Optional[RLAgent] = None
idempotency_cache = IdempotencyCache()
_ready = False
_grpc_server = None


# ── Lifespan ────────────────────────────────────────────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    """Modern lifespan handler — replaces deprecated @app.on_event."""
    global agent, _ready, _grpc_server

    logger.info("Starting RL Optimizer service...")

    # Initialize stores
    q_store.load()
    exp_store.open()

    # Initialize agent
    agent = RLAgent(q_store, exp_store)

    # Start periodic Q-table saves
    q_store.start_periodic_save()

    # Start gRPC server alongside FastAPI
    grpc_port = int(os.getenv("RL_GRPC_PORT", "50051"))
    _grpc_server = start_grpc_server(agent, q_store, exp_store, idempotency_cache, port=grpc_port)
    logger.info("gRPC server started on port %d", grpc_port)

    # Register signal handlers for graceful shutdown
    def _shutdown_handler(signum, frame):
        logger.info("Received signal %d, shutting down...", signum)
        if _grpc_server:
            _grpc_server.stop(grace=5)
        q_store.shutdown()
        exp_store.close()
        sys.exit(0)

    signal.signal(signal.SIGINT, _shutdown_handler)
    signal.signal(signal.SIGTERM, _shutdown_handler)

    _ready = True
    logger.info("RL Optimizer ready — Q-table: %d states, Experiences: %d",
                q_store.state_count(), exp_store.size())
    logger.info("Endpoints: HTTP=%d, gRPC=%d", Config.PORT, grpc_port)

    yield  # App is running

    # Shutdown
    logger.info("Shutting down RL Optimizer...")
    _ready = False
    if _grpc_server:
        _grpc_server.stop(grace=5)
        logger.info("gRPC server stopped.")
    q_store.shutdown()
    exp_store.close()
    logger.info("Shutdown complete.")


# ── App ─────────────────────────────────────────────────────────────────
app = FastAPI(
    title="MiniPostgres RL Query Optimizer",
    version="2.0.0",
    description="Reinforcement Learning optimizer for query execution strategy selection",
    lifespan=lifespan,
)


# ── Request / Response Models ───────────────────────────────────────────
class PredictRequest(BaseModel):
    """Query state for action prediction."""
    numRows: int = Field(ge=0, description="Number of rows in the table")
    isRange: bool = Field(description="Whether the query is a range predicate")
    hasIndex: bool = Field(description="Whether an index exists on the predicate column")
    predicateType: int = Field(default=0, ge=0, le=2,
                               description="0=equality, 1=narrow_range, 2=wide_range")
    estimatedMatches: int = Field(default=0, ge=0,
                                  description="Estimated number of matching rows")


class PredictResponse(BaseModel):
    action: int = Field(description="0=SeqScan, 1=IndexScan")
    state_key: str = Field(description="Encoded state key for debugging")


class UpdateState(BaseModel):
    """Embedded state in update requests."""
    numRows: int = Field(ge=0)
    isRange: bool
    hasIndex: bool
    predicateType: int = Field(default=0, ge=0, le=2)
    estimatedMatches: int = Field(default=0, ge=0)


class UpdateRequest(BaseModel):
    """Reward feedback for learning."""
    state: UpdateState
    action: int = Field(ge=0, le=1, description="0=SeqScan, 1=IndexScan")
    reward: float = Field(description="Negative execution time in ms")

    @field_validator("action")
    @classmethod
    def validate_action(cls, v):
        if v not in (0, 1):
            raise ValueError("Action must be 0 (SeqScan) or 1 (IndexScan)")
        return v


class UpdateResponse(BaseModel):
    status: str
    old_value: Optional[float] = None
    new_value: Optional[float] = None
    normalized_reward: Optional[float] = None
    epsilon: Optional[float] = None
    idempotent: bool = False


class HealthResponse(BaseModel):
    status: str
    ready: bool
    q_table_states: int
    experience_count: int
    version: str = "2.0.0"


# ── Middleware ──────────────────────────────────────────────────────────
@app.middleware("http")
async def add_request_id(request: Request, call_next):
    """Inject a correlation ID for request tracing."""
    request_id = request.headers.get("X-Request-ID", str(uuid.uuid4()))
    response: Response = await call_next(request)
    response.headers["X-Request-ID"] = request_id
    return response


# ── Endpoints ───────────────────────────────────────────────────────────
@app.post("/predict", response_model=PredictResponse)
async def predict(req: PredictRequest):
    """Get the recommended scan action for a query state."""
    if not _ready or agent is None:
        raise HTTPException(status_code=503, detail="Service not ready")

    state_key, *_ = RLAgent.encode_state(
        num_rows=req.numRows,
        is_range=req.isRange,
        has_index=req.hasIndex,
        predicate_type=req.predicateType,
        estimated_matches=req.estimatedMatches,
    )

    action = agent.predict(state_key, has_index=req.hasIndex, is_range=req.isRange)

    return PredictResponse(action=action, state_key=state_key)


@app.post("/update", response_model=UpdateResponse)
async def update(
    req: UpdateRequest,
    x_request_id: Optional[str] = Header(None),
):
    """Report execution feedback for RL learning."""
    if not _ready or agent is None:
        raise HTTPException(status_code=503, detail="Service not ready")

    # Idempotency check
    if x_request_id:
        cached = idempotency_cache.get(x_request_id)
        if cached is not None:
            logger.info("Idempotent replay for request %s", x_request_id)
            return UpdateResponse(status="ok", idempotent=True, **cached)

    # Encode state
    state_key, size_bucket, selectivity, has_index, pred_type, card = RLAgent.encode_state(
        num_rows=req.state.numRows,
        is_range=req.state.isRange,
        has_index=req.state.hasIndex,
        predicate_type=req.state.predicateType,
        estimated_matches=req.state.estimatedMatches,
    )

    # Raw time from negative reward
    raw_time_ms = abs(req.reward)

    # Update the agent
    result = agent.update(
        state_key=state_key,
        size_bucket=size_bucket,
        selectivity=selectivity,
        has_index=has_index,
        predicate_type=pred_type,
        cardinality_bucket=card,
        action=req.action,
        raw_time_ms=raw_time_ms,
        num_rows=req.state.numRows,
    )

    # Cache for idempotency
    if x_request_id:
        idempotency_cache.put(x_request_id, result)

    return UpdateResponse(status="ok", **result)


@app.get("/health", response_model=HealthResponse)
async def health():
    """Readiness probe for the RL optimizer."""
    return HealthResponse(
        status="healthy" if _ready else "starting",
        ready=_ready,
        q_table_states=q_store.state_count(),
        experience_count=exp_store.size() if _ready else 0,
    )


@app.get("/metrics")
async def metrics():
    """Learning statistics and performance dashboard."""
    if not _ready or agent is None:
        raise HTTPException(status_code=503, detail="Service not ready")

    agent_metrics = agent.metrics()
    agent_metrics["q_table_snapshot"] = q_store.snapshot()
    agent_metrics["idempotency_cache_size"] = idempotency_cache.size()
    return agent_metrics


# ── Main ────────────────────────────────────────────────────────────────
if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "optimizer:app",
        host=Config.HOST,
        port=Config.PORT,
        log_level="info",
        timeout_keep_alive=30,
    )
