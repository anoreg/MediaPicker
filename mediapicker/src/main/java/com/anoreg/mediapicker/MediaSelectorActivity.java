package com.anoreg.mediapicker;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.anoreg.dynamicpermission.PermissionDialog;
import com.anoreg.dynamicpermission.PermissionUtil;
import com.anoreg.log_lib.Log;
import com.anoreg.mediapicker.adapter.FolderAdapter;
import com.anoreg.mediapicker.adapter.ImageAdapter;
import com.anoreg.mediapicker.entity.Folder;
import com.anoreg.mediapicker.entity.Image;
import com.anoreg.mediapicker.model.ImageModel;
import com.anoreg.mediapicker.util.DateUtil;
import com.anoreg.mediapicker.util.ImageUtil;
import com.anoreg.mediapicker.util.UIUtil;

import java.io.File;
import java.util.ArrayList;

public class MediaSelectorActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PREVIEW = 2;
    private boolean isOpenFolder;
    private boolean isShowTime;
    private boolean isInitFolder;

    private int mMaxCount;
    private boolean isSingle;
    private boolean isViewImage;
    private boolean useCamera;
    //用于接收从外面传进来的已选择的图片列表。当用户原来已经有选择过图片，现在重新打开选择器，允许用
    // 户把先前选过的图片传进来，并把这些图片默认为选中状态。
    private ArrayList<String> mSelectedImages;

    private ImageAdapter mAdapter;
    private GridLayoutManager mLayoutManager;
    private ArrayList<Folder> mFolders;
    private Folder mFolder;

    private Handler mHideHandler = new Handler();
    private Runnable mHide = new Runnable() {
        @Override
        public void run() {
            hideTime();
        }
    };

    private File takePhotoFile;

    private ImageView btnBack;
    private TextView tvPreview, tvConfirm, tvFolderName, tvTime;
    private View btnFolder, masking;
    private RecyclerView rvImage, rvFolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        setContentView(R.layout.activity_media_selector);
        initView();

        Intent intent = getIntent();
        mMaxCount = intent.getIntExtra(MediaSelector.MAX_SELECT_COUNT, 0);
        isSingle = intent.getBooleanExtra(MediaSelector.IS_SINGLE, false);
        isViewImage = intent.getBooleanExtra(MediaSelector.IS_VIEW_IMAGE, true);
        useCamera = intent.getBooleanExtra(MediaSelector.USE_CAMERA, true);
        mSelectedImages = intent.getStringArrayListExtra(MediaSelector.SELECTED);

        setStatusBarColor();
        initListener();
        initImageList();
        loadImageForSDCard();
        hideFolderList();
        setSelectImageCount(0);
    }

    private void initView() {
        btnBack = findViewById(R.id.btn_back);
        tvPreview = findViewById(R.id.tv_preview);
        tvConfirm = findViewById(R.id.tv_confirm);
        btnFolder = findViewById(R.id.btn_folder);
        tvFolderName = findViewById(R.id.tv_folder_name);
        masking = findViewById(R.id.masking);
        rvImage = findViewById(R.id.rv_image);
        rvFolder = findViewById(R.id.rv_folder);
        tvTime = findViewById(R.id.tv_time);
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
                finish();
            }
        });

        tvPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UIUtil.resetClick(v);
                ArrayList<Image> images = new ArrayList<>();
                images.addAll(mAdapter.getSelectImages());
                toPreviewActivity(images, 0);
            }
        });

        tvConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UIUtil.resetClick(v);
                confirm();
            }
        });

        btnFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInitFolder) {
                    if (isOpenFolder) {
                        closeFolder();
                    } else {
                        openFolder();
                    }
                }
            }
        });

        masking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UIUtil.resetClick(v);
                closeFolder();
            }
        });

        rvImage.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                changeTime();
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                changeTime();
            }
        });
    }

    private void toPreviewActivity(ArrayList<Image> images, int position) {
        if (images != null && !images.isEmpty()) {
            PreviewActivity.openActivity(this, images,
                    mAdapter.getSelectImages(), isSingle, mMaxCount, position, REQUEST_IMAGE_PREVIEW);
        }
    }

    /**
     * 初始化图片列表
     */
    private void initImageList() {
        // 判断屏幕方向
        Configuration configuration = getResources().getConfiguration();
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            mLayoutManager = new GridLayoutManager(this, 3);
        } else {
            mLayoutManager = new GridLayoutManager(this, 5);
        }

        rvImage.setLayoutManager(mLayoutManager);
        mAdapter = new ImageAdapter(this, mMaxCount, isSingle, isViewImage);
        rvImage.setAdapter(mAdapter);
        ((SimpleItemAnimator) rvImage.getItemAnimator()).setSupportsChangeAnimations(false);
        if (mFolders != null && !mFolders.isEmpty()) {
            setFolder(mFolders.get(0));
        }
        mAdapter.setOnImageSelectListener(new ImageAdapter.OnImageSelectListener() {
            @Override
            public void OnImageSelect(Image image, boolean isSelect, int selectCount) {
                setSelectImageCount(selectCount);
            }
        });
        mAdapter.setOnItemClickListener(new ImageAdapter.OnItemClickListener() {
            @Override
            public void OnItemClick(Image image, int position) {
                toPreviewActivity(mAdapter.getData(), position);
            }

            @Override
            public void OnCameraClick() {
                checkPermissionAndCamera();
            }
        });
    }

    /**
     * 设置选中的文件夹，同时刷新图片列表
     *
     * @param folder
     */
    private void setFolder(Folder folder) {
        if (folder != null && mAdapter != null && !folder.equals(mFolder)) {
            mFolder = folder;
            tvFolderName.setText(folder.getName());
            rvImage.scrollToPosition(0);
            mAdapter.refresh(folder.getImages(), folder.isUseCamera());
        }
    }

    /**
     * 初始化图片文件夹列表
     */
    private void initFolderList() {
        if (mFolders != null && !mFolders.isEmpty()) {
            isInitFolder = true;
            rvFolder.setLayoutManager(new LinearLayoutManager(this));
            FolderAdapter adapter = new FolderAdapter(this, mFolders);
            adapter.setOnFolderSelectListener(new FolderAdapter.OnFolderSelectListener() {
                @Override
                public void OnFolderSelect(Folder folder) {
                    setFolder(folder);
                    closeFolder();
                }
            });
            rvFolder.setAdapter(adapter);
        }
    }

    /**
     * 刚开始的时候文件夹列表默认是隐藏的
     */
    private void hideFolderList() {
        rvFolder.post(new Runnable() {
            @Override
            public void run() {
                rvFolder.setTranslationY(rvFolder.getHeight());
                rvFolder.setVisibility(View.GONE);
            }
        });
    }

    /**
     * 弹出文件夹列表
     */
    private void openFolder() {
        if (!isOpenFolder) {
            masking.setVisibility(View.VISIBLE);
            ObjectAnimator animator = ObjectAnimator.ofFloat(rvFolder, "translationY",
                    rvFolder.getHeight(), 0).setDuration(300);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    rvFolder.setVisibility(View.VISIBLE);
                }
            });
            animator.start();
            isOpenFolder = true;
        }
    }

    /**
     * 收起文件夹列表
     */
    private void closeFolder() {
        if (isOpenFolder) {
            masking.setVisibility(View.GONE);
            ObjectAnimator animator = ObjectAnimator.ofFloat(rvFolder, "translationY",
                    0, rvFolder.getHeight()).setDuration(300);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    rvFolder.setVisibility(View.GONE);
                }
            });
            animator.start();
            isOpenFolder = false;
        }
    }

    private void confirm() {
        if (mAdapter == null) {
            return;
        }
        //因为图片的实体类是Image，而我们返回的是String数组，所以要进行转换。
        ArrayList<Image> selectImages = mAdapter.getSelectImages();
        ArrayList<String> images = new ArrayList<>();
        for (Image image : selectImages) {
            images.add(image.getPath());
        }

        //点击确定，把选中的图片通过Intent传给上一个Activity。
        setResult(images);
        finish();
    }

    private void setResult(ArrayList<String> images) {
        Intent intent = new Intent();
        intent.putStringArrayListExtra(MediaSelector.SELECT_RESULT, images);
        setResult(RESULT_OK, intent);
    }

    /**
     * 隐藏时间条
     */
    private void hideTime() {
        if (isShowTime) {
            ObjectAnimator.ofFloat(tvTime, "alpha", 1, 0).setDuration(300).start();
            isShowTime = false;
        }
    }

    /**
     * 显示时间条
     */
    private void showTime() {
        if (!isShowTime) {
            ObjectAnimator.ofFloat(tvTime, "alpha", 0, 1).setDuration(300).start();
            isShowTime = true;
        }
    }

    /**
     * 改变时间条显示的时间（显示图片列表中的第一个可见图片的时间）
     */
    private void changeTime() {
        int firstVisibleItem = getFirstVisibleItem();
        Image image = mAdapter.getFirstVisibleImage(firstVisibleItem);
        if (image != null) {
            String time = DateUtil.getDuration(this, image.getTime() * 1000);
            tvTime.setText(time);
            showTime();
            mHideHandler.removeCallbacks(mHide);
            mHideHandler.postDelayed(mHide, 1500);
        }
    }

    private int getFirstVisibleItem() {
        return mLayoutManager.findFirstVisibleItemPosition();
    }

    /**
     * 检查权限并拍照。
     */
    private void checkPermissionAndCamera() {
        PermissionUtil.requestCamera(this, new PermissionDialog.IPermissionListener() {
            @Override
            public void onGrant(boolean isGranted) {
                if (isGranted) dispatchTakePictureIntent();
            }
        });
    }

    /**
     * 从SDCard加载图片。
     */
    private void loadImageForSDCard() {
        ImageModel.loadImageForSDCard(this, new ImageModel.DataCallback() {
            @Override
            public void onSuccess(ArrayList<Folder> folders) {
                mFolders = folders;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mFolders != null && !mFolders.isEmpty()) {
                            initFolderList();
                            mFolders.get(0).setUseCamera(useCamera);
                            setFolder(mFolders.get(0));
                            if (mSelectedImages != null && mAdapter != null) {
                                mAdapter.setSelectedImages(mSelectedImages);
                                mSelectedImages = null;
                            }
                        }
                    }
                });
            }
        });
    }

    private void setSelectImageCount(int count) {
        if (count == 0) {
            tvConfirm.setEnabled(false);
            tvPreview.setEnabled(false);
            tvConfirm.setText("确定");
            tvPreview.setText("预览");
        } else {
            tvConfirm.setEnabled(true);
            tvPreview.setEnabled(true);
            tvPreview.setText("预览(" + count + ")");
            if (isSingle) {
                tvConfirm.setText("确定");
            } else if (mMaxCount > 0) {
                tvConfirm.setText("确定(" + count + "/" + mMaxCount + ")");
            } else {
                tvConfirm.setText("确定(" + count + ")");
            }
        }
    }

    private void dispatchTakePictureIntent() {
        takePhotoFile = ImageUtil.createPublicImageFile();
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri takePhotoUri = ImageUtil.getImageUriForFile(this, ImageUtil.getProviderAuthority(this), takePhotoFile);
        Log.d("take photo uri:" + takePhotoUri);
        takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        takePictureIntent.putExtra("return-data", false);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, takePhotoUri);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(this, R.string.picker_err_pick_img, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 处理图片预览页返回的结果
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PREVIEW) {
            if (data != null && data.getBooleanExtra(MediaSelector.IS_CONFIRM, false)) {
                //如果用户在预览页点击了确定，就直接把用户选中的图片返回给用户。
                confirm();
            } else {
                //否则，就刷新当前页面。
                mAdapter.notifyDataSetChanged();
                setSelectImageCount(mAdapter.getSelectImages().size());
            }
        } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                galleryAddPic();
                if (mFolders != null && mFolders.size() > 0) {
                    String path = takePhotoFile.getAbsolutePath();
                    Folder folder = mFolders.get(0);
                    Image image = new Image(path,
                            takePhotoFile.lastModified()/1000, takePhotoFile.getName(), "image/jpg");
                    folder.getImages().add(0, image);
                    mAdapter.notifyItemInserted(1);
                }
            }
        }
    }

    public void galleryAddPic() {
        if (takePhotoFile == null) return;

        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(takePhotoFile);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
    }

    /**
     * 横竖屏切换处理
     *
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mLayoutManager != null && mAdapter != null) {
            //切换为竖屏
            if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                mLayoutManager.setSpanCount(3);
            }
            //切换为横屏
            else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mLayoutManager.setSpanCount(5);
            }
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onBackPressed() {
        if (isOpenFolder) {
            closeFolder();
        } else {
            super.onBackPressed();
        }
    }
}
