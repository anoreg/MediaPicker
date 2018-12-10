package com.anoreg.mediapicker;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.anoreg.dynamicpermission.PermissionDialog;
import com.anoreg.dynamicpermission.PermissionUtil;
import com.anoreg.log_lib.Log;
import com.anoreg.mediapicker.util.ImageUtil;
import com.anoreg.mediapicker.util.UIUtil;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;

/**
 * Created by FeiYi on 18-10-16.
 */
public class PhotoPickDialog extends DialogFragment{

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final int REQUEST_IMAGE_CROP = 3;
    private static final int REQUEST_IMAGE_MIX = 4;

    private MediaSelector.IMediaSelectListener mListener;
    private File takePhotoFile;
    private File cropFile;
    private boolean isViewMix;
    private boolean isCrop;

    public interface PickListener {
        void onPickResult(String imagePath);
    }

    public PhotoPickDialog() {
        //Require empty constructor
    }

    public static PhotoPickDialog newInstance(@NonNull Bundle bundle, MediaSelector.IMediaSelectListener pickListener) {
        PhotoPickDialog photoPickDialog = new PhotoPickDialog();
        photoPickDialog.setArguments(bundle);
        photoPickDialog.mListener = pickListener;
        return photoPickDialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //此处只能用0作为theme, 否则widget的selectItemBackground效果为黄色, 很丑
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);

