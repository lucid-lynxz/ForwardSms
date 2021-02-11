package org.lynxz.forwardsms.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.lynxz.utils.log.LoggerUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

/**
 * V1.0 2019.3.12 通过修改系统参数来适配android设备尺寸,并提供了 dp/px 互转方法 以及 获取状态栏/导航条高度及沉浸式等方法
 * 注意: 若项目中引用了第三方UI库,且UI库的设计尺寸与当前项目不同,则可能会导致适配问题(本适配工具未处理这种情况)
 * <p>
 * 另外,本工具类还提供了状态栏透明及切换状态栏文字图案颜色模式的方法
 * </p>
 *
 * <p>
 * <p>
 * 今日头条屏幕适配方案的使用方法: <br>
 * 1. 在 application 类的 onCreate() 中执行: {@link #init(Application)},app就会以默认的
 * 360dp*667dp 来适配布局;<br>
 * 若需要指定其他尺寸,则请执行:{@link #init(Application, float, float)}
 * ,依次传入宽高设计尺寸(单位:dp)即可;<br>
 * 另外,提供了"严格"模式,此模式下,只有实现了接口 {@link Adaptable} 的activity才会进行适配
 * <p>
 * 2.[可选,已默认指定宽度适配] 若某个activity不想使用默认的适配参数,则请实现接口 {@link Adaptable} 接口;<br>
 * 3.若想指定某个Activity不做屏幕适配,则请实现接口 {@link DonotAdapt} 接口;<br>
 * <p>
 * 以下为状态栏相关操作: <br>
 * 1. 设置状态栏背景色透明: {@link #setStatusBarTranslucent(Activity)};<br>
 * 2. 修改状态栏背景色: {@link #setStatusBarColor(Activity, int)};<br>
 * 3.
 * 切换状态栏内容颜色模式(深色/浅色调):{@link #setStatusBarTextColorMode(Activity, boolean)};<br>
 * 4. 获取状态栏高度:{@link #getStatusBarHeight(Context)};<br>
 * 5. 获取底部导航栏高度: {@link #getNavigationBarHeight(Context)}<br>
 * <p>
 * 其他方法说明:<br>
 * 1. dp转px: {@link #dp2px(Context, int)};<br>
 * 2. px转dp: {@link #px2dp(Context, float)}<br>
 * 3. 获取view在屏幕上的坐标: {@link #getViewScreenLocation(View)};<br>
 * 4. 获取屏幕宽高(px):{@link #getScreenWidth(Context)}
 * {@link #getScreenHeight(Context)};<br>
 * 5. 屏幕截图: {@link #snapShotWithStatusBar(Activity)}
 * {@link #snapShotWithoutStatusBar(Activity)}<br>
 * <p>
 * 参考文章:
 * <ol>
 * <li>[今日头条屏幕适配](https://mp.weixin.qq.com/s/d9QCoBP6kV9VSWvVldVVwA)</li>
 * <li>[今日头条适配方案优化](https://www.jianshu.com/p/4254ea9d1b27)</li>
 * <li>[屏幕适配扫盲](https://www.jianshu.com/p/ec5a1a30694b)</li>
 * <li>[今日头条适配方案升级(增加对第三方库的适配)](https://juejin.im/post/5b7a29736fb9a019d53e7ee2?utm_source=gold_browser_extension)</li>
 * <li>[一个Activity中多个Fragment实现沉浸式状态栏](https://blog.csdn.net/impure/article/details/53965082)</li>
 * <li>[Android透明状态栏与状态栏文字颜色更改](https://www.jianshu.com/p/7392237bc1de)</li>
 * </ol>
 * <p>
 * 其他知识:
 *
 * <pre>
 *      dpi = Math.sqrt(w*w+h*h)/屏幕尺寸 (屏幕宽高单位:px,尺寸单位:inch)
 *      density = dpi / 160;
 *      px = density * dp;
 *      px = dp * (dpi / 160);
 *
 * 查看屏幕尺寸和分辨率密度
 * adb shell wm size
 * adb shell wm density
 * </pre>
 */
public class ScreenUtil {
    public enum ScreenOrientation {
        WIDTH, HEIGHT
    }

