package com.fbiego.dt78

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.fbiego.dt78.app.ForegroundService
import com.fbiego.dt78.app.SettingsActivity
import com.fbiego.dt78.data.MyDBHandler
import com.fbiego.dt78.data.StepsAdapter
import com.fbiego.dt78.data.StepsData
import com.fbiego.dt78.data.distance
import com.hadiidbouk.charts.BarData
import com.hadiidbouk.charts.ChartProgressBar
import com.fbiego.dt78.app.ForegroundService as FG

class StepsActivity : AppCompatActivity() {

    private var stepList = ArrayList<StepsData>()
    private var dayList = ArrayList<StepsData>()
    var day = 0
    var current = 0
    var maxDay = 0
    var stepSize = 70


    companion object {
        lateinit var stepRecycler: RecyclerView
        lateinit var next: ImageButton
        lateinit var prev: ImageButton

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_steps)

        val actionbar = supportActionBar
        actionbar!!.setDisplayHomeAsUpEnabled(true)

        next = findViewById(R.id.buttonNext)
        prev = findViewById(R.id.buttonPrev)

        stepRecycler = findViewById<View>(R.id.recyclerView) as RecyclerView
        stepRecycler.layoutManager =
            LinearLayoutManager(this)

        stepRecycler.isNestedScrollingEnabled = false

        next.setOnClickListener {
            current--
            if (current <= 0){
                next.isEnabled = false
            }
            if (current < maxDay-1){
                prev.isEnabled = true
            }
            loadDaySteps()

        }
        prev.setOnClickListener {
            current++
            if (current >= maxDay-1){
                prev.isEnabled = false
            }
            if (current > 0){
                next.isEnabled = true
            }
            loadDaySteps()

        }



    }

    @SuppressLint("DefaultLocale")
    override fun onResume() {
        super.onResume()

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        ForegroundService.lst_sync = pref.getLong(SettingsActivity.PREF_SYNC,System.currentTimeMillis() - 604800000)
        if (System.currentTimeMillis() > ForegroundService.lst_sync + (3600000 * 1)){
            if (ForegroundService().syncData()){
                Toast.makeText(this, R.string.sync_watch, Toast.LENGTH_SHORT).show()
                val editor: SharedPreferences.Editor = pref.edit()
                val time = System.currentTimeMillis()
                editor.putLong(SettingsActivity.PREF_SYNC, time)
                editor.apply()
                editor.commit()
            } else  {
                Toast.makeText(this, R.string.unable_sync, Toast.LENGTH_SHORT).show()
            }

        }

        dayList.clear()
        val dbHandler = MyDBHandler(this, null, null, 1)
        dayList = dbHandler.getDaysWithSteps()
        stepSize = dbHandler.getUser().step
        maxDay = dayList.size
        next.isEnabled = false
        prev.isEnabled = maxDay > 0

        loadDaySteps()

    }

    private fun loadDaySteps(){
        if (dayList.isNotEmpty()){

            stepList.clear()
            stepList = barChart()

            stepList.sortBy{
                it.id
            }
            var steps = 0
            var cal = 0

            stepList.forEach {
                steps += it.steps
                cal += it.calories
            }
            val stepText = findViewById<TextView>(R.id.stepsTextA)
            val calText = findViewById<TextView>(R.id.caloriesTextA)
            val disText = findViewById<TextView>(R.id.distanceTextA)
            val dayText = findViewById<TextView>(R.id.textDate)
            stepText.text = "$steps "+getString(R.string.steps)
            calText.text = "$cal "+getString(R.string.kcal)
            disText.text = distance(steps*stepSize, FG.unit!=0, this)
            val date = java.lang.String.format("%02d-%02d-20%02d", stepList[0].day, stepList[0].month,stepList[0].year)
            dayText.text = date

            val stepAdapter = StepsAdapter(stepList, stepSize)

            stepRecycler.apply {
                layoutManager =
                    LinearLayoutManager(this@StepsActivity)
                adapter = stepAdapter
            }

        }

    }

    private fun barChart(): ArrayList<StepsData>{
        val dataList = java.util.ArrayList<BarData>()
        val dbHandler = MyDBHandler(this, null, null, 1)
        val todaySteps = dbHandler.getStepsDay(dayList[current].year, dayList[current].month, dayList[current].day)
        var max = 4000
        todaySteps.forEach {
            dataList.add(BarData(it.hour.toString(), (it.steps+100).toFloat(), it.steps.toString()))
            if (it.steps > max){
                max = it.steps
            }
        }
        val mChart = findViewById<ChartProgressBar>(R.id.ChartProgressBarDay)
        mChart.setMaxValue(max.toFloat())
        mChart.setDataList(dataList)
        mChart.build()

        val stepListToday = ArrayList<StepsData>()
        todaySteps.forEach {
            if (it.steps > 0){
                stepListToday.add(it)
            }
        }
        return stepListToday
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}