# GaragePilot

A simple Applicaiton for Android Phones and Android Automotive OS to controlling your garage door via MQTT.

## How it's done Technical Details
- UI Framework: Jetpack Compose
- Communication: MQTT (HiveMQ Client)
- Meeros Door controller (will be replaced soon by a DIY project)

## Features

- Simple one-button garage door control
- Direct MQTT communication
- Status indication for door position and movement
- Easy configuration through settings screen
- Works with Android Automotive OS

## System Requirements

- Android Phone or Android Automotive OS device with Android SDK 28 or newer
- MQTT server (e.g., HiveMQ)
- MQTT-compatible garage door controller (e.g., DIY project or Meeros Door Controler interfaced though Node Red or Home Assist)

## Setup

1. Install the app on your Android Automotive OS device
2. Pick an MQTT server (e.g., HiveMQ) and create a user and password
3. Setup your MQTT server details in the settings
4. Connect your garage door controller to the same MQTT server
5. Control your garage door directly from your car

## Privacy & Security

- The app only sends basic door control commands via MQTT
- Secure MQTT communication using username/password authentication with your preferred MQTT server
- The location information is only used on the device to create a door automation feature

## Development

This is a private hobby project. The focus is on simplicity and reliability.
Feel free to use this as inspiration for your own projects.

### Version History
| Version | Comment | Description |
| --- | --- | --- |
| 34 | Let's make the door move | Enhanced garage door movement visualization with animated icons. Opening and closing states are now represented by smooth animations. |
| 33 | A long way from home |
| 32 | New icon |
| 31 | Connect me |
| 30 | Set things right |
| 29 | Lets drive |
| 28 | New Live |
| 26 | Stabilization: Fix 'Abort during opening/closing action' |
| 25 | AGP Upgrade |
| 24 | Swap Car and Home pictograms (User Feedback) |
| 13 | Geo location services |
| 4 | Color theme update |
| 3 | Initial public release |
| 2 | Internal test release |
| 1 | First internal release |