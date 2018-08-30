/**
 * Created with IntelliJ IDEA.
 *
 * @author Anders Xiao
 * @date 2018-08-30
 */
package com.labijie.caching.memory;

import com.labijie.caching.*;

import java.io.Closeable;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 表示一个内存缓存项。
 * Created by sharp on 2017/3/5.
 */
public class CacheEntry implements AutoCloseable {
    private boolean added;

    private static final Consumer<Object> expirationCallback = CacheEntry::expirationTokensExpired;
    private Consumer<CacheEntry> notifyCacheOfExpiration;
    private Consumer<CacheEntry> notifyCacheEntryReleased;

    private EvictionReason evictionReason;
    private List<Closeable> expirationTokenRegistrations;
    private boolean isExpired;
    private List<IChangeToken> expirationTokens;
    private Object key;
    private LocalDateTime absoluteExpiration;
    private Long slidingExpirationMS;
    private LocalDateTime lastAccessed;
    private final Object lock = new Object();
    private List<PostEvictionCallbackRegistration> postEvictionCallbacks;
    private Object value;
    private AutoCloseable scope;
    private CacheItemPriority priority;

    /**
     * 创建内存缓存项的新实例。
     * @param key 缓存键
     * @param notifyCacheEntryReleased 缓存项失效时的回调通知的函数。
     * @param notifyCacheOfExpiration 缓存项过期时的回调通知的函数。
     */
    public CacheEntry(
            Object key,
            Consumer<CacheEntry> notifyCacheEntryReleased,
            Consumer<CacheEntry> notifyCacheOfExpiration) {
        if (key == null) {
            throw new IllegalArgumentException("CacheEntry 构造函数 key 不能为空。");
        }
        if (notifyCacheOfExpiration == null) {
            throw new IllegalArgumentException("CacheEntry 构造函数 notifyCacheOfExpiration 不能为空。");
        }
        if (notifyCacheEntryReleased == null) {
            throw new IllegalArgumentException("CacheEntry 构造函数 notifyCacheEntryReleased 不能为空。");
        }
        this.key = key;
        this.setPriority(CacheItemPriority.Normal);
        this.notifyCacheOfExpiration = notifyCacheOfExpiration;
        this.notifyCacheEntryReleased = notifyCacheEntryReleased;

        scope = CacheEntryHelper.EnterScope(this);

    }

    /**
     * 注册缓存项失效的回调方法。
     * @param callback 回调函数。
     * @param state 回调函数传递的状态对象。
     */
    public void RegisterPostEvictionCallback(
            IPostEvictionCallback callback,
            Object state) {
        if (callback == null) {
            throw new IllegalArgumentException("RegisterPostEvictionCallback 方法参数 callback 不能为空。");
        }

        this.getPostEvictionCallbacks().add(new PostEvictionCallbackRegistration(callback, state));
    }


    /**
     * 获取缓移除时的回调链列表。
     * @return
     */
    public List<PostEvictionCallbackRegistration> getPostEvictionCallbacks() {
        if (postEvictionCallbacks == null) {
            postEvictionCallbacks = new ArrayList<>();
        }
        return postEvictionCallbacks;
    }

    /**
     * 获取用于处理缓存过期的令牌。
     * @return
     */
    public List<IChangeToken> getExpirationTokens() {
        if (this.expirationTokens == null) {
            this.expirationTokens = new ArrayList<IChangeToken>();

        }
        return expirationTokens;
    }

    /**
     * 获取缓存项的优先级，该优先级在处理内存收缩时会影响缓存清理策略。
     * @return
     */
    public CacheItemPriority getPriority() {
        return priority;
    }

    /**
     * 设置缓存项的优先级，该优先级在处理内存收缩时会影响缓存清理策略。
     * @param priority
     */
    public void setPriority(CacheItemPriority priority) {
        this.priority = priority;
    }

