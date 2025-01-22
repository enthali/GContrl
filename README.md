# GaragePilot

A simple Android Automotive OS app for controlling your garage door via MQTT.

## Technical Details
- Package Name: de.drachenfels.gcontrl
- Target Platforms: Android Automotive OS & Android Phone
- UI Framework: Jetpack Compose
- Communication: MQTT (HiveMQ Client)

## Features

- Simple one-button garage door control
- Direct MQTT communication
- Status indication for door position
- Easy configuration through settings screen
- Works with Android Automotive OS

## System Requirements

- Android Automotive OS device
- MQTT server (e.g., HiveMQ)
- MQTT-compatible garage door controller

## Setup

1. Install the app on your Android Automotive OS device
2. Configure your MQTT server details in the settings
3. Connect your garage door controller to the same MQTT server
4. Control your garage door directly from your car

## Privacy & Security

- The app only sends basic door control commands via MQTT
- No location tracking or data collection
- Secure MQTT communication using username/password authentication

## Development

This is a private hobby project. The focus is on simplicity and reliability.
Feel free to use this as inspiration for your own projects.

### Version History
| Version | Comment |
| --- | --- |
| 27 | Simplified app to basic MQTT control |
| 1-26 | Previous versions (different approach) |