package com.anoreg.mediapicker.adapter;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.anoreg.mediapicker.entity.Image;
import com.anoreg.mediapicker.util.ImageLoader;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by FeiYi on 18-10-29.
 */
public class ImagePagerAdapter extends PagerAdapter {
    private ArrayList<Image> mImgList;
    private OnItemClickListener mListener;

    public ImagePagerAdapter(ArrayList<Image> imgList, OnItemClickListener listener) {
        mListener = listener;
        mImgList = imgList;
    }

    @Override
    public int getCount() {
        return mImgList == null ? 0 : mImgList.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, final int position) {
        final Image image = mImgList.get(position);
        Context context = container.getContext();
        PhotoView photoView = new PhotoView(context);

        ImageLoader.loadUrl(context, Uri.fromFile(new File(image.getPath())).toString(), photoView);
        container.addView(photoView);
        photoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onItemClick(position, image);
                }
            }
        });
        return photoView;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        if (object instanceof PhotoView) {
            PhotoView view = (PhotoView) object;
            view.setImageDrawable(null);
            container.removeView(view);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position, Image image);
    }
}
