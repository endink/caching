package com.labijie.caching.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labijie.caching.StringUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by ax03 on 2017/7/12.
 */
public class SingleRedisCacheManager extends RedisCacheManager {

    private final static String NEW_LINE = System.getProperty("line.separator");
    private final static String REGION_SPLITER_CHARS = "-";

    // KEYS[1] = = key
    // ARGV[1] = absolute-expiration - ticks as long (-1 for none)
    // ARGV[2] = sliding-expiration - ticks as long (-1 for none)
    // ARGV[3] = relative-expiration (long, in seconds, -1 for none) - Min(absolute-expiration - Now, sliding-expiration)
    // ARGV[4] = data - byte[]
    // ARGV[5] = type - type string
    // ARGV[6] = compress - bool wether use gzip
    // this order should not change LUA script depends on it
    private final static String SET_SCRIPT = "local result = 1 " + NEW_LINE +
            "redis.call('HMSET', KEYS[1], 'absexp', ARGV[1], 'sldexp', ARGV[2], 'data', ARGV[4], 'type', ARGV[5], 'compress', ARGV[6], 'ser', ARGV[7]) " + NEW_LINE +
            "if ARGV[3] ~= '-1' then" + NEW_LINE +
            "result = redis.call('EXPIRE', KEYS[1], ARGV[3]) " + NEW_LINE +
            " end " + NEW_LINE +
            "return result";

    // ARGV[1] : key pattern
    private final static String QUERY_KEYS_SCRIPT = "local cursor = 0 " + NEW_LINE +
            "local pattern = ARGV[1] " + NEW_LINE +
            "local keys = {}" + NEW_LINE +
            "repeat " + NEW_LINE +
            "local r = redis.call('SCAN', cursor, 'MATCH', pattern) " + NEW_LINE +
            "cursor = tonumber(r[1]) " + NEW_LINE +
            "                   for k,v in ipairs(r[2]) do " + NEW_LINE +
            "       table.insert(keys, v) " + NEW_LINE +
            "end " + NEW_LINE +
            "until cursor == 0 " + NEW_LINE +
            "       return keys";

    private final static String ABSOLUTE_EXPIRATIONKEY = "absexp";
    private final static String SLIDING_EXPIRATION_KEY = "sldexp";
    private final static String DATA_KEY = "data";
    private final static String TYPE_KEY = "type";
    private final static String COMPRESS_KEY = "compress";
    private final static String SERIALIZER_KEY = "ser";
    private final static long NOT_PRESENT = -1;

    private String defaultRegion = null;
    private ObjectMapper jacksonMapper = null;

    public SingleRedisCacheManager(RedisCacheOptions redisOptions) {
        super(redisOptions);
        defaultRegion = "__default";
        jacksonMapper = new ObjectMapper();
    }

    protected String GetFullKey(String region, String key) {
        return this.GetKeyPrefix(region) + key;
    }

    protected  String GetKeyPrefix(String region){
        String r = StringUtil.isNullOrWhiteSpace(region) ? this.defaultRegion : region.trim();
        return "H" +REGION_SPLITER_CHARS + r + REGION_SPLITER_CHARS;
    }

    private void validateKey(String key) {
        if (StringUtil.isNullOrWhiteSpace(key)) {
            throw new IllegalArgumentException("key cant not be null or empty string");
        }
    }

