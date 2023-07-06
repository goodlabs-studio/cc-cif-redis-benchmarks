package studio.goodlabs;

import java.io.Closeable;
import java.util.List;
import java.util.Optional;

public interface RedisCache extends Closeable {

    Optional<String> get(String key);

    void mset(String... keysAndValues);

    List<String> scan(int maxCount);

    void removeAll(String... keys);

}
