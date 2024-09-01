package org.lynxz.version.transform

import org.lynxz.version.util.FileUtilSimple
import java.util.concurrent.ConcurrentHashMap
import kotlin.streams.toList

/**
 * 用例信息汇总
 */
object CaseManager {
    var androidTestRootPath: String = ""
    const val DEFAULT_SRC_PATH = "java/"

    // 若源码目录与常规目录不一致,可以在此处进行映射
    val clzDescMap = mapOf<String, String>(
//        "org/lynxz/demo/tts/" to "include/src_tts/"
    )

    // key表示class的description值, 如: corg/lynxz/demo/TestActivityTest
    private val clzMap: MutableMap<String, CaseClzBean> = ConcurrentHashMap()

    /**
     * 添加一个测试类信息, 若已存在,则不作处理
     * @param clzDesc String 测试类完整路径,如: corg/lynxz/demo/TestActivityTest
     * @param superClzDesc String 父类完整路径, 如: corg/lynxz/demo/BaseTestSuit
     * @return 返回添加成功的或已存在的对象
     */
    fun addClz(clzDesc: String, superClzDesc: String? = ""): CaseClzBean =
        clzMap.getOrDefault(clzDesc, CaseClzBean(clzDesc, superClzDesc = superClzDesc).apply {
            updateSrcFilePath()
        }).also { clzBean ->
            clzMap[clzDesc] = clzBean
        }

    /**
     * 精简类路径信息, 只保留类名
     * @param clzDesc String 原类完整路径, 如:  corg/lynxz/demo/TestActivityTest
     *                                   如:  Lorg/junit/Test;
     * @return String 类名, 如: TestActivityTest
     *                     如: Test
     */
    fun simplifyClzDesc(clzDesc: String): String {
        val desc = clzDesc.replace("\\", "/").replace(";", "")
        return desc.substring(desc.lastIndexOf("/") + 1).also {
            // println("simplifyClzDesc result=$it,src=$clzDesc")
        }
    }

    /**
     * 获取测试用例类信息
     * @param clzDesc String 测试类签名 如: corg/lynxz/demo/TestActivityTest
     * @return CaseClassBean?
     */
    fun getClz(clzDesc: String): CaseClzBean = addClz(clzDesc)

    /**
     * 添加测试用例方法信息
     * @param clzDesc String 用例所在类的完整路径, 如: corg/lynxz/demo/TestActivityTest
     * @param methodDesc String 方法签名, 如: changeListExistTest
     * @return CaseMethodBean
     */
    fun addMethod(clzDesc: String, methodDesc: String): CaseMethodBean =
        addClz(clzDesc).addMethod(methodDesc)

    fun getMethod(clzDesc: String, methodDesc: String): CaseMethodBean =
        addMethod(clzDesc, methodDesc)

    /**
     * 添加测试类的注解
     */
    fun addClzAnnotation(clzDesc: String, annotationDesc: String) =
        getClz(clzDesc).addAnnotation(annotationDesc)

    /**
     * 更新测试类的注解信息
     */
    fun updateClzAnnotation(clzDesc: String, annotationDesc: String, key: String, value: Any?) {
        getClz(clzDesc).updateAnnotation(annotationDesc, key, value)
    }

    /**
     * 添加测试方法的注解信息
     */
    fun addMethodAnnotation(
        clzDesc: String, // 用例所在类路径
        methodDesc: String, // 用例方法名
        annotationDesc: String // 注解类所在路径
    ) = getMethod(clzDesc, methodDesc).addAnnotation(annotationDesc)

    /**
     * 更新测试方法的注解信息
     */
    fun updateMethodAnnotation(
        clzDesc: String,
        methodDesc: String,
        annotationDesc: String,
        key: String,
        value: Any?
    ) = getMethod(clzDesc, methodDesc).updateAnnotation(annotationDesc, key, value)

