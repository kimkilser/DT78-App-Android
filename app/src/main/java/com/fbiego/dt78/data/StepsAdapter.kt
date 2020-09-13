package com.fbiego.dt78.data

import android.annotation.SuppressLint
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.fbiego.dt78.R

class StepsAdapter(stepsData: ArrayList<StepsData>): RecyclerView.Adapter<StepsAdapter.DataHolder>() {

    private val data = mutableListOf<StepsData>()


    init {
        data.addAll(stepsData)
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): DataHolder {
        val inflater = LayoutInflater.from(p0.context)
        val view = inflater.inflate(R.layout.step_item, p0, false)
        return DataHolder(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(p0: DataHolder, p1: Int) {
        p0.bind(data[p1])
    }

//    fun update(step: ArrayList<StepsData>){
//        this.data.clear()
//        this.data.addAll(step)
//    }

    class DataHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private val mDate: TextView = itemView.findViewById(R.id.date)
        private val mTime: TextView = itemView.findViewById(R.id.time)
        private val mStep: TextView = itemView.findViewById(R.id.steps)
        private val mCalorie: TextView = itemView.findViewById(R.id.calories)
        private val card: CardView = itemView.findViewById(R.id.cardView)

        @SuppressLint("DefaultLocale")
        fun bind (step: StepsData){
            val date = java.lang.String.format("%02d/%02d/20%02d", step.day, step.month,step.year)
            mDate.text = date
            val time = java.lang.String.format("%02d:00 - %02d:00", step.hour, step.hour+1)
            mTime.text = time
            mStep.text = "${step.steps} steps"
            mCalorie.text = "${step.calories} kcal"
            card.setBackgroundColor(colors(step.day%10))
        }

    }
}