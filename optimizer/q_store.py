"""
Thread-safe Q-table store with atomic persistence and rolling backups.

Design decisions:
- threading.Lock protects all Q-table mutations (FastAPI uses thread pools for sync deps)
- Atomic writes via temp-file + os.replace() prevent corruption on crash
- Rolling backups keep last N versions so a bad save never destroys history
- Batched saves via a daemon timer reduce disk I/O (instead of saving on every /update)
"""
import json
import logging
import os
import shutil
import threading
from typing import Optional

from config import Config

logger = logging.getLogger("rl_optimizer.q_store")


class QStore:
    """Thread-safe Q-table with atomic persistence and rolling backups."""

    def __init__(self, filepath: str = Config.Q_TABLE_FILE, backup_count: int = Config.BACKUP_COUNT):
        self._filepath = filepath
        self._backup_count = backup_count
        self._lock = threading.Lock()
        self._q_table: dict[str, list[float]] = {}
        self._visit_counts: dict[str, list[int]] = {}  # per-state-action visit counts for UCB
        self._dirty = False  # tracks whether q_table has unsaved changes
        self._save_timer: Optional[threading.Timer] = None
        self._shutdown = False

    # ── Public API ──────────────────────────────────────────────────────

    def load(self) -> None:
        """Load Q-table from disk. Safe to call at startup."""
        with self._lock:
            if os.path.exists(self._filepath):
                try:
                    with open(self._filepath, "r") as f:
                        data = json.load(f)
                    self._q_table = data.get("q_values", data) if isinstance(data, dict) else {}
                    self._visit_counts = data.get("visit_counts", {}) if isinstance(data, dict) and "q_values" in data else {}
                    logger.info("Loaded Q-table with %d states from %s", len(self._q_table), self._filepath)
                except (json.JSONDecodeError, IOError) as e:
                    logger.error("Failed to load Q-table, attempting backup recovery: %s", e)
                    self._recover_from_backup()
            else:
                logger.info("No Q-table found at %s, starting fresh", self._filepath)

    def get(self, state_key: str) -> list[float]:
        """Get Q-values for a state. Returns [0.0, 0.0] if state is unknown."""
        with self._lock:
            if state_key not in self._q_table:
                self._q_table[state_key] = [0.0, 0.0]
                self._visit_counts[state_key] = [0, 0]
            return list(self._q_table[state_key])  # return a copy

    def get_visits(self, state_key: str) -> list[int]:
        """Get visit counts for a state-action pair (used by UCB)."""
        with self._lock:
            if state_key not in self._visit_counts:
                self._visit_counts[state_key] = [0, 0]
            return list(self._visit_counts[state_key])

    def total_visits(self) -> int:
        """Total visits across all state-action pairs."""
        with self._lock:
            return sum(sum(v) for v in self._visit_counts.values())

    def update(self, state_key: str, action: int, new_value: float) -> None:
        """Update a Q-value and increment visit count."""
        with self._lock:
            if state_key not in self._q_table:
                self._q_table[state_key] = [0.0, 0.0]
                self._visit_counts[state_key] = [0, 0]
            self._q_table[state_key][action] = new_value
            self._visit_counts[state_key][action] += 1
            self._dirty = True

    def set_bias(self, state_key: str, action: int, value: float) -> None:
        """Set an initial heuristic bias for a state-action pair."""
        with self._lock:
            if state_key not in self._q_table:
                self._q_table[state_key] = [0.0, 0.0]
                self._visit_counts[state_key] = [0, 0]
            self._q_table[state_key][action] = value

    def state_count(self) -> int:
        """Number of known states."""
        with self._lock:
            return len(self._q_table)

    def snapshot(self) -> dict:
        """Return a deep copy of the Q-table for metrics/debugging."""
        with self._lock:
            return {
                "q_values": {k: list(v) for k, v in self._q_table.items()},
                "visit_counts": {k: list(v) for k, v in self._visit_counts.items()},
            }

    # ── Persistence ─────────────────────────────────────────────────────

    def save(self) -> None:
        """Atomically save Q-table to disk with rolling backup."""
        with self._lock:
            if not self._dirty:
                return
            data = {
                "q_values": self._q_table,
                "visit_counts": self._visit_counts,
            }
            self._dirty = False

        # Write outside lock to minimize lock hold time
        self._atomic_write(data)
        logger.info("Q-table saved (%d states)", len(data["q_values"]))

    def start_periodic_save(self, interval_sec: int = Config.SAVE_INTERVAL_SEC) -> None:
        """Start a background daemon timer to periodically save."""
        if self._shutdown:
            return

        def _tick():
            if not self._shutdown:
                self.save()
                self.start_periodic_save(interval_sec)

        self._save_timer = threading.Timer(interval_sec, _tick)
        self._save_timer.daemon = True
        self._save_timer.start()
        logger.info("Periodic save started (every %ds)", interval_sec)

    def shutdown(self) -> None:
        """Stop periodic saves and do a final save."""
        self._shutdown = True
        if self._save_timer:
            self._save_timer.cancel()
        self.save()
        logger.info("Q-store shut down, final save complete")

    # ── Internal ────────────────────────────────────────────────────────

    def _atomic_write(self, data: dict) -> None:
        """Write to temp file then atomically rename. Rotate backups first."""
        self._rotate_backups()
        tmp_path = self._filepath + ".tmp"
        try:
            with open(tmp_path, "w") as f:
                json.dump(data, f, indent=2)
                f.flush()
                os.fsync(f.fileno())
            os.replace(tmp_path, self._filepath)
        except IOError as e:
            logger.error("Failed to save Q-table: %s", e)
            if os.path.exists(tmp_path):
                os.remove(tmp_path)

    def _rotate_backups(self) -> None:
        """Keep the last N backups: q_table.bak.3 → q_table.bak.2 → q_table.bak.1."""
        if not os.path.exists(self._filepath):
            return
        for i in range(self._backup_count, 1, -1):
            src = f"{self._filepath}.bak.{i - 1}"
            dst = f"{self._filepath}.bak.{i}"
            if os.path.exists(src):
                shutil.move(src, dst)
        # Current file becomes bak.1
        shutil.copy2(self._filepath, f"{self._filepath}.bak.1")

    def _recover_from_backup(self) -> None:
        """Try to recover from the most recent backup."""
        for i in range(1, self._backup_count + 1):
            backup = f"{self._filepath}.bak.{i}"
            if os.path.exists(backup):
                try:
                    with open(backup, "r") as f:
                        data = json.load(f)
                    self._q_table = data.get("q_values", data) if isinstance(data, dict) else {}
                    self._visit_counts = data.get("visit_counts", {}) if isinstance(data, dict) and "q_values" in data else {}
                    logger.info("Recovered Q-table from backup %s (%d states)", backup, len(self._q_table))
                    return
                except (json.JSONDecodeError, IOError):
                    continue
        logger.warning("No valid backup found, starting with empty Q-table")
        self._q_table = {}
        self._visit_counts = {}
