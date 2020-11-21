# ChangeLog


## 2.0
>+ Removed manual entry of mac address, watch address will be selected from the list of paired devices
>+ Removed *Run as service* option in preferences, the app will run as service by default, can still be stopped and started from the main screen
>+ Increased visibility of the text icon for watch percentage
>+ Auto detect watch type between DT78 & DT92 and firmware version
>+ Added frequent contacts for DT92
>+ *Smart notification* on DT78 only, title and message will be in separate lines if notification is short
>+ Fixed bug causing `CursorIndexOutOfBoudsException` error

## 2.1
>+ Fixed static year bug, v2.0 and below will not set the correct time after year 2020
>+ Fixed dependency of permissions, app should not crash if permission is not granted
>+ Call and SMS notification can be turned on or off
>+ Added raise to wake setting for DT92
>+ *Smart notification v2* on DT78 only, space at the start of each line will be removed
>+ Added large number font on Google weather and phone battery notifications
