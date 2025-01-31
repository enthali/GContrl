package de.drachenfels.gcontrl.services

import de.drachenfels.gcontrl.utils.AndroidLogger
import de.drachenfels.gcontrl.utils.LogConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val speed: Float,
    val distanceToGarage: Double? = null
)

object LocationDataRepository {
    private val logger = AndroidLogger()
    private val _locationUpdates = MutableStateFlow<LocationData?>(null)
    val locationUpdates: StateFlow<LocationData?> = _locationUpdates.asStateFlow()

    suspend fun emitLocation(locationData: LocationData) {
        logger.d(LogConfig.TAG_LOCATION, "Emitting location: ${locationData.latitude}, ${locationData.longitude}, speed: ${locationData.speed}")
        _locationUpdates.value = locationData
    }
}