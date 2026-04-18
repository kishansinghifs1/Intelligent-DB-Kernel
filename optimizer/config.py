"""
Centralized configuration for the RL Query Optimizer.

All values are configurable via environment variables with sensible defaults.
"""
import os


class Config:
    """Immutable configuration loaded once at startup from environment variables."""

    # ── Server ──────────────────────────────────────────────────────────
    HOST: str = os.getenv("RL_HOST", "0.0.0.0")
    PORT: int = int(os.getenv("RL_PORT", "8000"))

    # ── Q-Learning Hyperparameters ──────────────────────────────────────
    LEARNING_RATE: float = float(os.getenv("RL_LEARNING_RATE", "0.1"))
    DISCOUNT_FACTOR: float = float(os.getenv("RL_DISCOUNT_FACTOR", "0.9"))

    # ── Exploration ─────────────────────────────────────────────────────
    EPSILON_START: float = float(os.getenv("RL_EPSILON_START", "0.3"))
    EPSILON_MIN: float = float(os.getenv("RL_EPSILON_MIN", "0.01"))
    EPSILON_DECAY: float = float(os.getenv("RL_EPSILON_DECAY", "0.994"))
    UCB_COEFFICIENT: float = float(os.getenv("RL_UCB_COEFFICIENT", "1.414"))

    # ── Experience Replay ───────────────────────────────────────────────
    REPLAY_BATCH_SIZE: int = int(os.getenv("RL_REPLAY_BATCH_SIZE", "32"))
    REPLAY_INTERVAL: int = int(os.getenv("RL_REPLAY_INTERVAL", "10"))
    EXPERIENCE_MAX_SIZE: int = int(os.getenv("RL_EXPERIENCE_MAX_SIZE", "100000"))

    # ── Persistence ─────────────────────────────────────────────────────
    Q_TABLE_FILE: str = os.getenv(
        "Q_TABLE_FILE",
        os.path.join(os.path.dirname(os.path.abspath(__file__)), "q_table.json"),
    )
    EXPERIENCE_DB: str = os.getenv(
        "EXPERIENCE_DB",
        os.path.join(os.path.dirname(os.path.abspath(__file__)), "experiences.db"),
    )
    SAVE_INTERVAL_SEC: int = int(os.getenv("RL_SAVE_INTERVAL_SEC", "30"))
    BACKUP_COUNT: int = int(os.getenv("RL_BACKUP_COUNT", "3"))

    # ── Idempotency ─────────────────────────────────────────────────────
    IDEMPOTENCY_CACHE_SIZE: int = int(os.getenv("RL_IDEMPOTENCY_CACHE_SIZE", "10000"))
    IDEMPOTENCY_TTL_SEC: int = int(os.getenv("RL_IDEMPOTENCY_TTL_SEC", "300"))

    # ── Request Timeout ─────────────────────────────────────────────────
    REQUEST_TIMEOUT_SEC: float = float(os.getenv("RL_REQUEST_TIMEOUT_SEC", "5.0"))
