package engine

import java.io.File

/** Starts the Python engine as a background process and stops it when the app closes. */
class EngineLauncher(private val port: Int = 8765) {
    private val client = EngineClient(port)
    private var process: Process? = null
    private val isWindows = System.getProperty("os.name").lowercase().contains("win")

    fun start() {
        // If the engine is already running (for example you started it manually), reuse it.
        if (client.isHealthy()) return

        val started = bundledEngine()?.let { launchBundled(it) } ?: launchDev()
        process = started

        repeat(120) {
            if (client.isHealthy()) return
            if (!started.isAlive) error("The engine stopped unexpectedly. Please reinstall ChromaCut.")
            Thread.sleep(500)
        }
        error("The engine did not start within 60 seconds.")
    }

    /** The packaged engine that ships inside the installed app, or null in development. */
    private fun bundledEngine(): File? {
        val resources = System.getProperty("compose.application.resources.dir") ?: return null
        val name = if (isWindows) "chromacut-engine.exe" else "chromacut-engine"
        val exe = File(resources, "chromacut-engine/$name")
        return exe.takeIf { it.exists() }
    }

    private fun launchBundled(exe: File): Process {
        exe.setExecutable(true)
        return ProcessBuilder(exe.absolutePath, port.toString())
            .directory(exe.parentFile)
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start()
    }

    /** Development mode: run the engine from the engine/ folder using its virtual environment. */
    private fun launchDev(): Process {
        val engineDir = File(System.getenv("CHROMACUT_ENGINE_DIR") ?: "../engine")
            .absoluteFile.normalize()
        val venvPython = if (isWindows) File(engineDir, ".venv/Scripts/python.exe")
        else File(engineDir, ".venv/bin/python")
        val python = if (venvPython.exists()) venvPython.absolutePath
        else if (isWindows) "python" else "python3"

        return ProcessBuilder(
            python, "-m", "uvicorn", "chromacut_engine.main:app",
            "--host", "127.0.0.1", "--port", port.toString(),
        )
            .directory(engineDir)
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .start()
    }

    fun stop() {
        if (process == null) return // we did not start it, so we do not stop it
        runCatching { client.shutdown() }
        process?.destroy()
    }
}
