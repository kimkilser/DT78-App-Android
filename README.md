# DT78-App-Android

#### Videos

+ [`How to connect`](https://youtu.be/4o1O2qxbPlw)
+ [`Notification test`](https://youtu.be/2429i_2OC2A)
+ [`DT66 Customized watchface`](https://youtu.be/CJ8nM-tDxSM)

#### Implemented

* Almost all features
* Notifications upto 125 characters to the watch
* Watch battery percentage on the ongoing notification
* Tested on DT78, DT92 & DT66

### App Install

[`DT78-App-v3.0.apk`](https://github.com/fbiego/DT78-App-Android/raw/master/app/release/DT78-App-v3.0.apk)

Released on `Thursday, 11 February 2021 08:50 Greenwich Mean Time (GMT)`

[Changelog](https://github.com/fbiego/DT78-App-Android/blob/master/app/release/changeLog.md):
>+ Added Czech language
>+ Added App level DND mode
>+ Implemented In-App Camera from [`here`](https://github.com/mmobin789/Android-Custom-Camera) (Experimental)
>+ Bug fixes & improvements

The camera may not work on some devices and on others only the back camera may work. Rooted users can long press to switch between In-app or External camera.



#### Translate

[`DT78 app translations`](https://docs.google.com/spreadsheets/d/1crHcLgeA30y7-kiXHY95TBrc7-_znlTKFR2QMc66zT4/edit?usp=sharing) on Google Sheets
+ Czech
+ English
+ German
+ Greek
+ Indonesian
+ Portuguese
+ Russian
+ Spanish
+ Vietnamese

#### Telegram

Join the telegram group [`DT78 Smartwatch`](https://t.me/dt78app)

### Screenshots

![1](dt78_app4.jpg?raw=true "3")

![2](dt78_app5.jpg?raw=true "2")

## Source Code

Changes:
+ Migrated project to `androidx`
+ Included [`Android-BLE-Library`](https://github.com/fbiego/DT78-App-Android/blob/master/Android-BLE-Library.zip) as a zip file
+ Converted `WheelView` to `Kotlin`

Extract as follows:
> `~/your-folder/DT78-App-Android-master`

> `~/your-folder/Android-BLE-Library`

You may need to delete the `.idea` folder before syncing the project

