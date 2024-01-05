package com.example.fridgeinventory.ui.add

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
import com.example.fridgeinventory.databinding.FragmentDashboardBinding
import com.example.fridgeinventory.ui.DBOperations
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.*
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.TimeZone

class AddFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var baseContext: Context
    private val httpClient = OkHttpClient()

    @Serializable class ItemResponse (val product : FoodItem)
    @Serializable class FoodItem (val product_name_en: String = "")
    fun useResponseString(responseString :  String) {
        val nameItem = view?.findViewById<EditText>(R.id.itemName)
        nameItem?.post {
            nameItem.setText(responseString)
        }
    }
    fun get(url: String) {
        val request = Request.Builder()
            .url(url)
            .build()
        var responseString : String
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseStr = response.body?.string()
                responseStr?.let {
                    try {
                        val responseJSON = Json {ignoreUnknownKeys = true}.decodeFromString<ItemResponse>(it)
                        responseString = responseJSON.product.product_name_en
                        useResponseString(responseString)
                    } catch (exception : MissingFieldException) {
                        Log.d("No food found", "food does not exist or has no English name")
                    }
                }

            }

            override fun onFailure(call: Call, e: IOException) {
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

        val buttonSubmit = root.findViewById<Button>(R.id.submitButton)
        buttonSubmit?.setOnClickListener{
            processSubmit()
        }

        val dbHelper = DBHelper(context)
        val db = dbHelper.readableDatabase
        // var results : ArrayList<String> = ArrayList<String>()
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
                    val resultBarcode = result.data?.getStringExtra("barcode")
                    val barcodeItem = view?.findViewById<EditText>(R.id.itemBarcode)
                    barcodeItem?.setText(resultBarcode.toString())
                    // get data from OpenFoodFacts API
                    // get https://world.openfoodfacts.org/api/v2/product/<barcode>.json
                    get("https://world.openfoodfacts.org/api/v2/product/$result.json")
                }
            }

        val buttonCamera = root.findViewById<Button>(R.id.cameraButton)
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
        val name = view?.findViewById<EditText>(R.id.itemName)
        val description = view?.findViewById<EditText>(R.id.itemDescription)
        val location = view?.findViewById<Spinner>(R.id.itemLocation)
        val expirationDate = view?.findViewById<EditText>(R.id.itemExpiration)
        val expirationDateFormatted : String
        try {
            // TODO: fix date sql sortable
            // TODO: Read more about week-era-year (yyyy vs YYYY)
            val formatter = DateTimeFormatter.ofPattern("MM/DD/yyyy")
            val expirationDateParsed = formatter
                .withZone(TimeZone.getDefault().toZoneId())
                .parse(expirationDate?.text.toString())
            expirationDateFormatted = formatter.format(expirationDateParsed)
        } catch (exception : DateTimeParseException) {
            highlightFieldRed(expirationDate)
            return
        }
        val barcode = view?.findViewById<EditText>(R.id.itemBarcode)
        val currentDate = LocalDateTime.now()
        val lifetime = view?.findViewById<EditText>(R.id.itemLifetime)
        val selectedItem = location?.selectedItem as Cursor

        val dbOp = DBOperations()
        dbOp.addItem(baseContext, name?.text.toString(), barcode?.text.toString(),
            expirationDateFormatted, selectedItem.getString(1).toString(), lifetime.toString(),
            description?.text.toString(), currentDate.toString())

        var bulder = NotificationCompat.Builder(this.baseContext)

        val formIntent = Intent(this.baseContext, MainActivity::class.java)
        startActivity(formIntent)
    }
}