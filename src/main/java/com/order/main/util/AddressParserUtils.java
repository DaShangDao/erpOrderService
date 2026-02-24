package com.order.main.util;

import com.order.main.dto.AddressResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 中文地址解析工具类
 * 支持：直辖市、普通省份、港澳台地址解析
 */
@Slf4j
public class AddressParserUtils {

    // 直辖市列表
    private static final Set<String> MUNICIPALITIES = new HashSet<>(Arrays.asList(
            "北京", "上海", "天津", "重庆"
    ));

    // 自治区列表
    private static final Set<String> AUTONOMOUS_REGIONS = new HashSet<>(Arrays.asList(
            "新疆维吾尔自治区", "广西壮族自治区", "宁夏回族自治区", "西藏自治区", "内蒙古自治区"
    ));

    // 自治区简称映射
    private static final Map<String, String> AUTONOMOUS_REGION_SHORT = new HashMap<String, String>() {{
        put("新疆", "新疆维吾尔自治区");
        put("广西", "广西壮族自治区");
        put("宁夏", "宁夏回族自治区");
        put("西藏", "西藏自治区");
        put("内蒙古", "内蒙古自治区");
    }};

    // 港澳台地区
    private static final Set<String> SPECIAL_REGIONS = new HashSet<>(Arrays.asList(
            "香港", "澳门", "台湾"
    ));

    /**
     * 核心地址解析方法 - 一个方法搞定所有地址解析
     * @param address 原始地址字符串
     * @return AddressResultDto 解析结果
     */
    public static AddressResultDto parseAddress(String address) {
        AddressResultDto result = new AddressResultDto();
        result.setFullAddress(address);

        if (!StringUtils.hasText(address)) {
            return result;
        }

        try {
            String trimmedAddress = address.trim();
            result.setFullAddress(trimmedAddress);

            // 主要处理逻辑
            String remaining = processMunicipality(result, trimmedAddress);
            remaining = processProvince(result, remaining);
            remaining = processCity(result, remaining);
            remaining = processDistrict(result, remaining);
            processStreetAndDetail(result, remaining);

        } catch (Exception e) {
            log.error("地址解析失败: {}", address, e);
            // 解析失败时，将整个地址放到详细地址中
            result.setDetail(address);
        }

        return result;
    }

    /**
     * 处理直辖市
     */
    private static String processMunicipality(AddressResultDto result, String address) {
        for (String muni : MUNICIPALITIES) {
            if (address.startsWith(muni)) {
                result.setProvince(muni + "市");
                result.setCity(muni + "市");

                // 去掉直辖市名称
                String remaining = address.substring(muni.length());

                // 处理"上海市上海市"这种情况
                if (remaining.startsWith("市")) {
                    remaining = remaining.substring(1);
                }
                // 处理"上海市上海"这种情况
                if (remaining.startsWith(muni)) {
                    remaining = remaining.substring(muni.length());
                }

                return remaining;
            }
        }
        return address;
    }

    /**
     * 处理省份
     */
    private static String processProvince(AddressResultDto result, String address) {
        // 如果已经设置了直辖市，直接返回
        if (StringUtils.hasText(result.getProvince())) {
            return address;
        }

        // 检查自治区
        for (Map.Entry<String, String> entry : AUTONOMOUS_REGION_SHORT.entrySet()) {
            if (address.startsWith(entry.getKey())) {
                result.setProvince(entry.getValue());
                return address.substring(entry.getKey().length());
            }
        }

        // 检查完整的自治区
        for (String region : AUTONOMOUS_REGIONS) {
            if (address.startsWith(region)) {
                result.setProvince(region);
                return address.substring(region.length());
            }
        }

        // 检查普通省份
        int provinceEnd = address.indexOf("省");
        if (provinceEnd > 0) {
            String province = address.substring(0, provinceEnd + 1);
            result.setProvince(province);
            return address.substring(provinceEnd + 1);
        }

        return address;
    }

    /**
     * 处理城市
     */
    private static String processCity(AddressResultDto result, String address) {
        // 如果已经设置了直辖市，直接返回
        if (StringUtils.hasText(result.getCity())) {
            return address;
        }

        int cityEnd = address.indexOf("市");
        if (cityEnd > 0) {
            String city = address.substring(0, cityEnd + 1);
            result.setCity(city);
            return address.substring(cityEnd + 1);
        }

        return address;
    }

    /**
     * 处理区县
     */
    private static String processDistrict(AddressResultDto result, String address) {
        // 优先查找"区"
        int districtEnd = address.indexOf("区");
        if (districtEnd > 0) {
            String district = address.substring(0, districtEnd + 1);
            result.setDistrict(district);
            return address.substring(districtEnd + 1);
        }

        // 查找"县"
        int countyEnd = address.indexOf("县");
        if (countyEnd > 0) {
            String county = address.substring(0, countyEnd + 1);
            result.setDistrict(county);
            return address.substring(countyEnd + 1);
        }

        // 查找"旗"（内蒙古等地）
        int flagEnd = address.indexOf("旗");
        if (flagEnd > 0) {
            String flag = address.substring(0, flagEnd + 1);
            result.setDistrict(flag);
            return address.substring(flagEnd + 1);
        }

        return address;
    }

    /**
     * 处理街道和详细地址
     */
    private static void processStreetAndDetail(AddressResultDto result, String address) {
        if (!StringUtils.hasText(address)) {
            result.setDetail("");
            return;
        }

        // 常见的街道/乡镇后缀
        String[] streetSuffixes = {"镇", "乡", "街道", "办事处", "路", "街", "巷", "道"};

        for (String suffix : streetSuffixes) {
            int index = address.indexOf(suffix);
            if (index > 0) {
                // 往前找2-6个字符作为街道名
                int start = Math.max(0, index - 6);
                String street = address.substring(start, index + suffix.length());
                result.setStreet(street);

                String detail = address.substring(index + suffix.length()).trim();
                result.setDetail(detail);
                return;
            }
        }

        // 如果没有找到街道信息，全部作为详细地址
        result.setDetail(address);
    }

    /**
     * 简单实用的解析方法 - 专门为替换你原来的代码设计
     * 直接返回你需要的五个字段：province, city, country, town, detail
     */
    public static Map<String, String> parseOriginalFormat(String address) {
        Map<String, String> result = new HashMap<>();

        // 初始化默认值
        result.put("province", "");
        result.put("city", "");
        result.put("country", ""); // 注意：这是区县，不是国家
        result.put("town", "");
        result.put("detail", "");

        try {
            // 使用核心解析方法
            AddressResultDto dto = parseAddress(address);

            // 映射到原来的字段名
            result.put("province", dto.getProvince() != null ? dto.getProvince() : "");
            result.put("city", dto.getCity() != null ? dto.getCity() : "");
            result.put("country", dto.getDistrict() != null ? dto.getDistrict() : ""); // district -> country
            result.put("town", dto.getStreet() != null ? dto.getStreet() : "");
            result.put("detail", dto.getDetail() != null ? dto.getDetail() : "");

            // 特殊情况处理：如果详细地址为空，使用完整地址
            if (!StringUtils.hasText(result.get("detail"))) {
                result.put("detail", address);
            }

        } catch (Exception e) {
            log.error("地址解析异常: {}", address, e);
            result.put("detail", address);
        }

        return result;
    }
}