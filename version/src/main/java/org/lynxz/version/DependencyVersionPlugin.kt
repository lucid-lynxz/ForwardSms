package org.lynxz.version

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.lynxz.version.transform.TestCaseTransform

class DependencyVersionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (project.name.equals("app")) {
            try {
                val buildDirPath = project.buildDir.absolutePath
                val rootDirPath = project.rootDir.absolutePath
                val projectDirPath = project.projectDir.absolutePath
                val caseFileName = "outputs/apk/testCaseInfo.json"
                println("DependencyVersionPlugin apply transform,project=${project.name},buildDirPath=$buildDirPath\nrootDirPath=$rootDirPath\nprojectDirPath=${projectDirPath}")
                project.extensions.getByType(AppExtension::class.java)
                    .registerTransform(
                        TestCaseTransform(
                            buildDirPath = buildDirPath,
                            projectDirPath = projectDirPath,
                            caseFileName = caseFileName
                        )
                    )
            } catch (e: Exception) {
                println("DependencyVersionPlugin apply exception: $e")
            }
        }
    }
}