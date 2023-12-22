package com.example.fridgeinventory.ui.add

import android.Manifest.permission.*
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.fridgeinventory.CameraActivity
import com.example.fridgeinventory.FormActivity
import com.example.fridgeinventory.MainActivity
import com.example.fridgeinventory.R
import com.example.fridgeinventory.databinding.ActivityFormBinding
import com.example.fridgeinventory.databinding.FragmentDashboardBinding
import com.example.fridgeinventory.ui.DBOperations
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.*
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
                    var responseJSON = Json {ignoreUnknownKeys = true}.decodeFromString<ItemResponse>(it)
                    responseString = responseJSON.product.product_name_en
                    useResponseString(responseString)
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
        val cameraIntent = Intent(this.baseContext, CameraActivity::class.java)
        intentLauncher.launch(cameraIntent)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        var buttonSubmit = root.findViewById<Button>(R.id.submitButton)
        buttonSubmit?.setOnClickListener{
            // TODO: Validate entries!
            processSubmit()
        }
        return root
    }

    private fun processSubmit(){
        var name = view?.findViewById<EditText>(R.id.itemName)
        var description = view?.findViewById<EditText>(R.id.itemDescription)
        var location = view?.findViewById<EditText>(R.id.itemLocation)
        var expirationDate = view?.findViewById<EditText>(R.id.itemExpiration)
        var expirationDateFormatted = DateTimeFormatter.ofPattern("MM/DD/YYYY")
            .parse(expirationDate?.text).toString()
        var barcode = view?.findViewById<EditText>(R.id.itemBarcode)
        var currentDate = LocalDateTime.now()
        var lifetime = "123"

        var dbOp = DBOperations()
        var id = dbOp.getNumEntries(context);
        dbOp.addItem(baseContext, name?.text.toString(), barcode?.text.toString(),
            expirationDateFormatted, location?.text.toString(), lifetime,
            description?.text.toString(), currentDate.toString())

        val formIntent = Intent(this.baseContext, MainActivity::class.java)
        startActivity(formIntent)
    }
}