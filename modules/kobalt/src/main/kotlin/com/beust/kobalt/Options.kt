package com.beust.kobalt

import com.beust.jcommander.JCommander
import com.beust.kobalt.api.ITask
import com.beust.kobalt.api.Kobalt
import com.beust.kobalt.api.KobaltContext
import com.beust.kobalt.api.Project
import com.beust.kobalt.app.ProjectFinder
import com.beust.kobalt.app.ProjectGenerator
import com.beust.kobalt.app.Templates
import com.beust.kobalt.app.UpdateKobalt
import com.beust.kobalt.app.remote.KobaltServer
import com.beust.kobalt.internal.PluginInfo
import com.beust.kobalt.internal.TaskManager
import com.beust.kobalt.internal.build.BuildSources
import com.beust.kobalt.internal.build.SingleFileBuildSources
import com.beust.kobalt.misc.CheckVersions
import com.beust.kobalt.misc.kobaltLog
import com.beust.kobalt.wrapper.Main
import com.google.common.collect.HashMultimap
import com.google.inject.Inject
import java.io.File

/**
 * Some options require a build file, others shouldn't have one and some don't care. This
 * class captures these requirements.
 */
open class Option(open val enabled: () -> Boolean, open val action: () -> Unit,
        open val requireBuildFile: Boolean = true)
class OptionalBuildOption(override val enabled: () -> Boolean, override val action: () -> Unit)
    : Option(enabled, action, false)

class Options @Inject constructor(
        val plugins: Plugins,
        val checkVersions: CheckVersions,
        val projectGenerator: ProjectGenerator,
        val pluginInfo: PluginInfo,
        val serverFactory: KobaltServer.IFactory,
        val updateKobalt: UpdateKobalt,
        val projectFinder: ProjectFinder,
        val taskManager: TaskManager,
        val resolveDependency: ResolveDependency
        ) {

    fun run(jc: JCommander, args: Args, argv: Array<String>): Int {
        val p = if (args.buildFile != null) File(args.buildFile) else File(".")
//        val buildFile = BuildFile(Paths.get(p.absolutePath), p.name)
        val buildSources = if (p.isDirectory) BuildSources(p.absoluteFile) else SingleFileBuildSources(p)
        val pluginClassLoader = javaClass.classLoader

        //
        // Attempt to parse the build file in order to correctly set up repos, plug-ins, etc...
        // If the build file can't be parsed, don't give up just yet since some options don't need
        // a correct build file to work.
        //
        var buildError: Throwable? = null
        val allProjects =
                try {
                    projectFinder.initForBuildFile(buildSources, args).projects
                } catch(ex: Exception) {
                    buildError = ex
                    listOf<Project>()
                }

        fun runIfSuccessfulBuild(buildError: Throwable?, action: () -> Unit) {
            buildError?.let { throw it }
            action()
        }

        // Modify `args` with options found in buildScript { kobaltOptions(...) }, if any
        addOptionsFromBuild(args, Kobalt.optionsFromBuild)

        val options = listOf<Option>(
                OptionalBuildOption( { -> args.templates != null }, {
                    //
                    // --init: create a new build project and install the wrapper
                    // Make sure the wrapper won't call us back with --noLaunch
                    //
                    projectGenerator.run(args, pluginClassLoader)
                    // The wrapper has to call System.exit() in order to set the exit code,
                    // so make sure we call it last (or possibly launch it in a separate JVM).
                    Main.main(arrayOf("--noLaunch") + argv)
                }),
                OptionalBuildOption( { -> args.usage }, { jc.usage() }),
                OptionalBuildOption( { -> args.update }, {
                    /* --update*/
                    updateKobalt.updateKobalt() }),
                OptionalBuildOption( { -> args.serverMode }, {
                    // --server
                    val port = serverFactory.create(args.force, args.port, { cleanUp() }).call()
                }),
                OptionalBuildOption( { -> args.listTemplates}, {
                    // --listTemplates
                    Templates().displayTemplates(pluginInfo)
                }),
                Option( { -> args.projectInfo }, {
                    // --projectInfo
                    runIfSuccessfulBuild(buildError) {
                        allProjects.forEach {
                            it.compileDependencies.filter { it.isMaven }.forEach {
                                resolveDependency.run(it.id)
                            }
                        }
                    }
                }),
                Option( { args.dependency != null }, {
                    // --resolve
                    args.dependency?.let { resolveDependency.run(it) }
                }),
                Option( { args.tasks }, {
                    // --tasks
                    runIfSuccessfulBuild(buildError) {
                        displayTasks(allProjects, Kobalt.context!!)
                    }
                }),
                Option( { args.checkVersions }, {
                    // --checkVersions
                    runIfSuccessfulBuild(buildError) {
                        checkVersions.run(allProjects)
                    }
                }),
                Option( { args.download }, {
                    // --download
                    updateKobalt.downloadKobalt()
                })
        )

        var processedOption = false
        options.forEach {
            if (it.enabled()) {
                if ((it.requireBuildFile && buildSources.exists()) || ! it.requireBuildFile) {
                    it.action()
                    processedOption = true
                } else if (it.requireBuildFile && ! buildSources.exists()) {
                    throw KobaltException("Couldn't find a build file")
                }
            }
        }

        var result = 0
        if (! processedOption) {
            //
            // Launch the build
            //
            if (! buildSources.exists()) {
                throw KobaltException("Could not find build file: " + buildSources)
            }
            runIfSuccessfulBuild(buildError) {
                val runTargetResult = taskManager.runTargets(args.targets, allProjects)
                if (result == 0) {
                    result = if (runTargetResult.taskResult.success) 0 else 1
                }

                // Shutdown all plug-ins
                plugins.shutdownPlugins()

                // Run the build report contributors
                pluginInfo.buildReportContributors.forEach {
                    it.generateReport(Kobalt.context!!)
                }
            }
        }
        return result
    }

    private fun cleanUp() {
        pluginInfo.cleanUp()
        taskManager.cleanUp()
        Kobalt.cleanUp()
    }

    private fun addOptionsFromBuild(args: Args, optionsFromBuild: ArrayList<String>) {
        optionsFromBuild.forEach {
            when(it) {
                Args.SEQUENTIAL -> args.sequential = true
                else -> throw IllegalArgumentException("Unsupported option found in kobaltOptions(): " + it)
            }
        }
    }

    private fun displayTasks(projects: List<Project>, context: KobaltContext) {
        //
        // List of tasks, --tasks
        //
        val tasksByPlugins = HashMultimap.create<String, ITask>()
        projects.forEach { project ->
            pluginInfo.taskContributors.forEach {
                val tasks = it.tasksFor(project, context)
                tasks.forEach {
                    tasksByPlugins.put(it.plugin.name, it)
                }
            }
        }
        listOf(taskManager.annotationTasks, taskManager.dynamicTasks).forEach { tasks ->
            tasks.forEach {
                tasksByPlugins.put(it.plugin.name, it)
            }
        }
        val sb = StringBuffer("List of tasks\n")
        tasksByPlugins.keySet().forEach { name ->
            sb.append("\n  " + AsciiArt.horizontalDoubleLine + " $name "
                    + AsciiArt.horizontalDoubleLine + "\n")
            tasksByPlugins[name].distinctBy(ITask::name).sortedBy(ITask::name).forEach { task ->
                sb.append("    ${task.name}\t\t${task.doc}\n")
            }
        }

        kobaltLog(1, sb.toString())
    }
}