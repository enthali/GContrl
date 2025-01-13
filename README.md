# Garage Door Opener

This is the Android app for the Drachen-Fels Garage Door Opener System.
It's a private hobby project that I maintain during my rare spare time.
Feedback is welcome.

The idea is simple: When I arrive at home, I want the garage door to open,
and when I drive off, I want it to close.

The system works, but like any system, it requires:

- Better documentation and installation guide
- The repository here at GitHub should include the whole solution - not just the Android App
- More thorough testing

The project has a very long list of things to consider for the future.

However, it works - at least for the one test instance I run at my house.
So feel free to copy the solution and use this project as inspiration for your own home automation module.

If you are interested in setting something like this up yourself, here are the main ingredients:

## System Setup

Home or Cloud:

- MQTT server to communicate between the Android app and the backend
- Node-RED server to bridge between the MQTT server and the MEROS Garage Door Module (if you use the MEROS Module)
- A domain address where you can reach your MQTT server using an SSL connection

Mobile:

- Android Phone
- A car with Google Automotive Services (e.g., Polestar/Volvo/GM or Honda)

At Your Garage:

- MEROS Garage Door Opener
- ESPHome device handling your garage door communicating via MQTT with your server

Data Privacy:
This app exclusively sends open and close commands to the door.
The location management is local on your device.

Security:

- SSL communication
- Additional features on the roadmap...

## Revision History:
| Version | comment |
| --- | --- |
| 27 | Refactor approach |
| 1-16 | Internal revisions |


