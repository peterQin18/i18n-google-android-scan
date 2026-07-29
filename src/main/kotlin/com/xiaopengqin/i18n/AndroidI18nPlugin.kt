package com.xiaopengqin.i18n

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Action

class AndroidI18nPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("androidI18n", AndroidI18nExtension::class.java)

        project.tasks.register("i18nScan", I18nScanTask::class.java, Action { task ->
            task.group = "internationalization"
            task.description = "Scans Android XML, Compose, and explicitly marked code strings."
            task.projectDirectory.set(project.layout.projectDirectory)
            task.scanRoots.set(extension.scanRoots)
            task.sourceMarker.set(extension.sourceMarker)
        })

        project.tasks.register("i18nCheck", I18nCheckTask::class.java, Action { task ->
            task.group = "verification"
            task.description = "Checks the i18n scan report according to the configured policy."
            task.failOnCandidates.set(extension.failOnCandidates)
        })

        project.tasks.register("i18nSync", I18nSyncTask::class.java, Action { task ->
            task.group = "internationalization"
            task.description = "Synchronizes i18n resources with Google Sheets."
            task.spreadsheetId.set(extension.googleSheet.spreadsheetId)
            task.glossaryEnabled.set(extension.glossary.enabled)
        })
    }
}
