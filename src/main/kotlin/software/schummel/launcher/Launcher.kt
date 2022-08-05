package software.schummel.launcher

import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    run(Version.V3)
}

fun run(version: Version) {
    val mcDir = File(System.getenv("APPDATA"), ".minecraft")
    val binDir = File(mcDir, "bin")
    val dll = lookUp(binDir)!!
    val launcherDir = File(mcDir, "koks-launcher")
    if(!launcherDir.exists())
        launcherDir.mkdir()
    val libsDir = File(launcherDir, "libs")
    if(!libsDir.exists())
        libsDir.mkdir()
    val versionSaving = File(launcherDir, version.name)
    if(!versionSaving.exists())
        versionSaving.mkdirs()

    val clientJar = File(versionSaving, "client.dll")

    if(!clientJar.exists()) {
        copyClientToLocation(version, clientJar.toPath())
    }

    val libs = libsDir.listFiles()?.filter { it.extension == "jar" }?.joinToString(";") { it.absolutePath }.plus(clientJar.absolutePath)

    val command = "\"java\" -XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump -Djava.library.path=\"${dll.absolutePath}\" -classpath \"$libs\" -Xmx4G -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:+DisableAttachMechanism -noverify net.minecraft.client.main.Main --version \"Koks\" --accessToken \"0\" --assetIndex \"1.8\" --gameDir \"${versionSaving.absolutePath}\" --assetsDir \"${File(mcDir, "assets").absolutePath}\""
    val processBuilder = ProcessBuilder(command)
    processBuilder.redirectErrorStream(true)
    processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
    val process = processBuilder.start()
    process.waitFor()
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