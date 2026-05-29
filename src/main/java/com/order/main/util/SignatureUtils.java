package com.order.main.util;

public class SignatureUtils {

    /**
     * 完整的签名合成方法
     * 规则：Base64(MD5(客户编号 + 密文 + privateKey))
     * 其中密文 = MD5(明文密码 + jadada236t2) 转大写
     *
     * @param customerCode 客户编号（如：J0086474299）
     * @param plainPassword 明文密码（如：H5CD3zE6）
     * @param privateKey 私钥（如：0258d71b55fc45e3ad7a7f38bf4b201a）
     * @return 最终签名
     */
    public static String generateSignature(String customerCode, String plainPassword, String privateKey) {
        try {
            // 第一步：生成密文
            String cipherText = generateCipherText(plainPassword);
            System.out.println("步骤1 - 密文: " + cipherText);

            // 第二步：拼接字符串：客户编号 + 密文 + privateKey
            String signStr = customerCode + cipherText + privateKey;
            System.out.println("步骤2 - 拼接字符串: " + signStr);

            // 第三步：MD5加密
            java.security.MessageDigest md5 = java.security.MessageDigest.getInstance("MD5");
            byte[] md5Bytes = md5.digest(signStr.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // 第四步：Base64编码
            String finalSignature = java.util.Base64.getEncoder().encodeToString(md5Bytes);
            System.out.println("步骤3 - 最终签名: " + finalSignature);

            return finalSignature;
        } catch (Exception e) {
            throw new RuntimeException("生成签名失败", e);
        }
    }

    /**
     * 生成密文
     * 规则：MD5(明文密码 + jadada236t2) 转大写
     */
    private static String generateCipherText(String plainPassword) {
        try {
            String salt = "jadada236t2";
            String signStr = plainPassword + salt;

            java.security.MessageDigest md5 = java.security.MessageDigest.getInstance("MD5");
            byte[] md5Bytes = md5.digest(signStr.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // 转换为十六进制字符串并转大写
            StringBuilder hexString = new StringBuilder();
            for (byte b : md5Bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString().toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("生成密文失败", e);
        }
    }

}