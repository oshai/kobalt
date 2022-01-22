package com.beust.kobalt

import com.beust.kobalt.misc.KFiles
import com.beust.kobalt.misc.kobaltLog
import com.beust.kobalt.misc.warn
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test
import java.io.File
import java.io.FileInputStream
import java.io.FileReader
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarInputStream

/**
 * Make sure the distribution zip file contains all the right files and no bad files.
 */
class VerifyKobaltZipTest : KobaltTest() {
    @Test
    fun verifySourceJarFile() {
        assertExistsInJar("kobalt-$KOBALT_VERSION-sources.jar", "com/beust/kobalt/Main.kt")
    }

    @Test
    fun verifyZipFile() {
        var foundKobaltw = false
        var foundJar = false
        var foundWrapperJar = false

        val root = "kobalt-$KOBALT_VERSION"
        val mainJarFilePath = "$root.jar"
        val zipFilePath = KFiles.joinDir("kobaltBuild", "libs", "$root.zip")
        if (File(zipFilePath).exists()) {
            val zipFile = JarFile(zipFilePath)
            val stream = JarInputStream(FileInputStream(zipFilePath))
            var entry = stream.nextEntry
            while (entry != null) {
                assertThat(entry.name).doesNotContain("\\")
                if (! entry.name.startsWith(root)) {
                    throw AssertionError("Entries in the zip file should be under the directory $root")
                }
                if (entry.name.endsWith("kobaltw")) {
                    val ins = zipFile.getInputStream(entry)
                    ins.readBytes().forEach {
                        // Look for carriage returns
                        if (it.compareTo(13) == 0) {
                            throw AssertionError("kobaltw has wrong line endings")
                        }
                    }
                    if (OperatingSystem.current().isWindows()) {
                        warn("Can't determine if kobaltw is executable under Windows")
                    } else if (!Files.isExecutable(Paths.get("dist/kobaltw"))) {
                        throw AssertionError("kobaltw has invalid permissions")
                    }
                    foundKobaltw = true
                } else if (entry.name.endsWith(mainJarFilePath)) {
                    val ins = zipFile.getInputStream(entry)
                    if (ins.available() < 20000000) {
                        throw AssertionError(mainJarFilePath + " is too small: " + mainJarFilePath)
                    }
                    verifyMainJarFile(ins)
                    foundJar = true
                } else if (entry.name.endsWith("kobalt-wrapper.jar")) {
                    val ins = zipFile.getInputStream(entry)
                    foundWrapperJar = true
                    assertExistence(ins, listOf("kobalt.properties", "com/beust/kobalt/wrapper/Main.class"))
                }
                entry = stream.nextEntry
            }
            if (!foundKobaltw) {
                throw AssertionError("Couldn't find kobaltw in $zipFilePath")
            }
            if (!foundJar) {
                throw AssertionError("Couldn't find jar in $zipFilePath")
            }
            if (!foundWrapperJar) {
                throw AssertionError("Couldn't find wrapper jar in $zipFilePath")
            }
            kobaltLog(1, "$zipFilePath looks correct")
        } else {
            kobaltLog(1, "Couldn't find $zipFilePath, skipping test")
        }
    }

    // Can't use Kobalt.version since the tests have their own src/test/resources/kobalt.properties
    val KOBALT_VERSION: String
        get() {
            val p = Properties()
            p.load(FileReader("src/main/resources/kobalt.properties"))
            val result = p.getProperty("kobalt.version")
            return result
        }

    private fun verifyMainJarFile(ins: InputStream) {
        assertExistence(ins,
                listOf("com/beust/kobalt/MainKt.class",
                    "templates/kobaltPlugin/kobaltPlugin.jar", "com/beust/kobalt/Args.class",
                    "com/beust/kobalt/wrapper/Main.class"),
                listOf("BuildKt.class", "Versions.class"))
    }

    private fun assertExistence(ins: InputStream,
            included: List<String>,
            excluded: List<String> = emptyList(),
            toName: (JarEntry) -> String = JarEntry::toString) {
        val seq = toSequence(ins)
        val foundItems = hashSetOf<String>()
        seq.forEach { entry ->
            val entryName = toName(entry)

            if (included.contains(entryName)) {
                foundItems.add(entryName)
            }

            if (excluded.any { entryName == it }) {
                throw AssertionError(entryName + " should not be in the jar file")
            }
        }

        if (foundItems != included.toSet()) {
            val missing = arrayListOf<String>().apply { addAll(included) }
            missing.removeAll(foundItems)
            throw AssertionError("Didn't find a few items: " + missing)
        }
    }

    private fun assertExistsInJar(jarName: String, vararg fileNames: String) {
        val sourceJarPath = KFiles.joinDir("kobaltBuild", "libs", jarName)
        val file = File(sourceJarPath)
        if (file.exists()) {
            assertExistence(FileInputStream(file), arrayListOf<String>().apply { addAll(fileNames) })
        } else {
            kobaltLog(1, "Couldn't find $file, skipping test")
        }
    }

    fun JarInputStream.asSequence() = generateSequence { nextJarEntry }

    private fun toSequence(ins: InputStream): Sequence<JarEntry> = JarInputStream(ins).asSequence()
}
