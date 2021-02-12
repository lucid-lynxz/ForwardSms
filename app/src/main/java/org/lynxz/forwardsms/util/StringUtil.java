package org.lynxz.forwardsms.util;


import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.UnderlineSpan;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lynxz.utils.log.LoggerUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * version: 1.1
 */
public class StringUtil {
    private static final String TAG = "StringUtil";

    /**
     * 判断字符串是否为空
     */
    public static boolean isEmpty(String src) {
        return src == null || src.length() == 0;
    }

    /**
     * 格式化json字符串再输出
     */
    @NotNull
    public static String toPrettyJson(Object obj) {
        return GsonUtil.INSTANCE.toJson(obj, true);
//        return toJsonInternal(mFormatGson, obj);
    }

    /**
     * 直接序列化(不做格式缩进)
     */
    @NotNull
    public static String toJson(Object obj) {
        return GsonUtil.INSTANCE.toJson(obj, false);
    }

    @Nullable
    public static <T> T parseJson(String json) {
        return GsonUtil.INSTANCE.parseJson(json);
    }

    /**
     * 解析json字符串为指定的普通对象(非list)
     */
    @Nullable
    public static <T> T parseJson(String json, Class<? extends T> cls) {
        return GsonUtil.INSTANCE.parseJson(json, cls);
    }

    /**
     * 对于转换成 List<Integer> 等整数的, Gson会将其解析成科学计数法,会被视为Double,导致报错
     * 建议解析成Double, 再自行进行转换
     * 参考: https://juejin.im/post/5cbb3c5af265da03ab23258c
     */
    public static <T> T parseListJson(String json) {
        return GsonUtil.INSTANCE.parseJson(json);
    }


    @NotNull
    private static String toJsonInternal(Gson gson, @Nullable Object obj) {
        try {
            return gson.toJson(obj);
        } catch (Exception e) {
            LoggerUtil.e(TAG, "toJson() fail:" + e.getMessage());
            return obj == null ? "" : obj.toString();
        }
    }

    /**
     * 对用户名做马赛克处理, 目前保留收尾明文, 中间部分用特定符号替代
     *
     * @param userName        明文
     * @param placeholderFlag 要使用的占位符, 默认为 *
     * @return
     */
    public static String userNameMosaic(String userName, String placeholderFlag) {
        if (TextUtils.isEmpty(placeholderFlag)) {
            placeholderFlag = "*";
        }
        int len = userName == null ? 0 : userName.length();
        if (len <= 1) {
            return "*";
        }

        String result = "";
        if (len >= 3) {
            StringBuilder sb = new StringBuilder();
            sb.append(userName.charAt(0));
            for (int i = 1; i < len - 1; i++) {
                sb.append(placeholderFlag);
            }
            sb.append(userName.charAt(len - 1));
            result = sb.toString();
        } else {
            result = userName.charAt(0) + placeholderFlag;
        }
        return result;
    }

    /**
     * 将文字已指定分隔符切割,生成list返回
     * P.S. 切割后,会进行 trim() 操作
     *
     * @param src             原始字符串,若为空,则返回长度为0的list
     * @param splitFlag       字符串分隔符,若为空, 则使用逗号分隔
     * @param ignoreBlankItem 是否忽略空字符串, true-切割后字符串为空,则忽略
     * @return
     */
    @NotNull
    public static ArrayList<String> convertStrToList(String src, String splitFlag, boolean ignoreBlankItem) {
        ArrayList<String> list = new ArrayList<>();
        if (TextUtils.isEmpty(src)) {
            return list;
        }

        if (TextUtils.isEmpty(splitFlag)) {
            splitFlag = ",";
        }

        String[] arr = src.split(splitFlag);
        for (String item : arr) {
            item = item.trim();
            if (ignoreBlankItem && TextUtils.isEmpty(item)) {
                continue;
            }
            list.add(item);
        }
        return list;
    }

    /**
     * 将文字已指定分隔符切割,生成list返回
     * P.S. 切割后,会进行 trim() 操作
     *
     * @param list            原始字符串列表,若为空,则""
     * @param splitFlag       字符串分隔符,若为空, 则使用逗号拼接
     * @param ignoreBlankItem 是否忽略空字符串, true-忽略list中的空字符串
     * @return
     */
    @NotNull
    public static String convertListToStr(List<String> list, String splitFlag, boolean ignoreBlankItem) {
        int size = list == null ? 0 : list.size();
        if (size == 0) {
            return "";
        }

        if (TextUtils.isEmpty(splitFlag)) {
            splitFlag = ",";
        }

        StringBuilder sb = new StringBuilder();
        for (String item : list) {
            item = item == null ? null : item.trim();
            if (ignoreBlankItem && TextUtils.isEmpty(item)) {
                continue;
            }
            sb.append(item).append(",");
        }
        String s = sb.toString();
        if (s.endsWith(",")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    /**
     * 给字符串添加下划线
     */
    public static SpannableString addUnderLine(String str) {
        SpannableString ss = new SpannableString(str);
        ss.setSpan(new UnderlineSpan(), 0, str.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return ss;
    }
}
