package de.drachenfels.gcontrl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.drachenfels.gcontrl.utils.LogConfig
import de.drachenfels.gcontrl.utils.AndroidLogger

private val logger = AndroidLogger()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var doorState by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GaragePilot") },
                actions = {
                    // Settings button
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection status
            ConnectionStatus(isConnected)

            // Door status
            DoorStatus(doorState)

            // Control button
            Button(
                onClick = {
                    doorState = !doorState
                    logger.d(LogConfig.TAG_MAIN, "Door state changed to: $doorState")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (doorState) "Close Door" else "Open Door")
            }
        }
    }
}

@Composable
private fun ConnectionStatus(isConnected: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = if (isConnected) Icons.Default.Settings else Icons.Default.Settings,
            contentDescription = "Connection Status",
            tint = if (isConnected) MaterialTheme.colorScheme.primary 
                   else MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (isConnected) "Connected" else "Disconnected")
    }
}

@Composable
private fun DoorStatus(isOpen: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .run {
                    if (isOpen) {
                        this.background(MaterialTheme.colorScheme.errorContainer)
                    } else {
                        this.background(MaterialTheme.colorScheme.primaryContainer)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isOpen) "Door Open" else "Door Closed",
                style = MaterialTheme.typography.headlineMedium,
                color = if (isOpen) MaterialTheme.colorScheme.onErrorContainer 
                        else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}