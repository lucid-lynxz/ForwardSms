抽取通用的签名以及依赖关系配置

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
            signingConfig signingConfigs.debug
        }
        release {
            initWith(debug)
        }
    }
}
```

# 使用方法
1. 将 `config` 目录复制到项目根目录下, 与 `app/` 模块同级;
2. 在 `project/build.gradle` 中添加依赖

```groovy
buildscript {
    // 写在闭包中才能引用到,而且可以被各module直接应用
    apply from: "./config/dependencies.gradle"

    repositories {
        google()
        jcenter()
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
// apply from: "../config/dependencies.gradle"
apply from: "../config/sign.gradle"

android{
    compileSdkVersion androidCompileSdkVersion
    defaultConfig {
        minSdkVersion androidMinSdkVersion
        targetSdkVersion androidTargetSdkVersion
    }

    buildTypes {
        debug {
            signingConfig signingConfigs.release
        }
        release {
            signingConfig signingConfigs.release
        }
    }
}

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    testImplementation(test.junit)
    androidTestImplementation(test.espresso)
    androidTestImplementation(test.testRunner)

    implementation(libs.appcompat)
    implementation(libs.supportV4)
    implementation(libs.constraintLayout)
    
    implementation(libs.kotlinLib)
    implementation(libs.ktxCore)
    implementation(libs.kotlinxCoroutinesCore)
    implementation(libs.kotlinxCoroutinesAndroid)

    // 依赖就可以改成如此形式了, 在多模块开发时会很方便
    // rx
    implementation(libs.rxJava2)
    implementation(libs.rxAndroid2)
    implementation(libs.rxPermission2)
}
```
