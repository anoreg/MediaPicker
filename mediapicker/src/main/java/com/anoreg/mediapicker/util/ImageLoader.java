package com.anoreg.mediapicker.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;

/**
 * Created by FeiYi on 18-12-10.
 */
public class ImageLoader {

    private static void commonloadUrl(Context context, String url, RequestOptions requestOptions, ImageView imageView) {
        //在原图到达前, 使用缩略图作为动态占位符, 原图到达后, 缩略图被抹去. 加载大图时, 提升体验
        RequestBuilder<Drawable> thumbnailRequest = Glide.with( context ).load( url );
        RequestBuilder<Drawable> requestBuilder = Glide.with(context).load(url).thumbnail(thumbnailRequest);
        if (requestOptions != null) requestBuilder = requestBuilder.apply(requestOptions);
        requestBuilder.into(imageView);
    }

    public static void loadUrl(Context context, String url, ImageView imageView) {
        commonloadUrl(context, url, null, imageView);
    }
}
