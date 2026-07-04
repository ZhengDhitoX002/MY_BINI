package com.mybini.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mybini.app.ui.theme.MyBiniTheme
import com.mybini.app.ui.screens.BrowserScreen
import com.mybini.app.ui.screens.LibraryScreen
import com.mybini.app.ui.screens.VaultScreen
import com.mybini.app.ui.screens.SettingsScreen
import com.mybini.app.ui.screens.AppTheme
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import com.mybini.app.data.downloader.DownloadService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyBiniTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

enum class ScreenTab(val route: String) {
    HOME("home"),
    BROWSER("browser"),
    LIBRARY("library"),
    VAULT("vault"),
    SETTINGS("settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf(ScreenTab.HOME) }
    var currentTheme by remember { mutableStateOf(AppTheme.DARK_KNIGHT) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Konten Halaman Aktif dengan Animasi Transisi
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ScreenTransition"
        ) { tab ->
            when (tab) {
                ScreenTab.HOME -> PlaceholderScreen(
                    title = "MY BINI Downloader",
                    subtitle = stringResource(R.string.paste_link_placeholder),
                    showSearchBar = true,
                    currentTheme = currentTheme
                )
                ScreenTab.BROWSER -> BrowserScreen(
                    onDownloadDetected = { url ->
                        selectedTab = ScreenTab.HOME
                    }
                )
                ScreenTab.LIBRARY -> LibraryScreen(currentTheme = currentTheme)
                ScreenTab.VAULT -> VaultScreen(currentTheme = currentTheme)
                ScreenTab.SETTINGS -> SettingsScreen(
                    currentTheme = currentTheme,
                    onThemeChange = { currentTheme = it }
                )
            }
        }

        // Floating Glassmorphic Navigation Bar di bagian bawah
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
                .height(72.dp)
        ) {
            // Efek Kaca Blur (Glassmorphism)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(20.dp) // Efek blur background
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x20FFFFFF)) // Semi-transparan putih
                    .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(24.dp))
            )

            // Baris Tombol Navigasi
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationItem(
                    icon = Icons.Default.Home,
                    label = stringResource(R.string.tab_home),
                    isSelected = selectedTab == ScreenTab.HOME,
                    onClick = { selectedTab = ScreenTab.HOME },
                    accentColor = currentTheme.primaryAccent
                )
                NavigationItem(
                    icon = Icons.Default.Info,
                    label = stringResource(R.string.tab_browser),
                    isSelected = selectedTab == ScreenTab.BROWSER,
                    onClick = { selectedTab = ScreenTab.BROWSER },
                    accentColor = currentTheme.primaryAccent
                )
                NavigationItem(
                    icon = Icons.Default.PlayArrow,
                    label = stringResource(R.string.tab_library),
                    isSelected = selectedTab == ScreenTab.LIBRARY,
                    onClick = { selectedTab = ScreenTab.LIBRARY },
                    accentColor = currentTheme.primaryAccent
                )
                NavigationItem(
                    icon = Icons.Default.Lock,
                    label = stringResource(R.string.tab_vault),
                    isSelected = selectedTab == ScreenTab.VAULT,
                    onClick = { selectedTab = ScreenTab.VAULT },
                    accentColor = currentTheme.primaryAccent
                )
                NavigationItem(
                    icon = Icons.Default.Settings,
                    label = stringResource(R.string.tab_settings),
                    isSelected = selectedTab == ScreenTab.SETTINGS,
                    onClick = { selectedTab = ScreenTab.SETTINGS },
                    accentColor = currentTheme.primaryAccent
                )
            }
        }
    }
}

