# Kustomisasi Tema Premium & Latar Belakang "MY BINI" Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mengimplementasikan mesin tema dinamis (Dark Knight, Cyberpunk, Pastel Gold) lengkap dengan perubahan latar belakang gradien dan warna aksen secara real-time di seluruh aplikasi.

**Architecture:** Mendefinisikan enum `AppTheme` yang memetakan warna gradien dan warna utama, mengelola state tema di tingkat root `MainScreen`, serta membuat `SettingsScreen` interaktif untuk memilih tema.

**Tech Stack:** Kotlin, Jetpack Compose.

---

### Task 8.1: SettingsScreen & Dynamic Themes Engine

**Files:**
- Create: `app/src/main/java/com/mybini/app/ui/screens/SettingsScreen.kt`

**Interfaces:**
- Consumes: None (Modul awal)
- Produces: `SettingsScreen` Composable untuk mengkonfigurasi tema dan parameter sistem.

- [ ] **Step 1: Buat berkas `SettingsScreen.kt`**
  
  Tulis berkas `app/src/main/java/com/mybini/app/ui/screens/SettingsScreen.kt` yang memuat enum tema, warna gradien, dan layout kartu pilihan tema:
  ```kotlin
  package com.mybini.app.ui.screens

  import androidx.compose.foundation.background
  import androidx.compose.foundation.border
  import androidx.compose.foundation.clickable
  import androidx.compose.foundation.layout.*
  import androidx.compose.foundation.shape.RoundedCornerShape
  import androidx.compose.material3.*
  import androidx.compose.runtime.*
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.graphics.Brush
  import androidx.compose.ui.graphics.Color
  import androidx.compose.ui.text.font.FontWeight
  import androidx.compose.ui.unit.dp
  import androidx.compose.ui.unit.sp

  enum class AppTheme(
      val displayName: String,
      val primaryAccent: Color,
      val gradientColors: List<Color>
  ) {
      DARK_KNIGHT(
          "Dark Knight",
          Color(0xFF3B82F6), // Blue
          listOf(Color(0xFF181824), Color(0xFF0C0C12))
      ),
      CYBERPUNK(
          "Cyberpunk Neon",
          Color(0xFFEC4899), // Pink
          listOf(Color(0xFF1E1035), Color(0xFF090314))
      ),
      PASTEL_GOLD(
          "Pastel Gold",
          Color(0xFFF59E0B), // Amber/Gold
          listOf(Color(0xFF291E18), Color(0xFF120B07))
      )
  }

  @Composable
  fun SettingsScreen(
      modifier: Modifier = Modifier,
      currentTheme: AppTheme,
      onThemeChange: (AppTheme) -> Unit
  ) {
      Box(
          modifier = modifier
              .fillMaxSize()
              .background(Brush.verticalGradient(currentTheme.gradientColors))
      ) {
          Column(
              modifier = Modifier
                  .fillMaxSize()
                  .padding(16.dp)
          ) {
              Text(
                  text = "Settings",
                  style = MaterialTheme.typography.headlineMedium,
                  color = Color.White,
                  modifier = Modifier.padding(bottom = 24.dp)
              )

              Text(
                  text = "TEMA PREMIUM",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.Gray,
                  modifier = Modifier.padding(bottom = 12.dp)
              )

              Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                  AppTheme.values().forEach { theme ->
                      ThemeSelectionCard(
                          theme = theme,
                          isSelected = theme == currentTheme,
                          onClick = { onThemeChange(theme) }
                      )
                  }
              }
              
              Spacer(modifier = Modifier.height(72.dp))
          }
      }
  }

  @Composable
  fun ThemeSelectionCard(
      theme: AppTheme,
      isSelected: Boolean,
      onClick: () -> Unit
  ) {
      Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
              containerColor = if (isSelected) Color(0x22FFFFFF) else Color(0x0DFFFFFF)
          ),
          modifier = Modifier
              .fillMaxWidth()
              .clickable { onClick() }
              .border(
                  width = 1.dp,
                  color = if (isSelected) theme.primaryAccent else Color(0x1AFFFFFF),
                  shape = RoundedCornerShape(16.dp)
              )
      ) {
          Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically
          ) {
              // Bulatan Warna Aksen Mini
              Box(
                  modifier = Modifier
                      .size(24.dp)
                      .background(theme.primaryAccent, shape = RoundedCornerShape(12.dp))
              )
              Spacer(modifier = Modifier.width(16.dp))
              Column {
                  Text(
                      text = theme.displayName,
                      color = Color.White,
                      fontWeight = FontWeight.Bold,
                      fontSize = 16.sp
                  )
                  Text(
                      text = "Ubah latar belakang dan aksen tombol",
                      color = Color.Gray,
                      fontSize = 12.sp
                  )
              }
          }
      }
  }
  ```

- [ ] **Step 2: Commit perubahan**
  ```bash
  git add app/src/main/java/com/mybini/app/ui/screens/SettingsScreen.kt
  git commit -m "feat: implement SettingsScreen and dynamic AppTheme values"
  ```

---

### Task 8.2: MainActivity Theme State Integration

**Files:**
- Modify: `app/src/main/java/com/mybini/app/MainActivity.kt`

**Interfaces:**
- Consumes: `SettingsScreen`, `AppTheme`
- Produces: State `currentTheme` dinamis di tingkat root dan memperbarui warna primer/navigasi.

- [ ] **Step 1: Perbarui `MainActivity.kt`**
  
  Integrasikan state `currentTheme` di tingkat root, teruskan warna primer ke semua tombol & ikon di navigasi bar bawah.

- [ ] **Step 2: Commit perubahan**
  ```bash
  git add app/src/main/java/com/mybini/app/MainActivity.kt
  git commit -m "feat: integrate dynamic theme updates to MainActivity navigation bar and content layout"
  ```