    /**
     * 获取缓存项最后访问时间（用于 LRU 算法）。
     * @return
     */
    LocalDateTime getLastAccessed() {
        return lastAccessed;
    }

    /**
     * 设置缓存项最后访问时间（用于 LRU 算法）。
     * @return
     */
    void setLastAccessed(LocalDateTime lastAccessed) {
        this.lastAccessed = lastAccessed;
    }

    /**
     * 获取缓存项的绝对过期时间（Utc）。
     * @return
     */
    public LocalDateTime getAbsoluteExpiration() {
        return absoluteExpiration;
    }

    /**
     * 设置缓存项的绝对过期时间（Utc）。
     * @param absoluteExpiration
     */
    public void setAbsoluteExpiration(LocalDateTime absoluteExpiration) {
        this.absoluteExpiration = absoluteExpiration;
    }

    /**
     * 设置相对于当前时间的缓存项绝对过期时间（单位：毫秒）。
     * @param milliseconds 相对于当前时间的毫秒数。
     */
    public void setAbsoluteExpirationRelativeToNowMS(Long milliseconds) {
        if (milliseconds != null && milliseconds.intValue() <= 0) {
            throw new IllegalArgumentException("setAbsoluteExpirationRelativeToNowMS 方法参数 milliseconds 必须大于 0 或为空。");
        }
        if (milliseconds == null) {
            this.absoluteExpiration = null;
        } else {
            LocalDateTime time = LocalDateTime.now(ZoneOffset.UTC).plus(milliseconds, ChronoUnit.MILLIS);
            this.absoluteExpiration = time;
        }
    }

    /**
     * 获取缓存项的滑动过期时间（单位：毫秒）。
     * @return
     */
    public Long getSlidingExpirationMilliseconds() {
        return slidingExpirationMS;
    }

    /**
     * 设置缓存项的滑动过期时间（单位：毫秒）。
     * @param milliseconds
     */
    public void setSlidingExpirationMilliseconds(Long milliseconds) {
        if (milliseconds != null && milliseconds.intValue() <= 0) {
            throw new IllegalArgumentException("setSlidingExpirationMilliseconds 方法 milliseconds 必须大于 0 或为空。");
        }
        this.slidingExpirationMS = milliseconds;
    }

    /**
     * 获取缓存项的键。
     * @return
     */
    public Object getKey() {
        return this.key;
    }

    /**
     * 获取缓存项的值。
     * @return
     */
    public Object getValue() {
        return value;
    }

    /**
     *  设置缓存项的值。
     * @param value
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * 获取缓存移除的原因。
     * @return
     */
    EvictionReason getEvictionReason() {
        return this.evictionReason;
    }


    void setEvictionReason(EvictionReason reason) {
        this.evictionReason = reason;
    }

    private static void expirationTokensExpired(final Object obj) {
        MemoryCache.getExecutionThreadPool().execute(() -> {
            CacheEntry entry = (CacheEntry) obj;
            entry.setExpired(EvictionReason.TokenExpired);
            entry.notifyCacheOfExpiration.accept(entry);
        });
    }

    /**
     * 强制缓存项过期。
     * @param reason 过期原因。
     */
    public void setExpired(EvictionReason reason) {
        if (this.getEvictionReason() == EvictionReason.None) {
            this.setEvictionReason(reason);
        }
        this.isExpired = true;
        this.detachTokens();
    }

    /**
     * 检查缓存项是否过期。
     * @param now 判断过期的基准时间。
     * @return
     */
    public boolean checkExpired(LocalDateTime now) {
        return this.isExpired || checkForExpiredTime(now) || checkForExpiredTokens();
    }

    private boolean checkForExpiredTime(LocalDateTime now) {
        if (this.absoluteExpiration != null
                && this.absoluteExpiration.compareTo(now) <= 0) {
            setExpired(EvictionReason.Expired);
            return true;
        }

        if (this.slidingExpirationMS != null
                && (this.getLastAccessed().plus(this.slidingExpirationMS, ChronoUnit.MILLIS).compareTo(now) < 0)) {
            setExpired(EvictionReason.Expired);
            return true;
        }
        return false;
    }

