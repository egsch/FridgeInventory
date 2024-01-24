package com.example.fridgeinventory

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import com.example.fridgeinventory.databinding.ActivityFormBinding
import com.example.fridgeinventory.ui.DBOperations
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
* Note: This activity is NO LONGER IN USE!
 * todo: delete
*/

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
        var name = binding.itemName
        var description = binding.itemDescription
        var location = binding.itemLocation
        var expirationDate = binding.itemExpiration
        var expirationDateFormatted = DateTimeFormatter.ofPattern("MM/DD/yyyy")
            .parse(expirationDate.text).toString()
        var barcode = binding.itemBarcode
        var currentDate = LocalDateTime.now()
        var lifetime = "123"

        var dbOp = DBOperations()
        var id = dbOp.getNumEntries(this);
        dbOp.addItem(baseContext, name.text.toString(), barcode.text.toString(),
            expirationDateFormatted, location.text.toString(), lifetime,
            description.text.toString(), currentDate.toString())

        val formIntent = Intent(this.baseContext, MainActivity::class.java)
        startActivity(formIntent)
    }
}