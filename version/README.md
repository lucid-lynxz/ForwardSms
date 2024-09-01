1. 在项目的 `setting.gradle` 中启用:
```groovy
includeBuild("version")
```
2. 在项目的 `build.gradle` 中添加并启用该插件
```groovy
buildscript {
 // ....
}

plugins {
    id "org.lynxz.version"
}

subprojects { proj ->
    proj.apply(plugin: "org.lynxz.version")
}
```

3. 在各模块的 `build.gradle` 中就可以引用指定类的信息, 如: `BuildConfigInfo`  `Libs` 等