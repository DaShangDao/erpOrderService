package com.order.main.dll;

import com.order.main.config.NativeLibConfig;
import com.sun.jna.Function;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import lombok.Setter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class PddSimpleDllLoader {

    private static NativeLibrary nativeLibrary;

    @Setter
    private static NativeLibConfig nativeLibConfig;
    // 拼多多订单同步
    private static Function pddOrderSynchronizationFunc;
    // 查询订单列表
    private static Function pddOrderBasicListGetFunc;
    // 查询面单服务订购及面单使用情况
    private static Function pddWaybillSearchFunc;
    // 电子面单云打印
    private static Function pddWaybillGetFunc;
    // 电子面单取号
    private static Function pddFdsWaybillGetFunc;
    // 商家取消获取的电子面单号
    private static Function pddWaybillCancelFunc;

    private static Function freeCStringFunc;




    public static void loadDLL() throws Exception {
        String libraryPath = getLibraryPath();
        System.out.println("正在加载 PDD native 库: " + libraryPath);

        File libraryFile = new File(libraryPath);

        // 验证库文件是否存在
        if (!libraryFile.exists()) {
            throw new FileNotFoundException("PDD Native 库文件不存在: " + libraryPath);
        }

        // 验证文件大小
        System.out.println("PDD 库文件大小: " + libraryFile.length() + " bytes");
        if (libraryFile.length() == 0) {
            throw new IOException("PDD 库文件为空: " + libraryPath);
        }

        // 在 Linux 系统上设置执行权限
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            boolean success = libraryFile.setExecutable(true);
            System.out.println("设置 PDD 库执行权限: " + (success ? "成功" : "失败"));
        }

        // 加载库文件
        nativeLibrary = NativeLibrary.getInstance(libraryFile.getAbsolutePath());

        // 拼多多订单同步
        pddOrderSynchronizationFunc = nativeLibrary.getFunction("PddOrderSynchronization");

        // 查询订单列表
        pddOrderBasicListGetFunc = nativeLibrary.getFunction("PddOrderBasicListGet");

        // 查询面单服务订购及面单使用情况
        pddWaybillSearchFunc = nativeLibrary.getFunction("PddWaybillSearch");

        // 电子面单云打印
        pddWaybillGetFunc = nativeLibrary.getFunction("PddWaybillGet");

        // 电子面单取号
        pddFdsWaybillGetFunc = nativeLibrary.getFunction("PddFdsWaybillGet");

        // 商家取消获取的电子面单号
        pddWaybillCancelFunc = nativeLibrary.getFunction("PddWaybillCancel");

        // 释放c串内存
        freeCStringFunc = nativeLibrary.getFunction("FreeCString");

        if (pddOrderSynchronizationFunc == null) throw new Exception("无法找到 PddOrderSynchronization 函数");
        if (pddOrderBasicListGetFunc == null) throw new Exception("无法找到 PddOrderBasicListGet 函数");
        if (pddWaybillSearchFunc == null) throw new Exception("无法找到 PddWaybillSearch 函数");
        if (pddWaybillGetFunc == null) throw new Exception("无法找到 PddWaybillGet 函数");
        if (pddFdsWaybillGetFunc == null) throw new Exception("无法找到 PddFdsWaybillGet 函数");
        if (freeCStringFunc == null) throw new Exception("无法找到 FreeCString 函数");

        System.out.println("PDD Native 库加载成功: " + libraryPath);
    }

    /**
     * 获取库文件路径
     */
    private static String getLibraryPath() throws IOException {
        // 优先使用外部路径
        String externalPath = nativeLibConfig.getPdd();
        File externalFile = new File(externalPath);

        if (externalFile.exists()) {
            System.out.println("使用 PDD 外部库文件: " + externalPath);
            return externalPath;
        }

        // 如果外部文件不存在，回退到原来的资源提取方式
        System.out.println("PDD 外部库文件不存在，回退到资源提取方式: " + externalPath);
        return extractLibraryFromResources(getPlatformLibraryName()).getAbsolutePath();
    }

    /**
     * 根据操作系统获取对应的库文件名（备用方案）
     */
    private static String getPlatformLibraryName() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            return "pdd.dll";
        } else if (osName.contains("linux")) {
            return "pdd.so";
        } else if (osName.contains("mac")) {
            return "pdd.dylib";
        } else {
            throw new UnsupportedOperationException("不支持的操作系统: " + osName);
        }
    }

    /**
     * 从资源目录提取 native 库文件（备用方案）
     */
    private static File extractLibraryFromResources(String libraryName) throws IOException {
        String resourcePath = "/native/" + libraryName;
        InputStream inputStream = SimpleDllLoader.class.getResourceAsStream(resourcePath);

        if (inputStream == null) {
            throw new IOException("PDD Native 库未找到: " + resourcePath);
        }

        File tempFile = File.createTempFile("pdd_native_", getLibrarySuffix(libraryName));
        tempFile.deleteOnExit();

        System.out.println("从资源提取 PDD 库文件: " + resourcePath + " -> " + tempFile.getAbsolutePath());

        try (InputStream is = inputStream) {
            Files.copy(is, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        // 设置执行权限
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            boolean success = tempFile.setExecutable(true);
            System.out.println("设置 PDD 库执行权限: " + (success ? "成功" : "失败"));
        }

        System.out.println("PDD 库文件提取完成，大小: " + tempFile.length() + " bytes");
        return tempFile;
    }

    /**
     * 获取库文件后缀
     */
    private static String getLibrarySuffix(String libraryName) {
        if (libraryName.endsWith(".dll")) return ".dll";
        if (libraryName.endsWith(".so")) return ".so";
        if (libraryName.endsWith(".dylib")) return ".dylib";
        return ".lib";
    }

    /**
     * 拼多多订单同步
     * @param clientId 拼多多开放平台ClientID
     * @param clientSecret 拼多多开放平台ClientSecret
     * @param accessToken 授权令牌
     * @param logisticsCompany 物流公司名称
     * @return 同步结果字符串
     */
    public static String executePddOrderSynchronization(String clientId, String clientSecret, String accessToken,
                                                        String logisticsCompany, String json) {
        try {
            String cleanedClientId = ensureUtf8(clientId);
            String cleanedClientSecret = ensureUtf8(clientSecret);
            String cleanedAccessToken = ensureUtf8(accessToken);
            String cleanedLogisticsCompany = ensureUtf8(logisticsCompany);
            String cleanedJson = ensureUtf8(json);

            Object result = pddOrderSynchronizationFunc.invoke(Pointer.class,
                    new Object[]{cleanedClientId, cleanedClientSecret, cleanedAccessToken,
                            cleanedLogisticsCompany, cleanedJson});
            return ptrToString((Pointer) result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 查询订单列表
     * @param clientId 拼多多开放平台ClientID
     * @param clientSecret 拼多多开放平台ClientSecret
     * @param accessToken 授权令牌
     * @param json 查询参数
     * @return 查询结果字符串
     */
    public static String executePddOrderBasicListGet(String clientId, String clientSecret, String accessToken,String json) {
        try {
            String cleanedClientId = ensureUtf8(clientId);
            String cleanedClientSecret = ensureUtf8(clientSecret);
            String cleanedAccessToken = ensureUtf8(accessToken);
            String cleanedJson = ensureUtf8(json);
            Object result = pddOrderBasicListGetFunc.invoke(Pointer.class,
                    new Object[]{cleanedClientId, cleanedClientSecret, cleanedAccessToken,cleanedJson});
            return ptrToString((Pointer) result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 拼多多dll接口调用
     * @param api           接口类型
     * @param clientId
     * @param clientSecret
     * @param accessToken
     * @param json
     * @return
     */
    public static String executePddApi(String api,String clientId, String clientSecret, String accessToken,String json){
        try {
            String cleanedClientId = ensureUtf8(clientId);
            String cleanedClientSecret = ensureUtf8(clientSecret);
            String cleanedAccessToken = ensureUtf8(accessToken);
            String cleanedJson = ensureUtf8(json);
            Object result = null;
            if (api.equals("PddWaybillSearch")){
                result = pddWaybillSearchFunc.invoke(Pointer.class,
                        new Object[]{cleanedClientId, cleanedClientSecret, cleanedAccessToken,cleanedJson});
            }else if (api.equals("PddWaybillGet")){
                result = pddWaybillGetFunc.invoke(Pointer.class,
                        new Object[]{cleanedClientId, cleanedClientSecret, cleanedAccessToken,cleanedJson});
            }else if (api.equals("PddFdsWaybillGet")){
                result = pddFdsWaybillGetFunc.invoke(Pointer.class,
                        new Object[]{cleanedClientId, cleanedClientSecret, cleanedAccessToken,cleanedJson});
            }else if (api.equals("PddWaybillCancel")){
                result = pddWaybillCancelFunc.invoke(Pointer.class,
                        new Object[]{cleanedClientId, cleanedClientSecret, cleanedAccessToken,cleanedJson});
            }
            return ptrToString((Pointer) result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }


    /**
     * 确保字符串使用UTF-8编码
     */
    private static String ensureUtf8(String str) {
        if (str == null) return null;
        try {
            byte[] utf8Bytes = str.getBytes(StandardCharsets.UTF_8);
            return new String(utf8Bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("UTF-8编码转换失败: " + e.getMessage());
            return str;
        }
    }

    private static String ptrToString(Pointer ptr) {
        if (ptr == null) return "";
        try {
            return ptr.getString(0, "UTF-8");
        } catch (Exception e) {
            System.err.println("字符串解码失败: " + e.getMessage());
            return "";
        } finally {
            freeCString(ptr);
        }
    }

    private static void freeCString(Pointer ptr) {
        if (ptr != null) {
            try {
                freeCStringFunc.invoke(Void.class, new Object[]{ptr});
            } catch (Exception e) {
                System.err.println("释放C字符串失败: " + e.getMessage());
            }
        }
    }
}