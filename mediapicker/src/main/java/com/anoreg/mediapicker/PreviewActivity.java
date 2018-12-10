package com.anoreg.mediapicker;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.anoreg.log_lib.Log;
import com.anoreg.mediapicker.adapter.ImagePagerAdapter;
import com.anoreg.mediapicker.entity.Image;
import com.anoreg.mediapicker.util.UIUtil;
import com.anoreg.mediapicker.view.PhotoViewPager;

import java.util.ArrayList;

public class PreviewActivity extends AppCompatActivity {

    private TextView tvIndicator, tvConfirm, tvSelect;
    private PhotoViewPager vpImage;
    private View rlTopBar, rlBottomBar;
    private ImageView btnBack;

    private boolean isShowBar = true;
    private boolean isConfirm;
    private boolean isSingle;
    private int mMaxCount;

    //tempImages和tempSelectImages用于图片列表数据的页面传输。
    //之所以不要Intent传输这两个图片列表，因为要保证两位页面操作的是同一个列表数据，同时可以避免数据量大时，
    // 用Intent传输发生的错误问题。
    private static ArrayList<Image> tempImages;
    private static ArrayList<Image> tempSelectImages;

    private ArrayList<Image> mImages;
    private ArrayList<Image> mSelectImages;

    public static void openActivity(Activity activity, ArrayList<Image> images,
                                    ArrayList<Image> selectImages, boolean isSingle,
                                    int maxSelectCount, int position, int requestCode) {
        tempImages = images;
        tempSelectImages = selectImages;
        Intent intent = new Intent(activity, PreviewActivity.class);
        intent.putExtra(MediaSelector.MAX_SELECT_COUNT, maxSelectCount);
        intent.putExtra(MediaSelector.IS_SINGLE, isSingle);
        intent.putExtra(MediaSelector.POSITION, position);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        setContentView(R.layout.activity_preview);
        setStatusBarColor();

        Intent intent = getIntent();
        mMaxCount = intent.getIntExtra(MediaSelector.MAX_SELECT_COUNT, 0);
        isSingle = intent.getBooleanExtra(MediaSelector.IS_SINGLE, false);

        mImages = tempImages;
        tempImages = null;
        mSelectImages = tempSelectImages;
        tempSelectImages = null;

        initView();
        initListener();
        initViewPager();
        tvIndicator.setText(1 + "/" + mImages.size());
        changeSelect(mImages.get(0));
        vpImage.setCurrentItem(intent.getIntExtra(MediaSelector.POSITION, 0));
    }

