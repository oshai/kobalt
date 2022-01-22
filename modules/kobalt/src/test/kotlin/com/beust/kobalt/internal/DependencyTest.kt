package com.beust.kobalt.internal

import com.beust.kobalt.BaseTest
import com.beust.kobalt.TestConfig
import com.beust.kobalt.api.ITestJvmFlagContributor
import com.beust.kobalt.api.ITestJvmFlagInterceptor
import com.beust.kobalt.api.KobaltContext
import com.beust.kobalt.api.Project
import com.beust.kobalt.maven.dependency.FileDependency
import com.beust.kobalt.project
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test
import javax.inject.Inject


/**
 * Test ITestJvmFlagContributor and ITestJvmFlagInterceptor.
 */
class DependencyTest @Inject constructor(val context: KobaltContext) : BaseTest() {
    private fun isWindows() = System.getProperty("os.name").toLowerCase().contains("ndows")
    private val A_JAR = if (isWindows()) "c:\\tmp\\a.jar" else "/tmp/a.jar"
    private val B_JAR = if (isWindows()) "c:\\tmp\\b.jar" else "/tmp/b.jar"

    private val project : Project get() = project { name = "dummy" }
    private val classpath = listOf(FileDependency(A_JAR))
    private val contributor = object : ITestJvmFlagContributor {
        override fun testJvmFlagsFor(project: Project, context: KobaltContext,
                currentFlags: List<String>): List<String> {
            return listOf("-agent", "foo")
        }
    }

    private val interceptor = object : ITestJvmFlagInterceptor {
        override fun testJvmFlagsFor(project: Project, context: KobaltContext,
                currentFlags: List<String>): List<String> {
            return currentFlags.map { if (it == A_JAR) B_JAR else it }
        }
    }

    private fun runTest(pluginInfo: IPluginInfo, expected: List<String>) {
        val result = TestNgRunner().calculateAllJvmArgs(project, context, TestConfig(project),
                classpath, pluginInfo)
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun noContributorsNoInterceptors() {
        runTest(BasePluginInfo(), listOf("-ea", "-classpath", A_JAR))
    }

    @Test
    fun contributorOnly() {
        runTest(BasePluginInfo().apply { testJvmFlagContributors.add(contributor) },
                listOf("-ea", "-classpath", A_JAR, "-agent", "foo"))
    }

    @Test
    fun interceptorOnly() {
        runTest(BasePluginInfo().apply { testJvmFlagInterceptors.add(interceptor) },
                listOf("-ea", "-classpath", B_JAR))
    }

    @Test
    fun contributorAndInterceptor() {
        runTest(BasePluginInfo().apply {
                testJvmFlagContributors.add(contributor)
                testJvmFlagInterceptors.add(interceptor)
            },
            listOf("-ea", "-classpath", B_JAR, "-agent", "foo"))
    }
}

