package com.order.main.dll;

import cn.hutool.core.lang.func.Func;
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

public class PrintSimpleDllLoader {

    private static NativeLibrary nativeLibrary;


    /**
     * 韵达
     */
    // 韵达电子面单下单
    private static Function ydCreateBmOrderFunc;
    // 韵达更新电子面单
    private static Function ydUpdateBmOrderFunc;
    // 韵达获取pdf文件
    private static Function ydBmGetPdfInfoFunc;
    // 韵达电子面单取消
    private static Function ydCancelBmOrderFunc;
    // 韵达电子面单余量查询接口
    private static Function ydSearchCountFunc;

    /**
     * 中通
     */
    // 创建订单
    private static Function ztoOpenCreateOrderFunc;
    //中通快递--取消订单接口
    private static Function ztoOpenCancelPreOrderFunc;
    //中通快递--查询订单接口
    private static Function ztoOpenGetOrderInfoFunc;
    //中通快递--获取打单余额
    private static Function ztoOpenQueryAvailableBalanceNewFunc;
    //中通快递--请求生成面单图片/PDF
    private static Function ztoOpenOrderPrintFunc;
    //中通快递--绑定电子面单
    private static Function ztoOpenBindingEaccountFunc;

    /**
     * 极兔
     */
    // 电子面单账号检验
    private static Function jtVipCheckCusPwdFunc;
    // 电子面单账号余额查询
    private static Function jtEssBalanceFunc;
    // 创建订单
    private static Function jtV2AddOrderFunc;
    // 查询订单
    private static Function jtOrderGetOrdersFunc;
    // 取消订单
    private static Function jtOrderCancelOrderFunc;


    private static Function freeCStringFunc;

    @Setter
    private static NativeLibConfig nativeLibConfig;


    public static void loadDLL() throws Exception {
        String libraryPath = getLibraryPath();
        System.out.println("正在加载 expressDeliveryOrder native 库: " + libraryPath);

        File libraryFile = new File(libraryPath);

        // 验证库文件是否存在
        if (!libraryFile.exists()) {
            throw new FileNotFoundException("expressDeliveryOrder Native 库文件不存在: " + libraryPath);
        }

        // 验证文件大小
        System.out.println("expressDeliveryOrder 库文件大小: " + libraryFile.length() + " bytes");
        if (libraryFile.length() == 0) {
            throw new IOException("expressDeliveryOrder 库文件为空: " + libraryPath);
        }

        // 在 Linux 系统上设置执行权限
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            boolean success = libraryFile.setExecutable(true);
            System.out.println("设置 expressDeliveryOrder 库执行权限: " + (success ? "成功" : "失败"));
        }

        // 加载库文件
        nativeLibrary = NativeLibrary.getInstance(libraryFile.getAbsolutePath());

        /**
         * 韵达
         */
        // 韵达电子面单下单
        ydCreateBmOrderFunc = nativeLibrary.getFunction("YdCreateBmOrder");
        // 韵达电子面单更新
        ydUpdateBmOrderFunc = nativeLibrary.getFunction("YdUpdateBmOrder");
        // 韵达电子面单打印
        ydBmGetPdfInfoFunc = nativeLibrary.getFunction("YdBmGetPdfInfo");
        // 电子面单取消
        ydCancelBmOrderFunc = nativeLibrary.getFunction("YdCancelBmOrder");
        // 电子面单余量查询接口
        ydSearchCountFunc = nativeLibrary.getFunction("YdSearchCount");

        /**
         * 中通
         */
        // 中通快递--获取打单余额
        ztoOpenQueryAvailableBalanceNewFunc = nativeLibrary.getFunction("ZtoOpenQueryAvailableBalanceNew");
        // 中通快递--创建订单
        ztoOpenCreateOrderFunc = nativeLibrary.getFunction("ZtoOpenCreateOrder");
        // 中通快递--取消订单接口
        ztoOpenCancelPreOrderFunc = nativeLibrary.getFunction("ZtoOpenCancelPreOrder");
        // 中通快递--查询订单接口
        ztoOpenGetOrderInfoFunc = nativeLibrary.getFunction("ZtoOpenGetOrderInfo");
        // 中通快递--请求生成面单图片/PDF
        ztoOpenOrderPrintFunc = nativeLibrary.getFunction("ZtoOpenOrderPrint");
        // 绑定电子面单
        ztoOpenBindingEaccountFunc = nativeLibrary.getFunction("ZtoOpenBindingEaccount");;

