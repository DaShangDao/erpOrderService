package com.order.main.util;

import com.order.main.dll.DllInitializer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OrderUtils {

    // private static final String ORDER_EXCEL_LOG_PATH = "D:/zhishu/dll/";
    private static final String ORDER_EXCEL_LOG_PATH = "/www/orderLog/";

    /**
     * 订单状态转换
     * @param orderStatus
     * @return
     */
    public static Long xyGetOrderStatus(int orderStatus){
        /**
         * 1: 待付款 2：待发货，3：已发货待签收，4：交易成功 5： 已退款 6 ：交易关闭
         */
        switch (orderStatus){
            case 11:return 1L;
            case 12:return 2L;
            case 21:return 3L;
            case 22:return 4L;
            case 23:return 5L;
            case 24:return 6L;
            default:return Long.parseLong(orderStatus+"");
        }
    }

    /**
     * 拼多多获取订单状态（发货状态）
     * @param orderStatus
     * @return
     */
    public static Long pddGetOrderStatus(int orderStatus){
        /**
         * 1: 待付款 2：待发货，3：已发货待签收，4：交易成功 5： 已退款 6 ：交易关闭
         */
        switch (orderStatus){
            case 1:return 2L;
            case 2:return 3L;
            case 3:return 4L;
            default:return Long.parseLong(orderStatus+"");
        }
    }

    /**
     * 孔夫子获取订单状态
     * @param orderStatus
     * @return
     */
    public static List<Long> kfzGetOrderStatus(String orderStatus){
        /**
         * 孔夫子类型
         *         "Shipped-Returning": "申请退货中",                        : 售后 : 2 待商家处理
         *         "ReturnPending": "等待买家退货",
         *         "SellerCancelledBeforeConfirm": "卖家已取消",
         *         "PaidToShip": "等待卖家发货",
         *         "PaidToConfirm": "等待卖家收款",
         *         "ShippedReturned": "已退货",
         *         "ShippedToReceipt": "卖家已发货",
         *         "Shipped-RefundRejected": "拒绝退款中",
         *         "Paid-RefundRejected": "拒绝退款中",
         *         "ReturnedToReceipt": "卖家待收退货",
         *         "AdminClosedBeforeConfirm": "管理员关闭",
         *         "Shipped-ReturnRejected": "拒绝退货中",
         *         "BuyerCancelledBeforeConfirm": "买家已取消",
         *         "BuyerCancelledBeforePay": "买家已取消",
         *         "SellerClosedBeforePay": "卖家已取消",
         *         "PaidRefunded": "已退款",
         *         "SellerCancelledAfterPay": "已退款",
         *         "ShippedRefunded": "已退款",
         *         "Successful": "成功完成",
         *         "Shipped-Refunding": "申请退款中",                : 售后 : 2 待商家处理
         *         "ConfirmedToPay": "等待买家付款",
         *         "Paid-Refunding": "申请退款中",                   : 售后 : 2 待商家处理
         *         "Pending": "等待卖家确认"
         *
         */
        Long orderStatusLong = 0L;
        Long afterSalesStatusLong = 0L;
        switch (orderStatus){
            case "Trading":orderStatusLong =  1L;break;
            case "Pending":orderStatusLong =  7L;break;
            case "ConfirmedToPay":orderStatusLong =  1L;break;
            case "PaidToShip":orderStatusLong =  2L;break;
            case "PaidToConfirm":orderStatusLong =  2L;break;
            case "ShippedToReceipt":orderStatusLong =  3L;break;
            case "sellerReviewed":orderStatusLong =  6L;break;
            case "Successful":orderStatusLong =  4L;break;
            case "BuyerCancelled":orderStatusLong =  6L;break;
            case "SellerCancelledBeforeConfirm":orderStatusLong =  6L;break;
            case "AdminClosedBeforeConfirm":orderStatusLong =  6L;break;
            case "PaidRefunded":orderStatusLong =  10L;break;
            case "SellerCancelledAfterPay":orderStatusLong =  10L;break;
            case "ShippedRefunded":orderStatusLong =  10L;break;
            case "ReturnPending": orderStatusLong = 602L;break;
            case "ShippedReturned":orderStatusLong = 603L;break;
            case "ReturnedToReceipt":orderStatusLong = 603L;break;
            case "Shipped-RefundRejected":orderStatusLong = 605L;break;
            case "Paid-RefundRejected":orderStatusLong = 605L;break;
            case "Shipped-ReturnRejected" :orderStatusLong = 601L;break;
            case "BuyerCancelledBeforeConfirm":orderStatusLong = 11L;break;
            case "BuyerCancelledBeforePay":orderStatusLong = 11L;break;
            case "SellerClosedBeforePay" : orderStatusLong = 606L;break;
        }

        if(orderStatusLong == 0 && (orderStatus.equals("Shipped-Returning") || orderStatus.equals("Shipped-Refunding") || orderStatus.equals("Paid-Refunding"))){
            afterSalesStatusLong = 2L;
            orderStatusLong = 7L;
        }else if (orderStatusLong >= 10){
            afterSalesStatusLong = orderStatusLong;
            orderStatusLong = 7L;
        }

        List<Long> list = new ArrayList<>();
        list.add(orderStatusLong);
        list.add(afterSalesStatusLong);

        return list;
    }


    /**
     * 订单类型对应文本
     * @param orderStatus
     * @return
     */
    public static String xyGetOrderStatusTxt(Long orderStatus){
        /**
         * 1: 待付款 2：待发货，3：已发货待签收，4：交易成功 5： 已退款 6 ：交易关闭
         */
        if (orderStatus == 1L) {
            return "待付款";
        } else if (orderStatus == 2L) {
            return "待发货";
        } else if (orderStatus == 3L) {
            return "已发货待签收";
        } else if (orderStatus == 4L) {
            return "交易成功";
        } else if (orderStatus == 5L) {
            return "已退款";
        } else if (orderStatus == 6L) {
            return "交易关闭";
        } else if (orderStatus == 7L){
            return "售后处理中";
        }
        return "未知订单类型：" + orderStatus;
    }

    /**
     * 售后状态转换
     * @param afterSalesStatus  售后状态
     * @return
     */
    public static String xyGetAfterSalesStatusTxt(Long afterSalesStatus){
        /**
         * 售后状态 afterSalesStatus:
         *  2   待商家处理
         *  4   待买家退货
         *  603 待商家收货
         *  11  退款关闭
         *  4   退款成功
         *  6   已拒绝退款
         *  604 待确认退货地址
         */
        if (afterSalesStatus == 0L){
            return "无";
        }else if (afterSalesStatus == 2L) {
            return "待商家处理";
        } else if (afterSalesStatus == 4L) {
            return "待买家退货";
        } else if (afterSalesStatus == 603L) {
            return "待商家收货";
        } else if (afterSalesStatus == 11L) {
            return "退款关闭";
        } else if (afterSalesStatus == 10L) {
            return "退款成功";
        } else if (afterSalesStatus == 6L) {
            return "已拒绝退款";
        } else if (afterSalesStatus == 604L) {
            return "待确认退货地址";
        } else if (afterSalesStatus == 602L) {
            return "待买家退货";
        } else if (afterSalesStatus == 605L) {
            return "拒绝退款中";
        } else if (afterSalesStatus == 601L) {
            return "拒绝退货中";
        } else if (afterSalesStatus == 606L) {
            return "卖家已取消";
        }


        else{
            return "未知售后类型";
        }
    }

    public static Long getAfterSalesStatus(Integer refundStatus){
        Long afterSalesStatus = 0L;
        switch (refundStatus){
            //待商家处理
            case 1: afterSalesStatus =  2L; break;
            //待买家退货
            case 2: afterSalesStatus =  4L; break;
            //待商家收货
            case 3: afterSalesStatus = 603L; break;
            //退款关闭
            case 4: afterSalesStatus = 11L; break;
            //退款成功
            case 5: afterSalesStatus = 10L; break;
            //已拒绝退款
            case 6: afterSalesStatus = 6L; break;
            //待确认退货地址
            case 8: afterSalesStatus = 604L; break;
        }
        return afterSalesStatus;
    }

    /**
     * 拼多多获取售后状态文本
     * @param refundStatus 售后状态码
     * @return 对应的状态文本
     */
    public static String pddGetAfterSalesStatusTxt(Integer refundStatus){
        switch (refundStatus){
            case 0:
                return "无售后";
            case 2:
                return "买家申请退款，待商家处理";
            case 3:
                return "退货退款，待商家处理";
            case 4:
                return "商家同意退款，退款中";
            case 5:
                return "平台同意退款，退款中";
            case 6:
                return "驳回退款，待买家处理";
            case 7:
                return "已同意退货退款，待用户发货";
            case 8:
                return "平台处理中";
            case 9:
                return "平台拒绝退款，退款关闭";
            case 10:
                return "退款成功";
            case 11:
                return "买家撤销";
            case 12:
                return "买家逾期未处理，退款失败";
            case 13:
                return "买家逾期，超过有效期";
            case 14:
                return "换货补寄待商家处理";
            case 15:
                return "换货补寄待用户处理";
            case 16:
                return "换货补寄成功";
            case 17:
                return "换货补寄失败";
            case 18:
                return "换货补寄待用户确认完成";
            case 21:
                return "待商家同意维修";
            case 22:
                return "待用户确认发货";
            case 24:
                return "维修关闭";
            case 25:
                return "维修成功";
            case 27:
                return "待用户确认收货";
            case 31:
                return "已同意拒收退款，待用户拒收";
            case 32:
                return "补寄待商家发货";
            case 33:
                return "同意召回后退款，待商家召回";
            default:
                return "未知状态(" + refundStatus + ")";
        }
    }

    public static String getErpAfterSalesStatusTxt(String erpAfterSalesStatus){
        //0 无售后  1 待仓库处理  2  待仓库收货 3 售后完成  4 拒绝  5 延后处理
        switch (erpAfterSalesStatus){
            case "0":
                return "无售后";
            case "1":
            case "6":
                return "待仓库处理";
            case "2":
                return "待仓库收货";
            case "3":
                return "售后完成";
            case "4":
                return "拒绝";
            case "5":
                return "延后处理";
            default:
                return "未知售后状态(" + erpAfterSalesStatus + ")";
        }
    }

    /**
     * 创建订单操作日志方法
     * @param orderNo       订单号
     * @param updateTime    更新时间
     * @param log           日志内容
     * @param userName      操作人名称
     * @param userId        操作人id
     * @return
     */
    public static Boolean createOrderExcelLog(String orderNo, String updateTime, String log, String userName, String userId) {
        try {
            // 根据orderNo创建文件夹路径
            String folderPath = ORDER_EXCEL_LOG_PATH + orderNo + "/";
            String filePath = folderPath + orderNo + ".xlsx";

            // 创建文件夹（如果不存在）
            File folder = new File(folderPath);
            if (!folder.exists()) {
                boolean created = folder.mkdirs();
                if (!created) {
                    System.out.println("创建文件夹失败：" + folderPath);
                    return false;
                }
            }

            // 获取当前时间的秒级时间戳
            long time = System.currentTimeMillis() / 1000;
            List<List<String>> excelHeadList = new ArrayList<>();
            excelHeadList.add(Arrays.asList("updateTime", "log", "userName", "userId", "erpUpdateTime"));
            excelHeadList.add(Arrays.asList(updateTime, log, userName, userId, time + ""));

            return DllInitializer.createExcelFile(
                    filePath,
                    "订单操作记录",
                    excelHeadList
            );
        } catch (Exception e) {
            System.out.println("创建excel日志失败：" + ORDER_EXCEL_LOG_PATH + orderNo + "/" + orderNo + ".xlsx");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 追加日志信息方法
     * @param orderNo       订单号
     * @param updateTime    更新时间
     * @param log           日志内容
     * @param userName      操作人名称
     * @param userId        操作人id
     * @return
     */
    public static Boolean addToOrderExcelLog(String orderNo,String updateTime,String log,String userName,String userId){
        try{
            String folderPath = ORDER_EXCEL_LOG_PATH + orderNo + "/";
            String filePath = folderPath + orderNo + ".xlsx";

            //获取当前时间的秒级时间戳
            long time = System.currentTimeMillis() / 1000;
            List<String> row = Arrays.asList(updateTime,log, userName,userId,time+"");
            // 使用DllInitializer的简化方法追加数据
            return  DllInitializer.appendExcelData(
                    filePath,
                    "订单操作记录",
                    row
            );
        }catch (Exception e){
            System.out.println("续写日志错误");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取订单操作日志
     * @param orderNo   订单编号
     * @return
     */
    public static List getErpOrderLogExcel(String orderNo){
        String folderPath = ORDER_EXCEL_LOG_PATH + orderNo + "/";
        String filePath = folderPath + orderNo + ".xlsx";

        return DllInitializer.readExcelData(
                filePath,
                "订单操作记录"
        );
    }
}
