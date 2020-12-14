package com.fbiego.dt78.data

import android.content.Context
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.fbiego.dt78.R
import com.fbiego.dt78.app.ForegroundService.Companion.dt78

class AppsAdapter(appsData: ArrayList<AppsData>, private val listener: (AppsData, Int, Int, Boolean) -> Unit, private val context: Context): RecyclerView.Adapter<AppsAdapter.DataHolder>() {

    private val data = mutableListOf<AppsData>()
    private var inflater = LayoutInflater.from(context)
    val adapter = NotifyAdapter(context, false)

    init {
        data.addAll(appsData)

    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): DataHolder {
        //val inflater = LayoutInflater.from(p0.context)
        val view = inflater.inflate(R.layout.app_item, p0, false)
        return DataHolder(view, adapter)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(p0: DataHolder, p1: Int) {
        p0.bind(data[p1], listener, p1)
    }

    fun swap(apps: ArrayList<AppsData>){
        val diffCallback = AppDiffCallback(this.data, apps)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.data.clear()
        this.data.addAll(apps)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onViewRecycled(holder: DataHolder) {
        holder.mCheckBox.setOnCheckedChangeListener(null)
        holder.mSpinner.onItemSelectedListener = null
        super.onViewRecycled(holder)
    }





    class DataHolder(itemView: View, private val notifyAdapter: NotifyAdapter) : RecyclerView.ViewHolder(itemView){
        private val mName: TextView = itemView.findViewById(R.id.appName)
        val mCheckBox: CheckBox = itemView.findViewById(R.id.appChecked)
        private val mIcon: ImageView = itemView.findViewById(R.id.appIcon)
        val mSpinner: Spinner = itemView.findViewById(R.id.appSpinner)

        fun bind (apps: AppsData, listener: (AppsData, Int, Int, Boolean) -> Unit, pos: Int){

            var pass = false

            mName.text = apps.name
            mCheckBox.isChecked = apps.enabled
            if (apps.channel == 0){
                apps.channel = checkPackage(apps.packageName, dt78)
            }

            //mIcon.setImageResource(icons(apps.channel))
            mIcon.setImageDrawable(apps.icon)

            mSpinner.adapter = notifyAdapter

            if (apps.enabled){
                mSpinner.visibility = View.VISIBLE
            } else {
                mSpinner.visibility = View.GONE
            }

            val onSelectListener = object : AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(p0: AdapterView<*>?) {

                }

                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    if (pass){
                        val icon = mSpinner.selectedItem as Int
                        listener(apps, 2, icon, true)
                    }
                    pass = true

                }

            }


            itemView.setOnClickListener {
                listener(apps, 0, pos, true)
            }
            mCheckBox.setOnCheckedChangeListener { compoundButton, b ->
                apps.enabled = b
                if (apps.enabled){
                    mSpinner.visibility = View.VISIBLE
                } else {
                    mSpinner.visibility = View.GONE
                }
                listener(apps, 1, pos, b)
            }

            mSpinner.setSelection(spinner(apps.channel))
            mSpinner.onItemSelectedListener = onSelectListener


        }



    }

}