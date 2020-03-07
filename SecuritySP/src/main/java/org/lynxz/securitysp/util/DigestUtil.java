package org.lynxz.securitysp.util;

import android.text.TextUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * 摘要算法
 */
public class DigestUtil {
    public static final String SHA1 = "SHA-1";
    public static final String SHA256 = "SHA-256";


    /**
     * 对元数据进行sha256摘要处理
     */
    @Nullable
    public static byte[] sha256(@NotNull byte[] srcData) {
        return encryptToByte(srcData, DigestUtil.SHA256);
    }

    @Nullable
    public static byte[] sha1(@NotNull byte[] srcData) {
        return encryptToByte(srcData, DigestUtil.SHA1);
    }

    /**
     * 对字符串加密,加密算法使用MD5,SHA-1,SHA-256,默认使用SHA-256
     *
     * @param strSrc  要加密的字符串
     * @param encName 加密类型, 参考 {@link #SHA256} , {@link #SHA1}
     */
    @Nullable
    public static String encryptToString(String strSrc, String encName) {
        byte[] result = encryptToByte(strSrc.getBytes(), encName);
        if (result == null || result.length == 0) {
            return null;
        }
        return ByteUtil.bytesToHexString(result);
    }


    /**
     * @param srcText 明文,字节数组
     * @param encName 摘要算法,若传空,则使用默认 "SHA-256" 算法
     * @return
     */
    @Nullable
    public static byte[] encryptToByte(byte[] srcText, String encName) {
        if (srcText == null || srcText.length == 0) {
            return null;
        }

        try {
            if (TextUtils.isEmpty(encName)) {
                encName = SHA256;
            }

            MessageDigest md = MessageDigest.getInstance(encName);
            md.update(srcText);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
