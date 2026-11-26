package com.github.soundpod.ui.screens.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.github.core.ui.LocalAppearance
import com.github.soundpod.R
import com.github.soundpod.db
import com.github.soundpod.models.Playlist
import com.github.soundpod.query
import com.github.soundpod.ui.components.ConfirmationDialog
import com.github.soundpod.ui.components.ParallaxScreenLayout
import com.github.soundpod.ui.components.TextFieldDialog
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text as ListText
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Icon
import com.github.soundpod.ui.screens.settings.subscreen.ListBackgroundEditor

import com.github.soundpod.customart.hasCustomArtwork
import com.github.soundpod.customart.rememberCustomArtworkPicker
import com.github.soundpod.customart.removeCustomArtwork
import com.github.soundpod.customart.toastCoverFailed
import com.github.soundpod.customart.toastCoverUpdated
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.filterNotNull

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun FavoritePlaylistScreen(
    playlistId: Long,
    pop: () -> Unit,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    var playlist: Playlist? by remember { mutableStateOf(null) }

    var isRenaming by rememberSaveable { mutableStateOf(false) }
    var isDeleting by rememberSaveable { mutableStateOf(false) }
    val (colorPalette) = LocalAppearance.current

    var isEditMode by remember { mutableStateOf(false) }
    var selectedUids by remember { mutableStateOf(emptySet<String>()) }
    var isSearching by remember { mutableStateOf(false) }

    var showListBackgroundSettings by remember { mutableStateOf(false) }
    var songCount by remember { mutableIntStateOf(0) }
    // No auto-cover: the playlist cover is only what the user sets.
    val thumbnailUrl: String? = null
    val context = LocalContext.current
    val playlistCoverKey = "playlist:$playlistId"
    val playlistHasCustomCover = hasCustomArtwork(playlistCoverKey)
    val pickCover = rememberCustomArtworkPicker(
        onDone = { toastCoverUpdated(context) },
        onError = { toastCoverFailed(context) }
    )

    LaunchedEffect(Unit) {
        db.playlist(playlistId).filterNotNull().collect { playlist = it }
    }

    LaunchedEffect(Unit) {
        db.playlistSongs(playlistId).collect { songs ->
            songCount = songs.size
        }
    }


    BackHandler(enabled = isEditMode || isSearching) {
        if (isSearching) {
            isSearching = false
        } else if (isEditMode) {
            isEditMode = false
            selectedUids = emptySet()
        }
    }

    ParallaxScreenLayout(
        title = {
            Column {
                Text(
                    text = playlist?.name ?: "",
                    style = typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorPalette.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = pluralStringResource(id = R.plurals.number_of_songs, count = songCount, songCount),
                    style = typography.bodySmall,
                    color = colorPalette.text.copy(alpha = 0.7f)
                )
            }
        },
        actions = {
            IconButton(
                onClick = { showListBackgroundSettings = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Photo,
                    contentDescription = stringResource(R.string.customize_list_background),
                    tint = colorPalette.text
                )
            }
            IconButton(
                onClick = onSearchClick
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = colorPalette.text
                )
            }
        },
        dropDownMenuContent = { dismissMenu ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.rename_playlist),
                        color = colorPalette.text,
                        style = typography.bodyLarge
                    )
                },
                onClick = {
                    isRenaming = true
                    dismissMenu()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.delete_playlist),
                        color = colorPalette.text,
                        style = typography.bodyLarge
                    )
                },
                onClick = {
                    isDeleting = true
                    dismissMenu()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(id = R.string.change_cover),
                        color = colorPalette.text,
                        style = typography.bodyLarge
                    )
                },
                onClick = {
                    pickCover(playlistCoverKey)
                    dismissMenu()
                }
            )
            if (playlistHasCustomCover) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(id = R.string.reset_cover),
                            color = colorPalette.text,
                            style = typography.bodyLarge
                        )
                    },
                    onClick = {
                        removeCustomArtwork(context, playlistCoverKey)
                        dismissMenu()
                    }
                )
            }
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(id = R.string.settings),
                        color = colorPalette.text,
                        style = typography.bodyLarge
                    )
                },
                onClick = {
                    onSettingsClick()
                    dismissMenu()
                }
            )
        },
        isLoading = playlist == null,
        thumbnailUrl = thumbnailUrl,
        thumbnailItemKey = playlistCoverKey,
        onThumbnailClick = { pickCover(playlistCoverKey) },
        listBackgroundKey = playlistCoverKey,
        headerTitle = playlist?.name ?: "",
        content = {
            FavoritePlaylistSongs(
                playlistId = playlistId,
                onGoToAlbum = onGoToAlbum,
                onGoToArtist = onGoToArtist
            )
        },
        onBackClick = {
            if (isSearching) {
                isSearching = false
            } else if (isEditMode) {
                isEditMode = false
                selectedUids = emptySet()
            } else {
                pop()
            }
        },
    )

    if (isRenaming) {
        TextFieldDialog(
            title = stringResource(id = R.string.rename_playlist),
            hintText = stringResource(id = R.string.playlist_name_hint),
            initialTextInput = playlist?.name ?: "",
            onDismiss = { isRenaming = false },
            onDone = { text ->
                query {
                    playlist?.copy(name = text)
                        ?.let(db::update)
                }
            }
        )
    }

    if (showListBackgroundSettings) {
        ModalBottomSheet(
            onDismissRequest = { showListBackgroundSettings = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ListBackgroundEditor(listKey = playlistCoverKey)
        }
    }

    if (isDeleting) {
        ConfirmationDialog(
            title = stringResource(id = R.string.delete_playlist_dialog),
            onDismiss = { isDeleting = false },
            onConfirm = {
                query {
                    playlist?.let(db::delete)
                }
                pop()
            }
        )
    }
}