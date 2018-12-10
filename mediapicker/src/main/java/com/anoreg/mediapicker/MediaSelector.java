package com.anoreg.mediapicker;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.anoreg.dynamicpermission.PermissionDialog;
import com.anoreg.dynamicpermission.PermissionUtil;

import java.util.ArrayList;

/**
 * Created by FeiYi on 18-10-29.
 */
public class MediaSelector {

    //图片选择的结果
    public static final String SELECT_RESULT = "select_result";
    //最大的图片选择数
    public static final String MAX_SELECT_COUNT = "max_select_count";
    //是否单选
    public static final String IS_SINGLE = "is_single";
    //是否使用拍照功能
    public static final String USE_CAMERA = "is_camera";
    //是否点击放大图片查看
    public static final String IS_VIEW_IMAGE = "is_view_image";
    //是否显示混合页面(既可包含相机又可包含图库)
    public static final String IS_VIEW_MIX = "is_view_mix";
    //初始位置
    public static final String POSITION = "position";
    //是否按下确定
    public static final String IS_CONFIRM = "is_confirm";
    //是否裁剪
    public static final String IS_CROP = "is_crop";
    //原来已选择的图片
    public static final String SELECTED = "selected";

    public static MediaSelectorBuilder builder() {
        return new MediaSelectorBuilder();
    }

    public static class MediaSelectorBuilder {
        private boolean isCrop;
        private boolean isSingle;
        private boolean isViewImage = true;
        private int maxSelectCount;
        private boolean useCamera = true;
        private boolean isViewMix;
        private ArrayList<String> selected;

        /** 是否需要裁剪 */
        public MediaSelectorBuilder setCrop(boolean isCrop) {
            this.isCrop = isCrop;
            return this;
        }
        /** 多选模式下是否单选 */
        public MediaSelectorBuilder setSingle(boolean isSingle) {
            this.isSingle = isSingle;
            return this;
        }
        /** 多选模式下是否使用相机 */
        public MediaSelectorBuilder setUseCamera(boolean useCamera) {
            this.useCamera = useCamera;
            return this;
        }
        /** 多选模式下设置最大值 */
        public MediaSelectorBuilder setMaxSelectCount(int maxSelectCount) {
            this.maxSelectCount = maxSelectCount;
            return this;
        }
        /** 是否显示图库混合模式 */
        public MediaSelectorBuilder
        setViewMix(boolean isViewMix) {
            this.isViewMix = isViewMix;
            return this;
        }
        /** 多选模式下, 勾选已被选择列表 */
        public MediaSelectorBuilder setSelected(ArrayList<String> selected) {
            this.selected = selected;
            return this;
        }
        /** 打开媒体选择视图 */
        public void start(final FragmentActivity activity, final IMediaSelectListener listener) {
            if (!isViewMix) {
                showMediaSelector(activity, listener);
            } else {
                PermissionUtil.requestStorage(activity, new PermissionDialog.IPermissionListener() {
                    @Override
                    public void onGrant(boolean isGranted) {
                        if (isGranted) showMediaSelector(activity, listener);
                    }
                });
            }
        }

        private void showMediaSelector(FragmentActivity activity, IMediaSelectListener listener) {
            PhotoPickDialog dialog = PhotoPickDialog.newInstance(dataPackages(), listener);
            dialog.showDialog(activity);
        }

        private Bundle dataPackages() {
            Bundle bundle = new Bundle();
            bundle.putBoolean(MediaSelector.IS_CROP, isCrop);
            bundle.putBoolean(MediaSelector.IS_SINGLE, isSingle);
            bundle.putBoolean(MediaSelector.IS_VIEW_IMAGE, isViewImage);
            bundle.putBoolean(MediaSelector.USE_CAMERA, useCamera);
            bundle.putInt(MediaSelector.MAX_SELECT_COUNT, maxSelectCount);
            bundle.putBoolean(MediaSelector.IS_VIEW_MIX, isViewMix);
            bundle.putStringArrayList(MediaSelector.SELECTED, selected);
            return bundle;
        }
    }

    public interface IMediaSelectListener {
        void onSelected(ArrayList<String> mediaPaths);
    }
}
