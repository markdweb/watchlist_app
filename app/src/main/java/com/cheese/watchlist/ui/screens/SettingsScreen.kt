package com.cheese.watchlist.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cheese.watchlist.viewmodel.WatchlistViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: WatchlistViewModel,
    darkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onManageGenres: () -> Unit,
    onManageSources: () -> Unit,
    onPcManager: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = viewModel.exportBackupJson()
                try {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    Toast.makeText(context, "Exported", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (json != null) {
                        viewModel.importBackupJson(json)
                    } else {
                        Toast.makeText(context, "Couldn't read file", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Import failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SettingsSection(title = "Appearance") {
                    SettingsRow(
                        icon = if (darkTheme) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                        label = if (darkTheme) "Dark theme" else "Light theme",
                        trailing = {
                            Switch(
                                checked = darkTheme,
                                onCheckedChange = onToggleDarkTheme,
                                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                            )
                        }
                    )
                }
            }

            item {
                SettingsSection(title = "Organize") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SettingsRow(
                            icon = Icons.Outlined.Sell,
                            label = "Manage Genres & Tags",
                            onClick = onManageGenres,
                            trailing = {
                                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        )
                        SettingsRow(
                            icon = Icons.Outlined.OpenInNew,
                            label = "Manage Sources",
                            onClick = onManageSources,
                            trailing = {
                                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "Backup") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SettingsRow(
                            icon = Icons.Outlined.SaveAlt,
                            label = "Export Data",
                            onClick = { exportLauncher.launch("watchlist_backup.json") },
                            trailing = {
                                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        )
                        SettingsRow(
                            icon = Icons.Outlined.FileUpload,
                            label = "Import Data",
                            onClick = { importLauncher.launch(arrayOf("application/json")) },
                            trailing = {
                                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "Connectivity") {
                    SettingsRow(
                        icon = Icons.Outlined.Computer,
                        label = "PC Manager",
                        onClick = onPcManager,
                        trailing = {
                            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        content()
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
            Text(label, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyLarge)
        }
        trailing()
    }
}
