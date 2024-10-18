package com.example.distancemeasureapp.qr_code

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.distancemeasureapp.R
import com.example.distancemeasureapp.databinding.ActivityQrcodeBinding
import com.example.distancemeasureapp.permissions.PermissionCallback
import com.example.distancemeasureapp.permissions.PermissionHelper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QRCodeActivity : AppCompatActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private val binding: ActivityQrcodeBinding by lazy {
        return@lazy ActivityQrcodeBinding.inflate(layoutInflater)
    }
    private lateinit var barcodeBoxView: BarcodeBoxView
    private var cameraManager: CameraManager? = null
    private var camerasInfoList: ArrayList<CameraInfo> = arrayListOf()
    private var permissionHelper: PermissionHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        barcodeBoxView = BarcodeBoxView(this)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        askCameraPermission()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun askCameraPermission() {
        permissionHelper = PermissionHelper(
            this, arrayOf(
                Manifest.permission.CAMERA
            ), 1001
        )

        permissionHelper?.requestPermissions(object : PermissionCallback {
            //this execute when all permissions are granted
            override fun onPermissionGranted() {
                startCamera()
                Toast.makeText(this@QRCodeActivity, "Permission granted", Toast.LENGTH_LONG)
                    .show()
            }

            //this will execute when all permissions are denied
            override fun onPermissionDenied() {
                Toast.makeText(this@QRCodeActivity, "Permission denied", Toast.LENGTH_LONG).show()
                permissionHelper?.openSettings()
            }

            //this will execute when permission denied by system and this will move user to setting screen
            override fun onPermissionDeniedBySystem() {
                Toast.makeText(
                    this@QRCodeActivity,
                    "Permission denied by system",
                    Toast.LENGTH_LONG
                ).show()

                permissionHelper?.openSettings()
            }

            // this will execute when some of permission is granted, in that case you can move user to
            // settings screen by calling openSetting with proper message for permissions which are not granted
            override fun onIndividualPermissionGranted(grantedPermission: Array<String>) {
                Toast.makeText(
                    this@QRCodeActivity,
                    "Permission individual granted",
                    Toast.LENGTH_LONG
                ).show()
            }

            //this will execute when permission are not added in Manifest but asking for runtime
            override fun onPermissionNotFoundInManifest() {
                Toast.makeText(this@QRCodeActivity, "Permission not found", Toast.LENGTH_LONG)
                    .show()
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //you need to add default result into helper class to manage results and get result in helper callback
        permissionHelper?.onRequestPermissionResult(requestCode, permissions, grantResults)
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            try {
                getCameraInformation()
            } catch (_: Exception) {

            }
            // Image analyzer
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also {
                    it.setAnalyzer(
                        cameraExecutor, QrCodeAnalyzer(
                            this,
                            barcodeBoxView,
                            binding.previewView.width.toFloat(),
                            binding.previewView.height.toFloat(),
                            camerasInfoList,
                            binding.txtDistance
                        )
                    )
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun getCameraInformation() {
        val cameraIds = cameraManager?.cameraIdList
        cameraIds?.forEach { item ->
            val cameraInfo = cameraManager?.getCameraCharacteristics(item)
            val focalLengths =
                cameraInfo?.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)

            if (focalLengths != null && focalLengths.isNotEmpty()) {
                val lensFacing = cameraInfo.get(CameraCharacteristics.LENS_FACING)
                val focalLength = focalLengths[0]
                camerasInfoList.add(CameraInfo(focalLength = focalLength, lensFacing = lensFacing))
            }
        }


    }


}

