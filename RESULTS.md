Redis Java Drivers in scope:
- Jedis
- Lettuce

# Results:

Jedis dismissed: it only supports client-side caching in Beta, and it is significantly slower

Caching:

    Misses: 100 (initial lookups)
    Hits: 10,000 (random repeated lookups from previous misses)

### Random lookup latency

| Driver                        | P(95), ms | P(99), ms | P(99.9), ms | min, ms | max, ms |
|-------------------------------|-----------|-----------|-------------|---------|---------|
| Jedis                         | 4.3       | 4.8       | 8.2         | 3.2     | 20.0    |
| Lettuce                       | 2.4       | 2.8       | 5.6         | 1.6     | 13.8    |
| Lettuce (client-side caching) | 0.0       | 2.4       | 3.0         | 0.0     | 4.9     |

### Jedis log

```
[stan@td-poc-vm01 cc2cif-1.0-SNAPSHOT]$ ./cacheLookup.sh -d jedis -m 100 -h 10000
20:28:35.948 [main] INFO  studio.goodlabs.CacheLookup - connecting to Redis using driver: jedis
20:28:35.953 [main] INFO  studio.goodlabs.JedisCache - Creating Redis pool: tdpoc.redis.cache.windows.net:6380/0
20:28:36.530 [main] INFO  studio.goodlabs.CacheLookup - executing 10100 lookups: 100 misses, 10000 hits
20:29:15.950 [main] INFO  studio.goodlabs.CacheLookup - elapsed time: 39.410 s.
20:29:15.962 [main] INFO  studio.goodlabs.CacheLookup - min = 3.2 ms
20:29:15.969 [main] INFO  studio.goodlabs.CacheLookup - median = 3.9 ms
20:29:15.970 [main] INFO  studio.goodlabs.CacheLookup - p(95) = 4.3 ms
20:29:15.971 [main] INFO  studio.goodlabs.CacheLookup - p(99) = 4.8 ms
20:29:15.972 [main] INFO  studio.goodlabs.CacheLookup - p(99.9) = 8.2 ms
20:29:15.975 [main] INFO  studio.goodlabs.CacheLookup - max = 20.0 ms
```

### Lettuce log

```
[stan@td-poc-vm01 cc2cif-1.0-SNAPSHOT]$ ./cacheLookup.sh -d lettuce -m 100 -h 10000
20:29:28.985 [main] INFO  studio.goodlabs.CacheLookup - connecting to Redis using driver: lettuce
20:29:29.977 [main] INFO  studio.goodlabs.CacheLookup - executing 10100 lookups: 100 misses, 10000 hits
20:29:51.043 [main] INFO  studio.goodlabs.CacheLookup - elapsed time: 21.058 s.
20:29:51.052 [main] INFO  studio.goodlabs.CacheLookup - min = 1.6 ms
20:29:51.059 [main] INFO  studio.goodlabs.CacheLookup - median = 2.0 ms
20:29:51.060 [main] INFO  studio.goodlabs.CacheLookup - p(95) = 2.4 ms
20:29:51.061 [main] INFO  studio.goodlabs.CacheLookup - p(99) = 2.8 ms
20:29:51.062 [main] INFO  studio.goodlabs.CacheLookup - p(99.9) = 5.6 ms
20:29:51.064 [main] INFO  studio.goodlabs.CacheLookup - max = 13.8 ms
```

### Lettuce log with client-side caching 

```
[stan@td-poc-vm01 cc2cif-1.0-SNAPSHOT]$ ./cacheLookup.sh -d lettuceCSC -m 100 -h 10000
20:29:57.405 [main] INFO  studio.goodlabs.CacheLookup - connecting to Redis using driver: lettuceCSC
20:29:58.370 [main] INFO  studio.goodlabs.CacheLookup - executing 10100 lookups: 100 misses, 10000 hits
20:29:58.918 [main] INFO  studio.goodlabs.CacheLookup - elapsed time: 0.538 s.
20:29:58.930 [main] INFO  studio.goodlabs.CacheLookup - min = 0.0 ms
20:29:58.938 [main] INFO  studio.goodlabs.CacheLookup - median = 0.0 ms
20:29:58.939 [main] INFO  studio.goodlabs.CacheLookup - p(95) = 0.0 ms
20:29:58.940 [main] INFO  studio.goodlabs.CacheLookup - p(99) = 2.4 ms
20:29:58.941 [main] INFO  studio.goodlabs.CacheLookup - p(99.9) = 3.0 ms
20:29:58.943 [main] INFO  studio.goodlabs.CacheLookup - max = 4.9 ms
```
