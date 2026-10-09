# HoneyBeat

A small music player for Android, styled like Cub by NeonBear. It plays only the songs Cave put in
`Music/Cave`, lets you make playlists, keeps playing with the screen off, and has notification and
lock-screen controls.

## Build the APK on GitHub

1. Upload the contents of this folder (including the hidden `.github` folder) to a new GitHub repo.
2. Open the **Actions** tab. The build starts by itself, or press **Run workflow** on **Build HoneyBeat APK**.
3. When it finishes, download `HoneyBeat-apk` from the run's Artifacts, unzip it, and install `HoneyBeat.apk` on the phone.

Other ways: open the folder in Android Studio and use Build > Build APK(s), or run `build-apk.bat` (result in `dist\HoneyBeat.apk`).

## Using it

- **Songs**: everything in Music/Cave, with search. Tap to play. Press **+** to add a song to a playlist.
- **Playlists**: make playlists, open one to play it, remove songs with **x**, or delete it.
- **Playing**: cover art, seek bar, previous / play / next, shuffle and repeat.
- **Options**: shuffle, repeat all, cover art, and **Rescan** after Cave downloads new songs.
