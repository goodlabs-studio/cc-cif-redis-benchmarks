package studio.goodlabs;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;
import java.util.stream.DoubleStream;

public class CacheLookup {

    private static final Logger logger = LoggerFactory.getLogger(CacheLookup.class);

    private static final int WARMUP_COUNT = 5;

    private static final Options OPTIONS = new Options();

    static {
        OPTIONS.addOption("m", "missCount", true, "cache miss count");
        OPTIONS.addOption("h", "hitCount", true, "cache hit count");
    }

    public static void main(String[] args) throws Exception {
        CommandLine commandLine = new DefaultParser().parse(OPTIONS, args);
        int hitCount = Integer.parseUnsignedInt(Objects.requireNonNull(commandLine.getOptionValue('h'), "missing hitCount"));
        int missCount = Integer.parseUnsignedInt(Objects.requireNonNull(commandLine.getOptionValue('m'), "missing missCount"));
        new CacheLookup(hitCount, missCount);
    }

    private final RandomGenerator random = new Random();

    public CacheLookup(int hitCount, int missCount) throws IOException {
        logger.info("connecting to Redis");
        try (RedisCache redisCache = RedisCache.initialize()) {
            try (CCCIFFile ccCifFile = new CCCIFFile(Path.of("data", "cc_cif.txt"))) {
                int lookupCount = missCount + hitCount;
                int[] indices = new int[lookupCount];
                // fill misses
                for (int i = 0; i < missCount; i++)
                    indices[i] = random.nextInt(36_000_000); // ignore possible unlikely collision
                // fill hits
                for (int i = missCount; i < lookupCount; ++i)
                    indices[i] = random.nextInt(missCount);
                // run
                logger.info("executing {} lookups: {} misses, {} hits", lookupCount, missCount, hitCount);
                double[] redisTimes = new double[lookupCount];
                for (int i = 0; i < lookupCount; ++i) {
                    int randomIndex = indices[i];
                    long t1 = System.nanoTime();
                    Map.Entry<String, String> entry = ccCifFile.read(randomIndex);
                    long t2 = System.nanoTime();
                    String ccNo = entry.getKey();
                    long t3 = System.nanoTime();
                    String cachedCif = redisCache.get(ccNo).orElseThrow(() -> new IllegalArgumentException("CIF not found for: " + ccNo));
                    long t4 = System.nanoTime();
                    double fileTime = (t2 - t1) / (double) TimeUnit.MILLISECONDS.toNanos(1);
                    double redisTime = (t4 - t3) / (double) TimeUnit.MILLISECONDS.toNanos(1);
                    redisTimes[i] = redisTime;
                    String cif = entry.getValue();
                    if (logger.isTraceEnabled())
                        logger.trace("{}: {} vs {}, file read time {} ms, redis time {} ms", ccNo, cif, cachedCif, fileTime, redisTime);
                    if (!cif.equals(cachedCif))
                        throw new IllegalArgumentException("CIF don't match: " + cif + " vs cached " + cif);
                }
                Percentile percentile = new Percentile();
                logger.info("min = {} ms", "%.1f".formatted(DoubleStream.of(redisTimes).skip(WARMUP_COUNT).min().orElseThrow()));
                logger.info("median = {} ms", "%.1f".formatted(percentile.evaluate(redisTimes, WARMUP_COUNT, lookupCount - WARMUP_COUNT, 50)));
                logger.info("p(95) = {} ms", "%.1f".formatted(percentile.evaluate(redisTimes, WARMUP_COUNT, lookupCount - WARMUP_COUNT, 95)));
                logger.info("p(99) = {} ms", "%.1f".formatted(percentile.evaluate(redisTimes, WARMUP_COUNT, lookupCount - WARMUP_COUNT, 99)));
                logger.info("p(99.9) = {} ms", "%.1f".formatted(percentile.evaluate(redisTimes, WARMUP_COUNT, lookupCount - WARMUP_COUNT, 99.9)));
                logger.info("max = {} ms", "%.1f".formatted(DoubleStream.of(redisTimes).skip(WARMUP_COUNT).max().orElseThrow()));
            }
        }
    }

}
