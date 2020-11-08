# DT78-App-Android

#### Notification test video

[`Youtube`](https://youtu.be/2429i_2OC2A)

#### Implemented

* Almost all features
* Notifications upto 125 characters to the watch
* Watch battery percentage on the ongoing notification
* Find phone feature rings and locks phone(requires Device Admin permission) then sends Phone battery percentage as notification to the watch 
* Tested on DT78 & DT92

### App Install

[`DT78-App-v2.0.apk`](https://github.com/fbiego/DT78-App-Android/raw/master/app/release/DT78-App-v2.0.apk)

Released on `Sunday, 8 November 2020 06:00 Greenwich Mean Time (GMT)`

Changelog:
>+ Removed manual entry of mac address, watch address will be selected from the list of paired devices
>+ Removed *Run as service* option in preferences, the app will run as service by default, can still be stopped and started from the main screen
>+ Increased visibility of the text icon for watch percentage
>+ Auto detect watch type between DT78 & DT92 and firmware version
>+ Added frequent contacts for DT92
>+ *Smart notification* on DT78 only, title and message will be in separate lines if notification is short
>+ Fixed bug causing `CursorIndexOutOfBoudsException` error

#### Telegram

Join the telegram group [`DT78 Smartwatch`](https://t.me/dt78app)

### Translation

Request the app to be translated in your language

Currently in [`en`](https://github.com/fbiego/DT78-App-Android/blob/master/en.xml), [`ru`](https://github.com/fbiego/DT78-App-Android/blob/master/ru.xml)

### Screenshots

![1](dt78_app3.jpg?raw=true "3")

![2](dt78_app2.jpg?raw=true "2")

## Dependencies

This project has a dependency on the Nordic Semiconductor [`Android-BLE-Library`](https://github.com/NordicSemiconductor/Android-BLE-Library/tree/6011e63816b792505b68d78b1c32b572a8f056e3) that should be cloned along side this project's folder.


Reverse engineering the smartwatch

[`Code repository`](https://github.com/fbiego/dt78)   [`Blog`](http://www.biego.tech/dt78)

