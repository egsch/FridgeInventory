package com.example.fridgeinventory

import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fridgeinventory.ui.DBOperations

class InventoryAdapter(private val dataSet: ArrayList<DBItemEntry>) :
    RecyclerView.Adapter<InventoryAdapter.ViewHolder>()  {

    private val mCursor: Cursor? = null
    private var expandedPosition = -1

    class ViewHolder (view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView
        val barcodeTextView: TextView
        val mainView : View = view
        val otherView : View
        val dateTextView : TextView
        val removeButton : Button
        init {
            // Define click listener for the ViewHolder's View
            textView = view.findViewById(R.id.inventory_item_title)
            barcodeTextView = view.findViewById(R.id.inventory_item_barcode)
            otherView = view.findViewById(R.id.extra_view)
            dateTextView = view.findViewById(R.id.inventory_item_date)
            removeButton = view.findViewById(R.id.remove_item_button)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return dataSet.size;
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (dataSet[position].deleted) {
            holder.mainView.visibility = View.GONE
            return
        }
        var isOpened = (expandedPosition == position)
        holder.textView.text = dataSet[position].name
        holder.barcodeTextView.text = dataSet[position].barcode
        holder.dateTextView.text = "Expires " + dataSet[position].expiration
        holder.otherView.visibility = if (isOpened) View.VISIBLE else View.GONE

        holder.mainView.setOnClickListener {
            // expand
            expandedPosition = if (isOpened) -1 else position
            notifyItemChanged(position)
        }
        holder.removeButton.setOnClickListener {
            var dbOp = DBOperations()
            dbOp.removeItem(holder.itemView.context, dataSet[position].id)
            dataSet.removeAt(position) // todo: not efficient?
            notifyItemRemoved(position)
        }
    }
}