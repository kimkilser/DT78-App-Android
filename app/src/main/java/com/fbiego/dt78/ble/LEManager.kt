package com.fbiego.dt78.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.fbiego.dt78.app.DataReceiver
import com.fbiego.dt78.app.RootUtil
import com.fbiego.dt78.data.byteArrayOfInts
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.*
import timber.log.Timber
import java.util.*

/**
 * Implements BLEManager
 */
class LEManager(context: Context) : BleManager<LeManagerCallbacks>(context) {

    var dt78RxCharacteristic: BluetoothGattCharacteristic? = null
    var dt78TxCharacteristic: BluetoothGattCharacteristic? = null
    var sendBat = false
    var shake = false

//    var espDisplayMessageCharacteristic: BluetoothGattCharacteristic? = null
//    var espDisplayTimeCharacteristic: BluetoothGattCharacteristic? = null
//    var espDisplayOrientationCharacteristic: BluetoothGattCharacteristic? = null

    companion object {
        const val MTU = 500
        val DT78_SERVICE_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        val DT78_TX_CHARACTERISTIC: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
        val DT78_RX_CHARACTERISTIC: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")


        fun readBatteryLevel(context: Context): Int {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = context.registerReceiver(null, intentFilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

            val batteryLevelPercent: Int = ((level.toFloat() / scale.toFloat()) * 100f).toInt()
            Timber.d("readTimeAndBatt {level=$level,scale=$scale,batteryLevel=$batteryLevelPercent%}")
            return batteryLevelPercent
        }




    }

    /**
     * This method must return the gatt callback used by the manager.
     * This method must not create a new gatt callback each time it is being invoked, but rather return a single object.
     *
     * @return the gatt callback object
     */
    override fun getGattCallback(): BleManagerGattCallback {
        return callback
    }

    /**
     * Write {@code message} to the remote device's characteristic
     */
    fun writeNotification(message: String, app: Int): Boolean {
        return write(message, app)
    }

    fun writeCaller(caller: String, type: Boolean): Boolean{
        return writeCall(caller, type)
    }


    /**
     * Write {@code message} to the remote device's characteristic
     */
//    fun writeTimeAndBatt(message: String): Boolean {
//        //
//        // read battery level
//        val batteryLevelPercent = Companion.readBatteryLevel(context)
//        return writeTimeAndBatteryLevel(batteryLevelPercent, message)
//    }



    fun batLevel(){
        val batteryLevelPercent = Companion.readBatteryLevel(context)
        val bat =  "Phone at $batteryLevelPercent %"
        write(bat, 0)
    }

    fun shakeCamera(): Boolean{
        return if (isConnected && isReady && dt78TxCharacteristic != null){
            requestMtu(MTU).enqueue()
            val act = byteArrayOfInts(0xAB, 0x00, 0x04, 0xFF, 0x79, 0x80, 0x01)
            val dec = byteArrayOfInts(0xAB, 0x00, 0x04, 0xFF, 0x79, 0x80, 0x00)
            shake = if (!shake){
                writeCharacteristic(dt78TxCharacteristic, act).enqueue()
                true
            } else {
                writeCharacteristic(dt78TxCharacteristic, dec).enqueue()
                false
            }
            true
        } else {
            false
        }
    }

    fun syncData(time: Long): Boolean{
        return if (isConnected && isReady && dt78TxCharacteristic != null){
            requestMtu(MTU).enqueue()
            val calendar = Calendar.getInstance(Locale.getDefault())
            calendar.time = Date(time)
            val stepRQ = byteArrayOfInts(0xAB, 0x00, 0x09, 0xFF, 0x51, 0x80, 0x00, calendar.get(Calendar.YEAR)-2000,
                calendar.get(Calendar.MONTH)+1,calendar.get(Calendar.DAY_OF_MONTH),calendar.get(Calendar.HOUR_OF_DAY), 0x00)
            val sleepRQ = byteArrayOfInts(0xAB, 0x00, 0x09, 0xFF, 0x52, 0x80, 0x00, calendar.get(Calendar.YEAR)-2000,
                calendar.get(Calendar.MONTH)+1,calendar.get(Calendar.DAY_OF_MONTH),calendar.get(Calendar.HOUR_OF_DAY), 0x00)
            writeCharacteristic(dt78TxCharacteristic, stepRQ).enqueue()
            writeCharacteristic(dt78TxCharacteristic, sleepRQ).enqueue()
            true
        } else {
            false
        }
    }


    fun stepsRequest(): Boolean{
        return if (isConnected && isReady && dt78TxCharacteristic != null){
            requestMtu(MTU).enqueue()
            val calendar = Calendar.getInstance(Locale.getDefault())

            val stepRq = byteArrayOfInts(0xAB, 0x00, 0x09, 0xFF, 0x51, 0x80, 0x00, calendar.get(Calendar.YEAR)-2000,
                calendar.get(Calendar.MONTH)+1,calendar.get(Calendar.DAY_OF_MONTH),calendar.get(Calendar.HOUR_OF_DAY)+1, 0x00)
            Timber.d("Requested steps @ : ${calendar.get(Calendar.YEAR)-2000} ,${calendar.get(Calendar.MONTH)+1}, ${calendar.get(Calendar.DAY_OF_MONTH)}, ${calendar.get(Calendar.HOUR_OF_DAY)+1}")
            writeCharacteristic(dt78TxCharacteristic, stepRq).enqueue()
            true
        } else {
            false
        }
    }

    fun findWatch(): Boolean{
        return if (isConnected && isReady && dt78TxCharacteristic != null){
            requestMtu(MTU).enqueue()
            val find = byteArrayOfInts(0xAB, 0x00, 0x03, 0xFF, 0x71, 0x80)
            writeCharacteristic(dt78TxCharacteristic, find)
                .with { device, data ->
                    Timber.d("Data sent to ${device.address} Data = ${data.size()}")
                }
                .fail { device, status ->
                    Timber.d("Failed to send data to ${device.name}, status = $status")
                }
                .done {
                    Timber.d("Data sent")
                }
                .enqueue()
            Timber.d("Find watch ok")
            true
        } else {
            Timber.d("Find fail: Connected - ${if (isConnected) "true" else "false"}  Ready - ${if (isReady) "true" else "false"}  Characteristic - ${if (dt78TxCharacteristic != null) " ok" else "null"}")
            false
        }
    }

    fun batRequest(): Boolean{
        return if (isConnected && isReady && dt78TxCharacteristic != null){
            requestMtu(MTU).enqueue()
            val find = byteArrayOfInts(0xAB, 0x00, 0x04, 0xFF, 0x91, 0x80, 0x01)
            writeCharacteristic(dt78TxCharacteristic, find).enqueue()
            true
        } else {
            false
        }
    }

    fun setTime(): Boolean {
        return if (isConnected && isReady && dt78TxCharacteristic != null) {
            requestMtu(MTU).enqueue()
            val calendar = Calendar.getInstance(Locale.getDefault())
            val time = byteArrayOfInts(
                0xAB,
                0x00,
                0x0B,
                0xFF,
                0x93,
                0x80,
                0x00,
                calendar.get(Calendar.YEAR) / 256,
                calendar.get(Calendar.YEAR) % 256,
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND)
            )
            writeCharacteristic(dt78TxCharacteristic, time).enqueue()
            Timber.d("Setting time")
            true
        } else {
            Timber.d("Unable to set time: Connected - ${if (isConnected) "true" else "false"}  Ready - ${if (isReady) "true" else "false"}  Characteristic - ${if (dt78TxCharacteristic != null) " ok" else "null"}")
            false
        }
    }

