"""
LRU-based idempotency cache for deduplicating /update requests.

When a Java client retries a failed request, we don't want to double-apply
the reward. Each request carries an X-Request-ID header. If we've seen it
before, we return the cached response without re-processing.
"""
import logging
import threading
import time
from collections import OrderedDict
from typing import Any, Optional

from config import Config

logger = logging.getLogger("rl_optimizer.idempotency")


class IdempotencyCache:
    """Thread-safe LRU cache with TTL for request deduplication."""

    def __init__(
        self,
        max_size: int = Config.IDEMPOTENCY_CACHE_SIZE,
        ttl_sec: int = Config.IDEMPOTENCY_TTL_SEC,
    ):
        self._max_size = max_size
        self._ttl_sec = ttl_sec
        self._lock = threading.Lock()
        self._cache: OrderedDict[str, tuple[float, Any]] = OrderedDict()

    def get(self, request_id: str) -> Optional[Any]:
        """
        Look up a request ID in the cache.
        Returns the cached response if found and not expired, else None.
        """
        with self._lock:
            if request_id not in self._cache:
                return None
            timestamp, response = self._cache[request_id]
            if time.monotonic() - timestamp > self._ttl_sec:
                # Expired — remove and return None
                del self._cache[request_id]
                return None
            # Move to end (most recently used)
            self._cache.move_to_end(request_id)
            logger.debug("Idempotency hit for request %s", request_id)
            return response

    def put(self, request_id: str, response: Any) -> None:
        """Store a response for a request ID."""
        with self._lock:
            if request_id in self._cache:
                self._cache.move_to_end(request_id)
                self._cache[request_id] = (time.monotonic(), response)
                return
            # Evict oldest if at capacity
            while len(self._cache) >= self._max_size:
                evicted_key, _ = self._cache.popitem(last=False)
                logger.debug("Evicted idempotency entry: %s", evicted_key)
            self._cache[request_id] = (time.monotonic(), response)

    def size(self) -> int:
        with self._lock:
            return len(self._cache)

    def cleanup_expired(self) -> int:
        """Remove all expired entries. Returns count of removed entries."""
        now = time.monotonic()
        removed = 0
        with self._lock:
            expired_keys = [
                k for k, (ts, _) in self._cache.items()
                if now - ts > self._ttl_sec
            ]
            for k in expired_keys:
                del self._cache[k]
                removed += 1
        if removed:
            logger.debug("Cleaned up %d expired idempotency entries", removed)
        return removed
