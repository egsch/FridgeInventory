package com.example.fridgeinventory.ui.add

import android.Manifest.permission.*
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import com.example.fridgeinventory.*
import com.example.fridgeinventory.databinding.ActivityFormBinding
import com.example.fridgeinventory.databinding.FragmentDashboardBinding
import com.example.fridgeinventory.ui.DBOperations
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.internal.format
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.TimeZone

class AddFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var baseContext: Context
    private val httpClient = OkHttpClient()
    private val nameText = ""
    @Serializable class ItemResponse (val product : FoodItem)
    @Serializable class FoodItem (val product_name_en: String = "")
    fun useResponseString(responseString :  String) {
        var nameItem = view?.findViewById<EditText>(R.id.itemName)
        nameItem?.post(Runnable {
            nameItem.setText(responseString)
        })
    }
    fun get(url: String ) {
        val request = Request.Builder()
            .url(url)
            .build()
        var responseString : String
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                var responseStr = response.body?.string()
                responseStr?.let {
                    try {
                        var responseJSON = Json {ignoreUnknownKeys = true}.decodeFromString<ItemResponse>(it)
                        responseString = responseJSON.product.product_name_en
                        useResponseString(responseString)
                    } catch (exception : MissingFieldException) {
                    }
                }

            }

            override fun onFailure(call: Call, e: IOException) {
                // do something
            }
        })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        baseContext = context
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        var buttonSubmit = root.findViewById<Button>(R.id.submitButton)
        buttonSubmit?.setOnClickListener{
            processSubmit()
        }

        val dbHelper = DBHelper(context)
        val db = dbHelper.readableDatabase
        var results : ArrayList<String> = ArrayList<String>()
        val spinner: Spinner = root.findViewById(R.id.itemLocation)
        val cursor : Cursor = db.rawQuery("SELECT ${DBContract.LocationEntry.NAME_COL} as _id,${DBContract.LocationEntry.NAME_COL} FROM ${DBContract.LocationEntry.TABLE_NAME};", null)
        val columnArray = arrayOf(DBContract.LocationEntry.NAME_COL)
        val viewArray = intArrayOf(android.R.id.text1)
        SimpleCursorAdapter(context, android.R.layout.simple_spinner_item, cursor, columnArray, viewArray, 0).also { adapter ->
            // Specify the layout to use when the list of choices appears.
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner.
            spinner.adapter = adapter
        }

        val intentLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    var result = result.data?.getStringExtra("barcode")
                    var barcodeItem = view?.findViewById<EditText>(R.id.itemBarcode)
                    barcodeItem?.setText(result.toString())
                    // get data from OpenFoodFacts API
                    // get https://world.openfoodfacts.org/api/v2/product/<barcode>.json
                    var jsonResponse = get("https://world.openfoodfacts.org/api/v2/product/" + result + ".json")
                }
            }

        var buttonCamera = root.findViewById<Button>(R.id.cameraButton)
        buttonCamera?.setOnClickListener{

            Log.d("here", "here")
            val cameraIntent = Intent(this.baseContext, CameraActivity::class.java)
            intentLauncher.launch(cameraIntent)

        }

        return root
    }

    private fun highlightFieldRed(editText: EditText?) {
        editText?.setError("Incorrect Date Format")
    }

    private fun processSubmit(){
        var name = view?.findViewById<EditText>(R.id.itemName)
        var description = view?.findViewById<EditText>(R.id.itemDescription)
        var location = view?.findViewById<Spinner>(R.id.itemLocation)
        var expirationDate = view?.findViewById<EditText>(R.id.itemExpiration)
        var expirationDateFormatted : String
        try {
            // TODO: Replace toString with other date format. Use time zones?
            val formatter = DateTimeFormatter.ofPattern("MM/DD/YYYY")
            val expirationDateParsed = formatter
                .withZone(TimeZone.getDefault().toZoneId())
                .parse(expirationDate?.text.toString())
            expirationDateFormatted = formatter.format(expirationDateParsed)
        } catch (exception : DateTimeParseException) {
            highlightFieldRed(expirationDate)
            return
        }
        var barcode = view?.findViewById<EditText>(R.id.itemBarcode)
        var currentDate = LocalDateTime.now()
        var lifetime = view?.findViewById<EditText>(R.id.itemLifetime)
        var selectedItem = location?.selectedItem as Cursor

        var dbOp = DBOperations()
        dbOp.addItem(baseContext, name?.text.toString(), barcode?.text.toString(),
            expirationDateFormatted, selectedItem.getString(1).toString(), lifetime.toString(),
            description?.text.toString(), currentDate.toString())

        var bulder = NotificationCompat.Builder(this.baseContext)

        val formIntent = Intent(this.baseContext, MainActivity::class.java)
        startActivity(formIntent)
    }
}