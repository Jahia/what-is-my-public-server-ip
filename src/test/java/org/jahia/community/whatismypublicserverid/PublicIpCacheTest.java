package org.jahia.community.whatismypublicserverid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cache / TTL behaviour of {@link PublicIp#get()}, driven entirely through the test seams
 * ({@code fetchFromUpstream()} + {@code nowMillis()}) with ZERO network access.
 *
 * <p>Proves the 60s process-wide static cache that the AGENTS.md doc wrongly denies (D1/F4/U1).
 */
@DisplayName("PublicIp result cache")
class PublicIpCacheTest {

    private static final long T0 = 1_000_000_000_000L;
    private static final long TTL_MS = 60_000L;

    @BeforeEach
    void resetCache() {
        PublicIp.resetCacheForTesting();
    }

    /** Test double: canned fetch results, a fetch counter, and a scriptable clock. */
    private static final class FakePublicIp extends PublicIp {
        final AtomicInteger fetchCount = new AtomicInteger(0);
        private final String[] results;
        private volatile long clock;

        FakePublicIp(long initialClock, String... results) {
            this.clock = initialClock;
            this.results = results;
        }

        void setClock(long clock) {
            this.clock = clock;
        }

        @Override
        long nowMillis() {
            return clock;
        }

        @Override
        String fetchFromUpstream() {
            final int n = fetchCount.getAndIncrement();
            return results[Math.min(n, results.length - 1)];
        }
    }

    @Test
    @DisplayName("S5: second call within the TTL is served from cache (no second upstream hit)")
    void secondCallWithinTtlHitsCache() {
        // Arrange
        final FakePublicIp subject = new FakePublicIp(T0, "203.0.113.7");

        // Act
        final String first = subject.get();
        subject.setClock(T0 + 59_000L); // still inside the 60s window
        final String second = subject.get();

        // Assert
        assertEquals("203.0.113.7", first);
        assertEquals("203.0.113.7", second);
        assertEquals(1, subject.fetchCount.get(), "second call must be served from cache");
    }

    @Test
    @DisplayName("S6: call at exactly T0+TTL is a miss and re-fetches upstream")
    void callAtTtlBoundaryReFetches() {
        // Arrange — (now - cacheTimestamp) < CACHE_TTL_MS, so == TTL is a miss
        final FakePublicIp subject = new FakePublicIp(T0, "203.0.113.7", "198.51.100.9");

        // Act
        final String first = subject.get();
        subject.setClock(T0 + TTL_MS);
        final String second = subject.get();

        // Assert
        assertEquals("203.0.113.7", first);
        assertEquals("198.51.100.9", second);
        assertNotEquals(first, second);
        assertEquals(2, subject.fetchCount.get(), "TTL boundary must trigger a re-fetch");
    }

    @Test
    @DisplayName("S7: cold cache fetches once, then warm cache serves without re-fetch")
    void coldCacheFetchesThenWarms() {
        // Arrange
        final FakePublicIp subject = new FakePublicIp(T0, "192.0.2.55");

        // Act
        final String cold = subject.get();
        final int afterCold = subject.fetchCount.get();
        final String warm = subject.get(); // same clock

        // Assert
        assertEquals("192.0.2.55", cold);
        assertEquals(1, afterCold, "cold cache performs exactly one fetch");
        assertEquals("192.0.2.55", warm);
        assertEquals(1, subject.fetchCount.get(), "warm cache must not re-fetch");
    }

    @Test
    @DisplayName("S18: concurrent cold-cache access is safe (benign lock-free write-race)")
    void concurrentColdCacheIsSafe() throws InterruptedException {
        // Arrange
        final int threads = 16;
        final FakePublicIp subject = new FakePublicIp(T0, "203.0.113.200");
        final ExecutorService pool = Executors.newFixedThreadPool(threads);
        final CountDownLatch ready = new CountDownLatch(threads);
        final CountDownLatch go = new CountDownLatch(1);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final String[] results = new String[threads];

        // Act
        for (int i = 0; i < threads; i++) {
            final int idx = i;
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    results[idx] = subject.get();
                } catch (Throwable t) {
                    failure.compareAndSet(null, t);
                }
            });
        }
        assertTrue(ready.await(5, TimeUnit.SECONDS), "workers did not start in time");
        go.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS), "workers did not finish in time");

        // Assert — no torn state; every caller gets the valid IP; at least one fetch happened
        assertEquals(null, failure.get(), "no worker may throw");
        for (final String r : results) {
            assertEquals("203.0.113.200", r);
        }
        assertTrue(subject.fetchCount.get() >= 1, "at least one upstream fetch must occur");
    }
}