    private Optional<CacheHashData> getAndRefresh(Jedis jedis, String fullKey, boolean getData) {
        List<String> hashResult = getData ?
                jedis.hmget(fullKey, ABSOLUTE_EXPIRATIONKEY, SLIDING_EXPIRATION_KEY,  DATA_KEY, TYPE_KEY, COMPRESS_KEY, SERIALIZER_KEY) :
                jedis.hmget(fullKey, ABSOLUTE_EXPIRATIONKEY, SLIDING_EXPIRATION_KEY);

        String[] arrays = new String[hashResult.size()];
        hashResult.toArray(arrays);

        if(hashResult.stream().filter(e->e != null).count() == 0){
            return Optional.empty();
        }

        // TODO: Error handling
        if (hashResult.size() >= 2) {
            hashResult.toArray(arrays);
            Optional<Long>[] values = MapMetadata(arrays);
            this.refreshExpire(fullKey, values[0], values[1], jedis);
        }

        if (hashResult.size() >= 6 &&
                !StringUtil.isNullOrWhiteSpace(arrays[2]) &&
                !StringUtil.isNullOrWhiteSpace(arrays[3]) &&
                !StringUtil.isNullOrWhiteSpace(arrays[4])) {
            String type = arrays[3];
            String data = arrays[2];
            Boolean compressed = Boolean.parseBoolean(arrays[4]);
            String serializer = arrays[5];

            return Optional.ofNullable(new CacheHashData(type, data, serializer, compressed));
        }
        return Optional.empty();
    }

    private Optional<Long>[] MapMetadata(String[] results) {
        Optional<Long> slidingExpiration = StringUtil.tryParseLong(results[1]);
        if(slidingExpiration.isPresent() && slidingExpiration.get().equals(NOT_PRESENT)){
            slidingExpiration = Optional.empty();
        }
        Optional<Long> absoluteExpirationTicks = StringUtil.tryParseLong(results[0]);
        if(absoluteExpirationTicks.isPresent() && absoluteExpirationTicks.get().equals(NOT_PRESENT)){
            absoluteExpirationTicks = Optional.empty();
        }
        return new Optional[]{absoluteExpirationTicks, slidingExpiration};
    }

    private void refreshExpire(String fullKey, Optional<Long> absExpr, Optional<Long> sldExpr, Jedis client) {
        // Note Refresh has no effect if there is just an absolute expiration (or neither).
        Long expr = null;
        if (sldExpr.isPresent()) {
            if (absExpr.isPresent()) {
                Long relExpr = absExpr.get() - System.currentTimeMillis();
                expr = (relExpr <= sldExpr.get() ? relExpr : sldExpr.get());
            } else {
                expr = sldExpr.get();
            }

            if (client == null) {
                try (Jedis jedis = this.createCommand(Jedis.class)) {
                    jedis.expire(fullKey, (int) (expr / 1000));
                }
            } else {
                client.expire(fullKey, (int) (expr / 1000));
            }
            // TODO: Error handling
        }
    }

    protected void setCore(String key, String region, Object data, Long timeoutMills, boolean useSlidingExpiration) {
        this.validateKey(key);

        if (data == null) {
            this.remove(key, region);
            return;
        }
        long creationTime = System.currentTimeMillis();

        String[] values = new String[]
                {
                        this.GetFullKey(region, key),
                        String.valueOf((!useSlidingExpiration && timeoutMills != null) ? creationTime + timeoutMills : NOT_PRESENT),
                        String.valueOf((useSlidingExpiration && timeoutMills != null) ? timeoutMills : NOT_PRESENT),
                        String.valueOf(timeoutMills != null ? timeoutMills / 1000 : NOT_PRESENT),
                        this.serializeData(this.getOptions().getSerializer(), data, this.getOptions().isUseGzip()),
                        data.getClass().getName(),
                        String.valueOf(this.getOptions().isUseGzip()),
                        this.getOptions().getSerializer()
                };

        try (Jedis jedis = this.createCommand(Jedis.class)) {
            String script = jedis.scriptLoad(SET_SCRIPT);
            Object result = jedis.evalsha(script,  1, values);
            if (result == null) {
                this.getLogger().error(String.format("添加缓存时 redis 缓存返回了错误的结果 (  key: %s, region: %s )。", key, region));
            }
        }
    }

    private <T> T deserializeData(String serializerName, Class<T> type, String data, boolean gzipCompress) {
        ICacheDataSerializer ser = CacheDataSerializerRegistry.DEFAULT.getSerializer(serializerName);
        return ser.deserializeData(type, data, gzipCompress);
    }


