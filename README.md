# DT78-App-Android

#### Videos

+ [`How to connect`](https://youtu.be/4o1O2qxbPlw)
+ [`Notification test`](https://youtu.be/2429i_2OC2A)

#### Implemented

* Almost all features
* Notifications upto 125 characters to the watch
* Watch battery percentage on the ongoing notification
* Find phone feature rings and locks phone(requires Device Admin permission) then sends Phone battery percentage as notification to the watch 
* Tested on DT78 & DT92

### App Install

[`DT78-App-v2.3.apk`](https://github.com/fbiego/DT78-App-Android/raw/master/app/release/DT78-App-v2.3.apk)

Released on `Sunday, 6 December 2020 10:25 Greenwich Mean Time (GMT)`

[Changelog](https://github.com/fbiego/DT78-App-Android/blob/master/app/release/changeLog.md):
>+ Added Vietnamese language
>+ Fixed hourly measurement data not being saved
>+ Quiet hours feature will not send notifications during quiet hours
>+ Service will not start automatically unless Bluetooth is on and the address has been set

#### Translate

[`DT78 app translations`](https://docs.google.com/spreadsheets/d/1crHcLgeA30y7-kiXHY95TBrc7-_znlTKFR2QMc66zT4/edit?usp=sharing) on Google Sheets

#### Telegram

Join the telegram group [`DT78 Smartwatch`](https://t.me/dt78app)

### Screenshots

![1](dt78_app3.jpg?raw=true "3")

![2](dt78_app2.jpg?raw=true "2")

## Dependencies

This project has a dependency on the Nordic Semiconductor [`Android-BLE-Library`](https://github.com/NordicSemiconductor/Android-BLE-Library/tree/6011e63816b792505b68d78b1c32b572a8f056e3) that should be cloned along side this project's folder.


Reverse engineering the smartwatch

[`Code repository`](https://github.com/fbiego/dt78)   [`Blog`](http://www.biego.tech/dt78)

