package engine

import java.io.File

/** Starts the Python engine as a background process and stops it when the app closes. */
class EngineLauncher(private val port: Int = 8765) {
    private val client = EngineClient(port)
    private var process: Process? = null

    fun start() {
        // If you already started the engine manually, just reuse it.
        if (client.isHealthy()) return

        val engineDir = File(System.getenv("CHROMACUT_ENGINE_DIR") ?: "../engine")
            .absoluteFile.normalize()
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val venvPython = if (isWindows) File(engineDir, ".venv/Scripts/python.exe")
        else File(engineDir, ".venv/bin/python")
        val python = if (venvPython.exists()) venvPython.absolutePath
        else if (isWindows) "python" else "python3"

        val started = ProcessBuilder(
            python, "-m", "uvicorn", "chromacut_engine.main:app",
            "--host", "127.0.0.1", "--port", port.toString(),
        )
            .directory(engineDir)
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .start()
        process = started

        repeat(60) {
            if (client.isHealthy()) return
            if (!started.isAlive) error("The Python engine stopped unexpectedly. Check the console output.")
            Thread.sleep(500)
        }
        error("The Python engine did not start within 30 seconds.")
    }

    fun stop() {
        if (process == null) return // we did not start it, so we do not stop it
        runCatching { client.shutdown() }
        process?.destroy()
    }
}
