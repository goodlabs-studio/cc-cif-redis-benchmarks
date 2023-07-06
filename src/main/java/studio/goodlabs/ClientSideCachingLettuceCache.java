package studio.goodlabs;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TrackingArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.support.caching.CacheAccessor;
import io.lettuce.core.support.caching.CacheFrontend;
import io.lettuce.core.support.caching.ClientSideCaching;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.WeakHashMap;

public class ClientSideCachingLettuceCache implements RedisCache {

    private static final Logger logger = LoggerFactory.getLogger(ClientSideCachingLettuceCache.class);

    private final RedisClient client;
    private final RedisCommands<String, String> commands;

    private final CacheFrontend<String, String> cacheFrontend;

    public ClientSideCachingLettuceCache(String host, int port, boolean ssl, char[] password, int database, Duration timeout) {
        client = RedisClient.create(RedisURI.builder()
                .withHost(host)
                .withPort(port)
                .withPassword(password)
                .withDatabase(database)
                .withTimeout(timeout)
                .withSsl(ssl)
                .build());

        client.setOptions(ClientOptions.builder()
                .protocolVersion(ProtocolVersion.RESP3).build());

        StatefulRedisConnection<String, String> connection = client.connect();
        commands = connection.sync();

        Map<String, String> localCache = new WeakHashMap<>(100);
        cacheFrontend = ClientSideCaching.enable(
                CacheAccessor.forMap(localCache), connection, TrackingArgs.Builder.enabled());
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(cacheFrontend.get(key));
    }

    @Override
    public void mset(String... keysAndValues) {
        if ((keysAndValues.length == 0) || ((keysAndValues.length % 2) != 0)) {
            throw new IllegalArgumentException("Invalid argument: " + Arrays.toString(keysAndValues));
        }
        Map<String, String> map = new HashMap<>(keysAndValues.length / 2);
        for (int i = 0; i < keysAndValues.length; i += 2)
            map.put(keysAndValues[i], keysAndValues[i + 1]);
        commands.mset(map);
    }

    @Override
    public void removeAll(String... keys) {
        commands.del(keys);
    }

    @Override
    public List<String> scan(int maxCount) {
        throw new UnsupportedOperationException(); // todo XXX
    }

    @Override
    public void close() {
        cacheFrontend.close();
        client.shutdown();
    }

    public static ClientSideCachingLettuceCache initialize() throws IOException {
        Properties properties = new Properties();
        properties.load(Files.newInputStream(Path.of("conf", "cloud.properties")));
        String redisPassword = Files.readAllLines(Path.of("secrets", "redis", "password.txt")).get(0);
        ClientSideCachingLettuceCache lettuceCache = new ClientSideCachingLettuceCache(
                properties.getProperty("redis.host"),
                Integer.parseUnsignedInt(properties.getProperty("redis.port")),
                Boolean.parseBoolean(properties.getProperty("redis.ssl")),
                redisPassword.toCharArray(),
                Integer.parseInt(properties.getProperty("redis.database")),
                Duration.ofMillis(Integer.parseUnsignedInt(properties.getProperty("redis.timeout")))
        );
        logger.trace("cluster info: {}", lettuceCache.commands.info());
        return lettuceCache;
    }

}
