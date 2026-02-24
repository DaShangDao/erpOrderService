package com.order.main.dll;

import com.sun.jna.Function;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class XySimpleDllLoader {

    private static NativeLibrary nativeLibrary;
    //创建商品
    private static Function executeGoodsCreatFunc;
    //上架商品
    private static Function executeGoodsPublishFunc; // 新增
    //下架商品
    private static Function executeGoodsDownShelfFunc;
    //修改价格
    private static Function executeGoodsEditPriceFunc;
    //修改库存
    private static Function executeGoodsEditStockFunc;
    //获取店铺信息
    private static Function executeSelectGoodsListPriceFunc;
    //擦亮商品
    private static Function executeGoodsFlashFunc;
    //订单快递单号同步
    private static Function executeXyOrderSynchronizationFunc; // 新增
    //查询订单列表
    private static Function executeGetOrderListFunc;

    private static Function freeCStringFunc;

    private static Function executeGetGoodsDetailFunc;

    // 外部库文件路径
    private static final String EXTERNAL_LIB_PATH = "/www/wwwroot/config/xy.so";
    // private static final String EXTERNAL_LIB_PATH = "D:/zhishu/dll/xy.dll";

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

        //创建商品
        executeGoodsCreatFunc = nativeLibrary.getFunction("ExecuteGoodsCreat");
        //上架商品
        executeGoodsPublishFunc = nativeLibrary.getFunction("ExecuteGoodsPublish");
        //下架商品
        executeGoodsDownShelfFunc = nativeLibrary.getFunction("ExecuteGoodsDownShelf");
        //修改价格
        executeGoodsEditPriceFunc = nativeLibrary.getFunction("ExecuteGoodsEditPrice");
        //修改库存
        executeGoodsEditStockFunc = nativeLibrary.getFunction("ExecuteGoodsEditStock");
        //获取商品数据
        executeSelectGoodsListPriceFunc = nativeLibrary.getFunction("ExecuteSelectGoodsListPrice");
        //擦亮商品
        executeGoodsFlashFunc = nativeLibrary.getFunction("ExecuteGoodsFlash");
        //获取商品详情
        executeGetGoodsDetailFunc = nativeLibrary.getFunction("ExecuteGetGoodsDetail");
        //订单快递单号同步
        executeXyOrderSynchronizationFunc = nativeLibrary.getFunction("ExecuteXyOrderSynchronization"); // 新增
        //查询订单列表
        executeGetOrderListFunc = nativeLibrary.getFunction("ExecuteGetOrderList");
        //释放c串内存
        freeCStringFunc = nativeLibrary.getFunction("FreeCString");

        if (executeGoodsCreatFunc == null) throw new Exception("无法找到 ExecuteGoodsCreat 函数");
        if (executeGoodsPublishFunc == null) throw new Exception("无法找到 ExecuteGoodsPublish 函数");
        if (executeGoodsDownShelfFunc == null) throw new Exception("无法找到 ExecuteGoodsDownShelf 函数");
        if (executeGoodsEditPriceFunc == null) throw new Exception("无法找到 ExecuteGoodsEditPrice 函数");
        if (executeGoodsEditStockFunc == null) throw new Exception("无法找到 ExecuteGoodsEditStock 函数");
        if (executeSelectGoodsListPriceFunc == null) throw new Exception("无法找到 ExecuteSelectGoodsListPrice 函数");
        if (executeGoodsFlashFunc == null) throw new Exception("无法找到 ExecuteGoodsFlash 函数");
        if (executeXyOrderSynchronizationFunc == null) throw new Exception("无法找到 ExecuteXyOrderSynchronization 函数");
        if (freeCStringFunc == null) throw new Exception("无法找到 FreeCString 函数");
        if (executeGetOrderListFunc == null) throw new Exception("无法找到 ExecuteGetOrderList 函数");
        if (executeGetGoodsDetailFunc == null) throw new Exception("无法找到 ExecuteGetGoodsDetail 函数");

        System.out.println("Native 库加载成功: " + libraryPath);
    }

    /**
     * 执行商品发布（带配置文件路径）
     */
    public static String executeGoodsPublish(String jsonData, String configPath) {
        try {
            String cleanedJson = ensureUtf8(jsonData);
            String cleanedConfigPath = ensureUtf8(configPath);

            Object result = executeGoodsPublishFunc.invoke(Pointer.class,
                    new Object[]{cleanedJson, cleanedConfigPath});
            return ptrToString((Pointer) result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 下架商品
     */
    public static String executeGoodsDownShelf(String jsonData, String configPath) {
        try {
            String cleanedJson = ensureUtf8(jsonData);
            String cleanedConfigPath = ensureUtf8(configPath);

            Object result = executeGoodsDownShelfFunc.invoke(Pointer.class,
                    new Object[]{cleanedJson, cleanedConfigPath});
            return ptrToString((Pointer) result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 修改价格
     */
    public static String executeGoodsEditPrice(String jsonData, String configPath) {
        try {
            String cleanedJson = ensureUtf8(jsonData);
            String cleanedConfigPath = ensureUtf8(configPath);

            Object result = executeGoodsEditPriceFunc.invoke(Pointer.class,
                    new Object[]{cleanedJson, cleanedConfigPath});
            return ptrToString((Pointer) result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 修改库存
     */
    public static String executeGoodsEditStock(String jsonData, String configPath) {
        try {
            String cleanedJson = ensureUtf8(jsonData);
            String cleanedConfigPath = ensureUtf8(configPath);

            Object result = executeGoodsEditStockFunc.invoke(Pointer.class,
                    new Object[]{cleanedJson, cleanedConfigPath});
            return ptrToString((Pointer) result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 获取商品数据
     */
    public static String executeSelectGoodsListPrice(String jsonData, String configPath) {
        try {
            String cleanedJson = ensureUtf8(jsonData);
            String cleanedConfigPath = ensureUtf8(configPath);

            Object result = executeSelectGoodsListPriceFunc.invoke(Pointer.class,
                    new Object[]{cleanedJson, cleanedConfigPath});
            return ptrToString((Pointer) result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 擦亮商品
     */
    public static String executeGoodsFlash(String jsonData, String configPath) {
        try {
            String cleanedJson = ensureUtf8(jsonData);
            String cleanedConfigPath = ensureUtf8(configPath);

            Object result = executeGoodsFlashFunc.invoke(Pointer.class,
                    new Object[]{cleanedJson, cleanedConfigPath});
            return ptrToString((Pointer) result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public static String executeGetGoodsDetail(String jsonData, String configPath) {
        try {
            String cleanedJson = ensureUtf8(jsonData);
            String cleanedConfigPath = ensureUtf8(configPath);

            Object result = executeGetGoodsDetailFunc.invoke(Pointer.class,
                    new Object[]{cleanedJson, cleanedConfigPath});
            return ptrToString((Pointer) result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }


    /**
     * 获取库文件路径
     */
    private static String getLibraryPath() throws IOException {
        // 优先使用外部路径
        String externalPath = EXTERNAL_LIB_PATH;
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
            return "xy.dll";
        } else if (osName.contains("linux")) {
            return "xy.so";
        } else if (osName.contains("mac")) {
            return "xy.dylib";
        } else {
            throw new UnsupportedOperationException("不支持的操作系统: " + osName);
        }
    }

    /**
     * 执行闲鱼订单同步
     */
    public static String executeXyOrderSynchronization(String jsonData, String configPath) {
        try {
            String cleanedJson = ensureUtf8(jsonData);
            String cleanedConfigPath = ensureUtf8(configPath);

            Object result = executeXyOrderSynchronizationFunc.invoke(Pointer.class,
                    new Object[]{cleanedJson, cleanedConfigPath});
            return ptrToString((Pointer) result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
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
     * 执行商品创建（带配置文件路径）
     */
    public static String executeGoodsCreat(String jsonData, String configPath) {
        try {
            String cleanedJson = ensureUtf8(jsonData);
            String cleanedConfigPath = ensureUtf8(configPath);

            Object result = executeGoodsCreatFunc.invoke(Pointer.class,
                    new Object[]{cleanedJson, cleanedConfigPath});
            return ptrToString((Pointer) result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 查询订单列表
     * @param jsonData
     * @param configPath
     * @return
     */
    public static String executeGetOrderList(String jsonData, String configPath){
        try {
            String cleanedJson = ensureUtf8(jsonData);
            String cleanedConfigPath = ensureUtf8(configPath);

            Object result = executeGetOrderListFunc.invoke(Pointer.class,
                    new Object[]{cleanedJson, cleanedConfigPath});
            return ptrToString((Pointer) result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 执行商品创建（使用默认配置）
     */
    public static String executeGoodsCreat(String jsonData) {
        try {
            String cleanedJson = ensureUtf8(jsonData);
            Object result = executeGoodsCreatFunc.invoke(Pointer.class,
                    new Object[]{cleanedJson, null});
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