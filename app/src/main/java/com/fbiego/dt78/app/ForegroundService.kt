package com.fbiego.dt78.app

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.*
import android.app.admin.DevicePolicyManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.*
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.preference.PreferenceManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.fbiego.dt78.BuildConfig
import com.fbiego.dt78.MainActivity
import com.fbiego.dt78.R
import com.fbiego.dt78.ble.LEManager
import com.fbiego.dt78.ble.LeManagerCallbacks
import com.fbiego.dt78.data.*
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import timber.log.Timber
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import com.fbiego.dt78.app.SettingsActivity as ST

/**
 *
 */
class ForegroundService : Service(), MessageListener, PhonecallListener, DataListener {

    companion object {
        val NOTIFICATION_DISPLAY_TIMEOUT = 2 * 60 * 1000 //2 minutes
        val SERVICE_ID = 9001
        val SERVICE_ID2 = 9002
        val NOTIFICATION_CHANNEL = BuildConfig.APPLICATION_ID
        val VESPA_DEVICE_ADDRESS = "00:00:00:00:00:00" // <--- YOUR MAC address here
        val formatter = SimpleDateFormat.getTimeInstance(DateFormat.SHORT)
        var bleManager: BleManager<LeManagerCallbacks>? = null
        var notId = 0
        var notBody = ""
        var notAppName = ""
        var deviceName = ""
        var bat = 0
        var steps = 0
        var calories = 0
        var lst_sync = 0L
        var findPhone = false
        var unit = 1
        var dt78 = true
        var lockPhone = false
        var notify = false
        var serviceRunning = false

        var stepSize = 70
        var watchVersion = ""

        var connected = false
        var iconSet = true
        var capitalize = false
        var prt = 0


    }

    private var startId = 0
    private lateinit var ring: Ringtone

    lateinit var deviceManager: DevicePolicyManager
    lateinit var compName: ComponentName



    var lastPost: Long = 0L
    /**
     * Called by the system when the service is first created.  Do not call this method directly.
     */
    override fun onCreate() {
        super.onCreate()

        notificationChannel(false)
        if (BuildConfig.DEBUG) {
            //Timber.plant(Timber.DebugTree())
            //Timber.plant(FileLog(this, "service.txt"))
        }

        val not = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ring = RingtoneManager.getRingtone(this,not)

        deviceManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, DeviceAdmin::class.java)

        Timber.w("onCreate")
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val remoteMacAddress = pref.getString(ST.PREF_KEY_REMOTE_MAC_ADDRESS, VESPA_DEVICE_ADDRESS)
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val leDevice = bluetoothManager.adapter.getRemoteDevice(remoteMacAddress)


