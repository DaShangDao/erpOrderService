package com.order.main.dll;

import com.pdd.pop.sdk.common.util.JsonUtil;
import com.sun.jna.Function;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.jna.ptr.PointerByReference;

public class SimpleDllLoader {

    private static NativeLibrary nativeLibrary;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 外部库文件路径
    private static final String EXTERNAL_LIB_PATH = "/www/wwwroot/config/excel.so";
    // private static final String EXTERNAL_LIB_PATH = "D:/zhishu/dll/excel.dll";

    // 定义Go DLL接口
    public interface ExcelDLL extends Library {
        // 创建新的Excel管理器并返回指针
        int NewExcelManagerInstance();

        // 释放Excel管理器
        void FreeExcelManager(int handle);

        // 读取Excel数据
        int ReadExcelData(int handle, String filename, String sheet, PointerByReference result);

        // 批量写入数据到Excel文件
        int WriteBatchData(int handle, String filename, String sheet,
                           String cellsJson, String valuesJson, int count);

        // 追加数据到Excel文件末尾
        int AppendDataToExcel(int handle, String filename, String sheet,
                              String valuesJson, int count);

        // 搜索包含关键字的单元格
        int SearchByKeyword(int handle, String filename, String sheet,
                            String keyword, Pointer result);

        // 搜索整行包含关键字的行
        int SearchRowsByKeyword(int handle, String filename, String sheet,
                                String keyword, Pointer result);

        // 创建新文件并写入数据
        int CreateAndWriteExcel(int handle, String filename, String sheet,
                                String rowsDataJson);

        // 增强版合并Excel文件（支持指定文件列表）
        int MergeExcelFilesEx(int handle, String sourceDir, String specificFilesJson,
                              String outputFile, String sheetName, int mergeByColumn,
                              int includeHeaders, int skipEmptyRows, String filePattern,
                              String sourceSheet, int addSourceColumn, int addIndexColumn);

        // 并行合并Excel文件（增强版）
        int MergeExcelFilesParallelEx(int handle, String sourceDir,
                                      Pointer specificFiles, int fileCount,
                                      String outputFile, String sheetName,
                                      int includeHeaders, int skipEmptyRows,
                                      String filePattern, String sourceSheet,
                                      int addSourceColumn, int addIndexColumn,
                                      int workers);

        // 合并同一文件中的多个sheet
        int MergeSheetsInFile(int handle, String filename, String outputFile,
                              String targetSheetName);

        // 释放C字符串
        void FreeCString(Pointer str);
    }

    private static ExcelDLL excelDLL;

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

        // 初始化DLL接口
        excelDLL = Native.load(libraryPath, ExcelDLL.class);

