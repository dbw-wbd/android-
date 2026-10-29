package com.wordstudy.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.wordstudy.R;
import com.wordstudy.adapter.VocabBookAdapter;
import com.wordstudy.data.AppDatabase;
import com.wordstudy.data.Word;

/**
 * 生词本页 Fragment
 */
public class VocabBookFragment extends Fragment {

    private TextView tvCount;      // 生词数量
    private TextView tvEmpty;      // 空状态提示
    private RecyclerView recyclerView;

    private VocabBookAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vocab_book, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvCount = view.findViewById(R.id.tv_vocab_count);
        tvEmpty = view.findViewById(R.id.tv_empty);
        recyclerView = view.findViewById(R.id.recycler_vocab);

        // 1. 给 RecyclerView 设置布局管理器：LinearLayoutManager = 垂直列表
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // 2. 创建适配器，并把“点击移除”的事件交给 removeWord 方法处理
        adapter = new VocabBookAdapter(word -> removeWord(word));
        recyclerView.setAdapter(adapter);

        // 3. 观察（订阅）数据库中的生词列表：数据变化时回调里的代码会自动执行
        AppDatabase.getInstance(requireContext())
                .wordDao()
                .loadVocabWords()
                .observe(getViewLifecycleOwner(), words -> {
                    // words 就是当前生词本里的全部单词
                    adapter.setData(words);

                    tvCount.setText("共 " + words.size() + " 个单词");

                    // 列表为空时显示提示文字，隐藏列表；否则反过来
                    boolean isEmpty = words.isEmpty();
                    tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                });
    }

    /** 把单词移出生词本*/
    private void removeWord(Word word) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("移出生词本")
                .setMessage("确定要把 “" + word.getWord() + "” 移出生词本吗？")
                .setNegativeButton("取消", null)
                .setPositiveButton("移除", (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        AppDatabase.getInstance(requireContext())
                                .wordDao()
                                .updateVocabFlag(word.getId(), false);
                    });
                    Toast.makeText(requireContext(),
                            "已移出生词本：" + word.getWord(),
                            Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}
