package com.anoreg.mediapicker.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.anoreg.log_lib.Log;
import com.anoreg.mediapicker.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by FeiYi on 17-9-5.
 */

public class DateUtil {
    public static final DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
    public static final DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);

    public static String formatDateWithTime(Date date) {
        return df1.format(date);
    }
    public static String formatDateWithDay(Date date) {
        return df2.format(date);
    }

    public static long dateToMilliseconds(Date date) {
        return date.getTime();
    }

    public static Date currentDate() {
        return new Date(System.currentTimeMillis());
    }

    public static Date getDate(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        return c.getTime();
    }

    public static String getDuration(Context context, Date rel_time) {
        return getDuration(context, rel_time, currentDate());
    }

    public static String getDuration(Context context, long rel_time) {
        return getDuration(context, getDate(rel_time), currentDate());
    }

    public static String getDuration(Context context, String rel_time, String now_time) {
        if (TextUtils.isEmpty(now_time)) {
            if (!TextUtils.isEmpty(rel_time)) {
                String showTime = rel_time.substring(0, rel_time.lastIndexOf(":"));
                Log.d("showTime:" + showTime);
                return showTime;
            }
            return "时间错误";
        }
        String backStr = "";
        Date d1 = null;
        Date d2 = null;
        try {
            d1 = df1.parse(rel_time);
            d2 = df1.parse(now_time);
            backStr = getDuration(context, d1, d2);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return backStr;
    }

    @NonNull
    public static String getDuration(Context context, Date realtime, Date nowtime) {
        String backStr;
        // 毫秒ms
        long diff = nowtime.getTime() - realtime.getTime();

        long diffMinutes = diff / (60 * 1000) % 60;
        long diffHours = diff / (60 * 60 * 1000) % 24;
        long diffDays = diff / (24 * 60 * 60 * 1000);

        if (diffDays != 0) {
            if (diffDays <= 7) {
                backStr = String.valueOf(diffDays) + context.getString(R.string.Days_ago);
            } else {
                backStr = formatDateWithDay(realtime);
            }
        } else if (diffHours != 0) {
            backStr = String.valueOf(diffHours) + context.getString(R.string.An_hour_ago);
        } else if (diffMinutes != 0) {
            backStr = String.valueOf(diffMinutes) + context.getString(R.string.minutes_ago);
        } else {
            backStr = context.getString(R.string.just);
        }
        return backStr;
    }
}
