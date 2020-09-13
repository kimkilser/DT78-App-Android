package com.fbiego.dt78.app

import android.annotation.TargetApi
import android.app.*
import android.app.admin.DevicePolicyManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.view.KeyEvent
import android.widget.Toast
import com.fbiego.dt78.BuildConfig
import com.fbiego.dt78.MainActivity
import com.fbiego.dt78.R
import com.fbiego.dt78.ble.LEManager
import com.fbiego.dt78.ble.LeManagerCallbacks
import com.fbiego.dt78.data.*
import kotlinx.android.synthetic.main.activity_main.*
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import timber.log.Timber
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.experimental.and

/**
 *
 */
class ForegroundService : Service(), MessageListener, PhonecallListener, DataListener {

    companion object {
        val NOTIFICATION_DISPLAY_TIMEOUT = 2 * 60 * 1000 //2 minutes
        val SERVICE_ID = 9001
        val SERVICE_ID2 = 9002
        val NOTIFICATION_CHANNEL = BuildConfig.APPLICATION_ID
        val VESPA_DEVICE_ADDRESS = "F6:BB:D2:BD:47:A7" // <--- YOUR MAC address here
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
//        initNotificationChannel()
//        initNotificationChannel2()

        val not = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ring = RingtoneManager.getRingtone(this,not)

        deviceManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, DeviceAdmin::class.java)

        Timber.w("onCreate")
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val remoteMacAddress = pref.getString(SettingsActivity.PREF_KEY_REMOTE_MAC_ADDRESS, VESPA_DEVICE_ADDRESS)
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val leDevice = bluetoothManager.adapter.getRemoteDevice(remoteMacAddress)


        lst_sync = pref.getLong(SettingsActivity.PREF_SYNC,System.currentTimeMillis() - 604800000)

        SMSReceiver.bindListener(this)
        PhonecallReceiver.bindListener(this)
        DataReceiver.bindListener(this)
        bleManager = LEManager(this)
        (bleManager as LEManager).setGattCallbacks(bleManagerCallback)
        if (bluetoothManager.adapter.state == BluetoothAdapter.STATE_ON) {
            (bleManager as LEManager).connect(leDevice).enqueue()
        }

        val intentFilter = IntentFilter(NotificationListener.EXTRA_ACTION)
        LocalBroadcastManager.getInstance(this).registerReceiver(localReceiver, intentFilter)

