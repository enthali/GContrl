# GaragePilot

An Android application for smartphones and Android Automotive OS to control your garage door via MQTT. Developed as a hobby project to enable garage control directly from your car or smartphone.

## Features

- Simple one-button garage door control
- Direct MQTT communication
- Status indication for door position and movement
- Easy configuration through settings screen
- Android Automotive OS support
- Animated icons for door movements
- Support for portrait and landscape orientation
- Geo-location based automation

## Technical Details

- UI Framework: Jetpack Compose
- Communication: MQTT (HiveMQ Client)
- Garage Door Controller: Meeros (will be replaced by DIY project)
- Supported Orientations: Portrait and Landscape

## Installation & Setup

### For Users
best you contact the developer and ask him to publish a proper user guide :)

### For Developers
#### Prerequisites
- Android Studio Lady Bug or newer
- Android SDK 28 or higher
- Kotlin development environment

#### Build Process
1. Clone the repository:
```bash
git clone https://github.com/yourusername/GContrl.git
```
2. Open the project in Android Studio
3. Build the project using Android Studio's build system

## Privacy & Security

- Only basic door control commands via MQTT
- Secure MQTT communication using username/password authentication
- Location information is only used locally for door automation
- No cloud storage
- No data collection outside the app

## Project Status

- Actively in development
- Available on Google Play Store

This is a private hobby project focusing on simplicity and reliability.
Feel free to use it as inspiration for your own projects.


## Development

This project is open for contributions. For major changes, please open an issue first to discuss what you would like to change.


## Version History

| Version | Title | Description                                                                                 |
|---------|-------|---------------------------------------------------------------------------------------------|
| 39 | We like to work in the foreground | Added location to the Notification, and implement location automation in foreground service |
| 38 | Let's reconnect | New reconnect logic ensuring to stay connected                                              |
| 37 | We stay online | The automation now works even if the app is not in the foreground                           |
| 36 | What a wide view again | Portrait and landscape support - startup crash fixed                                        |
| 35 | What a wide view | Portrait and landscape support - non-functional                                             |
| 34 | Let's make the door move | Enhanced garage door movement visualization with animated icons                             |
| 33 | A long way from home | Geo-location features improvements                                                          |
| 32 | New icon | New app icon design                                                                         |
| 31 | Connect me | Improved MQTT connection                                                                    |
| 30 | Set things right | Bug fixes                                                                                   |
| 29 | Lets drive | Android Automotive OS optimizations                                                         |
| 28 | New Live | Important updates                                                                           |
| 26 | Stabilization | Fix: 'Abort during opening/closing action'                                                  |
| 25 | AGP Upgrade | Android Gradle Plugin update                                                                |
| 24 | UI Update | Swapped car and house pictograms (user feedback)                                            |
| 13 | Geo location | Added geo-location services                                                                 |
| 4 | Color theme | Color scheme update                                                                         |
| 3 | Public | Initial public release                                                                      |
| 2 | Test | Internal test release                                                                       |
| 1 | Initial | First internal release                                                                      |

## Support & Contact

Please use GitHub Issues for:
- Bug reports
- Feature requests
- Questions about the app
- General feedback

## System Requirements

- Android Smartphone or Android Automotive OS device with Android SDK 28 or newer
- MQTT server (e.g., HiveMQ)
- MQTT-compatible garage door controller (e.g., DIY project or Meeros Door Controller via Node Red or Home Assistant)

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- All contributors to this project
- The open-source community
- The MQTT community
