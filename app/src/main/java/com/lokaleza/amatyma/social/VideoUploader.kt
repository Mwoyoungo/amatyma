package com.lokaleza.amatyma.social

import android.content.Context
import java.io.File

/**
 * Seam for pushing the local clip to the FastPix signed upload URL.
 *
 * The real implementation uses the FastPix Android upload SDK
 * (`io.fastpix:uploads`, hosted on GitHub Packages — needs a `read:packages`
 * token in Gradle). Until that token + dependency are added, [VideoUploaders.get]
 * returns [PlaceholderUploader] so the rest of the create flow compiles and runs.
 */
interface VideoUploader {
    val isConfigured: Boolean
    fun upload(
        file: File,
        signedUrl: String,
        onProgress: (Double) -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    )
}

object PlaceholderUploader : VideoUploader {
    override val isConfigured = false
    override fun upload(
        file: File,
        signedUrl: String,
        onProgress: (Double) -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        onError("Video upload turns on once the FastPix SDK token is configured.")
    }
}

/**
 * Single place to swap in the real FastPix uploader.
 * When the SDK dependency is added, return `FastPixVideoUploader(context)` here.
 */
object VideoUploaders {
    fun get(context: Context): VideoUploader = GcsResumableUploader()
}