        /**
         * 极兔
         */
        // 极兔快递--电子面单账号检验
//        jtVipCheckCusPwdFunc = nativeLibrary.getFunction("JtVipCheckCusPwd");
//        // 极兔快递--电子面单账号余额查询
//        jtEssBalanceFunc = nativeLibrary.getFunction("JtEssBalance");
//        // 极兔快递--创建订单
//        jtV2AddOrderFunc = nativeLibrary.getFunction("JtV2AddOrder");
//        // 极兔快递--查询订单
//        jtOrderGetOrdersFunc = nativeLibrary.getFunction("JtOrderGetOrders");
//        // 极兔快递--取消订单
//        jtOrderCancelOrderFunc = nativeLibrary.getFunction("JtOrderCancelOrder");

        // 释放c串内存
        freeCStringFunc = nativeLibrary.getFunction("FreeCString");

        /**
         * 韵达
         */
        if (ydCreateBmOrderFunc == null) throw new Exception("无法找到 ydCreateBmOrderFunc 函数");
        if (ydUpdateBmOrderFunc == null) throw new Exception("无法找到 ydUpdateBmOrderFunc 函数");
        if (ydBmGetPdfInfoFunc == null) throw new Exception("无法找到 YdBmGetPdfInfoFunc 函数");
        if (ydCancelBmOrderFunc == null) throw new Exception("无法找到 ydCancelBmOrderFunc 函数");
        if (ydSearchCountFunc == null) throw new Exception("无法找到 ydSearchCountFunc 函数");
        /**
         * 中通
         */
        if (ztoOpenQueryAvailableBalanceNewFunc == null) throw new Exception("无法找到 ztoOpenQueryAvailableBalanceNewFunc 函数");
        if (ztoOpenCreateOrderFunc == null) throw new Exception("无法找到 ztoOpenCreateOrderFunc 函数");
        if (ztoOpenCancelPreOrderFunc == null) throw new Exception("无法找到 ztoOpenCancelPreOrderFunc 函数");
        if (ztoOpenGetOrderInfoFunc == null) throw new Exception("无法找到 ztoOpenGetOrderInfoFunc 函数");
        if (ztoOpenOrderPrintFunc == null) throw new Exception("无法找到 ztoOpenOrderPrintFunc 函数");
        if(ztoOpenBindingEaccountFunc == null) throw new Exception("无法找到 ZtoOpenBindingEaccount 函数");
        /**
         * 极兔
         */
//        if (jtVipCheckCusPwdFunc == null) throw new Exception("无法找到 jtVipCheckCusPwdFunc 函数");
//        if (jtEssBalanceFunc == null) throw new Exception("无法找到 JtEssBalance 函数");
//        if (jtV2AddOrderFunc == null) throw new Exception("无法找到 JtV2AddOrder 函数");
//        if (jtOrderGetOrdersFunc == null) throw new Exception("无法找到 JtOrderGetOrders 函数");
//        if (jtOrderCancelOrderFunc == null) throw new Exception("无法找到 JtOrderCancelOrder 函数");



        if (freeCStringFunc == null) throw new Exception("无法找到 FreeCString 函数");

