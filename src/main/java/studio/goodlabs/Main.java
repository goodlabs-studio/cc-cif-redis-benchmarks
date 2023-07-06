package studio.goodlabs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        RedisCache redisCache = RedisCache.initialize();
        logger.info("Hello world!");
        Optional<String> test1 = Timer.time(() -> redisCache.get("test"));
        logger.info("test: {}", test1);
        Optional<String> test2 = Timer.time(() -> redisCache.get("test"));
        logger.info("test: {}", test2);
        // https://redis.io/docs/manual/client-side-caching/#:~:text=The%20Redis%20implementation%20of%20client,the%20same%20keys%20are%20modified.
    }

}
