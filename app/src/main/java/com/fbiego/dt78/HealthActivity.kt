package com.fbiego.dt78

import android.graphics.Color
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.Toast
import com.fbiego.dt78.app.DataListener
import com.fbiego.dt78.app.DataReceiver
import com.fbiego.dt78.data.*
import kotlinx.android.synthetic.main.activity_health.*
import no.nordicsemi.android.ble.data.Data
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList
import com.fbiego.dt78.app.ForegroundService as FG

class HealthActivity : AppCompatActivity(), DataListener {

    private var healthList = ArrayList<HealthData>()
    private var healthAdapter = HealthAdapter(healthList)

    var measuring = false
    companion object {
        lateinit var healthRecycler: RecyclerView

        var viewH = 0
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health)

        val actionbar = supportActionBar
        actionbar!!.setDisplayHomeAsUpEnabled(true)
        DataReceiver.bindListener(this)

        healthRecycler = findViewById<View>(R.id.recyclerHealthList) as RecyclerView
        healthRecycler.layoutManager =
            LinearLayoutManager(this)

        healthRecycler.isNestedScrollingEnabled = false

        healthRecycler.apply {
            layoutManager =
                LinearLayoutManager(this@HealthActivity)
            adapter = healthAdapter
        }



    }

    override fun onResume() {
        super.onResume()


        healthList.clear()
        val dbHandler = MyDBHandler(this, null, null, 1)
        healthList = when (viewH) {
            0 -> {
                hrmCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                dbHandler.getHeart()
            }
            1 -> {
                bpCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                dbHandler.getBp()
            }
            2 -> {
                sp02Card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                dbHandler.getSp02()
            }
            else -> {
                hrmCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                dbHandler.getHeart()
            }
        }

        healthAdapter.update(healthList)



    }

    fun measure(view: View) {

        if (measuring){
            stopMeasure()
            progressBar.visibility = View.GONE
            tapInfo.text = getString(R.string.start_measure)
            measuring = false
        } else {
            if (startMeasure()){
                progressBar.visibility = View.VISIBLE
                tapInfo.text = getString(R.string.stop_measure)
                measuring = true
                valueHealth.text = "--"
            } else{
                Toast.makeText(this, R.string.not_connect, Toast.LENGTH_LONG).show()
            }

        }
    }

    private fun startMeasure(): Boolean{
        return when (viewH){
            0 -> {
                FG().sendData(byteArrayOfInts(0xAB, 0x00, 0x04, 0xFF, 0x31, 0x0A, 0x01))
            }
            1 -> {
                FG().sendData(byteArrayOfInts(0xAB, 0x00, 0x04, 0xFF, 0x31, 0x22, 0x01))
            }
            2 -> {
                FG().sendData(byteArrayOfInts(0xAB, 0x00, 0x04, 0xFF, 0x31, 0x12, 0x01))
            }
            else -> false
        }
    }

    private fun stopMeasure(){
        when (viewH){
            0 -> {
                FG().sendData(byteArrayOfInts(0xAB, 0x00, 0x04, 0xFF, 0x31, 0x0A, 0x00))
            }
            1 -> {
                FG().sendData(byteArrayOfInts(0xAB, 0x00, 0x04, 0xFF, 0x31, 0x22, 0x00))
            }
            2 -> {
                FG().sendData(byteArrayOfInts(0xAB, 0x00, 0x04, 0xFF, 0x31, 0x12, 0x00))
            }
        }
        if (measuring){
            progressBar.visibility = View.GONE
        }
    }

    fun change(view: View){

        stopMeasure()

        hrmCard.setCardBackgroundColor(Color.WHITE)
        bpCard.setCardBackgroundColor(Color.WHITE)
        sp02Card.setCardBackgroundColor(Color.WHITE)
        healthList.clear()
        val dbHandler = MyDBHandler(this, null, null, 1)
        healthList = when(view.id){
            R.id.hrmCard -> {
                viewH = 0
                hrmCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                dbHandler.getHeart()

            }
            R.id.bpCard -> {
                viewH = 1
                bpCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                dbHandler.getBp()
            }
            R.id.sp02Card -> {
                viewH = 2
                sp02Card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                dbHandler.getSp02()
            }
            else -> {
                viewH = 0
                hrmCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                dbHandler.getHeart()
            }
        }

        measuring = false

        Timber.d("list size = ${healthList.size}")
        healthAdapter.update(healthList)
    }

    override fun onPause() {
        super.onPause()
        stopMeasure()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDataReceived(data: Data) {

        val dbHandler = MyDBHandler(this, null, null, 1)
        val calendar = Calendar.getInstance(Locale.getDefault())

        if (data.getByte(4) == (0x31).toByte()){

            Timber.d("Type = ${data.getByte(5)!!.toPInt()} and value = ${data.getByte(6)!!.toPInt()}")

            if (data.getByte(5) == (0x0A).toByte()){
                val bp = data.getByte(6)!!.toPInt()

                if (bp != 0){
                    runOnUiThread {
                        if (measuring){
                            valueHealth.text = "$bp "+getString(R.string.bpm)
                        }
                    }
                    dbHandler.insertHeart(
                        HeartData(calendar.get(Calendar.YEAR)-2000,calendar.get(Calendar.MONTH)+1,
                            calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), bp)
                    )
                }
            }

            if (data.getByte(5) == (0x12).toByte()){
                val sp = data.getByte(6)!!.toPInt()

                if (sp != 0){
                    runOnUiThread {
                        if (measuring){
                            valueHealth.text = "$sp %"
                        }
                    }
                    dbHandler.insertSp02(
                        OxygenData(calendar.get(Calendar.YEAR)-2000,calendar.get(Calendar.MONTH)+1,
                            calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), sp)
                    )
                }
            }

            if (data.getByte(5) == (0x22).toByte()){
                val bph = data.getByte(6)!!.toPInt()
                val bpl = data.getByte(7)!!.toPInt()

                if (bph != 0 ){
                    runOnUiThread {
                        if (measuring){
                            valueHealth.text = "$bpl/$bph "+getString(R.string.mmHg)
                        }
                    }
                    dbHandler.insertBp(
                        PressureData(calendar.get(Calendar.YEAR)-2000,calendar.get(Calendar.MONTH)+1,
                            calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), bph, bpl)
                    )
                }
            }
        }

        healthList = when (viewH) {
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

        healthAdapter.update(healthList)

    }
}