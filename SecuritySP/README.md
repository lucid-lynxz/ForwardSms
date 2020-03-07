# 自动对sharePreferences内容进行加解密

## 使用方法

### 获取 SecuritySP 对象
```kotlin
// 方法1: 默认使用内置的 aes 加解密算法
val securitySP = SecuritySP(context, "security_sp", Context.MODE_PRIVATE)

// 方法2: 使用自定义的密钥和加解密算法
// 自定义加解密算法实现
val customEncrptyUtil = object: ISpEncryptUtil{
    /**
     * @param secretKey 解密密钥,若为空,表示不解密
     * @param plainText 用户输入的原始明文内容
     * @return 解密字符串
     * */
    fun encrypt(secretKey: String?, plainText: String): String{
      // 完成加密,并返回密文,加密失败返回 ""
    }

    /**
     * @param secretKey 解密密钥,若为空,表示不解密
     * @param cipherText 从sp文件中获取的原始加密字符串
     * @return 解密后得到的文本
     * */
    fun decrypt(secretKey: String?, cipherText: String): String{
     // 完成解密,并返回明文, 解密失败,返回 ""
    }
}

// 用户自己保存密钥, 传入的值会透传到 ISpEncryptUtil 回调方法中
// 若使用aes加解密,也可以通过方法  SecuritySP.generateRandomAesKey(context) 随机生成一个密钥
String secretKey = null

// 传入自定义的加解密算法
val securitySP = SecuritySP(context, "security_sp", Context.MODE_PRIVATE, false, secretKey, customEncrptyUtil)
```

### 存取数据

```kotlin
val securitySP = SecuritySP(this, "security_sp", Context.MODE_PRIVATE) // 获取sp实例

// 存: 统一调用 putPreference() 方法即可, 工具会自动转换value数据为加密字符串, 然后存入sp中
securitySP.putPreference("StrKey", "str")  // 存储字符串数据
  .putPreference("BooleanKey", true) // 存储 boolean 数据
  .putPreference("LongKey", 0L) // 存储 long 数据
  .putPreference("IntKey", 0) // 存储 int 数据
  .putPreference("FloatKey", 0F) // 存储 float 数据


// 取: 统一调用 getPreference() 方法即可, 工具会自动根据默认值类型对sp字符串进行解密后进行类型转换
val strValue = securitySP.getPreference("StrKey", "defaultStr") // 获取字符串值
val booleanValue = securitySP.getPreference("BooleanKey", false) // 获取 boolean 值
val longValue = securitySP.getPreference("LongKey", 0L) // 获取 long 值
val intValue = securitySP.getPreference("IntKey", 0) // 获取 int 值
val floatValue = securitySP.getPreference("FloatKey", 1.0f) // 获取 float 值
```

### 移除自动生成的aes密码

使用内置aes解密算法时, 工具会将创建sp文件时生成的 aes 密钥存入到当前sp文件中
为确保安全, 用户可获取该密钥后自行存储, 并删除sp中的密钥值

```kotlin
// 使用内置的aes加解密算法, 若sp中不存在密钥数据, 则自动随机生成密钥并存入sp文件中
val securitySP = SecuritySP(context, "security_sp", Context.MODE_PRIVATE)

// 获取密钥, 并由用户自行存储
val securityKey = securitySP.getSecurityKey()

// 从sp中移除密钥数据
securitySP.removeSecurityKey()

// 移除密钥后, 下次再创建当前sp文件的 SecuritySP 对象时, 就需要明确传入 securityKey 或者  ISpEncryptUtil, 确保能加解密成功
```
