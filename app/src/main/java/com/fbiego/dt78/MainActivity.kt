package com.fbiego.dt78

//import androidx.appcompat.app.AppCompatActivity
//import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.NotificationManagerCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.CardView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.fbiego.dt78.app.*
import com.fbiego.dt78.ble.LEManager
import com.fbiego.dt78.data.MyDBHandler
import com.fbiego.dt78.data.NotifyAdapter
import com.fbiego.dt78.data.toPInt
import com.hadiidbouk.charts.BarData
import com.hadiidbouk.charts.ChartProgressBar
import no.nordicsemi.android.ble.data.Data
import timber.log.Timber
import java.util.*

class MainActivity : AppCompatActivity(), ConnectionListener {

    lateinit var fab: FloatingActionButton
    lateinit var menu: Menu


    var alertDialog: AlertDialog? = null

    private lateinit var timer: Timer
    private val noDelay = 1000L
    private val duration = 1000L * 30

    companion object{
        internal val RESULT_ENABLE = 1

        var button2: Button? = null
        var bat: TextView? = null
        var watch: TextView? = null
        var bt: ImageView? = null
        var step: TextView? = null
        var cal: TextView? = null
        var btnLock: Button? = null

        var connected = false

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

        fab = findViewById(R.id.fab)
        button2 = findViewById(R.id.button)
        bat = findViewById(R.id.battery)
        watch = findViewById(R.id.watchName)
        bt = findViewById(R.id.connect)
        step = findViewById(R.id.stepsText)
        cal = findViewById(R.id.caloriesText)

        ConnectionReceiver.bindListener(this)





        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        button2?.setOnClickListener {
            if (ForegroundService().findWatch()){
                Toast.makeText(this, "Finding watch", Toast.LENGTH_SHORT).show()

            } else {
                Toast.makeText(this, "Watch not connected", Toast.LENGTH_SHORT).show()
            }



        }

        fab.setOnClickListener {

            val enabled = NotificationManagerCompat.getEnabledListenerPackages(this).contains(BuildConfig.APPLICATION_ID)
            Timber.d("Notification Listener Enabled $enabled")

            if (alertDialog == null || !(alertDialog!!.isShowing)) {
                if (enabled) {

                    // lookup installed apps
                    val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                    installedApps.sortWith(kotlin.Comparator { a, b ->
                        val nameA = packageManager.getApplicationLabel(a).toString()
                        val nameB = packageManager.getApplicationLabel(b).toString()
                        nameA.compareTo(nameB)
                    })
                    val names: Array<String> = installedApps.map { applicationInfo -> packageManager.getApplicationLabel(applicationInfo).toString() }.toTypedArray()

                    val prefsAllowedPackages: MutableSet<String> = MainApplication.sharedPrefs.getStringSet(MainApplication.PREFS_KEY_ALLOWED_PACKAGES, mutableSetOf())
                    val checkedItems = BooleanArray(installedApps.size)
                    for (i in names.indices) {
                        checkedItems[i] = prefsAllowedPackages.contains(installedApps[i].packageName)
                    }

                    val modifiedList: ArrayList<String> = arrayListOf()
                    modifiedList.addAll(prefsAllowedPackages)

                    // show Apps
                    val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                            .setTitle(R.string.choose_app)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                // commit
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
                } else {
                    val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                            .setTitle(R.string.choose_app)
                            .setMessage("Looks like you must first grant this app access to notifications. Do you want to continue?")
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
            }
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
                Toast.makeText(this, "Stopping Service", Toast.LENGTH_SHORT).show()
                stopService(Intent(this, ForegroundService::class.java))
                item.isVisible = false
                menu.findItem(R.id.menu_item_start)?.isVisible = true
                true
            }
            R.id.menu_item_start -> {
                Toast.makeText(this, "Starting Service", Toast.LENGTH_SHORT).show()
                startService(Intent(this, ForegroundService::class.java))
                item.isVisible = false
                menu.findItem(R.id.menu_item_kill)?.isVisible = true
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }


    override fun onStart() {
        super.onStart()
        startService(Intent(this, ForegroundService::class.java))
        Timber.w("onStart")
    }

    override fun onResume() {
        super.onResume()
        setIcon(connected)
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        ForegroundService.lst_sync = pref.getLong(SettingsActivity.PREF_SYNC,System.currentTimeMillis() - 604800000)
        if (System.currentTimeMillis() > ForegroundService.lst_sync + (3600000 * 3)){
            if (ForegroundService().syncData()){
                Toast.makeText(this, "Syncing watch data", Toast.LENGTH_SHORT).show()
                val editor: SharedPreferences.Editor = pref.edit()
                val time = System.currentTimeMillis()
                editor.putLong(SettingsActivity.PREF_SYNC, time)
                editor.apply()
                editor.commit()
            } else  {
                Toast.makeText(this, "Unable to sync data", Toast.LENGTH_SHORT).show()
            }

        }
        bat?.text = "${ForegroundService.bat}%"
        watch?.text = ForegroundService.deviceName

        val dbHandler = MyDBHandler(this, null, null, 1)
        val hrm = findViewById<TextView>(R.id.textHrm)
        val bp = findViewById<TextView>(R.id.textBp)
        val sp = findViewById<TextView>(R.id.textSp)
        hrm.text = dbHandler.getHeartToday()
        bp.text = dbHandler.getBpToday()
        sp.text = dbHandler.getSp02Today()


        barChart()

        val timerTask = object: TimerTask(){
            override fun run() {
                if (ForegroundService.bleManager != null){
                    (ForegroundService.bleManager as LEManager).stepsRequest()
                    Timber.w("Timer Task: Steps requested")
                }


            }
        }

        timer = Timer()
        timer.schedule(timerTask,noDelay, duration)
    }

    override fun onPause() {
        super.onPause()

        timer.cancel()
        timer.purge()
    }

    override fun onDestroy() {
        super.onDestroy()
        // stop the service

        val isRunAsAService = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(SettingsActivity.PREF_KEY_RUN_AS_A_SERVICE, false)
        Timber.w("onDestroy {isService=$isRunAsAService}")
        if (!isRunAsAService) {
            stopService(Intent(this, ForegroundService::class.java))
        }
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
        builder.setTitle("Notification Test")
        builder.setMessage("Send a test notification to the watch\nSelect an icon that will be displayed with the text")
        val inflater = layoutInflater
        val dialogInflater = inflater.inflate(R.layout.notify_layout, null)
        val editText = dialogInflater.findViewById<EditText>(R.id.editText)
        val spinner = dialogInflater.findViewById<Spinner>(R.id.spinner)
        val adapter = NotifyAdapter(this)
        spinner.adapter = adapter

        builder.setView(dialogInflater)
        builder.setPositiveButton("Send"){_, _ ->
            if (!ForegroundService().testNotification(editText.text.toString(),
                spinner.selectedItem as Int
            )){
                Toast.makeText(this, "Watch not connected", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel"){_, _ ->

        }
        builder.show()
    }

    fun health(view: View){
        when (view.id) {
            R.id.hrmCard -> {
                HealthActivity.view = 0
            }
            R.id.bpCard -> {
                HealthActivity.view = 1
            }
            R.id.sp02Card -> {
                HealthActivity.view = 2
            }

        }

        startActivity(Intent(this, HealthActivity::class.javaObjectType))

    }


    fun receive(data: Data){
        if (data.size() == 8){
            if (data.getByte(4) == (0x91).toByte()){
                ForegroundService.bat = data.getByte(7)!!.toPInt()
                Timber.w("Battery: ${ForegroundService.bat}%")
                bat?.text = "${ForegroundService.bat}%"
                watch?.text = ForegroundService.deviceName
            }
        }
        if (data.size() == 17 && data.getByte(4) == (0x51).toByte() && data.getByte(5) == (0x08).toByte()){
            step?.text = ((data.getByte(7)!!.toPInt()*256)+(data.getByte(8)!!).toPInt()).toString() + " steps"
            val cl =  (((data.getByte(10)!!).toPInt()*256)+(data.getByte(11)!!).toPInt())
            cal?.text = "$cl kcal"
        }

    }

    override fun onConnectionChanged(state: Boolean) {

        runOnUiThread{
            connected = state
            setIcon(connected)
            if (connected){
                watch?.text = ForegroundService.deviceName
            }

            val pref = PreferenceManager.getDefaultSharedPreferences(this)
            ForegroundService.lst_sync = pref.getLong(SettingsActivity.PREF_SYNC,System.currentTimeMillis() - 604800000)
            if (System.currentTimeMillis() > ForegroundService.lst_sync + (3600000 * 3)){
                if (ForegroundService().syncData()){
                    Toast.makeText(this, "Syncing watch data", Toast.LENGTH_SHORT).show()
                    val editor: SharedPreferences.Editor = pref.edit()
                    val time = System.currentTimeMillis()
                    editor.putLong(SettingsActivity.PREF_SYNC, time)
                    editor.apply()
                    editor.commit()
                } else  {
                    Toast.makeText(this, "Unable to sync data", Toast.LENGTH_SHORT).show()
                }

            }
        }


    }
    fun onClick(view: View){
        val intent = Intent(this, StepsActivity::class.javaObjectType)
        startActivity(intent)

    }

    private fun setIcon(state: Boolean){
        if (state){
            bt?.imageTintList = ColorStateList.valueOf(Color.BLUE)
        } else {
            bt?.imageTintList = ColorStateList.valueOf(Color.GRAY)
        }
    }

//    fun enablePhone(view:View) {
//        val active = deviceManger.isAdminActive(compName)
//        if (active)
//        {
//            deviceManger.removeActiveAdmin(compName)
//            btnEnable.setText("Enable")
//            btnLock.setVisibility(View.GONE)
//        }
//        else
//        {
//            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
//            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
//            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "You should enable the app!")
//            startActivityForResult(intent, RESULT_ENABLE)
//        }
//    }
//    fun lockPhone(view:View) {
//        deviceManager.lockNow()
//    }
    fun activateShake(view: View){

    if (ForegroundService.bleManager != null){
        (ForegroundService.bleManager as LEManager).shakeCamera()
    }

    }

//    protected fun onActivityResult(requestCode:Int, resultCode:Int, @Nullable data:Intent) {
//        super.onActivityResult(requestCode, resultCode, data)
//        when (requestCode) {
//            RESULT_ENABLE -> {
//                if (resultCode == Activity.RESULT_OK)
//                {
//                    //btnEnable.setText("Disable")
//                    btnLock?.visibility = View.VISIBLE
//                }
//                else
//                {
//                    Toast.makeText(
//                        applicationContext, "Failed!",
//                        Toast.LENGTH_SHORT).show()
//                }
//                return
//            }
//        }
//    }

}
