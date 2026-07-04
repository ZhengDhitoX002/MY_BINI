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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.res.painterResource
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

    // Inisialisasi Ad-Blocker sekali di level layar
    LaunchedEffect(Unit) {
        AdBlocker.init(context)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 96.dp)
        ) {
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
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                
                IconButton(
                    onClick = { if (webView?.canGoForward() == true) webView?.goForward() }
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Forward", tint = Color.White)
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
                    Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = Color.White)
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

            // WebView Integration
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; SM-S901B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36"
                        
                        val cookieManager = android.webkit.CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        webViewClient = object : WebViewClient() {
                            
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString() ?: return false
                                if (url.startsWith("http://") || url.startsWith("https://")) {
                                    return false
                                }
                                try {
                                    val intent = android.content.Intent.parseUri(url, android.content.Intent.URI_INTENT_SCHEME)
                                    if (intent != null) {
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: android.content.ActivityNotFoundException) {
                                            val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                                            if (fallbackUrl != null) {
                                                view?.loadUrl(fallbackUrl)
                                            }
                                        }
                                        return true
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                return true
                            }

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
                  Icon(
                      painter = painterResource(id = android.R.drawable.stat_sys_download),
                      contentDescription = "Download Link",
                      tint = Color.White
                  )
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
