package com.fbiego.dt78

import android.app.TimePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.fbiego.dt78.data.*
import kotlinx.android.synthetic.main.activity_setings.*
import com.fbiego.dt78.app.SettingsActivity as ST
import com.fbiego.dt78.app.ForegroundService as FG

class SettingsWatchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setings)

        val actionbar = supportActionBar
        actionbar!!.setDisplayHomeAsUpEnabled(true)


        val setPref =  PreferenceManager.getDefaultSharedPreferences(this)
        var hr24 = !(setPref.getBoolean(ST.PREF_12H, false))
        //var dt78 = setPref.getBoolean(ST.PREF_WATCH_TYPE, true)
        var dt78 = FG.dt78
        val dbHandler = MyDBHandler(this, null, null, 1)

        var names = arrayListOf(getString(R.string.hr12_sys), getString(R.string.hourly), unit(setPref.getBoolean(ST.PREF_UNITS, false), this),
            getString(R.string.watch_type)+type(dt78)+"\t\t"+FG.watchVersion)

        var units = setPref.getBoolean(ST.PREF_UNITS, false)

        var states : ArrayList<Boolean?> = arrayListOf( !hr24,
            setPref.getBoolean(ST.PREF_HOURLY, false), null, null)
        var icons = arrayListOf(R.drawable.ic_12hr, R.drawable.ic_hourly, R.drawable.ic_klm, R.drawable.ic_watch)

        if (!dt78){
            names.add(getString(R.string.frequent))
            states.add(null)
            icons.add(R.drawable.ic_people)

        }

        val myUserList = UserListAdapter(this, icons, names, null, states)
        settingsListView.adapter = myUserList
        settingsListView.setOnItemClickListener { _, _, i, _ ->

            val editor: SharedPreferences.Editor = setPref.edit()
            when (i){
//                0 -> {
//                     states[i] = !states[i]!!
//                    editor.putBoolean(ST.PREF_RTW, states[i]!!)
//
//                    val rtw = byteArrayOfInts(0xAB, 0x00, 0x04, 0xFF, 0x77, 0x80, if (states[i]!!) 1 else 0)
//                    FG().sendData(rtw)
//                }
                0 -> {
                    states[i] = !states[i]!!
                    editor.putBoolean(ST.PREF_12H, states[i]!!)
                    hr24 = states[i]!!
                    FG().update12hr(states[i]!!)
                }
                1 -> {
                    states[i] = !states[i]!!
                    editor.putBoolean(ST.PREF_HOURLY, states[i]!!)
                    val hrl = byteArrayOfInts(0xAB, 0x00, 0x04, 0xFF, 0x78, 0x80, if (states[i]!!) 1 else 0)
                    FG().sendData(hrl)
                }
                2 -> {
                    units = !units
                    editor.putBoolean(ST.PREF_UNITS, units)
                    val user = dbHandler.getUser()
                    FG().updateUser(user, if (units) 1 else 0)
                    FG.unit = if (units) 1 else 0
                    names[i] = unit(units, this)
                }
                3 -> {
                    //dt78 = !dt78
                    //editor.putBoolean(ST.PREF_WATCH_TYPE, dt78)
                    //FG.dt78 = dt78
                    //names[i] = getString(R.string.watch_type)+type(dt78)
                }
                4 -> {
                    startActivity(Intent(this, ContactsActivity::class.java))


                }
            }

            myUserList.notifyDataSetChanged()
            editor.apply()
            editor.commit()
        }


        var current = ArrayList<Int>()
        val quiet = dbHandler.getSet(2)
        current.clear()
        if (quiet.isNotEmpty()){
            current.addAll(quiet)
        } else {
            val slp = arrayOf(2, 22, 0, 7, 0, 0, 0)
            current.addAll(slp)
            dbHandler.insertSet(current)
            FG().updateQuiet(current)

        }
        if (dt78){
            watchFaces.visibility = View.VISIBLE
        } else {
            watchFaces.visibility = View.GONE
        }
        val start = java.lang.String.format("%02d:%02d", current[1], current[2])
        val end = java.lang.String.format("%02d:%02d", current[3], current[4])
        quietStart.text = start
        quietEnd.text = end
        quietEnable.isChecked = current[5]!=0

        quietStart.setOnClickListener{
            val picker = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, i, i2 ->
                current[1] = i
                current[2] = i2
                quietStart.text = String.format("%02d:%02d", i, i2)
                dbHandler.insertSet(current)
                FG().updateQuiet(current)
            }, current[1], current[2], hr24)
            picker.show()
        }
        quietEnd.setOnClickListener {
            val picker = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, i, i2 ->
                current[3] = i
                current[4] = i2
                quietEnd.text = String.format("%02d:%02d", i, i2)
                dbHandler.insertSet(current)
                FG().updateQuiet(current)
            }, current[3], current[4], hr24)
            picker.show()
        }
        quietEnable.setOnCheckedChangeListener { _, b ->
            val st = if (b) 1 else 0
            current[5] = st
            dbHandler.insertSet(current)
            FG().updateQuiet(current)
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

//    override fun onConnectionChanged(state: Boolean) {
//        runOnUiThread{
//            if (state){
//                watchFaces.visibility = View.VISIBLE
//                settingsListView.visibility = View.VISIBLE
//            } else {
//                watchFaces.visibility = View.GONE
//                settingsListView.visibility = View.GONE
//            }
//        }
//    }

    fun watchFace(view: View){
         when (view.id){
             R.id.watch1 -> {
                 if (!FG().sendData(byteArrayOfInts(0xAB, 0x00, 0x06, 0xFF, 0x95, 0x80, 0x01, 0x0F, 0x0C))){
                     Toast.makeText(this, R.string.not_connect, Toast.LENGTH_SHORT).show()
                 }
             }
             R.id.watch2 -> {
                 if (!FG().sendData(byteArrayOfInts(0xAB, 0x00, 0x06, 0xFF, 0x95, 0x80, 0x01, 0x0F, 0x0D))){
                     Toast.makeText(this, R.string.not_connect, Toast.LENGTH_SHORT).show()
                 }
             }
             R.id.watch3 -> {
                 if (!FG().sendData(byteArrayOfInts(0xAB, 0x00, 0x06, 0xFF, 0x95, 0x80, 0x01, 0x0F, 0x0E))){
                     Toast.makeText(this, R.string.not_connect, Toast.LENGTH_SHORT).show()
                 }
             }
         }
    }
}