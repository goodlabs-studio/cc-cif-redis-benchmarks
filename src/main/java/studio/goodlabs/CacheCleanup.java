package studio.goodlabs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CacheCleanup {

    private static final Logger logger = LoggerFactory.getLogger(CacheCleanup.class);

    private static final boolean DELETE = false;
    private static final int MAX_COUNT = 1_000_000;

    public static void main(String[] args) throws IOException {
        logger.info("connecting to Redis");
        try (RedisCache redisCache = RedisCache.initialize()) {
            List<String> ccNumbers;
            do {
                logger.info("looking for #s");
                ccNumbers = redisCache.call(jedis -> scan(jedis, MAX_COUNT));
                logger.info("found #s: {}", "%,d".formatted(ccNumbers.size()));
                if (DELETE && !ccNumbers.isEmpty()) {
                    redisCache.run(jedis -> jedis.del(ccNumbers.toArray(String[]::new)));
                    logger.info("deleted #s");
                }
            } while (DELETE && !ccNumbers.isEmpty());
        }
    }

    @SuppressWarnings({"TypeMayBeWeakened", "SameParameterValue"})
    private static List<String> scan(Jedis jedis, int maxCount) {
        ScanParams scanParams = new ScanParams().count(maxCount);
        List<String> keys = null;
        String cursor = ScanParams.SCAN_POINTER_START;
        do {
            ScanResult<String> scanResult = jedis.scan(cursor, scanParams);
            List<String> result = scanResult.getResult();
            if (keys == null)
                keys = new ArrayList<>(result.size());
            keys.addAll(result);
            cursor = scanResult.getCursor();
        } while (!cursor.equals(ScanParams.SCAN_POINTER_START) && (keys.size() < maxCount));
        return keys;
    }

}
