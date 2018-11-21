package com.labijie.caching.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labijie.caching.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Anders Xiao
 * @date 2018-11-21
 */
public class JacksonCacheDataSerializer implements ICacheDataSerializer {
    private ObjectMapper jacksonMapper = null;

    private static final Logger LOGGER = LoggerFactory.getLogger(JacksonCacheDataSerializer.class);

    public JacksonCacheDataSerializer(){
        this.jacksonMapper = new ObjectMapper();
    }

    @Override
    public <T> T deserializeData(Class<T> type, String data, boolean gzipCompress) {
        if (data == null) {
            return null;
        }
        String content = data;
        try {
            if (gzipCompress) {
                content = StringUtil.gunzip(data);
            }
            return this.jacksonMapper.readValue(content, type);
        } catch (Exception ex) {
            LOGGER.error(String.format("Redis cache manager serialize fault ( compress:%s, class: %s )。", gzipCompress, type.getSimpleName()), ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String serializeData(Object data, boolean gzipCompress) {
        if (data == null) {
            return null;
        }
        try {
            String json = this.jacksonMapper.writeValueAsString(data);
            if (gzipCompress) {
                json = StringUtil.gzip(json);
            }
            return json;
        } catch (Exception ex) {
            LOGGER.error(String.format("Redis cache manager deserialize fault ( compress:%s, class: %s )。", gzipCompress, data.getClass().getSimpleName()));
            throw new RuntimeException(ex);
        }
    }
}
