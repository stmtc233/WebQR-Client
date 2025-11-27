package com.example.webqrclient

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import com.google.android.material.color.DynamicColors
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.webqrclient.data.QrData
import com.example.webqrclient.databinding.ActivityMainBinding
import com.example.webqrclient.network.ApiClient
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService

    private var lastScannedQrData: String? = null
    private var cameraControl: CameraControl? = null // 新增 CameraControl 变量

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivitiesIfAvailable(application)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ApiClient.initialize(this)
        updateApiUrlStatus()

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        binding.switchApiButton.setOnClickListener {
            showApiSelectionDialog()
        }
    }

    private fun updateApiUrlStatus() {
        binding.apiUrlTextView.text = "Current API: ${ApiClient.getCurrentBaseUrl()}"
    }

    private fun showApiSelectionDialog() {
        val availableUrls = ApiClient.availableUrls
        val urls = availableUrls.map { it.second }.toTypedArray()
        val displayLabels = availableUrls.map { "${it.first}: ${it.second}" }.toTypedArray()

        val currentUrl = ApiClient.getCurrentBaseUrl()
        val defaultSelection = availableUrls.indexOfFirst { "${it.first}: ${it.second}" == currentUrl }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select API Endpoint")
            .setSingleChoiceItems(displayLabels, defaultSelection) { dialog, which ->
                val selectedUrl = urls[which]
                ApiClient.setBaseUrl(selectedUrl)
                updateApiUrlStatus()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QrCodeAnalyzer { qrCodeValue ->
                        if (qrCodeValue.isNotEmpty() && qrCodeValue != lastScannedQrData) {
                            lastScannedQrData = qrCodeValue
                            runOnUiThread {
                                binding.statusTextView.text = "New QR Detected: $qrCodeValue\n⏰Uploading..."
                            }
                            uploadToServer(qrCodeValue)
                        }
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                // !!! 这里是已修改的部分 !!!
                // bindToLifecycle 返回一个 Camera 对象
                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

                // 从 Camera 对象获取 CameraControl 和 CameraInfo
                cameraControl = camera.cameraControl
                val cameraInfo = camera.cameraInfo

                // 设置 Slider 的监听器来控制缩放
                setupZoomSlider(cameraInfo)

                binding.statusTextView.text = "Scanning for QR Code..."
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                binding.statusTextView.text = "Error starting camera."
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // !!! 新增方法：设置缩放 Slider !!!
    private fun setupZoomSlider(cameraInfo: CameraInfo) {
        cameraInfo.zoomState.observe(this) { zoomState ->
            // 添加对 zoomState 和缩放比例的空值和有效性检查
            if (zoomState != null) {
                val minZoom = zoomState.minZoomRatio
                val maxZoom = zoomState.maxZoomRatio
                
                // 确保 minZoom < maxZoom，以避免 Slider 出现问题
                if (minZoom < maxZoom) {
                    binding.zoomSlider.valueFrom = minZoom
                    binding.zoomSlider.valueTo = maxZoom
                    // 设置初始值，确保 Slider 的 UI 与相机状态同步
                    binding.zoomSlider.value = zoomState.zoomRatio
                } else {
                    // 如果不支持缩放，可以禁用 Slider
                    binding.zoomSlider.isEnabled = false
                }
            }
        }

        binding.zoomSlider.addOnChangeListener { _, value, _ ->
            cameraControl?.setZoomRatio(value)
        }
    }

    private fun uploadToServer(qrCodeValue: String) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.uploadQrCode(QrData(qrCodeValue))
                if (response.isSuccessful) {
                    Log.d(TAG, "Upload successful for value: $qrCodeValue")
                    runOnUiThread {
                        binding.statusTextView.text = "✅Upload Success: $qrCodeValue"
                    }
                } else {
                    Log.e(TAG, "Upload failed with code: ${response.code()}")
                    runOnUiThread {
                        binding.statusTextView.text = "❌Upload Failed: $qrCodeValue"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during upload", e)
                runOnUiThread {
                    binding.statusTextView.text = "❌Upload Error: ${e.message}"
                }
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "WebQrClient"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
