package com.github.soundpod.customart

import android.content.Context
import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * A user-chosen artwork image (a photo from the device gallery) attached to a
 * stable item key:
 *
 *  - a song (YouTube Music video id, or a `content://` uri for local files)
 *  - an album browse id (`MPREb_...` / `VL...`)
 *  - an artist browse id (`UC...`)
 *
 * The image itself is copied into [Context.filesDir]/custom_artwork so it keeps
 * working after the original gallery photo moves or is deleted. Only the file
 * name is stored in the database.
 */
@Entity(tableName = "CustomArtwork")
data class CustomArtwork(
    @PrimaryKey val itemKey: String,
    val fileName: String,
    val updatedAt: Long = System.currentTimeMillis()
)

object CustomArtworkStore {
    private const val DIR = "custom_artwork"

    fun dir(context: Context): File =
        File(context.filesDir, DIR).apply { mkdirs() }

    fun fileFor(context: Context, fileName: String): File = File(dir(context), fileName)

    /**
     * Copies the picked image into private storage and returns its file name.
     * Returns null if the image cannot be read.
     */
    fun copyPickedImage(context: Context, uri: Uri, itemKey: String): String? {
        val extension = runCatching {
            context.contentResolver.getType(uri)?.substringAfterLast('/')?.lowercase()
        }.getOrNull()?.takeIf { it.matches(Regex("[a-z0-9]{1,5}")) } ?: "jpg"
        val safeKey = itemKey.replace(Regex("[^A-Za-z0-9_.-]"), "_").take(80)
        val fileName = "${safeKey}_${UUID.randomUUID().toString().take(8)}.$extension"
        val target = File(dir(context), fileName)
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            } ?: return null
            fileName
        } catch (_: Exception) {
            target.delete()
            null
        }
    }

    fun delete(context: Context, fileName: String) {
        runCatching { fileFor(context, fileName).delete() }
    }
}