        unit = if (pref.getBoolean(ST.PREF_UNITS, false)) 1 else 0
        dt78 = pref.getBoolean(ST.PREF_WATCH_TYPE, true)
        watchVersion = ""
        lockPhone = pref.getBoolean(ST.PREF_LOCK, false)
        lst_sync = pref.getLong(ST.PREF_SYNC, System.currentTimeMillis() - 604800000)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P){


        }
        PhonecallReceiver.bindListener(this)
        SMSReceiver.bindListener(this)
        DataReceiver.bindListener(this)
        bleManager = LEManager(this)
        (bleManager as LEManager).setGattCallbacks(bleManagerCallback)
        if (bluetoothManager.adapter.state == BluetoothAdapter.STATE_ON) {
            (bleManager as LEManager).connect(leDevice).enqueue()
            Timber.d("Bluetooth on. Connect leDevice")
        }
        Timber.d("Bluetooth adapter state: ${bluetoothManager.adapter.state}")

        val intentFilter = IntentFilter(NotificationListener.EXTRA_ACTION)
        LocalBroadcastManager.getInstance(this).registerReceiver(localReceiver, intentFilter)

        //registerReceiver(tickReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))


    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun notificationChannel(priority: Boolean): NotificationManager {
        val notificationMgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val notificationChannel: NotificationChannel
            if (priority){
                notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL, BuildConfig.APPLICATION_ID, NotificationManager.IMPORTANCE_HIGH)
                notificationChannel.description = getString(R.string.channel_desc)
                notificationChannel.lightColor = ContextCompat.getColor(applicationContext, R.color.colorPrimary)
                notificationChannel.enableLights(true)
                notificationChannel.enableVibration(true)
            } else {
                notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL, BuildConfig.APPLICATION_ID, NotificationManager.IMPORTANCE_MIN)
                notificationChannel.description = getString(R.string.channel_desc)
                notificationChannel.lightColor = ContextCompat.getColor(applicationContext, R.color.colorPrimary)
                notificationChannel.enableLights(false)
                notificationChannel.enableVibration(false)
            }
            notificationMgr.createNotificationChannel(notificationChannel)
        }
        return notificationMgr

    }


    fun findWatch(): Boolean{
        return if (bleManager != null){
            (bleManager as LEManager).findWatch()
            (bleManager as LEManager).batRequest()
            (bleManager as LEManager).stepsRequest()
        } else {
            false
        }

    }

    fun forceFind(): Boolean {

        return (bleManager as LEManager).findWatch()
    }

    fun sendData(data: ByteArray): Boolean{
        return if (bleManager != null){
            (bleManager as LEManager).writeBytes(data)
        } else {
            false
        }
    }

    fun syncData(): Boolean{
        return if (bleManager != null){
            (bleManager as LEManager).syncData(lst_sync - (3600000 * 6))
        } else {
            false
        }
    }

    fun updateUser(user: UserData, si: Int){
        val usr = byteArrayOfInts(0xAB, 0x00, 0x0A, 0xFF, 0x74, 0x80, user.step, user.age, user.height, user.weight, si, user.target/1000, 0x01)
        sendData(usr)
    }

    fun updateAlarm(alarm: AlarmData): Boolean{
        val state = if (alarm.enable) 1 else 0
        val al = byteArrayOfInts(0xAB, 0x00, 0x08, 0xFF, 0x73, 0x80, alarm.id, state, alarm.hour, alarm.minute, alarm.repeat)
        return sendData(al)
    }

    fun updateSed(data: ArrayList<Int>){
        val sed = byteArrayOfInts(0xAB, 0x00, 0x09, 0xFF, 0x75, 0x80, data[5], data[1], data[2], data[3], data[4], data[6])
        sendData(sed)
    }

    fun updateSleep(data: ArrayList<Int>){
        val slp = byteArrayOfInts(0xAB, 0x00, 0x08, 0xFF, 0x7F, 0x80, data[5], data[1], data[2], data[3], data[4])
        sendData(slp)
    }

    fun updateQuiet(data: ArrayList<Int>){
        val qt = byteArrayOfInts(0xAB, 0x00, 0x08, 0xFF, 0x76, 0x80, data[5], data[1], data[2], data[3], data[4])
        sendData(qt)
    }

    fun update12hr(state: Boolean){
        val hr12 = byteArrayOfInts(0xAB, 0x00, 0x04, 0xFF, 0x7C, 0x80, if (state) 1 else 0)
        sendData(hr12)
    }



    fun updateWatch(){
        val dbHandler = MyDBHandler(this, null, null, 1)

        val user = dbHandler.getUser()
        updateUser(user, unit)

        val setPref =  PreferenceManager.getDefaultSharedPreferences(this)
        update12hr(setPref.getBoolean(ST.PREF_12H, false))

        val als = dbHandler.getAlarms()
        if (als.isNotEmpty()){
            als.forEach {
                if (it.repeat != 128) {
                    updateAlarm(it)
                }

            }
        }

        val sed = dbHandler.getSet(0)
        if (sed.isNotEmpty()){
            updateSed(sed)
        }

        val sleep = dbHandler.getSet(1)
        if (sleep.isNotEmpty()){
            updateSleep(sleep)
        }



        val quiet = dbHandler.getSet(2)
        if (quiet.isNotEmpty()){
            updateQuiet(quiet)
        }

    }

    override fun onDestroy() {
        Timber.w("onDestroy")
        serviceRunning = false

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val edit = pref.edit()
        edit.putBoolean(ST.PREF_WATCH_TYPE, dt78)
        edit.apply()

        startId = 0
        bleManager?.disconnect()
        bleManager?.close()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localReceiver)
        //unregisterReceiver(tickReceiver)
        unregisterReceiver(bluetoothReceiver)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationMgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationMgr.deleteNotificationChannel(NOTIFICATION_CHANNEL)
        }

        super.onDestroy()
    }

    /**
     * Create/Update the notification
     */
    fun notify(contentText: String, priority: Boolean, bat: Int): Notification {
        // Launch the MainAcivity when user taps on the Notification

        val pendingIntent = PendingIntent.getActivity(this, 0
            , Intent(this, MainActivity::class.java)
            , PendingIntent.FLAG_UPDATE_CURRENT)


        val notBuild = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
        notBuild.setSmallIcon(battery(bat, iconSet))
        notBuild.color = color(bat)      //ContextCompat.getColor(this, R.color.colorPrimary)
        notBuild.setContentIntent(pendingIntent)
        //notBuild.setContentTitle(contentText)
        notBuild.setContentText(contentText)
        if (hasBat(bat)){
            notBuild.setSubText("$bat%")
        }
        if (priority) {
            notBuild.priority = NotificationCompat.PRIORITY_HIGH
            notBuild.setSound(Uri.parse("android.resource://${BuildConfig.APPLICATION_ID}/"+R.raw.notification))
            notBuild.setShowWhen(true)
        } else {
            notBuild.priority = priority(prt)
            notBuild.setSound(Uri.EMPTY)
            notBuild.setShowWhen(false)

        }
        notBuild.setOnlyAlertOnce(true)
        val notification= notBuild.build()
        notificationChannel(priority).notify(SERVICE_ID, notification)
        return notification
    }


    fun cancelNotification(notifyId: Int) {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notifyId)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Timber.w("onStartCommand {intent=${intent != null},flags=$flags,startId=$startId}")
        if (intent == null || this.startId != 0) {
            //service restarted
            Timber.w("onStartCommand - already running")
        } else {
            //started by intent or pending intent

//            val pref = PreferenceManager.getDefaultSharedPreferences(this)
//            val remoteMacAddress = pref.getString(ST.PREF_KEY_REMOTE_MAC_ADDRESS, VESPA_DEVICE_ADDRESS)
//            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//            val leDevice = bluetoothManager.adapter.getRemoteDevice(remoteMacAddress)
//            bleManager = LEManager(this)
//            (bleManager as LEManager).setGattCallbacks(bleManagerCallback)
//            if (bluetoothManager.adapter.state == BluetoothAdapter.STATE_ON) {
//                (bleManager as LEManager).connect(leDevice).enqueue()
//                Timber.d("Strart command: Bluetooth on. Connect leDevice")
//            }

            this.startId = startId
            val notification = notify(getString(R.string.scan), false, -1)
            startForeground(SERVICE_ID, notification)


        }
        val setPref =  PreferenceManager.getDefaultSharedPreferences(this)
        notify = setPref.getBoolean(ST.PREF_KEY_NOTIFY_DISCONNECT, false)
        iconSet = setPref.getBoolean(ST.PREF_IC_SET, true)
        capitalize = setPref.getBoolean(ST.PREF_CAPS, false)
        prt = setPref.getInt(ST.PREF_PRIORITY, 0)

        serviceRunning = true


        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun ring(state: Boolean){
        if (state){
            if (!ring.isPlaying){
                ring.volume = 1.0f
                ring.isLooping = true
                ring.play()
            }
            if (deviceManager.isAdminActive(compName) && lockPhone){
                deviceManager.lockNow()
            }
        } else {
            if (ring.isPlaying){
                ring.stop()
            }

        }

    }

    fun testNotification(text: String, app: Int): Boolean{
        return if (bleManager != null){
            var msg = text
            if (capitalize){
                msg = msg.toUpperCase()
            }
            (bleManager as LEManager).writeNotification(msg,app)
        } else {
            false
        }

    }



    val bleManagerCallback: LeManagerCallbacks = object : LeManagerCallbacks() {
        /**
         * Called when the device has been connected. This does not mean that the application may start communication.
         * A service discovery will be handled automatically after this call. Service discovery
         * may ends up with calling [.onServicesDiscovered] or
         * [.onDeviceNotSupported] if required services have not been found.
         * @param device the device that got connected
         */


        override fun onDeviceConnected(device: BluetoothDevice) {
            super.onDeviceConnected(device)
            connected = true
            Timber.d("onDeviceConnected ${device.name}")
            notify(getString(R.string.connected)+" ${device.name}", false, -1)
            deviceName = device.name

        }

        override fun onDeviceReady(device: BluetoothDevice) {
            super.onDeviceReady(device)
            Timber.d("FG - Device ready ${device.name}")
            connected = true

            deviceName = device.name

            if (bleManager != null){
                (bleManager as LEManager).setTime()
                (bleManager as LEManager).batRequest()
                //updateWatch()
            }

            Timber.d("bleManager: ${bleManager != null}")
            ConnectionReceiver().notifyStatus(true)
        }


        /**
         * Called when the Android device started connecting to given device.
         * The [.onDeviceConnected] will be called when the device is connected,
         * or [.onError] in case of error.
         * @param device the device that got connected
         */
        override fun onDeviceConnecting(device: BluetoothDevice) {
            super.onDeviceConnecting(device)
            connected = false
            Timber.d("Connecting to ${if (device.name.isNullOrEmpty()) "device" else device.name}")
            notify(getString(R.string.connecting)+" ${if (device.name.isNullOrEmpty()) "device" else device.name}", false, -1)
        }

        /**
         * Called when user initialized disconnection.
         * @param device the device that gets disconnecting
         */
        override fun onDeviceDisconnecting(device: BluetoothDevice) {
            super.onDeviceDisconnecting(device)
            connected = false
            Timber.d("Disconnecting from ${device.name}")
            notify(getString(R.string.disconnecting)+" ${device.name}", false, -10)
            ConnectionReceiver().notifyStatus(false)
        }

        /**
         * Called when the device has disconnected (when the callback returned
         * [BluetoothGattCallback.onConnectionStateChange] with state DISCONNECTED),
         * but ONLY if the [BleManager.shouldAutoConnect] method returned false for this device when it was connecting.
         * Otherwise the [.onLinklossOccur] method will be called instead.
         * @param device the device that got disconnected
         */
        override fun onDeviceDisconnected(device: BluetoothDevice) {
            super.onDeviceDisconnected(device)
            connected = false
            Timber.d("Disconnected from ${device.name}")
            notify(getString(R.string.disconnected)+" ${device.name}", notify, -10)

            ConnectionReceiver().notifyStatus(false)
        }

        /**
         * This callback is invoked when the Ble Manager lost connection to a device that has been connected
         * with autoConnect option (see [BleManager.shouldAutoConnect].
         * Otherwise a [.onDeviceDisconnected] method will be called on such event.
         * @param device the device that got disconnected due to a link loss
         */
        override fun onLinkLossOccurred(device: BluetoothDevice) {
            super.onLinkLossOccurred(device)
            connected = false
            Timber.d("Lost link to ${device.name}")
            notify(getString(R.string.loss_link)+" ${device.name}", notify, -10)
            ConnectionReceiver().notifyStatus(false)
            //MainActivity().buttonEnable(false)
        }


        override fun onError(device: BluetoothDevice, message: String, errorCode: Int) {
            super.onError(device, message, errorCode)
            Timber.e("Error: $errorCode, Message: $message")
//            if (errorCode == 133){
//                Timber.e("Check that the watch has not connected to other devices")
//            }
            stopSelf(startId)
        }


    }





