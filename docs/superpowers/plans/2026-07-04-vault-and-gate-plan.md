# UI Bilik Rahasia & Kunci PIN "MY BINI" Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mengimplementasikan gerbang keamanan PIN ganda (PIN Utama vs PIN Umpan/Decoy) dan layar galeri media terenkripsi Bilik Rahasia (Private Vault Screen) di Jetpack Compose.

**Architecture:** Menerapkan keypad numerik kustom, validasi hash PIN, instansiasi database terpisah secara dinamis berdasarkan jenis PIN, serta integrasi pemutar video terenkripsi luring.

**Tech Stack:** Kotlin, Jetpack Compose, Room DB, SQLCipher.

## Global Constraints
- Target platform: Android 14 (API 34) dan di atasnya (Minimum SDK = 34).
- Keamanan: Penyimpanan hash PIN di SharedPreferences terenkripsi (atau hashing dengan PBKDF2), database SQLCipher terpisah untuk data asli vs umpan.

---

### Task 5.1: Vault Screen UI & Passcode Gate Integration

**Files:**
- Create: `app/src/main/java/com/mybini/app/ui/screens/VaultScreen.kt`

**Interfaces:**
- Consumes: `MyBiniDatabase`, `SecurityManager`, `VideoPlayerScreen`
- Produces: `VaultScreen` Composable untuk mengamankan data dan memutar video terenkripsi.

