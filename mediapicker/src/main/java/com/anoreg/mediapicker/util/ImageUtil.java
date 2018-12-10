package com.anoreg.mediapicker.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;

import com.anoreg.log_lib.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by FeiYi on 18-12-10.
 */
public class ImageUtil {

    public static String getProviderAuthority(Context context) {
        String packageName = context.getPackageName();
        return packageName + ".fileprovider";
    }

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public static File createPublicImageFile() {
        if (isExternalStorageWritable()) {
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
            // This location works best if you want the created images to be shared
            // between applications and persist after your app has been uninstalled.

            // Create the storage directory if it does not exist
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    Log.d("Camera", "failed to create directory");
                    return null;
                }
            }

            // Create a media file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            return new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        }
        return null;
    }

    public static File createInternalImageFile(Context context) {
        return new File(getAvailableCacheDir(context), "IMG_" + System.currentTimeMillis() + ".jpg");
    }

    public static File getAvailableCacheDir(Context context) {
        if (isExternalStorageWritable()) {
            return context.getExternalCacheDir();
        } else {
            // 只有此应用才能访问。拍照的时候有问题，因为拍照的应用写入不了该文件
            return context.getCacheDir();
        }
    }

    public static Uri getImageUriForFile(Context context, String authority, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) return FileProvider.getUriForFile(context, authority, file);
        else return Uri.fromFile(file);
    }

    public static String getImagePathFromURI(Context context, Uri contentUri) {
        if (contentUri == null) return null;
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        if (cursor != null) {
            cursor.close();
        }
        return res;
    }

}
