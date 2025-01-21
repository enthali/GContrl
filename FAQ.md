# Frequently Asked Questions (FAQ)

[... vorheriger Inhalt bleibt gleich bis zur MQTT-Struktur ...]

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

[... Rest der FAQ bleibt gleich ...]