    /**
     * 在遍历method annotation结束后触发本方法
     * 用于移除非测试用例方法, 如不包含注解 @Test 或者 包含了注解 @Ignore 的方法
     * P.S. 类信息仍保留, 用于整个transform结束后, 更新子类注解信息
     * @param clzDesc 待清理的类路径信息,若为空,则对所有已扫描的类都生效
     */
    fun removeInvalidTestMethod(clzDesc: String?) {
        // println("removeInvalidTestMethod clzDesc=$clzDesc")
        if (clzDesc.isNullOrEmpty()) {
            clzMap.forEach { (_, clzBean) -> clzBean.removeInvalidTestMethod() }
        } else {
            getClz(clzDesc).removeInvalidTestMethod()
        }
    }

    /**
     * 删除无效的测试用例类(测试方法数为0的类)
     */
    @Synchronized
    private fun removeEmptyTestClz() {
        val iterator = clzMap.keys.iterator()
        while (iterator.hasNext()) {
            val clzDesc = iterator.next()
            if (getClz(clzDesc).methodMap.isEmpty()) {
                iterator.remove()
                // println("removeEmptyTestClz clzDesc=$clzDesc")
            }
        }
    }

    private const val autoClzDescKeyWord = "corg/lynxz/demo"

    /**
     * 整个transform执行完成后, 可以递归更新类的注解信息(可以指定只更新 xx.xx.xx 包下的注解)
     * 操作:
     * 类:  从父类获取注解信息, 若当前类不包含该父类的注解或者某个注解值,则新增,否则不处理
     * 方法: 从当前类中获取注解, 若当前方法中不包含该注解,则新增, 否则不处理
     */
    @Synchronized
    fun recurseUpdateProjectAnnotation() {
        clzMap.keys.forEach { recurseUpdateProjectClzAnnotation(it) }
        removeEmptyTestClz()
    }

    /**
     * 整个transform执行完成后, 可以递归更新用例类和方法的注解信息(可以只更新 xx.xx.xx 包下的注解)
     * 操作:
     * 1. 对于类: 从父类获取的所有注解信息, 若当前类不包含该父类的注解或者其中某个注解值,则新增,否则不处理
     * 2. 对于方法: 从当前类中获取注解(要求包含特定关键字), 若当前方法中不包含该注解或其中某个注解值,则新增,否则不处理
     */
    @Synchronized
    private fun recurseUpdateProjectClzAnnotation(
        clzDesc: String?,
        // 同步给用例的注解路径需要包含的关键字,空则表示不做过滤,仅在使用类名全路径时才有效
        annotationClzKeyWord4method: String = autoClzDescKeyWord,
        // 记录用例注解信息时, 注解类名是否使用全路径信息, 如: corg/lynxz/demo/TestActivityTest
        // 若为false,则过滤后会简化注解类名,如: TestActivityTest
        useFullAnnotationClzDesc: Boolean = false
    ): CaseClzBean? {
        if (clzDesc.isNullOrEmpty() || !clzDesc.contains(autoClzDescKeyWord)) {
            return null
        }

        val clzBean = getClz(clzDesc)
        val superClzDesc = clzBean.superClzDesc
        val superClzBean = recurseUpdateProjectClzAnnotation(superClzDesc)

        // 将父类注解信息同步到当前类中
        superClzBean?.annotationMap?.forEach { (superClzAnDesc, superClzAnPropertyMap) ->
            if (superClzAnPropertyMap.isEmpty()) { // 按需添加注解对象
                clzBean.addAnnotation(superClzAnDesc)
            } else { // 按需添加注解并更新注解值
                superClzAnPropertyMap.entries.forEach { (k, v) ->
                    clzBean.updateAnnotation(superClzAnDesc, k, v, true)
                }
            }
        }

        // 按需将类的注解更新到用例注解中
        clzBean.annotationMap.forEach { (clzAnDesc, clzAnPropertyMap) ->
            if (clzAnDesc.contains(annotationClzKeyWord4method)) {
                if (clzAnPropertyMap.isEmpty()) {
                    clzBean.methodMap.forEach { (methodDesc, methodBean) ->
                        methodBean.addAnnotation(clzAnDesc)
                    }
                } else {
                    clzAnPropertyMap.entries.forEach { (clzAnKey, clzAnValue) ->
                        clzBean.methodMap.forEach { (methodDesc, methodBean) ->
                            methodBean.updateAnnotation(clzAnDesc, clzAnKey, clzAnValue, true)
                        }
                    }
                }
            }
        }

        // 简化用例注解类名信息
        if (!useFullAnnotationClzDesc) {
            clzBean.methodMap.forEach { (methodDesc, methodBean) ->
                val tAnnotationMap = mutableMapOf<String, MutableMap<String, Any?>>()
                val iterator = methodBean.annotationMap.iterator()
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    val sKey = simplifyClzDesc(next.key)
                    tAnnotationMap[sKey] = next.value
                    iterator.remove()
                }
                methodBean.annotationMap.putAll(tAnnotationMap)
            }
        }

