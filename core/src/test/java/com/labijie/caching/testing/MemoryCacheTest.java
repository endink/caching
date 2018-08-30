package com.labijie.caching.testing;

import com.labijie.caching.IChangeToken;
import com.labijie.caching.memory.CacheEntry;
import com.labijie.caching.memory.MemoryCache;
import com.labijie.caching.memory.MemoryCacheEntryOptions;
import com.labijie.caching.memory.MemoryCacheOptions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.function.Consumer;

public class MemoryCacheTest {
    private MemoryCache memoryCache = null;

    @Before
    public void before() throws Exception {
        memoryCache = new MemoryCache(new MemoryCacheOptions());
    }

    @After
    public void after() throws Exception {
        memoryCache.close();
    }


    /**
     * Method: size()
     */
    @Test
    public void testSize() throws Exception {
        Assert.assertEquals("创建 memoryCache 新实例后数量不正确。", 0, memoryCache.size());

        LocalDateTime dt = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(2);

        memoryCache.set("cc", new Object(), dt);
        Assert.assertEquals("添加第 1 个元素后数量不正确。", 1, memoryCache.size());

        memoryCache.set("dd", new Object(), dt);
        Assert.assertEquals("添加第 2 个元素后数量不正确。", 2, memoryCache.size());

        memoryCache.remove("cc");
        Assert.assertEquals("移除第 1 个元素后数量不正确。", 1, memoryCache.size());

        memoryCache.remove("dd");
        Assert.assertEquals("移除第 2 个元素后数量不正确。", 0, memoryCache.size());
    }

    /**
     * Method: createEntry(Object key)
     */
    @Test
    public void testCreateEntry() throws Exception {
        CacheEntry entry = memoryCache.createEntry("cc");
        Assert.assertNotNull("创建 createEntry 返回了空值。", entry);
    }

    /**
     * Method: getOrDefault(Object key, Object defaultValue)
     */
    @Test
    public void testGetOrDefault() throws Exception {
        Object value = memoryCache.getOrDefault("1", null);
        Assert.assertNull("不存在的键调用 getOrDefault 应该返回 null。", value);

        memoryCache.set("2", new Object(), 3000L);
        Object value2 = memoryCache.getOrDefault("2", null);
        Assert.assertNotNull("存在的键调用 getOrDefault 返回了 null。", value2);

        Object newObj = new Object();
        Object value3 = memoryCache.getOrDefault("3", newObj);
        Assert.assertTrue("getOrDefault 新建 key 时返回了不同的引用对象。", value3 == newObj);
    }

    /**
     * Method: remove(Object key)
     */
    @Test
    public void testRemove() throws Exception {
        memoryCache.set("a", new Object(), 3000L);
        Object obj = memoryCache.get("a");
        Assert.assertNotNull("set 后再取值返回了 null。", obj);

        memoryCache.remove("a");
        Object obj2 = memoryCache.get("a");
        Assert.assertTrue("删除 key 后存在。。", obj2 == null);
    }

    /**
     * Method: set(Object key, T value, MemoryCacheEntryOptions options)
     */
    @Test
    public void testSetForKeyValueOptions() throws Exception {
        MemoryCacheEntryOptions options = new MemoryCacheEntryOptions();
        options.setSlidingExpirationMilliseconds(500L);

        Object value = new Object();
        memoryCache.set("a", value, options);
        Thread.sleep(1000);
        Object v = memoryCache.get("a");

        Assert.assertTrue("set 测试滑动过期时间无效。", v == null);
    }

    /**
     * Method: set(Object key, T value, IChangeToken expirationToken)
     */
    @Test
    public void testSetForKeyValueExpirationToken() throws Exception {
        Object value = new Object();
        memoryCache.set("a", value, new IChangeToken() {
            @Override
            public boolean hasChanged() {
                return true;
            }

            @Override
            public boolean enableActiveChangeCallbacks() {
                return true;
            }

            @Override
            public Closeable registerChangeCallback(final Consumer<Object> callback, Object state) {
                return new Closeable() {
                    @Override
                    public void close() throws IOException {
                        callback.accept(state);
                        System.out.println("callback invoked");
                    }
                };
            }
        });
        Thread.sleep(1000);
        Object v = memoryCache.get("a");

        Assert.assertTrue("set 测试滑动过期时间无效。", v == null);
    }

    /**
     * Method: set(Object key, T value, Date absoluteExpiration)
     */
    @Test
    public void testSetForKeyValueAbsoluteExpiration() throws Exception {
        LocalDateTime time = LocalDateTime.now(ZoneOffset.UTC ).plusSeconds(1);
        Object value = new Object();
        memoryCache.set("a", value, time);
        Object existed = memoryCache.get("a");
        Assert.assertNotNull("set 使用绝对过期时间缓存意外过期。", existed);
        Thread.sleep(2000);
        existed = memoryCache.get("a");
        Assert.assertNull("set 使用绝对过期时间缓存项未过期。", existed);
    }

