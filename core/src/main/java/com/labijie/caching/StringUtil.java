/**
 * Created with IntelliJ IDEA.
 *
 * @author Anders Xiao
 * @date 2018-08-30
 */
package com.labijie.caching;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


public final class StringUtil {
    private StringUtil(){}

    /**
     * 判断一个字符串是否是 null 、空串或仅包含空格的字符串。
     * @param value
     * @return
     */
    public static boolean isNullOrWhiteSpace(String value){
        if(value == null){
            return true;
        }
        value = value.trim();
        return value.compareTo("") == 0;
    }

    /**
     * 判断一个字符串是否是 null 或空串。
     * @param value
     * @return
     */
    public static boolean isNullOrEmpty(String value){
        if(value == null){
            return true;
        }
        return value.compareTo("") == 0;
    }

    public static Optional<Long> tryParseLong(String longValue){
        try {
            return Optional.ofNullable(Long.parseLong(longValue));
        }
        catch (NumberFormatException e){
            return Optional.ofNullable(null);
        }
    }

    public static Optional<Integer> tryParseInt(String longValue){
        try {
            return Optional.ofNullable(Integer.parseInt(longValue));
        }
        catch (NumberFormatException e){
            return Optional.ofNullable(null);
        }
    }

    public static Optional<Boolean> tryParseBoolean(String booleanValue){
        if(booleanValue != null && (booleanValue.equalsIgnoreCase("true") || booleanValue.equalsIgnoreCase("false"))) {
            return Optional.ofNullable(Boolean.parseBoolean(booleanValue));
        }
        return Optional.ofNullable(null);
    }

    public static String gunzip(String compressedStr) throws IOException {
        if(compressedStr==null){
            return null;
        }

        byte[] compressed = Base64.getDecoder().decode(compressedStr);
        try(ByteArrayOutputStream out= new ByteArrayOutputStream()) {
            try (ByteArrayInputStream in = new ByteArrayInputStream(compressed)) {
                try (GZIPInputStream ginzip = new GZIPInputStream(in)) {

                    byte[] buffer = new byte[1024];
                    int offset = -1;
                    while ((offset = ginzip.read(buffer)) != -1) {
                        out.write(buffer, 0, offset);
                    }
                    return out.toString("UTF-8");
                }
            }
        }
    }

    public static String gzip(String content) throws IOException {
        if (isNullOrWhiteSpace(content)) {
            return content;
        }

        try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
                gzip.write(content.getBytes("UTF-8"));
            }
            return Base64.getEncoder().encodeToString(out.toByteArray());
        }
    }
}