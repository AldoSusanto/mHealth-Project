# mHealth




#watchApp

Using the Comm sample from the Connect IQ for now as a base. It has a companion app for android that lets it receive/send messages.
This can be tested/ran using the Garmin Simulator on Eclipse, so long as you've connected your android device through USB and have debugging set on(I don't know about emulators, though).

First, import Comm in watchApp to Eclipse, and Comm in mobileApp to Android Studio.
Running it:

I used Android Studio(mobile app) and Eclipse(watch simulator) to run it.

Go through the tutorial for setting up Eclipse for Connect IQ at: https://developer.garmin.com/connect-iq/programmers-guide/getting-started/

Conenct your mobile device through usb with android debugging enabled.
Run 'adb forward tcp:7381 tcp:7381' on command prompt(or whatever equivalent).

Run the app on the device.

Run the wearable app on the simulator in Eclipse.

Click Connection->Start

Click on Simulator on the mobile device to connect.

Currently, the send feature is pretty limited, still messing around with it, trying to figure out how to send a whole file(or a way to send many things without overfilling the queue).

At the moment, it's sending step data, intensity minutes, and timestamps for both, which has to be set up first in the simulator by going to Simulation->Activity Monitoring->Set Activity Monitor Info->Throw in some random data for steps and intensity minutes.

Then to send it to the phone(The method to do this is from the base app, will obviously need to change it to make it less tedious later), you need to hold down the Menu button(the right button on the bottom)->Send Data->Hello World.

This will make the app on the phone print out a pop out window with a 2D array with the format:
[[step data],
[intensity minutes],
[timestamps for corresponding index for step and intensity minutes],
nulls for any other data wanted]

Notes:

When charging the Vivoactive HR, preferrably use an outlet charger rather than charging from a usb connection to a computer. If you do use a usb connection to a computer to charge, make sure to use the safely eject option whenever you're done charging, or else you risk losing data.

*For the watch app, Properties limit of 8kb

//----ADDITIONAL README FROM Integrated gdrive app and comm update -------

This version is the integrated version of the gdrive app welcome screen with the comm sample app from Garmin.

Basically, the garmin comm app will be launched when the sync button is pressed. The only thing that we need to do until tuesday is to receive the HR data and parse it into csv

The HR data is of type List<List<Number>> and it can be found in deviceactivity.java
Right now the mobile app will just receive the messages everytime it is send in a for loop, you can maybe change this to an if statement to ensure that the watch only sends it once.

For now, because we still use the watch simulator, the Array would still consists of zeroes and nulls, but on Raphael's side with the watch, it will actually work properly
