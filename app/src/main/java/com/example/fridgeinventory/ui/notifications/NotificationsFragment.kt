package com.example.fridgeinventory.ui.notifications

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fridgeinventory.MainActivity
import com.example.fridgeinventory.R
import com.example.fridgeinventory.databinding.FragmentNotificationsBinding
import com.example.fridgeinventory.ui.DBOperations
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class NotificationsFragment : Fragment() {
    private var _binding: FragmentNotificationsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this).get(NotificationsViewModel::class.java)

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textNotifications
        var locations = DBOperations().getLocations(context)
        textView.text = locations.toString()

        var buttonSubmit = root.findViewById<Button>(R.id.submit_location)
        buttonSubmit?.setOnClickListener{
            processSubmit()
        }

        return root
    }

    private fun processSubmit(){
        var locationName = view?.findViewById<EditText>(R.id.input_location)

        var dbOp = DBOperations()
        var returnCode = dbOp.addLocation(context, locationName?.text.toString())
        Log.d("processing adding location", returnCode.toString())
        if (!returnCode) {
            locationName?.error = "Location already exists"
        } else {
            val textView: TextView = binding.textNotifications
            var locations = DBOperations().getLocations(context)
            textView.text = locations.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}