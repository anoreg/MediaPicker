package com.anoreg.mediapicker.util;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * Created by FeiYi on 18-12-10.
 */
public class UIUtil {
    public static void resetClick(final View view) {
        view.setEnabled(false);
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                view.setEnabled(true);
            }
        }, 1000);
    }

    public static void setStatusBarColor(Activity activity, int color, boolean lightStatusBar) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window window = activity.getWindow();
            if (window != null) {
                View view = window.getDecorView();
                int flags = view.getSystemUiVisibility();
                if (lightStatusBar) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    window.getDecorView().setSystemUiVisibility(flags);
                } else {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    view.setSystemUiVisibility(flags);
                }
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(color);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            if (window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(color);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // TODO: 18-8-3 kitkat 可以通过在布局中增加paddingview来作为背景色
//            mPaddingView.setBackgroundColor(color);
        }
    }
}
