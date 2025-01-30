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
import de.drachenfels.gcontrl.BuildConfig
import de.drachenfels.gcontrl.ui.theme.GContrlTheme
import de.drachenfels.gcontrl.utils.LogConfig
import de.drachenfels.gcontrl.utils.AndroidLogger
import de.drachenfels.gcontrl.mqtt.MQTTService
import kotlinx.coroutines.*
import java.lang.Thread.sleep

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
                mqttService.disconnect()
                sleep(500)
                isTestingConnection = true

                // Definiere einen separaten Job für den Verbindungsversuch
                val connectionJob = launch {
                    try {
                        // Konfiguration speichern
                        prefs.edit()
                            .putString(KEY_MQTT_SERVER, mqttServer)
                            .putString(KEY_MQTT_USERNAME, mqttUser)
                            .putString(KEY_MQTT_PASSWORD, mqttPassword)
                            .putBoolean(KEY_CONFIG_VALID, false)
                            .apply()

                        // Verbindung testen
                        val connected = mqttService.connect()
                        if (connected) {
                            prefs.edit().putBoolean(KEY_CONFIG_VALID, true).apply()
                            Toast.makeText(context, "Connection successful", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        } else {
                            Toast.makeText(context, "Connection failed - could not establish connection", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        logger.e(LogConfig.TAG_SETTINGS, "Connection test failed", e)
                        Toast.makeText(context, "Connection failed: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        isTestingConnection = false
                    }
                }

                // Setze einen Timer, der den Job nach MQTT_TIMEOUT cancelt
                delay(MQTT_TIMEOUT)
                if (connectionJob.isActive) {
                    connectionJob.cancel()
                    withContext(Dispatchers.Main) {
                        isTestingConnection = false
                        Toast.makeText(context, "Connection timeout - please try again", Toast.LENGTH_LONG).show()
                    }
                }
            }
        },
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@Composable
private fun MqttConfigurationSection(
    mqttServer: String,
    onMqttServerChange: (String) -> Unit,
    mqttUser: String,
    onMqttUserChange: (String) -> Unit,
    mqttPassword: String,
    onMqttPasswordChange: (String) -> Unit,
    isTestingConnection: Boolean,
    onSaveAndTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "MQTT Configuration",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = mqttServer,
            onValueChange = onMqttServerChange,
            label = { Text("MQTT Server") },
            placeholder = { Text("example.hivemq.com") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = mqttUser,
            onValueChange = onMqttUserChange,
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = mqttPassword,
            onValueChange = onMqttPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (isTestingConnection) {
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Note: Settings will only be activated after a successful connection test",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VersionInfoSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp)
    ) {
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "About",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) - ${BuildConfig.GIT_BRANCH}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Build Date: ${BuildConfig.BUILD_DATE}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "GitHub: github.com/enthali/GaragePilot",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
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
            // MQTT Configuration Section
            MqttConfigurationSection(
                mqttServer = mqttServer,
                onMqttServerChange = onMqttServerChange,
                mqttUser = mqttUser,
                onMqttUserChange = onMqttUserChange,
                mqttPassword = mqttPassword,
                onMqttPasswordChange = onMqttPasswordChange,
                isTestingConnection = isTestingConnection,
                onSaveAndTest = onSaveAndTest
            )

            // Add Spacer for better spacing
            Spacer(modifier = Modifier.weight(1f))

            // Add the version info section
            VersionInfoSection()
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