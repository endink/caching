
package com.labijie.caching.redis;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Anders Xiao
 * @date 2018-11-21
 */
public interface ICacheDataSerializer {
    <T> T deserializeData(Class<T> type, String data, boolean gzipCompress);
    String serializeData(Object data, boolean gzipCompress);
}
