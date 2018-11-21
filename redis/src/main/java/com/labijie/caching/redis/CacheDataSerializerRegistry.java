package com.labijie.caching.redis;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Anders Xiao
 * @date 2018-11-21
 */
public class CacheDataSerializerRegistry {
    private HashMap<String, ICacheDataSerializer> serializers;

    public final static CacheDataSerializerRegistry DEFAULT = new CacheDataSerializerRegistry();

    private CacheDataSerializerRegistry(){
        this.serializers = new HashMap();
        this.registerSerializer("json", new JacksonCacheDataSerializer());
    }

    public ICacheDataSerializer getSerializer(String serializerName) {
        ICacheDataSerializer ser = this.serializers.getOrDefault(serializerName, null);
        if(ser == null){
            throw new RuntimeException("Cant find cache data serializer with name '"+ serializerName +"'");
        }
        return ser;
    }

    public void registerSerializer(String serializerName, ICacheDataSerializer serializer){
        this.serializers.put(serializerName, serializer);
    }
}
