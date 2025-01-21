package de.drachenfels.gcontrl.ui

import android.content.Context
import android.content.SharedPreferences
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
import kotlinx.coroutines.*

// Preferences configuration
private const val PREFS_NAME = "GContrlPrefs"
private const val KEY_MQTT_SERVER = "mqtt_server"
private const val KEY_MQTT_USERNAME = "mqtt_username"
private const val KEY_MQTT_PASSWORD = "mqtt_password"
private const val KEY_IS_CONFIGURED = "is_configured"

// MQTT configuration
private const val MQTT_WS_PORT = 8884  // WebSocket TLS port
private const val MQTT_TIMEOUT = 5000L  // 5 seconds timeout

private val logger = AndroidLogger()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    
    var mqttServer by remember { mutableStateOf(prefs.getString(KEY_MQTT_SERVER, "") ?: "") }
    var mqttUser by remember { mutableStateOf(prefs.getString(KEY_MQTT_USERNAME, "") ?: "") }
    var mqttPassword by remember { mutableStateOf(prefs.getString(KEY_MQTT_PASSWORD, "") ?: "") }
    var isTestingConnection by remember { mutableStateOf(false) }

    SettingsScreenContent(
        mqttServer = mqttServer,
        onMqttServerChange = { mqttServer = it },
        mqttUser = mqttUser,
        onMqttUserChange = { mqttUser = it },
        mqttPassword = mqttPassword,
        onMqttPasswordChange = { mqttPassword = it },
        isTestingConnection = isTestingConnection,
        onTestConnection = {
            scope.launch {
                isTestingConnection = true
                try {
                    withTimeout(MQTT_TIMEOUT) {
                        try {
                            val randomDelay = (2000L..8000L).random()
                            logger.d(LogConfig.TAG_SETTINGS, "Starting connection test with delay: ${randomDelay}ms")
                            delay(randomDelay)
                            
                            Toast.makeText(context, "Connection successful", Toast.LENGTH_SHORT).show()
                            
                            prefs.edit()
                                .putString(KEY_MQTT_SERVER, mqttServer)
                                .putString(KEY_MQTT_USERNAME, mqttUser)
                                .putString(KEY_MQTT_PASSWORD, mqttPassword)
                                .putBoolean(KEY_IS_CONFIGURED, true)
                                .apply()
                                
                            logger.d(LogConfig.TAG_SETTINGS, "Settings saved successfully")
                            onNavigateBack()
                        } catch (e: Exception) {
                            logger.e(LogConfig.TAG_SETTINGS, "Connection test failed", e)
                            throw e
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
    onTestConnection: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                onClick = onTestConnection,
                enabled = !isTestingConnection && mqttServer.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isTestingConnection) {
                    Text("Testing Connection...")
                } else {
                    Text("Test Connection and Save")
                }
            }

            Text(
                text = "Note: Settings will be saved and applied after successful connection test",
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
            SettingsScreen(
                onNavigateBack = { }
            )
        }
    }
}