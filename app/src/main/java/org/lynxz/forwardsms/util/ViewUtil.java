package org.lynxz.forwardsms.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

/**
 * @author lynxz
 * @version 1.0
 */
public class ViewUtil {

    /**
     * 隐藏键盘
     */
    public static void hideKeyboard(View focusView) {
        Context context = focusView == null ? null : focusView.getContext();
        if (context == null) {
            return;
        }

        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(focusView.getWindowToken(), 0);
        }
    }

    /**
     * 判断一个view是否是所给控件类型数组中的一种
     *
     * @return true-view属于 types 中的一种
     */
    private static boolean isViewInstanceOfAny(View view, Class... types) {
        int excludeSize = types == null ? 0 : types.length;
        if (excludeSize > 0) {
            for (Class excludeType : types) {
                if (excludeType.isInstance(view)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取view的文本,若该view并非textview,则返回默认值 defaultText
     */
    @NotNull
    public static String getViewText(View view, @NotNull String defaultText) {
        String result = defaultText;
        if (view instanceof TextView) {
            result = ((TextView) view).getText().toString();
        }

        return result;
    }

    /**
     * 设置指定的textview  跑马灯效果
     */
    public static void enableTextViewMarquee(TextView view) {
        view.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        view.setSingleLine(true);
        view.setSelected(true);
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
    }

    /**
     * 给 textview 添加删除线
     */
    public static void setStrikeThrough(TextView textView) {
        textView.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG);
    }

    /**
     * 给 textview 添加下划线
     */
    public static void setUnderLine(TextView textView) {
        textView.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
    }

    /**
     * 切换edittext是否可编辑
     */
    public static void changeEditable(EditText edt, boolean editable) {
        edt.setFocusable(editable);
        edt.setFocusableInTouchMode(editable);
        edt.setClickable(editable);
        if (!editable) {
            edt.setKeyListener(null); // 不可粘贴，长按不会弹出粘贴框
        }
    }

    /**
     * 设置view宽高为wrap
     *
     * @param vertical true-高度wrap  false-宽度wrap
     */
    public static void updateViewSizeWrap(View view, boolean vertical) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (vertical) {
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        } else {
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        view.setLayoutParams(lp);
    }

    /**
     * 设置view的水波纹前景色
     * 等效于xml中设置:
     * <p>
     * android:foreground="?android:attr/selectableItemBackground"
     */
    public static void setRippleForeground(View view) {
        int[] attribute = new int[]{android.R.attr.selectableItemBackground};
        TypedValue typedValue = new TypedValue();
        TypedArray typedArray = view.getContext().getTheme().obtainStyledAttributes(typedValue.resourceId, attribute);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view.setForeground(typedArray.getDrawable(0));
        } else {
            view.setBackground(typedArray.getDrawable(0));
        }
    }
}
