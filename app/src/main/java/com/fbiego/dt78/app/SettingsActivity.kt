package com.fbiego.dt78.app

import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.fbiego.dt78.BuildConfig
import com.fbiego.dt78.MainActivity
import com.fbiego.dt78.R
import com.fbiego.dt78.data.BtListAdapter
import com.fbiego.dt78.data.UserListAdapter
import com.fbiego.dt78.data.WheelView
import kotlinx.android.synthetic.main.activity_settings.*
import timber.log.Timber
import java.util.regex.Pattern
import com.fbiego.dt78.app.ForegroundService as FG
import kotlin.Boolean as Boolean1

/**
 *
 */
class SettingsActivity : AppCompatActivity() {

    lateinit var deviceManager: DevicePolicyManager
    lateinit var compName: ComponentName
    lateinit var states: ArrayList<Boolean1?>
    lateinit var editor: SharedPreferences.Editor
    lateinit var myUserList: UserListAdapter

    companion object {
        const val PREF_KEY_RUN_AS_A_SERVICE = "pref_as_bg_service"
        const val PREF_KEY_REMOTE_MAC_ADDRESS = "pref_remote_mac_address"
        const val PREF_KEY_START_AT_BOOT = "pref_start_at_boot"
        const val PREF_KEY_NOTIFY_DISCONNECT = "pref_notify_disconnect"
        const val PREF_KEY_NOTIFY_CALL = "pref_call"
        const val PREF_KEY_NOTIFY_SMS = "pref_sms"
        const val PREF_CURRENT_STEPS = "current_steps"
        const val PREF_CURRENT_CALORIES = "current_calories"
        const val PREF_RTW = "raise_to_wake"
        const val PREF_12H = "12_hour"
        const val PREF_HOURLY = "hourly_measure"
        const val PREF_UNITS = "units"
        const val PREF_LOCK = "lock"
        const val PREF_WATCH_TYPE = "watch_type"
        const val PREF_IC_SET = "icon_set"
        const val PREF_IC_COL = "colored"
        const val PREF_PRIORITY = "priority"
        const val PREF_CAPS = "capitalize"
        const val PREF_SOS = "sos_contact"
        val MAC_PATTERN = Pattern.compile("^([A-F0-9]{2}[:]?){5}[A-F0-9]{2}$")
        const val PREF_SYNC = "last_sync"
        lateinit var btAdapter: BluetoothAdapter

        var curLock = false

        internal val RESULT_ENABLE = 1

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val actionbar = supportActionBar
        actionbar!!.setDisplayHomeAsUpEnabled(true)

        deviceManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, DeviceAdmin::class.java)

