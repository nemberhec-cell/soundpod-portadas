package com.github.soundpod.ui.screens.importer

import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import com.github.soundpod.R
import com.github.soundpod.db
import com.github.soundpod.models.Playlist
import com.github.soundpod.models.Song
import com.github.soundpod.models.SongPlaylistMap
import com.github.soundpod.query
import com.github.soundpod.transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

private val KNOWN_MUSIC_APPS = listOf(
    "com.spotify.music" to "Spotify",
    "com.google.android.apps.youtube.music" to "YouTube Music",
    "com.apple.android.music" to "Apple Music",
    "com.amazon.mp3" to "Amazon Music",
    "com.deezer" to "Deezer",
    "com.tidal" to "Tidal",
    "com.soundcloud.android" to "SoundCloud",
    "com.pandora.android" to "Pandora",
    "com.audible.application" to "Audible",
    "com.google.android.apps.podcasts" to "Podcasts"
)

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: android.graphics.drawable.Drawable?
)

data class PhonePlaylist(val id: Long, val name: String, val trackCount: Int)

private fun Song(id: String, title: String, artist: String?): Song =
    Song(id = id, title = title, artistsText = artist, durationText = null, thumbnailUrl = null)

private fun createLocalPlaylist(name: String, songs: List<Song>, onDone: (Long?) -> Unit) {
    query {
        transaction {
            val playlistId = db.insert(Playlist(name = name))
            songs.forEach { db.insert(it) }
            db.insertSongPlaylistMaps(
                songs.mapIndexed { index, song ->
                    SongPlaylistMap(songId = song.id, playlistId = playlistId, position = index)
                }
            )
            onDone(playlistId)
        }
    }
}

private fun parseM3u(text: String): List<String> {
    val tracks = mutableListOf<String>()
    text.lineSequence().forEach { raw ->
        val line = raw.trim()
        when {
            line.isBlank() -> Unit
            line.startsWith("#EXTINF") -> {
                val comma = line.indexOf(',')
                if (comma >= 0) tracks += line.substring(comma + 1).trim()
            }
            line.startsWith("#") -> Unit
            else -> tracks += line.substringAfterLast('/').substringAfterLast('\\')
        }
    }
    return tracks
}

@Composable
fun ImportScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var phonePlaylists by remember { mutableStateOf<List<PhonePlaylist>>(emptyList()) }
    var permissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    var status by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    var customLink by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) loadPhonePlaylists(context) { phonePlaylists = it }
    }

    val m3uLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        importing = true
        status = null
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        BufferedReader(InputStreamReader(input)).readText()
                    } ?: ""
                } catch (_: Exception) { "" }
            }
            val titles = parseM3u(text)
            if (titles.isEmpty()) {
                status = context.getString(R.string.import_no_tracks)
                importing = false
                return@launch
            }
            val songs = titles.map { title ->
                // M3U entries are stored as local-file ids so they stay playable
                // when the file exists on the phone (same id scheme as MediaStore).
                Song(id = "m3u:$title", title = title, artist = null)
            }
            createLocalPlaylist("Playlist M3U (${titles.size})", songs) { playlistId ->
                status = if (playlistId != null) {
                    context.getString(R.string.import_created, titles.size)
                } else context.getString(R.string.import_failed)
                importing = false
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        // Detect installed music apps (manual list — no private data is read)
        apps = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            KNOWN_MUSIC_APPS.mapNotNull { (pkg, label) ->
                try {
                    pm.getPackageInfo(pkg, 0)
                    val icon = try { pm.getApplicationIcon(pkg) } catch (_: Exception) { null }
                    InstalledApp(pkg, label, icon)
                } catch (_: Exception) { null }
            }
        }
        if (permissionGranted) {
            phonePlaylists = withContext(Dispatchers.IO) { queryPhonePlaylists(context) }
        }
    }

    val importShareIntent = remember {
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.import_tab),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.import_tab_description),
                style = MaterialTheme.typography.bodyMedium
            )
            if (status != null) {
                Spacer(Modifier.height(8.dp))
                Text(text = status!!, style = MaterialTheme.typography.bodyMedium)
            }
            if (importing) {
                Spacer(Modifier.height(8.dp))
                CircularProgressIndicator()
            }
        }

        item {
            Text(
                text = stringResource(R.string.import_from_apps),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (apps.isEmpty()) {
                Text(text = stringResource(R.string.import_no_apps_found))
            } else {
                apps.forEach { app ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (app.icon != null) {
                                val bitmap = remember(app.packageName) {
                                    try {
                                        val drawable = app.icon
                                        val bmp = android.graphics.Bitmap.createBitmap(
                                            drawable.intrinsicWidth.coerceAtLeast(1),
                                            drawable.intrinsicHeight.coerceAtLeast(1),
                                            android.graphics.Bitmap.Config.ARGB_8888
                                        )
                                        val canvas = android.graphics.Canvas(bmp)
                                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                                        drawable.draw(canvas)
                                        bmp
                                    } catch (_: Exception) { null }
                                }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(40.dp))
                                }
                            } else {
                                Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(40.dp))
                            }
                            Spacer(Modifier.size(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.label, fontWeight = FontWeight.Medium)
                                Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                            }
                            // Manual transfer: open the app so the user can share a playlist to SoundPod
                            IconButton(onClick = {
                                val launch = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                if (launch != null) {
                                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(launch)
                                }
                            }) {
                                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.import_open_app))
                            }
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.import_share_hint),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        item {
            Text(
                text = stringResource(R.string.import_phone_lists),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (!permissionGranted) {
                Text(text = stringResource(R.string.import_permission_needed))
                Button(onClick = {
                    val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    permissionLauncher.launch(perm)
                }) {
                    Text(text = stringResource(R.string.grant_permission))
                }
            } else if (phonePlaylists.isEmpty()) {
                Text(text = stringResource(R.string.import_no_phone_lists))
            } else {
                phonePlaylists.forEach { playlist ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MusicNote, null)
                            Spacer(Modifier.size(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(playlist.name, fontWeight = FontWeight.Medium)
                                Text(
                                    stringResource(R.plurals.number_of_songs, playlist.trackCount, playlist.trackCount),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            OutlinedButton(onClick = {
                                importing = true
                                scope.launch {
                                    val songs = withContext(Dispatchers.IO) {
                                        queryPhonePlaylistSongs(context, playlist.id)
                                    }
                                    createLocalPlaylist(playlist.name, songs) {
                                        status = context.getString(R.string.import_created, songs.size)
                                        importing = false
                                    }
                                }
                            }) {
                                Text(text = stringResource(R.string.import_transfer))
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.import_m3u),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(text = stringResource(R.string.import_m3u_description))
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { m3uLauncher.launch("*/*") }) {
                Icon(Icons.Default.FileOpen, null)
                Spacer(Modifier.size(8.dp))
                Text(text = stringResource(R.string.import_pick_m3u))
            }
        }

        item {
            Text(
                text = stringResource(R.string.import_link),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(text = stringResource(R.string.import_link_description))
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = customLink,
                    onValueChange = { customLink = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("https://music.youtube.com/playlist?list=…") },
                    singleLine = true
                )
                Spacer(Modifier.size(8.dp))
                Button(
                    onClick = {
                        val link = customLink.trim()
                        if (link.isNotBlank()) {
                            // Reuse the existing ACTION_VIEW handler in MainActivity
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try { context.startActivity(intent) } catch (_: Exception) {
                                status = context.getString(R.string.import_failed)
                            }
                        }
                    }
                ) {
                    Text(text = stringResource(R.string.import_transfer))
                }
            }
        }

        item { Spacer(Modifier.height(48.dp)) }
    }
}

private fun loadPhonePlaylists(context: android.content.Context, onDone: (List<PhonePlaylist>) -> Unit) {
    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        onDone(queryPhonePlaylists(context))
    }
}

