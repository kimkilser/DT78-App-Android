package com.fbiego.dt78.data

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.preference.PreferenceManager
import android.provider.Settings.Global.getString
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.fbiego.dt78.R
import com.fbiego.dt78.app.ForegroundService as FG
import com.fbiego.dt78.app.SettingsActivity

class StepsAdapter(stepsData: ArrayList<StepsData>, steps: Int): RecyclerView.Adapter<StepsAdapter.DataHolder>() {

    private val data = mutableListOf<StepsData>()
    private val step = steps


    init {
        data.addAll(stepsData)
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): DataHolder {
        val inflater = LayoutInflater.from(p0.context)
        val view = inflater.inflate(R.layout.step_item, p0, false)
        return DataHolder(view, p0.context, step)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(p0: DataHolder, p1: Int) {
        p0.bind(data[p1])
    }

//    fun update(step: ArrayList<StepsData>){
//        this.data.clear()
//        this.data.addAll(step)
//    }

    class DataHolder(itemView: View, context: Context, stepz: Int) : RecyclerView.ViewHolder(itemView){
        private val mDistance: TextView = itemView.findViewById(R.id.distance)
        private val mTime: TextView = itemView.findViewById(R.id.time)
        private val mStep: TextView = itemView.findViewById(R.id.steps)
        private val mCalorie: TextView = itemView.findViewById(R.id.calories)
        private val card: CardView = itemView.findViewById(R.id.cardView)
        private val cnt = context
        private val stepsize = stepz

        @SuppressLint("DefaultLocale")
        fun bind (step: StepsData){
            val metres = step.steps * stepsize
            val dis = distance(metres, FG.unit!=0, cnt)

            val time = java.lang.String.format("%02d:00 - %02d:00", step.hour, step.hour+1)
            mDistance.text = dis
            mTime.text = time
            mStep.text = "${step.steps} "+cnt.resources.getString(R.string.steps)
            mCalorie.text = "${step.calories} "+cnt.resources.getString(R.string.kcal)
            card.setCardBackgroundColor(Color.parseColor("#4adf54"))
        }

    }
}