    fun writeBytes(bytes: ByteArray): Boolean{
        return if (isConnected && isReady && dt78TxCharacteristic != null){
            requestMtu(MTU).enqueue()
            writeCharacteristic(dt78TxCharacteristic, bytes).enqueue()
            true
        } else {
            false
        }
    }



    private fun writeCall(name: String, type: Boolean): Boolean{
        return if (isConnected && isReady && dt78TxCharacteristic != null){
            requestMtu(MTU).enqueue()
            val end = byteArrayOfInts(0xAB, 0x00, 0x05, 0xFF, 0x72, 0x80, 0x02, 0x02)
            if (type){
                writeNotification(name, 3)
            } else {
                writeCharacteristic(dt78TxCharacteristic, end).enqueue()
            }
            true
        } else {
            false
        }
    }




    private fun write(message: String, app: Int): Boolean {
        Timber.d("write {connected=$isConnected,hasCharacteristic=${dt78TxCharacteristic != null}}")
        return if (isConnected && isReady && dt78TxCharacteristic != null) {
            requestMtu(MTU).enqueue()
            val msg = message.toByteArray()
            val msgByte = if (msg.size > 125){
                msg[124] = 95
                msg.slice(0 until 125)

            } else {
                msg.slice(msg.indices)
            }
            val start = byteArrayOfInts(0xAB, 0x00)
            val len = (msgByte.size + 5).toByte()
            val type = when (app) {
                1 -> {  //whatsapp
                    byteArrayOfInts(0xFF, 0x72, 0x80, 0x0A, 0x02)
                }
                2 -> {  //twitter
                    byteArrayOfInts(0xFF, 0x72, 0x80, 0x0F, 0x02)
                }
                3 -> {  //caller
                    byteArrayOfInts(0xFF, 0x72, 0x80, 0x01, 0x02)
                }
                4 -> {  //instagram
                    byteArrayOfInts(0xFF, 0x72, 0x80, 0x12, 0x02)
                }
                5 -> {  //facebook
                    byteArrayOfInts(0xFF, 0x72, 0x80, 0x11, 0x02)
                }
                6 -> {  //messenger
                    byteArrayOfInts(0xFF, 0x72, 0x80, 0x10, 0x02)
                }
                7 -> {  //skype
                    byteArrayOfInts(0xFF, 0x72, 0x80, 0x08, 0x02)
                }
                8 -> {  //QQ
                    byteArrayOfInts(0xFF, 0x72, 0x80, 0x07, 0x02)
                }
                9 -> {  //wechat
                    byteArrayOfInts(0xFF, 0x72, 0x80, 0x09, 0x02)
                }
                10 -> {  //line
                    byteArrayOfInts(0xFF, 0x72, 0x80, 0x0E, 0x02)
                }
                11 -> {  //weibo
                    byteArrayOfInts(0xFF, 0x72, 0x80, 0x13, 0x02)
                }
                12 -> {  //kakaotalk
                    byteArrayOfInts(0xFF, 0x72, 0x80, 0x14, 0x02)
                }
                13 -> { //telegram
                    byteArrayOfInts(0xFF, 0x72, 0x80, 0x18, 0x02)
                }
                14 -> { //viber
                    byteArrayOfInts(0xFF, 0x72, 0x80, 0x16, 0x02)
                }
                else -> {   //messages
                    byteArrayOfInts(0xFF, 0x72, 0x80, 0x03, 0x02)
                }
            }

            if (msgByte.size <= 12){
                val send = start + len + type + msgByte
                writeCharacteristic(dt78TxCharacteristic, send).enqueue()
                Timber.d("Send type 0")
                Timber.d("Msg = $message & Length = ${len-5} & Channel = $app")
            } else {
                val msg0 = msgByte.slice(0 until 12)
                val send = start + len + type + msg0
                writeCharacteristic(dt78TxCharacteristic, send).enqueue()
                Timber.d("Loop = start & Length = ${send.size}")

                val rem = msgByte.size - 12
                val lp = rem/19
                val rm = rem%19
                val sub = msgByte.slice(12 until msgByte.size)
                for (i in 0 until lp){
                    val st = sub.slice(i*19 until (i*19)+19)
                    val send1 = byteArrayOfInts(i) + st
                    writeCharacteristic(dt78TxCharacteristic, send1).enqueue()
                    Timber.d("Loop = $i & Length = ${send1.size}")
                }
                if (rm != 0){
                    val st = sub.slice(lp*19 until sub.size)
                    val send2 = byteArrayOfInts(lp) + st
                    writeCharacteristic(dt78TxCharacteristic, send2).enqueue()
                    Timber.d("Loop = $lp & Length = ${send2.size}")
                }

                Timber.d("Msg = $msg & Length = ${len-5}")
                Timber.d("Send type 1")
            }
            true
        } else {
            false
        }
    }

//    private fun writeData(bytes: ByteArray): Boolean{
//
//        return if (isConnected && isReady && dt78TxCharacteristic != null) {
//            requestMtu(MTU).enqueue()
//
//            if (bytes.size <= 12){
//                writeCharacteristic(dt78TxCharacteristic, bytes).enqueue()
//            } else {
//                val msg0 = bytes.slice(0 until 12)
//                val send = start + len + type + msg0
//                writeCharacteristic(dt78TxCharacteristic, send).enqueue()
//                Timber.d("Loop = start & Length = ${send.size}")
//
//                val rem = msgByte.size - 12
//                val lp = rem/19
//                val rm = rem%19
//                val sub = msgByte.slice(12 until msgByte.size)
//                for (i in 0 until lp){
//                    val st = sub.slice(i*19 until (i*19)+19)
//                    val send1 = byteArrayOfInts(i) + st
//                    writeCharacteristic(dt78TxCharacteristic, send1).enqueue()
//                    Timber.d("Loop = $i & Length = ${send1.size}")
//                }
//                if (rm != 0){
//                    val st = sub.slice(lp*19 until sub.size)
//                    val send2 = byteArrayOfInts(lp) + st
//                    writeCharacteristic(dt78TxCharacteristic, send2).enqueue()
//                    Timber.d("Loop = $lp & Length = ${send2.size}")
//                }
//
//                Timber.d("Msg = $msg & Length = ${len-5}")
//                Timber.d("Send type 1")
//            }
//            true
//        } else {
//
//            false
//        }
//
//    }

