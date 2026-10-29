package com.wordstudy.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 预设单词列表（默认词书 "默认 15 词"）
 * 想换单词 / 加单词：直接改下面的 add(...) 即可，格式为
 *      add(列表, "单词", "音标", "中文释义");
 */
public class PresetWords {

    /** 默认词书的名称，CSV 加载器会用这个常量来避免同名冲突 */
    public static final String DEFAULT_WORDBOOK_NAME = "默认 15 词";

    /** 返回内置的 15 个四六级单词（属于 "默认 15 词" 词书） */
    public static List<Word> getPresetWords() {
        List<Word> list = new ArrayList<>();
        add(list, "abandon", "/əˈbændən/", "vt. 放弃，抛弃；遗弃");
        add(list, "benefit", "/ˈbenɪfɪt/", "n. 好处，益处  vt. 使受益");
        add(list, "campus", "/ˈkæmpəs/", "n. 校园，校区");
        add(list, "debate", "/dɪˈbeɪt/", "n./v. 辩论，争论");
        add(list, "efficient", "/ɪˈfɪʃnt/", "adj. 高效的，效率高的");
        add(list, "facility", "/fəˈsɪləti/", "n. 设施，设备；便利");
        add(list, "genuine", "/ˈdʒenjuɪn/", "adj. 真正的；真诚的");
        add(list, "horizon", "/həˈraɪzn/", "n. 地平线；眼界，见识");
        add(list, "illustrate", "/ˈɪləstreɪt/", "vt. 说明，阐明；举例说明");
        add(list, "journal", "/ˈdʒɜːnl/", "n. 期刊，杂志；日记");
        add(list, "keen", "/kiːn/", "adj. 敏锐的；热衷的，渴望的");
        add(list, "maintain", "/meɪnˈteɪn/", "vt. 维持，保持；维修，保养");
        add(list, "negotiate", "/nɪˈɡəʊʃieɪt/", "v. 谈判，协商，洽谈");
        add(list, "obstacle", "/ˈɒbstəkl/", "n. 障碍，阻碍，妨碍物");
        add(list, "pursue", "/pəˈsjuː/", "vt. 追求，从事；追赶");
        return list;
    }

    /** 小工具方法：向列表中添加一个单词，词书统一标记为 DEFAULT_WORDBOOK_NAME */
    private static void add(List<Word> list, String word, String phonetic, String meaning) {
        list.add(new Word(word, phonetic, meaning, DEFAULT_WORDBOOK_NAME));
    }
}