        return clzBean
    }

    /**
     * 获取所有的用例对象
     */
    fun getAllMethodList(): List<CaseMethodBean> {
        val result = mutableListOf<CaseMethodBean>()
        clzMap.forEach { (_, clzBean) ->
            clzBean.methodMap.forEach { (_, methodBean) ->
                result.add(methodBean)
            }
        }
        return result
    }

    fun getAllClzList(): List<CaseClzBean> = clzMap.values.stream().toList()
}

open class UpdateAnnotationAction {
    /**
     * 更新指定的注解值
     * @param updateWhenAbsent Boolean 若为true,则仅当前对象不包含该注解key时才更新
     */
    fun update(
        propertyMap: MutableMap<String, Any?>, // 注解属性map对象
        key: String, // 待更新的注解属性名
        value: Any?, // 新的属性值
        updateWhenAbsent: Boolean = false // 是否仅在属性不存在时才更新
    ) {
        if (!updateWhenAbsent || !propertyMap.containsKey(key)) {
            propertyMap[key] = value
        }
    }
}

/**
 * 测试用例类
 */
data class CaseClzBean(
    val clzDesc: String, // 类完整路径名, 如: corg/lynxz/demo/TestActivityTest
    val superClzDesc: String? = "", // 父类完整路径名, 如: corg/lynxz/demo/BaseTestSuit
    var srcFilePath: String = "", // 源码文件路径, 如: corg/lynxz/demo/TestActivityTest.java
    val methodMap: MutableMap<String, CaseMethodBean> = ConcurrentHashMap(), // 测试用例方法列表,key表示方法的descriptor
    val annotationMap: MutableMap<String, MutableMap<String, Any?>> = ConcurrentHashMap() // 类注解信息,key表示注解类的descriptor,value表示属性信息
) : UpdateAnnotationAction() {

    /**
     * 更新源码文件路径信息, 若 path 为空,则根据 clzDesc 来自动匹配
     */
    fun updateSrcFilePath(path: String? = null) {
        if (srcFilePath.isNotBlank()) {
            return
        }

        var tClzDesc = ""
        srcFilePath = if (FileUtilSimple.checkFileExists(path)) {
            FileUtilSimple.recookPath(path!!)
        } else {
            val arr = clzDesc.split("/")
            tClzDesc = if (arr.size >= 4) {
                arr.subList(0, 4).joinToString("/")
            } else {
                clzDesc
            }
            tClzDesc = FileUtilSimple.recookPath("$tClzDesc/")
            val dirRelPath =
                CaseManager.clzDescMap.getOrDefault(tClzDesc, CaseManager.DEFAULT_SRC_PATH)
            val dirPath = "${CaseManager.androidTestRootPath}/$dirRelPath"
            val srcPathJava = FileUtilSimple.recookPath("$dirPath/$clzDesc.java")
            val srcPathKt = FileUtilSimple.recookPath("$dirPath/$clzDesc.kt")
            if (FileUtilSimple.checkFileExists(srcPathJava)) {
                srcPathJava
            } else if (FileUtilSimple.checkFileExists(srcPathKt)) {
                srcPathKt
            } else {
                ""
            }
        }
        // println("clz updateSrcFilePath clzDesc=$clzDesc,tClzDesc=$tClzDesc,srcFilePath=$srcFilePath")
    }

    /**
     * 添加一个新注解(若对应注解已存在,则不作操作)
     * @param annotationClzDesc String
     * @return MutableMap<String, Any?>
     */
    fun addAnnotation(annotationClzDesc: String): MutableMap<String, Any?> =
        annotationMap.getOrDefault(annotationClzDesc, mutableMapOf())
            .also { annotationMap[annotationClzDesc] = it }

    /**
     * 更新注解信息
     * @param annotationClzDesc String 注解类的完整路径名
     */
    fun updateAnnotation(
        annotationClzDesc: String,
        key: String,
        value: Any?,
        updateWhenAbsent: Boolean = false
    ) = addAnnotation(annotationClzDesc).apply {
        update(this, key, value, updateWhenAbsent)
    }

    /**
     * 添加测试用例方法, 若方法已存在, 则不做操作
     * @param methodDesc String 方法名, 如: changeListExistTest
     */
    fun addMethod(methodDesc: String): CaseMethodBean =
        methodMap.getOrDefault(methodDesc, CaseMethodBean(methodDesc, clzDesc).also { methodBean ->
            methodBean.updateSrcFilePath(srcFilePath)
        }).also { methodBean ->
            methodMap[methodDesc] = methodBean
        }

    /**
     * 获取测试方法信息
     * @param methodDesc String 方法名, 如: changeListExistTest
     * @return CaseMethodBean?
     */
    fun getMethod(methodDesc: String) = methodMap[methodDesc]

    /**
     * 删除非测试用例方法
     */
    fun removeInvalidTestMethod() {
        val iterator = methodMap.values.iterator()
        while (iterator.hasNext()) {
            if (!iterator.next().checkCaseValid()) {
                iterator.remove()
            }
        }
    }
}