    private void initView() {
        final View decorView = getWindow().getDecorView();
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                Log.d("system ui visibility:" + visibility + ", isShowBar:" + isShowBar);
                // Note that system bars will only be "visible" if none of the
                // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
                if (visibility == 0) {
                    // The system bars are visible. Make any desired
                    if (!isShowBar) {
                        decorView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                setStatusBarVisible(false);
                            }
                        }, 1000);
                    }
                } else {
                    // The system bars are NOT visible. Make any desired
                }
            }
        });
        tvIndicator = findViewById(R.id.tv_indicator);
        vpImage = findViewById(R.id.vp_image);
        rlTopBar = findViewById(R.id.rl_top_bar);
        rlBottomBar = findViewById(R.id.rl_bottom_bar);
        btnBack = findViewById(R.id.btn_back);
        tvConfirm = findViewById(R.id.tv_confirm);
        tvSelect = findViewById(R.id.tv_select);
    }

    /**
     * 初始化ViewPager
     */
    private void initViewPager() {
        ImagePagerAdapter adapter = new ImagePagerAdapter(mImages, new ImagePagerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position, Image image) {
                if (isShowBar) {
                    hideBar();
                } else {
                    showBar();
                }
            }
        });
        vpImage.setAdapter(adapter);
        vpImage.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                tvIndicator.setText(position + 1 + "/" + mImages.size());
                changeSelect(mImages.get(position));
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    /**
     * 显示和隐藏状态栏
     *
     * @param show
     */
    private void setStatusBarVisible(boolean show) {
        View decordView = getWindow().getDecorView();
        int uiOption;
        if (show) {
            uiOption = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            setStatusBarColor();
        } else {
            uiOption =  View.SYSTEM_UI_FLAG_IMMERSIVE
                    // Set the content to appear under the system bars so that the
                    // content doesn't resize when the system bars hide and show.
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    // Hide the nav bar and status bar
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
        }
        decordView.setSystemUiVisibility(uiOption);
    }

    /**
     * 显示头部和尾部栏
     */
    private void showBar() {
        isShowBar = true;
        setStatusBarVisible(true);
        //添加延时，保证StatusBar完全显示后再进行动画。
        rlTopBar.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (rlTopBar != null) {
                    ObjectAnimator animator = ObjectAnimator.ofFloat(rlTopBar, "translationY",
                            rlTopBar.getTranslationY(), 0).setDuration(300);
                    animator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            super.onAnimationStart(animation);
                            if (rlTopBar != null) {
                                rlTopBar.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                    animator.start();
                    ObjectAnimator.ofFloat(rlBottomBar, "translationY", rlBottomBar.getTranslationY(), 0)
                            .setDuration(300).start();
                }
            }
        }, 100);
    }

    /**
     * 隐藏头部和尾部栏
     */
    private void hideBar() {
        isShowBar = false;
        ObjectAnimator animator = ObjectAnimator.ofFloat(rlTopBar, "translationY",
                0, -rlTopBar.getHeight()).setDuration(300);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (rlTopBar != null) {
                    rlTopBar.setVisibility(View.GONE);
                    //添加延时，保证rlTopBar完全隐藏后再隐藏StatusBar。
                    rlTopBar.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            setStatusBarVisible(false);
                        }
                    }, 5);
                }
            }
        });
        animator.start();
        ObjectAnimator.ofFloat(rlBottomBar, "translationY", 0, rlBottomBar.getHeight())
                .setDuration(300).start();
    }

    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int statusBarColor = ContextCompat.getColor(this,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? R.color.picker_statusBar_color_primary : R.color.picker_statusBar_color_dark);
            UIUtil.setStatusBarColor(this, statusBarColor, true);
        }
    }

    private void initListener() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UIUtil.resetClick(v);
                onBackPressed();
            }
        });
        tvConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UIUtil.resetClick(v);
                isConfirm = true;
                onBackPressed();
            }
        });
        tvSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickSelect();
            }
        });
    }

    private void clickSelect() {
        int position = vpImage.getCurrentItem();
        if (mImages != null && mImages.size() > position) {
            Image image = mImages.get(position);
            if (mSelectImages.contains(image)) {
                mSelectImages.remove(image);
            } else if (isSingle) {
                mSelectImages.clear();
                mSelectImages.add(image);
            } else if (mMaxCount <= 0 || mSelectImages.size() < mMaxCount) {
                mSelectImages.add(image);
            }
            changeSelect(image);
        }
    }

    private void changeSelect(Image image) {
        tvSelect.setCompoundDrawablesWithIntrinsicBounds(mSelectImages.contains(image) ?
                R.drawable.icon_image_select : R.drawable.icon_image_un_select, 0, 0, 0);
        setSelectImageCount(mSelectImages.size());
    }

    private void setSelectImageCount(int count) {
        if (count == 0) {
            tvConfirm.setEnabled(false);
            tvConfirm.setText("确定");
        } else {
            tvConfirm.setEnabled(true);
            if (isSingle) {
                tvConfirm.setText("确定");
            } else if (mMaxCount > 0) {
                tvConfirm.setText("确定(" + count + "/" + mMaxCount + ")");
            } else {
                tvConfirm.setText("确定(" + count + ")");
            }
        }
    }

    @Override
    public void onBackPressed() {
        //Activity关闭时，通过Intent把用户的操作(确定/返回)传给MediaSelectorActivity。
        Intent intent = new Intent();
        intent.putExtra(MediaSelector.IS_CONFIRM, isConfirm);
        setResult(RESULT_OK, intent);
        super.onBackPressed();
    }
}