        isViewMix = getArguments().getBoolean(MediaSelector.IS_VIEW_MIX);
        isCrop = getArguments().getBoolean(MediaSelector.IS_CROP);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        int layoutId = isViewMix ? 0 : R.layout.dialog_pick_img;
        View view = layoutId > 0 ? inflater.inflate(layoutId, container, false)
                : super.onCreateView(inflater, container, savedInstanceState);
        initWindow();
        return view;
    }

    private void initWindow() {
        Window window = getDialogWindow();
        if (window != null) {
            //默认从底部弹出
            window.setGravity(Gravity.BOTTOM);
            window.setDimAmount(0.25f);
            window.getAttributes().windowAnimations = R.style.PickerBottomInAnimation;
            //去掉Dialog外层shadow
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    public Window getDialogWindow() {
        Dialog dialog = getDialog();
        Window window = null;
        if (dialog != null) window = dialog.getWindow();
        return window;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initView();
    }

    private void initView() {
        if (isViewMix) {
            setCancelable(false);
            getDialog().setCanceledOnTouchOutside(false);
            showImageMix();
        } else {
            View view = getView();
            view.findViewById(R.id.take_photo).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UIUtil.resetClick(v);
                    takePhoto();
                }
            });
            view.findViewById(R.id.pick_photo).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UIUtil.resetClick(v);
                    pickPhoto();
                }
            });
            view.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UIUtil.resetClick(v);
                    dismissDialog();
                }
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        adapterSize();
        View view = getView();
        if (view != null) {
            view.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Window window = getDialogWindow();
                    //不设置的话, 按下home键, 再重新回到界面, 动画会被重新执行, 所以进入动画执行后, 置空进入动画
                    if (window != null) window.setWindowAnimations(R.style.PickerBottomInAnimation);
                }
            }, 500);
        }
    }

    private void adapterSize() {
        Window window = getDialogWindow();
        if (window != null) {
            WindowManager.LayoutParams lps = window.getAttributes();
            lps.width = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(lps);
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        //the fields will be released system release ram
        // (screen lock, low memary, etc), so we just close dialog here
        if (getActivity() == null || getActivity().isFinishing()) {
            dismissDialog();
        }
    }

    private void showImageMix() {
        Intent intent = new Intent(getActivity(), MediaSelectorActivity.class);
        intent.putExtras(getArguments());
        startActivityForResult(intent, REQUEST_IMAGE_MIX);
    }

    private void onPickResult(String path) {
        ArrayList<String> mediaPath = new ArrayList<>(1);
        mediaPath.add(path);
        onPickResults(mediaPath);
    }

    private void onPickResults(ArrayList<String> paths) {
        Log.d("pick result:\n" + paths);
        if (mListener != null) mListener.onSelected(paths);
        dismissDialog();
    }

    private void pickPhoto() {
        PermissionUtil.requestStorage(getActivity(), new PermissionDialog.IPermissionListener() {
            @Override
            public void onGrant(boolean isGranted) {
                if (isGranted) {
                    dispatchPickPictureIntent();
                }
            }
        });
    }

    private void takePhoto() {
        PermissionUtil.requestCamera(getActivity(), new PermissionDialog.IPermissionListener() {
            @Override
            public void onGrant(boolean isGranted) {
                if (isGranted) {
                    dispatchTakePictureIntent();
                }
            }
        });
    }

    /**
     * 发送 Intent 跳转到系统图库页面
     */
    private void dispatchPickPictureIntent() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, REQUEST_IMAGE_PICK);
    }

    /**
     * 发送 Intent 跳转到系统拍照页面
     */
    private void dispatchTakePictureIntent() {
        takePhotoFile = ImageUtil.createPublicImageFile();
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri takePhotoUri = ImageUtil.getImageUriForFile(getActivity(), ImageUtil.getProviderAuthority(getActivity()), takePhotoFile);
        Log.d("take photo uri:" + takePhotoUri);
        takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                |Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        takePictureIntent.putExtra("return-data", false);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, takePhotoUri);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(getActivity(), R.string.picker_err_pick_img, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 系统自带裁剪方法
     * @param imagePath 需要被裁剪图片绝对路径
     */
    private void dispatchCropPictureIntent(String imagePath) {
        cropFile = ImageUtil.createInternalImageFile(getActivity());
        Uri sourceUri = ImageUtil.getImageUriForFile(getActivity(), ImageUtil.getProviderAuthority(getActivity()), new File(imagePath));
        Uri cropUri = ImageUtil.getImageUriForFile(getActivity(), ImageUtil.getProviderAuthority(getActivity()), cropFile);
        Intent cropPictureIntent = new Intent("com.android.camera.action.CROP");
        cropPictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                |Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        cropPictureIntent.putExtra("crop", "true");
//        cropPictureIntent.putExtra("aspectX", 3);
//        cropPictureIntent.putExtra("aspectY", 2);
//        cropPictureIntent.putExtra("outputX", 640);
//        cropPictureIntent.putExtra("outputY", 427);
        cropPictureIntent.putExtra("scale", true);
        cropPictureIntent.putExtra("return-data", false);
        cropPictureIntent.setDataAndType(sourceUri, "image/*");
        cropPictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cropUri);
        cropPictureIntent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        List<ResolveInfo> infoList = queryActivityByIntent(cropPictureIntent);
        if (infoList != null && !infoList.isEmpty()) {
            for (ResolveInfo resolveInfo : infoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                getActivity().grantUriPermission(packageName, cropUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
            startActivityForResult(cropPictureIntent, REQUEST_IMAGE_CROP);
        } else {
            Log.d("no app can crop image");
            onPickResult(imagePath);
        }
    }

    private void cropImage(String imagePath) {
        cropFile = ImageUtil.createInternalImageFile(getActivity());
        Uri sourceUri = Uri.fromFile(new File(imagePath));
        Uri cropUri = Uri.fromFile(new File(cropFile.getAbsolutePath()));
        UCrop uCrop = UCrop.of(sourceUri, cropUri);

        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
//        options.setFreeStyleCropEnabled(true);
        options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.ALL);
        options.setStatusBarColor(getColor(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
                R.color.picker_statusBar_color_primary : R.color.picker_statusBar_color_dark));
        options.setActiveWidgetColor(getColor(R.color.picker_color_accent));
        options.setToolbarWidgetColor(getColor(R.color.picker_text_color_dark));
        options.setToolbarColor(getColor(R.color.picker_color_primary));
        uCrop.withOptions(options);

        uCrop.start(getActivity(), this, REQUEST_IMAGE_CROP);
    }

    private int getColor(int colorResId) {
        return ContextCompat.getColor(getActivity(), colorResId);
    }

    private List<ResolveInfo> queryActivityByIntent(Intent intent){
        return getActivity().getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (RESULT_OK == resultCode) {
            switch (requestCode) {
                case REQUEST_IMAGE_CAPTURE:
                    handlePickResult(takePhotoFile.getAbsolutePath());
                    break;
                case REQUEST_IMAGE_PICK:
                    Uri pickUrl = data.getData();
                    Log.d("pick photo url:" + pickUrl);
                    handlePickResult(ImageUtil.getImagePathFromURI(getActivity(), pickUrl));
                    break;
                case REQUEST_IMAGE_CROP:
                    onPickResult(cropFile.getAbsolutePath());
                    break;
                case REQUEST_IMAGE_MIX:
                    ArrayList<String> images = data.getStringArrayListExtra(MediaSelector.SELECT_RESULT);
                    onPickResults(images);
                    break;
                default:
                    break;
            }
        } else {
            switch (requestCode) {
                case REQUEST_IMAGE_MIX:
                    dismissDialog();
                    break;
            }
        }
    }

    private void handlePickResult(String path) {
        Log.d("pick path:" + path);
        if (!TextUtils.isEmpty(path)) {
            if (!isCrop) {
                onPickResult(path);
            } else {
//            dispatchCropPictureIntent(path);
                cropImage(path);
            }
        } else {
            Toast.makeText(getActivity(), R.string.picker_err_pick_img, Toast.LENGTH_SHORT).show();
        }
    }

    public void showDialog(FragmentActivity context) {
        FragmentManager manager;
        if (context != null && (manager = context.getSupportFragmentManager()) != null) {
            show(manager, "PhotoPickDialog");
        }
    }

    public void dismissDialog() {
        Dialog dialog = getDialog();
        if (dialog != null && dialog.isShowing()) {
            dismissAllowingStateLoss();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
