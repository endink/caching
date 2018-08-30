/**
 * Created with IntelliJ IDEA.
 *
 * @author Anders Xiao
 * @date 2018-08-30
 */
package com.labijie.caching;
/**
 * Created with IntelliJ IDEA.
 *
 * @author Anders Xiao
 * @date 2018-08-30
 */
public final class Guard {
    private Guard(){}

    public static void ArgumentNullOrWhiteSpaceString(String argumentValue, String argumentName) {
        Guard.ArgumentNotNullOrEmptyString(argumentValue, argumentName, true);
    }

    public static void ArgumentNotNullOrEmptyString(String argumentValue, String argumentName) {
        Guard.ArgumentNotNullOrEmptyString(argumentValue, argumentName, false);
    }

    private static void ArgumentNotNullOrEmptyString(String argumentValue, String argumentName, boolean trimString) {
        if ((trimString && StringUtil.isNullOrWhiteSpace(argumentValue)) || (!trimString && StringUtil.isNullOrEmpty(argumentValue)))
        {
            throw new IllegalArgumentException(String.format("参数 %s 不能为空或空串。", argumentName));
        }
    }

    /**
     * Checks an argument to ensure it isn't null
     *
     * @param argumentValue
     * @param argumentName
     */
    public static void ArgumentNotNull(Object argumentValue, String argumentName) {
        if (argumentValue == null)
            throw new IllegalArgumentException(String.format("参数 %s 不能为空。", argumentName));
    }


    public static void InSecondRange(int data, String argumentName) {
        if (data > 59 || data < 0) {
            throw new IllegalArgumentException(String.format("参数 %s 表示秒数，取值必须在 0 - 59 之间。", argumentName));
        }
    }

    public static void InMonthRange(int data, String argumentName) {
        if (data > 12 || data < 1) {
            throw new IllegalArgumentException(String.format("参数 %s 表示月份，取值必须在 1 - 12 之间。", argumentName));
        }
    }

    public static void InMinuteRange(int data, String argumentName) {
        if (data > 59 || data < 0) {
            throw new IllegalArgumentException(String.format("参数 %s 表示分钟，取值必须在 0 - 59 之间。", argumentName));
        }
    }

    public static void InHourRange(int data, String argumentName) {
        if (data > 23 || data < 0) {
            throw new IllegalArgumentException(String.format("参数 %s 表示小时，取值必须在 0 - 59 之间。", argumentName));
        }
    }

    public static void InMonthDayRange(int data, String argumentName) {
        if (data > 31 || data < 1) {
            throw new IllegalArgumentException(String.format("参数 %s 表示每月的日期，取值必须在 1 - 31 之间。", argumentName));
        }
    }
}