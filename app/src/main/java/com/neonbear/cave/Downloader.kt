package com.neonbear.cave

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Downloads one track as MP3 and hands it to an OutputTarget. */
object Downloader {

    enum class Result { Done, Skipped, Failed }

    /** Process ids of running yt-dlp jobs, so Cancel can kill them. */
    val active: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /** The most recent failure reason, shown in the app so you can see why a song failed. */
    @Volatile var lastError: String = ""

    /** Extra yt-dlp options tried in order. If YouTube blocks one way of asking, the next may work. */
    private val attempts: List<List<String>> = listOf(
        emptyList(),
        listOf("--extractor-args", "youtube:player_client=android_vr"),
        listOf("--extractor-args", "youtube:player_client=tv"),
    )

    fun safeName(s: String): String {
        var r = s.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().trimEnd('.')
        if (r.length > 150) r = r.take(150)
        return r.ifEmpty { "track" }
    }

    fun cancelAll() {
        for (pid in active.toList()) {
            runCatching { YoutubeDL.getInstance().destroyProcessById(pid) }
        }
    }

    fun download(
        ctx: Context,
        track: Track,
        album: String,
        dest: OutputTarget,
        bitrate: Int,
        cover: Boolean,
        skipExisting: Boolean,
        pid: String,
        log: (String) -> Unit,
    ): Result {
        val fileName = safeName(track.display) + ".mp3"
        if (skipExisting && dest.exists(fileName)) return Result.Skipped

        val work = File(ctx.cacheDir, "dl").apply { mkdirs() }
        fun cleanup() {
            work.listFiles()?.filter { it.name.startsWith(pid) }?.forEach { it.delete() }
        }

        cleanup()
        active.add(pid)
        try {
            var ok = false
            for (extra in attempts) {
                cleanup()
                ok = runYtDlp(track, album, work, bitrate, cover, pid, extra, log)
                if (!ok && cover) {                     // cover art can be what breaks it
                    cleanup()
                    ok = runYtDlp(track, album, work, bitrate, false, pid, extra, log)
                }
                if (ok) break
                if (pid !in active) break               // cancelled
            }

            val mp3 = File(work, "$pid.mp3")
            if (!ok || !mp3.exists()) return Result.Failed

            if (!dest.write(fileName, mp3)) {
                lastError = "Couldn't save $fileName to the output folder."
                log(lastError)
                return Result.Failed
            }
            return Result.Done
        } finally {
            active.remove(pid)
            cleanup()
        }
    }

    private fun runYtDlp(
        track: Track,
        album: String,
        work: File,
        bitrate: Int,
        cover: Boolean,
        pid: String,
        extra: List<String>,
        log: (String) -> Unit,
    ): Boolean {
        val target = track.youtubeId?.let { "https://www.youtube.com/watch?v=$it" }
            ?: "ytsearch1:${track.artist} - ${track.title} audio"

        fun q(s: String) = s.replace("\"", "").replace("\\", "")

        val req = YoutubeDLRequest(target)
        req.addOption("-x")
        req.addOption("--audio-format", "mp3")
        req.addOption("--audio-quality", "${bitrate}K")
        req.addOption("--no-playlist")
        req.addOption("--no-warnings")
        req.addOption("-o", File(work, "$pid.%(ext)s").absolutePath)
        if (extra.size == 2) req.addOption(extra[0], extra[1])

        if (track.youtubeId != null) {
            req.addOption("--embed-metadata")
        } else {
            // Spotify / Apple Music: tag the file with the real title, artist and album.
            req.addOption(
                "--postprocessor-args",
                "ExtractAudio:-metadata \"title=${q(track.title)}\" -metadata \"artist=${q(track.artist)}\" " +
                    "-metadata \"album=${q(album)}\"",
            )
        }
        if (cover) {
            req.addOption("--embed-thumbnail")
            req.addOption("--convert-thumbnails", "jpg")
        }

        return try {
            YoutubeDL.getInstance().execute(req, pid)
            File(work, "$pid.mp3").exists()
        } catch (e: Exception) {
            val why = (e.message ?: e.javaClass.simpleName).trim()
            lastError = why.lines().lastOrNull { it.isNotBlank() }?.take(300) ?: why.take(300)
            log("FAILED (${extra.lastOrNull() ?: "default"}): ${track.display}\n${why.take(800)}")
            false
        }
    }
}
