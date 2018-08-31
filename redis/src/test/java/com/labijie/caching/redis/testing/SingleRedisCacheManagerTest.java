package com.labijie.caching.redis.testing;

import com.labijie.caching.redis.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SingleRedisCacheManagerTest {
    private SingleRedisCacheManager cache;

    @Before
    public void before() throws Exception {
        RedisCacheOptions options = new RedisCacheOptions();
        options.setServer("127.0.0.1:6379");
        this.cache = new SingleRedisCacheManager(options);
    }

    @After
    public void after() throws Exception {
        this.cache.clear();
    }

    @Test
    public void loopTest() throws Exception {
        String data = "TTTTTTTT";
        cache.set("loop", data, 3000L, "unit-test", true);
        for (int i=0;i<5;i++) {
            Object result = cache.get("loop", "unit-test");
            Thread.sleep(1000L);
            Assert.assertTrue("",  result.equals(data));
        }
    }

    /**
     * Method: get(String key, String region)
     */
    @Test
    public void testGet() throws Exception {

        Assert.assertNull("当值不存在时 get 应为 null", cache.get("a", "b"));
        Assert.assertNull("当值不存在时 get 应为 null", cache.get("b", "a"));
        Assert.assertNull("当值不存在时 get 应为 null", cache.get("a", ""));
        Assert.assertNull("当值不存在时 get 应为 null", cache.get("a", (String)null));

        Integer val = Integer.MAX_VALUE;
        cache.set("a", val, null, "b", false);
        Assert.assertEquals("get 方法取到的值和 set 放入的值不一致。", val, cache.get("a", "b"));
    }

    /**
     * Method: set(String key, Object data, Long timeoutMilliseconds, String region, boolean useSlidingExpiration)
     */
    @Test
    public void testSet() throws Exception {
        Long val = 100L;
        cache.set("a", val, null, "region1", false);
        cache.set("b", val, 5000L, "region2", false);
        cache.set("c", val, 5000L, null, true);
        cache.set("d", val, null, null, true);
        cache.set("e", val, null, "", true);

        Assert.assertEquals("get 方法取到的值和 set 放入的值不一致。", val, cache.get("a", "region1"));
        Assert.assertEquals("get 方法取到的值和 set 放入的值不一致。", val, cache.get("b", "region2"));
        Assert.assertEquals("get 方法取到的值和 set 放入的值不一致。", val, cache.get("c", (String)null));
        Assert.assertEquals("get 方法取到的值和 set 放入的值不一致。", val, cache.get("d", (String)null));
        Assert.assertEquals("get 方法取到的值和 set 放入的值不一致。", val, cache.get("e", ""));
    }

    /**
     * Method: remove(String key, String region)
     */
    @Test
    public void testRemove() throws Exception {
        Long val = 33322L;
        cache.set("a", val, null, "region1", false);
        cache.set("b", val, 5000L, "region2", false);
        cache.set("c", val, 5000L, null, true);
        cache.set("d", val, null, null, true);
        cache.set("e", val, null, "", true);

        cache.remove("a", "region1");
        cache.remove("b", "region2");
        cache.remove("c", null);
        cache.remove("d", null);
        cache.remove("e", "");

        Assert.assertNull("remove 方法未生效。", cache.get("a", "region1"));
        Assert.assertNull("remove 方法未生效。", cache.get("b", "region2"));
        Assert.assertNull("remove 方法未生效。", cache.get("c", (String)null));
        Assert.assertNull("remove 方法未生效。", cache.get("d", (String)null));
        Assert.assertNull("remove 方法未生效。", cache.get("e", ""));
    }


    /**
     * Method: clearRegion(String region)
     */
    @Test
    public void testClearRegion() throws Exception {
        Long val = 3333L;
        cache.set("a", val, null, "region1", false);
        cache.set("b", val, 5000L, "region2", false);
        cache.set("c", val, 5000L, null, true);
        cache.set("d", val, null, null, true);
        cache.set("e", val, null, "", true);
        cache.set("f", val, 5000L, "region3", false);

        cache.clearRegion("region1");
        cache.clearRegion("region2");

        Assert.assertNull("clearRegion 方法未生效。", cache.get("a", "region1"));
        Assert.assertNull("clearRegion 方法未生效。", cache.get("b", "region2"));
        Assert.assertNotNull("clearRegion 清除了多余的区域。", cache.get("c", (String)null));
        Assert.assertNotNull("clearRegion 清除了多余的区域。", cache.get("d", (String)null));
        Assert.assertNotNull("clearRegion 清除了多余的区域。", cache.get("e", ""));
        Assert.assertNotNull("clearRegion 清除了多余的区域。", cache.get("f", "region3"));
    }

    /**
     * Method: clear()
     */
    @Test
    public void testClear() throws Exception {
        Long val = 3333L;
        cache.set("a", val, null, "region1", false);
        cache.set("b", val, 5000L, "region2", false);
        cache.set("c", val, 5000L, null, true);
        cache.set("d", val, null, null, true);
        cache.set("e", val, null, "", true);

        cache.clear();

        Assert.assertNull("clear 方法未生效。", cache.get("a", "region1"));
        Assert.assertNull("clear 方法未生效。", cache.get("b", "region2"));
        Assert.assertNull("clear 方法未生效。", cache.get("c", (String)null));
        Assert.assertNull("clear 方法未生效。", cache.get("d", (String)null));
        Assert.assertNull("clear 方法未生效。", cache.get("e", ""));
    }


} 