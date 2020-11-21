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

[`DT78-App-v2.1.apk`](https://github.com/fbiego/DT78-App-Android/raw/master/app/release/DT78-App-v2.1.apk)

Released on `Saturday, 21 November 2020 19:00 Greenwich Mean Time (GMT)`

[Changelog](https://github.com/fbiego/DT78-App-Android/blob/master/app/release/changeLog.md):
>+ Fixed static year bug, v2.0 and below will not set the correct time after year 2020
>+ Fixed dependency of permissions, app should not crash if permission is not granted
>+ Call and SMS notification can be turned on or off
>+ Added raise to wake setting for DT92
>+ *Smart notification v2* on DT78 only, space at the start of each line will be removed
>+ Added large number font on Google weather and phone battery notifications

#### Telegram

Join the telegram group [`DT78 Smartwatch`](https://t.me/dt78app)

### Screenshots

![1](dt78_app3.jpg?raw=true "3")

![2](dt78_app2.jpg?raw=true "2")

## Dependencies

This project has a dependency on the Nordic Semiconductor [`Android-BLE-Library`](https://github.com/NordicSemiconductor/Android-BLE-Library/tree/6011e63816b792505b68d78b1c32b572a8f056e3) that should be cloned along side this project's folder.


Reverse engineering the smartwatch

[`Code repository`](https://github.com/fbiego/dt78)   [`Blog`](http://www.biego.tech/dt78)

