package com.beust.kobalt

import com.beust.jcommander.JCommander
import com.beust.kobalt.api.IClasspathDependency
import com.beust.kobalt.api.Kobalt
import com.beust.kobalt.app.MainModule
import com.beust.kobalt.app.UpdateKobalt
import com.beust.kobalt.app.remote.KobaltClient
import com.beust.kobalt.internal.KobaltSettings
import com.beust.kobalt.internal.PluginInfo
import com.beust.kobalt.maven.DependencyManager
import com.beust.kobalt.maven.Http
import com.beust.kobalt.maven.dependency.FileDependency
import com.beust.kobalt.misc.*
import java.io.File
import java.net.URLClassLoader
import javax.inject.Inject

fun main(argv: Array<String>) {
    val result = Main.mainNoExit(argv)
    if (result != 0) {
        System.exit(result)
    }
}

class Main @Inject constructor(
        val plugins: Plugins,
        val http: Http,
        val files: KFiles,
        val executors: KobaltExecutors,
        val dependencyManager: DependencyManager,
        val github: GithubApi2,
        val updateKobalt: UpdateKobalt,
        val client: KobaltClient,
        val pluginInfo: PluginInfo,
        val options: Options) {

    companion object {
        fun mainNoExit(argv: Array<String>): Int {
            val (jc, args) = parseArgs(argv)
            if (args.usage) {
                jc.usage()
                return 0
            }
            if (args.version) {
                println("Kobalt ${Kobalt.version}")
                return 0
            }
            Kobalt.init(MainModule(args, KobaltSettings.readSettingsXml()))
            val result = launchMain(Kobalt.INJECTOR.getInstance(Main::class.java), jc, args, argv)
            return result
        }

        private fun parseArgs(argv: Array<String>): Main.RunInfo {
            val args = Args()
            val result = JCommander(args)
            result.parse(*argv)
            KobaltLogger.setLogLevel(args)
            return Main.RunInfo(result, args)
        }

        /**
         * Entry point for tests, which can instantiate their main object with their own module and injector.
         */
        fun launchMain(main: Main, jc: JCommander, args: Args, argv: Array<String>) : Int {
            return main.run {
                val runResult = run(jc, args, argv)
                pluginInfo.cleanUp()
                executors.shutdown()
                runResult
            }
        }
    }

    data class RunInfo(val jc: JCommander, val args: Args)

    private fun installCommandLinePlugins(args: Args): ClassLoader {
        var pluginClassLoader = javaClass.classLoader
        val dependencies = arrayListOf<IClasspathDependency>()
        args.pluginIds?.let {
            // We want this call to go to the network if no version was specified, so set localFirst to false
            dependencies.addAll(it.split(',').map { dependencyManager.create(it) })
        }
        args.pluginJarFiles?.let {
            dependencies.addAll(it.split(',').map { FileDependency(it) })
        }
        if (dependencies.size > 0) {
            val urls = dependencies.map { it.jarFile.get().toURI().toURL() }
            pluginClassLoader = URLClassLoader(urls.toTypedArray())
            plugins.installPlugins(dependencies, pluginClassLoader)
        }

        return pluginClassLoader
    }

    fun run(jc: JCommander, args: Args, argv: Array<String>): Int {

        //
        // Install plug-ins requested from the command line
        //
        installCommandLinePlugins(args)

        if (args.client) {
            client.run()
            return 0
        }

        var result = 1

        val latestVersionFuture = github.latestKobaltVersion

        try {
            result = runWithArgs(jc, args, argv)
        } catch(ex: Throwable) {
            error("", ex.cause ?: ex)
        }

        if (!args.update) {
            updateKobalt.checkForNewVersion(latestVersionFuture)
        }
        return result
    }

    private fun runWithArgs(jc: JCommander, args: Args, argv: Array<String>): Int {
        val p = if (args.buildFile != null) File(args.buildFile) else File(".")
        args.buildFile = p.absolutePath
        
        if (!args.update) {
            kobaltLog(1, AsciiArt.banner + Kobalt.version + "\n")
        }

        return options.run(jc, args, argv)
    }


}
