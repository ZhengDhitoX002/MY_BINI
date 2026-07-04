# Perpustakaan Offline & Pemutar Media "MY BINI" Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mengimplementasikan layar perpustakaan berkas hasil unduhan asinkron dari Room DB dan pemutar video/audio ExoPlayer Media3 yang mendukung pemutaran terenkripsi RAM-only (AesCtrDataSource).

**Architecture:** Membaca database menggunakan Kotlin Coroutines Flow secara asinkron, mengintegrasikan PlayerView Media3 di Jetpack Compose menggunakan AndroidView, dan menginisialisasi ExoPlayer dengan DataSource kustom untuk dekripsi luring.

**Tech Stack:** Kotlin, Jetpack Compose, Media3 ExoPlayer, Room DB.

## Global Constraints
- Target platform: Android 14 (API 34) dan di atasnya (Minimum SDK = 34).
- Keamanan: Deteksi status enkripsi berkas secara otomatis untuk memilih `AesCtrDataSource` vs `DefaultDataSource`.
- Lifecycle: Pelepasan sumber daya ExoPlayer (`release()`) secara otomatis saat Composable dihancurkan.

---

### Task 4.1: Media3 ExoPlayer Video Player Composable

**Files:**
- Create: `app/src/main/java/com/mybini/app/ui/screens/VideoPlayerScreen.kt`

**Interfaces:**
- Consumes: `AesCtrDataSource`
- Produces: `VideoPlayerScreen` Composable untuk memutar media terenkripsi maupun mentah.

- [ ] **Step 1: Buat kelas Composable `VideoPlayerScreen.kt`**
  
  Tulis berkas `app/src/main/java/com/mybini/app/ui/screens/VideoPlayerScreen.kt`:
  ```kotlin
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

      // Inisialisasi ExoPlayer secara lazy
      val exoPlayer = remember {
          ExoPlayer.Builder(context).build().apply {
              // Siapkan DataSource sesuai status enkripsi berkas
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

      // Hancurkan player saat Composable keluar dari komposisi (Cegah leak memori RAM!)
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
          // Player View
          AndroidView(
              factory = { ctx ->
                  PlayerView(ctx).apply {
                      player = exoPlayer
                      useController = true
                  }
              },
              modifier = Modifier.fillMaxSize()
          )

          // Tombol Tutup Player
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
  ```

- [ ] **Step 2: Commit perubahan**
  ```bash
  git add app/src/main/java/com/mybini/app/ui/screens/VideoPlayerScreen.kt
  git commit -m "feat: implement VideoPlayerScreen using Media3 ExoPlayer with encrypted playback support"
  ```

---

### Task 4.2: Library Screen UI & Database Integration

**Files:**
- Create: `app/src/main/java/com/mybini/app/ui/screens/LibraryScreen.kt`

**Interfaces:**
- Consumes: `MyBiniDatabase`, `VideoPlayerScreen`
- Produces: `LibraryScreen` Composable untuk menampilkan riwayat unduhan luring.

