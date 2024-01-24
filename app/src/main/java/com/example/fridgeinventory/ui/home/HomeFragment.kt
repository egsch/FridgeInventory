package com.example.fridgeinventory.ui.home

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fridgeinventory.*
import com.example.fridgeinventory.databinding.FragmentHomeBinding
import com.example.fridgeinventory.ui.DBOperations

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var intentLauncher : ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val searchBar = binding.searchBar
            searchBar.setQuery(result.data?.getStringExtra("barcode"), true)
            searchBar.isIconified = false
            searchBar.requestFocus()
        }
    }

    inner class SpinnerActivity : Activity(), AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val searchBar = binding.searchBar
            val query = searchBar.query
            onQueryTextSubmitFunc(query.toString())
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            // do nothing
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // get filter arguments:
        var filter = arguments?.getInt("filterArg")
        Log.d("filterArg", filter.toString())
        var filterString = ""
        if (filter != null && filter >= 0) {
            Log.d("filterArg", filter.toString())
            filterString = filter.toString()
        }

        val dbOperations = DBOperations()
        val dataset = dbOperations.readData(context, filterString, DBContract.ItemEntry.EXPIRATION_COL)

        // set up recycler view for inventory contents
        val inventoryAdapter = InventoryAdapter(dataset)
        val recyclerView: RecyclerView = binding.inventoryRv
        recyclerView.adapter = inventoryAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.addItemDecoration(
            DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
        )

        // add link to camera
        val cameraButton = binding.cameraButton
        cameraButton.setOnClickListener {
            val cameraIntent = Intent(context, CameraActivity::class.java)
            intentLauncher.launch(cameraIntent)
        }

        val dbHelper = DBHelper(context)
        val db : SQLiteDatabase = dbHelper.readableDatabase
        val spinner: Spinner = binding.filterLocationSpinner
        val sortSpinner : Spinner = binding.sortSpinner

        // set up location spinner
        val cursor : Cursor = db.rawQuery("SELECT ${DBContract.LocationEntry.NAME_COL} as _id,${DBContract.LocationEntry.NAME_COL} FROM ${DBContract.LocationEntry.TABLE_NAME};", null)
        val locationOptions = ArrayList<String>()
        context?.getString(R.string.default_loc_filter_label)?.let { locationOptions.add(it) }
        if (cursor.moveToFirst()) {
            do {
                locationOptions.add(cursor.getString(1))
            } while (cursor.moveToNext())
        }
        cursor.close()
        val arrayAdapter = ArrayAdapter(
            context!!,
            android.R.layout.simple_spinner_dropdown_item,
            locationOptions
        )
        spinner.adapter = arrayAdapter
        spinner.onItemSelectedListener = SpinnerActivity()
        if (filter != null && filter > 0) {
            spinner.setSelection(filter)
        }


        // set up sort spinner
        val sortOptions = ArrayList<String>()
        val dateOption = context?.getString(R.string.date_sort_option)
        val nameOption = context?.getString(R.string.alpha_sort_option)
        val expirationOption = context?.getString(R.string.expiration_sort_option)
        sortOptions.add(expirationOption.toString())
        sortOptions.add(nameOption.toString())
        sortOptions.add(dateOption.toString())
        val sortArrayAdapter = ArrayAdapter(
            context!!,
            android.R.layout.simple_spinner_dropdown_item,
            sortOptions
        )
        sortSpinner.adapter = sortArrayAdapter
        sortSpinner.onItemSelectedListener = SpinnerActivity()

        // set up search bar action
        val searchBar = binding.searchBar
        searchBar.setOnQueryTextListener(object : android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = onQueryTextSubmitFunc(query)

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
        searchBar.setOnCloseListener { onQueryTextSubmitFunc("") }

        return root
    }

    fun onQueryTextSubmitFunc(query: String?): Boolean {
        val dataset : ArrayList<DBItemEntry>
        val dbOperations = DBOperations()
        // get filter
        var filter: String = binding.filterLocationSpinner.selectedItem as String
        if (filter == context?.getString(R.string.default_loc_filter_label).toString()) {
            filter = ""
        }
        // get sort
        var sort: String = binding.sortSpinner.selectedItem as String
        if (sort == context?.getString(R.string.date_sort_option).toString()) {
            sort = DBContract.ItemEntry.DATE_COL
        } else if (sort == context?.getString(R.string.expiration_sort_option).toString()) {
            sort = DBContract.ItemEntry.EXPIRATION_COL
        } else if (sort == context?.getString(R.string.alpha_sort_option).toString()) {
            sort = DBContract.ItemEntry.NAME_COL + " COLLATE NOCASE"
        }

        if (query.isNullOrEmpty()) {
            dataset = dbOperations.readData(context, filter, sort)
            val recyclerView: RecyclerView = binding.inventoryRv
            val inventoryAdapter = InventoryAdapter(dataset)
            recyclerView.adapter = inventoryAdapter
            return true
        } else {
            dataset = dbOperations.searchData(context, query, filter, sort)
            val recyclerView: RecyclerView = binding.inventoryRv
            val inventoryAdapter = InventoryAdapter(dataset)
            recyclerView.adapter = inventoryAdapter
            return true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}