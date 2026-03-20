package com.order.main.dll;

import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.SQLOutput;
import java.util.List;
import java.util.ArrayList;

@Component
public class DllInitializer {
    private static boolean initialized = false;
    private static boolean xyInitialized = false;
    private static boolean kfzInitialized = false;
    private static boolean pddInitialized = false;
    private static boolean printInitialized = false;
    private static int defaultExcelManagerHandle = 0;

    private static String xyConfigPath = "/www/wwwroot/config/config.ini";
    //private static String xyConfigPath = "D:/zhishu/test/config.ini";


    @PostConstruct
    public void init() {
        try {
            System.out.println("正在加载 Excel Dll库...");
            SimpleDllLoader.loadDLL();
            // 创建默认的Excel管理器
            defaultExcelManagerHandle = SimpleDllLoader.createExcelManager();
            initialized = true;
            System.out.println("Excel Dll 库加载成功");

            System.out.println("正在加载 闲鱼 DLL库");
            XySimpleDllLoader.loadDLL();
            xyInitialized = true;
            System.out.println("闲鱼 DLL 库加载成功");

            System.out.println("正在加载 孔夫子 DLL库");
            KfzSimpleDllLoader.loadDLL();
            kfzInitialized = true;
            System.out.println("孔夫子 DLL 库加载成功");

            System.out.println("正在加载 拼多多 DLL库"); // 新增
            PddSimpleDllLoader.loadDLL(); // 新增
            pddInitialized = true; // 新增
            System.out.println("拼多多 DLL 库加载成功"); // 新增

            System.out.println("正在加载 打单 DLL库");
            PrintSimpleDllLoader.loadDLL();
            printInitialized = true;
            System.out.println("打单 DLL 库加载成功");
        } catch (Exception e) {
            System.err.println("Native 库加载失败: " + e.getMessage());
            throw new RuntimeException("Native 库加载失败", e);
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * 获取默认Excel管理器句柄
     */
    public static int getDefaultExcelManagerHandle() {
        if (!initialized) {
            throw new IllegalStateException("DLL未初始化");
        }
        return defaultExcelManagerHandle;
    }

    /**
     * 简化方法：读取Excel数据（使用默认管理器）
     */
    /**
     * 简化方法：读取Excel数据（使用默认管理器）
     */
    public static List readExcelData(String filename, String sheet) {
        try {
            System.out.println("[DllInitializer] 读取Excel: " + filename + ", sheet: " + sheet);

            // 清理文件路径
            filename = cleanFilePath(filename);

            List result = SimpleDllLoader.readExcelData(
                    getDefaultExcelManagerHandle(),
                    filename,
                    sheet
            );

            System.out.println("[DllInitializer] 读取成功，获取到 " + (result != null ? result.size() : 0) + " 行数据");

            return result != null ? result : new ArrayList<>();

        } catch (Exception e) {
            System.err.println("[DllInitializer] 读取Excel失败: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 执行孔夫子订单列表查询
     */
    public static String executeKongfzOrderList(int appId, String appSecret, String accessToken, String orderListJson) {
        if (!kfzInitialized) {
            throw new IllegalStateException("孔夫子 DLL库未初始化");
        }
        return KfzSimpleDllLoader.executeKongfzOrderList(appId, appSecret, accessToken, orderListJson);
    }

    /**
     *  获取单个订单信息
     * @param appId
     * @param appSecret
     * @param accessToken
     * @param userType
     * @param orderId
     * @return
     */
    public static String executeKongfzOrderGet(int appId, String appSecret, String accessToken,String userType,int orderId){
        if (!kfzInitialized) {
            throw new IllegalStateException("孔夫子 DLL库未初始化");
        }
        return KfzSimpleDllLoader.executeKongfzOrderGet(appId, appSecret, accessToken, userType, orderId);
    }

    /**
     * 简化方法：写入Excel数据（使用默认管理器）
     */
    public static boolean writeExcelData(String filename, String sheet,
                                         List<String> cells, List<String> values) {
        return SimpleDllLoader.writeBatchData(getDefaultExcelManagerHandle(),
                filename, sheet, cells, values);
    }

    /**
     * 简化方法： 续写excel数据
     */
    public static boolean appendExcelData(String filename, String sheet,List<String> values){
        return SimpleDllLoader.appendDataToExcel(getDefaultExcelManagerHandle(), filename, sheet, values);
    }

    /**
     * 简化方法：搜索关键词（使用默认管理器）
     */
    public static List<String> searchByKeyword(String filename, String sheet, String keyword) {
        return SimpleDllLoader.searchByKeyword(getDefaultExcelManagerHandle(),
                filename, sheet, keyword);
    }

    /**
     * 简化方法：创建并写入Excel文件（使用默认管理器）
     */
    public static boolean createExcelFile(String filename, String sheet,
                                          List<List<String>> data) {
        return SimpleDllLoader.createAndWriteExcel(getDefaultExcelManagerHandle(),
                filename, sheet, data);
    }

    /**
     * 清理文件路径
     */
    private static String cleanFilePath(String path) {
        if (path == null) return null;
        return path.replaceAll("[\\p{Cntrl}]", "").trim().replace('\\', '/');
    }

    /**
     * 在Spring应用关闭时清理资源
     */
    @PreDestroy
    public void cleanup() {
        if (defaultExcelManagerHandle != 0) {
            SimpleDllLoader.freeExcelManager(defaultExcelManagerHandle);
            defaultExcelManagerHandle = 0;
        }
        initialized = false;
        System.out.println("Excel DLL资源已清理");
    }

    /**
     * 执行商品上架
     */
    public static String executeGoodsPublish(String jsonData) {
        if (!xyInitialized) throw new IllegalStateException("闲鱼 DLL库未初始化");

        if (xyConfigPath != null) {
            xyConfigPath = cleanFilePath(xyConfigPath);
            System.out.println("使用配置文件路径: " + xyConfigPath);
        } else {
            System.out.println("警告：配置文件路径为空，将使用默认配置");
        }

        return XySimpleDllLoader.executeGoodsPublish(jsonData, xyConfigPath);
    }

    /**
     * 执行下架商品
     * @param jsonData
     * @return
     */
    public static String executeGoodsDownShelf(String jsonData) {
        if (!xyInitialized) throw new IllegalStateException("闲鱼 DLL库未初始化");

        if (xyConfigPath != null) {
            xyConfigPath = cleanFilePath(xyConfigPath);
            System.out.println("使用配置文件路径: " + xyConfigPath);
        } else {
            System.out.println("警告：配置文件路径为空，将使用默认配置");
        }

        return XySimpleDllLoader.executeGoodsDownShelf(jsonData, xyConfigPath);
    }

    /**
     * 闲鱼修改库存
     * @param jsonData
     * @return
     */
    public static String executeGoodsEditStock(String jsonData) {
        if (!xyInitialized) throw new IllegalStateException("闲鱼 DLL库未初始化");

        if (xyConfigPath != null) {
            xyConfigPath = cleanFilePath(xyConfigPath);
            System.out.println("使用配置文件路径: " + xyConfigPath);
        } else {
            System.out.println("警告：配置文件路径为空，将使用默认配置");
        }
        return XySimpleDllLoader.executeGoodsEditStock(jsonData, xyConfigPath);
    }

    /**
     * 执行孔夫子订单同步
     */
    public static String executeKongfzOrderSynchronization(int appId, String appSecret, String accessToken,
                                                           String shippingComName, int orderId, String shippingId,
                                                           String shippingCom, String shipmentNum,
                                                           String userDefined, String moreShipmentNum) {
        if (!kfzInitialized) {
            throw new IllegalStateException("孔夫子 DLL库未初始化");
        }
        return KfzSimpleDllLoader.executeKongfzOrderSynchronization(appId, appSecret, accessToken,
                shippingComName, orderId, shippingId, shippingCom, shipmentNum, userDefined, moreShipmentNum);
    }

    /**
     * 执行闲鱼订单同步
     */
    public static String executeXyOrderSynchronization(String jsonData) {
        if (!xyInitialized) throw new IllegalStateException("闲鱼 DLL库未初始化");

        if (xyConfigPath != null) {
            xyConfigPath = cleanFilePath(xyConfigPath);
            System.out.println("使用配置文件路径: " + xyConfigPath);
        } else {
            System.out.println("警告：配置文件路径为空，将使用默认配置");
        }

        return XySimpleDllLoader.executeXyOrderSynchronization(jsonData, xyConfigPath);
    }

    /**
     * 拉取订单
     * @param jsonData
     * @return
     */
    public static String xyGetOrderList(String jsonData){
        if (!xyInitialized) throw new IllegalStateException("闲鱼 DLL库未初始化");

        if (xyConfigPath != null) {
            xyConfigPath = cleanFilePath(xyConfigPath);
            System.out.println("使用配置文件路径: " + xyConfigPath);
        } else {
            System.out.println("警告：配置文件路径为空，将使用默认配置");
        }

        return XySimpleDllLoader.executeGetOrderList(jsonData, xyConfigPath);
    }

    /**
     * 执行拼多多订单同步
     */
    public static String executePddOrderSynchronization(String clientId, String clientSecret, String accessToken,
                                                        String logisticsCompany, String json) {
        if (!pddInitialized) {
            throw new IllegalStateException("拼多多 DLL库未初始化");
        }
        return PddSimpleDllLoader.executePddOrderSynchronization(clientId, clientSecret, accessToken,
                logisticsCompany, json);
    }

    /**
     * 查询订单列表
     * @param clientId
     * @param clientSecret
     * @param accessToken
     * @param json
     * @return
     */
    public static String executePddOrderBasicListGet(String clientId, String clientSecret, String accessToken, String json) {
        if (!pddInitialized) {
            throw new IllegalStateException("拼多多 DLL库未初始化");
        }
        return PddSimpleDllLoader.executePddOrderBasicListGet(clientId, clientSecret, accessToken,json);
    }


    /**
     * 韵达电子面单下单
     * requestJSON  创建订单参数JSON字符串
     * appKey       请求发起方应用密钥
     * appSecret    签名密钥
     */
    public static String ydCreateBmOrder(String requestJSON,String appKey,String appSecret) {
        if (!printInitialized) {
            throw new IllegalStateException("打单 DLL库未初始化");
        }
        return PrintSimpleDllLoader.ydCreateBmOrder(requestJSON,appKey,appSecret);
    }

    /**
     * 电子面单更新
     * @param requestJSON
     * @param appKey
     * @param appSecret
     * @return
     */
    public static String ydUpdateBmOrder(String requestJSON,String appKey,String appSecret) {
        if (!printInitialized) {
            throw new IllegalStateException("打单 DLL库未初始化");
        }
        return PrintSimpleDllLoader.ydUpdateBmOrder(requestJSON,appKey,appSecret);
    }

    /**
     * 韵达电子面单打印
     * requestJSON  打印参数JSON字符串
     * appKey       请求发起方应用密钥
     * appSecret    签名密钥
     */
    public static String ydBmGetPdfInfo(String requestJSON,String appKey,String appSecret) {
        if (!printInitialized) {
            throw new IllegalStateException("打单 DLL库未初始化");
        }
        return PrintSimpleDllLoader.ydBmGetPdfInfo(requestJSON,appKey,appSecret);
    }

    /**
     * 韵达电子面单取消
     * requestJSON  打印参数JSON字符串
     * appKey       请求发起方应用密钥
     * appSecret    签名密钥
     */
    public static String ydCancelBmOrder(String requestJSON,String appKey,String appSecret) {
        if (!printInitialized) {
            throw new IllegalStateException("打单 DLL库未初始化");
        }
        return PrintSimpleDllLoader.ydCancelBmOrder(requestJSON,appKey,appSecret);
    }

    /**
     * 电子面单余量查询接口
     * requestJSON  打印参数JSON字符串
     * appKey       请求发起方应用密钥
     * appSecret    签名密钥
     */
    public static String ydSearchCount(String requestJSON,String appKey,String appSecret) {
        if (!printInitialized) {
            throw new IllegalStateException("打单 DLL库未初始化");
        }
        return PrintSimpleDllLoader.ydSearchCount(requestJSON,appKey,appSecret);
    }
}