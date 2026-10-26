package ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import engine.EngineClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import model.Adjustments
import model.Grade
import model.PreviewRequest
import org.jetbrains.skia.Image as SkiaImage
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun EditorScreen(client: EngineClient) {
    var path by remember { mutableStateOf<String?>(null) }
    var adj by remember { mutableStateOf(Adjustments()) }
    var grade by remember { mutableStateOf(Grade()) }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    var status by remember { mutableStateOf("Open a photo to begin") }

    // Re-render whenever the photo or any edit changes. The short delay
    // means dragging a slider only sends the final value, not every step.
    LaunchedEffect(path, adj, grade) {
        val p = path ?: return@LaunchedEffect
        delay(120)
        status = "Rendering..."
        try {
            val bytes = withContext(Dispatchers.IO) {
                client.preview(PreviewRequest(path = p, adjustments = adj, grade = grade))
            }
            preview = SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
            status = File(p).name
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            status = "Error: ${e.message}"
        }
    }

    Row(Modifier.fillMaxSize()) {
        // ----- Image viewer -----
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Box(
                Modifier.weight(1f).fillMaxWidth().background(Color(0xFF111111)),
                contentAlignment = Alignment.Center,
            ) {
                val image = preview
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = "Photo preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                    )
                } else {
                    Text("No photo open")
                }
            }
            Text(status, Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
        }

        // ----- Edit panel -----
        Column(
            Modifier.width(340.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    pickFile("Open photo")?.let {
                        path = it
                        adj = Adjustments()
                        grade = Grade()
                    }
                }) { Text("Open photo") }

                OutlinedButton(onClick = {
                    adj = Adjustments()
                    grade = Grade()
                }) { Text("Reset") }
            }

            Section("Basic")
            SliderRow("Exposure", adj.exposure, -3f..3f) { adj = adj.copy(exposure = it) }
            SliderRow("Temperature", adj.temperature, -1f..1f) { adj = adj.copy(temperature = it) }
            SliderRow("Tint", adj.tint, -1f..1f) { adj = adj.copy(tint = it) }
            SliderRow("Contrast", adj.contrast, -1f..1f) { adj = adj.copy(contrast = it) }
            SliderRow("Saturation", adj.saturation, -1f..1f) { adj = adj.copy(saturation = it) }

            Section("Color wheels")
            WheelGroup("Lift (shadows)", grade.lift, -0.5f..0.5f) { grade = grade.copy(lift = it) }
            WheelGroup("Gamma (midtones)", grade.gamma, -1f..1f) { grade = grade.copy(gamma = it) }
            WheelGroup("Gain (highlights)", grade.gain, -1f..1f) { grade = grade.copy(gain = it) }

            Section("LUT")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    pickFile("Load .cube LUT")?.let { grade = grade.copy(lut_path = it) }
                }) { Text("Load LUT") }
                if (grade.lut_path != null) {
                    TextButton(onClick = { grade = grade.copy(lut_path = null) }) { Text("Remove") }
                }
            }
            Text(
                grade.lut_path?.let { File(it).name } ?: "No LUT loaded",
                style = MaterialTheme.typography.bodySmall,
            )
            if (grade.lut_path != null) {
                SliderRow("Intensity", grade.lut_intensity, 0f..1f) { grade = grade.copy(lut_intensity = it) }
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(12.dp))
    Text(title, style = MaterialTheme.typography.titleMedium)
    HorizontalDivider()
}

@Composable
private fun SliderRow(
    label: String,
    value: Double,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Double) -> Unit,
) {
    Column {
        Row {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text("%.2f".format(value), style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toDouble()) },
            valueRange = range,
        )
    }
}

@Composable
private fun WheelGroup(
    title: String,
    values: List<Double>,
    range: ClosedFloatingPointRange<Float>,
    onChange: (List<Double>) -> Unit,
) {
    Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
    listOf("Red", "Green", "Blue").forEachIndexed { i, channel ->
        SliderRow(channel, values[i], range) { v ->
            onChange(values.toMutableList().also { it[i] = v })
        }
    }
}

private fun pickFile(title: String): String? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.isVisible = true
    val name = dialog.file ?: return null
    return File(dialog.directory, name).absolutePath
}
