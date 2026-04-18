"""
SQLite-backed experience replay buffer for stable RL learning.

Stores every (state, action, reward) tuple so the agent can replay
random mini-batches, breaking temporal correlation and reinforcing
rare-but-important experiences.

SQLite is perfect here: zero-config, handles millions of rows, and
lives right next to the Q-table file.
"""
import logging
import sqlite3
import threading
from dataclasses import dataclass
from typing import Optional

from config import Config

logger = logging.getLogger("rl_optimizer.experience_store")


@dataclass
class Experience:
    """A single recorded experience."""
    state_key: str
    size_bucket: int
    selectivity: int
    has_index: int
    predicate_type: int
    cardinality_bucket: int
    action: int
    reward: float
    raw_time_ms: float
    num_rows: int


class ExperienceStore:
    """Thread-safe SQLite experience replay buffer."""

    def __init__(
        self,
        db_path: str = Config.EXPERIENCE_DB,
        max_size: int = Config.EXPERIENCE_MAX_SIZE,
    ):
        self._db_path = db_path
        self._max_size = max_size
        self._lock = threading.Lock()
        self._conn: Optional[sqlite3.Connection] = None

    def open(self) -> None:
        """Open the database and create the schema if needed."""
        self._conn = sqlite3.connect(self._db_path, check_same_thread=False)
        self._conn.execute("PRAGMA journal_mode=WAL")  # Better concurrent read/write
        self._conn.execute("PRAGMA synchronous=NORMAL")  # Good balance of safety/speed
        self._conn.execute("""
            CREATE TABLE IF NOT EXISTS experiences (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                state_key       TEXT    NOT NULL,
                size_bucket     INTEGER NOT NULL,
                selectivity     INTEGER NOT NULL,
                has_index       INTEGER NOT NULL,
                predicate_type  INTEGER NOT NULL DEFAULT 0,
                cardinality_bucket INTEGER NOT NULL DEFAULT 0,
                action          INTEGER NOT NULL,
                reward          REAL    NOT NULL,
                raw_time_ms     REAL    NOT NULL,
                num_rows        INTEGER NOT NULL,
                created_at      TEXT    DEFAULT (datetime('now'))
            )
        """)
        self._conn.execute(
            "CREATE INDEX IF NOT EXISTS idx_exp_state ON experiences(state_key)"
        )
        self._conn.commit()
        logger.info("Experience store opened at %s", self._db_path)

    def add(self, exp: Experience) -> None:
        """Add a new experience to the buffer."""
        with self._lock:
            self._conn.execute(
                """INSERT INTO experiences 
                   (state_key, size_bucket, selectivity, has_index, predicate_type,
                    cardinality_bucket, action, reward, raw_time_ms, num_rows)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                (
                    exp.state_key, exp.size_bucket, exp.selectivity,
                    exp.has_index, exp.predicate_type, exp.cardinality_bucket,
                    exp.action, exp.reward, exp.raw_time_ms, exp.num_rows,
                ),
            )
            self._conn.commit()

    def sample(self, batch_size: int = Config.REPLAY_BATCH_SIZE) -> list[Experience]:
        """Sample a random mini-batch for experience replay."""
        with self._lock:
            cursor = self._conn.execute(
                "SELECT state_key, size_bucket, selectivity, has_index, "
                "predicate_type, cardinality_bucket, action, reward, raw_time_ms, num_rows "
                "FROM experiences ORDER BY RANDOM() LIMIT ?",
                (batch_size,),
            )
            return [
                Experience(
                    state_key=row[0], size_bucket=row[1], selectivity=row[2],
                    has_index=row[3], predicate_type=row[4],
                    cardinality_bucket=row[5], action=row[6], reward=row[7],
                    raw_time_ms=row[8], num_rows=row[9],
                )
                for row in cursor.fetchall()
            ]

    def size(self) -> int:
        """Number of experiences in the buffer."""
        with self._lock:
            cursor = self._conn.execute("SELECT COUNT(*) FROM experiences")
            return cursor.fetchone()[0]

    def prune(self) -> None:
        """Remove oldest experiences if buffer exceeds max size."""
        current_size = self.size()
        if current_size <= self._max_size:
            return
        excess = current_size - self._max_size
        with self._lock:
            self._conn.execute(
                "DELETE FROM experiences WHERE id IN "
                "(SELECT id FROM experiences ORDER BY id ASC LIMIT ?)",
                (excess,),
            )
            self._conn.commit()
        logger.info("Pruned %d old experiences (kept %d)", excess, self._max_size)

    def avg_reward_last_n(self, n: int) -> Optional[float]:
        """Average reward of the last N experiences."""
        with self._lock:
            cursor = self._conn.execute(
                "SELECT AVG(reward) FROM (SELECT reward FROM experiences ORDER BY id DESC LIMIT ?)",
                (n,),
            )
            result = cursor.fetchone()[0]
            return float(result) if result is not None else None

    def action_distribution(self) -> dict[str, int]:
        """Count of experiences by action."""
        with self._lock:
            cursor = self._conn.execute(
                "SELECT action, COUNT(*) FROM experiences GROUP BY action"
            )
            action_names = {0: "SeqScan", 1: "IndexScan"}
            return {action_names.get(row[0], str(row[0])): row[1] for row in cursor.fetchall()}

    def learning_curve(self, window: int = 100) -> list[dict]:
        """Average reward per window of episodes for plotting."""
        with self._lock:
            cursor = self._conn.execute(
                "SELECT id, reward FROM experiences ORDER BY id ASC"
            )
            rows = cursor.fetchall()

        if not rows:
            return []

        curve = []
        for i in range(0, len(rows), window):
            chunk = rows[i : i + window]
            avg_reward = sum(r[1] for r in chunk) / len(chunk)
            curve.append({"episode": i, "avg_reward": round(avg_reward, 4)})
        return curve

    def close(self) -> None:
        """Close the database connection."""
        if self._conn:
            self._conn.close()
            logger.info("Experience store closed")
