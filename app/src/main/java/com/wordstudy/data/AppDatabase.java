package com.wordstudy.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Word.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    /** 数据库文件名，实际保存在 /data/data/com.wordstudy/databases/word_database */
    private static final String DB_NAME = "word_database";

    /** 唯一实例，volatile 保证多线程下可见性 */
    private static volatile AppDatabase instance;

    /** 获取 Dao（由 Room 自动实现） */
    public abstract WordDao wordDao();

    /**
     * 数据库操作执行器：所有数据库读写都放到这个子线程里执行。
     * 原因：主线程（UI 线程）做数据库操作会卡界面，Android 会直接抛异常。
     * newSingleThreadExecutor() 表示只开一个后台线程，任务按顺序执行，简单可靠。
     */
    public static final ExecutorService databaseWriteExecutor = Executors.newSingleThreadExecutor();

    /**
     * 获取数据库实例（双检锁单例写法）
     */
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),   // 用 Application 上下文，避免内存泄漏
                                    AppDatabase.class,
                                    DB_NAME)
                            // 表结构变化时，
                            // 直接销毁旧表重建，避免因为缺少 Migration 类而崩溃。
                            // 代价是旧数据全部丢失，但课设项目可以接受。
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