        //registerReceiver(tickReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))


    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun notificationChannel(): NotificationManager {
        val notificationMgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL, BuildConfig.APPLICATION_ID, NotificationManager.IMPORTANCE_MIN)
            notificationChannel.description = getString(R.string.channel_desc)
            notificationChannel.lightColor = ContextCompat.getColor(applicationContext, R.color.colorPrimary)
            notificationChannel.enableLights(false)
            notificationChannel.enableVibration(false)
            notificationMgr.createNotificationChannel(notificationChannel)
        }
        return notificationMgr

    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun notificationChannel2(): NotificationManager {
        val notificationMgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL, BuildConfig.APPLICATION_ID, NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.description = getString(R.string.channel_desc)
            notificationChannel.lightColor = ContextCompat.getColor(applicationContext, R.color.colorPrimary)
            notificationChannel.enableLights(true)
            notificationChannel.enableVibration(true)
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

    fun syncData(): Boolean{
        return if (bleManager != null){
            (bleManager as LEManager).syncData(lst_sync)
        } else {
            false
        }
    }

    override fun onDestroy() {
        Timber.w("onDestroy")

        startId = 0
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
    fun notify(contentText: String): Notification {
        // Launch the MainAcivity when user taps on the Notification

        val pendingIntent = PendingIntent.getActivity(this, 0
            , Intent(this, MainActivity::class.java)
            , PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_watch)
            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .setContentIntent(pendingIntent)
            //.setContentTitle(contentText)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSound(Uri.EMPTY)
            .setContentText(contentText)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .build()
        notificationChannel().notify(SERVICE_ID, notification)
        return notification
    }

    fun notifyConnection(contentText: String): Notification {
        // Launch the MainAcivity when user taps on the Notification
        val stat = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(SettingsActivity.PREF_KEY_NOTIFY_DISCONNECT, false)


        val pendingIntent = PendingIntent.getActivity(this, 0
            , Intent(this, MainActivity::class.java)
            , PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_watch)
            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .setContentIntent(pendingIntent)
            //.setContentTitle(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(Uri.parse("android.resource://${BuildConfig.APPLICATION_ID}/"+R.raw.notification))
            .setContentText(contentText)
            .setShowWhen(true)
            .setOnlyAlertOnce(true)
            .build()
        if (stat){
            notificationChannel2().notify(SERVICE_ID2, notification)
        }
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
            this.startId = startId
            val notification = notify("Scanning...")
            startForeground(SERVICE_ID, notification)
        }



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
            if (deviceManager.isAdminActive(compName)){
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
            (bleManager as LEManager).writeNotification(text,app)
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
            Timber.d("onDeviceConnected ${device.name}")
            notify("Connected to ${device.name}")

        }

        override fun onDeviceReady(device: BluetoothDevice) {
            super.onDeviceReady(device)
            ConnectionReceiver().notifyStatus(true)
            deviceName = device.name
            cancelNotification(SERVICE_ID2)
            if (bleManager != null){
                (bleManager as LEManager).setTime()
                (bleManager as LEManager).batRequest()
            }
        }


        /**
         * Called when the Android device started connecting to given device.
         * The [.onDeviceConnected] will be called when the device is connected,
         * or [.onError] in case of error.
         * @param device the device that got connected
         */
        override fun onDeviceConnecting(device: BluetoothDevice) {
            super.onDeviceConnecting(device)
            Timber.d("Connecting to ${if (device.name.isNullOrEmpty()) "device" else device.name}")
            notify("Connecting to ${if (device.name.isNullOrEmpty()) "device" else device.name}")
        }

        /**
         * Called when user initialized disconnection.
         * @param device the device that gets disconnecting
         */
        override fun onDeviceDisconnecting(device: BluetoothDevice) {
            super.onDeviceDisconnecting(device)
            Timber.d("Disconnecting from ${device.name}")
            notify("Disconnecting from ${device.name}")
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
            Timber.d("Disconnected from ${device.name}")
            notify("Disconnected from ${device.name}")

            notifyConnection("Disconnected from ${device.name}")
            ConnectionReceiver().notifyStatus(false)
            //MainActivity().buttonEnable(false)
        }

        /**
         * This callback is invoked when the Ble Manager lost connection to a device that has been connected
         * with autoConnect option (see [BleManager.shouldAutoConnect].
         * Otherwise a [.onDeviceDisconnected] method will be called on such event.
         * @param device the device that got disconnected due to a link loss
         */
        override fun onLinkLossOccurred(device: BluetoothDevice) {
            super.onLinkLossOccurred(device)
            Timber.d("Lost link to ${device.name}")
            notify("Lost link to ${device.name}")
            notifyConnection("Lost link to ${device.name}")
            ConnectionReceiver().notifyStatus(false)
            //MainActivity().buttonEnable(false)
        }


        override fun onError(device: BluetoothDevice, message: String, errorCode: Int) {
            super.onError(device, message, errorCode)
            Timber.w("Error ${device.name}")
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
                        val buffer = StringBuffer(256)
                        var sep = 0
                        var sep2 = 0
                        when (notificationPackage) {
                            "com.whatsapp" -> {
                                app = 1
                            }
                            "com.whatsapp.w4b"  -> {
                                app = 1
                            }
                            "com.twitter.android" -> {
                                app = 2
                            }
                            "com.instagram.android" -> {
                                app = 4
                            }
                            "com.facebook.katana" -> {
                                app = 5
                            }
                            "com.facebook.lite" -> {
                                app = 5
                            }
                            "com.facebook.orca" -> {
                                app = 6
                            }
                            "com.facebook.mlite" -> {
                                app = 6
                            }
                            "com.skype.raider" -> {
                                app = 7
                            }
                            "com.skype.m2" -> {
                                app = 7
                            }
                            "com.tencent.mobileqq" -> {
                                app = 8
                            }
                            "com.tencent.mm" -> {
                                app = 9
                            }
                            "jp.naver.line.android" -> {
                                app = 10
                            }
                            "com.linecorp.linelite" -> {
                                app = 10
                            }
                            "com.weico.international" -> {
                                app = 11
                            }
                            "com.sina.weibo" -> {
                                app = 11
                            }
                            "com.kakao.talk" -> {
                                app = 12
                            }
                            else -> {
                                buffer.append(notificationAppName)
                                sep = 1
                                app = 0
                            }
                        }

                        if (notificationTitle != "null" && notificationTitle != notificationAppName){
                            if (sep == 1){
                                buffer.append(": ")
                                sep2 = 1

                            }
                            buffer.append(notificationTitle)
                            sep = 2

                        }
                        if (notificationBody != "null"){
                            if (sep == 1){
                                buffer.append(": ")
                            } else if (sep == 2) {
                                if (sep2 == 0){
                                    buffer.append(": ")
                                } else {
                                    buffer.append("; ")
                                }

                            }
                            buffer.append(notificationBody)
                        }


                        //buffer.append("\" via ")
                        //buffer.append(notificationAppName)
                        //buffer.append(" @ ")
                        //buffer.append(formatter.format(Date(notificationTimestamp)))
                        if (app == 1){
                            if (!(buffer.contains("new messages") || buffer.contains("messages from"))){
                                (bleManager as LEManager).writeNotification(buffer.substring(0,
                                    buffer.length.coerceAtMost(256)
                                ),app)
                                notBody = notificationBody
                                notAppName = notificationAppName
                            }
                        } else {
                            (bleManager as LEManager).writeNotification(buffer.substring(0,
                                buffer.length.coerceAtMost(256)
                            ),app)
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
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_ON -> {
                        // TODO: 2018/01/03 connect to remote
                        val remoteMacAddress = PreferenceManager.getDefaultSharedPreferences(context)
                                .getString(SettingsActivity.PREF_KEY_REMOTE_MAC_ADDRESS, VESPA_DEVICE_ADDRESS)
                        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                        val leDevice = bluetoothManager.adapter.getRemoteDevice(remoteMacAddress)

                        bleManager = LEManager(context)
                        (bleManager as LEManager).setGattCallbacks(bleManagerCallback)
                        (bleManager as LEManager).connect(leDevice)
                    }
                    BluetoothAdapter.STATE_TURNING_OFF -> {
                        // TODO: 2018/01/03 close connections
                        (bleManager as LEManager).disconnect()
                        (bleManager as LEManager).close()
                    }
                }
            }
        }
    }

    override fun messageReceived(message: String?) {
        if (message != null) {
            (bleManager as LEManager).writeNotification(message,0)
        }
    }

    override fun callReceived(caller: String?) {
        if (caller != null){
            (bleManager as LEManager).writeCaller(caller, true)
        }
    }

    override fun callEnded() {
        (bleManager as LEManager).writeCaller("", false)
    }

    override fun onDataReceived(data: Data) {
        if (data.size() == 8){
            if (data.getByte(4) == (0x91).toByte()){
                bat = data.getByte(7)!!.toPInt()
                Timber.w("Battery: $bat%")
                notify("Connected: $deviceName  Battery: $bat%")
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

            if (data.getByte(4) == (0x79).toByte() && data.getByte(6) == (0x01).toByte()){
//                Thread(
//                    Runnable {
//                        try {
//                            val inst = Instrumentation()
//                            //This is for Volume Down, change to
//                            //KEYCODE_VOLUME_UP for Volume Up.
//                            inst.sendKeyDownUpSync(KeyEvent.KEYCODE_VOLUME_UP)
//                        } catch (e:InterruptedException) {}
//
//                }).start()

            }

        }

//        if (data.size() == 17 && data.getByte(4) == (0x51).toByte() && data.getByte(5) == (0x08).toByte()){
//            steps = (data.getByte(7)!!.toPInt()*256)+data.getByte(8)!!.toPInt()
//            calories = (data.getByte(10)!!.toPInt()*256)+data.getByte(11)!!.toPInt()
//            Timber.w("Steps = $steps")
//            notify("Connected: $deviceName  Battery: $bat%  Steps: $steps Calories: $calories")
//        }

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
        MainActivity().receive(data)
    }


}