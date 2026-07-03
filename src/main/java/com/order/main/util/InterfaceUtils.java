package com.order.main.util;

import com.pdd.pop.sdk.common.exception.JsonParseException;
import com.pdd.pop.sdk.common.util.JsonUtil;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;


public class InterfaceUtils {

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000); // 连接超时时间
        factory.setReadTimeout(30000);    // 读取超时时间
        return new RestTemplate(factory);
    }

    private static RestTemplate createRestTemplateKfz() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 连接超时时间
        factory.setReadTimeout(30000);    // 读取超时时间
        return new RestTemplate(factory);
    }



    public static String getInterface2(String ip, String url) {
        // 目标接口 URL
        url = ip + url;
        // 创建 RestTemplate 实例
        RestTemplate restTemplate = createRestTemplateWithUtf8();

        // 发起 GET 请求
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        String responseBody = response != null ? response.getBody() : null;

        if(responseBody == null){
            throw new RuntimeException("拼多多服务接口异常：返回空数据");
        }

        // 检查返回数据格式
        if (responseBody != null) {
            // 如果是数字-数字的格式，直接返回
            if (responseBody.matches("\\d+-\\d+")) {
                return responseBody;
            }
            // 如果包含汉字，则抛出异常，异常信息为返回体内容
            else if (containsChinese(responseBody)) {
                throw new RuntimeException(responseBody);
            }
        }

        return responseBody;
    }


    /**
     * 判断字符串是否包含中文字符
     * @param str 要检查的字符串
     * @return 是否包含中文
     */
    private static boolean containsChinese(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return str.matches(".*[\u4e00-\u9fa5]+.*");
    }

    public static String getInterface(String ip, String url) {
        // 目标接口 URL
        url = ip + url;
        // 创建 RestTemplate 实例
        RestTemplate restTemplate = createRestTemplateWithUtf8();

        int retryCount = 0;
        int maxRetries = 0;

        ResponseEntity<String> response = null;

        while (retryCount <= maxRetries) {
            try {
                // 发起 GET 请求
                response = restTemplate.getForEntity(url, String.class);
                if(response != null
                        && response.getBody() != null
                        && response.getBody().equals("调用过于频繁，请调整调用频率")){
                    throw new RuntimeException();
                }
                break; // 成功则跳出循环
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                // 替代JSON解析的简单版本
                if (errorMessage != null && errorMessage.contains("\"error\"")) {
                    // 简单提取error信息
                    int errorIndex = errorMessage.indexOf("\"error\"");
                    int colonIndex = errorMessage.indexOf(":", errorIndex);
                    int quoteStart = errorMessage.indexOf("\"", colonIndex + 1);
                    int quoteEnd = errorMessage.indexOf("\"", quoteStart + 1);

                    if (quoteStart > 0 && quoteEnd > quoteStart) {
                        String extractedError = errorMessage.substring(quoteStart + 1, quoteEnd);
                        throw new RuntimeException(extractedError);
                    }
                }

                retryCount++;
                if (retryCount > maxRetries) {
                    throw new RuntimeException("连接超时,请稍后重试");
                }
                // 指数退避：0.4s, 0.8s, 1.6s...
                long backoffTime = (long) Math.pow(2, retryCount) * 200; // 单位：毫秒
                try {
                    System.out.println("请求失败，正在进行第 " + retryCount + " 次重试... 请求链接：" + url + "，等待 " + backoffTime + " 毫秒");
                    Thread.sleep(backoffTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试等待被中断");
                }
            }
        }

        return response != null ? response.getBody() : null;
    }


    // 创建支持 UTF-8 的 RestTemplate
    private static RestTemplate createRestTemplateWithUtf8() {
        RestTemplate restTemplate = new RestTemplate();

        // 获取原有的消息转换器
        List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();

        // 移除原有的 StringHttpMessageConverter
        converters.removeIf(converter -> converter instanceof StringHttpMessageConverter);

        // 添加支持 UTF-8 的 StringHttpMessageConverter
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringConverter.setWriteAcceptCharset(false); // 重要：防止在请求头中添加 charset

        // 将 UTF-8 转换器添加到列表开头，优先使用
        converters.add(0, stringConverter);

        restTemplate.setMessageConverters(converters);
        return restTemplate;
    }

    public static String getInterfacePost(String ip, String url, Map params) {
        // 目标接口 URL
        url = ip + url;
        // 创建 RestTemplate 实例
        RestTemplate restTemplate = createRestTemplate();
        // 发起 Post 请求
        ResponseEntity<String> response = restTemplate.postForEntity(url, params, String.class);
        String responseBody = response.getBody();

        return responseBody;
    }

    public static void getInterfacePostWithJson(String ip, String url, String jsonBody) {
        // 目标接口 URL
        url = ip + url;
        // 创建 RestTemplate 实例
        RestTemplate restTemplate = createRestTemplate();
        // 设置请求头
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

        restTemplate.postForEntity(url, requestEntity, String.class);
    }

    public static String postForm(String ip, String url, Map<String, Object> formData) {
        // 拼接完整 URL
        url = ip + url;

        // 创建 RestTemplate 实例
        RestTemplate restTemplate = createRestTemplate();

        // 构建表单数据 - 使用Object作为值类型，可以接受String或File
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        // 处理表单数据
        formData.forEach((key, value) -> {
            if (value == null) {
                // 可以选择跳过，或者存入空字符串 ""
                body.add(key, "");
            } else if (value instanceof File) {
                body.add(key, new FileSystemResource((File) value));
            } else {
                body.add(key, value.toString());
            }
        });

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // 发送 POST 请求，并加入重试机制
        int retryCount = 0;
        int maxRetries = 0;
        ResponseEntity<String> response = null;

        while (retryCount <= maxRetries) {
            try {
                // 发送 POST 请求
                response = restTemplate.postForEntity(url, requestEntity, String.class);
                break; // 成功则跳出循环
            } catch (Exception e) {
                retryCount++;
                if (retryCount > maxRetries) {
                    throw new RuntimeException("连接超时,请稍后重试");
                }
                // 指数退避：500ms, 1s, 2s...
                long backoffTime = (long) Math.pow(2, retryCount) * 500; // 单位：毫秒
                try {
                    Thread.sleep(backoffTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试等待被中断");
                } catch (JsonParseException jse){
                    System.out.println("解析json错误");
                }
            }
        }

        return response != null ? response.getBody() : null;
    }

    public static String postFormNoTimeout(String ip, String url, Map<String, Object> formData) {
        // 拼接完整 URL
        url = ip + url;

        // 创建 RestTemplate 实例（不设置超时时间）
        RestTemplate restTemplate = new RestTemplate();

        // 构建表单数据 - 使用Object作为值类型，可以接受String或File
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        // 处理表单数据
        formData.forEach((key, value) -> {
            if (value == null) {
                // 可以选择跳过，或者存入空字符串 ""
                body.add(key, "");
            } else if (value instanceof File) {
                body.add(key, new FileSystemResource((File) value));
            } else {
                body.add(key, value.toString());
            }
        });

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // 发送 POST 请求，并加入重试机制
        int retryCount = 0;
        int maxRetries = 3;
        ResponseEntity<String> response = null;

        while (retryCount <= maxRetries) {
            try {
                // 发送 POST 请求
                response = restTemplate.postForEntity(url, requestEntity, String.class);
                break; // 成功则跳出循环
            } catch (Exception e) {
                e.printStackTrace();
                retryCount++;
                if (retryCount > maxRetries) {
                    throw new RuntimeException("连接超时,请稍后重试");
                }
                // 指数退避：500ms, 1s, 2s...
                long backoffTime = (long) Math.pow(2, retryCount) * 500; // 单位：毫秒
                try {
                    Thread.sleep(backoffTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试等待被中断");
                } catch (JsonParseException jse) {
                    System.out.println("解析json错误");
                }
            }
        }

        return response != null ? response.getBody() : null;
    }



    /**
     * 通用带签名和认证的GET请求（支持指定签名方法）
     *
     * @param baseUrl            完整的基础URL（包含IP和路径，如：http://192.168.101.213:9090/api/admin/employee/list）
     * @param queryParams        Query参数（会自动添加签名）
     * @param authorizationToken Bearer Token（可选，传null则不添加）
     * @param signMethod         签名方法（md5或sha256）
     * @return 响应内容
     */
    public static String getWithSign(String baseUrl, Map<String, String> queryParams, String authorizationToken, String signMethod) {
        // 复制参数，避免修改原Map
        Map<String, String> paramsWithSign = new HashMap<>();
        if (queryParams != null) {
            paramsWithSign.putAll(queryParams);
        }

        // 如果签名方法未指定且参数中没有sign_method，使用默认值
        if (signMethod != null && !paramsWithSign.containsKey("sign_method")) {
            paramsWithSign.put("sign_method", signMethod);
        }

        // 获取实际的签名方法
        String actualSignMethod = paramsWithSign.getOrDefault("sign_method", "md5");

        // 构建待签名字符串
        String signString = buildSignString(paramsWithSign, null);
        System.out.println("待签名字符串: " + signString);

        // 计算签名
        String sign = calculateSign(signString, actualSignMethod);
        System.out.println("计算签名: " + sign);

        // 将签名添加到参数中
        paramsWithSign.put("sign", sign);

        // 构建完整URL
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);
        for (Map.Entry<String, String> entry : paramsWithSign.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                builder.queryParam(entry.getKey(), entry.getValue());
            }
        }
        String finalUrl = builder.toUriString();
        System.out.println("请求URL: " + finalUrl);

        // 创建 RestTemplate
        RestTemplate restTemplate = createRestTemplateWithUtf8();

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        if (authorizationToken != null && !authorizationToken.isEmpty()) {
            headers.set("Authorization", "Bearer " + authorizationToken);
        }

        // 发送请求
        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                finalUrl,
                HttpMethod.GET,
                requestEntity,
                String.class
        );

        return response.getBody();
    }

    /**
     * 通用带签名和认证的POST表单请求（支持指定签名方法）
     *
     * @param baseUrl 完整的基础URL
     * @param queryParams Query参数（可选）
     * @param bodyParams Body表单参数（会自动添加签名）
     * @param authorizationToken Bearer Token（可选）
     * @param signMethod 签名方法（md5或sha256）
     * @return 响应内容
     */
    public static String postFormWithSign(String baseUrl, Map<String, String> queryParams,
                                          Map<String, String> bodyParams, String authorizationToken,
                                          String signMethod) {
        // 复制参数，避免修改原Map
        Map<String, String> queryWithSign = new HashMap<>();
        Map<String, String> bodyWithSign = new HashMap<>();

        if (queryParams != null) {
            queryWithSign.putAll(queryParams);
        }
        if (bodyParams != null) {
            bodyWithSign.putAll(bodyParams);
        }

        // 如果签名方法未指定且参数中没有sign_method，使用默认值
        if (signMethod != null && !bodyWithSign.containsKey("sign_method")) {
            bodyWithSign.put("sign_method", signMethod);
        }

        // 获取实际的签名方法
        String actualSignMethod = bodyWithSign.getOrDefault("sign_method", "md5");

        // 构建待签名字符串（包含Query和Body参数）
        String signString = buildSignString(queryWithSign, bodyWithSign);
        System.out.println("待签名字符串: " + signString);

        // 计算签名
        String sign = calculateSign(signString, actualSignMethod);
        System.out.println("计算签名: " + sign);

        // 将签名添加到Body参数中
        bodyWithSign.put("sign", sign);

        // 构建完整URL（带Query参数）
        String finalUrl = baseUrl;
        if (queryWithSign != null && !queryWithSign.isEmpty()) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);
            for (Map.Entry<String, String> entry : queryWithSign.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    builder.queryParam(entry.getKey(), entry.getValue());
                }
            }
            finalUrl = builder.toUriString();
        }
        System.out.println("请求URL: " + finalUrl);

        // 创建 RestTemplate
        RestTemplate restTemplate = createRestTemplateWithUtf8();

        // 构建表单数据
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        for (Map.Entry<String, String> entry : bodyWithSign.entrySet()) {
            if (entry.getValue() != null) {
                body.add(entry.getKey(), entry.getValue());
            }
        }

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
        if (authorizationToken != null && !authorizationToken.isEmpty()) {
            headers.set("Authorization", "Bearer " + authorizationToken);
        }

        // 发送请求
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(finalUrl, requestEntity, String.class);

        return response.getBody();
    }

    /**
     * 构建待签名字符串（支持Query和Body参数）
     * 参数名按ASCII排序，拼接格式：key1=value1&key2=value2，前后包裹appSecret
     *
     * @param queryParams Query参数
     * @param bodyParams Body参数（表单格式）
     * @return 待签名字符串
     */
    private static String buildSignString(Map<String, String> queryParams, Map<String, String> bodyParams) {
        String appSecret = "psi_api_sign_secret";
        Map<String, String> allParams = new HashMap<>();

        // 合并Query参数（排除sign）
        if (queryParams != null) {
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (!"sign".equals(entry.getKey()) && entry.getValue() != null && !entry.getValue().isEmpty()) {
                    allParams.put(entry.getKey(), entry.getValue());
                }
            }
        }

        // 合并Body参数（排除sign）
        if (bodyParams != null) {
            for (Map.Entry<String, String> entry : bodyParams.entrySet()) {
                if (!"sign".equals(entry.getKey()) && entry.getValue() != null && !entry.getValue().isEmpty()) {
                    allParams.put(entry.getKey(), entry.getValue());
                }
            }
        }

        // 参数名ASCII排序
        List<String> keys = new ArrayList<>(allParams.keySet());
        Collections.sort(keys);

        // 构建参数字符串
        StringBuilder paramPairs = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) {
                paramPairs.append("&");
            }
            paramPairs.append(keys.get(i)).append("=").append(allParams.get(keys.get(i)));
        }

        // 前后包裹appSecret
        return appSecret + paramPairs.toString() + appSecret;
    }

    /**
     * 计算签名（支持MD5和SHA256）
     *
     * @param signString 待签名字符串
     * @param signMethod 签名方法（md5或sha256）
     * @return 签名字符串（大写）
     */
    private static String calculateSign(String signString, String signMethod) {
        if ("sha256".equalsIgnoreCase(signMethod)) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] digest = md.digest(signString.getBytes(StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString().toUpperCase();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("SHA-256 algorithm not found", e);
            }
        } else {
            // 默认使用MD5
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] digest = md.digest(signString.getBytes(StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString().toUpperCase();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("MD5 algorithm not found", e);
            }
        }
    }
}
