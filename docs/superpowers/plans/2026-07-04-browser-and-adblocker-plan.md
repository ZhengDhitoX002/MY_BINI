# Browser Bawaan & Ad-Blocker "MY BINI" Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mengimplementasikan browser internal terintegrasi Android WebView dengan filter pemblokir iklan (Ad-Blocker) berbasis EasyList dan pendeteksi video/audio otomatis (Link Sniffer) yang memicu tombol unduhan melayang.

**Architecture:** Memanfaatkan WebViewClient untuk mengintersepsi request (`shouldInterceptRequest`), membandingkan domain dengan HashSet pemblokir iklan berkinerja tinggi, memindai tautan media dengan Regex, dan menampilkan UI browser Jetpack Compose yang responsif dengan efek Glassmorphism.

**Tech Stack:** Kotlin, Jetpack Compose, Android WebView, Coroutines.

## Global Constraints
- Target platform: Android 14 (API 34) dan di atasnya (Minimum SDK = 34).
- Keamanan: Intersepsi URL ad-blocker berjalan secara non-blocking di memori RAM menggunakan HashSet.
- UI: Efek Glassmorphic blur pada tombol unduhan melayang, address bar, dan progress bar pemuatan halaman.

---

### Task 3.1: Ad-Blocker Engine & Assets

**Files:**
- Create: `app/src/main/assets/ads-blocklist.txt`
- Create: `app/src/main/java/com/mybini/app/data/downloader/AdBlocker.kt`

**Interfaces:**
- Consumes: None (Modul awal)
- Produces: `AdBlocker.init(context)` dan `AdBlocker.shouldBlock(url): Boolean`

- [ ] **Step 1: Buat berkas database domain iklan `ads-blocklist.txt`**
  
  Tulis berkas `app/src/main/assets/ads-blocklist.txt` dengan domain iklan populer untuk diblokir:
  ```text
  doubleclick.net
  google-analytics.com
  googlesyndication.com
  adservice.google.com
  adsystem.com
  adnxs.com
  outbrain.com
  taboola.com
  popads.net
  popcash.net
  adsterra.com
  exoclick.com
  juicyads.com
  ```

- [ ] **Step 2: Buat kelas mesin pemblokir `AdBlocker.kt`**
  
  Tulis berkas `app/src/main/java/com/mybini/app/data/downloader/AdBlocker.kt`:
  ```kotlin
  package com.mybini.app.data.downloader

  import android.content.Context
  import java.io.BufferedReader
  import java.io.InputStreamReader
  import java.net.URI

  object AdBlocker {
      private val blockedDomains = HashSet<String>()

      fun init(context: Context) {
          try {
              val assetManager = context.assets
              val inputStream = assetManager.open("ads-blocklist.txt")
              val reader = BufferedReader(InputStreamReader(inputStream))
              var line: String?
              while (reader.readLine().also { line = it } != null) {
                  val trimmed = line!!.trim()
                  if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                      blockedDomains.add(trimmed)
                  }
              }
              reader.close()
              inputStream.close()
          } catch (e: Exception) {
              e.printStackTrace()
          }
      }

      fun shouldBlock(url: String): Boolean {
          return try {
              val host = URI(url).host ?: return false
              // Cek jika host persis sama atau diakhiri oleh domain yang diblokir
              blockedDomains.any { host == it || host.endsWith(".$it") }
          } catch (e: Exception) {
              false
          }
      }
  }
  ```

- [ ] **Step 3: Commit perubahan**
  ```bash
  git add app/src/main/assets/ads-blocklist.txt app/src/main/java/com/mybini/app/data/downloader/AdBlocker.kt
  git commit -m "feat: add AdBlocker engine and default ads blocklist asset"
  ```

---

### Task 3.2: Link Sniffer & Interceptor

**Files:**
- Create: `app/src/main/java/com/mybini/app/data/downloader/LinkSniffer.kt`

**Interfaces:**
- Consumes: None (Utilitas pemindaian)
- Produces: `LinkSniffer.sniff(url): Boolean` dan callback pemicu event.

