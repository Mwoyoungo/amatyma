package com.lokaleza.amatyma.social

import android.os.Handler
import android.os.Looper
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * SDK-free video uploader.
 *
 * The signed URL `createFastpixUpload` returns is a GCS resumable session URI,
 * so we just PUT the file straight to it — no vendor SDK (the FastPix Android
 * SDK 2.0.0 transitively drags React Native into the app), no GitHub Packages.
 * All of FastPix's real value (transcode → HLS, thumbnails, webhooks) is
 * server-side and unchanged.
 *
 * Callbacks are marshalled back to the main thread so Compose state updates
 * happen safely.
 */
class GcsResumableUploader : VideoUploader {

    override val isConfigured = true
    private val main = Handler(Looper.getMainLooper())

    override fun upload(
        file: File,
        signedUrl: String,
        onProgress: (Double) -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        Thread {
            var conn: HttpURLConnection? = null
            try {
                val total = file.length()
                conn = (URL(signedUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    doOutput = true
                    connectTimeout = 30_000
                    readTimeout = 120_000
                    setRequestProperty("Content-Type", "video/mp4")
                    setRequestProperty("Content-Range", "bytes 0-${total - 1}/$total")
                    setFixedLengthStreamingMode(total)
                }

                file.inputStream().use { input ->
                    conn.outputStream.use { output ->
                        val buffer = ByteArray(256 * 1024)
                        var uploaded = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            uploaded += read
                            val pct = if (total > 0) (uploaded.toDouble() / total) * 100.0 else 0.0
                            main.post { onProgress(pct) }
                        }
                        output.flush()
                    }
                }

                val code = conn.responseCode
                if (code in 200..299) {
                    main.post { onSuccess() }
                } else {
                    main.post { onError("Upload failed (HTTP $code)") }
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Upload error"
                main.post { onError(msg) }
            } finally {
                conn?.disconnect()
            }
        }.start()
    }
}