@Composable
fun RowScope.NavigationItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) accentColor else Color(0x99FFFFFF),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier.height(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 3.dp)
                        .background(accentColor, shape = RoundedCornerShape(1.5.dp))
                )
            } else {
                Text(
                    text = label,
                    color = Color(0x66FFFFFF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(
    title: String,
    subtitle: String,
    showSearchBar: Boolean,
    currentTheme: AppTheme = AppTheme.DARK_KNIGHT
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = currentTheme.gradientColors))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 80.dp) // Beri ruang agar tidak tertutup floating bar
        ) {
            Text(
                text = title,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White, currentTheme.primaryAccent)
                    )
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            if (showSearchBar) {
                Spacer(modifier = Modifier.height(24.dp))
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val client = remember { okhttp3.OkHttpClient() }
                val ytScraper = remember { com.mybini.app.data.scraper.YouTubeScraper(client) }
                val igScraper = remember { com.mybini.app.data.scraper.InstagramScraper(client) }
                val spMatcher = remember { com.mybini.app.data.scraper.SpotifyMatcher(client, ytScraper) }

                var textState by remember { mutableStateOf("") }
                var torrentStatus by remember { mutableStateOf("") }
                
                TextField(
                    value = textState,
                    onValueChange = { textState = it },
                    placeholder = { Text("Paste link here...", color = Color.Gray) },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0x1AFFFFFF),
                        unfocusedContainerColor = Color(0x1AFFFFFF),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { 
                        val url = textState.trim()
                        if (url.isEmpty()) return@Button

                        if (url.startsWith("magnet:?")) {
                            torrentStatus = "Initializing Torrent..."
                            com.mybini.app.data.downloader.TorrentEngine.startDownload(context, url) { progress, speed, peers, seeds, status ->
                                torrentStatus = if (status == "Finished") {
                                    "Torrent Selesai Diunduh!"
                                } else {
                                    "Mengunduh Torrent: ${(progress * 100).toInt()}% ($speed MB/s, Peers: $peers, Seeds: $seeds)"
                                }
                            }
                        } else {
                            torrentStatus = "Scraping media info..."
                            scope.launch(Dispatchers.Default) {
                                val result = when {
                                    url.contains("youtube") || url.contains("youtu.be") -> ytScraper.extractMediaUrl(url)
                                    url.contains("instagram.com") -> igScraper.extractMediaUrl(url)
                                    url.contains("spotify.com") -> spMatcher.extractMediaUrl(url)
                                    else -> com.mybini.app.data.scraper.ScrapeResult.Error("URL tidak dikenali. Gunakan YouTube, Instagram, Spotify, atau Magnet Link.")
                                }

                                withContext(Dispatchers.Main) {
                                    when (result) {
                                        is com.mybini.app.data.scraper.ScrapeResult.Success -> {
                                            val bestItem = result.mediaItems.firstOrNull()
                                            if (bestItem != null) {
                                                torrentStatus = "Memulai unduhan..."
                                                val ext = if (bestItem.mimeType.contains("audio")) "mp3" else "mp4"
                                                val targetFile = File(context.getExternalFilesDir(null), "${System.currentTimeMillis()}.$ext")
                                                
                                                val intent = Intent(context, DownloadService::class.java).apply {
                                                    action = DownloadService.ACTION_START_DOWNLOAD
                                                    putExtra(DownloadService.EXTRA_DOWNLOAD_ID, System.currentTimeMillis().toString())
                                                    putExtra(DownloadService.EXTRA_URL, bestItem.downloadUrl)
                                                    putExtra(DownloadService.EXTRA_FILE_PATH, targetFile.absolutePath)
                                                    putExtra(DownloadService.EXTRA_TITLE, bestItem.title)
                                                }
                                                context.startForegroundService(intent)
                                                torrentStatus = "Mengunduh di latar belakang..."
                                            } else {
                                                torrentStatus = "Gagal menemukan tautan media langsung"
                                            }
                                        }
                                        is com.mybini.app.data.scraper.ScrapeResult.Error -> {
                                            torrentStatus = "Error: ${result.message}"
                                        }
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = currentTheme.primaryAccent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Download", color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                if (torrentStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = torrentStatus, color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}
