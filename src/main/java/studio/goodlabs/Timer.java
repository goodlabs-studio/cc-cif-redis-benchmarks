package studio.goodlabs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class Timer {

    private static final Logger logger = LoggerFactory.getLogger(Timer.class);

    public static void time(Runnable runnable) {
        long t1 = System.nanoTime();
        runnable.run();
        long t2 = System.nanoTime();
        float timeElapsed = (t2 - t1) / (float) TimeUnit.SECONDS.toNanos(1);
        logger.debug("Time elapsed: {} s.", timeElapsed);
    }

    public static <V> V time(Callable<V> callable) {
        long t1 = System.nanoTime();
        V v;
        try {
            v = callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        long t2 = System.nanoTime();
        float timeElapsed = (t2 - t1) / (float) TimeUnit.SECONDS.toNanos(1);
        logger.debug("Time elapsed: {} s.", timeElapsed);
        return v;
    }

}
