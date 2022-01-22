package com.beust.kobalt.maven

import com.beust.kobalt.BaseTest
import com.beust.kobalt.api.IClasspathDependency
import com.beust.kobalt.api.Kobalt
import com.beust.kobalt.api.Project
import com.beust.kobalt.app.BuildFileCompiler
import com.beust.kobalt.maven.aether.Filters
import com.beust.kobalt.maven.aether.Scope
import com.google.inject.Inject
import java.util.Collections
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.aether.graph.DependencyFilter
import org.eclipse.aether.graph.DependencyNode
//import org.eclipse.aether.util.filter.AndDependencyFilter
import org.testng.annotations.Test

class AndDependencyFilter : DependencyFilter {
    private val filters: MutableCollection<DependencyFilter?> = LinkedHashSet()

    /**
     * Creates a new filter from the specified filters. Prefer [.newInstance]
     * if any of the input filters might be `null`.
     *
     * @param filters The filters to combine, may be `null` but must not contain `null` elements.
     */
    constructor(vararg filters: DependencyFilter?) {
        if (filters != null) {
            Collections.addAll(this.filters, *filters)
        }
    }

    /**
     * Creates a new filter from the specified filters.
     *
     * @param filters The filters to combine, may be `null` but must not contain `null` elements.
     */
    constructor(filters: Collection<DependencyFilter>?) {
        if (filters != null) {
            this.filters.addAll(filters)
        }
    }

    override fun accept(node: DependencyNode, parents: List<DependencyNode>): Boolean {
        for (filter in filters) {
            if (!filter!!.accept(node, parents)) {
                return false
            }
        }
        return true
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj == null || javaClass != obj.javaClass) {
            return false
        }
        val that = obj as AndDependencyFilter
        return filters == that.filters
    }

    override fun hashCode(): Int {
        var hash = javaClass.hashCode()
        hash = hash * 31 + filters.hashCode()
        return hash
    }

    companion object {
        /**
         * Creates a new filter from the specified filters.
         *
         * @param filter1 The first filter to combine, may be `null`.
         * @param filter2 The second filter to combine, may be `null`.
         * @return The combined filter or `null` if both filter were `null`.
         */
        fun newInstance(filter1: DependencyFilter?, filter2: DependencyFilter?): DependencyFilter? {
            if (filter1 == null) {
                return filter2
            } else if (filter2 == null) {
                return filter1
            }
            return AndDependencyFilter(filter1, filter2)
        }
    }
}

class DependencyManagerTest @Inject constructor(
    val dependencyManager: DependencyManager,
    compilerFactory: BuildFileCompiler.IFactory
) : BaseTest(compilerFactory) {

    private fun assertContains(dependencies: List<IClasspathDependency>, vararg ids: String) {
        ids.forEach { id ->
            if (!dependencies.any { it.id.contains(id) }) {
                throw AssertionError("Couldn't find $id in $dependencies")
            }
        }
    }

    private fun assertDoesNotContain(dependencies: List<IClasspathDependency>, vararg ids: String) {
        ids.forEach { id ->
            if (dependencies.any { it.id.contains(id) }) {
                throw AssertionError("$id should not be found in $dependencies")
            }
        }
    }

    @Test
    fun createId() {
        // Caused a StackOverflowException in 0.923
        val id = dependencyManager.createMaven("com.beust.kobalt:kobalt-line-count:jar:(0,]")
    }

    @Test(description = "Make sure that COMPILE scope dependencies get resolved properly")
    fun testScopeDependenciesShouldBeDownloaded() {
        val testDeps = listOf(dependencyManager.create("org.testng:testng:6.10"))

        val filter = AndDependencyFilter(Filters.EXCLUDE_OPTIONAL_FILTER, Filters.COMPILE_FILTER)

        // Should only resolve to TestNG and JCommander
        dependencyManager.transitiveClosure(testDeps, filter).let { dependencies ->
            assertThat(dependencies.any { it.id.contains(":jcommander:") })
            assertContains(dependencies, ":testng:")
        }

        // Should resolve to TestNG and its dependencies
        dependencyManager.transitiveClosure(testDeps).let { dependencies ->
            assertContains(dependencies, ":jcommander:")
            assertContains(dependencies, ":testng:")

            // Optional dependencies
            assertDoesNotContain(dependencies, ":bsh:")
            assertDoesNotContain(dependencies, ":ant:")
            assertDoesNotContain(dependencies, ":ant-launcher:")
        }
    }

    @Test
    fun honorRuntimeDependenciesBetweenProjects() {
        val project2 = findDependentProject()
        val dependencies = dependencyManager.calculateDependencies(project2, Kobalt.context!!,
                Filters.EXCLUDE_OPTIONAL_FILTER)
        assertContains(dependencies, ":klaxon:")
        assertContains(dependencies, ":guice:")
        assertContains(dependencies, ":guava:")
        assertDoesNotContain(dependencies, ":junit:")
    }

    @Test
    fun honorRuntimeDependenciesBetweenProjects2() {
        val project2 = findDependentProject()

        Kobalt.context!!.let { context ->
            dependencyManager.calculateDependencies(project2, context,
                    scopes = listOf(Scope.COMPILE)).let { dependencies ->
                assertContains(dependencies, ":klaxon:jar:0.27")
                assertContains(dependencies, ":guice:")
                assertDoesNotContain(dependencies, ":jcommander:")
                assertDoesNotContain(dependencies, ":junit:")
            }

            dependencyManager.calculateDependencies(project2, context,
                    scopes = listOf(Scope.RUNTIME)).let { dependencies ->
                assertContains(dependencies, ":jcommander:")
                assertDoesNotContain(dependencies, ":klaxon:jar:0.27")
                assertDoesNotContain(dependencies, ":guice:")
                assertDoesNotContain(dependencies, ":junit:")
            }

            dependencyManager.calculateDependencies(project2, context,
                    scopes = listOf(Scope.COMPILE, Scope.RUNTIME)).let { dependencies ->
                assertContains(dependencies, ":klaxon:")
                assertContains(dependencies, ":jcommander:")
                assertContains(dependencies, ":guice:")
                assertDoesNotContain(dependencies, ":junit:")
            }

        }
    }

    private fun findDependentProject(): Project {
        val projectDirectory = createTemporaryProjectDirectory()
        val sharedBuildFile = """
            import com.beust.kobalt.*

            val lib2 = project {
                name = "lib2"
                directory = "$projectDirectory"
                dependencies {
                    // pick dependencies that don't have dependencies themselves, to avoid interferences
                    compile("com.beust:klaxon:0.27",
                        "com.google.inject:guice:4.0")
                    runtime("com.beust:jcommander:1.48")
                    compileOptional("junit:junit:4.12")
                }
            }

            val p = project(lib2) {
                name = "transitive2"
            }
        """
        Kobalt.context = null
        return compileBuildFile(projectDirectory, sharedBuildFile).projects.first { it.name == "transitive2" }
    }
}