- [ ] **Step 1: Buat berkas Composable `VaultScreen.kt`**
  
  Tulis berkas `app/src/main/java/com/mybini/app/ui/screens/VaultScreen.kt` yang mengurus gerbang PIN, PIN Umpan, dan penjelajahan media terenkripsi:
  ```kotlin
  package com.mybini.app.ui.screens

  import android.content.Context
  import android.net.Uri
  import androidx.compose.foundation.background
  import androidx.compose.foundation.border
  import androidx.compose.foundation.clickable
  import androidx.compose.foundation.layout.*
  import androidx.compose.foundation.lazy.grid.GridCells
  import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
  import androidx.compose.foundation.lazy.grid.items
  import androidx.compose.foundation.shape.RoundedCornerShape
  import androidx.compose.material.icons.Icons
  import androidx.compose.material.icons.filled.Delete
  import androidx.compose.material.icons.filled.Lock
  import androidx.compose.material.icons.filled.PlayArrow
  import androidx.compose.material3.*
  import androidx.compose.runtime.*
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.graphics.Brush
  import androidx.compose.ui.graphics.Color
  import androidx.compose.ui.platform.LocalContext
  import androidx.compose.ui.text.font.FontWeight
  import androidx.compose.ui.text.style.TextAlign
  import androidx.compose.ui.unit.dp
  import androidx.compose.ui.unit.sp
  import com.mybini.app.data.database.MyBiniDatabase
  import com.mybini.app.data.database.VaultItem
  import com.mybini.app.data.downloader.SecurityManager
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.launch
  import kotlinx.coroutines.withContext
  import java.io.File

  @Composable
  fun VaultScreen(
      modifier: Modifier = Modifier
  ) {
      val context = LocalContext.current
      val scope = rememberCoroutineScope()

      val sharedPref = remember { context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE) }
      
      var isPinCreated by remember { mutableStateOf(sharedPref.contains("real_pin_hash")) }
      var isAuthenticated by remember { mutableStateOf(false) }
      var isDecoyMode by remember { mutableStateOf(false) }
      
      var pinInput by remember { mutableStateOf("") }
      var pinSetupStep by remember { mutableStateOf(1) } // 1: Setup Real, 2: Setup Decoy
      var tempRealPin by remember { mutableStateOf("") }

      var vaultItems by remember { mutableStateOf(emptyList<VaultItem>()) }
      var activePlayUri by remember { mutableStateOf<Uri?>(null) }
      
      // Kunci enkripsi database hasil derivasi PIN
      var derivedDbKey by remember { mutableStateOf(ByteArray(32)) }

      // DAO Database yang aktif
      var activeDatabase: MyBiniDatabase? by remember { mutableStateOf(null) }

      fun loadVaultContent(passphrase: ByteArray, isDecoy: Boolean) {
          derivedDbKey = passphrase
          val dbName = if (isDecoy) "mybini_decoy.db" else "mybini_secure.db"
          activeDatabase = MyBiniDatabase.getDatabase(context, passphrase, dbName)
          
          scope.launch(Dispatchers.IO) {
              val items = activeDatabase?.vaultDao()?.getAllVaultItems() ?: emptyList()
              withContext(Dispatchers.Main) {
                  vaultItems = items
                  isAuthenticated = true
                  isDecoyMode = isDecoy
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
          if (!isAuthenticated) {
              // Layar Pembuatan / Input PIN
              Column(
                  modifier = Modifier
                      .fillMaxSize()
                      .padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center
              ) {
                  Text(
                      text = if (!isPinCreated) "Buat PIN Bilik Rahasia" else "Masukkan PIN Anda",
                      fontSize = 24.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                      textAlign = TextAlign.Center
                  )
                  
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                      text = if (!isPinCreated) {
                          if (pinSetupStep == 1) "Langkah 1: Masukkan PIN Utama Anda" 
                          else "Langkah 2: Masukkan PIN Umpan (Decoy)"
                      } else "PIN ganda aktif untuk keamanan optimal",
                      fontSize = 14.sp,
                      color = Color.Gray
                  )

                  Spacer(modifier = Modifier.height(32.dp))
                  
                  // Tampilan Indicator PIN bulat
                  Row(
                      horizontalArrangement = Arrangement.spacedBy(16.dp),
                      modifier = Modifier.padding(bottom = 32.dp)
                  ) {
                      for (i in 1..4) {
                          val isFilled = i <= pinInput.length
                          Box(
                              modifier = Modifier
                                  .size(16.dp)
                                  .background(
                                      color = if (isFilled) MaterialTheme.colorScheme.primary else Color(0x33FFFFFF),
                                      shape = RoundedCornerShape(8.dp)
                                  )
                          )
                      }
                  }

                  // Custom Numeric Keypad
                  CustomNumericKeypad(
                      onNumberClick = { num ->
                          if (pinInput.length < 4) {
                              pinInput += num
                              
                              if (pinInput.length == 4) {
                                  // Evaluasi PIN saat lengkap 4 digit
                                  if (!isPinCreated) {
                                      // Setup Mode
                                      if (pinSetupStep == 1) {
                                          tempRealPin = pinInput
                                          pinInput = ""
                                          pinSetupStep = 2
                                      } else {
                                          val realHash = tempRealPin.hashCode().toString()
                                          val decoyHash = pinInput.hashCode().toString()
                                          sharedPref.edit()
                                              .putString("real_pin_hash", realHash)
                                              .putString("decoy_pin_hash", decoyHash)
                                              .apply()
                                          
                                          isPinCreated = true
                                          // Derivasi kunci untuk database utama
                                          val salt = SecurityManager.generateSalt()
                                          val derivedKey = SecurityManager.deriveKeyFromPasscode(tempRealPin.toCharArray(), salt)
                                          loadVaultContent(derivedKey, false)
                                      }
                                  } else {
                                      // Input PIN Mode
                                      val inputHash = pinInput.hashCode().toString()
                                      val realHash = sharedPref.getString("real_pin_hash", "")
                                      val decoyHash = sharedPref.getString("decoy_pin_hash", "")
                                      
                                      val salt = SecurityManager.generateSalt()
                                      val derivedKey = SecurityManager.deriveKeyFromPasscode(pinInput.toCharArray(), salt)
                                      
                                      if (inputHash == realHash) {
                                          loadVaultContent(derivedKey, false)
                                      } else if (inputHash == decoyHash) {
                                          loadVaultContent(derivedKey, true)
                                      } else {
                                          // PIN Salah, reset input
                                          pinInput = ""
                                      }
                                  }
                              }
                          }
                      },
                      onDeleteClick = {
                          if (pinInput.isNotEmpty()) {
                              pinInput = pinInput.dropLast(1)
                          }
                      }
                  )
              }
          } else {
              // Galeri Bilik Rahasia setelah login sukses
              Column(
                  modifier = Modifier
                      .fillMaxSize()
                      .padding(16.dp)
              ) {
                  Row(
                      modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                  ) {
                      Text(
                          text = if (isDecoyMode) "Bilik Rahasia (Umpan)" else "Bilik Rahasia",
                          style = MaterialTheme.typography.headlineMedium,
                          color = Color.White
                      )
                      IconButton(onClick = { 
                          isAuthenticated = false 
                          pinInput = ""
                          activeDatabase = null
                      }) {
                          Icon(Icons.Default.Lock, contentDescription = "Kunci Kembali", tint = Color.White)
                      }
                  }

                  if (vaultItems.isEmpty()) {
                      Box(
                          modifier = Modifier.weight(1f).fillMaxWidth(),
                          contentAlignment = Alignment.Center
                      ) {
                          Text("Tidak ada berkas rahasia.", color = Color.Gray)
                      }
                  } else {
                      LazyVerticalGrid(
                          columns = GridCells.Fixed(2),
                          horizontalArrangement = Arrangement.spacedBy(12.dp),
                          verticalArrangement = Arrangement.spacedBy(12.dp),
                          modifier = Modifier.weight(1f)
                      ) {
                          items(vaultItems) { item ->
                              VaultItemCard(
                                  item = item,
                                  onClick = { activePlayUri = Uri.fromFile(File(item.encryptedFilePath)) },
                                  onDelete = {
                                      scope.launch(Dispatchers.IO) {
                                          activeDatabase?.vaultDao()?.deleteVaultItem(item)
                                          val file = File(item.encryptedFilePath)
                                          if (file.exists()) file.delete()
                                          
                                          val updated = activeDatabase?.vaultDao()?.getAllVaultItems() ?: emptyList()
                                          withContext(Dispatchers.Main) {
                                              vaultItems = updated
                                          }
                                      }
                                  }
                              )
                          }
                      }
                  }
                  Spacer(modifier = Modifier.height(72.dp))
              }

              // Pemutar video langsung mendekripsi byte di RAM (ExoPlayer Secure Player)
              activePlayUri?.let { uri ->
                  // Mencari metadata item yang pas untuk mengambil IV
                  val currentItem = vaultItems.firstOrNull { File(it.encryptedFilePath).absolutePath == uri.path }
                  val ivBytes = currentItem?.initVector?.let { android.util.Base64.decode(it, android.util.Base64.DEFAULT) } ?: ByteArray(12)
                  
                  VideoPlayerScreen(
                      mediaUri = uri,
                      isEncrypted = true,
                      encryptionKey = derivedDbKey,
                      encryptionIv = ivBytes,
                      onClose = { activePlayUri = null }
                  )
              }
          }
      }
  }

  @Composable
  fun VaultItemCard(
      item: VaultItem,
      onClick: () -> Unit,
      onDelete: () -> Unit
  ) {
      Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF)),
          modifier = Modifier
              .fillMaxWidth()
              .aspectRatio(1f)
              .clickable { onClick() }
      ) {
          Box(modifier = Modifier.fillMaxSize()) {
              Column(
                  modifier = Modifier
                      .fillMaxSize()
                      .padding(16.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center
              ) {
                  Icon(
                      Icons.Default.PlayArrow,
                      contentDescription = "Play Secure",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(48.dp)
                  )
                  Spacer(modifier = Modifier.height(12.dp))
                  Text(
                      text = item.originalTitle,
                      color = Color.White,
                      fontSize = 14.sp,
                      maxLines = 2,
                      textAlign = TextAlign.Center
                  )
              }
              IconButton(
                  onClick = onDelete,
                  modifier = Modifier.align(Alignment.TopEnd)
              ) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
              }
          }
      }
  }

  @Composable
  fun CustomNumericKeypad(
      onNumberClick: (String) -> Unit,
      onDeleteClick: () -> Unit
  ) {
      val buttons = listOf(
          listOf("1", "2", "3"),
          listOf("4", "5", "6"),
          listOf("7", "8", "9"),
          listOf("", "0", "Hapus")
      )

      Column(
          verticalArrangement = Arrangement.spacedBy(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
      ) {
          for (row in buttons) {
              Row(
                  horizontalArrangement = Arrangement.spacedBy(24.dp),
                  modifier = Modifier.fillMaxWidth(0.8f)
              ) {
                  for (label in row) {
                      Box(
                          modifier = Modifier
                              .weight(1f)
                              .aspectRatio(1.5f)
                              .background(Color(0x0DFFFFFF), shape = RoundedCornerShape(12.dp))
                              .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                              .clickable {
                                  if (label == "Hapus") {
                                      onDeleteClick()
                                  } else if (label.isNotEmpty()) {
                                      onNumberClick(label)
                                  }
                              },
                          contentAlignment = Alignment.Center
                      ) {
                          if (label.isNotEmpty()) {
                              Text(
                                  text = label,
                                  fontSize = 20.sp,
                                  fontWeight = FontWeight.Bold,
                                  color = Color.White
                              )
                          }
                      }
                  }
              }
          }
      }
  }
  ```

- [ ] **Step 2: Commit perubahan**
  ```bash
  git add app/src/main/java/com/mybini/app/ui/screens/VaultScreen.kt
  git commit -m "feat: implement secure VaultScreen with decoy pin gate and numerical keypad"
  ```
