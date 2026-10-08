package com.neonbear.cave

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import java.io.File

/** Where finished MP3s are put. */
interface OutputTarget {
    fun exists(fileName: String): Boolean
    fun write(fileName: String, src: File): Boolean
}

/** A folder the user picked with the system folder picker. */
class SafOutput(private val ctx: Context, private val dir: DocumentFile) : OutputTarget {
    override fun exists(fileName: String) = dir.findFile(fileName) != null

    override fun write(fileName: String, src: File): Boolean {
        dir.findFile(fileName)?.delete()
        val out = dir.createFile("audio/mpeg", fileName) ?: return false
        val stream = ctx.contentResolver.openOutputStream(out.uri) ?: return false
        stream.use { o -> src.inputStream().use { it.copyTo(o) } }
        return true
    }
}

/** Default on Android 10+: Music/Cave through MediaStore. Needs no permission at all. */
@RequiresApi(29)
class MediaStoreOutput(private val ctx: Context, relativePath: String) : OutputTarget {
    private val rel = relativePath.trimEnd('/') + "/"
    private val collection: Uri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    private fun find(fileName: String): Uri? {
        ctx.contentResolver.query(
            collection,
            arrayOf(MediaStore.Audio.Media._ID),
            "${MediaStore.Audio.Media.DISPLAY_NAME}=? AND ${MediaStore.Audio.Media.RELATIVE_PATH}=?",
            arrayOf(fileName, rel),
            null,
        )?.use { c ->
            if (c.moveToFirst()) return ContentUris.withAppendedId(collection, c.getLong(0))
        }
        return null
    }

    override fun exists(fileName: String) = find(fileName) != null

    override fun write(fileName: String, src: File): Boolean {
        val resolver = ctx.contentResolver
        find(fileName)?.let { resolver.delete(it, null, null) }

        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.RELATIVE_PATH, rel)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: return false
        return try {
            val stream = resolver.openOutputStream(uri) ?: throw java.io.IOException("no stream")
            stream.use { o -> src.inputStream().use { it.copyTo(o) } }
            values.clear()
            values.put(MediaStore.Audio.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            false
        }
    }
}

/** Default on Android 7 to 9: a plain folder in Music/Cave (needs the storage permission). */
class LegacyOutput(private val ctx: Context, private val dir: File) : OutputTarget {
    override fun exists(fileName: String) = File(dir, fileName).exists()

    override fun write(fileName: String, src: File): Boolean {
        return try {
            dir.mkdirs()
            val target = File(dir, fileName)
            src.copyTo(target, overwrite = true)
            MediaScannerConnection.scanFile(ctx, arrayOf(target.absolutePath), arrayOf("audio/mpeg"), null)
            true
        } catch (e: Exception) {
            false
        }
    }
}

object OutputFactory {
    const val DEFAULT_LABEL = "Music/Cave"

    /** The default Music/Cave destination, optionally inside a playlist subfolder. */
    fun musicCave(ctx: Context, subfolder: String?): OutputTarget {
        val sub = subfolder?.let { Downloader.safeName(it) }
        return if (Build.VERSION.SDK_INT >= 29) {
            MediaStoreOutput(ctx, "Music/Cave" + (sub?.let { "/$it" } ?: ""))
        } else {
            @Suppress("DEPRECATION")
            val music = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC)
            LegacyOutput(ctx, File(File(music, "Cave"), sub ?: ""))
        }
    }
}
