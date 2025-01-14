package com.sunnychung.application.multiplatform.hellohttp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import co.touchlab.kermit.MutableLoggerConfig
import co.touchlab.kermit.Severity
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.jayway.jsonpath.spi.json.JsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import com.jayway.jsonpath.spi.mapper.MappingProvider
import com.sunnychung.application.multiplatform.hellohttp.document.OperationalDI
import com.sunnychung.application.multiplatform.hellohttp.document.UserPreferenceDI
import com.sunnychung.application.multiplatform.hellohttp.error.MultipleProcessError
import com.sunnychung.application.multiplatform.hellohttp.helper.InitClasses
import com.sunnychung.application.multiplatform.hellohttp.helper.InitNativeClasses
import com.sunnychung.application.multiplatform.hellohttp.model.UserPreference
import com.sunnychung.application.multiplatform.hellohttp.model.Version
import com.sunnychung.application.multiplatform.hellohttp.model.getApplicableRenderingApiList
import com.sunnychung.application.multiplatform.hellohttp.platform.LinuxOS
import com.sunnychung.application.multiplatform.hellohttp.platform.MacOS
import com.sunnychung.application.multiplatform.hellohttp.platform.WindowsOS
import com.sunnychung.application.multiplatform.hellohttp.platform.currentOS
import com.sunnychung.application.multiplatform.hellohttp.platform.isMacOs
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.AppView
import com.sunnychung.application.multiplatform.hellohttp.ux.DataLossWarningDialogWindow
import io.github.dralletje.ktreesitter.graphql.TreeSitterGraphql
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.harawata.appdirs.AppDirsFactory
import java.awt.Dimension
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    System.setProperty("apple.awt.application.appearance", "system")
//    System.setProperty("skiko.renderApi", "OPENGL") // IllegalArgumentException: "MacOS does not support OPENGL rendering API."
//    System.setProperty("skiko.renderApi", "SOFTWARE")
    if (args.isNotEmpty()) {
        println("args = [${args.joinToString(",") { "'$it'" }}]")
        args.forEach { arg ->
            when {
                arg.startsWith("--logLevel=") -> {
                    val logLevel = when (val l = arg.removePrefix("--logLevel=").trim()) {
                        "verbose" -> Severity.Verbose
                        "debug" -> Severity.Debug
                        "info" -> Severity.Info
                        "warn" -> Severity.Warn
                        "error" -> Severity.Error
                        else -> throw IllegalArgumentException("Unknown log level $l")
                    }
                    (log.config as MutableLoggerConfig).minSeverity = logLevel
                }
                else -> throw IllegalArgumentException("Unknown option $arg")
            }
        }
    }
    val appDir = AppDirsFactory.getInstance().getUserDataDir("Hello HTTP", null, null)
    println("appDir = $appDir")
    AppContext.dataDir = File(appDir)
    runBlocking {
        try {
            AppContext.SingleInstanceProcessService.apply { dataDir = File(appDir) }.enforce()
        } catch (e: MultipleProcessError) {
            application {
                Window(title = "Hello HTTP", onCloseRequest = { exitProcess(1) }) {
                    AlertDialog(
                        onDismissRequest = { exitProcess(1) },
                        text = { Text("Another instance of Hello HTTP is running. Please close that process before starting another one.") },
                        confirmButton = {
                            Text(
                                text = "Close",
                                modifier = Modifier.clickable { exitProcess(1) }.padding(10.dp)
                            )
                        },
                    )
                }
            }
            exitProcess(1)
        }
        loadNativeLibraries()
        println("Preparing to start")

        var dataVersion: Version? = null
        var appVersion: Version? = null
        var userPreference: UserPreference? = null

        coroutineScope {
            withContext(Dispatchers.IO) {
                launch {
                    AppContext.PersistenceManager.initialize()

                    dataVersion =
                        AppContext.OperationalRepository.read(OperationalDI())!!.data.appVersion.let { Version(it) }
                    appVersion = AppContext.MetadataManager.version.let { Version(it) }

                    userPreference = AppContext.UserPreferenceRepository.read(UserPreferenceDI())!!.preference
                }

                launch {
                    AppContext.ResourceManager.loadAllResources()
                }
            }
        }

        val applicableRenderingApis = getApplicableRenderingApiList(currentOS()).toSet()
        userPreference!!.preferredRenderingApi_Experimental?.takeIf { it in applicableRenderingApis }?.value?.let {
            System.setProperty("skiko.renderApi", it)
            println("Set skiko.renderApi = $it")
        }

        val prepareCounter = AtomicInteger(0)

        application {
            var isContinue by remember { mutableStateOf<Boolean?>(null) }
            var isPrepared by remember { mutableStateOf(false) }
            if (isContinue == null) {
                if (dataVersion!! > appVersion!!) {
                    DataLossWarningDialogWindow(
                        dataVersion = dataVersion!!.versionName,
                        appVersion = appVersion!!.versionName
                    ) {
                        isContinue = it
                    }
                } else {
                    isContinue = true
                }
            }
            if (isContinue == false) {
                println("Exit")
                exitApplication()
            } else if (isContinue == true) {
                LaunchedEffect(Unit) {
                    println("Preparing after continue")
                    if (prepareCounter.addAndGet(1) > 1) {
                        throw RuntimeException("Prepare more than once")
                    }

                    AppContext.OperationalRepository.read(OperationalDI())
                        .also {
                            it!!.data.appVersion = appVersion!!.versionName
                            AppContext.OperationalRepository.awaitUpdate(OperationalDI())
                        }
                    AppContext.AutoBackupManager.backupNow()
                    val preference = AppContext.UserPreferenceRepository.read(UserPreferenceDI())!!.preference
                    AppContext.UserPreferenceViewModel.setColorTheme(preference.colourTheme)

                    // json path initialization
                    Configuration.setDefaults(object : Configuration.Defaults {
                        override fun jsonProvider(): JsonProvider = JacksonJsonProvider()
                        override fun options(): MutableSet<Option> = mutableSetOf()
                        override fun mappingProvider(): MappingProvider = JacksonMappingProvider()
                    })

                    Thread {
                        InitNativeClasses()
                        InitClasses()
                    }.start()

                    delay(500L)
                    isPrepared = true
                }

                if (isPrepared) {
                    Window(
                        title = "Hello HTTP",
                        onCloseRequest = ::exitApplication,
                        icon = painterResource("image/appicon.svg"),
                        state = rememberWindowState(width = 1024.dp, height = 560.dp)
                    ) {
                        AppContext.instance.renderingApi = this.window.renderApi.name

                        with(LocalDensity.current) {
                            window.minimumSize = if (isMacOs()) {
                                Dimension(800, 450)
                            } else {
                                Dimension(800.dp.roundToPx(), 450.dp.roundToPx())
                            }
                        }
                        AppView()
                    }
                } else {
                    // dummy window to prevent application exit before the main window is loaded
                    Window(
                        title = "Hello HTTP",
                        icon = painterResource("image/appicon.svg"),
                        visible = false,
                        onCloseRequest = {}
                    ) {}
                }
            }
        }
    }
}

