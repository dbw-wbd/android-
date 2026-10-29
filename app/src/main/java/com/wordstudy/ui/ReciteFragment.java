package com.wordstudy.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.wordstudy.R;
import com.wordstudy.adapter.WordListAdapter;
import com.wordstudy.data.AppDatabase;
import com.wordstudy.data.PresetWords;
import com.wordstudy.data.Word;
import com.wordstudy.data.WordDao;
import com.wordstudy.util.CsvParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 背诵页 Fragment —— App 的核心页面（支持多词书 ）
 */
public class ReciteFragment extends Fragment {

    /** 标记数据库是否已经初始化过（避免重复弹首次启动对话框） */
    private static final String PREFS_NAME = "wordbook_prefs";
    private static final String KEY_DB_INITIALIZED = "db_initialized";
    /** 上次正在背的词书，下次打开自动恢复 */
    private static final String KEY_LAST_WORDBOOK = "last_wordbook";
    /** 每个词书各自记录"上次背到第几个"，key = index_词书名 */
    private static final String KEY_INDEX_PREFIX = "index_";

    // ---------- 界面控件 ----------
    private TextView tvWord;
    private TextView tvPhonetic;
    private TextView tvMeaning;
    private TextView tvProgress;
    private MaterialCardView cardWord;

    private TextView tvWordList;           // 单词列表小按钮
    private TextView tvImportCsv;          // 导入 CSV 小按钮
    private Spinner spinnerWordbook;       // 词书下拉
    private MaterialButton btnPrev;
    private MaterialButton btnAddVocab;
    private MaterialButton btnNext;

    // ---------- 数据 ----------
    /** 当前词书的单词列表 */
    private final List<Word> wordList = new ArrayList<>();
    private int currentIndex = 0;
    private String currentWordbook = PresetWords.DEFAULT_WORDBOOK_NAME;  // 默认词书名

    private WordDao wordDao;
    private SharedPreferences prefs;

    /** ActivityResultLauncher：调起系统文件选择器选 .csv 文件 */
    private ActivityResultLauncher<String> pickCsvLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recite, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. 找到所有控件
        tvWord = view.findViewById(R.id.tv_word);
        tvPhonetic = view.findViewById(R.id.tv_phonetic);
        tvMeaning = view.findViewById(R.id.tv_meaning);
        tvProgress = view.findViewById(R.id.tv_progress);
        cardWord = view.findViewById(R.id.card_word);
        tvWordList = view.findViewById(R.id.tv_word_list);
        tvImportCsv = view.findViewById(R.id.tv_import_csv);
        spinnerWordbook = view.findViewById(R.id.spinner_wordbook);
        btnPrev = view.findViewById(R.id.btn_prev);
        btnAddVocab = view.findViewById(R.id.btn_add_vocab);
        btnNext = view.findViewById(R.id.btn_next);