- [ ] **Step 1: Buat kelas `LinkSniffer.kt`**
  
  Tulis berkas `app/src/main/java/com/mybini/app/data/downloader/LinkSniffer.kt`:
  ```kotlin
  package com.mybini.app.data.downloader

  import java.util.regex.Pattern

  object LinkSniffer {
      // Regex untuk mendeteksi file media (video & audio) langsung
      private val MEDIA_REGEX = Pattern.compile(
          ".*\\.(mp4|m3u8|mp3|m4a|aac|ogg|webm|mov)(\\?.*)?$",
          Pattern.CASE_INSENSITIVE
      )

      fun sniff(url: String): Boolean {
          if (url.isEmpty()) return false
          val matcher = MEDIA_REGEX.matcher(url)
          return matcher.matches()
      }
  }
  ```

- [ ] **Step 2: Commit perubahan**
  ```bash
  git add app/src/main/java/com/mybini/app/data/downloader/LinkSniffer.kt
  git commit -m "feat: add LinkSniffer regex for media stream detection"
  ```

---

### Task 3.3: WebView Integration & Browser Screen UI

**Files:**
- Create: `app/src/main/java/com/mybini/app/ui/screens/BrowserScreen.kt`

**Interfaces:**
- Consumes: `AdBlocker`, `LinkSniffer`
- Produces: `BrowserScreen` Composable untuk navigasi web dan download otomatis.

