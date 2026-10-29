package com.wordstudy.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wordstudy.R;
import com.wordstudy.data.Word;

import java.util.ArrayList;
import java.util.List;

public class WordListAdapter extends RecyclerView.Adapter<WordListAdapter.WordViewHolder> {

    /** 点击某一行的回调 */
    public interface OnWordClickListener {
        void onWordClick(Word word);
    }

    /** 该词书的全部单词（不过滤） */
    private final List<Word> allWords = new ArrayList<>();
    /** 当前界面上真正显示的单词（过滤后的结果） */
    private final List<Word> shownWords = new ArrayList<>();

    /** 当前正在背的单词，用于高亮显示 */
    private Word currentWord;

    private final OnWordClickListener listener;

    public WordListAdapter(OnWordClickListener listener) {
        this.listener = listener;
    }

    /**
     * 设置数据
     *
     * @param words       当前词书的全部单词
     * @param currentWord 当前卡片上的单词（会被高亮）
     */
    public void setData(List<Word> words, Word currentWord) {
        this.allWords.clear();
        this.allWords.addAll(words);
        this.currentWord = currentWord;

        this.shownWords.clear();
        this.shownWords.addAll(allWords);
        notifyDataSetChanged();
    }

    /**
     * 按关键字过滤：单词包含关键字（不区分大小写）或释义包含关键字
     */
    public void filter(String keyword) {
        shownWords.clear();

        if (keyword == null || keyword.trim().isEmpty()) {
            shownWords.addAll(allWords);
        } else {
            String key = keyword.trim().toLowerCase();
            for (Word word : allWords) {
                boolean matchWord = word.getWord() != null
                        && word.getWord().toLowerCase().contains(key);
                boolean matchMeaning = word.getMeaning() != null
                        && word.getMeaning().contains(key);
                if (matchWord || matchMeaning) {
                    shownWords.add(word);
                }
            }
        }
        notifyDataSetChanged();
    }

    /** 当前单词在"显示列表"中的位置，用于打开弹窗时自动滚动到它；找不到返回 -1 */
    public int getCurrentPosition() {
        return shownWords.indexOf(currentWord);
    }

    /** 过滤后还剩多少条（判断是否显示"没有找到"） */
    public int getShownCount() {
        return shownWords.size();
    }

    @NonNull
    @Override
    public WordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_word_list, parent, false);
        return new WordViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull WordViewHolder holder, int position) {
        Word word = shownWords.get(position);

        // 序号：用它在整本词书中的位置（而不是过滤后的位置），保证序号始终正确
        int originIndex = allWords.indexOf(word);
        holder.tvIndex.setText(String.valueOf(originIndex + 1));

        holder.tvWord.setText(word.getWord());
        holder.tvMeaning.setText(word.getMeaning());

        // 高亮当前正在背的单词
        boolean isCurrent = word == currentWord;
        holder.itemView.setBackgroundResource(isCurrent
                ? R.drawable.bg_word_item_current
                : android.R.color.transparent);
        holder.tvWord.setTextColor(holder.itemView.getContext().getColor(
                isCurrent ? R.color.primary : R.color.text_primary));

        // 点击整行 → 回调跳转
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onWordClick(word);
            }
        });
    }

    @Override
    public int getItemCount() {
        return shownWords.size();
    }

    /** ViewHolder：缓存一行里的控件 */
    static class WordViewHolder extends RecyclerView.ViewHolder {
        TextView tvIndex;
        TextView tvWord;
        TextView tvMeaning;

        WordViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIndex = itemView.findViewById(R.id.tv_list_index);
            tvWord = itemView.findViewById(R.id.tv_list_word);
            tvMeaning = itemView.findViewById(R.id.tv_list_meaning);
        }
    }
}
