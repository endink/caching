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
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by ax03 on 2017/3/6.
 */
public class MemoryCache implements AutoCloseable {
    private final ConcurrentHashMap<Object, CacheEntry> entries;
    private boolean closed;

    private static final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final Consumer<CacheEntry> setEntry;
    private final Consumer<CacheEntry> entryExpirationNotification;

    private Long expirationScanFrequencyMilliseconds;
    private LocalDateTime lastExpirationScan;

    public MemoryCache(MemoryCacheOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("MemoryCache 构造函数 options 不能为空。");
        }
        this.entries = new ConcurrentHashMap<>();
        setEntry = this::setEntry;
        this.entryExpirationNotification = this::entryExpired;
        this.expirationScanFrequencyMilliseconds = options.getScanFrequency();
        //最后一次扫描时间设置为当前时间。
        lastExpirationScan = LocalDateTime.now(ZoneOffset.UTC);
        if (options.isCompact()) {
            GcNotification.Register(this::doMemoryPreassureCollection, null);
        }
    }

    /**
     * MemoryCache 所有异步操作使用的统一线程池。
     *
     * @return
     */
    public static ExecutorService getExecutionThreadPool() {
        return threadPool;
    }

    private void checkClosed() {
        if (closed) {
            throw new RuntimeException("不能在 MemoryCache 关闭后进行操作。");
        }
    }

    /**
     * 获取当前缓存的大小。
     * @return
     */
    public int size() {
        return this.entries.size();
    }

    public CacheEntry createEntry(Object key) {
        checkClosed();
        //该缓存项并不会被添加到缓存队列，而是等待GC 收集时才会被添加。
        return new CacheEntry(
                key,
                this.setEntry,
                this.entryExpirationNotification
        );
    }

    /**
     * 根据缓存键获取缓存对象（注意不是获取缓存项 CacheEntry，而是真实缓存对象）。
     * @param key
     * @return
     */
    public Object getOrDefault(Object key, Object defaultValue) {
        if (key == null) {
            throw new IllegalArgumentException("MemoryCache getIfAbsent 调用 key 不能为空");
        }
        checkClosed();

        Object result = null;
        LocalDateTime utcNow = LocalDateTime.now(ZoneOffset.UTC);

        CacheEntry entry = this.entries.getOrDefault(key, null);
        if (entry != null) {
            // 由于使用惰性过期算法，首先去检查是否过期，过期直接移除。
            if (entry.checkExpired(utcNow) && entry.getEvictionReason() != EvictionReason.Replaced) {
                removeEntry(entry);
            } else {
                entry.setLastAccessed(utcNow);
                result = entry.getValue();

                // 当缓存项是在其他上下文创建时，需要复制过期令牌。
                entry.propagateOptions(CacheEntryHelper.getCurrent());
            }
        }
        startScanForExpiredItems();

        return result == null ? defaultValue : result;
    }

    public void remove(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("Memory 必须指定要移除的缓存键，参数: key。");
        }

        checkClosed();
        CacheEntry entry = this.entries.remove(key);
        if (entry != null) {
            entry.setExpired(EvictionReason.Removed);
            entry.invokeEvictionCallbacks();
        }

        startScanForExpiredItems();
    }

    public <T> T get(Object key) {
        return (T) this.getOrDefault(key, null);
    }

    public <T> T getOrCreate(Object key, Function<CacheEntry, T> factory) {
        Object result = this.get(key);
        if (result == null && factory != null) {
            CacheEntry entry = this.createEntry(key);
            result = factory.apply(entry);
            if(result != null) {
                entry.setValue(result);
                // 必须手动调用 close，而不能使用 try\final 结构，中间过程出错无法调用到 close 不会将缓存添加到缓存列表
                entry.close();
            }
        }
        return (T) result;
    }

    public <T> T set(Object key, T value, MemoryCacheEntryOptions options) {
        if (key == null) {
            throw new IllegalArgumentException("MemoryCache.set 调用 key 参数不能为空。");
        }
        if (value == null) {
            throw new IllegalArgumentException("MemoryCache.set 调用 value 参数不能为空。");
        }
        CacheEntry entry = this.createEntry(key);
        if (options != null) {
            MemoryCacheEntryOptions.configureCacheEntry(entry, options);
        }
        entry.setValue(value);
        entry.close();

        return value;
    }

    public <T> T set(Object key, T value, IChangeToken expirationToken) {
        if (expirationToken == null) {
            throw new IllegalArgumentException("MemoryCache.set 调用 expirationToken 参数不能为空。");
        }
        MemoryCacheEntryOptions options = new MemoryCacheEntryOptions();
        options.getExpirationTokens().add(expirationToken);
        return this.set(key, value, options);
    }

    public <T> T set(Object key, T value, LocalDateTime absoluteExpirationUtc) {
        MemoryCacheEntryOptions options = null;
        if(absoluteExpirationUtc != null) {
            options = new MemoryCacheEntryOptions();
            options.setAbsoluteExpiration(absoluteExpirationUtc);
        }
        return this.set(key, value, options);
    }

    public <T> T set(Object key, T value, Long slidingExpirationMilliseconds) {
        if (slidingExpirationMilliseconds != null && slidingExpirationMilliseconds <= 0) {
            throw new IllegalArgumentException("MemoryCache.set 调用 slidingExpirationMilliseconds 参数必须大于 0 或为空。");
        }
        MemoryCacheEntryOptions options = null;
        if (slidingExpirationMilliseconds != null) {
            options = new MemoryCacheEntryOptions();
            options.setSlidingExpirationMilliseconds(slidingExpirationMilliseconds);
        }
        return this.set(key, value, options);
    }

    //添加一个缓存对象到HASH表（当GC回收时可以自动添加），用于防止对象被意外收集（由于JVM GC过程无法 Hook，暂时无效，或许可以使用 finalize？）
    private void setEntry(CacheEntry entry) {
        if (closed) {
            return;
        }

        LocalDateTime utcNow = LocalDateTime.now(ZoneOffset.UTC);

        LocalDateTime absoluteExpiration = null;
        //先考虑相对过期时间，在考虑绝对过期时间。
        if (entry.getAbsoluteExpiration() != null) {
            absoluteExpiration = entry.getAbsoluteExpiration();
        }

        // 实体被添加的时间进行记录（LUA算法）
        entry.setLastAccessed(utcNow);

        CacheEntry priorEntry = this.entries.getOrDefault(entry.getKey(), null);
        if (priorEntry != null) {
            //已经存在缓存项，先把旧的项过期。
            priorEntry.setExpired(EvictionReason.Replaced);
        }

        if (!entry.checkExpired(utcNow)) {
            boolean entryAdded = false;

            if (priorEntry == null) {
                // 注意：只在没有存在键时候添加。
                CacheEntry inMap = this.entries.computeIfAbsent(entry.getKey(), k->entry);
                entryAdded = (inMap == entry); //同 一个引用表明已经被添加。
            } else {
                // 这里表示新的项没有被添加进去，可能 getOrDefault 之后有其他线程去动了缓存项。
                //发生这种情况时候看看新的项是不是我们添加进去的，通过 replace 确认一下
                entryAdded = this.entries.replace(entry.getKey(), priorEntry, entry);

                if (!entryAdded) {
                    // 确定有其他线程动过这个的缓存项了，尝试看看是不是过期了，不是过期以另外一个线程给进去的值稳准。
                    // 如果过期了要主要不要让他过期，所以 Absent 确保有值，我们正在做 set 啊，没值会很奇怪。
                    CacheEntry inMap = this.entries.computeIfAbsent(entry.getKey(), k->entry);
                    entryAdded = (inMap == entry);
                }
            }

            if (entryAdded) {
                entry.attachTokens();
            } else {
                entry.setExpired(EvictionReason.Replaced);
                entry.invokeEvictionCallbacks();
            }

            if (priorEntry != null) {
                priorEntry.invokeEvictionCallbacks();
            }
        } else {
            entry.invokeEvictionCallbacks();
            if (priorEntry != null) {
                this.removeEntry(priorEntry);
            }
        }
        startScanForExpiredItems();
    }

    private void entryExpired(CacheEntry entry) {
        //TODO: 应该考虑批量处理？
        removeEntry(entry);
        startScanForExpiredItems();
    }

    private void startScanForExpiredItems() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (this.lastExpirationScan.plus(this.expirationScanFrequencyMilliseconds, ChronoUnit.MILLIS).compareTo(now) < 0) {
            this.lastExpirationScan = now;
            final MemoryCache cache = this;
            MemoryCache.getExecutionThreadPool().execute(() -> {
                scanForExpiredItems(cache);
            });
        }
    }

    private static void scanForExpiredItems(MemoryCache cache) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        ArrayList<CacheEntry> toRemove = new ArrayList<>();
        cache.entries.values().stream().filter(entry -> entry.checkExpired(now)).forEach(e -> toRemove.add(e));

        toRemove.stream().forEach(cache::removeEntry);
    }

    private void removeEntry(CacheEntry entry) {
        if(this.entries.remove(entry.getKey(), entry)){
            entry.invokeEvictionCallbacks();
        }
    }

    /// 在内存紧张时通过调用此方法来回收内存，但是内存真正回收时间取决于下一次的GC.
    /// 回收 10% 条目以压缩内存。
    private boolean doMemoryPreassureCollection(Object state) {
        if (closed) {
            return false;
        }

        compact(0.10);

        return true;
    }

    /**
     * 指定一个百分比 (0.10 for 10%) 元素数（或者内存？）对缓存, 移除使用以下策略:
     * 1. 移除所有已过期的缓存项。
     * 2.  不同的 CacheItemPriority 按桶分装。
     * 3. 排列出“最不活跃”（最后一次使用时间距离现在最远）的项.
     * 可以考虑的策略： 最接近的绝对过期时间优先清理。
     * 可以考虑的策略： 最接近的滑动过期时间优先清理。
     * 可以考虑的策略： 占用内存最大的对象优先清理。
     * @param percentage 收缩百分比，1表示全部收缩。
     */
    public void compact(double percentage) {
        List<CacheEntry> entriesToRemove = new ArrayList<>();
        List<CacheEntry> lowPriEntries = new ArrayList<>();
        List<CacheEntry> normalPriEntries = new ArrayList<>();
        List<CacheEntry> highPriEntries = new ArrayList<>();

        //  按缓存项优先级和是否过期装桶。
        LocalDateTime utcNow = LocalDateTime.now(ZoneOffset.UTC);
        for (CacheEntry entry : this.entries.values()) {
            if (entry.checkExpired(utcNow)) {
                entriesToRemove.add(entry);
            } else {
                switch (entry.getPriority()) {
                    case Low:
                        lowPriEntries.add(entry);
                        break;
                    case Normal:
                        normalPriEntries.add(entry);
                        break;
                    case High:
                        highPriEntries.add(entry);
                        break;
                    case NeverRemove:
                        break;
                    default:
                        throw new RuntimeException("Not implemented: " + entry.getPriority());
                }
            }
        }

        int removalCountTarget = (int) (this.entries.size() * percentage);

        expirePriorityBucket(removalCountTarget, entriesToRemove, lowPriEntries);
        expirePriorityBucket(removalCountTarget, entriesToRemove, normalPriEntries);
        expirePriorityBucket(removalCountTarget, entriesToRemove, highPriEntries);

        entriesToRemove.forEach(this::removeEntry);
    }

    // 处理每个优先级桶的元素过期。
    private void expirePriorityBucket(int removalCountTarget, List<CacheEntry> entriesToRemove, List<CacheEntry> priorityEntries) {
        // 移除指标达不到不操作?或许可以改进
        if (removalCountTarget <= entriesToRemove.size()) {
            return;
        }

        //桶中的元素数量未达到移除指标，无脑全部移除。
        if (entriesToRemove.size() + priorityEntries.size() <= removalCountTarget) {
            for (CacheEntry entry : priorityEntries) {
                entry.setExpired(EvictionReason.Capacity);
            }
            entriesToRemove.addAll(priorityEntries);
            return;
        }

        //桶中的元素数量超出了移除指标，事实 LRU 算法（先排序，在移除）
        priorityEntries.sort(new Comparator<CacheEntry>() {
            @Override
            public int compare(CacheEntry o1, CacheEntry o2) {
                return o1.getLastAccessed().compareTo(o2.getLastAccessed());
            }
        });

        for (CacheEntry entry : priorityEntries) {
            entry.setExpired(EvictionReason.Capacity);
            entriesToRemove.add(entry);
            if (removalCountTarget <= entriesToRemove.size()) {
                break;
            }
        }
    }

    @Override
    public void close() throws Exception {
        if(!this.closed) {
            this.closed = true;
            this.entries.clear();
        }
    }
}
