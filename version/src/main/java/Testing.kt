object Testing {
    const val jUnit = "junit:junit:4.12"

    // support test相关版本查看: https://mvnrepository.com/artifact/com.android.support.test
    const val orchestrator = "com.android.support.test:orchestrator:1.0.2"
    const val rules = "com.android.support.test:rules:1.0.2"
    const val runner = "com.android.support.test:runner:1.0.2"
    const val espressoCore = "com.android.support.test.espresso:espresso-core:3.0.2"
    const val espressoIntents = "com.android.support.test.espresso:espresso-intents:3.0.2"
    const val espressoIdlingRes = "com.android.support.test.espresso:espresso-idling-resource:3.0.2"

    // AndroidX test相关版本查看: https://mvnrepository.com/artifact/androidx.test
    const val orchestratorX = "androidx.test:orchestrator:1.4.0"
    const val rulesX = "androidx.test:rules:1.4.0"
    const val runnerX = "androidx.test:runner:1.4.0"
    const val extJunitX = "androidx.test.ext:junit:1.1.2"
    const val testServiceX = "androidx.test.services:test-services:1.4.0"
    const val espressoCoreX = "androidx.test.espresso:espresso-core:3.5.0-alpha05"
    const val espressoIntentsX = "androidx.test.espresso:espresso-intents:3.5.0-alpha05"
    const val espressoIdlingResX = "androidx.test.espresso:espresso-idling-resource:3.5.0-alpha05"

    /*
     * mock TextUtils 方法时, 需要添加 byteBuddy(testImplementation Libs.byteBuddy),
     * 否则报错:Could not initialize plugin: interface org.mockito.plugins.MockMaker
     */
    val powerMockCore = "org.mockito:mockito-core:2.8.9"
    val powerMockJUnit = "org.powermock:powermock-module-junit4:1.7.3"
    val powerMockJunitRule = "org.powermock:powermock-module-junit4-rule:1.7.3"
    val powerMockandroid = "org.mockito:mockito-android:2.8.9"
    val powerMockMockito2 = "org.powermock:powermock-api-mockito2:1.7.3" //注意这里是mockito2
    val powerMockClassLoading = "org.powermock:powermock-classloading-xstream:1.7.3"
}