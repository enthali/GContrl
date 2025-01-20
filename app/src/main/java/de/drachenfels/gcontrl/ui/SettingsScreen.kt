package de.drachenfels.gcontrl.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import de.drachenfels.gcontrl.utils.LogConfig
import de.drachenfels.gcontrl.utils.AndroidLogger

private val logger = AndroidLogger()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mqttServer by remember { mutableStateOf("") }
    var mqttUser by remember { mutableStateOf("") }
    var mqttPassword by remember { mutableStateOf("") }
    
    var enableDebugMqtt by remember { mutableStateOf(LogConfig.ENABLE_DEBUG_MQTT) }
    var enableDebugSettings by remember { mutableStateOf(LogConfig.ENABLE_DEBUG_SETTINGS) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // MQTT Configuration
            Text(
                text = "MQTT Configuration",
                style = MaterialTheme.typography.titleMedium
            )
            
            OutlinedTextField(
                value = mqttServer,
                onValueChange = { 
                    mqttServer = it
                    logger.d(LogConfig.TAG_SETTINGS, "MQTT Server changed to: $it")
                },
                label = { Text("MQTT Server") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = mqttUser,
                onValueChange = { 
                    mqttUser = it
                    logger.d(LogConfig.TAG_SETTINGS, "MQTT User changed")
                },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = mqttPassword,
                onValueChange = { 
                    mqttPassword = it
                    logger.d(LogConfig.TAG_SETTINGS, "MQTT Password changed")
                },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            // Debug Settings
            Text(
                text = "Debug Settings",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enable MQTT Debug")
                Switch(
                    checked = enableDebugMqtt,
                    onCheckedChange = { 
                        enableDebugMqtt = it
                        LogConfig.ENABLE_DEBUG_MQTT = it
                        logger.d(LogConfig.TAG_SETTINGS, "MQTT Debug enabled: $it")
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enable Settings Debug")
                Switch(
                    checked = enableDebugSettings,
                    onCheckedChange = { 
                        enableDebugSettings = it
                        LogConfig.ENABLE_DEBUG_SETTINGS = it
                        logger.d(LogConfig.TAG_SETTINGS, "Settings Debug enabled: $it")
                    }
                )
            }

            // Save Button
            Button(
                onClick = {
                    logger.d(LogConfig.TAG_SETTINGS, "Saving settings")
                    // TODO: Save settings to DataStore
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Save")
            }
        }
    }
}