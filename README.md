# Cave for Android

Same idea as Cave on Windows: paste a YouTube Music, Spotify or Apple Music playlist link,
pick an output folder, get MP3s. It remembers recent playlists; **Sync** only downloads new songs.

## Build the APK
Needs Android Studio installed once (it brings Java and the Android SDK).

Option A, no command line: open this folder in Android Studio, wait for the sync,
then **Build > Build APK(s)**. The file ends up in `app/build/outputs/apk/debug/`.

Option B: run `build-apk.bat` (needs Gradle for the first run: `winget install Gradle.Gradle`).
The result is `dist\Cave.apk`.

Install: copy the APK to the phone, open it, allow "install unknown apps".

Option C, no Android Studio: put this folder in a GitHub repository (the `.github` folder must come
with it). Open the repo's **Actions** tab, pick **Build Cave APK**, press **Run workflow**, and download
`Cave-apk` from the finished run. The log there also shows any build error.

## How it works
yt-dlp and ffmpeg run inside the app (youtubedl-android library). Spotify/Apple Music tracks are
read from their public pages, then each song is searched on YouTube and converted to MP3 and tagged.
MP3s go to Music/Cave on the phone by default (Android 10+ needs no permission for this). You can pick
another folder with the folder picker, and "Reset" goes back to Music/Cave.
A notification shows while downloading so Android keeps the app running.

## Limits
- Playlists must be public. Spotify's public page only exposes about 100 tracks.
- Search matching can pick a wrong version of a song now and then.
- Cover art is off by default (turn it on in Options); if it fails Cave retries without it.
- If downloads fail, press Update in Options (Cave also updates yt-dlp itself every 12 hours).
  The red "Last error" line on the Clone page and the Log tab show why a song failed.
- Only download music you have the right to copy.