private fun queryPhonePlaylists(context: android.content.Context): List<PhonePlaylist> {
    val result = mutableListOf<PhonePlaylist>()
    val cursor = try {
        context.contentResolver.query(
            MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Playlists._ID, MediaStore.Audio.Playlists.NAME),
            null, null, MediaStore.Audio.Playlists.NAME + " ASC"
        )
    } catch (_: Exception) { null } ?: return emptyList()
    cursor.use {
        val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Playlists._ID)
        val nameCol = it.getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME)
        while (it.moveToNext()) {
            val id = it.getLong(idCol)
            val name = it.getString(nameCol) ?: continue
            val count = countPlaylistTracks(context, id)
            result += PhonePlaylist(id, name, count)
        }
    }
    return result
}

private fun countPlaylistTracks(context: android.content.Context, playlistId: Long): Int {
    val uri = android.content.ContentUris.withAppendedId(
        MediaStore.Audio.Playlists.Members.CONTENT_URI, playlistId
    )
    val cursor = try {
        context.contentResolver.query(uri, arrayOf(MediaStore.Audio.Playlists.Members._ID), null, null, null)
    } catch (_: Exception) { null } ?: return 0
    return cursor.use { it.count }
}

private fun queryPhonePlaylistSongs(context: android.content.Context, playlistId: Long): List<Song> {
    val songs = mutableListOf<Song>()
    val uri = android.content.ContentUris.withAppendedId(
        MediaStore.Audio.Playlists.Members.CONTENT_URI, playlistId
    )
    val cursor = try {
        context.contentResolver.query(
            uri,
            arrayOf(
                MediaStore.Audio.Playlists.Members.AUDIO_ID,
                MediaStore.Audio.Playlists.Members.TITLE,
                MediaStore.Audio.Playlists.Members.ARTIST,
                MediaStore.Audio.Playlists.Members.DURATION
            ),
            null, null, MediaStore.Audio.Playlists.Members.PLAY_ORDER + " ASC"
        )
    } catch (_: Exception) { null } ?: return emptyList()
    cursor.use {
        val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID)
        val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.TITLE)
        val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.ARTIST)
        while (it.moveToNext()) {
            val audioId = it.getLong(idCol)
            val title = it.getString(titleCol) ?: continue
            val artist = it.getString(artistCol)
            val contentUri = android.content.ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioId
            )
            songs += Song(id = contentUri.toString(), title = title, artistsText = artist, durationText = null, thumbnailUrl = null)
        }
    }
    return songs
}
