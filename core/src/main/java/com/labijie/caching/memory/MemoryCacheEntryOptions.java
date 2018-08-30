/**
 * Created with IntelliJ IDEA.
 *
 * @author Anders Xiao
 * @date 2018-08-30
 */
package com.labijie.caching.memory;

import com.labijie.caching.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ax03 on 2017/3/6.
 */
public class MemoryCacheEntryOptions
{
    private LocalDateTime absoluteExpiration;
    private Long slidingExpiration;
    private List<IChangeToken> expirationTokens;
    private List<PostEvictionCallbackRegistration> postEvictionCallbacks;
    private CacheItemPriority priority;

    public MemoryCacheEntryOptions() {
        this.expirationTokens = new ArrayList<>();
        this.priority = CacheItemPriority.Normal;
        this.postEvictionCallbacks = new ArrayList<>();
    }

    static final void configureCacheEntry(CacheEntry entry, MemoryCacheEntryOptions options){
        entry.setAbsoluteExpiration(options.getAbsoluteExpiration());
        entry.setSlidingExpirationMilliseconds(options.getSlidingExpirationMilliseconds());
        entry.setPriority(options.getPriority());

        List<IChangeToken> tokens = entry.getExpirationTokens();
        for (IChangeToken token : options.getExpirationTokens()) {
            tokens.add(token);
        }

        for (PostEvictionCallbackRegistration postEvictionCallback : options.getPostEvictionCallbacks()) {
            entry.RegisterPostEvictionCallback(postEvictionCallback.getGetEvictionCallback(), postEvictionCallback.getState());
        }
    }

    /**
     * 获取缓存项优先级。
     * @return
     */
    public CacheItemPriority getPriority() {
        return priority;
    }

    /**
     * 设置缓存项优先级。
     * @param priority
     */
    public void setPriority(CacheItemPriority priority) {
        this.priority = priority;
    }

    /**
     * 获取缓存项令牌集合。
     * @return
     */
    public List<IChangeToken> getExpirationTokens() {
        return expirationTokens;
    }

    /**
     * 获取缓存移除时的回调列表。
     * @return
     */
    public List<PostEvictionCallbackRegistration> getPostEvictionCallbacks() {
        return postEvictionCallbacks;
    }

    /**
     * 获取缓存项绝对过期时间。
     * @return
     */
    public LocalDateTime getAbsoluteExpiration() {
        return absoluteExpiration;
    }

    /**
     * 设置缓存项绝对过期时间（必须使用 Utc 时间）。
     * @param absoluteExpiration 要设置的过期时间。
     */
    public void setAbsoluteExpiration(LocalDateTime absoluteExpiration) {
        this.absoluteExpiration = absoluteExpiration;
    }

    /**
     * 获取缓存项滑动过期时间。
     * @return
     */
    public Long getSlidingExpirationMilliseconds() {
        return slidingExpiration;
    }

    /**
     * 设置缓存项滑动过期时间（单位：毫秒）。
     * @param milliseconds 滑动过期时间（毫秒）。
     */
    public void setSlidingExpirationMilliseconds(Long milliseconds) {
        if(milliseconds != null && milliseconds.longValue() <= 0){
            throw  new IllegalArgumentException("MemoryCacheEntryOptions 滑动过期时间 slidingExpirationMilliseconds 必须为空大于 0。");
        }
        this.slidingExpiration = milliseconds;
    }

    /**
     * 设置相对于当前时间的绝对过期时间（单位：毫秒）
     * @param milliseconds  相对于当前时间的毫秒数。
     */
    public void setAbsoluteExpirationRelativeToNow(Long milliseconds){
        if (milliseconds != null && milliseconds.intValue() <= 0) {
            throw new IllegalArgumentException("MemoryCacheEntryOptions.setAbsoluteExpirationRelativeToNowMS 方法参数 milliseconds 必须大于 0 或为空。");
        }
        if (milliseconds == null) {
            this.absoluteExpiration = null;
        } else {
            LocalDateTime exp = LocalDateTime.now(ZoneOffset.UTC).plus(milliseconds, ChronoUnit.MILLIS);
            this.absoluteExpiration = exp;
        }
    }
}
