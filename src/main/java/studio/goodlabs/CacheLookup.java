package studio.goodlabs;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.DoubleStream;

public class CacheLookup {

    private static final Logger logger = LoggerFactory.getLogger(CacheLookup.class);

    private static final int LOOKUP_COUNT = 100;
    private static final int WARMUP_COUNT = 5;

    public static void main(String[] args) throws IOException {
        logger.info("connecting to Redis");
        try (RedisCache redisCache = RedisCache.initialize()) {
            try (CCCIFFile ccCifFile = new CCCIFFile(Path.of("data", "cc_cif.txt"))) {
                double[] redisTimes = new double[LOOKUP_COUNT];
                for (int i = 0; i < LOOKUP_COUNT; ++i) {
                    int randomIndex = new Random().nextInt(36_000_000);
                    long t1 = System.nanoTime();
                    Map.Entry<String, String> entry = ccCifFile.read(randomIndex);
                    long t2 = System.nanoTime();
                    String ccNo = entry.getKey();
                    String cachedCif = redisCache.get(ccNo).orElseThrow(() -> new IllegalArgumentException("CIF not found for: " + ccNo));
                    long t3 = System.nanoTime();
                    double fileTime = (t2 - t1) / (double) TimeUnit.MILLISECONDS.toNanos(1);
                    double redisTime = (t3 - t2) / (double) TimeUnit.MILLISECONDS.toNanos(1);
                    redisTimes[i] = redisTime;
                    String cif = entry.getValue();
                    logger.debug("{}: {} vs {}, file time {} ms, redis time {} ms", ccNo, cif, cachedCif, fileTime, redisTime);
                    if (!cif.equals(cachedCif))
                        throw new IllegalArgumentException("CIF don't match: " + cif + " vs cached " + cif);
                }
                Percentile percentile = new Percentile();
                logger.info("min = {} ms", "%.1f".formatted(DoubleStream.of(redisTimes).skip(WARMUP_COUNT).min().getAsDouble()));
                logger.info("median = {} ms", "%.1f".formatted(percentile.evaluate(redisTimes, WARMUP_COUNT, LOOKUP_COUNT - WARMUP_COUNT, 50)));
                logger.info("p(0.95) = {} ms", "%.1f".formatted(percentile.evaluate(redisTimes, WARMUP_COUNT, LOOKUP_COUNT - WARMUP_COUNT, 95)));
                logger.info("p(0.99) = {} ms", "%.1f".formatted(percentile.evaluate(redisTimes, WARMUP_COUNT, LOOKUP_COUNT - WARMUP_COUNT, 99)));
                logger.info("max = {} ms", "%.1f".formatted(DoubleStream.of(redisTimes).skip(WARMUP_COUNT).max().getAsDouble()));
            }
        }
    }

}
