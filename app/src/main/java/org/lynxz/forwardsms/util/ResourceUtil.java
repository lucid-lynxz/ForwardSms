package org.lynxz.forwardsms.util;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import org.jetbrains.annotations.NotNull;
import org.lynxz.forwardsms.SmsApplication;

/**
 * version: 1
 * date: 2020.9.26
 * <p>
 * 资源获取工具类
 * 根据项目的不同修改 {@link #getApplication()} 方法, 返回非空application
 */
public class ResourceUtil {
    private static Application mApplication;

    @NotNull
    private static Application getApplication() {
        if (mApplication == null) {
            mApplication = SmsApplication.app;
        }
        return mApplication;
    }

    /**
     * 获取颜色值
     */
    @ColorInt
    public static int getColor(@ColorRes int resId) {
        return getApplication().getResources().getColor(resId);
    }

    /**
     * 获取资源字符串
     */
    public static String getString(@StringRes int resId) {
        return getApplication().getResources().getString(resId);
    }

    /**
     * @param resId 尺寸资源id
     * @return 返回对应的px值
     */
    public static int getDp2px(@DimenRes int resId) {
        return getApplication().getResources().getDimensionPixelSize(resId);
    }

    /**
     * 获取资源图片
     */
    public static Drawable getDrawable(@DrawableRes int resId) {
        Drawable drawable = getApplication().getResources().getDrawable(resId);
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        return drawable;
    }

    /**
     * 计算文本宽度, 单位:px
     *
     * @param text     文本
     * @param textSize 字体大小,单位:px
     * @param isDpSize true-textSize表示dp, false-px
     */
    public static float getTextWidth(String text, int textSize, boolean isDpSize) {
        Application application = getApplication();
        if (isDpSize) {
            textSize = ScreenUtil.dp2px(application, textSize);
        }

        TextPaint paint = new TextPaint();
//        float scaledDensity = application.getResources().getDisplayMetrics().scaledDensity;
//        paint.setTextSize(scaledDensity * textSize);
        paint.setTextSize(textSize);
        return paint.measureText(text);
    }

    /**
     * 计算文本高度,单位:px
     *
     * @param text     文本
     * @param textSize 字体大小,单位:px
     * @param isDpSize true-textSize表示dp, false-px
     */
    public static float getTextHeight(String text, int textSize, boolean isDpSize) {
        Application application = getApplication();
        if (isDpSize) {
            textSize = ScreenUtil.dp2px(application, textSize);
        }

        TextPaint paint = new TextPaint();
//        float scaledDensity = application.getResources().getDisplayMetrics().scaledDensity;
//        paint.setTextSize(scaledDensity * textSize);
        paint.setTextSize(textSize);
        Rect rect = new Rect();
        paint.getTextBounds(text, 0, text.length(), rect);
        return rect.height();
    }

    /**
     * 计算文本区域,进而可获取其宽度和高度
     *
     * @param text     文本
     * @param textSize 字体大小,单位:px
     * @param isDpSize true-textSize表示dp, false-px
     */
    public static Rect getTextRect(String text, int textSize, boolean isDpSize) {
        Application application = getApplication();
        if (isDpSize) {
            textSize = ScreenUtil.dp2px(application, textSize);
        }

        TextPaint paint = new TextPaint();
//        float scaledDensity = application.getResources().getDisplayMetrics().scaledDensity;
//        paint.setTextSize(scaledDensity * textSize);
        paint.setTextSize(textSize);
        Rect rect = new Rect();
        paint.getTextBounds(text, 0, text.length(), rect);
        return rect;
    }

    /**
     * 将图片资源水平复制N张,拼接生成一张新图片
     *
     * @param resId         资源id
     * @param count         拼接次数, 最小为1
     * @param marginInnerPx 图片间的间隔, 最小0
     * @return
     */
    public static Bitmap composeImage(@DrawableRes int resId, int count, int marginInnerPx) {
        if (count <= 1) {
            count = 1;
        }

        if (marginInnerPx < 0) {
            marginInnerPx = 0;
        }

        Application application = getApplication();
        Bitmap bitmap = BitmapFactory.decodeResource(application.getResources(), resId);
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Bitmap rBitmap = Bitmap.createBitmap(w * count + (count - 1) * marginInnerPx, h, bitmap.getConfig());
        Canvas canvas = new Canvas(rBitmap);
        for (int i = 0; i < count; i++) {
            int margin = Math.max(0, (i - 1) * marginInnerPx);
            canvas.drawBitmap(bitmap, w * i + margin, 0, null);
        }
        return rBitmap;
    }
}