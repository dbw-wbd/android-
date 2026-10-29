package com.wordstudy.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV 文件解析器
 *
 * 支持两种用法：
 *   parseFromInputStream(inputStream, wordbookName)
 *       → 从任意 InputStream 解析（assets 文件、用户选的外部文件、网络下载都行）
 *
 * CSV 格式约定：
 *   - 每行一条记录，英文逗号 , 分隔 3 个字段：单词 / 音标 / 释义
 *   - 第一行如果是表头（包含 "word" / "音标" 等）会自动跳过
 *   - 字段内部如果有逗号，用双引号 " 包裹（标准 CSV 规则）
 *   - 空行会跳过
 *   - 编码必须是 UTF-8
 *
 * 使用示例：
 *   // 从 assets 读内置词书
 *   InputStream is = context.getAssets().open("wordbooks/cet4_200.csv");
 *   List<Word> words = CsvParser.parseFromInputStream(is, "CET-4 高频");
 *   dao.insertAll(words);
 *
 *   // 从用户选择的外部文件读（ContentResolver + openInputStream）
 *   InputStream is = resolver.openInputStream(uri);
 *   List<Word> words = CsvParser.parseFromInputStream(is, "导入词书");
 */
public class CsvParser {

    /** 解析结果可能遇到的异常 —— 包装成 RuntimeException 让上层统一处理 */
    public static class ParseException extends RuntimeException {
        public ParseException(String msg) { super(msg); }
        public ParseException(String msg, Throwable cause) { super(msg, cause); }
    }

    /** 识别为表头的关键词（任一字段命中就认为这行是表头） */
    private static final String[] HEADER_KEYWORDS = {"word", "english", "phonetic", "soundmark", "音标", "单词"};

    /**
     * 从 InputStream 解析 CSV。
     *
     * @param inputStream  输入流（关闭工作交给调用方）
     * @param wordbookName 这些单词归入哪个词书（例如 "CET-4 高频"、"导入词书.csv"）
     * @return 解析出的 Word 列表（至少 1 个单词）
     */
    public static List<com.wordstudy.data.Word> parseFromInputStream(
            InputStream inputStream, String wordbookName) throws ParseException {

        if (inputStream == null) {
            throw new ParseException("输入流为 null，无法解析 CSV");
        }
        List<com.wordstudy.data.Word> result = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                // 跳过首尾空白 + 完全空的行
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                // 跳过表头
                if (lineNo == 1 && isHeaderLine(trimmed)) continue;

                // 解析这一行
                String[] fields = splitCsvLine(trimmed);
                if (fields.length < 1) continue;

                String word    = fields[0].trim();
                String phonetic = fields.length >= 2 ? fields[1].trim() : "";
                String meaning = fields.length >= 3 ? fields[2].trim() : "";

                // 单词不能为空，否则整条跳过
                if (word.isEmpty()) continue;

                result.add(new com.wordstudy.data.Word(word, phonetic, meaning, wordbookName));
            }

        } catch (IOException e) {
            throw new ParseException("CSV 读取失败：" + e.getMessage(), e);
        }

        if (result.isEmpty()) {
            throw new ParseException("CSV 中没有解析出任何单词，请检查文件格式");
        }
        return result;
    }

    /**
     * 判断一行是不是表头。规则：全部转小写后，如果包含 word / 单词 / 音标 这类关键词，
     * 就当作表头跳过。这样不管表头写英文还是中文都能自动识别。
     */
    private static boolean isHeaderLine(String line) {
        String lower = line.toLowerCase();
        for (String kw : HEADER_KEYWORDS) {
            if (lower.contains(kw.toLowerCase())) return true;
        }
        return false;
    }

    /**
     * 标准 CSV 行分割 —— 支持引号包裹的字段。
     * 例如：a,"b,c",d → ["a", "b,c", "d"]
     */
    private static String[] splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                // 两个连续引号 = 字段内的字面引号（标准 CSV 转义）
                if (inQuote && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;   // 跳过下一个引号
                } else {
                    inQuote = !inQuote;
                }
            } else if (c == ',' && !inQuote) {
                fields.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        fields.add(cur.toString());   // 最后一个字段

        return fields.toArray(new String[0]);
    }
}
