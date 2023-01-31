Garage Door Opener
This is the android app to the Drachen-Fels Garage Door Opener System.
it's a private hobby project that I maintain during my rare spare time.
Feedback is welcome.
The idea is simple. When I arrive at home I want the garage door to open
and when I drive off I want it to close.
The system works, but as any system requires
- a much better documentation and installation guide
- The repository should include the whole solution - not just the Android App...
- more test cases
- improved security ...
  ... a very long list of things to consider for the future.

However it works - at least for our family.

If you are interested in setting something like this up yourself here are the main ingredients:

System Setup
@home or cloud
Raspberry Pi hosting
- Mosquitto MQTT server
- Node Red server
- a domain address you can reach your server

@mobil
Android Phone
car with Google Automotive Services e.g. Polestar

@home
MEROS Garage Door Opener

Data privacy
This app, sending exclusively sending open and close commands to the Door. The location management is local
on the device.

Revision History:
1 - 16 - initial "experiments"

