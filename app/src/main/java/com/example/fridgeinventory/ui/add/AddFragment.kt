package com.example.fridgeinventory.ui.add
import android.app.Activity
import android.Manifest
import android.app.AlarmManager
import android.app.AlarmManager.OnAlarmListener
import android.app.AlarmManager.RTC
import android.app.PendingIntent
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class AddFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var baseContext: Context
    private val httpClient = OkHttpClient()

    @Serializable class ItemResponse (val product : FoodItem)
    @Serializable class FoodItem (val product_name_en: String = "")
    fun useResponseString(responseString :  String) {
        val nameItem = binding.itemName
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

        val buttonSubmit = binding.submitButton
        buttonSubmit?.setOnClickListener{
            processSubmit()
        }

        val dbHelper = DBHelper(context)
        val db = dbHelper.readableDatabase
        // var results : ArrayList<String> = ArrayList<String>()
        val spinner: Spinner = binding.itemLocation
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
                    val barcodeItem = binding.itemBarcode
                    barcodeItem?.setText(resultBarcode.toString())
                    // get data from OpenFoodFacts API
                    // get https://world.openfoodfacts.org/api/v2/product/<barcode>.json
                    get("https://world.openfoodfacts.org/api/v2/product/$resultBarcode.json")
                }
            }

        val buttonCamera = binding.cameraButton
        buttonCamera?.setOnClickListener{
            val cameraIntent = Intent(this.baseContext, CameraActivity::class.java)
            intentLauncher.launch(cameraIntent)
        }

        return root
    }

    private fun highlightFieldRed(editText: EditText?) {
        editText?.setError(getString(R.string.incorrect_date_format))
    }

    private fun processSubmit(){
        val name = binding.itemName
        val description = binding.itemDescription
        val location = binding.itemLocation
        val expirationDate = binding.itemExpiration
        val barcode = binding.itemBarcode
        val lifetime = binding.itemLifetime
        val selectedItem = location?.selectedItem as Cursor // selected location from spinner
        val currentDate = LocalDateTime.now()

        // check if date is formatted correctly; format in YYYY/MM/DD for sorting
        var dateMilliseconds : Long
        val expirationDateFormatted : String
        try {
            // convert dates to year-month-day by parsing and then reformatting
            // also checks if date is correctly formatted
            val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
            val expirationDateParsed = LocalDate.parse(expirationDate?.text.toString(), formatter)
                .atStartOfDay().atZone(ZoneId.systemDefault())
            dateMilliseconds = expirationDateParsed.toInstant().toEpochMilli()
            val ymdFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
            expirationDateFormatted = ymdFormatter.format(expirationDateParsed)
        } catch (exception : DateTimeParseException) {
            // notify of mistake
            highlightFieldRed(expirationDate)
            return
        }

        // add item to database
        val dbOp = DBOperations()
        val rowId = dbOp.addItem(baseContext, name?.text.toString(), barcode?.text.toString(),
            expirationDateFormatted, selectedItem.getString(1).toString(), lifetime.toString(),
            description?.text.toString(), currentDate.toString())

        // if added correctly, set alarm for notification
        if (rowId != null) {
            // call sendNotificationNow() on alarm
            val listener = OnAlarmListener{sendNotificationNow(rowId)}
            val alarmManager = context?.getSystemService(ALARM_SERVICE) as AlarmManager
            var lifetimeOffset = 0
            if (lifetime?.text.toString() != "" && lifetime != null) {
                lifetimeOffset = Integer.parseInt(lifetime.text.toString()) * 86400000
            }
            var date = dateMilliseconds + lifetimeOffset
            // used window alarm since we don't need exact alarm - could switch to just set()
            // currently sending at midnight in current time zone (at time of addition)
            alarmManager.setWindow(
                RTC,
                date,
                700000,
                null,
                listener,
                null
            )
        }

        // notification channel is created in onCreate() for mainActivity()
        // send user home after submission!
        val homeIntent = Intent(this.baseContext, MainActivity::class.java)
        startActivity(homeIntent)
    }

    private fun sendNotificationNow(itemId: Int) {
        // check if item has been deleted:
        var dbOp = DBOperations()
        if (!dbOp.itemExists(this.baseContext, itemId)){
            return
        }
        // get DBItemEntry object
        val itemEntry : DBItemEntry = dbOp.getItemEntry(this.baseContext, itemId) ?: return

        val intent = Intent(this.baseContext, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this.baseContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val builder =
            NotificationCompat.Builder(this.baseContext, getString(R.string.expiration_channel_id))
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentTitle("Expiring: ${itemEntry.name}")
                .setContentText("${itemEntry.expiration}")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this.baseContext)) {
            if (ActivityCompat.checkSelfPermission(
                    baseContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                }

                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                var notId = itemId.toInt()
                this.notify(notId, builder.build())
            }
        }
    }
}