package com.gznb.mozart.commons.caching.memory;

import com.labijie.caching.memory.MemoryCacheManager;
import com.labijie.caching.memory.MemoryCacheOptions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class MemoryCacheManagerTest {

    private MemoryCacheManager memoryCache;

    @Before
    public void before() throws Exception {
        this.memoryCache = new MemoryCacheManager(new MemoryCacheOptions());
    }

    @After
    public void after() throws Exception {
        this.memoryCache.clear();
    }

    /**
     * Method: get(String key, String region)
     */
    @Test
    public void testGet() throws Exception {

        Assert.assertNull("当值不存在时 get 应为 null", memoryCache.get("a", "b"));
        Assert.assertNull("当值不存在时 get 应为 null", memoryCache.get("b", "a"));
        Assert.assertNull("当值不存在时 get 应为 null", memoryCache.get("a", ""));
        Assert.assertNull("当值不存在时 get 应为 null", memoryCache.get("a", (String)null));

        Object val = new Object();
        memoryCache.set("a", val, null, "b", false);
        Assert.assertEquals("get 方法取到的值和 set 放入的值不一致。", val, memoryCache.get("a", "b"));
    }

    /**
     * Method: set(String key, Object data, Long timeoutMilliseconds, String region, boolean useSlidingExpiration)
     */
    @Test
    public void testSet() throws Exception {
        Object val = new Object();
        memoryCache.set("a", val, null, "region1", false);
        memoryCache.set("b", val, 5000L, "region2", false);
        memoryCache.set("c", val, 5000L, null, true);
        memoryCache.set("d", val, null, null, true);
        memoryCache.set("e", val, null, "", true);

        Assert.assertEquals("get 方法取到的值和 set 放入的值不一致。", val, memoryCache.get("a", "region1"));
        Assert.assertEquals("get 方法取到的值和 set 放入的值不一致。", val, memoryCache.get("b", "region2"));
        Assert.assertEquals("get 方法取到的值和 set 放入的值不一致。", val, memoryCache.get("c", (String)null));
        Assert.assertEquals("get 方法取到的值和 set 放入的值不一致。", val, memoryCache.get("d", (String)null));
        Assert.assertEquals("get 方法取到的值和 set 放入的值不一致。", val, memoryCache.get("e", ""));
    }

    /**
     * Method: remove(String key, String region)
     */
    @Test
    public void testRemove() throws Exception {
        Object val = new Object();
        memoryCache.set("a", val, null, "region1", false);
        memoryCache.set("b", val, 5000L, "region2", false);
        memoryCache.set("c", val, 5000L, null, true);
        memoryCache.set("d", val, null, null, true);
        memoryCache.set("e", val, null, "", true);

        memoryCache.remove("a", "region1");
        memoryCache.remove("b", "region2");
        memoryCache.remove("c", null);
        memoryCache.remove("d", null);
        memoryCache.remove("e", "");

        Assert.assertNull("remove 方法未生效。", memoryCache.get("a", "region1"));
        Assert.assertNull("remove 方法未生效。", memoryCache.get("b", "region2"));
        Assert.assertNull("remove 方法未生效。", memoryCache.get("c", (String)null));
        Assert.assertNull("remove 方法未生效。", memoryCache.get("d", (String)null));
        Assert.assertNull("remove 方法未生效。", memoryCache.get("e", ""));
    }


    /**
     * Method: clearRegion(String region)
     */
    @Test
    public void testClearRegion() throws Exception {
        Object val = new Object();
        memoryCache.set("a", val, null, "region1", false);
        memoryCache.set("b", val, 5000L, "region2", false);
        memoryCache.set("c", val, 5000L, null, true);
        memoryCache.set("d", val, null, null, true);
        memoryCache.set("e", val, null, "", true);
        memoryCache.set("f", val, 5000L, "region3", false);

        memoryCache.clearRegion("region1");
        memoryCache.clearRegion("region2");

        Assert.assertNull("clearRegion 方法未生效。", memoryCache.get("a", "region1"));
        Assert.assertNull("clearRegion 方法未生效。", memoryCache.get("b", "region2"));
        Assert.assertNotNull("clearRegion 清除了多余的区域。", memoryCache.get("c", (String)null));
        Assert.assertNotNull("clearRegion 清除了多余的区域。", memoryCache.get("d", (String)null));
        Assert.assertNotNull("clearRegion 清除了多余的区域。", memoryCache.get("e", ""));
        Assert.assertNotNull("clearRegion 清除了多余的区域。", memoryCache.get("f", "region3"));
    }

    /**
     * Method: clear()
     */
    @Test
    public void testClear() throws Exception {
        Object val = new Object();
        memoryCache.set("a", val, null, "region1", false);
        memoryCache.set("b", val, 5000L, "region2", false);
        memoryCache.set("c", val, 5000L, null, true);
        memoryCache.set("d", val, null, null, true);
        memoryCache.set("e", val, null, "", true);

        memoryCache.clear();

        Assert.assertNull("clear 方法未生效。", memoryCache.get("a", "region1"));
        Assert.assertNull("clear 方法未生效。", memoryCache.get("b", "region2"));
        Assert.assertNull("clear 方法未生效。", memoryCache.get("c", (String)null));
        Assert.assertNull("clear 方法未生效。", memoryCache.get("d", (String)null));
        Assert.assertNull("clear 方法未生效。", memoryCache.get("e", ""));
    }

    /**
     * Method: getRegionNameFormFullKey(String fullKey)
     */
    @Test
    public void testGetRegionNameFormFullKey() throws Exception {

        try {
            Method method = MemoryCacheManager.class.getDeclaredMethod("getRegionNameFormFullKey", String.class);
            method.setAccessible(true);
            String regionName = (String) method.invoke(memoryCache, "a|b");
            Assert.assertEquals("a", regionName);

        } catch (NoSuchMethodException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }

    }

} 
