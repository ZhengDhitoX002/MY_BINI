package com.mybini.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mybini.app.data.database.DownloadItem
import com.mybini.app.data.database.MyBiniDatabase
import com.mybini.app.data.downloader.GeminiNanoWrapper
import com.mybini.app.data.downloader.WhisperTranscriber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.mybini.app.ui.screens.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    currentTheme: AppTheme = AppTheme.DARK_KNIGHT
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Inisialisasi DB luring
    val db = remember { MyBiniDatabase.getDatabase(context, ByteArray(32), "mybini_db") }
    val downloadDao = db.downloadDao()

    var downloadList by remember { mutableStateOf(emptyList<DownloadItem>()) }
    var activeVideoUri by remember { mutableStateOf<Uri?>(null) }

    // State untuk Dialog AI
    var aiDialogItem by remember { mutableStateOf<DownloadItem?>(null) }
    var aiTranscript by remember { mutableStateOf("") }
    var aiSummary by remember { mutableStateOf("") }
    var isAiLoading by remember { mutableStateOf(false) }

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
            .background(Brush.verticalGradient(colors = currentTheme.gradientColors))
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
                            },
                            onAiClick = {
                                aiDialogItem = item
                                isAiLoading = true
                                aiTranscript = ""
                                aiSummary = ""
                                scope.launch(Dispatchers.Default) {
                                    // Panggil Whisper Transcriber lokal
                                    val text = WhisperTranscriber.transcribe(File(item.filePath))
                                    // Panggil Gemini Nano lokal untuk meringkas teks
                                    val sum = GeminiNanoWrapper.summarize(text)
                                    withContext(Dispatchers.Main) {
                                        aiTranscript = text
                                        aiSummary = sum
                                        isAiLoading = false
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
                isEncrypted = false,
                onClose = { activeVideoUri = null }
            )
        }

        // Sheet Dialog AI Transkripsi & Ringkasan Luring
        aiDialogItem?.let { item ->
            AlertDialog(
                onDismissRequest = { aiDialogItem = null },
                confirmButton = {
                    TextButton(onClick = { aiDialogItem = null }) {
                        Text("Tutup", color = MaterialTheme.colorScheme.primary)
                    }
                },
                title = {
                    Text(
                        text = "Analisis AI Luring (Whisper & Gemini)",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.6f)
                    ) {
                        Text(item.title, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (isAiLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Transkrip Suara:",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = aiTranscript,
                                    color = Color(0xCCFFFFFF),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                                )
                                Text(
                                    text = "Ringkasan Eksekutif:",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = aiSummary,
                                    color = Color(0xCCFFFFFF),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                },
                containerColor = Color(0xFF1E1E2C),
                textContentColor = Color.White,
                titleContentColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
            )
        }
    }
}

@Composable
fun DownloadItemCard(
    item: DownloadItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onAiClick: () -> Unit
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
            IconButton(onClick = onAiClick) {
                Icon(Icons.Default.Info, contentDescription = "AI Action", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}
