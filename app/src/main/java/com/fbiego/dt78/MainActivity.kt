package com.fbiego.dt78

//import androidx.appcompat.app.AppCompatActivity
//import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.fbiego.dt78.app.*
import com.fbiego.dt78.ble.LEManager
import com.fbiego.dt78.data.*
import com.hadiidbouk.charts.BarData
import com.hadiidbouk.charts.ChartProgressBar
import kotlinx.android.synthetic.main.activity_main.*
import no.nordicsemi.android.ble.data.Data
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList
import com.fbiego.dt78.app.ForegroundService as FG

class MainActivity : AppCompatActivity(), ConnectionListener {

    lateinit var menu: Menu

    var force = false


    var alertDialog: AlertDialog? = null

    private lateinit var timer: Timer
    private val noDelay = 500L
    private val duration = 1000L * 30

    var loadingApps = false

    companion object{
        internal val RESULT_ENABLE = 1

        var button2: Button? = null
        var bat: TextView? = null
        var watch: TextView? = null
        var bt: ImageView? = null
        var per: ImageView? = null
        var step: TextView? = null
        var cal: TextView? = null
        var dis: TextView? = null
        var btnLock: Button? = null
        var cam: Button? = null



        const val PERMISSIONS_CONTACTS = 100
        const val PERMISSION_SMS = 42
        const val PERMISSION_CALL = 52
        const val PERMISSION_CALL_LOG = 54



    }

    private fun barChart(){
        val dataList = ArrayList<BarData>()
        val dbHandler = MyDBHandler(this, null, null, 1)
        val todaySteps = dbHandler.getStepsToday()
        var max = 4000
        todaySteps.forEach {
            dataList.add(BarData(it.hour.toString(), (it.steps+100).toFloat(), it.steps.toString()))
            if (it.steps > max){
                max = it.steps
            }
        }
        val mChart = findViewById<ChartProgressBar>(R.id.ChartProgressBar)
        mChart.setMaxValue(max.toFloat())
        mChart.setDataList(dataList)
        mChart.build()
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button2 = findViewById(R.id.button)
        bat = findViewById(R.id.battery)
        watch = findViewById(R.id.watchName)
        bt = findViewById(R.id.connect)
        per = findViewById(R.id.batIcon)
        step = findViewById(R.id.stepsText)
        cal = findViewById(R.id.caloriesText)
        dis = findViewById(R.id.distanceText)
        cam = findViewById(R.id.cameraBtn)
        ConnectionReceiver.bindListener(this)



        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        button2?.setOnClickListener {
            if (FG().findWatch()){
                Toast.makeText(this, R.string.find_watch, Toast.LENGTH_SHORT).show()

            } else {
                Toast.makeText(this, R.string.not_connect, Toast.LENGTH_SHORT).show()
            }


        }



    }