        btAdapter = BluetoothAdapter.getDefaultAdapter()

    }

    override fun onResume() {
        super.onResume()
        val setPref =  PreferenceManager.getDefaultSharedPreferences(this)
        var mac = setPref.getString(PREF_KEY_REMOTE_MAC_ADDRESS, "00:00:00:00:00:00")



        curLock = setPref.getBoolean(PREF_LOCK, false)

        if (curLock){
            curLock = deviceManager.isAdminActive(compName)
        }
        if (deviceManager.isAdminActive(compName)){
            Timber.d("Admin active")
        } else {
            Timber.d("Admin disabled")
        }
        val items = arrayListOf("MIN", "LOW", "DEFAULT", "HIGH", "MAX")
        var priority = setPref.getInt(PREF_PRIORITY, 0)
        var icSet = setPref.getBoolean(PREF_IC_SET, false)
        val set = if (icSet) R.drawable.ic_bat40w else R.drawable.ic_per40

        val names = arrayListOf(getString(R.string.start_at_boot), getString(R.string.lock_phone), getString(R.string.notify_disconnect),
            getString(R.string.mac_addr)+": $mac",getString(R.string.icon_set), getString(R.string.priority) + ": "+items[priority],getString(R.string.capitalize), getString(R.string.error_log),
            getString(R.string.app_name)+ " v"+BuildConfig.VERSION_NAME)

        states = arrayListOf( setPref.getBoolean( PREF_KEY_START_AT_BOOT, false),
            curLock, setPref.getBoolean(PREF_KEY_NOTIFY_DISCONNECT, false),null, null, null,setPref.getBoolean(PREF_CAPS, false), null,
            null)
        val icons = arrayListOf( R.drawable.ic_boot, R.drawable.ic_lock, R.drawable.ic_discon, R.drawable.ic_addr,
            set, R.drawable.ic_prt, R.drawable.ic_text,
            R.drawable.ic_info, R.drawable.ic_app)

        val myUserList = UserListAdapter(this, icons, names, null, states)
        setListView.adapter = myUserList
        setListView.setOnItemClickListener { _, _, i, _ ->


            editor = setPref.edit()
            when (i){
//                0 -> {
//                    states[i] = !states[i]!!
//                    editor.putBoolean(PREF_KEY_RUN_AS_A_SERVICE, states[i]!!)
//                }
                0 -> {
                    states[i] = !states[i]!!
                    editor.putBoolean(PREF_KEY_START_AT_BOOT, states[i]!!)
                }
                1 -> {

                    if (states[i]!!) {
                        deviceManager.removeActiveAdmin(compName)
                        states[i] = false
                        editor.putBoolean(PREF_LOCK, false)
                        Timber.d("Disabled")
                    } else {
                        Timber.d("Device admin intent")
                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
                        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, R.string.enable_lock)
                        startActivityForResult(intent, RESULT_ENABLE)
                    }
                }
                2 -> {
                    states[i] = !states[i]!!
                    editor.putBoolean(PREF_KEY_NOTIFY_DISCONNECT, states[i]!!)
                }
                3 -> {
                    val alert = AlertDialog.Builder(this)
                    var alertDialog: AlertDialog? = null
                    alert.setTitle(R.string.mac_addr)
                    var devs = ""
                    val btNames = ArrayList<String>()
                    val btAddress = ArrayList<String>()
                    if (btAdapter.isEnabled){
                        devs = getString(R.string.not_paired)
                        val devices: Set<BluetoothDevice> = btAdapter.bondedDevices
                        for (device in devices){
                            btNames.add(device.name)
                            btAddress.add(device.address)
                        }

                    } else {
                        devs = getString(R.string.turn_on_bt)
                    }
                    alert.setMessage(devs)
                    val layout = LinearLayout(this)
                    layout.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

                    val listView = ListView(this)
                    val myBTlist = BtListAdapter(this, btNames.toTypedArray(), btAddress.toTypedArray(), mac!!)
                    listView.adapter = myBTlist

                    listView.setOnItemClickListener { adapterView, view, j, l ->
                        //Toast.makeText(this, btAddress[j], Toast.LENGTH_SHORT).show()
                        editor.putString(PREF_KEY_REMOTE_MAC_ADDRESS, btAddress[j])
                        editor.apply()
                        editor.commit()
                        mac = btAddress[j]
                        names[i] = getString(R.string.mac_addr)+": $mac"
                        myUserList.notifyDataSetChanged()
                        alertDialog?.dismiss()
                    }
                    val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    params.setMargins(20, 20, 20, 20)
                    layout.addView(listView, params)
                    alert.setView(layout)
                    alert.setPositiveButton(R.string.bt_settings){_, _ ->
                        val intentOpenBluetoothSettings = Intent()
                        intentOpenBluetoothSettings.action = Settings.ACTION_BLUETOOTH_SETTINGS
                        startActivity(intentOpenBluetoothSettings)
                    }
                    alert.setNegativeButton(R.string.cancel){_, _ ->

                    }
                    alert.setOnCancelListener {
                        myUserList.notifyDataSetChanged()
                    }
                    alertDialog = alert.create()
                    alertDialog.show()
                }
                4 -> {
                    icSet = !icSet
                    editor.putBoolean(PREF_IC_SET, icSet)
                    icons[i] = if (icSet) R.drawable.ic_bat40w else R.drawable.ic_per40
                    FG.iconSet = icSet
                }
                5 -> {
                    val values = arrayListOf(0, 1, 2, 3, 4)
                    val outer = LayoutInflater.from(this).inflate(R.layout.wheel_view, null)
                    val wheelView = outer.findViewById<WheelView>(R.id.wheel_view_wv)
                    wheelView.setItems(items)
                    wheelView.setSeletion(values.indexOf(priority))

                    val dialog = AlertDialog.Builder(this)
                    dialog.setTitle(title)
                        .setView(outer)
                        .setPositiveButton(R.string.save){_, _ ->
                            priority = wheelView.seletedIndex
                            editor.putInt(PREF_PRIORITY, priority)
                            names[i] = getString(R.string.priority) + ": "+items[priority]
                            FG.prt = priority
                            myUserList.notifyDataSetChanged()
                            editor.apply()
                            editor.commit()
                        }
                        .show()
                }
                6 -> {
                    states[i] = !states[i]!!
                    editor.putBoolean(PREF_CAPS, states[i]!!)
                    FG.capitalize = states[i]!!
                }
                7 -> {
                    startActivity(Intent(this, ErrorLogActivity::class.java))
                }

            }
            myUserList.notifyDataSetChanged()
            editor.apply()
            editor.commit()
        }

//        fragmentManager.beginTransaction()
//                .replace(android.R.id.content, SettingsFragment())
//                .commit()
    }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RESULT_ENABLE -> {
                if (resultCode == Activity.RESULT_OK) {
                    states[2] = true
                    editor.putBoolean(PREF_LOCK, true)
                    editor.apply()
                    editor.commit()
                } else {
                    Toast.makeText(
                        applicationContext, R.string.not_enable,
                        Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }



    override fun onStart() {
        super.onStart()
        stopService(Intent(this, FG::class.java))
    }

    override fun onSupportNavigateUp(): Boolean1 {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    private fun checkContactPermission(): kotlin.Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ActivityCompat.checkSelfPermission( this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        return true
    }

    private fun checkSmsPermission(): kotlin.Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission( this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        return true
    }


    private fun checkCallPermission(activity: AppCompatActivity): kotlin.Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ActivityCompat.checkSelfPermission(activity , Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        return true
    }

    private fun requestContactPermission(){
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.READ_CONTACTS), MainActivity.PERMISSIONS_CONTACTS
        )
    }

    private fun requestSMSPermissions() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.READ_SMS), MainActivity.PERMISSION_SMS
        )
    }
    private fun requestCallPermissions() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.READ_PHONE_STATE), MainActivity.PERMISSION_CALL
        )
    }



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == MainActivity.PERMISSIONS_CONTACTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {

            }
        }
    }
}