fun loadNativeLibraries() {
    val libraries = listOf("tree-sitter-graphql" to TreeSitterGraphql)
    val systemArch = if (currentOS() == WindowsOS) {
        "x64"
    } else {
        getSystemArchitecture().let {
            when (it.lowercase()) {
                "x86_64" -> "x64"
                else -> it
            }
        }
    }.uppercase()
    libraries.forEach { (name, enclosingClazz) ->
        val libFileName = when (currentOS()) {
            LinuxOS -> "lib${name}-${systemArch}.so"
            MacOS -> "lib${name}-${systemArch}.dylib"
            else -> "${name}-${systemArch}.dll"
        }
        println("Loading native lib $libFileName")
        val dest = File(File(AppContext.dataDir, "lib"), libFileName)
        dest.parentFile.mkdirs()
        try {
            enclosingClazz.javaClass.classLoader.getResourceAsStream(libFileName).use { `is` ->
                `is` ?: throw RuntimeException("Lib $libFileName not found")
                FileOutputStream(dest).use { os ->
                    `is`.copyTo(os)
                }
            }
        } catch (e: IOException) {
            println("Warning: ${dest.path} is not writable. Skip exporting lib. Exception: ${e.message}")
        }
        System.load(dest.absolutePath)
    }
}

fun getSystemArchitecture(): String {
    return exec("uname", "-m").trim()
}

fun exec(vararg components: String): String {
    val pb = ProcessBuilder(*components)
    val process = pb.start()
    val output = process.inputStream.bufferedReader().readText()
    val exitCode = process.waitFor()
    if (exitCode != 0) {
        throw RuntimeException("${components.first()} Process finished with exit code $exitCode")
    }
    return output
}