    /**
     * 严格模式下,只有实现本接口的页面才进行屏幕适配 另外,可通过实现该接口来指定activity要特殊适配的尺寸和方向
     */
    public interface Adaptable {

        /**
         * 指定当前页面的屏幕适配方向,可以跟application中设置的不同 若返回null则表示使用默认适配方向
         */
        ScreenOrientation getAdaptOrientation();

        /**
         * 指定适配的设计图宽度尺寸(dp),若返回0或负数,则表示使用app默认适配宽度尺寸
         * 请参考:{@link #init(Application, ScreenOrientation, float, float, boolean)}
         */
        int getAdaptWidthDp();

        /**
         * 指定适配的设计图高度尺寸(dp),若返回0或负数,则表示使用app默认适配高度尺寸
         * 请参考:{@link #init(Application, ScreenOrientation, float, float, boolean)}
         */
        int getAdaptHeightDp();
    }

    /**
     * 空接口, 实现该接口的页面将不做适配,保持系统比例
     */
    public interface DonotAdapt {

    }

    private static final ScreenOrientation DEFAULT_ORIENTATION = ScreenOrientation.WIDTH;
    private static final float DEFAULT_DESIGN_WIDTH = 360f;
    private static final float DEFAULT_DESIGN_HEIGHT = 667f;

    // 美工设计图的方向及宽高尺寸,单位dp,作为各 activity 的默认适配参数
    private static float designWidth = DEFAULT_DESIGN_WIDTH;
    private static float designHeight = DEFAULT_DESIGN_HEIGHT;
    private static ScreenOrientation designOrientation = DEFAULT_ORIENTATION;

    private static float appDensity;
    private static float appScaledDensity;
    private static DisplayMetrics appDisplayMetrics;
    private static int barHeight;
    private static boolean isStrictMode = false;

    /**
     * 设置默认分辨率适配(667dp * 360dp) 宽度适配, 非严格模式(未实现接口 {@link DonotAdapt} 的activity都进行适配)
     * 请参考: {@link #init(Application, ScreenOrientation, float, float, boolean)}
     */
    public static void init(@NonNull Application application) {
        init(application, DEFAULT_ORIENTATION, DEFAULT_DESIGN_WIDTH, DEFAULT_DESIGN_HEIGHT, false);
    }

    /**
     * 指定要适配的分辨率 宽度适配, 非严格模式(未实现接口 {@link DonotAdapt} 的activity都进行适配) 请参考:
     * {@link #init(Application, ScreenOrientation, float, float, boolean)}
     */
    public static void init(@NonNull Application application, float designWidthDp, float designHeightDp) {
        init(application, DEFAULT_ORIENTATION, designWidthDp, designHeightDp, false);
    }

    /**
     * 设置分辨率适配
     *
     * @param designWidthDp  美工设计图默认屏幕宽度,单位:dp
     * @param designHeightDp 美工设计图默认屏幕高度,单位:dp
     * @param strictMode     是否使用严格模式 true-严格模式: 只有实现了接口 {@link Adaptable} 的
     *                       activity 才进行屏幕适配 false-宽松模式(默认): 只要不是实现接口
     *                       {@link DonotAdapt} 的 activity 都进行适配
     */
    public static void init(@NonNull final Application application, ScreenOrientation orientation, float designWidthDp,
            float designHeightDp, boolean strictMode) {
        isStrictMode = strictMode;
        designWidth = designWidthDp;
        designHeight = designHeightDp;
        designOrientation = orientation;

        // 获取application的DisplayMetrics
        appDisplayMetrics = application.getResources().getDisplayMetrics();
        // 获取状态栏高度
        barHeight = getStatusBarHeight(application);
        if (appDensity == 0) {
            // 初始化的时候赋值
            appDensity = appDisplayMetrics.density;
            appScaledDensity = appDisplayMetrics.scaledDensity;

            // 自动在activity的 onCreate() 中设置宽度适配,若有需要改为高度适配请在 activity 中修改
            application.registerActivityLifecycleCallbacks(new EmptyActivityLifecycleCallback() {
                // @Override
                // public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                //
                // }

                @Override
                public void onActivityResumed(Activity activity) {
                    super.onActivityResumed(activity);
                    if (activity instanceof DonotAdapt) {
                        return;
                    }

                    if (!isStrictMode || activity instanceof Adaptable) {
                        setActivityAdaptParam(activity);
                    }
                }
            });

            // 添加字体变化的监听
            application.registerComponentCallbacks(new ComponentCallbacks() {
                @Override
                public void onConfigurationChanged(@NonNull Configuration newConfig) {
                    // 字体改变后,将appScaledDensity重新赋值
                    // if (newConfig != null && newConfig.fontScale > 0) {
                    // appScaledDensity =
                    // application.getResources().getDisplayMetrics().scaledDensity;
                    // }
                }

                @Override
                public void onLowMemory() {
                }
            });
        }
    }

