package com.mybini.app.ui.screens

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.mybini.app.media.AesCtrDataSource

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    modifier: Modifier = Modifier,
    mediaUri: Uri,
    isEncrypted: Boolean = false,
    encryptionKey: ByteArray? = null,
    encryptionIv: ByteArray? = null,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    // Inisialisasi ExoPlayer secara asinkron
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            // Tentukan sumber data (DataSource) apakah dienkripsi atau terbuka
            val dataSourceFactory = androidx.media3.datasource.DataSource.Factory {
                if (isEncrypted && encryptionKey != null && encryptionIv != null) {
                    AesCtrDataSource(encryptionKey, encryptionIv)
                } else {
                    DefaultDataSource.Factory(context).createDataSource()
                }
            }

            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(mediaUri))

            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
        }
    }

    // Release player saat Composable keluar dari Lifecycle (Mencegah leak RAM)
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Pemutar Video (Media3 PlayerView)
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Tombol Tutup Video
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color(0x80000000), shape = androidx.compose.foundation.shape.CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close Player", tint = Color.White)
        }
    }
}
