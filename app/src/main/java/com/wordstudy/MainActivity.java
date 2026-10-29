package com.wordstudy;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.wordstudy.ui.ReciteFragment;
import com.wordstudy.ui.VocabBookFragment;

   /**主界面
     */
public class MainActivity extends AppCompatActivity {

    /** Fragment 的 tag，用于界面重建（如旋转屏幕）后重新找到它们 */
    private static final String TAG_RECITE = "tag_recite";
    private static final String TAG_VOCAB = "tag_vocab";

    private Fragment reciteFragment;    // 背诵页
    private Fragment vocabBookFragment; // 生词本页
    private Fragment activeFragment;    // 当前正在显示的页面

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        FragmentManager fm = getSupportFragmentManager();

        if (savedInstanceState == null) {
            // ---------- 第一次进入 App ----------
            reciteFragment = new ReciteFragment();
            vocabBookFragment = new VocabBookFragment();

            // 两个都添加进来，但生词本先隐藏，默认显示背诵页
            fm.beginTransaction()
                    .add(R.id.fragment_container, vocabBookFragment, TAG_VOCAB)
                    .hide(vocabBookFragment)
                    .add(R.id.fragment_container, reciteFragment, TAG_RECITE)
                    .commit();

            activeFragment = reciteFragment;
        } else {
            // ---------- 界面重建（例如屏幕旋转、系统回收后恢复） ----------
            reciteFragment = fm.findFragmentByTag(TAG_RECITE);
            vocabBookFragment = fm.findFragmentByTag(TAG_VOCAB);
            activeFragment = (vocabBookFragment != null && !vocabBookFragment.isHidden())
                    ? vocabBookFragment : reciteFragment;
        }

        // 设置底部 Tab 点击监听
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_recite) {
                switchFragment(reciteFragment);
                return true;
            } else if (id == R.id.nav_vocab) {
                switchFragment(vocabBookFragment);
                return true;
            }
            return false;
        });
    }

    /**
     * 切换显示的 Fragment：显示目标页面，隐藏其他页面
     */
    private void switchFragment(Fragment target) {
        if (target == null || target == activeFragment) {
            return;   // 已经在当前页面，什么都不做
        }
        getSupportFragmentManager().beginTransaction()
                .hide(activeFragment)
                .show(target)
                .commit();
        activeFragment = target;
    }
}
