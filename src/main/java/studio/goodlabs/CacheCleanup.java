package studio.goodlabs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class CacheCleanup {

    private static final Logger logger = LoggerFactory.getLogger(CacheCleanup.class);

    private static final boolean DELETE = false;
    private static final int MAX_COUNT = 1_000_000;

    public static void main(String[] args) throws IOException {
        logger.info("connecting to Redis");
        try (RedisCache redisCache = JedisCache.initialize()) {
            List<String> ccNumbers;
            do {
                logger.info("looking for #s");
                ccNumbers = redisCache.scan(MAX_COUNT);
                logger.info("found #s: {}", "%,d".formatted(ccNumbers.size()));
                if (DELETE && !ccNumbers.isEmpty()) {
                    redisCache.removeAll(ccNumbers.toArray(String[]::new));
                    logger.info("deleted #s");
                }
            } while (DELETE && !ccNumbers.isEmpty());
        }
    }

}
