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
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.mybini.app.data.downloader.BackupManager
import java.io.File
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer

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
    val context = LocalContext.current
    
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
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "CADANGAN & PEMULIHAN",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Button(
                onClick = {
                    val file = BackupManager.exportBackup(context)
                    if (file != null) {
                        Toast.makeText(context, "Cadangan sukses dibuat: ${file.name}", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Gagal membuat cadangan", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryAccent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Buat Cadangan Baru (.mybini)", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val backupDir = File(context.getExternalFilesDir(null), "backups")
                    val files = backupDir.listFiles { _, name -> name.endsWith(".mybini") }
                    val latestBackup = files?.maxByOrNull { it.lastModified() }
                    
                    if (latestBackup != null) {
                        val success = BackupManager.importBackup(context, latestBackup)
                        if (success) {
                            Toast.makeText(context, "Pemulihan cadangan sukses!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Gagal memulihkan cadangan", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Tidak ada berkas cadangan .mybini", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1EFFFFFF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Pulihkan Cadangan Terakhir", color = Color.White, fontWeight = FontWeight.Bold)
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
    val scale by animateFloatAsState(if (isSelected) 1.02f else 1.0f, label = "scale")

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0x22FFFFFF) else Color(0x0DFFFFFF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable { onClick() }
            .border(
                width = 1.5.dp,
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
