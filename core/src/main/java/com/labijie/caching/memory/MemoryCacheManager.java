/**
 * Created with IntelliJ IDEA.
 *
 * @author Anders Xiao
 * @date 2018-08-30
 */
package com.labijie.caching.memory;

import com.labijie.caching.*;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ax03 on 2017/3/6.
 */
public class MemoryCacheManager implements ICacheManager {

    public final static String DEFAULT_CACHE_NAME = "mozart.default";
    private ConcurrentHashMap<String, HashSet<String>> caches;
    private MemoryCache cache = null;

    public MemoryCacheManager(MemoryCacheOptions options) {
        Guard.argumentNotNull(options, "options");

        setup(options);
    }

    public void setup(MemoryCacheOptions options) {
        if(options == null){
            options = new MemoryCacheOptions();
        }
        caches = new ConcurrentHashMap<String, HashSet<String>>();
        cache = new MemoryCache(options);
    }

    private void validateRegion(String region) {
        if (StringUtil.isNullOrWhiteSpace(region)) {
            return;
        }
        if (region.contains("|")) {
            throw new IllegalArgumentException("缓存区域名称中不能包含字符串 \"|\" 。");
        }
    }

    private String getFullKey(String region, String key) {
        region = getRegionName(region);
        return region + "|" + key;
    }

    private String getRegionName(String region) {
        return StringUtil.isNullOrWhiteSpace(region) ? DEFAULT_CACHE_NAME : region.trim();
    }

    private String getRegionNameFormFullKey(String fullKey) {
        return fullKey.split("|")[0];
    }

    private MemoryCacheEntryOptions createTimeoutOptions(Long cacheTime, boolean useSlidingExpiration) {
        MemoryCacheEntryOptions options = new MemoryCacheEntryOptions();
        if (!useSlidingExpiration) {
            options.setAbsoluteExpirationRelativeToNow(cacheTime);
        } else {
            options.setSlidingExpirationMilliseconds(cacheTime);
        }
        return options;
    }

    private void callback(Object key, Object item, EvictionReason reason, Object state) {
        String stringKey = key == null ? "" : key.toString();
        String region = this.getRegionNameFormFullKey(stringKey);
        switch (reason) {
            case Capacity:
            case Removed:
            case Expired:
                HashSet<String> regionKeys = caches.get(stringKey);
                if (regionKeys != null) {
                    regionKeys.remove(stringKey);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public Object get(String key, String region) {
        Guard.argumentNullOrWhiteSpaceString(key, "key");
        this.validateRegion(region);

        String name = getRegionName(region);
        String fullKey = this.getFullKey(name, key);

        return cache.get(fullKey);
    }

    @Override
    public void set(String key, Object data, Long timeoutMilliseconds, String region, boolean useSlidingExpiration) {
        Guard.argumentNullOrWhiteSpaceString(key, "key");
        this.validateRegion(region);
        if (data == null) {
            this.remove(key, region);
            return;
        }

        String name = getRegionName(region);
        String fullKey = this.getFullKey(name, key);

        HashSet<String> regionKeys = caches.computeIfAbsent(name, k -> new HashSet<String>());
        regionKeys.add(fullKey);
        MemoryCacheEntryOptions options = createTimeoutOptions(timeoutMilliseconds, useSlidingExpiration);
        options.getPostEvictionCallbacks().add(new PostEvictionCallbackRegistration(new IPostEvictionCallback() {
            @Override
            public void callback(Object key, Object value, EvictionReason reason, Object state) {
                ((MemoryCacheManager) state).callback(key, value, reason, state);
            }
        }, this));
        cache.set(fullKey, data, options);
    }

    @Override
    public void remove(String key, String region) {
        this.validateRegion(region);

        String name = getRegionName(region);
        String fullKey = this.getFullKey(name, key);

        cache.remove(fullKey);
    }

    @Override
    public boolean refresh(String key, String region) {
        Object result = this.get(key, region);
        return result != null;
    }

    @Override
    public void clearRegion(String region) {
        this.validateRegion(region);

        String name = getRegionName(region);
        HashSet<String> regionKeys = caches.getOrDefault(name, null);
        if (regionKeys != null) {
            for (String v : regionKeys) {
                cache.remove(v);
            }
        }
    }

    @Override
    public void clear() {
        Enumeration<String> keys = this.caches.keys();
        while (keys.hasMoreElements()) {
            HashSet<String> values = this.caches.getOrDefault(keys.nextElement(), null);
            if (values != null) {
                for (String v : values) {
                    cache.remove(v);
                }
            }
        }
    }
}
