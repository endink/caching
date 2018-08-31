package com.labijie.caching.redis;

import com.labijie.caching.Guard;
import com.labijie.caching.ICacheManager;
import com.labijie.caching.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.JedisPool;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by ax03 on 2017/7/11.
 */
public abstract class RedisCacheManager implements ICacheManager {

    private static Logger log = null;

    public final String CLEAR_SCRIPT = "redis.call('FLUSHALL')";

    private JedisPool pool;
    private Set<HostAndPort> hostAndPort;

    private RedisCacheOptions options;

    public RedisCacheManager(RedisCacheOptions redisOptions) {
        Guard.ArgumentNotNull(redisOptions, "redisOptions");
        Guard.ArgumentNotNullOrEmptyString(redisOptions.getServer(), "redisOptions.server");
        this.options = redisOptions;
    }


    protected Logger getLogger(){
        if(log == null){
            synchronized (this) {
                if (log == null) {
                    log = LoggerFactory.getLogger(RedisCacheManager.class);
                }
            }
        }
        return log;
    }

    private Set<HostAndPort> getServers(){
        if(hostAndPort == null) {
            if (options == null || StringUtil.isNullOrWhiteSpace(options.getServer())) {
                throw new IllegalArgumentException("RedisCacheOptions 未配置或 server 未配置。");
            }
            String[] hostArray = options.getServer().split(",");
            if (hostArray.length == 0) {
                throw new IllegalArgumentException("RedisCacheOptions 没有配置 server。");
            }
            this.hostAndPort = Arrays.asList(hostArray).stream().map(this::getHostAndPort).collect(Collectors.toSet());
        }
        return this.hostAndPort;
    }

    private HostAndPort getHostAndPort(String serverString) {

        String[] socketString = serverString.split(":");
        if (socketString.length == 1) {
            return new HostAndPort(serverString, 6379);
        }
        if (socketString.length == 2) {
            Integer port = 0;
            try {
                port = Integer.parseInt(socketString[1]);
            } catch (NumberFormatException e) {

            }
            if (0 > port || port > 65535) {
                throw new IllegalArgumentException("RedisCacheOptions 配置的 server 端口号不正确或超出范围。");
            }
            return new HostAndPort(socketString[0], port);
        }
        throw new IllegalArgumentException("RedisCacheOptions 配置的 server 格式不正确，正确为 address:port。");
    }

    protected  RedisCacheOptions getOptions(){
        return this.options != null ? this.options :(this.options = new RedisCacheOptions());
    }

    private JedisPool getConnectionPool(){
        if(pool == null){
            synchronized (this){
                if(pool == null){
                    HostAndPort hp = this.getServers().stream().findFirst().get();
                    pool = new JedisPool(hp.getHost(),hp.getPort());
                }
            }
        }
        return pool;
    }

    protected <T extends JedisCommands> T createCommand(Class<T> c){
        JedisPool pool = this.getConnectionPool();
        switch (this.options.getMode()){
            case Gluster:
                return (T)new JedisCluster(this.hostAndPort, this.options.getTimeoutMills(), this.options.getPool());
            case Single:
            default:
                return (T)this.getConnectionPool().getResource();
        }
    }
}