        System.out.println("Native 库加载成功: " + libraryPath);
    }

    /**
     * 创建Excel管理器实例
     */
    public static int createExcelManager() {
        if (excelDLL == null) {
            throw new IllegalStateException("DLL未加载，请先调用loadDLL()");
        }
        int handle = excelDLL.NewExcelManagerInstance();
        System.out.println("创建Excel管理器，句柄: " + handle);
        return handle;
    }

    /**
     * 释放Excel管理器
     */
    public static void freeExcelManager(int handle) {
        if (excelDLL != null) {
            excelDLL.FreeExcelManager(handle);
            System.out.println("释放Excel管理器，句柄: " + handle);
        }
    }

    /**
     * 读取Excel数据
     */
    public static List readExcelData(int handle, String filename, String sheet) {
        //打印参数日志
        System.out.println("[DllInitializer] 读取Excel: " + filename + ", sheet: " + sheet+",handle:"+handle);
        if (excelDLL == null) {
            throw new IllegalStateException("DLL未加载，请先调用loadDLL()");
        }

        // 正确创建指针引用
        PointerByReference resultRef = new PointerByReference();

        int ret = excelDLL.ReadExcelData(handle, ensureUtf8(filename), ensureUtf8(sheet), resultRef);

        if (ret != 0) {
            throw new RuntimeException("读取Excel数据失败");
        }

        Pointer resultPtr = resultRef.getValue();
        if (resultPtr == null) {
            throw new RuntimeException("返回结果为空");
        }

        String jsonResult = ptrToString(resultPtr);
        excelDLL.FreeCString(resultPtr);

        try {
            if (jsonResult == null || jsonResult.isEmpty()) {
                return new ArrayList<>();
            }

            return JsonUtil.transferToObj(jsonResult, List.class);
        } catch (Exception e) {
            System.err.println("解析JSON失败: " + jsonResult);
            throw new RuntimeException("解析返回数据失败", e);
        }
    }

    /**
     * 批量写入数据到Excel文件
     */
    public static boolean writeBatchData(int handle, String filename, String sheet,
                                         List<String> cells, List<String> values) {
        if (excelDLL == null) {
            throw new IllegalStateException("DLL未加载，请先调用loadDLL()");
        }

        try {
            String cellsJson = objectMapper.writeValueAsString(cells);
            String valuesJson = objectMapper.writeValueAsString(values);

            int ret = excelDLL.WriteBatchData(handle, ensureUtf8(filename), ensureUtf8(sheet),
                    cellsJson, valuesJson, cells.size());

            return ret == 0;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化数据失败", e);
        }
    }

    /**
     * 追加数据到Excel文件末尾
     */
    public static boolean appendDataToExcel(int handle, String filename, String sheet,
                                            List<String> values) {
        if (excelDLL == null) {
            throw new IllegalStateException("DLL未加载，请先调用loadDLL()");
        }

        try {
            String valuesJson = objectMapper.writeValueAsString(values);

            int ret = excelDLL.AppendDataToExcel(handle, ensureUtf8(filename), ensureUtf8(sheet),
                    valuesJson, values.size());

            return ret == 0;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化数据失败", e);
        }
    }

    /**
     * 搜索包含关键字的单元格
     */
    public static List<String> searchByKeyword(int handle, String filename, String sheet,
                                               String keyword) {
        if (excelDLL == null) {
            throw new IllegalStateException("DLL未加载，请先调用loadDLL()");
        }

        Pointer resultPtr = new Pointer(0);

        int ret = excelDLL.SearchByKeyword(handle, ensureUtf8(filename), ensureUtf8(sheet),
                ensureUtf8(keyword), resultPtr);

        if (ret != 0) {
            throw new RuntimeException("搜索关键字失败");
        }

        String jsonResult = ptrToString(resultPtr);
        excelDLL.FreeCString(resultPtr);

        try {
            return objectMapper.readValue(jsonResult,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            throw new RuntimeException("解析返回数据失败", e);
        }
    }

    /**
     * 搜索整行包含关键字的行
     */
    public static List<List<String>> searchRowsByKeyword(int handle, String filename, String sheet,
                                                         String keyword) {
        if (excelDLL == null) {
            throw new IllegalStateException("DLL未加载，请先调用loadDLL()");
        }

        Pointer resultPtr = new Pointer(0);

        int ret = excelDLL.SearchRowsByKeyword(handle, ensureUtf8(filename), ensureUtf8(sheet),
                ensureUtf8(keyword), resultPtr);

        if (ret != 0) {
            throw new RuntimeException("搜索行失败");
        }

        String jsonResult = ptrToString(resultPtr);
        excelDLL.FreeCString(resultPtr);

        try {
            return objectMapper.readValue(jsonResult,
                    objectMapper.getTypeFactory().constructCollectionType(List.class,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
        } catch (Exception e) {
            throw new RuntimeException("解析返回数据失败", e);
        }
    }

    /**
     * 创建新文件并写入数据
     */
    public static boolean createAndWriteExcel(int handle, String filename, String sheet,
                                              List<List<String>> rowsData) {
        if (excelDLL == null) {
            throw new IllegalStateException("DLL未加载，请先调用loadDLL()");
        }

        try {
            String rowsDataJson = objectMapper.writeValueAsString(rowsData);

            int ret = excelDLL.CreateAndWriteExcel(handle, ensureUtf8(filename), ensureUtf8(sheet),
                    rowsDataJson);

            return ret == 0;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化数据失败", e);
        }
    }

    /**
     * 增强版合并Excel文件
     */
    public static boolean mergeExcelFilesEx(int handle, String sourceDir, List<String> specificFiles,
                                            String outputFile, String sheetName, boolean mergeByColumn,
                                            boolean includeHeaders, boolean skipEmptyRows,
                                            String filePattern, String sourceSheet,
                                            boolean addSourceColumn, boolean addIndexColumn) {
        if (excelDLL == null) {
            throw new IllegalStateException("DLL未加载，请先调用loadDLL()");
        }

        try {
            String specificFilesJson = objectMapper.writeValueAsString(specificFiles);

            int ret = excelDLL.MergeExcelFilesEx(handle,
                    sourceDir != null ? ensureUtf8(sourceDir) : null,
                    specificFilesJson,
                    ensureUtf8(outputFile),
                    ensureUtf8(sheetName),
                    mergeByColumn ? 1 : 0,
                    includeHeaders ? 1 : 0,
                    skipEmptyRows ? 1 : 0,
                    filePattern != null ? ensureUtf8(filePattern) : null,
                    sourceSheet != null ? ensureUtf8(sourceSheet) : null,
                    addSourceColumn ? 1 : 0,
                    addIndexColumn ? 1 : 0);

            return ret == 0;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化数据失败", e);
        }
    }

    /**
     * 合并同一文件中的多个sheet
     */
    public static boolean mergeSheetsInFile(int handle, String filename, String outputFile,
                                            String targetSheetName) {
        if (excelDLL == null) {
            throw new IllegalStateException("DLL未加载，请先调用loadDLL()");
        }

        int ret = excelDLL.MergeSheetsInFile(handle, ensureUtf8(filename),
                ensureUtf8(outputFile), ensureUtf8(targetSheetName));

        return ret == 0;
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
            return "excel.dll";
        } else if (osName.contains("linux")) {
            return "excel.so";
        } else if (osName.contains("mac")) {
            return "excel.dylib";
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

    /**
     * 将Pointer转换为字符串
     */
    private static String ptrToString(Pointer ptr) {
        if (ptr == null) return "";
        try {
            return ptr.getString(0, "UTF-8");
        } catch (Exception e) {
            System.err.println("字符串解码失败: " + e.getMessage());
            return "";
        }
    }

    /**
     * 获取Excel DLL接口实例（用于直接调用）
     */
    public static ExcelDLL getExcelDLL() {
        if (excelDLL == null) {
            throw new IllegalStateException("DLL未加载，请先调用loadDLL()");
        }
        return excelDLL;
    }
}