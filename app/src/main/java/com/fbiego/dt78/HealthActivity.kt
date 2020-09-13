package com.fbiego.dt78

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import com.fbiego.dt78.data.HealthAdapter
import com.fbiego.dt78.data.HealthData
import com.fbiego.dt78.data.MyDBHandler
import com.fbiego.dt78.data.StepsAdapter

class HealthActivity : AppCompatActivity() {

    private var healthList = ArrayList<HealthData>()

    companion object {
        lateinit var healthRecycler: RecyclerView
        var view = 0
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health)

        val actionbar = supportActionBar
        actionbar!!.setDisplayHomeAsUpEnabled(true)

        healthRecycler = findViewById<View>(R.id.recyclerHealthList) as RecyclerView
        healthRecycler.layoutManager = LinearLayoutManager(this)

        healthRecycler.isNestedScrollingEnabled = false


    }

    override fun onResume() {
        super.onResume()


        healthList.clear()
        val dbHandler = MyDBHandler(this, null, null, 1)
        healthList = when (view) {
            0 -> {
                dbHandler.getHeart()
            }
            1 -> {
                dbHandler.getBp()
            }
            2 -> {
                dbHandler.getSp02()
            }
            else -> {
                dbHandler.getHeart()
            }
        }

        val healthAdapter = HealthAdapter(healthList)

        healthRecycler.apply {
            layoutManager = LinearLayoutManager(this@HealthActivity)
            adapter = healthAdapter
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}