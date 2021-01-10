package com.fbiego.dt78

import android.content.pm.ApplicationInfo
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.Toast
import com.fbiego.dt78.app.MainApplication
import com.fbiego.dt78.data.AppsAdapter
import com.fbiego.dt78.data.AppsData
import com.fbiego.dt78.data.AppsLoader
import kotlinx.android.synthetic.main.activity_apps.*
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

class AppsActivity : AppCompatActivity() {
    
    private val appsList = ArrayList<AppsData>()
    private lateinit var appsAdapter : AppsAdapter

    var channels = ArrayList<Int>()
    var appsPref = ArrayList<String>()

    var loadingApps = false
    
    companion object {
        lateinit var appsRecycler: RecyclerView
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_apps)

        val actionbar = supportActionBar
        actionbar!!.setDisplayHomeAsUpEnabled(true)

        appsAdapter = AppsAdapter(appsList, this@AppsActivity::appClicked, this)

        appsRecycler = findViewById<View>(R.id.recyclerAppsList) as RecyclerView
        appsRecycler.layoutManager =
            LinearLayoutManager(this)
        val div = DividerItemDecoration(
            appsRecycler.context,
            LinearLayoutManager.VERTICAL
        )
        appsRecycler.addItemDecoration(div)
        appsRecycler.isNestedScrollingEnabled = false

        appsRecycler.apply {
            layoutManager =
                LinearLayoutManager(this@AppsActivity)
            adapter = appsAdapter
        }
        appsRecycler.itemAnimator?.changeDuration = 0

    }

    override fun onResume() {
        super.onResume()

        if (!loadingApps){
            progressLoading.visibility = View.VISIBLE
            loadingApps = true

            AsyncTask.THREAD_POOL_EXECUTOR.execute(AppsLoader(this@AppsActivity))
        } else {
            progressLoading.visibility = View.GONE
        }


    }

    fun appsLoaded(names: Array<String>, installedApps: MutableList<ApplicationInfo>,
                   modifiedList: ArrayList<String>, chan: ArrayList<Int>, checkedItems: BooleanArray){

        runOnUiThread {

            loadingApps = false

            appsList.clear()
            appsPref.clear()
            channels.clear()
            names.forEach {
                val index = names.indexOf(it)
                //-------------------------------------------------------------------------
                // {add application Icon} : 2020-12-15 00:54:01 - by leejh(woono)
                //-------------------------------------------------------------------------
                val icon = installedApps[index].loadIcon(packageManager)

                appsList.add(AppsData(icon, it, installedApps[index].packageName, chan[index], checkedItems[index], true))
                if (checkedItems[index]){
                    appsPref.add(installedApps[index].packageName)
                    channels.add(chan[index])
                }
            }

            appsList.sortWith(compareBy({!it.enabled}, {it.name}))
            appsAdapter.swap(appsList)

            progressLoading.visibility = View.GONE
        }

    }
    
    private fun appClicked(appsData: AppsData, item: Int, icon: Int, state: Boolean){

        when (item){
//            0 -> {
//                val builder = AlertDialog.Builder(this)
//                builder.setTitle(appsData.name)
//                builder.setMessage("${appsData.packageName}\nChannel: ${appsData.channel}\nEnabled: ${appsData.enabled}")
//                builder.setPositiveButton("Close", null)
//                builder.show()
//            }
            1 -> {

                //Toast.makeText(this, (if (state) "Enabled" else "Disabled") +" ${appsData.name}", Toast.LENGTH_SHORT).show()
                if (state){
                    appsPref.add(appsData.packageName)
                    channels.add(appsData.channel)
                } else {
                    val index = appsPref.indexOf(appsData.packageName)
                    appsPref.removeAt(index)
                    channels.removeAt(index)
                }
            }
            2 -> {
                //Toast.makeText(this, "Clicked ${appsData.name} icon to: $icon", Toast.LENGTH_SHORT).show()
                val index = appsPref.indexOf(appsData.packageName)
                channels[index] = icon
            }
        }

    }

    override fun onPause() {
        super.onPause()

        val modifiedList = ArrayList<String>()

        if (appsPref.isNotEmpty()){
            appsPref.forEach {
                val index = appsPref.indexOf(it)
                modifiedList.add("${channels[index]},${appsPref[index]}")
                Timber.d("${channels[index]},${appsPref[index]}")
            }
        }
        MainApplication.sharedPrefs.edit().putStringSet(MainApplication.PREFS_KEY_ALLOWED_PACKAGES, modifiedList.toSet()).apply()

        //Toast.makeText(this, "Saving changes", Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}