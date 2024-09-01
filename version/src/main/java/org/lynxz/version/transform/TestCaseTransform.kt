package org.lynxz.version.transform

import org.lynxz.version.util.FileUtilSimple
import org.lynxz.version.util.GsonUtilSimple
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * 记录所有测试用例方法信息, 生成日志文件
 * @param buildDirPath 当前project build/ 目录绝对路径, 若为空,则无效
 * @param projectDirPath 当前project目录绝对路径, 若为空,则无效
 * @param caseFileName 输出扫描到的用例结果到 buildDirPath 时, 要使用的文件名
 */
class TestCaseTransform(
    private val buildDirPath: String = "",
    private val projectDirPath: String = "",
    private val caseFileName: String = "testCaseInfo.json"
) : BaseTransform() {
    init {
        CaseManager.androidTestRootPath = "$projectDirPath/src/androidTest".replace("\\", "/")
        printLog("TestCaseTransform buildDirPath=$buildDirPath,caseFileName=$caseFileName,androidTestRootPath=${CaseManager.androidTestRootPath}")
    }

    override fun modifyClassCode(inputFile: File, outputFile: File) {
        // 只关注测试用例类
        val filePath = FileUtilSimple.recookPath(inputFile.absolutePath)
        printLog("startModifyClassCode: ${filePath}")
//        if (!filePath.contains("debugAndroidTest/classes/corg/lynxz/demo/TestActivityTest.class")) {
        if (!filePath.contains("debugAndroidTest")) {
            FileUtilSimple.copyFile(inputFile, outputFile)
            return
        }

        try {
            val fis = FileInputStream(inputFile)
            val cr = ClassReader(fis)
            if (cr.access and Opcodes.ACC_PUBLIC == 0) {
                FileUtilSimple.copyFile(inputFile, outputFile)
                fis.close()
                return
            }

            val msg = """
                
                modifyClassCode ${filePath}
                access=${cr.access}
                clsName=${cr.className}
                superName=${cr.superName}
                interfaces=${cr.interfaces}
            """.trimIndent()
            printLog(msg)

            val fos = FileOutputStream(outputFile)
            val cw = ClassWriter(ClassWriter.COMPUTE_MAXS)
            cr.accept(LogCaseMethodVisitor(Opcodes.ASM7, cw), ClassReader.EXPAND_FRAMES)
            fos.write(cw.toByteArray())
            fis.close()
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onTransformFinished() {
        super.onTransformFinished()
        val allClzList0 = CaseManager.getAllClzList()
        val allMethodList0 = CaseManager.getAllMethodList()
        printLog("onTransformFinished allClzSize=${allClzList0.size}, allMethodSize=${allMethodList0.size}")
        if (allClzList0.isEmpty() || allMethodList0.isEmpty()) {
            return
        }

//        val allClzJson0 = GsonUtilSimple.toJson(allClzList0)
//        val allClzPath0 = FileUtilSimple.recookPath("$buildDirPath/outputs/apk/allClzMap_0.json")
//        FileUtilSimple.writeToFile(allClzJson0, allClzPath0, false)
//        printLog("onTransformFinished allClzList0 size=${allClzList0.size}, Json0Length=${allClzJson0?.length}")
//
//        val allCaseMethodJson0 = GsonUtilSimple.toJson(allMethodList0)
//        val caseInfoFilePath0 =
//            FileUtilSimple.recookPath("$buildDirPath/outputs/apk/caseInfo_0.json")
//        FileUtilSimple.writeToFile(allCaseMethodJson0, caseInfoFilePath0, false)
//        printLog("onTransformFinished allMethodList0 size=${allMethodList0.size},Json0Length=${allCaseMethodJson0?.length}")

        CaseManager.recurseUpdateProjectAnnotation() // 进行注解更新及清理空白的测试类

//        val allClzList = CaseManager.getAllClzList()
//        val allClzJson = GsonUtilSimple.toJson(allClzList)
//        val allClzPath = FileUtilSimple.recookPath("$buildDirPath/outputs/apk/allClzMap_1.json")
//        FileUtilSimple.writeToFile(allClzJson, allClzPath, false)
//        printLog("onTransformFinished allClzList size=${allClzList.size}, JsonSize=${allClzJson?.length}")

        val allMethod = CaseManager.getAllMethodList()
        val allMethodJson = GsonUtilSimple.toJson(allMethod)
        printLog("onTransformFinished allCaseMethod size=${allMethod.size},jsonLength=${allMethodJson.length}")

        if (buildDirPath.isNotBlank()) {
            val caseInfoFilePath = FileUtilSimple.recookPath("$buildDirPath/$caseFileName")
            FileUtilSimple.createFile(caseInfoFilePath)
            val fileExist = FileUtilSimple.checkFileExists(caseInfoFilePath)
            val fileLen = FileUtilSimple.getFileLen(caseInfoFilePath)
            printLog("onTransformFinished 输出测试用例汇总结果到文件:$caseInfoFilePath,fileExist=$fileExist,fileLen=$fileLen")
            FileUtilSimple.writeToFile(allMethodJson, caseInfoFilePath, false)
        } else {
            printLog("onTransformFinished allCaseMethod jsonStr=$allMethodJson")
        }
    }

    // 记录所有测试用例方法信息
    class LogCaseMethodVisitor(api: Int, classVisitor: ClassVisitor?) :
        ClassVisitor(api, classVisitor) {
        private var clzDesc: String = "" // 类完整路径

        override fun visit(
            version: Int,
            access: Int,
            name: String, // 类完整名称, 如: corg/lynxz/demo/TestActivityTest
            signature: String?, // 类的签名，如果类不是泛型或者没有继承泛型类，那么signature为空
            superName: String?, // 父类完整名称, 如: corg/lynxz/demo/BaseTestSuit
            interfaces: Array<out String>?
        ) {
            super.visit(version, access, name, signature, superName, interfaces)
            printLog("ClassVisitor visit start name=$name,signature=${signature},superName=$superName,interfaces=$interfaces")
            clzDesc = name
            CaseManager.addClz(name, superName)
        }

        override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor {
            val va = super.visitAnnotation(descriptor, visible)
            printLog("ClassVisitor visitAnnotation descriptor=${descriptor},visible=$visible")
            CaseManager.addClzAnnotation(clzDesc, descriptor)
            return object : AnnotationVisitor(api, va) {
                override fun visit(name: String, value: Any?) {
                    super.visit(name, value)
                    val isClass = value is Class<*>
                    val isType = value is Type
                    if (isClass or isType) {
                        CaseManager.updateClzAnnotation(clzDesc, descriptor, name, "$value")
                    } else {
                        CaseManager.updateClzAnnotation(clzDesc, descriptor, name, value)
                    }
                    printLog("   ClassVisitor visitAnnotation2 $descriptor,name=$name,value=$value,javaClass=${value?.javaClass},isType=$isType,isClass=$isClass")
                }
            }
        }

        override fun visitMethod(
            access: Int,
            methodName: String, // 方法名
            descriptor: String?, // 方法签名, 如: ()V
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor {
            val mv = super.visitMethod(access, methodName, descriptor, signature, exceptions)
            printLog("ClassVisitor visitMethod name=$methodName,descriptor=${descriptor},signature=$signature")
            val curMethodBean = CaseManager.addMethod(clzDesc, methodName)
            return object : MethodVisitor(api, mv) {
                override fun visitLineNumber(line: Int, start: Label?) {
                    super.visitLineNumber(line, start)
                    curMethodBean.updateLineNumIfNecessary(line)
//                    printLog("visitLineNumber line=$line,start=${GsonUtilSimple.toJson(start)}")
                }

                override fun visitAnnotation(
                    annotationDescriptor: String,
                    visible: Boolean
                ): AnnotationVisitor {
                    val va = super.visitAnnotation(annotationDescriptor, visible)
                    printLog("MethodVisitor visitAnnotation descriptor=${annotationDescriptor},visible=$visible,methodName=$methodName,methodDescriptor=$descriptor")
                    CaseManager.addMethodAnnotation(clzDesc, methodName, annotationDescriptor)
                    return object : AnnotationVisitor(api, va) {
                        override fun visit(annotationPropertyName: String, value: Any?) {
                            super.visit(annotationPropertyName, value)
                            CaseManager.updateMethodAnnotation(
                                clzDesc,
                                methodName,
                                annotationDescriptor,
                                annotationPropertyName,
                                value
                            )
                            printLog("   MethodVisitor visitAnnotation2 $annotationDescriptor,name=$annotationPropertyName,value=$value")
                        }
                    }
                }

                override fun visitParameterAnnotation(
                    parameter: Int,
                    descriptor: String,
                    visible: Boolean
                ): AnnotationVisitor {
                    printLog("MethodVisitor visitParameterAnnotation descriptor=${descriptor},visible=$visible,parameter=$parameter,methodName=$methodName,methodDescriptor=$descriptor")
                    return super.visitParameterAnnotation(parameter, descriptor, visible)
                }

                override fun visitEnd() {
                    super.visitEnd()
                    CaseManager.removeInvalidTestMethod(clzDesc) // 删除无效的用例方法
                }
            }
        }
    }
}
