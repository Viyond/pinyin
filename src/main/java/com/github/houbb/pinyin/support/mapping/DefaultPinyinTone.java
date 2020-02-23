package com.github.houbb.pinyin.support.mapping;

import com.github.houbb.heaven.annotation.ThreadSafe;
import com.github.houbb.heaven.constant.PunctuationConst;
import com.github.houbb.heaven.util.guava.Guavas;
import com.github.houbb.heaven.util.io.StreamUtil;
import com.github.houbb.heaven.util.lang.ObjectUtil;
import com.github.houbb.heaven.util.lang.StringUtil;
import com.github.houbb.heaven.util.util.CollectionUtil;
import com.github.houbb.pinyin.constant.PinyinConst;
import com.github.houbb.pinyin.constant.enums.PinyinToneNumEnum;
import com.github.houbb.pinyin.model.CharToneInfo;
import com.github.houbb.pinyin.model.ToneItem;
import com.github.houbb.pinyin.spi.IPinyinChinese;
import com.github.houbb.pinyin.support.chinese.PinyinChineses;
import com.github.houbb.pinyin.util.ToneHelper;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 正常的拼音注音形式
 *
 * （1）单个字的词库与词组的词库分离
 * （2）二者词库都是懒加载，只有使用的时候才会去初始化。
 *
 * @author binbin.hou
 * @since 0.0.1
 */
@ThreadSafe
public class DefaultPinyinTone extends AbstractPinyinTone {

    /**
     * 单个字的 Map
     * DCL 单例，惰性加载。
     *
     * （1）注意多音字的问题
     * （2）默认只返回第一个
     * （3）为了提升读取的性能，在初始化的时候，直接设计好。
     * @since 0.0.1
     */
    private static volatile Map<String, List<String>> charMap;

    /**
     * 词组 map
     *
     * DCL 单例，惰性加载。
     * @since 0.0.1
     */
    private static volatile Map<String, String> phraseMap;

    @Override
    protected List<String> getCharTones(String chinese) {
        return getCharMap().get(chinese);
    }

    @Override
    protected String getCharTone(String segment) {
        // 大部分拼音都是单个字，不是多音字。
        // 直接在初始化的时候，设置好。
        List<String> pinyinList = getCharMap().get(segment);
        if(CollectionUtil.isNotEmpty(pinyinList)) {
            return pinyinList.get(0);
        }

        // 没有则返回空
        return null;
    }

    @Override
    protected String getPhraseTone(String segment) {
        return getPhraseMap().get(segment);
    }

    /**
     * 获取单个字符的 map
     *
     * @return map
     * @since 0.0.1
     */
    private Map<String, List<String>> getCharMap() {
        if(ObjectUtil.isNotNull(charMap)) {
            return charMap;
        }

        synchronized (DefaultPinyinTone.class) {
            if(ObjectUtil.isNull(charMap)) {
                final long startTime = System.currentTimeMillis();
                final IPinyinChinese pinyinChinese = PinyinChineses.simple();

                List<String> lines = StreamUtil.readAllLines(PinyinConst.PINYIN_DICT_CHAR_SYSTEM);
                // 自定义词库
                List<String> defineLines = StreamUtil.readAllLines(PinyinConst.PINYIN_DICT_CHAR_DEFINE);
                lines.addAll(defineLines);
                charMap = Guavas.newHashMap(lines.size());

                for(String line : lines) {
                    String[] strings = line.split(PunctuationConst.COLON);
                    List<String> pinyinList = StringUtil.splitToList(strings[1]);

                    final String word = strings[0];
                    charMap.put(word, pinyinList);

                    // 转换为简体，再一次存储
                    String simple = pinyinChinese.toSimple(word);
                    if(!word.equals(simple)) {
                        charMap.put(simple, pinyinList);
                    }
                }

                final long endTime = System.currentTimeMillis();
                System.out.println("[Pinyin] char dict loaded, cost time " + (endTime-startTime)+" ms!");
            }
        }

        return charMap;
    }

    /**
     * 获取词组的拼音 Map
     * （1）词组的拼音是确定的。
     * @return map
     * @since 0.0.1
     */
    private Map<String, String> getPhraseMap() {
        if(ObjectUtil.isNotNull(phraseMap)) {
            return phraseMap;
        }
        synchronized (DefaultPinyinTone.class) {
            if(ObjectUtil.isNull(phraseMap)) {
                final long startTime = System.currentTimeMillis();
                final IPinyinChinese pinyinChinese = PinyinChineses.simple();

                List<String> lines = StreamUtil.readAllLines(PinyinConst.PINYIN_DICT_PHRASE_SYSTEM);
                // 处理自定义字典
                List<String> defineLines = StreamUtil.readAllLines(PinyinConst.PINYIN_DICT_PHRASE_DEFINE);
                lines.addAll(defineLines);
                phraseMap = Guavas.newHashMap(lines.size());

                for(String line : lines) {
                    String[] strings = line.split(PunctuationConst.COLON);
                    String word = strings[0];
                    phraseMap.put(word, strings[1]);

                    // 转换为简体，再一次存储
                    String simple = pinyinChinese.toSimple(word);
                    if(!word.equals(simple)) {
                        phraseMap.put(simple, strings[1]);
                    }
                }

                final long endTime = System.currentTimeMillis();
                System.out.println("[Pinyin] phrase dict loaded, cost time " + (endTime-startTime)+" ms!");
            }
        }

        return phraseMap;
    }

    @Override
    public Set<String> phraseSet() {
        Map<String, String> map = getPhraseMap();
        return map.keySet();
    }

    @Override
    public int toneNum(String chinese) {
        //1. 获取拼音
        final String tone = getCharTone(chinese);

        if(StringUtil.isNotEmpty(tone)) {
            CharToneInfo toneInfo = this.getCharToneInfo(tone);

            int index = toneInfo.getIndex();

            // 轻声
            if (index < 0) {
                return PinyinToneNumEnum.FIVE.num();
            }

            // 直接返回对应的音标
            return toneInfo.getToneItem().getTone();
        }

        // 默认返回未知
        return PinyinToneNumEnum.UN_KNOWN.num();
    }

    /**
     * 获取对应的声调信息
     * @param tone 拼音信息
     * @return 声调信息
     * @since 0.0.3
     */
    protected CharToneInfo getCharToneInfo(final String tone) {
        CharToneInfo charToneInfo = new CharToneInfo();
        charToneInfo.setIndex(-1);

        int length = tone.length();
        for(int i = 0; i < length; i++) {
            char currentChar = tone.charAt(i);
            ToneItem toneItem = ToneHelper.getToneItem(currentChar);

            if (ObjectUtil.isNotNull(toneItem)) {
                charToneInfo.setToneItem(toneItem);
                charToneInfo.setIndex(i);
                break;
            }
        }

        return charToneInfo;
    }

}
