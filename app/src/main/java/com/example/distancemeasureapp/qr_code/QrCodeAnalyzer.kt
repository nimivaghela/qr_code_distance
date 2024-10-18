package com.example.distancemeasureapp.qr_code

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class QrCodeAnalyzer(
    private val context: Context,
    private val barcodeBoxView: BarcodeBoxView,
    private val previewViewWidth: Float,
    private val previewViewHeight: Float,
    private var camerasInfoList: ArrayList<CameraInfo>,
    private val txtDistance: ListView
) : ImageAnalysis.Analyzer {
    private var itemList: ArrayList<String> = arrayListOf()

    /**
     * This parameters will handle preview box scaling
     */
    private var scaleX = 1f
    private var scaleY = 1f

    private fun translateX(x: Float) = x * scaleX
    private fun translateY(y: Float) = y * scaleY

    private fun adjustBoundingRect(rect: Rect) = RectF(
        translateX(rect.left.toFloat()),
        translateY(rect.top.toFloat()),
        translateX(rect.right.toFloat()),
        translateY(rect.bottom.toFloat())
    )

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        val img = image.image
        if (img != null) {
            // Update scale factors
            scaleX = previewViewWidth / img.height.toFloat()
            scaleY = previewViewHeight / img.width.toFloat()

            val inputImage = InputImage.fromMediaImage(img, image.imageInfo.rotationDegrees)

            // Process image searching for barcodes
            val options = BarcodeScannerOptions.Builder()
                .build()

            val scanner = BarcodeScanning.getClient(options)

            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        for (barcode in barcodes) {
                            // Handle received barcodes...
                            Toast.makeText(
                                context,
                                "Value: " + barcode.rawValue,
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            val bounds = barcode.boundingBox

                            // Update bounding rect
                            barcode.boundingBox?.let { rect ->
                                barcodeBoxView.setRect(
                                    adjustBoundingRect(
                                        rect
                                    )
                                )
                            }
                            if (bounds != null) {
                                vibratePhone(context, 500)
                                calculateDistance(bounds.width(), barcode.displayValue.toString())
                            }
                        }
                    } else {
                        // Remove bounding rect
                        barcodeBoxView.setRect(RectF())
                    }
                }
                .addOnFailureListener {
                    Log.e("My TAG ==>", it.message.toString())
                    if (itemList.isEmpty()) {
                        itemList.clear()
                        itemList.add("No QR Code Found")
                        setDataToListView()
                    }
                }
        }

        image.close()
    }

    @SuppressLint("DefaultLocale")
    private fun calculateDistance(width: Int, barcode: String) {
        itemList.clear()

        camerasInfoList.forEachIndexed { index, cameraInfo ->
            val focalLength = cameraInfo.focalLength
            val realQRSize = 50.0
            val distance = String.format("%.2f", ((realQRSize * focalLength) / width) * 10)
            Log.d(
                "My TAG ==>",
                "Distance of QR is $distance CM according to camera lens facing ${cameraInfo.lensFacing} $width"
            )

            itemList.add(
                "The QR code is approximately $distance CM away from the camera ${index + 1}."
            )

//            "The QR code is approximately $distance cm away from the camera lens."
        }

        setDataToListView()

        Log.d("My TAG ==>", " --------------- Trial end ---------------")

    }

    private fun setDataToListView() {
        val arrayAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_list_item_1, itemList
        )
        txtDistance.adapter = arrayAdapter
    }

    private fun vibratePhone(context: Context, duration: Long) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        // VibrationEffect for API 26 and above
        val vibrationEffect =
            VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(vibrationEffect)
    }
}