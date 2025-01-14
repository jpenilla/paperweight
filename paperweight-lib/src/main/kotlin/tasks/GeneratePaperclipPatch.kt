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

package io.papermc.paperweight.tasks

import io.papermc.paperweight.PaperweightException
import io.papermc.paperweight.util.deleteForcefully
import io.papermc.paperweight.util.openZip
import io.papermc.paperweight.util.path
import io.sigpipe.jbsdiff.Diff
import java.io.IOException
import java.nio.file.Path
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Properties
import kotlin.experimental.and
import kotlin.io.path.*
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile

abstract class GeneratePaperclipPatch : ZippedTask() {

    @get:InputFile
    abstract val originalJar: RegularFileProperty

    @get:InputFile
    abstract val patchedJar: RegularFileProperty

    @get:Input
    abstract val mcVersion: Property<String>

    override fun run(rootDir: Path) {
        val patchFile = rootDir.resolve("paperMC.patch")
        val propFile = rootDir.resolve("patch.properties")
        val protocol = rootDir.resolve("META-INF/$PROTOCOL_FILE")

        try {
            patchedJar.path.openZip().use { zipFs ->
                val protocolPath = zipFs.getPath("META-INF", PROTOCOL_FILE)
                if (protocolPath.notExists()) {
                    protocol.deleteForcefully()
                    return@use
                }

                protocol.parent.createDirectories()
                protocolPath.copyTo(protocol, overwrite = true)
            }
        } catch (e: IOException) {
            throw PaperweightException("Failed to read $patchedJar contents", e)
        }

        // Read the files into memory
        println("Reading jars into memory")
        val originalBytes = originalJar.path.readBytes()
        val patchedBytes = patchedJar.path.readBytes()

        println("Creating Paperclip patch")
        try {
            patchFile.outputStream().use { patchOutput ->
                Diff.diff(originalBytes, patchedBytes, patchOutput)
            }
        } catch (e: Exception) {
            throw PaperweightException("Error creating patch between ${originalJar.path} and ${patchedJar.path}", e)
        }

        // Add the SHA-256 hashes for the files
        val digestSha256 = try {
            MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw PaperweightException("Could not create SHA-256 hasher", e)
        }

        // Vanilla's URL uses a SHA1 hash of the vanilla server jar
        val digestSha1 = try {
            MessageDigest.getInstance("SHA1")
        } catch (e: NoSuchAlgorithmException) {
            throw PaperweightException("Could not create SHA1 hasher", e)
        }

        println("Hashing files")
        val originalSha1 = digestSha1.digest(originalBytes)
        val originalSha256 = digestSha256.digest(originalBytes)
        val patchedSha256 = digestSha256.digest(patchedBytes)

        val prop = Properties()
        prop["originalHash"] = toHex(originalSha256)
        prop["patchedHash"] = toHex(patchedSha256)
        prop["patch"] = "paperMC.patch"
        prop["sourceUrl"] = "https://launcher.mojang.com/v1/objects/" + toHex(originalSha1).toLowerCase() + "/server.jar"
        prop["version"] = mcVersion.get()

        println("Writing properties file")
        propFile.bufferedWriter().use { writer ->
            prop.store(writer, "Default Paperclip launch values. Can be overridden by placing a paperclip.properties file in the server directory.")
        }
    }

    private fun toHex(hash: ByteArray): String {
        val sb: StringBuilder = StringBuilder(hash.size * 2)
        for (aHash in hash) {
            sb.append("%02X".format(aHash and 0xFF.toByte()))
        }
        return sb.toString()
    }

    companion object {
        const val PROTOCOL_FILE = "io.papermc.paper.daemon.protocol"
    }
}