    private static float getActivityDensity(Activity activity) {
        float width = designWidth;
        float height = designHeight;
        ScreenOrientation orientation = designOrientation;

        if (activity instanceof Adaptable) {
            orientation = ((Adaptable) activity).getAdaptOrientation();
            if (orientation == null) {
                orientation = designOrientation;
            }
            int adaptWidth = ((Adaptable) activity).getAdaptWidthDp();
            int adaptHeight = ((Adaptable) activity).getAdaptHeightDp();
            if (adaptWidth > 0) {
                width = adaptWidth;
            }

            if (adaptHeight > 0) {
                height = adaptHeight;
            }
        }

        float targetDensity;
        if (ScreenOrientation.WIDTH == orientation) {
            targetDensity = appDisplayMetrics.widthPixels / width;
        } else {
            // 由于设计图是包含标题栏和导航栏,因此高度应计算全屏幕高度
            // TODO: lynxz 2018/12/5 导航条不显示时才包含其高度
            // targetDensity = (appDisplayMetrics.heightPixels - barHeight) / height;
            targetDensity = appDisplayMetrics.heightPixels / height;
            // targetDensity = getScreenHeightWithNavigationBar(activity) / height;
        }

        return targetDensity;
    }

    /**
     * 设置activity的适配参数 默认都统一使用 init() 初始化时指定的设计图参数, 若想单独指定某个Activity的适配参数,则让该
     * Activity 实现接口 {@link Adaptable} 即可; targetDensity targetScaledDensity
     * targetDensityDpi 这三个参数是统一修改过后的值
     */
    private static void setActivityAdaptParam(@Nullable Activity activity) {
        if (activity == null) {
            return;
        }
        float targetDensity = getActivityDensity(activity);
        float targetScaledDensity = targetDensity * (appScaledDensity / appDensity);
        int targetDensityDpi = (int) (160 * targetDensity);
        /*
         * 最后在这里将修改过后的值赋给系统参数 只修改Activity的density值
         */
        DisplayMetrics activityDisplayMetrics = activity.getResources().getDisplayMetrics();

        activityDisplayMetrics.density = targetDensity;
        activityDisplayMetrics.densityDpi = targetDensityDpi;
        activityDisplayMetrics.scaledDensity = targetScaledDensity;
        // 如果不希望字体大小随系统设置的字体大小变化而变化,可以将其指定为确定值
        // activityDisplayMetrics.scaledDensity = targetDensity;
    }

    /**
     * dp转px
     */
    public static int dp2px(Context context, int dip) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dip * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px 的单位 转成为 dp
     */
    public static int px2dp(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * 获取屏幕宽度(px)
     */
    public static int getScreenWidth(Context context) {
        WindowManager windowmanager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowmanager == null) {
            return 0;
        }
        Display display = windowmanager.getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }

