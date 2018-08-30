/**
 * Created with IntelliJ IDEA.
 *
 * @author Anders Xiao
 * @date 2018-08-30
 */
package com.labijie.caching;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * 表示一个监视变化的令牌（当变化发生时可以使缓存失效）。
 * Created by sharp on 2017/3/5.
 */
public interface IChangeToken {
    /**
     * 指示令牌是否发生变化, 发生变化将导致缓存失效。
     * @return
     */
    boolean hasChanged();

    /**
     * 是否允许注册一个变化追踪对象，并返回一个可释放的资源。
     * @return
     */
    boolean enableActiveChangeCallbacks();

    /**
     * 注册一个变化回掉函数，当变化发生时对会进行回调。
     * @param callback 缓存系统传入的回调函数。
     * @param state 回掉时的状态参数（实际为 CacheEntry）。
     * @return 返回一个可回”关闭“对象来释放占用的资源，缓存失效时将调用 close 方法。
     */
    default Closeable registerChangeCallback(final Consumer<Object> callback, final Object state){
        return new Closeable() {
            @Override
            public void close() throws IOException {
                callback.accept(state);
            }
        };
    }
}