    private String serializeData(String serializerName, Object data,  boolean gzipCompress) {
        ICacheDataSerializer ser = CacheDataSerializerRegistry.DEFAULT.getSerializer(serializerName);
        return ser.serializeData(data, gzipCompress);
    }


    @Override
    public Object get(String key, String region) {
        this.validateKey(key);
        String fullKey = this.GetFullKey(region, key);
        try (Jedis jedis = super.createCommand(Jedis.class)) {
            Optional<CacheHashData> data = this.getAndRefresh(jedis, fullKey, true);
            if (data.isPresent()) {

                CacheHashData cacheHashData = data.get();
                //考虑程序变更后类型可能已经不存在或更名。
                Class clazz = null;
                try {
                    clazz = Class.forName(cacheHashData.getType());
                } catch (ClassNotFoundException cne) {
                    this.remove(key, region);
                    this.getLogger().warn(String.format("缓存数据中的类型 %s 可能已经变更，导致缓存失效（key: %s, region: %s）。",
                            data.get().getType(), key, region), cne);
                    return null;
                }
                //考虑数据结构更新后缓存反序列化的问题。
                try {
                    return this.deserializeData(
                            cacheHashData.getSerializer(),
                            clazz,
                            cacheHashData.getData(),
                            cacheHashData.isCompressed());
                } catch (RuntimeException ex) {
                    this.remove(key, region);
                    return null;
                }

            }
        } catch (JedisException e) {
            this.getLogger().error(String.format("从 redis 中获取数据失败 ( key: %s, region: %s )。", key, region), e);
        }
        return null;
    }

    @Override
    public void set(String key, Object data, Long timeoutMilliseconds, String region, boolean useSlidingExpiration) {
        if(data.getClass().equals(Object.class)){
            this.getLogger().warn("放入缓存的对象类型为 Object, 已被忽略。");
            return;
        }
        try {
            this.setCore(key, region, data, timeoutMilliseconds, useSlidingExpiration);
        } catch (JedisException ex) {
            this.getLogger().error("添加缓存时发生错误。", ex);
        }
    }

    @Override
    public void remove(String key, String region) {
        this.validateKey(key);
        try (Jedis jedis = this.createCommand(Jedis.class)) {
            String fullKey = this.GetFullKey(region, key);
            jedis.del(fullKey);
        } catch (JedisException ex) {
            this.getLogger().warn(String.format("移除缓存时 Redis 发生错误 ( key: %s, region: %s )。", key, region), ex);
        }
    }

    @Override
    public boolean refresh(String key, String region) {
        this.validateKey(key);
        String fullKey = this.GetFullKey(region, key);
        try {
            try (Jedis jedis = this.createCommand(Jedis.class)) {
                return this.getAndRefresh(jedis, fullKey, false).isPresent();
            }
        } catch (JedisException ex) {
            this.getLogger().warn(String.format("刷新缓存时 Redis 发生错误 ( key: %s, region: %s )。", key, region), ex);
            return false;
        }
    }

    @Override
    public void clearRegion(String region) {
        region = StringUtil.isNullOrWhiteSpace(region) ? "default" : region.trim();
        String parttern = this.GetKeyPrefix(region)+ "*";
        try(Jedis jedis = this.createCommand(Jedis.class)) {
            String script = jedis.scriptLoad(QUERY_KEYS_SCRIPT);
            List<String> keys = (List<String>) jedis.evalsha(script, 0, new String[]{parttern});

            if (keys != null && keys.size() > 0) {
                keys = keys.stream().distinct().collect(Collectors.toList());
                String[] keysArray = new String[keys.size()];
                keys.toArray(keysArray);
                jedis.del(keysArray);
            }
        }
        catch (JedisException ex){
            this.getLogger().error("清理缓存区域时发生错误。", ex);
        }
    }

    @Override
    public void clear() {
        try(Jedis jedis = this.createCommand(Jedis.class)) {
           String script = jedis.scriptLoad(CLEAR_SCRIPT);
            jedis.evalsha(script);
        }
        catch (JedisException ex){
            this.getLogger().error("清理缓存时发生错误。", ex);
        }
    }
}
