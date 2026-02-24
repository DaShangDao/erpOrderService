package com.order.main.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 孔夫子开放平台API调用工具类（支持accessToken）
 */
public class KongfzApiClient {

    // API网关地址
    private static final String API_GATEWAY = "https://open.kongfz.com/router/rest";

    private final String appId;
    private final String appSecret;
    private final String signMethod; // "md5" 或 "hmac"
    private String accessToken;      // 用户授权token

    /**
     * 构造函数（不带accessToken）
     */
    public KongfzApiClient(String appId, String appSecret, String signMethod) {
        this(appId, appSecret, signMethod, null);
    }

    /**
     * 构造函数（带accessToken）
     */
    public KongfzApiClient(String appId, String appSecret, String signMethod, String accessToken) {
        this.appId = appId;
        this.appSecret = appSecret;
        this.signMethod = signMethod.toLowerCase();
        this.accessToken = accessToken;

        if (!"md5".equals(this.signMethod) && !"hmac".equals(this.signMethod)) {
            throw new IllegalArgumentException("签名方法必须是 'md5' 或 'hmac'");
        }
    }

    /**
     * 设置/更新accessToken
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * 获取当前时间字符串 (GMT+8 时区)
     */
    public String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        return sdf.format(new Date());
    }

    /**
     * 构建完整的API请求参数（不包含sign）
     * @param method API方法名
     * @param businessParams 业务参数
     * @param requireAccessToken 是否需要accessToken
     * @return 参数Map
     */
    public Map<String, String> buildRequestParams(String method, Map<String, String> businessParams,
                                                  boolean requireAccessToken) {
        // 1. 构建基础参数
        Map<String, String> allParams = new LinkedHashMap<>();
        allParams.put("method", method);
        allParams.put("appId", appId);
        allParams.put("datetime", getCurrentDateTime());
        allParams.put("v", "1.0");
        allParams.put("signMethod", signMethod);
        allParams.put("format", "json");
        allParams.put("simplify", "0");

        // 2. 添加accessToken（如果需要）
        if (requireAccessToken) {
            if (accessToken == null || accessToken.trim().isEmpty()) {
                throw new IllegalStateException("此接口需要accessToken，但当前accessToken为空");
            }
            allParams.put("accessToken", accessToken);
        }

        // 3. 添加业务参数
        if (businessParams != null) {
            allParams.putAll(businessParams);
        }

        // 4. 生成签名
        String sign = generateSign(allParams);
        allParams.put("sign", sign);

        return allParams;
    }

    /**
     * 构建表单数据字符串（application/x-www-form-urlencoded）
     */
    private String buildFormData(Map<String, String> params) {
        List<String> paramPairs = new ArrayList<>();
        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = URLEncoder.encode(entry.getKey(), "UTF-8");
                String value = URLEncoder.encode(
                        entry.getValue() != null ? entry.getValue() : "",
                        "UTF-8"
                );
                paramPairs.add(key + "=" + value);
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("URL编码失败", e);
        }

        return String.join("&", paramPairs);
    }

    /**
     * 生成签名
     * @param params 所有请求参数（不包含sign参数）
     * @return 32位十六进制大写签名
     */
    public String generateSign(Map<String, String> params) {
        try {
            // 1. 移除sign参数，并按ASCII码排序
            Map<String, String> sortedParams = new TreeMap<>(params);
            sortedParams.remove("sign");

            // 2. 拼接参数名和参数值
            StringBuilder signStringBuilder = new StringBuilder();
            for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
                String value = entry.getValue() != null ? entry.getValue() : "";
                signStringBuilder.append(entry.getKey()).append(value);
            }
            String signString = signStringBuilder.toString();

            // 3. 根据签名方法计算签名
            String signature;
            if ("md5".equals(signMethod)) {
                signature = generateMD5Signature(signString);
            } else {
                signature = generateHmacMD5Signature(signString);
            }

            return signature.toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("生成签名失败", e);
        }
    }

    /**
     * 生成MD5签名：md5(appSecret + 字符串 + appSecret)
     */
    private String generateMD5Signature(String signString) throws NoSuchAlgorithmException {
        String toHash = appSecret + signString + appSecret;
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(toHash.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(digest);
    }

    /**
     * 生成HMAC-MD5签名
     */
    private String generateHmacMD5Signature(String signString) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(
                appSecret.getBytes(StandardCharsets.UTF_8),
                "HmacMD5"
        );
        Mac mac = Mac.getInstance("HmacMD5");
        mac.init(keySpec);
        byte[] digest = mac.doFinal(signString.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(digest);
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 构建完整的API请求URL
     * @param method API方法名
     * @param businessParams 业务参数
     * @param requireAccessToken 是否需要accessToken
     * @return 完整的请求URL
     */
    public String buildRequestUrl(String method, Map<String, String> businessParams,
                                  boolean requireAccessToken) {
        // 1. 构建基础参数
        Map<String, String> allParams = new HashMap<>();
        allParams.put("method", method);
        allParams.put("appId", appId);
        allParams.put("datetime", getCurrentDateTime());
        allParams.put("v", "1.0");
        allParams.put("signMethod", signMethod);
        allParams.put("format", "json");
        allParams.put("simplify", "0");

        // 2. 添加accessToken（如果需要）
        if (requireAccessToken) {
            if (accessToken == null || accessToken.trim().isEmpty()) {
                throw new IllegalStateException("此接口需要accessToken，但当前accessToken为空");
            }
            allParams.put("accessToken", accessToken);
        }

        // 3. 添加业务参数
        if (businessParams != null) {
            allParams.putAll(businessParams);
        }

        // 4. 生成签名
        String sign = generateSign(allParams);
        allParams.put("sign", sign);

        // 5. 构建查询字符串
        return buildQueryString(allParams);
    }

    /**
     * 构建查询参数字符串
     */
    private String buildQueryString(Map<String, String> params) {
        List<String> paramPairs = new ArrayList<>();
        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = URLEncoder.encode(entry.getKey(), "UTF-8");
                String value = URLEncoder.encode(
                        entry.getValue() != null ? entry.getValue() : "",
                        "UTF-8"
                );
                paramPairs.add(key + "=" + value);
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("URL编码失败", e);
        }

        String queryString = String.join("&", paramPairs);
        return API_GATEWAY + "?" + queryString;
    }

    /**
     * 调用店铺商品列表API（需要accessToken）
     * @param type 商品类型：sale（在售）、delisting（下架）等
     * @param pageNum 页码，从1开始
     * @param pageSize 每页大小，最大100
     * @param otherParams 其他业务参数
     * @return API响应
     */
    public String callShopItemList(String type, Integer pageNum, Integer pageSize,
                                   Map<String, String> otherParams) throws Exception {
        Map<String, String> businessParams = new HashMap<>();

        // 设置基本业务参数
        if (type != null) {
            businessParams.put("type", type);
        }
        if (pageNum != null) {
            businessParams.put("pageNum", pageNum.toString());
        }
        if (pageSize != null) {
            businessParams.put("pageSize", pageSize.toString());
        }

        // 添加其他业务参数
        if (otherParams != null) {
            businessParams.putAll(otherParams);
        }

        // 构建参数并发送请求
        Map<String, String> requestParams = buildRequestParams(
                "kongfz.shop.item.list",
                businessParams,
                true
        );

        return sendPostRequest(requestParams);
    }

    /**
     * 发送HTTP POST请求（参数放在请求体中）
     */
    public String sendPostRequest(Map<String, String> params) throws Exception {
        // 构建表单数据
        String formData = buildFormData(params);

        // 调试信息
        System.out.println("=== POST请求调试信息 ===");
        System.out.println("请求URL: " + API_GATEWAY);
        System.out.println("请求参数: ");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if ("accessToken".equals(key) && value != null && value.length() > 10) {
                value = value.substring(0, 10) + "..." + value.substring(value.length() - 10);
            }
            System.out.println("  " + key + ": " + value);
        }
        System.out.println("表单数据: " + formData);

        // 创建连接
        URL url = new URL(API_GATEWAY);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // 设置请求属性
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        conn.setDoOutput(true); // 允许输出
        conn.setDoInput(true);  // 允许输入

        // 设置请求头
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "Kongfz-API-Client/1.0");

        // 发送请求体
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = formData.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
            System.out.println("已发送请求体，长度: " + input.length + " 字节");
        }

        // 获取响应
        int responseCode = conn.getResponseCode();
        System.out.println("响应状态码: " + responseCode);

        // 读取响应
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        responseCode == 200 ? conn.getInputStream() : conn.getErrorStream(),
                        StandardCharsets.UTF_8
                )
        )) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        String responseStr = response.toString();
        System.out.println("响应内容: " + responseStr);
        System.out.println("=== 请求完成 ===\n");

        if (responseCode != 200) {
            throw new RuntimeException("HTTP请求失败，状态码: " + responseCode +
                    "，响应: " + responseStr);
        }

        return responseStr;
    }

    /**
     * 发送HTTP请求（兼容旧方法）
     * @deprecated 使用 sendPostRequest(Map) 代替
     */
    @Deprecated
    public String sendGetRequest(String url) throws Exception {
        // 解析URL中的参数
        int queryIndex = url.indexOf("?");
        if (queryIndex == -1) {
            throw new IllegalArgumentException("URL中没有查询参数");
        }

        String queryString = url.substring(queryIndex + 1);
        String[] pairs = queryString.split("&");

        Map<String, String> params = new HashMap<>();
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
            String value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
            params.put(key, value);
        }

        return sendPostRequest(params);
    }

    /**
     * 解析API响应
     */
    public static Map<String, Object> parseApiResponse(String jsonResponse) {
        // 这里使用简单的JSON解析，实际项目中建议使用Jackson、Gson等库
        try {
            // 简单演示，实际需要完整JSON解析
            if (jsonResponse.contains("errorResponse") &&
                    !jsonResponse.contains("\"errorResponse\":null")) {
                System.err.println("API调用返回错误: " + jsonResponse);
                return Collections.singletonMap("error", true);
            }
            // 这里可以添加完整的JSON解析逻辑
            return Collections.singletonMap("success", true);
        } catch (Exception e) {
            throw new RuntimeException("解析API响应失败", e);
        }
    }
}