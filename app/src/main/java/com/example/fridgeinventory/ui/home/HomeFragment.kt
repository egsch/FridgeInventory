package com.example.fridgeinventory.ui.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SearchView.OnCloseListener
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.R
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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
    var searchString : String = ""
    var intentLauncher : ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val searchBar = binding.searchBar
            searchBar.setQuery(result.data?.getStringExtra("barcode"), true)
            searchBar.isIconified = false
            searchBar.requestFocus()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // val textView: TextView = binding.textHome
        val dbOperations = DBOperations()
        val dataset = dbOperations.readData(context);

        val inventoryAdapter = InventoryAdapter(dataset)

        val recyclerView: RecyclerView = binding.inventoryRv
        recyclerView.adapter = inventoryAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.addItemDecoration(
            DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
        )

        val cameraButton = binding.cameraButton
        cameraButton.setOnClickListener {
            val cameraIntent = Intent(context, CameraActivity::class.java)
            intentLauncher.launch(cameraIntent)
        }

        val searchBar = binding.searchBar
        searchBar.setOnQueryTextListener(object : android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                var dataset : ArrayList<DBItemEntry>
                if (query.isNullOrEmpty()) {
                    dataset = dbOperations.readData(context)
                    val recyclerView: RecyclerView = binding.inventoryRv
                    val inventoryAdapter = InventoryAdapter(dataset)
                    recyclerView.adapter = inventoryAdapter
                    return true
                } else {
                    dataset = dbOperations.searchData(context, query)
                    val recyclerView: RecyclerView = binding.inventoryRv
                    val inventoryAdapter = InventoryAdapter(dataset)
                    recyclerView.adapter = inventoryAdapter
                    return true
                };
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        searchBar.setOnCloseListener(object : OnCloseListener{
            override fun onClose(): Boolean {
                var dataset = dbOperations.readData(context)
                val recyclerView: RecyclerView = binding.inventoryRv
                val inventoryAdapter = InventoryAdapter(dataset)
                recyclerView.adapter = inventoryAdapter
                return true
            }
        })

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}