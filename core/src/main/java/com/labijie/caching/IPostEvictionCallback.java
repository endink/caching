/**
 * Created with IntelliJ IDEA.
 *
 * @author Anders Xiao
 * @date 2018-08-30
 */
package com.labijie.caching;

@FunctionalInterface
public interface IPostEvictionCallback {
    void callback(Object key, Object value, EvictionReason reason, Object state);
}