    fun appsList(view: View){

        val enabled = NotificationManagerCompat.getEnabledListenerPackages(this).contains(BuildConfig.APPLICATION_ID)
        Timber.d("Notification Listener Enabled $enabled")

        if (alertDialog == null || !(alertDialog!!.isShowing)) {
            if (enabled) {

                Timber.d("Loading apps")

//                if (!loadingApps){
//                    appsText.text = getString(R.string.loading)
//                    progressBar2.visibility = View.VISIBLE
//                    loadingApps = true
//                    AsyncTask.THREAD_POOL_EXECUTOR.execute(AppsLoader(this@MainActivity))
//                }
                startActivity(Intent(this, AppsActivity::class.java))

            } else {
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                    .setTitle(R.string.choose_app)
                    .setMessage(R.string.grant_notification)
                    .setNegativeButton(android.R.string.no, null)
                    .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                        if (!enabled) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            } else {
                                startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                            }
                        }
                    }
                    .setOnDismissListener { alertDialog = null }
                    .setOnCancelListener { alertDialog = null }
                alertDialog = builder.create()
                alertDialog!!.show()

            }
            //findViewById<ProgressBar>(R.id.progressBar2).visibility = View.GONE
        }
    }

    fun appsLoaded(names: Array<String>, installedApps: MutableList<ApplicationInfo>,
                   modifiedList: ArrayList<String>, checkedItems: BooleanArray){

        runOnUiThread {
            loadingApps = false
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                .setTitle(R.string.choose_app)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    // commit
                    appsNo.text = modifiedList.size.toString()
                    MainApplication.sharedPrefs.edit().putStringSet(MainApplication.PREFS_KEY_ALLOWED_PACKAGES, modifiedList.toSet()).apply()
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    // close without commit
                }
                .setMultiChoiceItems(names, checkedItems) { _, position, checked ->
                    if (checked) {
                        modifiedList.add(installedApps[position].packageName)
                    } else {
                        modifiedList.remove(installedApps[position].packageName)
                    }
                }
                .setOnDismissListener { alertDialog = null }
                .setOnCancelListener { alertDialog = null }
            alertDialog = builder.create()
            alertDialog!!.show()
            appsText.text = getString(R.string.choose_apps)
            progressBar2.visibility = View.GONE
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        this.menu = menu
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_prefs -> {
                showPreferences()
                true
            }
            R.id.menu_item_kill -> {
                ConnectionReceiver().notifyStatus(false)
                Toast.makeText(this, R.string.stop_service, Toast.LENGTH_SHORT).show()
                stopService(Intent(this, FG::class.java))
                item.isVisible = false
                menu.findItem(R.id.menu_item_start)?.isVisible = true
                true
            }
            R.id.menu_item_start -> {
                Toast.makeText(this, R.string.start_service, Toast.LENGTH_SHORT).show()
                startService(Intent(this, FG::class.java))
                item.isVisible = false
                menu.findItem(R.id.menu_item_kill)?.isVisible = true
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }


    override fun onStart() {
        super.onStart()
        startService(Intent(this, FG::class.java))
        Timber.w("onStart")
    }

    override fun onResume() {
        super.onResume()

        setIcon(FG.connected)

        if (FG.connected){
            watch?.text = FG.deviceName
        }
        checkPermission()
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        FG.lst_sync = pref.getLong(SettingsActivity.PREF_SYNC,System.currentTimeMillis() - 604800000)
        if (System.currentTimeMillis() > FG.lst_sync + (3600000 * 3)){
            if (FG().syncData()){
                Toast.makeText(this, R.string.sync_watch, Toast.LENGTH_SHORT).show()
                val editor: SharedPreferences.Editor = pref.edit()
                val time = System.currentTimeMillis()
                editor.putLong(SettingsActivity.PREF_SYNC, time)
                editor.apply()
                editor.commit()
            }
//            else  {
//                Toast.makeText(this, R.string.unable_sync, Toast.LENGTH_SHORT).show()
//            }

        }
        bat?.text = "${FG.bat}%"
        watch?.text = FG.deviceName
        per?.setImageResource(battery(FG.bat, true))
        per?.imageTintList = ColorStateList.valueOf(color(FG.bat))

        if (RootUtil.isDeviceRooted()){
            cam?.visibility = View.VISIBLE
        } else {
            cam?.visibility = View.INVISIBLE
        }

        appsNo.text = MainApplication.sharedPrefs.getStringSet(MainApplication.PREFS_KEY_ALLOWED_PACKAGES, mutableSetOf()).size.toString()


        val dbHandler = MyDBHandler(this, null, null, 1)
        val hrm = findViewById<TextView>(R.id.textHrm)
        val bp = findViewById<TextView>(R.id.textBp)
        val sp = findViewById<TextView>(R.id.textSp)
        val stepsCal = dbHandler.getStepCalToday()
        val stepSize = dbHandler.getUser().step
        hrm.text = dbHandler.getHeartToday()
        bp.text = dbHandler.getBpToday()
        sp.text = dbHandler.getSp02Today()
        step?.text = stepsCal.steps.toString()
        cal?.text = "${stepsCal.calories} "+this.resources.getString(R.string.kcal)
        dis?.text = distance(stepsCal.steps*stepSize, FG.unit!=0, this)


        barChart()

        val timerTask = object: TimerTask(){
            override fun run() {
                if (FG.bleManager != null){
                    (FG.bleManager as LEManager).stepsRequest()
                    Timber.w("Timer Task: Steps requested")
                }


            }
        }

//        if (::menu.isInitialized){
//            menu.findItem(R.id.menu_item_kill)?.isVisible = FG.serviceRunning
//            menu.findItem(R.id.menu_item_start)?.isVisible = !FG.serviceRunning
//        }


        timer = Timer()
        timer.schedule(timerTask,noDelay, duration)
    }

    fun sync(view: View){
        if (FG().syncData()){
            Toast.makeText(this, R.string.sync_watch, Toast.LENGTH_SHORT).show()
        } else  {
            Toast.makeText(this, R.string.unable_sync, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()

        timer.cancel()
        timer.purge()
    }

    override fun onDestroy() {
        super.onDestroy()
        // stop the service

//        val isRunAsAService = PreferenceManager.getDefaultSharedPreferences(this)
//                .getBoolean(SettingsActivity.PREF_KEY_RUN_AS_A_SERVICE, true)
//        Timber.w("onDestroy {isService=$isRunAsAService}")
//        if (!isRunAsAService) {
//            stopService(Intent(this, FG::class.java))
//        }
    }

    private fun showPreferences() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    fun buttonEnable(state: Boolean){
        runOnUiThread {
            button2?.isEnabled = state
        }

    }

    fun testNotify(view: View){
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.test_notification)
        builder.setMessage(R.string.test_notification_desc)
        val inflater = layoutInflater
        val dialogInflater = inflater.inflate(R.layout.notify_layout, null)
        val editText = dialogInflater.findViewById<EditText>(R.id.editText)
        val spinner = dialogInflater.findViewById<Spinner>(R.id.spinner)
        val adapter = NotifyAdapter(this, true)
        spinner.adapter = adapter

        builder.setView(dialogInflater)
        builder.setPositiveButton(R.string.send){_, _ ->
            if (!FG().testNotification(editText.text.toString(),
                spinner.selectedItem as Int
            )){
                Toast.makeText(this, R.string.not_connect, Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton(R.string.cancel){_, _ ->

        }
        builder.show()
    }

    fun health(view: View){
        when (view.id) {
            R.id.hrmCard -> {
                HealthActivity.viewH = 0
            }
            R.id.bpCard -> {
                HealthActivity.viewH = 1
            }
            R.id.sp02Card -> {
                HealthActivity.viewH = 2
            }

        }

        startActivity(Intent(this, HealthActivity::class.javaObjectType))

    }



    fun onDataReceived(data: Data, context: Context, stepsZ: Int){

        Timber.w("Data received")
        if (data.size() == 8){
            if (data.getByte(4) == (0x91).toByte()){
                FG.bat = data.getByte(7)!!.toPInt()
                Timber.w("Battery: ${FG.bat}%")
                bat?.text = "${FG.bat}%"
                watch?.text = FG.deviceName
                per?.setImageResource(battery(FG.bat, true))
                per?.imageTintList = ColorStateList.valueOf(color(FG.bat))
            }
        }

        if (data.size() == 17 && data.getByte(4) == (0x51).toByte() && data.getByte(5) == (0x08).toByte()){
            val steps = ((data.getByte(7)!!.toPInt()*256)+(data.getByte(8)!!).toPInt())
            step?.text = steps.toString()
            val cl =  (((data.getByte(10)!!).toPInt()*256)+(data.getByte(11)!!).toPInt())
            cal?.text = "$cl "+context.resources.getString(R.string.kcal)
            dis?.text = distance(steps*stepsZ, FG.unit!=0, context)
        }


    }

    override fun onConnectionChanged(state: Boolean) {

        runOnUiThread{
            FG.connected = state
            setIcon(FG.connected)
            if (FG.connected){
                watch?.text = FG.deviceName
            }

            val pref = PreferenceManager.getDefaultSharedPreferences(this)
            FG.lst_sync = pref.getLong(SettingsActivity.PREF_SYNC,System.currentTimeMillis() - 604800000)
            if (System.currentTimeMillis() > FG.lst_sync + (3600000 * 3)){
                if (FG().syncData()){
                    Toast.makeText(this, R.string.sync_watch, Toast.LENGTH_SHORT).show()
                    val editor: SharedPreferences.Editor = pref.edit()
                    val time = System.currentTimeMillis()
                    editor.putLong(SettingsActivity.PREF_SYNC, time)
                    editor.apply()
                    editor.commit()
                }
            }
        }


    }

    fun onClick(view: View){
        var intent = Intent(this, StepsActivity::class.javaObjectType)

        when (view.id) {
            R.id.reminder -> {
                intent = Intent(this, ReminderActivity::class.javaObjectType)
            }
            R.id.layoutSteps -> {
                intent = Intent(this, StepsActivity::class.javaObjectType)
            }
            R.id.userInfo -> {
                intent = Intent(this, UserActivity::class.javaObjectType)
            }
            R.id.settings -> {
                intent = Intent(this, SettingsWatchActivity::class.javaObjectType)
            }

        }
        startActivity(intent)

    }

    private fun setIcon(state: Boolean){
        if (state){
            bt?.imageTintList = ColorStateList.valueOf(Color.BLUE)
        } else {
            bt?.imageTintList = ColorStateList.valueOf(Color.DKGRAY)
            per?.imageTintList = ColorStateList.valueOf(Color.DKGRAY)
        }
    }

    fun activateShake(view: View){
        if (FG.bleManager != null){
            (FG.bleManager as LEManager).shakeCamera()
        }

    }


    private fun checkPermission(){
        if (!checkSmsPermission()){
            requestSMSPermissions()
        } else {
            if (!checkCallPermission()){
                requestCallPermissions()
            } else {
                if (!checkContactPermission()){
                    requestContactPermission()
                } else {
                    if (!checkCallLogPermission() && !versionO()){
                        requestCallLogPermission()
                    } else {
                        val pref = PreferenceManager.getDefaultSharedPreferences(this)
                        val remoteMacAddress = pref.getString(SettingsActivity.PREF_KEY_REMOTE_MAC_ADDRESS,
                            FG.VESPA_DEVICE_ADDRESS
                        )
                        val later = pref.getBoolean("later", false)
                        if (remoteMacAddress == "00:00:00:00:00:00" && !later){
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle(R.string.mac_addr)
//                            val layout = LinearLayout(this)
//                            layout.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
//                            val text = EditText(this)
//                            text.hint = remoteMacAddress
//                            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
//                            params.setMargins(20, 20, 20, 20)
//                            text.setPadding(10, 10, 10, 10)
//                            layout.addView(text, params)
                            builder.setMessage(R.string.setup_desc)
                            val editor: SharedPreferences.Editor = pref.edit()
                            builder.setNegativeButton(R.string.later){_, _ ->
                                editor.putBoolean("later", true)
                                editor.apply()
                                editor.commit()
                            }
                            builder.setPositiveButton(R.string.setup_now){_, _ ->
                                showPreferences()
                            }
                            builder.show()
                        }
                    }

                }
            }
        }



    }

    private fun versionO(): Boolean{
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.P
    }

    private fun checkContactPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ActivityCompat.checkSelfPermission( this@MainActivity, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        return true
    }

    private fun checkSmsPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission( this@MainActivity, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        return true
    }


    private fun checkCallPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ActivityCompat.checkSelfPermission( this@MainActivity, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        return true
    }

    private fun checkCallLogPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ActivityCompat.checkSelfPermission( this@MainActivity, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        return true
    }

    private fun requestContactPermission(){
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.READ_CONTACTS), PERMISSIONS_CONTACTS
        )
    }

    private fun requestCallLogPermission(){
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.READ_CALL_LOG), PERMISSION_CALL_LOG
        )
    }

    private fun requestSMSPermissions() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.READ_SMS), PERMISSION_SMS
        )
    }
    private fun requestCallPermissions() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.READ_PHONE_STATE), PERMISSION_CALL
        )
    }



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_CONTACTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {

            }
        }
    }


}
