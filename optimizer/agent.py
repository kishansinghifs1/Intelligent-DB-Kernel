"""
RL Agent — Q-Learning with UCB exploration, reward normalization, and experience replay.

This is the brain of the optimizer. It decides which scan strategy to use
and learns from execution feedback.

Architecture:
  - State encoding: 5-feature bucketing → ~360 possible states 5-feature bucketing → ~360 possible states
  - Action selection: UCB + decaying ε-greedy hybrid
  - Reward normalization: running baseline per state
  - Learning: single-step Q-update + periodic mini-batch replay
"""
import logging
import math
import threading
from typing import Optional

import numpy as np

from config import Config
from experience_store import Experience, ExperienceStore
from q_store import QStore

logger = logging.getLogger("rl_optimizer.agent")


class RLAgent:
    """
    Reinforcement Learning agent for query optimization.

    Uses Q-Learning in a contextual bandit setting (single-step, no next-state)
    with UCB exploration and experience replay for stability.
    """

    NUM_ACTIONS = 2  # 0=SeqScan, 1=IndexScan

    def __init__(self, q_store: QStore, exp_store: ExperienceStore):
        self._q_store = q_store
        self._exp_store = exp_store
        self._epsilon = Config.EPSILON_START
        self._update_count = 0
        self._predict_count = 0
        self._lock = threading.Lock()

        # Running reward baselines per state for normalization
        self._reward_baselines: dict[str, float] = {}
        self._baseline_alpha = 0.1  # EMA smoothing factor

    # ── State Encoding ──────────────────────────────────────────────────

    @staticmethod
    def encode_state(
        num_rows: int,
        is_range: bool,
        has_index: bool,
        predicate_type: int = 0,
        estimated_matches: int = 0,
    ) -> tuple[str, int, int, int, int, int]:
        """
        Encode raw query features into a discretized state key.

        Returns (state_key, size_bucket, selectivity, has_index, pred_type, cardinality_bucket)
        """
        # Size bucket: 4 levels
        if num_rows < 50:
            size = 0       # tiny
        elif num_rows < 1000:
            size = 1       # small
        elif num_rows < 100_000:
            size = 2       # medium
        else:
            size = 3       # large

        # Selectivity: 3 levels
        if not is_range:
            selectivity = 0  # equality
        elif predicate_type == 2:
            selectivity = 2  # wide range
        else:
            selectivity = 1  # narrow range

        index = 1 if has_index else 0

        # Predicate type: 0=equality, 1=narrow_range, 2=wide_range
        pred_type = min(predicate_type, 2)

        # Cardinality bucket: based on estimated matches
        if estimated_matches < 10:
            card = 0   # low selectivity (few matches — index is good)
        elif estimated_matches < 1000:
            card = 1   # medium
        else:
            card = 2   # high selectivity (many matches — seq might win)

        state_key = f"{size}-{selectivity}-{index}-{pred_type}-{card}"
        return state_key, size, selectivity, index, pred_type, card

    # ── Action Selection ────────────────────────────────────────────────

    def predict(self, state_key: str, has_index: bool, is_range: bool) -> int:
        """
        Choose an action using UCB + ε-greedy hybrid.

        Returns 0 (SeqScan) or 1 (IndexScan).
        """
        with self._lock:
            self._predict_count += 1

        q_values = self._q_store.get(state_key)
        visits = self._q_store.get_visits(state_key)
        total = max(self._q_store.total_visits(), 1)

        # Apply heuristic bias for unseen states
        if sum(visits) == 0:
            if has_index and not is_range:
                self._q_store.set_bias(state_key, 1, 0.1)  # slight IndexScan edge
                q_values = self._q_store.get(state_key)

        # ε-greedy: random exploration with decaying probability
        if np.random.random() < self._epsilon:
            action = np.random.randint(self.NUM_ACTIONS)
            logger.debug("ε-explore: action=%d state=%s ε=%.4f", action, state_key, self._epsilon)
            return int(action)

        # UCB scores
        ucb_scores = []
        for a in range(self.NUM_ACTIONS):
            if visits[a] == 0:
                ucb_scores.append(float("inf"))  # never tried → explore
            else:
                exploration_bonus = Config.UCB_COEFFICIENT * math.sqrt(
                    math.log(total) / visits[a]
                )
                ucb_scores.append(q_values[a] + exploration_bonus)

        action = int(np.argmax(ucb_scores))
        logger.debug(
            "UCB-exploit: action=%d state=%s q=%s ucb=%s",
            action, state_key,
            [f"{v:.4f}" for v in q_values],
            [f"{v:.4f}" for v in ucb_scores],
        )
        return action

    # ── Learning ────────────────────────────────────────────────────────

    def update(
        self,
        state_key: str,
        size_bucket: int,
        selectivity: int,
        has_index: int,
        predicate_type: int,
        cardinality_bucket: int,
        action: int,
        raw_time_ms: float,
        num_rows: int,
    ) -> dict:
        """
        Process a reward signal and update the Q-table.

        Steps:
        1. Normalize the reward using a running baseline
        2. Q-update: Q(s,a) += α * (normalized_reward - Q(s,a))
        3. Store experience in replay buffer
        4. Decay exploration rate
        5. Periodically replay a mini-batch
        """
        # Normalize reward
        reward = self._normalize_reward(state_key, raw_time_ms)

        # Q-update
        q_values = self._q_store.get(state_key)
        old_value = q_values[action]
        new_value = old_value + Config.LEARNING_RATE * (reward - old_value)
        self._q_store.update(state_key, action, new_value)

        logger.info(
            "Q-update: state=%s action=%d old=%.4f new=%.4f reward=%.4f (raw=%.2fms)",
            state_key, action, old_value, new_value, reward, raw_time_ms,
        )

        # Store experience
        exp = Experience(
            state_key=state_key,
            size_bucket=size_bucket,
            selectivity=selectivity,
            has_index=has_index,
            predicate_type=predicate_type,
            cardinality_bucket=cardinality_bucket,
            action=action,
            reward=reward,
            raw_time_ms=raw_time_ms,
            num_rows=num_rows,
        )
        self._exp_store.add(exp)

        # Decay exploration
        with self._lock:
            self._update_count += 1
            self._epsilon = max(
                Config.EPSILON_MIN,
                self._epsilon * Config.EPSILON_DECAY,
            )
            count = self._update_count

        # Periodic replay
        if count % Config.REPLAY_INTERVAL == 0:
            self._replay_batch()

        # Periodic prune
        if count % 1000 == 0:
            self._exp_store.prune()

        return {
            "old_value": round(old_value, 6),
            "new_value": round(new_value, 6),
            "normalized_reward": round(reward, 6),
            "epsilon": round(self._epsilon, 6),
        }

    def _normalize_reward(self, state_key: str, raw_time_ms: float) -> float:
        """
        Normalize reward relative to a running baseline for each state.

        Faster-than-average → positive reward.
        Slower-than-average → negative reward.
        """
        raw_reward = -raw_time_ms  # negative time = reward

        with self._lock:
            if state_key not in self._reward_baselines:
                self._reward_baselines[state_key] = raw_reward
                return 0.0  # first observation — neutral

            baseline = self._reward_baselines[state_key]
            # Update baseline with EMA
            self._reward_baselines[state_key] = (
                self._baseline_alpha * raw_reward
                + (1 - self._baseline_alpha) * baseline
            )

        # Normalized: how much better/worse than baseline
        denom = abs(baseline) + 1e-6
        return (raw_reward - baseline) / denom

    def _replay_batch(self) -> None:
        """Sample a mini-batch from experience buffer and replay Q-updates."""
        batch = self._exp_store.sample(Config.REPLAY_BATCH_SIZE)
        if not batch:
            return

        replayed = 0
        for exp in batch:
            q_values = self._q_store.get(exp.state_key)
            old_value = q_values[exp.action]
            new_value = old_value + Config.LEARNING_RATE * (exp.reward - old_value)
            self._q_store.update(exp.state_key, exp.action, new_value)
            replayed += 1

        logger.debug("Replayed %d experiences from buffer", replayed)

    # ── Metrics ─────────────────────────────────────────────────────────

    def metrics(self) -> dict:
        """Return current agent statistics."""
        with self._lock:
            epsilon = self._epsilon
            predict_count = self._predict_count
            update_count = self._update_count

        return {
            "total_predictions": predict_count,
            "total_updates": update_count,
            "exploration_rate": round(epsilon, 6),
            "q_table_states": self._q_store.state_count(),
            "experience_buffer_size": self._exp_store.size(),
            "avg_reward_last_100": self._exp_store.avg_reward_last_n(100),
            "avg_reward_last_1000": self._exp_store.avg_reward_last_n(1000),
            "action_distribution": self._exp_store.action_distribution(),
            "learning_curve": self._exp_store.learning_curve(window=100),
        }