    /**
     * Method: set(Object key, T value, Long slidingExpirationMilliseconds)
     */
    @Test
    public void testSetForKeyValueSlidingExpirationMilliseconds() throws Exception {
        Object value = new Object();
        memoryCache.set("a", value, 2000L);
        Thread.sleep(1000);
        Object existed = memoryCache.get("a");
        Assert.assertTrue("set 使用滑动过期时间缓存项失效。", value == existed);

        Thread.sleep(1000);
        existed = memoryCache.get("a");
        Assert.assertTrue("set 使用滑动过期时间缓存项失效。", value == existed);

        Thread.sleep(1000);
        existed = memoryCache.get("a");
        Assert.assertTrue("set 使用滑动过期时间缓存项失效。", value == existed);

        Thread.sleep(3000);
        existed = memoryCache.get("a");
        Assert.assertTrue("set 使用滑动过期时间缓存项未失效。", existed == null);
    }

    /**
     * Method: compact(double percentage)
     */
    @Test
    public void testCompact() throws Exception {
        memoryCache.set("1", new Object(), 10 * 60 * 1000L);
        memoryCache.set("2", new Object(), 10 * 60 * 1000L);
        memoryCache.set("3", new Object(), 10 * 60 * 1000L);
        memoryCache.set("4", new Object(), 10 * 60 * 1000L);
        memoryCache.set("5", new Object(), 10 * 60 * 1000L);
        memoryCache.set("6", new Object(), 10 * 60 * 1000L);
        memoryCache.set("7", new Object(), 10 * 60 * 1000L);
        memoryCache.set("8", new Object(), 10 * 60 * 1000L);
        memoryCache.set("9", new Object(), 10 * 60 * 1000L);
        memoryCache.set("10", new Object(), 10 * 60 * 1000L);

        memoryCache.compact(0.3);

        Assert.assertEquals("compact 收缩内存未生效。", 7, memoryCache.size());
    }


    /**
     * Method: checkClosed()
     */
    @Test(expected = RuntimeException.class)
    public void testCheckClosed() throws Exception {
        memoryCache.close();
        memoryCache.set("a", new Object(), 1000L);
    }

    /**
     * Method: setEntry(CacheEntry entry)
     */
    @Test
    public void testSetEntry() throws Exception {
        CacheEntry entry = memoryCache.createEntry("33");
        Assert.assertEquals("createEntry 不应该自动添加的缓存项。", 0, memoryCache.size());
        try {
            Method method = MemoryCache.class.getDeclaredMethod("setEntry", CacheEntry.class);
            method.setAccessible(true);
            method.invoke(memoryCache, entry);

            Assert.assertEquals("setEntry(CacheEntry entry)  缓存项未被添加到列表。", 1, memoryCache.size());

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }


    /**
     * Method: scanForExpiredItems(MemoryCache cache)
     */
    @Test
    public void testScanForExpiredItems() throws Exception {

        try {
            Method method = MemoryCache.class.getDeclaredMethod("scanForExpiredItems", MemoryCache.class);
            method.setAccessible(true);

            add10ItemToCacheManager();
            Thread.sleep(2001L);
            method.invoke(null, memoryCache);
            Assert.assertEquals("scanForExpiredItems 未把缓存项过期。", 8, memoryCache.size());


            memoryCache = new MemoryCache(new MemoryCacheOptions());
            add10ItemToCacheManager();
            Thread.sleep(5001L);
            method.invoke(null, memoryCache);
            Assert.assertEquals("scanForExpiredItems 未把缓存项过期。", 5, memoryCache.size());

            memoryCache = new MemoryCache(new MemoryCacheOptions());
            add10ItemToCacheManager();
            Thread.sleep(9001L);
            method.invoke(null, memoryCache);
            Assert.assertEquals("scanForExpiredItems 未把缓存项过期。", 1, memoryCache.size());


        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    private void add10ItemToCacheManager() {
        memoryCache.set("1", new Object(), 1000L);
        memoryCache.set("2", new Object(), 2000L);
        memoryCache.set("3", new Object(), 3000L);
        memoryCache.set("4", new Object(), 4000L);
        memoryCache.set("5", new Object(), 5000L);
        memoryCache.set("6", new Object(), 6000L);
        memoryCache.set("7", new Object(), 7000L);
        memoryCache.set("8", new Object(), 8000L);
        memoryCache.set("9", new Object(), 9000L);
        memoryCache.set("10", new Object(), 10000L);
    }


} 
