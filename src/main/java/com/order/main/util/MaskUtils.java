package com.order.main.util;

/**
 * 脱敏工具
 */
public class MaskUtils {

    public static String maskName(String name) {
        if (name == null || name.trim().isEmpty()) return name;
        name = name.trim();
        int len = name.length();
        if (len == 1) return name;
        if (len == 2) return name.substring(0, 1) + "*";
        // 3个字及以上：保留第一个字，后面最多4个*
        int stars = Math.min(len - 1, 4);
        return name.substring(0, 1) + "*".repeat(stars);
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return phone;
        phone = phone.trim();
        if (phone.length() != 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    // 乡镇/街道脱敏：保留前2个字，后面最多4个*
    public static String maskTown(String town) {
        town = town.trim();
        int len = town.length();
        if (len <= 2) {
            return town; // 太短就不脱敏了
        }
        if (len == 3) {
            return town.substring(0, 2) + "*";
        }
        // 4个字及以上：保留前2个字，后面最多4个*
        int stars = Math.min(len - 2, 4);
        return town.substring(0, 2) + "*".repeat(stars);
    }
}
