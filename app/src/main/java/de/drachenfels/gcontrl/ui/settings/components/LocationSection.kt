package de.drachenfels.gcontrl.ui.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LocationAutomationSection(
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    location: Pair<Double, Double>?,
    onSetCurrentLocation: () -> Unit,
    triggerDistance: Int,
    onTriggerDistanceChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "Location Automation",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Enable/Disable Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable Location Automation")
            Switch(
                checked = isEnabled,
                onCheckedChange = onEnabledChange
            )
        }

        // Only show these when enabled
        if (isEnabled) {
            Spacer(modifier = Modifier.height(16.dp))

            // Garage Location
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Garage Location: ")
                    Text(
                        text = location?.let { 
                            String.format("%.6f°, %.6f°", it.first, it.second)
                        } ?: "No location set",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (location == null) 
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onSetCurrentLocation,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Set Current Location")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Trigger Distance
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Trigger Distance: ")
                    Text(
                        text = "$triggerDistance meters",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = triggerDistance.toFloat(),
                    onValueChange = { onTriggerDistanceChange(it.toInt()) },
                    valueRange = 10f..200.1f,
                    steps = 18, // (200-10)/10 - 1 = 18  10m intervals
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "10m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "200m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}