    /**
     * Returns whether to connect to the remote device just once (false) or to add the address to white list of devices
     * that will be automatically connect as soon as they become available (true). In the latter case, if
     * Bluetooth adapter is enabled, Android scans periodically for devices from the white list and if a advertising packet
     * is received from such, it tries to connect to it. When the connection is lost, the system will keep trying to reconnect
     * to it in. If true is returned, and the connection to the device is lost the [BleManagerCallbacks.onLinklossOccur]
     * callback is called instead of [BleManagerCallbacks.onDeviceDisconnected].
     *
     * This feature works much better on newer Android phone models and many not work on older phones.
     *
     * This method should only be used with bonded devices, as otherwise the device may change it's address.
     * It will however work also with non-bonded devices with private static address. A connection attempt to
     * a device with private resolvable address will fail.
     *
     * The first connection to a device will always be created with autoConnect flag to false
     * (see [BluetoothDevice.connectGatt]). This is to make it quick as the
     * user most probably waits for a quick response. However, if this method returned true during first connection and the link was lost,
     * the manager will try to reconnect to it using [BluetoothGatt.connect] which forces autoConnect to true .
     *
     * @return autoConnect flag value
     */
    override fun shouldAutoConnect(): Boolean {
        return true
    }

    /**
     * Implements GATTCallback methods
     */
    private val callback: BleManagerGattCallback = object : BleManagerGattCallback() {
        /**
         * This method should return `true` when the gatt device supports the required services.
         *
         * @param gatt the gatt device with services discovered
         * @return `true` when the device has the required service
         */
        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val gattService: BluetoothGattService? = gatt.getService(DT78_SERVICE_UUID)
            if (dt78TxCharacteristic == null) {
                gattService?.getCharacteristic(DT78_TX_CHARACTERISTIC)?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                dt78TxCharacteristic = gattService?.getCharacteristic(DT78_TX_CHARACTERISTIC)
            }
            if (dt78RxCharacteristic == null) {
                dt78RxCharacteristic = gattService?.getCharacteristic(DT78_RX_CHARACTERISTIC)
            }

            Timber.w("Gatt service ${gattService != null}, RX ${dt78RxCharacteristic != null}, TX ${dt78TxCharacteristic != null}")

            return gattService != null
                    && dt78RxCharacteristic != null
                    && dt78TxCharacteristic != null
        }