        // 2. 初始化依赖
        wordDao = AppDatabase.getInstance(requireContext()).wordDao();
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // 3. 注册 ActivityResultLauncher —— 调起系统"从文件选择器打开"的对话框
        //    GetContent 接收一个 mime type 字符串，返回 Uri；OpenDocument 接收 String[]
        //    这里用 GetContent 更简单：一次只传一个 mime type，launch 调用也只传一个字符串
        pickCsvLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> handleImportedCsv(uri));

        // 4. 设置按钮点击事件
        tvWordList.setOnClickListener(v -> showWordListDialog());
        tvImportCsv.setOnClickListener(v -> pickCsvFile());
        btnPrev.setOnClickListener(v -> showPrevWord());
        btnNext.setOnClickListener(v -> showNextWord());
        btnAddVocab.setOnClickListener(v -> addCurrentWordToVocabBook());

        // 5. Spinner 切换词书
        spinnerWordbook.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View v,
                                                  int position, long id) {
                String chosen = (String) parent.getItemAtPosition(position);
                if (chosen != null && !chosen.equals(currentWordbook)) {
                    // 先保存旧词书背到的位置，再切到新词书
                    saveProgress();
                    currentWordbook = chosen;
                    saveLastWordbook();
                    loadWordsOfCurrentWordbook();
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // 6. 判断是否首次启动，加载数据库
        checkInitializationAndLoad();
    }
    /**
     * 首次启动时弹对话框让用户选词书；之后自动恢复上次的词书和进度。
     */
    private void checkInitializationAndLoad() {
        boolean initialized = prefs.getBoolean(KEY_DB_INITIALIZED, false);
        if (!initialized) {
            showFirstLaunchDialog();
        } else {
            // 恢复上次正在背的词书（如果还存在的话）
            String savedBook = prefs.getString(KEY_LAST_WORDBOOK, null);
            if (savedBook != null && !savedBook.isEmpty()) {
                currentWordbook = savedBook;
            }
            reloadWordbooksAndLoadCurrent();
        }
    }

    private void showFirstLaunchDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.first_launch_title)
                .setMessage(R.string.first_launch_message)
                .setCancelable(false)
                .setPositiveButton(R.string.first_launch_btn_default, (d, w) -> {
                    loadPresetDefaultWordbook();
                    markInitialized();
                })
                .setNegativeButton(R.string.first_launch_btn_cet4, (d, w) -> {
                    loadBuiltInCet4Wordbook();
                    markInitialized();
                })
                .setNeutralButton(R.string.first_launch_btn_import, (d, w) -> {
                    markInitialized();
                    pickCsvFile();
                })
                .show();
    }

    /** 第一次加载结束后，把 "已初始化" 这个状态存进 SharedPreferences */
    private void markInitialized() {
        prefs.edit().putBoolean(KEY_DB_INITIALIZED, true).apply();
    }

    private void loadPresetDefaultWordbook() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // 幂等检查：如果 "默认 15 词" 这个词书已经有单词，就跳过，避免重复插入
            if (wordDao.countWordsByWordbook(PresetWords.DEFAULT_WORDBOOK_NAME) == 0) {
                wordDao.insertAll(PresetWords.getPresetWords());
            }
            if (isAdded()) {
                requireActivity().runOnUiThread(this::reloadWordbooksAndLoadCurrent);
            }
        });
    }

    private void loadBuiltInCet4Wordbook() {
        // 先把默认 15 词也插入，保证 Spinner 里总有一个词书
        loadPresetDefaultWordbook();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try (InputStream is = requireContext().getAssets().open("wordbooks/cet4_200.csv")) {
                String name = "CET-4 高频 200 词";
                // 幂等：已加载过就跳过
                if (wordDao.countWordsByWordbook(name) > 0) {
                    if (isAdded()) requireActivity().runOnUiThread(this::reloadWordbooksAndLoadCurrent);
                    return;
                }
                List<Word> words = CsvParser.parseFromInputStream(is, name);
                wordDao.insertAll(words);

                // 切换到这个新词书，让用户一打开就看到它
                currentWordbook = name;
                if (isAdded()) {
                    requireActivity().runOnUiThread(this::reloadWordbooksAndLoadCurrent);
                }
            } catch (IOException e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "读取 CET-4 CSV 失败：" + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
                }
            } catch (CsvParser.ParseException e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "CET-4 CSV 解析失败：" + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    // 导入外部 CSV（系统文件选择器）
    private void pickCsvFile() {
        try {
            // GetContent 只接收一个 mime type 字符串
            pickCsvLauncher.launch("text/csv");
        } catch (Exception e) {
            Toast.makeText(requireContext(), "无法打开文件选择器：" + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }


    private void handleImportedCsv(Uri uri) {
        if (uri == null) return;   // 用户取消了选择

        // 先把默认词书也插进去，保证 Spinner 里总有东西
        loadPresetDefaultWordbook();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try (InputStream is = requireContext()
                    .getContentResolver().openInputStream(uri)) {
                // 从 Uri 的最后一段猜出文件名，当作词书名（去掉 .csv 后缀）
                String suggestedName = extractNameFromUri(uri);
                String wordbookName = "导入词书 · " + suggestedName;

                // 幂等：如果同名词书已经存在，给个时间戳区分
                if (wordDao.countWordsByWordbook(wordbookName) > 0) {
                    wordbookName = wordbookName + " (" + System.currentTimeMillis() % 10000 + ")";
                }

                List<Word> words = CsvParser.parseFromInputStream(is, wordbookName);
                wordDao.insertAll(words);

                currentWordbook = wordbookName;
                final String importedMsg = getString(
                        R.string.wordbook_toast_imported, wordbookName, words.size());
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        reloadWordbooksAndLoadCurrent();
                        Toast.makeText(requireContext(), importedMsg, Toast.LENGTH_LONG).show();
                    });
                }
            } catch (IOException e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(),
                                    "读取文件失败：" + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            } catch (CsvParser.ParseException e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(),
                                    "CSV 解析失败：" + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(),
                                    "导入失败：" + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }
        });
    }


    private String extractNameFromUri(Uri uri) {
        String path = uri.getPath();
        if (path == null || path.isEmpty()) return "未命名";
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        int dot = name.lastIndexOf('.');
        if (dot >= 0) name = name.substring(0, dot);
        return name.isEmpty() ? "未命名" : name;
    }

    //  Spinner 刷新 + 按词书加载单词
    private void reloadWordbooksAndLoadCurrent() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<String> allBooks = wordDao.loadAllWordbooks();

            if (allBooks.isEmpty()) {
                // 数据库空空如也，应该不会走到这里（首次启动对话框会兜底），保险起见
                allBooks.add(PresetWords.DEFAULT_WORDBOOK_NAME);
            }

            // 确保 currentWordbook 一定存在
            if (!allBooks.contains(currentWordbook)) {
                currentWordbook = allBooks.get(0);
            }

            // 切回主线程更新 Spinner 和卡片
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    updateSpinner(allBooks);
                    loadWordsOfCurrentWordbook();
                });
            }
        });
    }

    /**
     * 用新的词书列表刷新 Spinner，定位到 currentWordbook。
     */
    private void updateSpinner(List<String> allBooks) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                allBooks);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWordbook.setAdapter(adapter);

        int pos = allBooks.indexOf(currentWordbook);
        if (pos < 0) pos = 0;

        // 临时移除监听，避免 setSelection 触发 onItemSelected 再查一次库
        spinnerWordbook.setOnItemSelectedListener(null);
        spinnerWordbook.setSelection(pos, false);
        spinnerWordbook.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View v,
                                                  int position, long id) {
                String chosen = (String) parent.getItemAtPosition(position);
                if (chosen != null && !chosen.equals(currentWordbook)) {
                    // 先保存旧词书背到的位置，再切到新词书
                    saveProgress();
                    currentWordbook = chosen;
                    saveLastWordbook();
                    loadWordsOfCurrentWordbook();
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    /**
     * 按当前词书查库并刷新卡片。
     * 同时恢复"这本书上次背到第几个单词"，实现关闭 App 后继续背。
     */
    private void loadWordsOfCurrentWordbook() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Word> words = wordDao.loadWordsByWordbook(currentWordbook);

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    wordList.clear();
                    wordList.addAll(words);

                    // 恢复上次的进度（下标可能越界，clamp 到合法范围）
                    int savedIndex = prefs.getInt(KEY_INDEX_PREFIX + currentWordbook, 0);
                    currentIndex = words.isEmpty() ? 0
                            : Math.min(Math.max(savedIndex, 0), words.size() - 1);

                    showCurrentWord(false);
                });
            }
        });
    }

    //进度记忆（SharedPreferences）
    private void saveProgress() {
        if (prefs == null || currentWordbook == null) return;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_LAST_WORDBOOK, currentWordbook);
        if (!wordList.isEmpty()) {
            editor.putInt(KEY_INDEX_PREFIX + currentWordbook, currentIndex);
        }
        editor.apply();
    }

    /** 只记录"上次用的词书"（切换词书时使用） */
    private void saveLastWordbook() {
        if (prefs == null || currentWordbook == null) return;
        prefs.edit().putString(KEY_LAST_WORDBOOK, currentWordbook).apply();
    }


    // 单词列表弹窗（搜索 + 点击跳转）
    private void showWordListDialog() {
        if (wordList.isEmpty()) {
            Toast.makeText(requireContext(), R.string.wordbook_empty_db, Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. 创建弹窗并加载布局
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_word_list);
        Window window = dialog.getWindow();
        if (window != null) {
            // 让圆角背景生效（去掉系统默认的白色方角窗口背景）
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etSearch = dialog.findViewById(R.id.et_search);
        RecyclerView recyclerView = dialog.findViewById(R.id.recycler_word_list);
        TextView tvEmpty = dialog.findViewById(R.id.tv_list_empty);

        // 2. 配置列表
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);

        WordListAdapter adapter = new WordListAdapter(word -> {
            // 点击某一行：找到它在 wordList 中的位置，跳过去
            int index = wordList.indexOf(word);
            if (index >= 0) {
                currentIndex = index;
            }
            dialog.dismiss();
            showCurrentWord(true);   // showCurrentWord 里会保存进度
        });
        adapter.setData(wordList, wordList.get(currentIndex));
        recyclerView.setAdapter(adapter);

        // 3. 搜索框实时过滤
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override public void afterTextChanged(Editable s) {
                adapter.filter(s.toString());
                boolean empty = adapter.getShownCount() == 0;
                tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
            }
        });

        // 4. 显示弹窗，并设置成"屏幕宽 90%、高 72%"的大小
        dialog.show();
        if (window != null) {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            window.setLayout((int) (dm.widthPixels * 0.9f), (int) (dm.heightPixels * 0.72f));
        }

        // 5. 自动滚动到当前单词（放在当前单词上方约 1/3 处，让上下文可见）
        recyclerView.post(() -> {
            int currentPos = adapter.getCurrentPosition();
            if (currentPos >= 0) {
                layoutManager.scrollToPositionWithOffset(currentPos, recyclerView.getHeight() / 3);
            }
        });
    }

    // 卡片交互（上一个 / 下一个 / 加入生词本）
    /** 上一个：到第一个再点会回到最后一个 */
    private void showPrevWord() {
        if (wordList.isEmpty()) {
            Toast.makeText(requireContext(), R.string.wordbook_empty_db, Toast.LENGTH_SHORT).show();
            return;
        }
        currentIndex = (currentIndex - 1 + wordList.size()) % wordList.size();
        showCurrentWord(true);
    }

    /** 下一个：到最后一个再点会回到第一个 */
    private void showNextWord() {
        if (wordList.isEmpty()) {
            Toast.makeText(requireContext(), R.string.wordbook_empty_db, Toast.LENGTH_SHORT).show();
            return;
        }
        currentIndex = (currentIndex + 1) % wordList.size();
        showCurrentWord(true);
    }

    /** 把第 currentIndex 个单词显示到卡片上 */
    private void showCurrentWord(boolean animate) {
        if (wordList.isEmpty()) {
            tvWord.setText("暂无单词");
            tvPhonetic.setText("");
            tvMeaning.setText(getString(R.string.wordbook_empty_db));
            tvProgress.setText("0 / 0");
            return;
        }

        Word word = wordList.get(currentIndex);
        tvWord.setText(word.getWord());
        tvPhonetic.setText(word.getPhonetic());
        tvMeaning.setText(word.getMeaning());
        tvProgress.setText((currentIndex + 1) + " / " + wordList.size());

        // 记住当前进度，下次打开 App 会回到这个单词
        saveProgress();

        if (animate) {
            cardWord.setAlpha(0f);
            cardWord.animate().alpha(1f).setDuration(150).start();
        }
    }

    /** 加入生词本：先查再写，避免重复 */
    private void addCurrentWordToVocabBook() {
        if (wordList.isEmpty()) {
            Toast.makeText(requireContext(), R.string.wordbook_empty_db, Toast.LENGTH_SHORT).show();
            return;
        }

        Word currentWord = wordList.get(currentIndex);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            boolean alreadyInBook = wordDao.isInVocabBook(currentWord.getId());
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    if (alreadyInBook) {
                        Toast.makeText(requireContext(),
                                currentWord.getWord() + " 已经在生词本中啦", Toast.LENGTH_SHORT).show();
                    } else {
                        AppDatabase.databaseWriteExecutor.execute(() ->
                                wordDao.updateVocabFlag(currentWord.getId(), true));
                        Toast.makeText(requireContext(),
                                "已加入生词本：" + currentWord.getWord(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