//    var tickReceiver: BroadcastReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent?) {
//            if (System.currentTimeMillis() - lastPost > NOTIFICATION_DISPLAY_TIMEOUT) {
//                (bleManager as LEManager).stepsRequest()
//            }
//        }
//    }

    private var localReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if ((bleManager as LEManager).isConnected && intent != null) {
                Timber.d("onReceive")
                var app = 0
                val notificationIcon = intent.getIntExtra(NotificationListener.EXTRA_ICON, 0)
                val notificationId = intent.getIntExtra(NotificationListener.EXTRA_NOTIFICATION_ID_INT, 0)
                val notificationPackage = intent.getStringExtra(NotificationListener.EXTRA_PACKAGE)
                val notificationAppName = intent.getStringExtra(NotificationListener.EXTRA_APP_NAME)
                val notificationTitle = intent.getStringExtra(NotificationListener.EXTRA_TITLE)
                val notificationBody = intent.getStringExtra(NotificationListener.EXTRA_BODY)
                val notificationTimestamp = intent.getLongExtra(NotificationListener.EXTRA_TIMESTAMP_LONG, 0)
                val notificationDismissed = intent.getBooleanExtra(NotificationListener.EXTRA_NOTIFICATION_DISMISSED, true)
                //
                Timber.d("onNotificationPosted {app=$notificationAppName,id=$notificationId,title=$notificationTitle,body=$notificationBody,posted=$notificationTimestamp,package=$notificationPackage}")
                if (!(notBody == notificationBody && notAppName == notificationAppName) && notificationPackage != null){

                    if (notificationDismissed) {
                        //val success = (bleManager as LEManager).writeTimeAndBatt(formatter.format(Date()))
                        lastPost = notificationTimestamp
                        //Timber.d("writeTime {success=$success}")
                    } else {
                        var appN = false
                        var titleN = false
                        var bodyN = false
                        val buffer = StringBuffer(256)
                        app = if (notificationIcon == 0){

                            appN = true
                            0
                        } else {
                            notificationIcon
                        }

                        if (notificationTitle != "null" && notificationTitle != notificationAppName){

                            titleN = true

                        }
                        if (notificationBody != "null"){


                            bodyN = true
                        }

                        val line = 25

                        if (appN) {
                            buffer.append(notificationAppName)
                        }
                        if (titleN) {
                            if (appN){
                                val rem = if (bodyN){
                                    notificationTitle.length + notificationBody.length + 2
                                } else {
                                    notificationTitle.length
                                }
                                if (buffer.length < line && rem < 125 - line && dt78){
                                    for (x in 0 until line-buffer.length){
                                        buffer.append(" ")
                                    }
                                } else {
                                    buffer.append(": ")
                                }
                            }
                            buffer.append(notificationTitle)
                        }
                        if (bodyN) {
                            if (!appN && titleN){
                                val rem = notificationBody.length
                                if (buffer.length < line && rem < 125 - line && dt78){
                                    for (x in 0 until line -buffer.length){
                                        buffer.append(" ")
                                    }
                                } else {
                                    buffer.append(": ")
                                }
                            } else if (appN || titleN){
                                buffer.append(": ")
                            }
                            buffer.append(notificationBody)
                        }

                        if (app == 1){
                            if (!(buffer.contains("new messages") || buffer.contains("messages from"))){

                                var msg = buffer.substring(0, buffer.length.coerceAtMost(256) )
                                if (capitalize){
                                    msg = msg.toUpperCase()
                                }
                                if (msg.isNotEmpty()){
                                    (bleManager as LEManager).writeNotification(msg, app)
                                }

                                notBody = notificationBody
                                notAppName = notificationAppName
                            }
                        } else if (notificationPackage == "in.sweatco.app") {
                            if (!(buffer.contains("SWC earned today"))){
                                var msg = buffer.substring(0, buffer.length.coerceAtMost(256) )
                                if (capitalize){
                                    msg = msg.toUpperCase()
                                }
                                if (msg.isNotEmpty()){
                                    (bleManager as LEManager).writeNotification(msg, app)
                                }
                                notBody = notificationBody
                                notAppName = notificationAppName
                            }

                        } else {
                            var msg = buffer.substring(0, buffer.length.coerceAtMost(256) )
                            if (capitalize){
                                msg = msg.toUpperCase()
                            }
                            if (msg.isNotEmpty()){
                                (bleManager as LEManager).writeNotification(msg, app)
                            }
                            notBody = notificationBody
                            notAppName = notificationAppName
                        }

                        lastPost = notificationTimestamp
                        //Timber.d("writeMessage {success=$success}")
                    }
                } else {
                    Timber.d("Previous notification detected: body=$notBody app=$notAppName or nullPackage=$notificationPackage")
                }

            }
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action?.equals(BluetoothAdapter.ACTION_STATE_CHANGED) == true) {
                Timber.d("Bluetooth adapter changed in receiver")
                Timber.d("BT adapter state: ${intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)}")
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_ON -> {
                        // TODO: 2018/01/03 connect to remote
                        val remoteMacAddress = PreferenceManager.getDefaultSharedPreferences(context)
                                .getString(ST.PREF_KEY_REMOTE_MAC_ADDRESS, VESPA_DEVICE_ADDRESS)
                        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                        val leDevice = bluetoothManager.adapter.getRemoteDevice(remoteMacAddress)

                        bleManager = LEManager(context)
                        (bleManager as LEManager).setGattCallbacks(bleManagerCallback)
                        (bleManager as LEManager).connect(leDevice)
                        Timber.d("Bluetooth STATE ON")
                    }
                    BluetoothAdapter.STATE_TURNING_OFF -> {
                        // TODO: 2018/01/03 close connections
                        (bleManager as LEManager).disconnect()
                        (bleManager as LEManager).close()
                        Timber.d("Bluetooth TURNING OFF")
                    }
                }
            }
        }
    }

    override fun messageReceived(message: String?) {
        if (message != null) {
            var msg = message
            if (capitalize){
                msg = msg.toUpperCase()
            }
            (bleManager as LEManager).writeNotification(msg,0)
        }
    }

    override fun callReceived(caller: String?) {
        if (caller != null){
            var name = caller
            if (capitalize){
                name =  name.toUpperCase()
            }
            (bleManager as LEManager).writeCaller(name, true)
        }
    }

    override fun callEnded() {
        (bleManager as LEManager).writeCaller("", false)
    }

    @SuppressLint("DefaultLocale")
    override fun onDataReceived(data: Data) {

        Timber.w("Data received")
        if (data.size() == 8){
            if (data.getByte(4) == (0x91).toByte()){
                bat = data.getByte(7)!!.toPInt()
                Timber.w("Battery: $bat%")
                notify(getString(R.string.connected)+" $deviceName", false, bat)
            }
        }

        if (data.size() == 7){
            if (data.getByte(4) == (0x7D).toByte()){
                if(data.getByte(6) == (0x01).toByte()){
                    findPhone = true
                    ring(true)
                }
                if(data.getByte(6) == (0x00).toByte()){
                    if (findPhone){
                        ring(false)
                        (bleManager as LEManager).batLevel()
                        findPhone = false
                    }
                }
            }

        }

        if (data.size() == 20){
            if (data.getByte(4) == (0x92).toByte()){

                data.getByte(6) //major version
                data.getByte(7) //minor version

                when (data.getByte(17)) {
                    (0x60).toByte() -> {
                        //dt78
                        dt78 = true
                    }
                    (0xA2).toByte() -> {
                        //dt92
                        dt78 = false
                    }
                }
                Timber.d("Identifier: Type=${data.getByte(17)}")
                watchVersion = java.lang.String.format("v%01d.%02d", data.getByte(6)!!.toPInt(), data.getByte(7)!!.toPInt())
            }
        }


        val dbHandler = MyDBHandler(this, null, null, 1)
        if (data.getByte(4) == (0x51).toByte()){
            if (data.getByte(5) == (0x20).toByte()){
                val st = (data.getByte(11)!!.toPInt() *256)+(data.getByte(12)!!.toPInt())
                val cl = (data.getByte(14)!!.toPInt()*256)+(data.getByte(15)!!.toPInt())
                if (st != 0){
                    dbHandler.insertSteps(StepsData(data.getByte(6)!!.toPInt(), data.getByte(7)!!.toPInt(),
                        data.getByte(8)!!.toPInt(), data.getByte(9)!!.toPInt(), st, cl))
                }
            }
            if (data.getByte(5) == (0x11).toByte()){
                val bp = data.getByte(11)!!.toPInt()
                if (bp != 0){
                    dbHandler.insertHeart(HeartData(data.getByte(6)!!.toPInt(), data.getByte(7)!!.toPInt(),
                        data.getByte(8)!!.toPInt(), data.getByte(9)!!.toPInt(), data.getByte(10)!!.toPInt(), bp))
                }
            }

            if (data.getByte(5) == (0x12).toByte()){
                val sp = data.getByte(11)!!.toPInt()
                if (sp != 0){
                    dbHandler.insertSp02(OxygenData(data.getByte(6)!!.toPInt(), data.getByte(7)!!.toPInt(),
                        data.getByte(8)!!.toPInt(), data.getByte(9)!!.toPInt(), data.getByte(10)!!.toPInt(), sp))
                }
            }

            if (data.getByte(5) == (0x14).toByte()){
                val bph = data.getByte(11)!!.toPInt()
                val bpl = data.getByte(12)!!.toPInt()
                if (bph != 0 ){
                    dbHandler.insertBp(PressureData(data.getByte(6)!!.toPInt(), data.getByte(7)!!.toPInt(),
                        data.getByte(8)!!.toPInt(), data.getByte(9)!!.toPInt(), data.getByte(10)!!.toPInt(), bph, bpl))
                }
            }
        }

        if (data.getByte(4) == (0x52).toByte()){
            val dur = (data.getByte(12)!!.toPInt()*256)+data.getByte(13)!!.toPInt()
            if (dur != 0){
                dbHandler.insertSleep(
                    SleepData(data.getByte(6)!!.toPInt(), data.getByte(7)!!.toPInt(),
                        data.getByte(8)!!.toPInt(), data.getByte(9)!!.toPInt(), data.getByte(10)!!.toPInt(),
                            data.getByte(11)!!.toPInt(), dur)
                )
            }
        }

        MainActivity().onDataReceived(data, this, dbHandler.getUser().step)
    }


    fun smartNotify(app: String, title: String, body: String): String{
        var outString = ""
        if (app.length < 25 && body.length + title.length < 125 - 25){
            val rem = 25 - app.length
            outString = app
            for (x in 0 until rem){
                outString += " "
            }
        } else {
            outString += ": "
        }
        return outString
    }


}