- [ ] **Step 1: Buat Composable Screen `BrowserScreen.kt`**
  
  Tulis berkas `app/src/main/java/com/mybini/app/ui/screens/BrowserScreen.kt`:
  ```kotlin
  package com.mybini.app.ui.screens

  import android.annotation.SuppressLint
  import android.graphics.Bitmap
  import android.webkit.WebResourceRequest
  import android.webkit.WebResourceResponse
  import android.webkit.WebView
  import android.webkit.WebViewClient
  import androidx.compose.animation.AnimatedVisibility
  import androidx.compose.animation.fadeIn
  import androidx.compose.animation.fadeOut
  import androidx.compose.foundation.background
  import androidx.compose.foundation.border
  import androidx.compose.foundation.layout.*
  import androidx.compose.foundation.shape.RoundedCornerShape
  import androidx.compose.material.icons.Icons
  import androidx.compose.material.icons.filled.ArrowBack
  import androidx.compose.material.icons.filled.ArrowForward
  import androidx.compose.material.icons.filled.Download
  import androidx.compose.material.icons.filled.Refresh
  import androidx.compose.material3.*
  import androidx.compose.runtime.*
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.graphics.Color
  import androidx.compose.ui.platform.LocalContext
  import androidx.compose.ui.unit.dp
  import androidx.compose.ui.viewinterop.AndroidView
  import com.mybini.app.data.downloader.AdBlocker
  import com.mybini.app.data.downloader.LinkSniffer
  import java.io.ByteArrayInputStream

  @SuppressLint("SetJavaScriptEnabled")
  @Composable
  fun BrowserScreen(
      modifier: Modifier = Modifier,
      onDownloadDetected: (String) -> Unit
  ) {
      val context = LocalContext.current
      var webView: WebView? by remember { mutableStateOf(null) }
      var urlInput by remember { mutableStateOf("https://www.google.com") }
      var progress by remember { mutableStateOf(0f) }
      var isLoading by remember { mutableStateOf(false) }
      var sniffedUrl by remember { mutableStateOf("") }

      // Inisialisasi Ad-Blocker sekali
      LaunchedEffect(Unit) {
          AdBlocker.init(context)
      }

      Box(modifier = modifier.fillMaxSize()) {
          Column(modifier = Modifier.fillMaxSize()) {
              // Address Bar & Navigasi
              Row(
                  modifier = Modifier
                      .fillMaxWidth()
                      .padding(8.dp),
                  verticalAlignment = Alignment.CenterVertically
              ) {
                  IconButton(
                      onClick = { if (webView?.canGoBack() == true) webView?.goBack() }
                  ) {
                      Icon(Icons.Default.ArrowBack, contentDescription = "Back", color = Color.White)
                  }
                  
                  IconButton(
                      onClick = { if (webView?.canGoForward() == true) webView?.goForward() }
                  ) {
                      Icon(Icons.Default.ArrowForward, contentDescription = "Forward", color = Color.White)
                  }

                  TextField(
                      value = urlInput,
                      onValueChange = { urlInput = it },
                      colors = TextFieldDefaults.colors(
                          focusedTextColor = Color.White,
                          unfocusedTextColor = Color.White,
                          focusedContainerColor = Color(0x1AFFFFFF),
                          unfocusedContainerColor = Color(0x1AFFFFFF),
                          focusedIndicatorColor = Color.Transparent,
                          unfocusedIndicatorColor = Color.Transparent
                      ),
                      shape = RoundedCornerShape(12.dp),
                      modifier = Modifier
                          .weight(1f)
                          .height(52.dp)
                          .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp)),
                      singleLine = true
                  )

                  IconButton(onClick = { webView?.reload() }) {
                      Icon(Icons.Default.Refresh, contentDescription = "Reload", color = Color.White)
                  }
              }

              if (isLoading) {
                  LinearProgressIndicator(
                      progress = { progress },
                      modifier = Modifier.fillMaxWidth(),
                      color = MaterialTheme.colorScheme.primary,
                      trackColor = Color.Transparent
                  )
              }

              // WebView Interop
              AndroidView(
                  factory = { ctx ->
                      WebView(ctx).apply {
                          settings.javaScriptEnabled = true
                          settings.domStorageEnabled = true
                          webViewClient = object : WebViewClient() {
                              
                              override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                  super.onPageStarted(view, url, favicon)
                                  isLoading = true
                                  sniffedUrl = "" // Reset sniffer tiap kali ganti halaman
                                  if (url != null) urlInput = url
                              }

                              override fun onPageFinished(view: WebView?, url: String?) {
                                  super.onPageFinished(view, url)
                                  isLoading = false
                              }

                              override fun shouldInterceptRequest(
                                  view: WebView?,
                                  request: WebResourceRequest?
                              ): WebResourceResponse? {
                                  val requestUrl = request?.url?.toString() ?: return null

                                  // 1. Cek Ad-Blocker
                                  if (AdBlocker.shouldBlock(requestUrl)) {
                                      return WebResourceResponse(
                                          "text/javascript",
                                          "UTF-8",
                                          ByteArrayInputStream(ByteArray(0))
                                      )
                                  }

                                  // 2. Cek Link Sniffer
                                  if (LinkSniffer.sniff(requestUrl)) {
                                      sniffedUrl = requestUrl
                                  }

                                  return super.shouldInterceptRequest(view, request)
                              }
                          }
                          loadUrl(urlInput)
                          webView = this
                      }
                  },
                  modifier = Modifier.weight(1f)
              )
          }

          // Tombol Unduh Melayang (Bouncing Floating Action Button)
          AnimatedVisibility(
              visible = sniffedUrl.isNotEmpty(),
              enter = fadeIn(),
              exit = fadeOut(),
              modifier = Modifier
                  .align(Alignment.BottomEnd)
                  .padding(end = 16.dp, bottom = 80.dp) // Beri margin agar tidak menabrak BottomBar
          ) {
              FloatingActionButton(
                  onClick = { onDownloadDetected(sniffedUrl) },
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = Color.White,
                  shape = RoundedCornerShape(16.dp),
                  modifier = Modifier.border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
              ) {
                  Icon(Icons.Default.Download, contentDescription = "Download Link")
              }
          }
      }

      // Hancurkan WebView saat Composable keluar dari komposisi (Cegah leak RAM!)
      DisposableEffect(Unit) {
          onDispose {
              webView?.destroy()
              webView = null
          }
      }
  }
  ```

- [ ] **Step 2: Commit perubahan**
  ```bash
  git add app/src/main/java/com/mybini/app/ui/screens/BrowserScreen.kt
  git commit -m "feat: implement BrowserScreen with adblocking and link sniffing WebView integration"
  ```
