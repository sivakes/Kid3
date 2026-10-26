package engine

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.PreviewRequest
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/** Talks to the Python engine over local HTTP. */
class EngineClient(port: Int = 8765) {
    private val base = "http://127.0.0.1:$port"
    private val http = HttpClient.newHttpClient()
    private val json = Json { encodeDefaults = true }

    fun isHealthy(): Boolean = try {
        val request = HttpRequest.newBuilder(URI("$base/health"))
            .timeout(Duration.ofSeconds(1)).GET().build()
        http.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() == 200
    } catch (e: Exception) {
        false
    }

    /** Returns JPEG bytes of the edited preview. */
    fun preview(req: PreviewRequest): ByteArray {
        val request = HttpRequest.newBuilder(URI("$base/image/preview"))
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.encodeToString(req)))
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofByteArray())
        if (response.statusCode() != 200) {
            error("Engine error ${response.statusCode()}: ${String(response.body())}")
        }
        return response.body()
    }

    fun shutdown() {
        val request = HttpRequest.newBuilder(URI("$base/shutdown"))
            .timeout(Duration.ofSeconds(2))
            .POST(HttpRequest.BodyPublishers.noBody()).build()
        http.send(request, HttpResponse.BodyHandlers.discarding())
    }
}
