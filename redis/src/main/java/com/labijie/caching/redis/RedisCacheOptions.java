package com.labijie.caching.redis;

import com.labijie.caching.StringUtil;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

/**
 * Created by ax03 on 2017/7/11.
 */

public class RedisCacheOptions {
    private String server;
    private RedisMode mode;
    private JedisPoolConfig pool;
    private int timeoutMills;
    private boolean useGzip;
    private String serializer;
    private String password;
    private Integer database;

    public String getSerializer() {
        return StringUtil.isNullOrWhiteSpace(serializer) ? "json" : serializer.trim();
    }

    public void setSerializer(String serializer) {
        this.serializer = serializer;
    }

    public boolean isUseGzip() {
        return useGzip;
    }

    public void setUseGzip(boolean useGzip) {
        this.useGzip = useGzip;
    }


    public int getTimeoutMills() {
        return timeoutMills;
    }

    public void setTimeoutMills(int timeoutMills) {
        this.timeoutMills = timeoutMills;
    }

    /**
     * 获取连接池配置。
     * @return
     */
    public JedisPoolConfig getPool() {
        return pool == null ? (pool = new JedisPoolConfig()) : pool;
    }

    /**
     * 设置连接池配置。
     * @param pool
     */
    public void setPool(JedisPoolConfig pool) {
        this.pool = pool;
    }

    public RedisMode getMode() {
        return (mode == null) ? (mode =RedisMode.Single) : mode;
    }

    public void setMode(RedisMode mode) {
        this.mode = mode;
    }


    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getDatabase() {
        return Math.max(Protocol.DEFAULT_DATABASE, this.database);
    }

    public void setDatabase(Integer database) {
        this.database =  database;
    }
}
