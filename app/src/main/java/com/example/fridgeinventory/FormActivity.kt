package com.example.fridgeinventory

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import com.example.fridgeinventory.databinding.ActivityFormBinding
import com.example.fridgeinventory.ui.DBOperations
import java.time.LocalDateTime

class FormActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
private lateinit var binding: ActivityFormBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

     binding = ActivityFormBinding.inflate(layoutInflater)
     setContentView(binding.root)

        var extras: Bundle? = intent.extras
        var barcode = extras?.getString("barcode")
        var barcodeItem = findViewById<EditText>(R.id.itemBarcode)
        barcodeItem.setText(barcode.toString())

        var buttonSubmit = findViewById<Button>(R.id.submitButton)
        buttonSubmit.setOnClickListener{processSubmit()}
    }

    private fun processSubmit(){
        var name = findViewById<EditText>(R.id.itemName)
        var description = findViewById<EditText>(R.id.itemDescription)
        var location = findViewById<EditText>(R.id.itemLocation)
        var expirationDate = findViewById<EditText>(R.id.itemExpiration)
        var barcode = findViewById<EditText>(R.id.itemBarcode)
        var currentDate = LocalDateTime.now()
        var lifetime = "123"

        var dbOp = DBOperations()
        var id = dbOp.getNumEntries(this);
        dbOp.addItem(baseContext, id, name.text.toString(), barcode.text.toString(),
            expirationDate.text.toString(), location.text.toString(), lifetime,
            description.text.toString(), currentDate.toString())
    }
}