    private boolean checkForExpiredTokens() {
        if (this.expirationTokens != null) {
            for (IChangeToken expiredToken : this.getExpirationTokens()) {
                if (expiredToken.hasChanged()) {
                    this.setExpired(EvictionReason.TokenExpired);
                    return true;
                }
            }
        }
        return false;
    }

    void attachTokens() {
        if (this.expirationTokens != null) {
            synchronized (this.lock) {
                for (IChangeToken expirationToken : this.expirationTokens) {
                    if (expirationToken.enableActiveChangeCallbacks()) {
                        if (this.expirationTokenRegistrations == null) {
                            this.expirationTokenRegistrations = new ArrayList<>(1);
                        }
                        Closeable registration = expirationToken.registerChangeCallback(expirationCallback, this);
                        if(registration != null) {
                            this.expirationTokenRegistrations.add(registration);
                        }
                    }
                }
            }
        }
    }

    private void detachTokens() {
        synchronized (lock) {
            List<Closeable> registrations = expirationTokenRegistrations;
            if (registrations != null) {
                expirationTokenRegistrations = null;
                //关闭一下。
                for (Closeable registration : registrations) {
                    try {
                        registration.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    void invokeEvictionCallbacks() {
        if (this.postEvictionCallbacks != null) {
            final CacheEntry entry = this;
            MemoryCache.getExecutionThreadPool().execute(() -> invokeCallbacks(entry));
        }
    }

    private static void invokeCallbacks(CacheEntry entry) {
        AtomicReference<List<PostEvictionCallbackRegistration>> atom = new AtomicReference<>();
        atom.set(entry.postEvictionCallbacks);

        //在进行一次回调以后需要清理。
        List<PostEvictionCallbackRegistration> callbackRegistrations = atom.getAndUpdate(v -> v = null);
        if (callbackRegistrations == null) {
            return;
        }
        for (PostEvictionCallbackRegistration registration : callbackRegistrations) {
            IPostEvictionCallback callback = registration.getGetEvictionCallback();
            if (callback != null) {
                try {
                    callback.callback(entry.getKey(), entry.getValue(), entry.getEvictionReason(), registration.getState());
                } catch (Throwable e) {

                }
            }
        }
    }

    public void close(){
        if (!added) {
            added = true;
            try {
                scope.close();
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            notifyCacheEntryReleased.accept(this);
            //多线程下，可能会通过子线程调用close 因此，需要处理线程栈，要求 CacheEntry 总是成对的 create 和 close 调用。
            //可以解决在 create 之后发起其他线程调用后状态保持问题。
            propagateOptions(CacheEntryHelper.getCurrent());
        }
    }

    /**
     * 当创建了 Entry 还没有添加到缓存列表时发生了GC，应该主动添加。
     */
    @Override
    protected void finalize() {
        this.close();
    }

    void propagateOptions(CacheEntry parent) {
        if (parent == null) {
            return;
        }

        // 复制 expiration tokens 和  AbsoluteExpiration 到缓存项层次.
        // 无需关心是否已经被缓存 Token 总是和缓存项关联。
        if (expirationTokens != null) {
            synchronized (lock) {
                synchronized (parent.lock) {
                    for (IChangeToken expirationToken : expirationTokens) {
                        parent.getExpirationTokens().add(expirationToken);
                    }
                }
            }
        }

        if (absoluteExpiration != null) {
            if (parent.absoluteExpiration == null || absoluteExpiration.compareTo(parent.absoluteExpiration) < 0) {
                parent.absoluteExpiration = absoluteExpiration;
            }
        }
    }

    //处理引用比较提高性能。
    @Override
    public boolean equals(Object obj2){
        return this == obj2;
    }
}
