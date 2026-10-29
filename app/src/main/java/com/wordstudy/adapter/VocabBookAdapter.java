package com.wordstudy.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wordstudy.R;
import com.wordstudy.data.Word;

import java.util.ArrayList;
import java.util.List;


public class VocabBookAdapter extends RecyclerView.Adapter<VocabBookAdapter.VocabViewHolder> {


    public interface OnRemoveClickListener {
        void onRemove(Word word);
    }

    /** 列表数据 */
    private final List<Word> dataList = new ArrayList<>();
    private final OnRemoveClickListener listener;

    public VocabBookAdapter(OnRemoveClickListener listener) {
        this.listener = listener;
    }

    /**
     * 更新数据并通知 RecyclerView 刷新界面
     */
    public void setData(List<Word> newData) {
        dataList.clear();
        if (newData != null) {
            dataList.addAll(newData);
        }
        notifyDataSetChanged();   // 课设数据量很小，直接整体刷新即可
    }

    @NonNull
    @Override
    public VocabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vocab_book, parent, false);
        return new VocabViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull VocabViewHolder holder, int position) {
        Word word = dataList.get(position);

        holder.tvWord.setText(word.getWord());
        holder.tvPhonetic.setText(word.getPhonetic());
        holder.tvMeaning.setText(word.getMeaning());

        // 点击垃圾桶图标 → 回调给 Fragment 处理（弹确认框 + 更新数据库）
        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemove(word);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    /**
     * ViewHolder：缓存条目布局中的控件，避免每次都 findViewById（提升滑动性能）
     */
    static class VocabViewHolder extends RecyclerView.ViewHolder {
        TextView tvWord;
        TextView tvPhonetic;
        TextView tvMeaning;
        ImageButton btnRemove;

        VocabViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWord = itemView.findViewById(R.id.tv_item_word);
            tvPhonetic = itemView.findViewById(R.id.tv_item_phonetic);
            tvMeaning = itemView.findViewById(R.id.tv_item_meaning);
            btnRemove = itemView.findViewById(R.id.btn_item_remove);
        }
    }
}
