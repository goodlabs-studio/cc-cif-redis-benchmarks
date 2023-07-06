package studio.goodlabs;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

public class RedisCache implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(RedisCache.class);

    private final JedisPool jedisPool;

    @SuppressWarnings({"ConstructorWithTooManyParameters", "BooleanParameter"})
    public RedisCache(String host, int port, boolean ssl, int database, Optional<String> maybePassword, int timeout,
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

    @SuppressWarnings("WeakerAccess")
    public <V> V call(Function<? super Jedis, V> function) {
        Jedis jedis = jedisPool.getResource();
        try {
            return function.apply(jedis);
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public void run(Consumer<? super Jedis> consumer) {
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

    public static RedisCache initialize() throws IOException {
        Properties properties = new Properties();
        properties.load(Files.newInputStream(Path.of("conf", "cloud.properties")));
        String redisPassword = Files.readAllLines(Path.of("secrets", "redis", "password.txt")).get(0);
        RedisCache redisCache = new RedisCache(
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
        logger.info("cluster info: {}", redisCache.<String>call(jedis -> jedis.info()));
        return redisCache;
    }

}
