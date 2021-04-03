# 抽取通用的签名以及依赖关系配置

## sign.gradle 的使用
1. 复制 config/ 目录到项目根目录下
2. 在根目录的 `local.properties` 中填入如下内容(根据实际keystore信息修改):

```plain
keyStoreFile=config/lynxz_key_store.jks
storePassword=lynx@333
keyAlias=lynx
keyPassword=lynx@333
```

3. 修改 `app/build.gradle` 文件

```groovy
apply from: "../config/sign.gradle"

android {
    buildTypes {
        debug {
            signingConfig signingConfigs.release
        }
        release {
            initWith(debug)
        }
    }
}
```

## 使用方法
1. 将 `config` 目录复制到项目根目录下, 与 `app/` 模块同级;
2. 在 `project/build.gradle` 中添加依赖

```groovy
buildscript {
    // 写在闭包中才能引用到,而且可以被各module直接应用
    apply from: "./config/dependencies.gradle"
    ext.use_androidx = false // false-使用support库

    repositories {
        // google()
        // jcenter()
        maven { url 'https://maven.aliyun.com/repository/google/' }
        maven { url 'https://maven.aliyun.com/repository/jcenter/' }
        maven { url 'https://jitpack.io' }
        mavenCentral()
    }

    dependencies {
        //classpath(classPathLib.gradleTool)
        //classpath(classPathLib.kotlinGradlePlugin)
        // 对比上面的方式,感觉还是写成这样比较方便点
        classpath 'com.android.tools.build:gradle:3.1.3'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinLibVersion"
    }
}

// 我们甚至可以在 subprojects 中这么写,来统一添加plugin, 避免每个module都要写一遍
// subprojects {
//     println("subproject name: ${project.name}")
//     //指定主module名称 ,需要先添加 android plugin才能添加kotlin-android plugin
//     if (project.name == 'app') {
//         apply plugin: 'com.android.application'
//     } else {
//         apply plugin: 'com.android.library'
//     }
//     apply plugin: 'kotlin-android'
//     apply plugin: 'kotlin-android-extensions'
//     apply plugin: 'kotlin-kapt'
// }
```

3. 在 `app/build.gradle` 中添加依赖:

```groovy
// app/build.gradle
apply plugin: 'com.android.application'
apply plugin: 'kotlin-android'
apply plugin: 'kotlin-android-extensions'
apply plugin: 'kotlin-kapt'
// 导入签名配置
apply from: "../config/sign.gradle"

android {
    compileSdkVersion androidCompileSdkVersion
    buildToolsVersion androidBuildToolsVersion
    defaultConfig {
        minSdkVersion androidMinSdkVersion
        targetSdkVersion androidTargetSdkVersion

        if (use_androidx) {
            testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
        } else {
            testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
        }
    }

    buildTypes {
        debug {
            signingConfig signingConfigs.release
        }
        release {
            initWith(debug)
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    testImplementation(test.junit)
    implementation(libs.kotlinLib)

    if (use_androidx) {
        androidTestImplementation(test.junitExtX)
        androidTestImplementation(test.espressoX)
        androidTestImplementation(test.testRunnerX)

        implementation(libs.appcompatX)
        implementation(libs.supportV4X)
        implementation(libs.constraintLayoutX)

        implementation(libs.ktxCore)
        implementation(libs.kotlinxCoroutinesCore)
        implementation(libs.kotlinxCoroutinesAndroid)

        implementation(libs.xUtils)
    } else {
        androidTestImplementation(test.espresso)
        androidTestImplementation(test.testRunner)

        implementation(libs.appcompat)
        implementation(libs.supportV4)
        implementation(libs.constraintLayout)

        // rx依赖
//        implementation(libs.rxJava2)
//        implementation(libs.rxAndroid2)
//        implementation(libs.rxPermission2)
    }
}
```
