package com.cheese.watchlist.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cheese.watchlist.server.PasscodeGenerator
import com.cheese.watchlist.server.WebServerManager
import com.cheese.watchlist.ui.theme.StatusDropped
import com.cheese.watchlist.ui.theme.StatusWatched

@Composable
fun PcManagerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val serverState by WebServerManager.state.collectAsState()

    var showBanner by remember { mutableStateOf(true) }
    var passcodeEnabled by remember { mutableStateOf(false) }
    var passcodeCode by remember { mutableStateOf("") }
    var lastKnownRunning by remember { mutableStateOf(false) }

    // Fire the "started"/"stopped" toast exactly once per transition, and surface any error.
    LaunchedEffect(serverState.isRunning, serverState.error) {
        if (serverState.isRunning && !lastKnownRunning) {
            Toast.makeText(context, "PC Manager started", Toast.LENGTH_SHORT).show()
        } else if (!serverState.isRunning && lastKnownRunning) {
            Toast.makeText(context, "PC Manager stopped", Toast.LENGTH_SHORT).show()
        }
        serverState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
        lastKnownRunning = serverState.isRunning
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PC Manager") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (showBanner) {
                item {
                    GuideBanner(onDismiss = { showBanner = false })
                }
            }

            item {
                ServerStatusSection(
                    isRunning = serverState.isRunning,
                    onStart = {
                        val code = if (passcodeEnabled) passcodeCode else ""
                        WebServerManager.start(context, passcodeEnabled, code)
                    },
                    onStop = { WebServerManager.stop() }
                )
            }

            if (serverState.isRunning && serverState.url != null) {
                item {
                    AddressSection(url = serverState.url!!, context = context)
                }
            }

            item {
                PasscodeSection(
                    enabled = passcodeEnabled,
                    code = passcodeCode,
                    serverRunning = serverState.isRunning,
                    onToggle = { checked ->
                        passcodeEnabled = checked
                        passcodeCode = if (checked) PasscodeGenerator.generate() else ""
                    },
                    onRegenerate = { passcodeCode = PasscodeGenerator.generate() }
                )
            }
        }
    }
}

@Composable
private fun GuideBanner(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row {
                Icon(Icons.Outlined.Computer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Row { Icon(Icons.Outlined.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        androidx.compose.foundation.layout.Spacer(Modifier.size(10.dp))
        Text(
            "Manage Watchlist from your PC",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(4.dp))
        Text(
            "Connect your computer and Android phone to the same WiFi network, then open the address below in your browser.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(10.dp))
        Text(
            "Your PC and Android device must be connected to the same WiFi network. Only use this address on a trusted network.",
            style = MaterialTheme.typography.bodyMedium,
            color = StatusDropped
        )
    }
}

@Composable
private fun ServerStatusSection(isRunning: Boolean, onStart: () -> Unit, onStop: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Text("PC Manager", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isRunning) StatusWatched else MaterialTheme.colorScheme.onSurfaceVariant)
            ) {}
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
            Text(
                if (isRunning) "PC Manager is running" else "Offline",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        androidx.compose.foundation.layout.Spacer(Modifier.size(14.dp))
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (isRunning) {
                Button(
                    onClick = onStop,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusDropped)
                ) {
                    Text("Stop PC Manager")
                }
            } else {
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Start PC Manager")
                }
            }
        }
    }
}

@Composable
private fun AddressSection(url: String, context: Context) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Text(
            "PC Browser Address",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
        Text(url, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        androidx.compose.foundation.layout.Spacer(Modifier.size(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Watchlist PC Manager address", url))
                    Toast.makeText(context, "Address copied", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
                Text("Copy")
            }
            Button(
                onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: Exception) {
                        Toast.makeText(context, "Couldn't open browser", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Icon(
                    Icons.Outlined.OpenInBrowser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(18.dp)
                )
                androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
                Text("Open in Browser", color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

@Composable
private fun PasscodeSection(
    enabled: Boolean,
    code: String,
    serverRunning: Boolean,
    onToggle: (Boolean) -> Unit,
    onRegenerate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Require Passcode", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                enabled = !serverRunning,
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
            )
        }

        AnimatedVisibility(
            visible = enabled,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                androidx.compose.foundation.layout.Spacer(Modifier.size(14.dp))
                Text(
                    "PC Manager Passcode",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        code,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onRegenerate, enabled = !serverRunning) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = "Regenerate code",
                            tint = if (serverRunning) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                androidx.compose.foundation.layout.Spacer(Modifier.size(10.dp))
                Text(
                    "Anyone connected to your local network who knows this code can access PC Manager.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (serverRunning) {
                    androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                    Text(
                        "Stop PC Manager to change the passcode.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (!enabled) {
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
            Text(
                "PC Manager can be accessed on your local network without a passcode.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
