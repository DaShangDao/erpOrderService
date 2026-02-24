package com.order.main.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * 时间格式转换工具类
 * 支持多种常见时间格式转换为时间戳
 */
public class TimestampConverter {

    // 定义常见的时间格式模式
    private static final String[] DATE_PATTERNS = {
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyyMMddHHmmss",
            "yyyy年MM月dd日 HH时mm分ss秒",
            "EEE MMM dd HH:mm:ss zzz yyyy"  // 类似 "Sun Dec 28 16:08:02 CST 2025"
    };

    /**
     * 将字符串时间转换为时间戳（毫秒）
     * @param dateTimeStr 时间字符串
     * @return 时间戳（毫秒），如果转换失败返回null
     */
    public static Long toTimestamp(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }

        // 尝试所有预定义的模式
        for (String pattern : DATE_PATTERNS) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                LocalDateTime localDateTime;

                // 如果模式包含时区信息
                if (pattern.contains("XXX") || pattern.contains("zzz")) {
                    ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateTimeStr, formatter);
                    return zonedDateTime.toInstant().toEpochMilli();
                } else {
                    // 不包含时区信息，使用系统默认时区
                    localDateTime = LocalDateTime.parse(dateTimeStr, formatter);
                    ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
                    return zonedDateTime.toInstant().toEpochMilli();
                }
            } catch (DateTimeParseException e) {
                // 当前模式不匹配，继续尝试下一个模式
                continue;
            }
        }

        // 如果没有匹配的模式，尝试使用 Date.parse（处理类似 "2025-12-28 16:08:02" 的格式）
        try {
            // 替换常见分隔符以便 Date.parse 能识别
            String normalized = dateTimeStr
                    .replace('T', ' ')
                    .replace('-', '/')
                    .replace('年', '/')
                    .replace('月', '/')
                    .replace('日', ' ')
                    .replace('时', ':')
                    .replace('分', ':')
                    .replace('秒', ' ');

            Date date = new Date(normalized);
            return date.getTime();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将字符串时间转换为时间戳（秒）
     * @param dateTimeStr 时间字符串
     * @return 时间戳（秒），如果转换失败返回null
     */
    public static Long toTimestampSeconds(String dateTimeStr) {
        Long milliseconds = toTimestamp(dateTimeStr);
        return milliseconds != null ? milliseconds / 1000 : null;
    }

    /**
     * 使用指定格式将字符串时间转换为时间戳
     * @param dateTimeStr 时间字符串
     * @param pattern 时间格式模式
     * @return 时间戳（毫秒），如果转换失败返回null
     */
    public static Long toTimestamp(String dateTimeStr, String pattern) {
        if (dateTimeStr == null || pattern == null) {
            return null;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

            if (pattern.contains("XXX") || pattern.contains("zzz")) {
                // 包含时区信息
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateTimeStr, formatter);
                return zonedDateTime.toInstant().toEpochMilli();
            } else {
                // 不包含时区信息
                LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr, formatter);
                ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
                return zonedDateTime.toInstant().toEpochMilli();
            }
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * 将时间戳转换为指定格式的时间字符串
     * @param timestamp 时间戳（毫秒）
     * @param pattern 时间格式模式
     * @return 格式化的时间字符串
     */
    public static String fromTimestamp(Long timestamp, String pattern) {
        if (timestamp == null || pattern == null) {
            return null;
        }

        try {
            ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(timestamp),
                    ZoneId.systemDefault()
            );
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return zonedDateTime.format(formatter);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取当前时间的时间戳（毫秒）
     * @return 当前时间戳
     */
    public static long currentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 获取当前时间的时间戳（秒）
     * @return 当前时间戳（秒）
     */
    public static long currentTimestampSeconds() {
        return System.currentTimeMillis() / 1000;
    }


}