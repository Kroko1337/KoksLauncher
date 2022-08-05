package software.schummel.launcher

import com.sun.javafx.tk.Toolkit
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import javax.swing.JOptionPane
import kotlin.system.exitProcess


var process: Process? = null

fun main(args: Array<String>) {
    val versions = ArrayList<String>()
    Version.values().forEach {
        versions.add(it.display)
    }
    val array = versions.toTypedArray()
    val versionName = JOptionPane.showInputDialog(
        null,
        "Which version do you want to start?",
        "Choose version",
        JOptionPane.QUESTION_MESSAGE,
        null,
        array,
        array[0]
    ) as String
    Version.values().filter { it.display == versionName }.forEach {
        run(it)
    }

    Toolkit.getToolkit().addShutdownHook {
        process?.destroy()
    }
}

fun run(version: Version) {
    val mcDir = File(System.getenv("APPDATA"), ".minecraft")
    val binDir = File(mcDir, "bin")
    val launcherDir = File(mcDir, "koks-launcher")
    if (!launcherDir.exists())
        launcherDir.mkdir()
    val dll = File(launcherDir, "natives")
    if(!dll.exists()) {
        dll.mkdir();
        val input = Version::class.java.getResourceAsStream("/natives") ?: throw RuntimeException("Cant find natives")
        val reader = BufferedReader(InputStreamReader(input))
        reader.lines().forEach { Version::class.java.getResourceAsStream("/natives/$it")
            ?.let { it1 -> Files.copy(it1, File(dll, it).toPath(), StandardCopyOption.REPLACE_EXISTING) } }

    }

    val libsDir = File(launcherDir, "libs")
    if (!libsDir.exists())
        libsDir.mkdir()
    val versionSaving = File(launcherDir, version.name)
    if (!versionSaving.exists())
        versionSaving.mkdirs()

    val clientJar = File(versionSaving, "client.dll")

    if (!clientJar.exists()) {
        copyClientToLocation(version, clientJar.toPath())
    }

    val libs = libsDir.listFiles()?.filter { it.extension == "jar" }?.joinToString(";") { it.absolutePath }
        .plus(clientJar.absolutePath)

    val command =
        "\"java\" -XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump -Djava.library.path=\"${dll.absolutePath}\" -classpath \"$libs\" -Xmx4G -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:+DisableAttachMechanism -noverify net.minecraft.client.main.Main --version \"Koks\" --accessToken \"0\" --assetIndex \"1.8\" --gameDir \"${versionSaving.absolutePath}\" --assetsDir \"${
            File(
                mcDir,
                "assets"
            ).absolutePath
        }\""
    val processBuilder = ProcessBuilder(command)
    processBuilder.redirectErrorStream(true)
    processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
    process = processBuilder.start()
    process?.waitFor()
}

fun copyClientToLocation(version: Version, path: Path) {
    val input = Version::class.java.getResourceAsStream("/clients/" + version.jar)
    if (input != null) {
        Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING)
    } else {
        println("Cant find client")
        exitProcess(-1)
    }
}

fun lookUp(file: File): File? {
    file.listFiles()?.forEach {
        if (it.isDirectory) {
            val file = lookUp(it)
            if (file != null)
                return file
        } else {
            if (it.name.endsWith(".dll")) {
                return file
            }
        }
    }
    return null
}

enum class Version(val display: String, val jar: String) {
    V1("v1.0.0", "Koks1.0.0.dll"), V3("v3.0.0", "Koks3.0.0.dll")
}