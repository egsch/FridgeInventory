package com.example.fridgeinventory.ui.locations

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fridgeinventory.DBOperations
import com.example.fridgeinventory.R
import com.example.fridgeinventory.databinding.FragmentLocationsBinding

class LocationsFragment : Fragment() {
    private var _binding: FragmentLocationsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    class LocationAdapter(private var dataSet: ArrayList<String>) :
        RecyclerView.Adapter<LocationAdapter.ViewHolder>()  {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: TextView
            val deleteIcon : ImageView
            init {
                // Define click listener for the ViewHolder's View
                textView = view.findViewById(R.id.location_list_title)
                deleteIcon = view.findViewById(R.id.delete_icon)
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.location_list_item, parent, false)

            return ViewHolder(view)
        }

        override fun getItemCount(): Int {
            return dataSet.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textView.text = dataSet[position]

            holder.itemView.setOnClickListener{
                // navigate to home/list view using navController
                val bundle = bundleOf("filterArg" to position)
                holder.itemView.findNavController().navigate(R.id.navigation_home, bundle)
            }

            holder.deleteIcon.setOnClickListener{
                var dbOp = DBOperations()
                if (dbOp.removeLocation(it.context,dataSet[position])) {
                    dataSet = DBOperations().getLocations(it.context)
                    Log.d("dataset", dataSet.toString())
                    notifyDataSetChanged()
                } else {
                    Log.d("here", dataSet.toString())
                    Toast.makeText(it.context, "Remove items in location before deleting location", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationsBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val locations = DBOperations().getLocations(context)

        val locationAdapter = LocationAdapter(locations)
        val recyclerView = binding.locationRv
        recyclerView.adapter = locationAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.addItemDecoration(
            DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
        )

        val buttonSubmit = binding.submitLocation
        buttonSubmit?.setOnClickListener{
            processSubmit()
        }

        return root
    }

    private fun processSubmit(){
        val locationName = binding.inputLocation

        val dbOp = DBOperations()
        val returnCode = dbOp.addLocation(context, locationName?.text.toString())
        Log.d("processing adding location", returnCode.toString())
        if (!returnCode) {
            locationName?.error = getString(R.string.location_already_exists)
        } else {
            val locations = DBOperations().getLocations(context)
            val locationAdapter = LocationAdapter(locations)
            val recyclerView = binding.locationRv
            recyclerView.adapter = locationAdapter
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.addItemDecoration(
                DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
            )
            locationName?.setText("")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}