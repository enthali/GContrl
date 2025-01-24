package de.drachenfels.gcontrl.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.drachenfels.gcontrl.ui.theme.GContrlTheme
import de.drachenfels.gcontrl.utils.LogConfig
import de.drachenfels.gcontrl.utils.AndroidLogger
import de.drachenfels.gcontrl.mqtt.MQTTService
import kotlinx.coroutines.*

// MQTT configuration
private const val MQTT_TIMEOUT = 5000L  // 5 seconds timeout
private const val PREFS_NAME = "GContrlPrefs"
private const val KEY_MQTT_SERVER = "mqtt_server"
private const val KEY_MQTT_USERNAME = "mqtt_username"
private const val KEY_MQTT_PASSWORD = "mqtt_password"
private const val KEY_CONFIG_VALID = "mqtt_config_valid"

private val logger = AndroidLogger()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    mqttService: MQTTService,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Disconnect any existing connection when entering settings
    mqttService.disconnect()

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    
    // Load current values from preferences
    var mqttServer by remember { mutableStateOf(prefs.getString(KEY_MQTT_SERVER, "") ?: "") }
    var mqttUser by remember { mutableStateOf(prefs.getString(KEY_MQTT_USERNAME, "") ?: "") }
    var mqttPassword by remember { mutableStateOf(prefs.getString(KEY_MQTT_PASSWORD, "") ?: "") }
    var isTestingConnection by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    SettingsScreenContent(
        mqttServer = mqttServer,
        onMqttServerChange = { mqttServer = it },
        mqttUser = mqttUser,
        onMqttUserChange = { mqttUser = it },
        mqttPassword = mqttPassword,
        onMqttPasswordChange = { mqttPassword = it },
        isTestingConnection = isTestingConnection,
        onSaveAndTest = {
            scope.launch {
                isTestingConnection = true
                try {
                    // First save the new configuration
                    prefs.edit()
                        .putString(KEY_MQTT_SERVER, mqttServer)
                        .putString(KEY_MQTT_USERNAME, mqttUser)
                        .putString(KEY_MQTT_PASSWORD, mqttPassword)
                        .putBoolean(KEY_CONFIG_VALID, false)  // Initially mark as invalid
                        .apply()

                    // Then test connection
                    withTimeout(MQTT_TIMEOUT) {
                        val connected = mqttService.connect()
                        if (connected) {
                            // If connect was successful, mark config as valid
                            prefs.edit().putBoolean(KEY_CONFIG_VALID, true).apply()
                            Toast.makeText(context, "Connection successful", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        } else {
                            Toast.makeText(context, "Connection failed - could not establish connection", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    logger.e(LogConfig.TAG_SETTINGS, "Connection timeout after ${MQTT_TIMEOUT}ms")
                    Toast.makeText(context, "Connection timeout - please try again", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    logger.e(LogConfig.TAG_SETTINGS, "Connection test failed", e)
                    Toast.makeText(context, "Connection failed: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isTestingConnection = false
                }
            }
        },
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenContent(
    mqttServer: String,
    onMqttServerChange: (String) -> Unit,
    mqttUser: String,
    onMqttUserChange: (String) -> Unit,
    mqttPassword: String,
    onMqttPasswordChange: (String) -> Unit,
    isTestingConnection: Boolean,
    onSaveAndTest: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        // TODO find other icon for back (Warning:(116, 44) 'ArrowBack: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Filled.ArrowBack)
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
            Text(
                text = "MQTT Configuration",
                style = MaterialTheme.typography.titleMedium
            )
            
            OutlinedTextField(
                value = mqttServer,
                onValueChange = onMqttServerChange,
                label = { Text("MQTT Server") },
                placeholder = { Text("example.hivemq.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = mqttUser,
                onValueChange = onMqttUserChange,
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = mqttPassword,
                onValueChange = onMqttPasswordChange,
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            if (isTestingConnection) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )
            }

            Button(
                onClick = onSaveAndTest,
                enabled = !isTestingConnection && mqttServer.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isTestingConnection) {
                    Text("Testing Connection...")
                } else {
                    Text("Save and Test Connection")
                }
            }

            Text(
                text = "Note: Settings will only be activated after a successful connection test",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=411dp,height=891dp,dpi=420,isRound=false,chinSize=0dp,orientation=portrait"
)
@Composable
fun SettingsScreenPreview() {
    GContrlTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // Preview-Version mit lokalem Context
            val context = LocalContext.current
            SettingsScreen(
                mqttService = MQTTService(context),
                onNavigateBack = { }
            )
        }
    }
}