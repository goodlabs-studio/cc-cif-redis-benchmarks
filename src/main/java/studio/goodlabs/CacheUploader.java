package studio.goodlabs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CacheUploader {

    private static final Logger logger = LoggerFactory.getLogger(CacheUploader.class);

    private static final int LINE_BUFFER_SIZE = 100_000;

    public static void main(String[] args) throws IOException {
        RedisCache redisCache = JedisCache.initialize();
        Path mappingFile = Path.of("data", "cc_cif.txt");
        try (BufferedReader in = Files.newBufferedReader(mappingFile)) {
            List<String> lines = new ArrayList<>(LINE_BUFFER_SIZE);
            AtomicInteger counter = new AtomicInteger(0);
            while (!Thread.interrupted()) {
                String line = in.readLine();
                if (line == null)
                    //noinspection BreakStatement
                    break;
                lines.add(line);
                if (lines.size() >= LINE_BUFFER_SIZE) {
                    populateCache(redisCache, lines, counter);
                    lines.clear();
                }
            }
            if (!lines.isEmpty()) {
                populateCache(redisCache, lines, counter);
            }
        }
    }

    private static void populateCache(RedisCache redisCache, List<String> lines, AtomicInteger counter) {
        String[] keysAndValues = new String[lines.size() * 2];
        for (int i = 0; i < lines.size(); ++i) {
            String line = lines.get(i);
            //noinspection MagicNumber
            String ccNo = line.substring(0, 16);
            //noinspection MagicNumber
            String cif = line.substring(17);
            keysAndValues[i * 2] = ccNo;
            keysAndValues[i * 2 + 1] = cif;
        }
        redisCache.mset(keysAndValues);
        logger.debug("Populated {} entries", "%,d".formatted(counter.addAndGet(lines.size())));
    }

    // PoC for TD using Redis in Azure.
    // 36MM accounts mapping between card number (16 digits) to another number called CIF (9 digits) to measure the latency on lookup on the client side.
    // DSAP will be providing this mapping on their system and another system called FDIP outside of DSAP will perform the lookup.
    // We need to know what the 36MM mapping Redis's size will look like and how various approaches including client side caching performance will look like.
    // We will also need the understanding on how fast the mapping update (only insert) will reflect in the mapping.

}
