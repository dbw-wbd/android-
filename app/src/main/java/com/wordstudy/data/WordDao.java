package com.wordstudy.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * 数据访问对象（DAO = Data Access Object）
 *   loadAllWordbooks()        → 查数据库里有哪些词书（用于 Spinner 下拉选择）
 *   loadWordsByWordbook(...)  → 按词书筛选单词
 *   loadVocabByWordbook(...)  → 按词书筛选生词本
 *   countWordsByWordbook(...) → 查某个词书有多少词
 *   deleteByWordbook(...)     → 删除某词书的全部单词（换词书时清理用）
 */
@Dao
public interface WordDao {

    /**
     * 批量插入单词。OnConflictStrategy.IGNORE 表示：
     * 如果插入的数据主键已存在就忽略，避免崩溃。
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<Word> words);

    /** 统计表中共有多少个单词（用来判断是不是第一次启动、需不需要写入预设单词） */
    @Query("SELECT COUNT(*) FROM word_table")
    int countAllWords();

    /** 统计表中有多少个不同的词书名称（用来判断是否已有至少一份词书可背诵） */
    @Query("SELECT COUNT(DISTINCT wordbook) FROM word_table")
    int countWordbooks();

    /**
     * 查出数据库里所有出现过的词书名称（按字母序）。
     * 背诵页顶部的 Spinner 下拉框就用这个列表。
     */
    @Query("SELECT DISTINCT wordbook FROM word_table ORDER BY wordbook ASC")
    List<String> loadAllWordbooks();

    /** 查询某个词书共有多少单词（首次加载时用来决定要不要再导一次） */
    @Query("SELECT COUNT(*) FROM word_table WHERE wordbook = :wordbook")
    int countWordsByWordbook(String wordbook);

    /**
     * 按词书筛选全部单词（背诵页用）。
     * 返回普通 List，需在子线程调用。
     */
    @Query("SELECT * FROM word_table WHERE wordbook = :wordbook ORDER BY id ASC")
    List<Word> loadWordsByWordbook(String wordbook);

    /**
     * 一次性读出全部单词，按 id 排序 —— 全词库搜索用（很少用到）。
     * 返回普通 List，不是 LiveData，表示"只在这一刻取一次数据"。
     */
    @Query("SELECT * FROM word_table ORDER BY id ASC")
    List<Word> loadAllWords();

    /**
     * 查询生词本中的全部单词（不按词书过滤，跨词书的生词本）—— 返回 LiveData。
     * LiveData 的好处：数据库内容一旦变化（例如移除了某个生词），
     * 界面观察者会自动收到最新数据，不用手动刷新列表。
     */
    @Query("SELECT * FROM word_table WHERE inVocabBook = 1 ORDER BY id ASC")
    LiveData<List<Word>> loadVocabWords();

    /**
     * 按词书筛选生词（可选功能，某些场景下用户只想背特定词书的生词）。
     * 当前 App 未使用，保留接口方便以后扩展。
     */
    @Query("SELECT * FROM word_table WHERE inVocabBook = 1 AND wordbook = :wordbook ORDER BY id ASC")
    LiveData<List<Word>> loadVocabByWordbook(String wordbook);

    /**
     * 修改某个单词的生词本状态：true = 加入生词本，false = 移出生词本。
     * SQLite 中没有布尔类型，Room 会把 true/false 存成 1/0。
     */
    @Query("UPDATE word_table SET inVocabBook = :inBook WHERE id = :id")
    void updateVocabFlag(int id, boolean inBook);

    /**
     * 查询某个单词当前是否已在生词本中。
     * 点击"加入生词本"按钮前先查一次数据库，保证结果一定是最新的。
     */
    @Query("SELECT inVocabBook FROM word_table WHERE id = :id")
    boolean isInVocabBook(int id);

    /**
     * 删除某词书的全部单词。
     * 场景：用户导入词书时用了同名，想清理重来；或者 App 版本升级后想重新初始化。
     * 注意：这不会删除其它词书的单词，属于"局部清理"。
     */
    @Query("DELETE FROM word_table WHERE wordbook = :wordbook")
    void deleteByWordbook(String wordbook);
}
