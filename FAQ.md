# Frequently Asked Questions (FAQ)

## General Questions

### What is GaragePilot?
GaragePilot is a simple Android Automotive OS application that lets you control your garage door directly from your car's dashboard via MQTT.

### How does it work?
The system consists of:
- The GaragePilot app on your Android Automotive OS device
- An MQTT server for message handling
- A compatible garage door controller that accepts MQTT commands

### Is this app available on the Play Store?
Yes, the app is available on the Play Store for Android Automotive OS devices.

## Setup & Installation

### What do I need to set this up?
You need:
- An Android Automotive OS device (e.g., Polestar, Volvo)
- An MQTT server (like HiveMQ)
- A compatible garage door controller that supports MQTT

### What server options do I have?
You can:
- Use public MQTT services like HiveMQ
- Host your own MQTT server
- Use any MQTT broker that supports username/password authentication

## Privacy & Security

### What data does the app collect?
The app only sends door control commands via MQTT. No other data is collected or transmitted.

### Is the system secure?
The system uses:
- MQTT authentication with username/password
- Encrypted MQTT communication
- No sensitive data processing

## Troubleshooting

### The door doesn't respond to commands
Check:
- Is your MQTT server reachable?
- Are your MQTT credentials correct?
- Is your door controller connected to MQTT?

### How can I test the connection?
Use the connection test feature in the settings screen.

## Development

### What's on the roadmap?
Planned improvements include:
- Enhanced security features
- Additional MQTT configuration options
- Expanded test coverage
- Better documentation