        /**
         * This method should set up the request queue needed to initialize the profile.
         * Enabling Service Change indications for bonded devices is handled before executing this
         * queue. The queue may have requests that are not available, e.g. read an optional
         * service when it is not supported by the connected device. Such call will trigger
         * {@link Request#fail(FailCallback)}.
         * <p>
         * This method is called from the main thread when the services has been discovered and
         * the device is supported (has required service).
         * <p>
         * Remember to call {@link Request#enqueue()} for each request.
         * <p>
         * A sample initialization should look like this:
         * <pre>
         * &#64;Override
         * protected void initialize() {
         *    requestMtu(MTU)
         *       .with((device, mtu) -> {
         *           ...
         *       })
         *       .enqueue();
         *    setNotificationCallback(characteristic)
         *       .with((device, data) -> {
         *           ...
         *       });
         *    enableNotifications(characteristic)
         *       .done(device -> {
         *           ...
         *       })
         *       .fail((device, status) -> {
         *           ...
         *       })
         *       .enqueue();
         * }
         * </pre>
         */
        override fun initialize() {
            Timber.d("Initialising...")

            val rooted = RootUtil.isDeviceRooted


            requestMtu(MTU).enqueue()


            setNotificationCallback(dt78RxCharacteristic)
                .with { device, data ->
                    Timber.d("Data received from ${device.address} Data = ${data.size()}")


                    if (data.size() == 7 && rooted){
                        if (data.getByte(4) == (0x99).toByte()){
                            when (data.getByte(6)) {
                                (0x00).toByte() -> {

                                    try {
                                        val p1 = Runtime.getRuntime().exec("su -c input keyevent 85")

                                    } catch (e: Exception){

                                    }

                                }
                                (0x01).toByte() -> {
                                    try {
                                        val su = Runtime.getRuntime().exec("su -c input keyevent 87")
                                    } catch (e: Exception){

                                    }
                                }
                                (0x02).toByte() -> {
                                    try {
                                        val su = Runtime.getRuntime().exec("su -c input keyevent 88")
                                    } catch (e: Exception){

                                    }
                                }
                            }
                        }

                        if (data.getByte(4) == (0x79).toByte() && data.getByte(6) == (0x01).toByte()){
                            try {
                                val su = Runtime.getRuntime().exec("su -c input keyevent 24")
                                su.waitFor()
                            } catch (e: Exception){

                            }

                        }
                    }

                    DataReceiver().getData(data)


                }
            enableNotifications(dt78RxCharacteristic)
                    .done {
                        Timber.d("Successfully enabled DT78RxCharacteristic notifications")
                    }
                    .fail { _, _ ->
                        Timber.w("Failed to enable DT78RxCharacteristic notifications")
                    }
                    .enqueue()
//            enableIndications(dt78RxCharacteristic)
//                    .done {
//                        Timber.d("Successfully wrote message")
//                    }
//                    .fail { device, status ->
//                        Timber.w("Failed to write message to ${device.address} - status: $status")
//                    }
//                    .enqueue()


        }


        override fun onDeviceDisconnected() {
            dt78RxCharacteristic = null
            dt78TxCharacteristic = null
        }
    }

}
