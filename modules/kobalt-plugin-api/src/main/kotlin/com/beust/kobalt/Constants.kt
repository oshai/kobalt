package com.beust.kobalt

import com.beust.kobalt.misc.KFiles

object Constants {
    const val LOG_QUIET_LEVEL = 0
    const val LOG_DEFAULT_LEVEL = 1
    const val LOG_MAX_LEVEL = 3
    val BUILD_FILE_NAME = "Build.kt"
    val BUILD_FILE_DIRECTORY = "kobalt/src"
    val BUILD_FILE_PATH = KFiles.joinDir(BUILD_FILE_DIRECTORY, BUILD_FILE_NAME)
    val KOTLIN_COMPILER_VERSION = "1.3.72"

    internal val DEFAULT_REPOS = listOf<HostConfig>(
            //            "https://maven-central.storage.googleapis.com/",
            HostConfig("https://repo1.maven.org/maven2/", "Maven"),
            HostConfig("https://jcenter.bintray.com/", "JCenter")
//            "https://repository.jetbrains.com/all/", // <-- contains snapshots

            // snapshots
//            "https://oss.sonatype.org/content/repositories/snapshots/"
//            , "https://repository.jboss.org/nexus/content/repositories/root_repository/"
    )

}
