# Frequently Asked Questions (FAQ)

## General Questions

### What is GContrl?
GContrl is an Android application that automatically controls your garage door based on your location. It opens the door when you arrive home and closes it when you leave.

### How does the system work?
The system uses several components working together:
- Your Android phone with the GContrl app for location detection
- An MQTT server for message handling
- A compatible garage door controller
- Google Automotive Services integration for enhanced vehicle awareness

### Is this app available on the Play Store?
No, this is a private hobby project and needs to be built and installed manually.

## Setup & Installation

### What do I need to set this up?
Current setup requires:
- An MQTT server (either self-hosted on Raspberry Pi or public services like HiveMQ)
- A compatible garage door controller (MEROS or upcoming DIY ESP32-based solution)
- An Android phone
- A car with Google Automotive Services (e.g., Polestar)

### What server options do I have?
You can choose between:
1. Self-hosted setup:
   - Run your own MQTT server on a Raspberry Pi
   - Complete control over your data
   - More complex initial setup

2. Cloud-based setup (coming soon):
   - Use public MQTT services like HiveMQ
   - Easier to set up
   - No need for own server hardware

### What garage door controller options are available?
Current and planned options include:
1. MEROS garage door opener (current)
2. DIY ESP32-based solution (upcoming):
   - More cost-effective
   - Fully customizable
   - Open-source hardware design

## Privacy & Security

### What data does the app collect?
The app only processes location data locally on your device. It sends exclusively open and close commands to your garage door system. No location data is transmitted to any server.

### Is the system secure?
The system uses standard security measures like:
- MQTT authentication
- Encrypted communication
- Local processing of sensitive data
Users should follow best practices for network security, especially when using public MQTT servers.

## Troubleshooting

### The door doesn't open automatically when I arrive
Check:
- Is location service enabled on your phone?
- Is the app running in the background?
- Is your MQTT server reachable?
- Is your home location correctly configured?

### The door doesn't close when I leave
Verify:
- Is your departure zone correctly configured?
- Are all network components reachable?
- Is the door controller responding to commands?

### How can I manually control the door?
The app provides manual controls to open and close the door regardless of your location.

## Development

### How can I contribute to the project?
While this is primarily a private project, feedback and suggestions are welcome. The main areas that need improvement are:
- Documentation and installation guides
- Test coverage
- Security enhancements
- ESP32-based controller development

### What's on the roadmap?
Planned improvements include:
- ESP32-based DIY garage door controller
- Support for public MQTT servers
- Simplified setup process without Node-RED
- Enhanced security features
- Expanded test coverage
- Better documentation
