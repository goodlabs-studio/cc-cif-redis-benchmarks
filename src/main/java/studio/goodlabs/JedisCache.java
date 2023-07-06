package studio.goodlabs;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

public class JedisCache implements RedisCache {

    private static final Logger logger = LoggerFactory.getLogger(JedisCache.class);

    private final JedisPool jedisPool;

    @SuppressWarnings({"ConstructorWithTooManyParameters", "BooleanParameter"})
    public JedisCache(String host, int port, boolean ssl, int database, Optional<String> maybePassword, int timeout,
                      int minIdle, int maxIdle, int maxTotal) {
        GenericObjectPoolConfig<Jedis> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMinIdle(minIdle);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMaxTotal(maxTotal);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        logger.info("Creating Redis pool: {}:{}/{}", host, port, database);
        jedisPool = new JedisPool(poolConfig, host, port, timeout, maybePassword.orElse(null), database, ssl);
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(call(jedis -> jedis.get(key)));
    }

    public void put(String key, String value) {
        run(jedis -> jedis.set(key, value));
    }

    public void remove(String key) {
        run(jedis -> jedis.del(key));
    }

    @Override
    public void removeAll(String... keys) {
        run(jedis -> jedis.del(keys));
    }

    @Override
    public void mset(String... keysAndValues) {
        run(jedis -> jedis.mset(keysAndValues));
    }

    @Override
    public List<String> scan(int maxCount) {
        //noinspection OverlyLongLambda
        return call(jedis -> {
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
        });
    }

    @SuppressWarnings("WeakerAccess")
    private <V> V call(Function<? super Jedis, V> function) {
        Jedis jedis = jedisPool.getResource();
        try {
            return function.apply(jedis);
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    @SuppressWarnings("WeakerAccess")
    private void run(Consumer<? super Jedis> consumer) {
        Jedis jedis = jedisPool.getResource();
        try {
            consumer.accept(jedis);
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    @Override
    public void close() {
        try {
            jedisPool.close();
        } catch (JedisException e) {
            // nop
        }
    }

    public static JedisCache initialize() throws IOException {
        Properties properties = new Properties();
        properties.load(Files.newInputStream(Path.of("conf", "cloud.properties")));
        String redisPassword = Files.readAllLines(Path.of("secrets", "redis", "password.txt")).get(0);
        JedisCache redisCache = new JedisCache(
                properties.getProperty("redis.host"),
                Integer.parseUnsignedInt(properties.getProperty("redis.port")),
                Boolean.parseBoolean(properties.getProperty("redis.ssl")),
                Integer.parseInt(properties.getProperty("redis.database")),
                Optional.of(redisPassword),
                Integer.parseUnsignedInt(properties.getProperty("redis.timeout")),
                Integer.parseUnsignedInt(properties.getProperty("redis.minIdle")),
                Integer.parseUnsignedInt(properties.getProperty("redis.maxIdle")),
                Integer.parseUnsignedInt(properties.getProperty("redis.maxTotal"))
        );
        logger.info("cluster info: {}", redisCache.<String>call(Jedis::info));
        return redisCache;
    }

}
