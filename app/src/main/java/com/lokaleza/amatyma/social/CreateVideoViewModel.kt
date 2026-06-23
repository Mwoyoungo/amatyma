package com.lokaleza.amatyma.social

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class CreatePhase { IDLE, UPLOADING, PROCESSING, ERROR }

/**
 * Drives the Create screen: 30–60s validation → createFastpixUpload (callable)
 * → push bytes via [VideoUploader] → FastPix webhook flips the post to "ready".
 */
class CreateVideoViewModel(app: Application) : AndroidViewModel(app) {

    var selectedUri by mutableStateOf<Uri?>(null); private set
    var durationSec by mutableStateOf(0); private set
    var durationError by mutableStateOf<String?>(null); private set

    var caption by mutableStateOf("")
    var topic by mutableStateOf("Wellness")

    var phase by mutableStateOf(CreatePhase.IDLE); private set
    var progress by mutableStateOf(0f); private set
    var message by mutableStateOf<String?>(null); private set

    private val uploader = VideoUploaders.get(app)

    val canPost: Boolean
        get() = selectedUri != null && durationError == null && phase != CreatePhase.UPLOADING

    fun onVideoPicked(uri: Uri) {
        val seconds = videoDurationSeconds(uri)
        selectedUri = uri
        durationSec = seconds
        message = null
        phase = CreatePhase.IDLE
        durationError = when {
            seconds <= 0 -> "Couldn't read that video — try another."
            seconds < 30 || seconds > 60 -> "Video must be 30–60 seconds (this one is ${seconds}s)."
            else -> null
        }
    }

    fun post(onPosted: () -> Unit) {
        val uri = selectedUri ?: return
        if (durationError != null) return

        if (!uploader.isConfigured) {
            phase = CreatePhase.ERROR
            message = "Video upload turns on once the FastPix SDK token is added."
            return
        }

        phase = CreatePhase.UPLOADING
        progress = 0f
        message = "Starting upload…"

        val payload = hashMapOf<String, Any>("caption" to caption.trim(), "topic" to topic)
        FirebaseFunctions.getInstance()
            .getHttpsCallable("createFastpixUpload")
            .call(payload)
            .addOnSuccessListener { result ->
                val data = result.getData() as? Map<*, *>
                val signedUrl = data?.get("signedUrl") as? String
                if (signedUrl.isNullOrEmpty()) {
                    phase = CreatePhase.ERROR
                    message = "Couldn't start the upload. Try again."
                    return@addOnSuccessListener
                }
                viewModelScope.launch {
                    val file = withContext(Dispatchers.IO) { copyToCache(uri) }
                    uploader.upload(
                        file = file,
                        signedUrl = signedUrl,
                        onProgress = { p -> progress = (p / 100.0).toFloat() },
                        onSuccess = {
                            phase = CreatePhase.PROCESSING
                            message = "Processing your video…"
                            onPosted()
                        },
                        onError = { e ->
                            phase = CreatePhase.ERROR
                            message = e
                        },
                    )
                }
            }
            .addOnFailureListener { e ->
                phase = CreatePhase.ERROR
                message = e.message ?: "Upload failed. Try again."
            }
    }

    private fun videoDurationSeconds(uri: Uri): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(getApplication(), uri)
            val ms = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            (ms / 1000).toInt()
        } catch (e: Exception) {
            0
        } finally {
            retriever.release()
        }
    }

    private fun copyToCache(uri: Uri): File {
        val ctx = getApplication<Application>()
        val file = File(ctx.cacheDir, "upload_${System.currentTimeMillis()}.mp4")
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        return file
    }
}
