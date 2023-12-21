package com.example.fridgeinventory

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.fridgeinventory.databinding.FragmentDashboardBinding
import com.example.fridgeinventory.ui.add.AddViewModel
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    /*override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
    } */

    // This property is only valid between onCreateView and
    // onDestroyView.
    // private val binding get() = _binding!!
    var barcodeFound = false;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        var cancelButton = findViewById<Button>(R.id.cancel_button)
        cancelButton.setOnClickListener {
            val intent = Intent()
            setResult(Activity.RESULT_CANCELED, intent)
            finish()
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            Toast.makeText(
                this,
                "Permissions not granted by the user.",
                Toast.LENGTH_SHORT
            ).show()
            ActivityCompat.requestPermissions(
               this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // configure our MLKit BarcodeScanning client/* passing in our desired barcode formats - MLKit supports additional formats outside of the ones listed here, and you may not need to offer support for all of these. You should only specify the ones you need */
        val options = BarcodeScannerOptions.Builder().setBarcodeFormats(
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_PDF417
        ).build()// getClient() creates a new instance of the MLKit barcode scanner with the specified options
        val scanner = BarcodeScanning.getClient(options)

        // setting up the analysis use case
        // tried using backpressurestrategy to only analyze one image at a time
        // but it didn't work
        val analysisUseCase = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()


// define the actual functionality of our analysis use case
        analysisUseCase.setAnalyzer(
            // newSingleThreadExecutor() will let us perform analysis on a single worker thread
            Executors.newSingleThreadExecutor(),
            { imageProxy ->
                processImageProxy(scanner, imageProxy)
            }
        )

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val viewFinder = findViewById<PreviewView>(R.id.viewFinder)

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, analysisUseCase
                )

            } catch (exc: Exception) {
                Log.e("TAG", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImageProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy
    ) {

        imageProxy.image?.let { image ->
            val inputImage =
                InputImage.fromMediaImage(
                    image,
                    imageProxy.imageInfo.rotationDegrees
                )

            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodeList ->
                    val barcode = barcodeList.getOrNull(0)        // `rawValue` is the decoded value of the barcode
                    barcode?.rawValue?.let { value ->
                        if (!barcodeFound) {
                            barcodeFound = true
                            Log.d("found a barcode!", value)
                            // update our textView to show the decoded value
                            findViewById<TextView>(R.id.text_add).text = value
                            val intent = Intent()
                            // val formIntent = Intent(this.baseContext, FormActivity::class.java)
                            intent.putExtra("barcode", value)
                            setResult(Activity.RESULT_OK, intent)
                            finish()

                        }
                    }

                }
                .addOnFailureListener {
                    // This failure will happen if the barcode scanning model
                    // fails to download from Google Play Services
                    Log.e("fail", it.message.orEmpty())
                }.addOnCompleteListener {
                    // When the image is from CameraX analysis use case, must
                    // call image.close() on received images when finished
                    // using them. Otherwise, new images may not be received
                    // or the camera may stall.
                    imageProxy.image?.close()
                    imageProxy.close()
                }
        }
    }


    /*fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val addViewModel =
            ViewModelProvider(this).get(AddViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textAdd
        addViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }*/
}