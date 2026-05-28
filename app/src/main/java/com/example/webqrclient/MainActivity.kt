package com.example.webqrclient

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.webqrclient.data.QrData
import com.example.webqrclient.databinding.ActivityMainBinding
import com.example.webqrclient.network.ApiClient
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService

    private var lastScannedQrData: String? = null
    private var cameraControl: CameraControl? = null
    private var qrCodeAnalyzer: QrCodeAnalyzer? = null

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivitiesIfAvailable(application)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        updateApiUrlStatus()

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (isCameraPermissionGranted()) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.switchApiButton.setOnClickListener { showApiSelectionDialog() }
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val basePadding = dp(16)

            binding.statusTextView.updatePadding(
                left = bars.left + basePadding,
                right = bars.right + basePadding,
                bottom = bars.bottom + basePadding
            )
            binding.apiUrlTextView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = bars.top + basePadding
                marginStart = bars.left + basePadding
            }
            binding.zoomSlider.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = bars.bottom + dp(50)
            }
            insets
        }
    }

    private fun updateApiUrlStatus() {
        binding.apiUrlTextView.text =
            getString(R.string.api_current_label, ApiClient.getCurrentBaseUrlLabel())
    }

    private fun showApiSelectionDialog() {
        val urls = ApiClient.availableUrls.map { it.second }
        val displayLabels = ApiClient.availableUrls
            .map { "${it.first}: ${it.second}" }
            .toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(R.string.api_select_dialog_title)
            .setSingleChoiceItems(displayLabels, ApiClient.getSelectedIndex()) { dialog, which ->
                ApiClient.setBaseUrl(urls[which])
                updateApiUrlStatus()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val analyzer = QrCodeAnalyzer { qrCodeValue ->
                if (qrCodeValue.isNotEmpty() && qrCodeValue != lastScannedQrData) {
                    lastScannedQrData = qrCodeValue
                    runOnUiThread {
                        binding.statusTextView.text =
                            getString(R.string.status_new_qr_uploading, qrCodeValue)
                    }
                    uploadToServer(qrCodeValue)
                }
            }
            qrCodeAnalyzer = analyzer

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(cameraExecutor, analyzer) }

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer
                )
                cameraControl = camera.cameraControl
                setupZoomSlider(camera.cameraInfo)
                binding.statusTextView.setText(R.string.status_scanning)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                binding.statusTextView.setText(R.string.status_camera_error)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun setupZoomSlider(cameraInfo: CameraInfo) {
        cameraInfo.zoomState.observe(this) { zoomState ->
            if (zoomState == null) return@observe
            val minZoom = zoomState.minZoomRatio
            val maxZoom = zoomState.maxZoomRatio
            if (minZoom < maxZoom) {
                binding.zoomSlider.valueFrom = minZoom
                binding.zoomSlider.valueTo = maxZoom
                binding.zoomSlider.value = zoomState.zoomRatio
            } else {
                binding.zoomSlider.isEnabled = false
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
                    binding.statusTextView.text =
                        getString(R.string.status_upload_success, qrCodeValue)
                } else {
                    Log.e(TAG, "Upload failed with code: ${response.code()}")
                    binding.statusTextView.text =
                        getString(R.string.status_upload_failed, qrCodeValue)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during upload", e)
                binding.statusTextView.text =
                    getString(R.string.status_upload_error, e.message ?: "")
            }
        }
    }

    private fun isCameraPermissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        qrCodeAnalyzer?.close()
        qrCodeAnalyzer = null
    }

    private companion object {
        const val TAG = "WebQrClient"
    }
}
