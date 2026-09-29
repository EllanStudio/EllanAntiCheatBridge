package studio.ellan.anticheatbridge.velocity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

final class RateLimiter {
    private final int maximumPerSecond;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    RateLimiter(int maximumPerSecond) {
        this.maximumPerSecond = maximumPerSecond;
    }

    boolean allow(String key) {
        long second = System.currentTimeMillis() / 1000L;
        Window window = windows.compute(key, (ignored, current) -> {
            if (current == null || current.second != second) {
                return new Window(second);
            }
            return current;
        });
        return window.count.incrementAndGet() <= maximumPerSecond;
    }

    private static final class Window {
        private final long second;
        private final AtomicInteger count = new AtomicInteger();

        private Window(long second) {
            this.second = second;
        }
    }
}
