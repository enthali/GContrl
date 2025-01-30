package de.drachenfels.gcontrl.ui.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun MqttConfigurationSection(
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