- [ ] **Step 1: Buat kelas Composable `LibraryScreen.kt`**
  
  Tulis berkas `app/src/main/java/com/mybini/app/ui/screens/LibraryScreen.kt`:
  ```kotlin
  package com.mybini.app.ui.screens

  import android.net.Uri
  import androidx.compose.foundation.background
  import androidx.compose.foundation.clickable
  import androidx.compose.foundation.layout.*
  import androidx.compose.foundation.lazy.LazyColumn
  import androidx.compose.foundation.lazy.items
  import androidx.compose.foundation.shape.RoundedCornerShape
  import androidx.compose.material.icons.Icons
  import androidx.compose.material.icons.filled.Delete
  import androidx.compose.material.icons.filled.PlayArrow
  import androidx.compose.material3.*
  import androidx.compose.runtime.*
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.graphics.Brush
  import androidx.compose.ui.graphics.Color
  import androidx.compose.ui.platform.LocalContext
  import androidx.compose.ui.unit.dp
  import androidx.compose.ui.unit.sp
  import com.mybini.app.data.database.DownloadItem
  import com.mybini.app.data.database.MyBiniDatabase
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.launch
  import kotlinx.coroutines.withContext
  import java.io.File

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  fun LibraryScreen(
      modifier: Modifier = Modifier
  ) {
      val context = LocalContext.current
      val scope = rememberCoroutineScope()
      
      // Inisialisasi DB dengan dummy key (Fase 5 nanti diubah jadi real key PIN)
      val db = remember { MyBiniDatabase.getDatabase(context, ByteArray(32), "mybini_db") }
      val downloadDao = db.downloadDao()

      var downloadList by remember { mutableStateOf(emptyList<DownloadItem>()) }
      var activeVideoUri by remember { mutableStateOf<Uri?>(null) }

      // Ambil data download secara asinkron
      LaunchedEffect(Unit) {
          withContext(Dispatchers.IO) {
              val items = downloadDao.getAllDownloads()
              withContext(Dispatchers.Main) {
                  downloadList = items
              }
          }
      }

      Box(
          modifier = modifier
              .fillMaxSize()
              .background(
                  Brush.verticalGradient(
                      colors = listOf(Color(0xFF181824), Color(0xFF0C0C12))
                  )
              )
      ) {
          Column(
              modifier = Modifier
                  .fillMaxSize()
                  .padding(16.dp)
          ) {
              Text(
                  text = "Offline Library",
                  style = MaterialTheme.typography.headlineMedium,
                  color = Color.White,
                  modifier = Modifier.padding(bottom = 16.dp)
              )

              if (downloadList.isEmpty()) {
                  Box(
                      modifier = Modifier.weight(1f).fillMaxWidth(),
                      contentAlignment = Alignment.Center
                  ) {
                      Text("Belum ada file terunduh.", color = Color.Gray)
                  }
              } else {
                  LazyColumn(
                      modifier = Modifier.weight(1f),
                      verticalArrangement = Arrangement.spacedBy(12.dp)
                  ) {
                      items(downloadList) { item ->
                          DownloadItemCard(
                              item = item,
                              onClick = { activeVideoUri = Uri.fromFile(File(item.filePath)) },
                              onDelete = {
                                  scope.launch(Dispatchers.IO) {
                                      downloadDao.deleteDownload(item)
                                      val file = File(item.filePath)
                                      if (file.exists()) file.delete()
                                      
                                      val updated = downloadDao.getAllDownloads()
                                      withContext(Dispatchers.Main) {
                                          downloadList = updated
                                      }
                                  }
                              }
                          )
                      }
                  }
              }
              Spacer(modifier = Modifier.height(72.dp)) // Cegah tertutup Bottom Bar
          }

          // Overlay Pemutar Video
          activeVideoUri?.let { uri ->
              VideoPlayerScreen(
                  mediaUri = uri,
                  isEncrypted = false, // File di library standar bersifat terbuka (tidak dienkripsi)
                  onClose = { activeVideoUri = null }
              )
          }
      }
  }

  @Composable
  fun DownloadItemCard(
      item: DownloadItem,
      onClick: () -> Unit,
      onDelete: () -> Unit
  ) {
      Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF)),
          modifier = Modifier
              .fillMaxWidth()
              .clickable { onClick() }
      ) {
          Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically
          ) {
              Icon(
                  Icons.Default.PlayArrow,
                  contentDescription = "Play",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(36.dp)
              )
              Spacer(modifier = Modifier.width(16.dp))
              Column(modifier = Modifier.weight(1f)) {
                  Text(item.title, color = Color.White, fontSize = 16.sp, maxLines = 1)
                  Text(
                      text = "${item.mediaType} • ${(item.sizeBytes / (1024 * 1024))} MB",
                      color = Color.Gray,
                      fontSize = 12.sp
                  )
              }
              IconButton(onClick = onDelete) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
              }
          }
      }
  }
  ```

- [ ] **Step 2: Commit perubahan**
  ```bash
  git add app/src/main/java/com/mybini/app/ui/screens/LibraryScreen.kt
  git commit -m "feat: implement LibraryScreen UI displaying downloaded media list from database"
  ```
