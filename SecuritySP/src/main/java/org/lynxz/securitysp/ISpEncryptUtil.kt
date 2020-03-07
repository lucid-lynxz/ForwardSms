package org.lynxz.securitysp

/**
 * 用于sp文件key-value加解密
 * 由用户自定义传入
 * */
interface ISpEncryptUtil {
    /**
     * @param secretKey 解密密钥,若为空,表示不解密
     * @param plainText 用户输入的原始明文内容
     * @return 解密字符串
     * */
    fun encrypt(secretKey: String?, plainText: String): String

    /**
     * @param secretKey 解密密钥,若为空,表示不解密
     * @param cipherText 从sp文件中获取的原始加密字符串
     * @return 解密后得到的文本
     * */
    fun decrypt(secretKey: String?, cipherText: String): String
}