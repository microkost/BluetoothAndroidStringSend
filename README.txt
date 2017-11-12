Android Demonstration Application for sending Strings over Bluetooth.

It's attemp to make one class solution for sending texts over IEEE 802.15.1 channel. Original idea was to make minimalistic app with methods which are easy to read for other developers. Final and production solution will have to use professional threading via Handler and better exception handling (recomended solution lower).

Demo was developed as part of presentation Transfering Data between Devices for TTOW0620 Android Application Development course at JAMK.fi. Presentation about more methods and bluetooth overview is there https://goo.gl/TgTW9x.

How it works (user):
* activate bluetooth interface (method on)
* (pair devices throught system - no app support)
* select remote device from list
* choose if your device is server or client (mutually exclusive)
* click open and wait till list of devices dissapear
* then you can type and click send for transfer to other device
* if you need to reset communication and settings use button close

How it works (developer)
* gathering BT interface as HW
* printing paired devices list to Array Adapter
* onclick event for choosing one as remote device
* new thread based on server/client decision
* opening bluetooth socket as RFCOMM serial device
* enstablishing socket and waiting for accept / connect
* starting background reading thread which cleans streams after transfer
* opening bytestreams and oldschool data handling
* printing results to UI thread

Known issue 1: problem with too old devices: Tested on devices with Android 6 and 4, it helps when newer device is server. There is list of methods how to call for Bluetooth server socket https://goo.gl/5Axwpu. 

It is always recomended to use official documentation and examples. If you need production app for bluetooth chat, I can fully recomended 
https://developer.android.com/samples/BluetoothChat/index.html but complexity to understand this project for bluetooth (and potetionaly android) newbie developer is high.

Known issue 2: stream cuts last character in message. Hacked by adding extra letter. Should be solved more wise.

My solution is open by Creative Common right (CC-BY-SA). Source of inspiration https://goo.gl/kF5k5y. Feel free to contribute with improve or fork of project.