    /**
     * 得到屏幕高度(px,不包含底部导航条高度)
     */
    public static int getScreenHeight(Context context) {
        WindowManager windowmanager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowmanager == null) {
            return 0;
        }
        DisplayMetrics dm = new DisplayMetrics();
        windowmanager.getDefaultDisplay().getMetrics(dm); // 不包含导航栏
        // windowmanager.getDefaultDisplay().getRealMetrics(dm); // 包含底部导航条高度
        return dm.heightPixels;
    }

    /**
     * 得到屏幕高度(px)
     */
    public static int getScreenHeightWithNavigationBar(Context context) {
        WindowManager windowmanager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowmanager == null) {
            return 0;
        }
        DisplayMetrics dm = new DisplayMetrics();
        // windowmanager.getDefaultDisplay().getMetrics(dm); // 不包含导航栏
        windowmanager.getDefaultDisplay().getRealMetrics(dm); // 包含底部导航条高度
        return dm.heightPixels;
    }

    /**
     * 获取 View 的坐标
     */
    public static RectF getViewScreenLocation(View view) {
        int[] location = new int[2];
        // 获取控件在屏幕中的位置，返回的数组分别为控件左顶点的 x、y 的值
        view.getLocationOnScreen(location);
        return new RectF(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight());
    }

    /**
     * 获取当前屏幕截图，包含状态栏
     */
    public static Bitmap snapShotWithStatusBar(Activity activity) {
        View view = activity.getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap bmp = view.getDrawingCache();
        int width = getScreenWidth(activity);
        int height = getScreenHeight(activity);
        Bitmap bp = Bitmap.createBitmap(bmp, 0, 0, width, height);
        view.destroyDrawingCache();
        return bp;
    }

    /**
     * 获取当前屏幕截图，不包含状态栏
     */
    public static Bitmap snapShotWithoutStatusBar(Activity activity) {
        View view = activity.getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap bmp = view.getDrawingCache();
        Rect frame = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
        int statusBarHeight = frame.top;

        int width = getScreenWidth(activity);
        int height = getScreenHeight(activity);
        Bitmap bp = Bitmap.createBitmap(bmp, 0, statusBarHeight, width, height - statusBarHeight);
        view.destroyDrawingCache();
        return bp;
    }

    // ------- 状态栏修改相关操作 -------

    /**
     * 获取状态栏高度
     */
    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * 获取手机底部导航条高度
     */
    public static int getNavigationBarHeight(Context context) {
        int var1 = 0;
        int var2 = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (var2 > 0) {
            var1 = context.getResources().getDimensionPixelSize(var2);
        }
        return var1 + dp2px(context, 5);
    }

    /**
     * 给activity的状态栏设置颜色
     *
     * @param activity activity
     * @param color    颜色值
     */
    public static void setStatusBarColor(Activity activity, @ColorInt int color) {
        Window window = activity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(color);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            View view = new View(activity);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    getStatusBarHeight(activity));
            view.setLayoutParams(params);
            view.setBackgroundColor(color);

            ViewGroup decorView = (ViewGroup) window.getDecorView();
            decorView.addView(view);

            ViewGroup contentView = activity.findViewById(android.R.id.content);
            contentView.setPadding(0, getStatusBarHeight(activity), 0, 0);
        }
    }

    /**
     * 设置activity全屏，状态栏透明，内容填充到状态栏中
     */
    public static void setStatusBarTranslucent(Activity activity) {
        Window window = activity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            View decorView = window.getDecorView();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    private static void setNavTrans(Activity activity, boolean isNavTrans) {
        if (!isNavTrans && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            // 要重新add
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    /**
     * 修改状态栏文字颜色，这里小米，魅族区别对待
     */
    public static void setStatusBarTextColorMode(Activity activity, boolean darkMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            RomType romType = getStatusBarAvailableRomType();
            switch (romType) {
            case MIUI:
                setMIUIStatusBarColorMode(activity, darkMode);
                break;
            case FLYME:
                setFlymeStatusBarColorMode(activity, darkMode);
                break;
            case ANDROID_NATIVE:
                setAndroidNativeStatusBarColorMode(activity, darkMode);
                break;
            }
        }
    }

    /**
     * 几种可能的系统
     */
    enum RomType {
        MIUI(1), FLYME(2), ANDROID_NATIVE(3), NA(4);
        private int romType = 0;

        RomType(int type) {
            this.romType = type;
        }

        public int getRomType() {
            return romType;
        }
    }

    private static RomType getStatusBarAvailableRomType() {
        // 判断是否是 miui V6.0以上
        if (getMiUIVersionCode() >= MIUI_V60_VERSION_CODE) {
            return RomType.MIUI;
        }

        if (isFlymeV4OrAbove()) {
            return RomType.FLYME;
        }

        if (isAndroidMOrAbove()) {
            return RomType.ANDROID_NATIVE;
        }

        return RomType.NA;
    }

    /**
     * Android Api 23以上 系统版本判定: 原生系统在M以上才支持状态栏文字颜色切换
     */
    private static boolean isAndroidMOrAbove() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    /**
     * 小米系统判定
     * <p>
     * MIUI V6 为: 4 MIUI V7 为: 5
     */
    private static int sMiUIVersionCode = -1;
    private static final int MIUI_V60_VERSION_CODE = 4;
    private static final int MIUI_V70_VERSION_CODE = 5;

    /**
     * 获取miui系统版本号,V6.x以上支持状态条颜色模式切换
     */
    private static int getMiUIVersionCode() {
        if (sMiUIVersionCode >= 0) {
            return sMiUIVersionCode;
        }
        FileInputStream fis = null;
        sMiUIVersionCode = -1;
        try {
            final Properties properties = new Properties();
            fis = new FileInputStream(new File(Environment.getRootDirectory(), "build.prop"));
            properties.load(fis);
            String uiCode = properties.getProperty("ro.miui.ui.version.code", null);
            if (uiCode != null) {
                sMiUIVersionCode = Integer.parseInt(uiCode);
            }
        } catch (Exception e) {
            LoggerUtil.d("" + e.getMessage());
            // e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sMiUIVersionCode;
    }

    /**
     * 魅族系统判断 Flyme V4的displayId格式为 [Flyme OS 4.x.x.xA] Flyme V5的displayId格式为 [Flyme
     * 5.x.x.x beta]
     */
    private static boolean isFlymeV4OrAbove() {
        String displayId = Build.DISPLAY;
        if (!TextUtils.isEmpty(displayId) && displayId.contains("Flyme")) {
            String[] displayIdArray = displayId.split(" ");
            for (String temp : displayIdArray) {
                // 版本号4以上，形如4.x.
                if (temp.matches("^[4-9]\\.(\\d+\\.)+\\S*")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 原生系统修改状态栏字体颜色模式
     */
    public static void setAndroidNativeStatusBarColorMode(Activity activity, boolean darkMode) {
        View decor = activity.getWindow().getDecorView();
        if (darkMode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                decor.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        } else {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    /**
     * 小米系统下状态栏文字颜色的修改(在深色和浅色之间切换)
     */
    @SuppressLint("PrivateApi")
    private static boolean setMIUIStatusBarColorMode(Activity activity, boolean darkMode) {
        boolean result = false;
        Window window = activity.getWindow();
        if (window != null) {
            Class clazz = window.getClass();
            try {
                Class layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
                Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
                int darkModeFlag = field.getInt(layoutParams);
                Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
                if (darkMode) {
                    extraFlagField.invoke(window, darkModeFlag, darkModeFlag);// 状态栏透明且黑色字体
                } else {
                    extraFlagField.invoke(window, 0, darkModeFlag);// 清除黑色字体
                }
                result = true;

                // 开发版 7.7.13 及以后版本采用了系统API，旧方法无效但不会报错，所以两个方式都要加上
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && getMiUIVersionCode() >= MIUI_V70_VERSION_CODE) {
                    if (darkMode) {
                        activity.getWindow().getDecorView().setSystemUiVisibility(
                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                    } else {
                        activity.getWindow().getDecorView().setSystemUiVisibility(
                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 魅族系统状态栏文字颜色修改
     */
    private static boolean setFlymeStatusBarColorMode(Activity activity, boolean darkMode) {
        boolean result = false;
        if (activity != null) {
            try {
                WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
                Field darkFlag = WindowManager.LayoutParams.class.getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON");
                Field meizuFlags = WindowManager.LayoutParams.class.getDeclaredField("meizuFlags");
                darkFlag.setAccessible(true);
                meizuFlags.setAccessible(true);
                int bit = darkFlag.getInt(null);
                int value = meizuFlags.getInt(lp);
                if (darkMode) {
                    value |= bit;
                } else {
                    value &= ~bit;
                }
                meizuFlags.setInt(lp, value);
                activity.getWindow().setAttributes(lp);
                result = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static class EmptyActivityLifecycleCallback implements Application.ActivityLifecycleCallbacks {

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

        }

        @Override
        public void onActivityStarted(Activity activity) {

        }

        @Override
        public void onActivityResumed(Activity activity) {

        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public void onActivityStopped(Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {

        }
    }
}