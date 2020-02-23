package com.github.houbb.pinyin.api;

import java.util.List;

/**
 * 拼音核心用户 api
 * @author binbin.hou
 * @since 0.0.1
 */
public interface IPinyin {

    /**
     * 获取指定字符串的拼音
     * @param string 原始字符串
     * @return 转换后的结果
     * @since 0.0.1
     */
    String toPinyin(final String string);

    /**
     * 返回所有拼音列表
     * （1）返回当前汉字的所有拼音列表
     * （2）如果不是汉字，返回其本身
     * @param chinese 中文汉字
     * @return 返回所有拼音列表
     * @since 0.0.2
     */
    List<String> toPinyin(final char chinese);

    /**
     * 是否为同音字
     * @param chineseOne 中文一
     * @param chineseTwo 中文二
     * @return 是否相同
     * @since 0.0.8
     */
    boolean hasSamePinyin(final char chineseOne, final char chineseTwo);

    /**
     * 获取拼音的音调编号
     * 12345
     * @param chinese 中文拼音
     * @return 结果
     * @since 0.1.0
     */
    int toneNum(final char chinese);

}
