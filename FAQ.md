# Frequently Asked Questions (FAQ)

### How is the MQTT communication structured?
We follow a simple topic/message pattern:

Topics:
- garage/command - Receives control commands ("open", "close", "stop")
- garage/state - Current door state (OPEN, CLOSED, OPENING, CLOSING, STOPPED)

This design:
- Uses a clean topic structure with meaningful messages
- Keeps app logic simple (just send commands and display state)
- Puts control logic in the ESPHome controller where it belongs
- Makes it easy to add additional clients
- Allows the controller to implement safety checks
- Makes the system more testable

### Why these architectural choices?

Our architecture follows these principles:
- Screens and their state management are combined for simplicity
- MQTT connection management is separated for lifecycle reasons
- ViewModels are not needed due to simple state management in Compose
- Settings are stored in SharedPreferences as it's sufficient for our needs
- Centralized logging through the Logger utility

This design:
- Keeps the codebase simple and maintainable
- Minimizes unnecessary abstractions
- Makes the app easier to test and debug
- Provides good separation of concerns where needed

### Why these technical choices?

- Jetpack Compose: Modern UI toolkit that simplifies UI development and maintenance
- HiveMQ: Reliable MQTT client with good Android support and active maintenance
- Dual platform support (Automotive OS & Phone): Allows testing on regular Android devices

This approach:
- Reduces development complexity
- Ensures good maintainability
- Makes testing easier on regular Android devices
- Uses well-supported, modern technologies

### How is debugging and logging handled?

Debug logging uses these flags:
- ENABLE_DEBUG (Master switch)
- ENABLE_DEBUG_MQTT
- ENABLE_DEBUG_SETTINGS

Standard Tags:
- TAG_MAIN = "GContrl"
- TAG_MQTT = "MQTT"
- TAG_SETTINGS = "Settings"

This design:
- Provides granular control over debug output
- Uses consistent tags for easy log filtering
- Follows Android logging best practices
- Ensures sensitive data is not logged in production