package com.github.soundpod.service

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.media3.common.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Saves a downloaded song as a real .mp3 in the public Downloads folder. */
object Mp3Exporter {

    private fun safeName(raw: String): String {
        val cleaned = raw
            .replace(Regex("[\\\\/:*?\"<>|]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trimEnd('.', ' ')
        return cleaned.ifBlank { "SoundPod" }.take(120)
    }

    /**
     * Converts [sourceFile] (Opus/WebM or any ffmpeg-readable audio) to MP3 and
     * publishes it to Download/SoundPod so it shows in the phone's Music/Downloads.
     * Returns the display file name, or null on failure.
     */
    suspend fun exportToDownloads(
        context: Context,
        sourceFile: File,
        title: String,
        artist: String?
    ): String? = withContext(Dispatchers.IO) {
        if (!sourceFile.exists() || sourceFile.length() == 0L) return@withContext null

        val base = safeName(buildString {
            append(title)
            if (!artist.isNullOrBlank()) append(" - ").append(artist)
        })
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val fileName = "$base.mp3"

        // Convert in private storage first, then publish via MediaStore.
        val tmpDir = File(context.cacheDir, "mp3_export").apply { mkdirs() }
        val tmpMp3 = File(tmpDir, "$stamp-$fileName")
        try {
            if (tmpMp3.exists()) tmpMp3.delete()
            val session = FFmpegKit.executeWithArguments(
                arrayOf(
                    "-y", "-i", sourceFile.absolutePath,
                    "-vn", "-ac", "2", "-ar", "44100", "-b:a", "192k",
                    "-c:a", "libmp3lame",
                    "-metadata", "title=$title",
                    "-metadata", "artist=${artist ?: ""}",
                    tmpMp3.absolutePath
                )
            )
            if (!ReturnCode.isSuccess(session.returnCode) || !tmpMp3.exists() || tmpMp3.length() == 0L) {
                Log.w("SoundPod-MP3", "FFmpeg conversion failed: ${session.returnCode}")
                return@withContext null
            }

            val published = publishToDownloads(context, tmpMp3, fileName)
            if (published != null) {
                // Make it appear right away in Music/Downloads apps.
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(published.path),
                    arrayOf("audio/mpeg"),
                    null
                )
            }
            published?.name
        } catch (e: Exception) {
            Log.e("SoundPod-MP3", "MP3 export failed", e)
            null
        } finally {
            runCatching { tmpMp3.delete() }
        }
    }

    private fun publishToDownloads(context: Context, tmpMp3: File, fileName: String): File? {
        // Android 10+: MediaStore into Download/SoundPod (no permission needed).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/SoundPod")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            var uri: android.net.Uri? = null
            return try {
                uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.let { u ->
                    resolver.openOutputStream(u)?.use { out -> tmpMp3.inputStream().use { it.copyTo(out) } }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(u, values, null, null)
                    // Return a File handle for the scanner; path is the public one.
                    File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/SoundPod/$fileName")
                }
            } catch (e: Exception) {
                Log.e("SoundPod-MP3", "MediaStore publish failed", e)
                uri?.let { runCatching { resolver.delete(it, null, null) } }
                null
            }
        }
        // Android 9 and older: direct file in Download/SoundPod.
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "SoundPod"
        ).apply { mkdirs() }
        val dest = File(dir, fileName)
        return try {
            tmpMp3.copyTo(dest, overwrite = true)
            dest
        } catch (e: Exception) {
            Log.e("SoundPod-MP3", "Legacy publish failed", e)
            null
        }
    }
}
