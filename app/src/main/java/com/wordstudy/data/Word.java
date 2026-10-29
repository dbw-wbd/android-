package com.wordstudy.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 单词实体类（Entity）
 */
@Entity(tableName = "word_table")
public class Word {

    /** 主键，autoGenerate = true 表示由数据库自增生成 */
    @PrimaryKey(autoGenerate = true)
    private int id;

    /** 英文单词 */
    private String word;

    /** 音标 */
    private String phonetic;

    /** 中文释义 */
    private String meaning;

    /** 是否已加入生词本 */
    private boolean inVocabBook;
    private String wordbook;

    /** Room 需要使用的无参构造方法 */
    public Word() {
    }

    /**
     * 便捷构造方法 —— 不指定词书（默认归入 "default"）。
     * @Ignore 表示 Room 读取数据库时不要用它。
     */
    @Ignore
    public Word(String word, String phonetic, String meaning) {
        this(word, phonetic, meaning, "default");
    }

    /**
     * 完整构造方法 —— 指定词书名称。
     */
    @Ignore
    public Word(String word, String phonetic, String meaning, String wordbook) {
        this.word = word;
        this.phonetic = phonetic;
        this.meaning = meaning;
        this.inVocabBook = false;
        this.wordbook = wordbook;
    }

    // ---------------- 以下为 getter / setter，Room 通过它们读写字段 ----------------

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getPhonetic() {
        return phonetic;
    }

    public void setPhonetic(String phonetic) {
        this.phonetic = phonetic;
    }

    public String getMeaning() {
        return meaning;
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }

    public boolean isInVocabBook() {
        return inVocabBook;
    }

    public void setInVocabBook(boolean inVocabBook) {
        this.inVocabBook = inVocabBook;
    }

    public String getWordbook() {
        return wordbook;
    }

    public void setWordbook(String wordbook) {
        this.wordbook = wordbook;
    }
}
