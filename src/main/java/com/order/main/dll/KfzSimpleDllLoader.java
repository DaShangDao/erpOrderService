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

public class KfzSimpleDllLoader {

    private static NativeLibrary nativeLibrary;
    /**
     * -- SETTER --
     *  初始化配置
     */
    @Setter
    private static NativeLibConfig nativeLibConfig;
    // 获取孔夫子订单列表
    private static Function kongfzOrderListFunc;
    // 查询孔夫子单个订单
    private static Function kongfzOrderGetFunc;
    // 同步订单的快递单号
    private static Function kongfzOrderSynchronizationFunc;

    private static Function freeCStringFunc;



    public static void loadDLL() throws Exception {
        String libraryPath = getLibraryPath();
        System.out.println("正在加载 native 库: " + libraryPath);

        File libraryFile = new File(libraryPath);

        // 验证库文件是否存在
        if (!libraryFile.exists()) {
            throw new FileNotFoundException("Native 库文件不存在: " + libraryPath);
        }

        // 验证文件大小
        System.out.println("库文件大小: " + libraryFile.length() + " bytes");
        if (libraryFile.length() == 0) {
            throw new IOException("库文件为空: " + libraryPath);
        }

        // 在 Linux 系统上设置执行权限
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            boolean success = libraryFile.setExecutable(true);
            System.out.println("设置执行权限: " + (success ? "成功" : "失败"));
        }

        // 加载库文件
        nativeLibrary = NativeLibrary.getInstance(libraryFile.getAbsolutePath());
        // 获取订单列表
        kongfzOrderListFunc = nativeLibrary.getFunction("KongfzOrderList");
        // 查询单个订单
        kongfzOrderGetFunc = nativeLibrary.getFunction("KongfzOrderGet");
        // 同步订单的快递单号
        kongfzOrderSynchronizationFunc = nativeLibrary.getFunction("KongfzOrderSynchronization");
        // 释放c串内存
        freeCStringFunc = nativeLibrary.getFunction("FreeCString");

        if (kongfzOrderListFunc == null) throw new Exception("无法找到 KongfzOrderList 函数");
        if (kongfzOrderGetFunc == null) throw new Exception("无法找到 KongfzOrderGet 函数");
        if (kongfzOrderSynchronizationFunc == null) throw new Exception("无法找到 KongfzOrderSynchronization 函数");
        if (freeCStringFunc == null) throw new Exception("无法找到 FreeCString 函数");

        System.out.println("Native 库加载成功: " + libraryPath);
    }



    /**
     * 获取库文件路径
     */
    private static String getLibraryPath() throws IOException {
        // 优先使用外部路径
        String externalPath = nativeLibConfig.getKfz();
        File externalFile = new File(externalPath);

        if (externalFile.exists()) {
            System.out.println("使用外部库文件: " + externalPath);
            return externalPath;
        }

        // 如果外部文件不存在，回退到原来的资源提取方式
        System.out.println("外部库文件不存在，回退到资源提取方式: " + externalPath);
        return extractLibraryFromResources(getPlatformLibraryName()).getAbsolutePath();
    }

    /**
     * 根据操作系统获取对应的库文件名（备用方案）
     */
    private static String getPlatformLibraryName() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            return "kongfz.dll";
        } else if (osName.contains("linux")) {
            return "kongfz.so";
        } else if (osName.contains("mac")) {
            return "kongfz.dylib";
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
            throw new IOException("Native 库未找到: " + resourcePath);
        }

        File tempFile = File.createTempFile("native_", getLibrarySuffix(libraryName));
        tempFile.deleteOnExit();

        System.out.println("从资源提取库文件: " + resourcePath + " -> " + tempFile.getAbsolutePath());

        try (InputStream is = inputStream) {
            Files.copy(is, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        // 设置执行权限
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            boolean success = tempFile.setExecutable(true);
            System.out.println("设置执行权限: " + (success ? "成功" : "失败"));
        }

        System.out.println("库文件提取完成，大小: " + tempFile.length() + " bytes");
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
     * 查询订单列表
     * @param appId 开放平台分配给应用的AppId
     * @param appSecret App密钥
     * @param accessToken 用户登录授权成功后，开放平台颁发给应用的授权信息
     * @param orderListJson 查询订单请求结构体字符串
     * @return 查询结果字符串
     */
    public static String executeKongfzOrderList(int appId, String appSecret, String accessToken, String orderListJson) {
        try {
            String cleanedAppSecret = ensureUtf8(appSecret);
            String cleanedAccessToken = ensureUtf8(accessToken);
            String cleanedOrderListJson = ensureUtf8(orderListJson);

            Object result = kongfzOrderListFunc.invoke(Pointer.class,
                    new Object[]{appId, cleanedAppSecret, cleanedAccessToken, cleanedOrderListJson});
            return ptrToString((Pointer) result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 获取单个订单信息
     * @param appId 开放平台分配给应用的AppId
     * @param appSecret App密钥
     * @param accessToken 用户登录授权成功后，开放平台颁发的授权信息
     * @param userType 用户类型
     * @param orderId 订单编号
     * @return 订单信息字符串
     */
    public static String executeKongfzOrderGet(int appId, String appSecret, String accessToken,String userType ,int orderId) {
        try {
            String cleanedAppSecret = ensureUtf8(appSecret);
            String cleanedAccessToken = ensureUtf8(accessToken);
            String cleanedUserType = ensureUtf8(userType);


            Object result = kongfzOrderGetFunc.invoke(Pointer.class,
                    new Object[]{appId, cleanedAppSecret, cleanedAccessToken,cleanedUserType,orderId});
            return ptrToString((Pointer) result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 孔网订单同步
     * @param appId 开放平台分配给应用的AppId
     * @param appSecret App密钥
     * @param accessToken 用户登录授权成功后，开放平台颁发给应用的授权信息
     * @param shippingComName 快递名称
     * @param orderId 订单编号
     * @param shippingId 配送方式
     * @param shippingCom 快递公司。当shippingId!=noLogistics时，此参数为必填
     * @param shipmentNum 快递单号。当shippingId!=noLogistics时，此参数为必填
     * @param userDefined 用户自定义物流公司。当shippingCom=other时，此参数为必填
     * @param moreShipmentNum 填写更多的快递单号，以逗号分隔
     * @return 同步结果字符串
     */
    public static String executeKongfzOrderSynchronization(int appId, String appSecret, String accessToken,
                                                           String shippingComName, int orderId, String shippingId,
                                                           String shippingCom, String shipmentNum,
                                                           String userDefined, String moreShipmentNum) {
        try {
            String cleanedAppSecret = ensureUtf8(appSecret);
            String cleanedAccessToken = ensureUtf8(accessToken);
            String cleanedShippingComName = ensureUtf8(shippingComName);
            String cleanedShippingId = ensureUtf8(shippingId);
            String cleanedShippingCom = ensureUtf8(shippingCom);
            String cleanedShipmentNum = ensureUtf8(shipmentNum);
            String cleanedUserDefined = ensureUtf8(userDefined);
            String cleanedMoreShipmentNum = ensureUtf8(moreShipmentNum);

            Object result = kongfzOrderSynchronizationFunc.invoke(Pointer.class,
                    new Object[]{appId, cleanedAppSecret, cleanedAccessToken,
                            cleanedShippingComName, orderId, cleanedShippingId,
                            cleanedShippingCom, cleanedShipmentNum,
                            cleanedUserDefined, cleanedMoreShipmentNum});
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