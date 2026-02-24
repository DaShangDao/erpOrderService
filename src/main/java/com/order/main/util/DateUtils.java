package com.order.main.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    /**
     * 根据天数偏移量获取 GMT+8 时区的时间字符串
     * @param daysOffset 天数偏移量（正数表示未来，负数表示过去，0表示当前）
     * @return 格式为 yyyy-MM-dd HH:mm:ss 的时间字符串
     */
    public static String getTimeByDayOffset(int daysOffset) {
        // 创建 GMT+8 时区
        ZoneId gmtPlus8 = ZoneId.of("GMT+8");

        // 获取当前 GMT+8 时区的时间并添加天数偏移
        ZonedDateTime dateTime = ZonedDateTime.now(gmtPlus8).plusDays(daysOffset);

        // 定义格式化器
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 格式化并返回结果
        return dateTime.format(formatter);
    }

    /**
     * 将时间戳（10位或13位）转换为 GMT+8 时区的格式化时间字符串
     * @param timestamp 时间戳（10位秒或13位毫秒）
     * @return 格式为 yyyy-MM-dd HH:mm:ss 的时间字符串
     */
    public static String formatTimestampToGMT8(long timestamp) {
        // 创建 GMT+8 时区
        ZoneId gmtPlus8 = ZoneId.of("GMT+8");

        Instant instant;

        // 通过字符串长度判断时间戳位数
        String timestampStr = String.valueOf(timestamp);
        if (timestampStr.length() == 10) {
            // 10位时间戳（秒）
            instant = Instant.ofEpochSecond(timestamp);
        } else if (timestampStr.length() == 13) {
            // 13位时间戳（毫秒）
            instant = Instant.ofEpochMilli(timestamp);
        } else {
            // 其他情况，默认按毫秒处理
            instant = Instant.ofEpochMilli(timestamp);
        }

        // 将 Instant 转换为 GMT+8 时区的 ZonedDateTime
        ZonedDateTime dateTime = ZonedDateTime.ofInstant(instant, gmtPlus8);

        // 定义格式化器
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 格式化并返回结果
        return dateTime.format(formatter);
    }

    /**
     * 将 yyyy-MM-dd HH:mm:ss 格式的时间字符串转换为13位时间戳（毫秒）
     * 默认按 GMT+8 时区解析
     * @param dateTimeStr 时间字符串，格式：yyyy-MM-dd HH:mm:ss
     * @return 13位时间戳（毫秒）
     */
    public static long parseDateTimeToTimestamp(String dateTimeStr) {
        // 创建 GMT+8 时区
        ZoneId gmtPlus8 = ZoneId.of("GMT+8");

        // 定义格式化器
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 解析时间字符串为 LocalDateTime，然后添加时区信息
        java.time.LocalDateTime localDateTime = java.time.LocalDateTime.parse(dateTimeStr, formatter);

        // 将 LocalDateTime 转换为 GMT+8 时区的 ZonedDateTime
        ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, gmtPlus8);

        // 转换为时间戳（毫秒）
        return zonedDateTime.toInstant().toEpochMilli();
    }
}
