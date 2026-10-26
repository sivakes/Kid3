import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import engine.EngineClient
import engine.EngineLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ui.EditorScreen

fun main() = application {
    val launcher = remember { EngineLauncher() }
    val client = remember { EngineClient() }
    var ready by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) { launcher.start() }
            ready = true
        } catch (e: Exception) {
            error = e.message
        }
    }

    Window(
        onCloseRequest = {
            launcher.stop()
            exitApplication()
        },
        title = "ChromaCut",
        state = rememberWindowState(width = 1280.dp, height = 800.dp),
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            Surface(Modifier.fillMaxSize()) {
                when {
                    error != null -> Centered("Could not start the engine:\n$error")
                    !ready -> Centered("Starting engine...")
                    else -> EditorScreen(client)
                }
            }
        }
    }
}

@Composable
private fun Centered(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(message) }
}
