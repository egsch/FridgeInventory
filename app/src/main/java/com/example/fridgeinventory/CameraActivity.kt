package com.example.fridgeinventory

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.view.LifecycleCameraController
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.fridgeinventory.databinding.ActivityCameraBinding
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

class CameraActivity : AppCompatActivity() {
    private var barcodeFound = false;
    private lateinit var binding: ActivityCameraBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val cancelButton = binding.cancelButton
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
                getString(R.string.permissionsNotGranted),
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
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
        )
    }

    private fun startCamera() {
        val cameraController = LifecycleCameraController(this)

        // all barcode formats!
        val options = BarcodeScannerOptions.Builder().setBarcodeFormats(
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_CODABAR,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_ITF,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_PDF417,
            Barcode.FORMAT_AZTEC,
            Barcode.FORMAT_DATA_MATRIX
        ).build()

        // instance of barcode scanning from MLKit
        val scanner = BarcodeScanning.getClient(options)
        val preview = binding.viewFinder

        cameraController.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(this),
            MlKitAnalyzer(
                listOf(scanner),
                COORDINATE_SYSTEM_VIEW_REFERENCED,
                ContextCompat.getMainExecutor(this),
            ) { result: MlKitAnalyzer.Result? ->
                val resultBarcode = result?.getValue(scanner)
                if (resultBarcode == null || resultBarcode.size <= 0 || resultBarcode[0].rawValue == null) {
                    return@MlKitAnalyzer
                } else {
                    val stringBarcodeResult = resultBarcode[0].rawValue.toString()
                    Log.d("barcode result:", stringBarcodeResult)
                    barcodeFound = true
                    val intent = Intent()

                    intent.putExtra("barcode", stringBarcodeResult)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
            }
        )

        cameraController.bindToLifecycle(this)
        preview.controller = cameraController
    }
}