        System.out.println("expressDeliveryOrder Native 库加载成功: " + libraryPath);
    }

    /**
     * 获取库文件路径
     */
    private static String getLibraryPath() throws IOException {
        // 优先使用外部路径
        String externalPath = nativeLibConfig.getPrintSimple();
        File externalFile = new File(externalPath);

        if (externalFile.exists()) {
            System.out.println("使用 expressDeliveryOrder 外部库文件: " + externalPath);
            return externalPath;
        }

        // 如果外部文件不存在，回退到原来的资源提取方式
        System.out.println("expressDeliveryOrder 外部库文件不存在，回退到资源提取方式: " + externalPath);
        return extractLibraryFromResources(getPlatformLibraryName()).getAbsolutePath();
    }

    /**
     * 根据操作系统获取对应的库文件名（备用方案）
     */
    private static String getPlatformLibraryName() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            return "expressDeliveryOrder.dll";
        } else if (osName.contains("linux")) {
            return "expressDeliveryOrder.so";
        } else if (osName.contains("mac")) {
            return "expressDeliveryOrder.dylib";
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
            throw new IOException("expressDeliveryOrder Native 库未找到: " + resourcePath);
        }

        File tempFile = File.createTempFile("expressDeliveryOrder_native_", getLibrarySuffix(libraryName));
        tempFile.deleteOnExit();

        System.out.println("从资源提取 expressDeliveryOrder 库文件: " + resourcePath + " -> " + tempFile.getAbsolutePath());

        try (InputStream is = inputStream) {
            Files.copy(is, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        // 设置执行权限
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            boolean success = tempFile.setExecutable(true);
            System.out.println("设置 expressDeliveryOrder 库执行权限: " + (success ? "成功" : "失败"));
        }

        System.out.println("expressDeliveryOrder 库文件提取完成，大小: " + tempFile.length() + " bytes");
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
     * 韵达电子面单下单
     * requestJSON  创建订单参数JSON字符串
     * appKey       请求发起方应用密钥
     * appSecret    签名密钥
     */
    public static String ydCreateBmOrder(String requestJSON,String appKey,String appSecret) {
        try {
            String cleanedRequestJSON = ensureUtf8(requestJSON);
            String cleanedAppKey = ensureUtf8(appKey);
            String cleanedAppSecret = ensureUtf8(appSecret);

            Object result = ydCreateBmOrderFunc.invoke(Pointer.class,
                    new Object[]{cleanedRequestJSON, cleanedAppKey, cleanedAppSecret});
            return ptrToString((Pointer) result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 韵达电子面单更新
     * requestJSON  更新订单参数JSON字符串
     * appKey       请求发起方应用密钥
     * appSecret    签名密钥
     */
    public static String ydUpdateBmOrder(String requestJSON,String appKey,String appSecret) {
        try {
            String cleanedRequestJSON = ensureUtf8(requestJSON);
            String cleanedAppKey = ensureUtf8(appKey);
            String cleanedAppSecret = ensureUtf8(appSecret);

            Object result = ydUpdateBmOrderFunc.invoke(Pointer.class,
                    new Object[]{cleanedRequestJSON, cleanedAppKey, cleanedAppSecret});
            return ptrToString((Pointer) result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 韵达电子面单打印
     * requestJSON  打印参数JSON字符串
     * appKey       请求发起方应用密钥
     * appSecret    签名密钥
     */
    public static String ydBmGetPdfInfo(String requestJSON,String appKey,String appSecret) {
        try {
            String cleanedRequestJSON = ensureUtf8(requestJSON);
            String cleanedAppKey = ensureUtf8(appKey);
            String cleanedAppSecret = ensureUtf8(appSecret);
            Object result = ydBmGetPdfInfoFunc.invoke(Pointer.class,
                    new Object[]{cleanedRequestJSON, cleanedAppKey, cleanedAppSecret});
            return ptrToString((Pointer) result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 韵达电子面单取消
     * requestJSON  打印参数JSON字符串
     * appKey       请求发起方应用密钥
     * appSecret    签名密钥
     */
    public static String ydCancelBmOrder(String requestJSON,String appKey,String appSecret) {
        try {
            String cleanedRequestJSON = ensureUtf8(requestJSON);
            String cleanedAppKey = ensureUtf8(appKey);
            String cleanedAppSecret = ensureUtf8(appSecret);
            Object result = ydCancelBmOrderFunc.invoke(Pointer.class,
                    new Object[]{cleanedRequestJSON, cleanedAppKey, cleanedAppSecret});
            return ptrToString((Pointer) result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }


    /**
     * 电子面单余量查询接口
     * requestJSON  打印参数JSON字符串
     * appKey       请求发起方应用密钥
     * appSecret    签名密钥
     */
    public static String ydSearchCount(String requestJSON,String appKey,String appSecret) {
        try {
            String cleanedRequestJSON = ensureUtf8(requestJSON);
            String cleanedAppKey = ensureUtf8(appKey);
            String cleanedAppSecret = ensureUtf8(appSecret);
            Object result = ydSearchCountFunc.invoke(Pointer.class,
                    new Object[]{cleanedRequestJSON, cleanedAppKey, cleanedAppSecret});
            return ptrToString((Pointer) result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 中通DLL方法
     */
    public static String executeZTOApi(String api,String appKey, String appSecret,String json){
        try {
            String cleanedAppKey = ensureUtf8(appKey);
            String cleanedAppSecret = ensureUtf8(appSecret);
            String cleanedJson = ensureUtf8(json);
            Object result = null;
            if (api.equals("ZtoOpenQueryAvailableBalanceNew")){
                // 获取打单余额
                result = ztoOpenQueryAvailableBalanceNewFunc.invoke(Pointer.class,
                        new Object[]{cleanedJson,cleanedAppKey,cleanedAppSecret});
            }else if (api.equals("ZtoOpenCreateOrder")){
                // 创建订单
                result = ztoOpenCreateOrderFunc.invoke(Pointer.class,
                        new Object[]{cleanedJson,cleanedAppKey,cleanedAppSecret});
            }else if (api.equals("ZtoOpenCancelPreOrder")){
                // 取消订单接口
                result = ztoOpenCancelPreOrderFunc.invoke(Pointer.class,
                        new Object[]{cleanedJson,cleanedAppKey,cleanedAppSecret});
            }else if (api.equals("ZtoOpenGetOrderInfo")){
                // 查询订单接口
                result = ztoOpenGetOrderInfoFunc.invoke(Pointer.class,
                        new Object[]{cleanedJson,cleanedAppKey,cleanedAppSecret});
            }else if (api.equals("ZtoOpenOrderPrint")){
                // 请求生成面单图片/PDF
                result = ztoOpenOrderPrintFunc.invoke(Pointer.class,
                        new Object[]{cleanedJson,cleanedAppKey,cleanedAppSecret});
            }else if(api.equals("ZtoOpenBindingEaccount")){
                // 绑定电子面单
                result = ztoOpenBindingEaccountFunc.invoke(Pointer.class,
                        new Object[]{cleanedJson,cleanedAppKey,cleanedAppSecret});
            }
            return ptrToString((Pointer) result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }


    /**
     * 极兔DLL方法
     * @param api
     * @param apiAccount
     * @param privateKey
     * @param json
     * @return
     */
    public static String execteJtApi(String api,String apiAccount,String privateKey,String json){
        try {
            String cleanedApiAccount = ensureUtf8(apiAccount);
            String cleanedPrivateKey = ensureUtf8(privateKey);
            String cleanedJson = ensureUtf8(json);
            Object result = null;
            if (api.equals("JtVipCheckCusPwd")){
                // 电子面单账号检验
                result = jtVipCheckCusPwdFunc.invoke(Pointer.class,
                        new Object[]{cleanedJson,cleanedApiAccount,cleanedPrivateKey});
            }else if (api.equals("JtEssBalance")){
                // 电子面单账号余额查询
                result = jtEssBalanceFunc.invoke(Pointer.class,
                        new Object[]{cleanedJson,cleanedApiAccount,cleanedPrivateKey});
            }else if (api.equals("JtV2AddOrder")){
                // 创建订单
                result = jtV2AddOrderFunc.invoke(Pointer.class,
                        new Object[]{cleanedJson,cleanedApiAccount,cleanedPrivateKey});
            }else if (api.equals("JtOrderGetOrders")){
                // 查询订单
                result = jtOrderGetOrdersFunc.invoke(Pointer.class,
                        new Object[]{cleanedJson,cleanedApiAccount,cleanedPrivateKey});
            }else if (api.equals("JtOrderCancelOrder")){
                // 取消订单
                result = jtOrderCancelOrderFunc.invoke(Pointer.class,
                        new Object[]{cleanedJson,cleanedApiAccount,cleanedPrivateKey});
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