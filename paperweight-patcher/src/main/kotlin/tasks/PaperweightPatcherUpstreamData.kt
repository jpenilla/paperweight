/*
 * paperweight is a Gradle plugin for the PaperMC project.
 *
 * Copyright (c) 2021 Kyle Wood (DemonWav)
 *                    Contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 2.1 only, no later versions.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 */

package io.papermc.paperweight.patcher.tasks

import io.papermc.paperweight.util.Constants
import io.papermc.paperweight.util.path
import kotlin.io.path.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.build.NestedRootBuildRunner

abstract class PaperweightPatcherUpstreamData : DefaultTask() {

    @get:InputDirectory
    abstract val projectDir: DirectoryProperty

    @get:InputDirectory
    abstract val workDir: DirectoryProperty

    @get:OutputFile
    abstract val dataFile: RegularFileProperty

    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun run() {
        val params = NestedRootBuildRunner.createStartParameterForNewBuild(services)
        params.projectDir = projectDir.get().asFile

        params.setTaskNames(listOf(Constants.PAPERWEIGHT_PREPARE_DOWNSTREAM))
        params.projectProperties[Constants.UPSTREAM_WORK_DIR_PROPERTY] = workDir.path.absolutePathString()
        params.projectProperties[Constants.PAPERWEIGHT_PREPARE_DOWNSTREAM] = dataFile.path.absolutePathString()

        NestedRootBuildRunner.createNestedRootBuild(null, params, services).run {
            it.run()
        }
    }
}
