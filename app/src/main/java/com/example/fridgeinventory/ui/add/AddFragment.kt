package com.example.fridgeinventory.ui.add

import android.Manifest.permission.*
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Insets.add
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fridgeinventory.CameraActivity
import com.example.fridgeinventory.FormActivity
import com.example.fridgeinventory.R
import com.example.fridgeinventory.databinding.FragmentDashboardBinding
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class AddFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private lateinit var baseContext: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        baseContext = context

        val intentLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {

                    var formIntent = Intent(this.baseContext, FormActivity::class.java)
                    formIntent.putExtra("barcode", result.data?.getStringExtra("barcode"))
                    startActivity(formIntent)
                } else if (result.resultCode == Activity.RESULT_CANCELED) {
                    var formIntent = Intent(this.baseContext, FormActivity::class.java)
                    formIntent.putExtra("barcode", "")
                    startActivity(formIntent)
                }
            }
        val cameraIntent = Intent(this.baseContext, CameraActivity::class.java)
        intentLauncher.launch(cameraIntent)
    }
}