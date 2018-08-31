package com.labijie.caching.redis;

/**
 * Created by ax03 on 2017/7/12.
 */
class CacheHashData {
    public String type;
    public String data;
    private boolean compressed;
    private String serializer;

    public CacheHashData(String type, String data) {
        this(type, data, null, false);
    }

    public CacheHashData(String type, String data, String serializer, boolean compressed) {
        this.type = type;
        this.data = data;
        this.compressed = compressed;
        this.serializer = serializer;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public boolean isCompressed() {
        return compressed;
    }

    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }

    public String getSerializer() {
        return serializer;
    }

    public void setSerializer(String serializer) {
        this.serializer = serializer;
    }
}