/**
 * 测试用例方法
 */
data class CaseMethodBean(
    val caseName: String, // 用例方法名, 如: changeListExistTest
    private val clzDescOrigin: String, // transform提供的用例方法所在类完整路径名, 如: corg/lynxz/demo/TestActivityTest
    var clzSrcFilePath: String = "", // 所在源文件路径
    var lineNum: Int = -1, // 根据方法首条指令所在行号 -1 得到的方法行号近似值, 正数有效
    val clzPath: String = clzDescOrigin.replace("/", "."), // 获取通过点分隔的用例类路径
    val annotationMap: MutableMap<String, MutableMap<String, Any?>> = ConcurrentHashMap() // 方法注解信息,key表示注解类的descriptor,value表示注解属性信息
) : UpdateAnnotationAction() {
    fun updateSrcFilePath(path: String) {
        if (path.isBlank()) {
            return
        }
        val arr = path.split("app/src/androidTest/")
        if (arr.size >= 2) { // 只显示相对路径
            clzSrcFilePath = arr[1]
        } else {
            clzSrcFilePath = path
        }
        // println("methodBean: $caseName updateSrcFilePath clzDescOrigin=$clzDescOrigin,clzSrcFilePath=$clzSrcFilePath")
    }

    /**
     * 更新方法所在行号位置, 仅在lineNum无效时该方法才起作用
     * 此处传入的是方法第一条指令所在行号, 向上偏移2行作为方法行号
     * P.S. 此为近似值,不精确,但不影响espressoAuto源码查看
     */
    fun updateLineNumIfNecessary(num: Int): CaseMethodBean {
        if (lineNum <= 0) {
            lineNum = num - 1
            // println("updateLineNumIfNecessary($num), method=$caseName")
        }
        return this
    }

    fun addAnnotation(annotationClzDesc: String): MutableMap<String, Any?> =
        annotationMap.getOrDefault(annotationClzDesc, mutableMapOf())
            .also { annotationMap[annotationClzDesc] = it }

    /**
     * 更新注解信息
     * @param annotationClzDesc String 注解类的完整路径名
     */
    fun updateAnnotation(
        annotationClzDesc: String, key: String, value: Any?,
        updateWhenAbsent: Boolean = false
    ) = addAnnotation(annotationClzDesc).apply {
        update(this, key, value, updateWhenAbsent)
    }

    /**
     * 是否是有效的测试用例方法
     */
    fun checkCaseValid() =
        annotationMap["Lorg/junit/Test;"] != null && annotationMap["Lorg/junit/Ignore;"] == null
}