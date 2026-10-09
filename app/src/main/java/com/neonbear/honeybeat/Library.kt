package com.neonbear.honeybeat

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import android.content.SharedPreferences
import androidx.media3.common.MediaMetadata
import org.json.JSONArray
import org.json.JSONObject

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val path: String,
) {
    val uri: Uri get() = songUri(id)
}

fun songUri(id: Long): Uri =
    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

fun Song.toItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(uri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .build()
    )
    .build()

@Suppress("DEPRECATION")
fun loadSongs(ctx: Context): List<Song> {
    val out = ArrayList<Song>()
    val a = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val proj = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATA,
    )
    val sel = "${MediaStore.Audio.Media.DATA} LIKE '%/Music/Cave/%'" +
        " AND ${MediaStore.Audio.Media.DURATION} > 0"
    ctx.contentResolver.query(a, proj, sel, null, "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC")?.use { c ->
        val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val iDur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val iData = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        while (c.moveToNext()) {
            val artist = c.getString(iArtist)?.takeIf { it != "<unknown>" } ?: "Unknown artist"
            val album = c.getString(iAlbum)?.takeIf { it != "<unknown>" } ?: ""
            out += Song(
                id = c.getLong(iId),
                title = c.getString(iTitle) ?: "Untitled",
                artist = artist,
                album = album,
                durationMs = c.getLong(iDur),
                path = c.getString(iData) ?: "",
            )
        }
    }
    return out
}

fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

data class Playlist(val name: String, val paths: List<String>)

fun loadPlaylists(prefs: SharedPreferences): List<Playlist> = try {
    val arr = JSONArray(prefs.getString("playlists", "[]"))
    (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        val p = o.getJSONArray("paths")
        Playlist(o.getString("name"), (0 until p.length()).map { p.getString(it) })
    }
} catch (_: Exception) {
    emptyList()
}

fun savePlaylists(prefs: SharedPreferences, list: List<Playlist>) {
    val arr = JSONArray()
    list.forEach { pl ->
        arr.put(JSONObject().put("name", pl.name).put("paths", JSONArray(pl.paths)))
    }
    prefs.edit().putString("playlists", arr.toString()).apply()
}
