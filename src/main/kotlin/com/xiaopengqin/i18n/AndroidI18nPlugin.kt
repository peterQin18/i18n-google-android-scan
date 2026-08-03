package com.xiaopengqin.i18n

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Action

class AndroidI18nPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("androidI18n", AndroidI18nExtension::class.java)
        val reportFile = project.layout.buildDirectory.file("reports/i18n/scan.json")
        val manifestFile = project.layout.projectDirectory.file("src/main/i18n/i18n-manifest.json")
        extension.outputDirectory.convention(project.layout.buildDirectory.dir("generated/i18n/res"))
        project.pluginManager.withPlugin("com.android.application") {
            project.afterEvaluate {
                val android = project.extensions.findByName("android") ?: return@afterEvaluate
                val sourceSets = android.javaClass.methods.firstOrNull { it.name == "getSourceSets" }?.invoke(android) ?: return@afterEvaluate
                val main = sourceSets.javaClass.methods.firstOrNull {
                    it.name == "getByName" && it.parameterCount == 1
                }?.invoke(sourceSets, "main") ?: return@afterEvaluate
                val res = main.javaClass.methods.firstOrNull { it.name == "getRes" }?.invoke(main) ?: return@afterEvaluate
                res.javaClass.methods.firstOrNull {
                    it.name == "srcDir" && it.parameterCount == 1
                }?.invoke(res, extension.outputDirectory.get().asFile)
            }
        }

        val scanTask = project.tasks.register("i18nScan", I18nScanTask::class.java, Action { task ->
            task.group = "internationalization"
            task.description = "Scans Android XML, Compose, and explicitly marked code strings."
            task.projectDirectory.set(project.layout.projectDirectory)
            task.scanRoots.set(extension.scanRoots)
            task.sourceMarker.set(extension.sourceMarker)
            task.reportFile.set(reportFile)
            task.manifestFile.set(manifestFile)
        })

        project.tasks.register("i18nCheck", I18nCheckTask::class.java, Action { task ->
            task.group = "verification"
            task.description = "Checks the i18n scan report according to the configured policy."
            task.failOnCandidates.set(extension.failOnCandidates)
            task.reportFile.set(reportFile)
            task.dependsOn(scanTask)
        })

        project.tasks.register("i18nTranslate", I18nTranslateTask::class.java, Action { task ->
            task.group = "internationalization"
            task.description = "Translates scanned copy and generates Android string resources."
            task.reportFile.set(reportFile)
            task.projectDirectory.set(project.layout.projectDirectory)
            task.sourceLocale.set(extension.sourceLocale)
            task.targetLocales.set(extension.targetLocales)
            task.outputDirectory.set(extension.outputDirectory)
            task.generatedFileName.set(extension.generatedFileName)
            task.baseUrl.set(extension.llm.baseUrl)
            task.apiKey.set(extension.llm.apiKey)
            task.model.set(extension.llm.model)
            task.temperature.set(extension.llm.temperature)
            task.dependsOn(scanTask)
        })

        val applyTask = project.tasks.register("i18nApply", I18nApplyTask::class.java, Action { task ->
            task.group = "internationalization"
            task.description = "Replaces scanned Compose Text literals with stringResource calls."
            task.reportFile.set(reportFile)
            task.androidNamespace.convention("")
            task.migrationFile.set(project.layout.buildDirectory.file("reports/i18n/non-compose-migration.md"))
            task.manifestFile.set(manifestFile)
            task.dependsOn("i18nTranslate")
        })

        project.pluginManager.withPlugin("com.android.application") {
            project.afterEvaluate {
                val android = project.extensions.findByName("android") ?: return@afterEvaluate
                val namespace = android.javaClass.methods.firstOrNull { it.name == "getNamespace" && it.parameterCount == 0 }
                    ?.invoke(android) as? String ?: return@afterEvaluate
                applyTask.configure { it.androidNamespace.set(namespace) }
            }
        }

        project.tasks.register("i18nSync", I18nSyncTask::class.java, Action { task ->
            task.group = "internationalization"
            task.description = "Synchronizes i18n resources with Google Sheets."
            task.spreadsheetId.set(extension.googleSheet.spreadsheetId)
            task.glossaryEnabled.set(extension.glossary.enabled)
        })
    }
}
