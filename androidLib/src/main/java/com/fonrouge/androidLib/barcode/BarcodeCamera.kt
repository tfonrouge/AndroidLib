package com.fonrouge.androidLib.barcode

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fonrouge.androidLib.ui.CodeEntry
import com.fonrouge.androidLib.viewModel.VMCamera
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

class BarcodeCamera {

    private var camera: Camera? = null

    val torchState: Boolean get() = camera?.cameraInfo?.torchState?.value?.let { it != 0 } ?: false

    @Composable
    fun CameraPreview(
        vmCamera: VMCamera,
        onReadBarcode: (CodeEntry) -> Unit,
        onFilter: ((Barcode) -> Boolean)? = null,
    ) {
        val lifecycleOwner = LocalLifecycleOwner.current
        val imageCapture = remember {
            ImageCapture
                .Builder()
                .build()
        }

        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    layoutParams =
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    scaleType = PreviewView.ScaleType.FILL_START

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener(
                        {
                            startCamera(
                                context = context,
                                previewView = this,
                                imageCapture = imageCapture,
                                lifecycleOwner = lifecycleOwner,
                                onReadBarcode = onReadBarcode,
                                onFilter = onFilter,
                                vmCamera = vmCamera,
                            )
                        },
                        ContextCompat.getMainExecutor(context)
                    )
                }
            }
        )
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startCamera(
        context: Context,
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        imageCapture: ImageCapture,
        onReadBarcode: (CodeEntry) -> Unit,
        onFilter: ((Barcode) -> Boolean)? = null,
        vmCamera: VMCamera,
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

            val executor = Executors.newSingleThreadExecutor()

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_AZTEC,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_CODE_93,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_PDF417
                )
                .enableAllPotentialBarcodes()
                .build()

            val scanner = BarcodeScanning.getClient(options)

            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                processImageProxy(
                    barcodeScanner = scanner,
                    imageProxy = imageProxy,
                    onReadBarcode = onReadBarcode,
                    onFilter = onFilter,
                    vmCamera = vmCamera,
                )
            }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, imageCapture, imageAnalysis
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

            camera?.cameraControl?.enableTorch(vmCamera.torchState.value)

        }, ContextCompat.getMainExecutor(context))
    }

    fun toggleFlash(
        isOn: Boolean,
    ) {
        camera?.cameraControl?.enableTorch(isOn)
    }

    @ExperimentalGetImage
    private fun processImageProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy,
        onReadBarcode: (CodeEntry) -> Unit,
        onFilter: ((Barcode) -> Boolean)? = null,
        vmCamera: VMCamera,
    ) {
        imageProxy.image?.let { image ->
            val inputImage =
                InputImage.fromMediaImage(
                    image,
                    imageProxy.imageInfo.rotationDegrees
                )
            barcodeScanner.process(inputImage)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        task.result?.getOrNull(0)?.let { barcode ->
                            if (vmCamera.uiState.value.scannerOpen) {
                                if (onFilter == null || onFilter(barcode)) {
                                    if (!barcode.displayValue.isNullOrEmpty() && (System.currentTimeMillis() - vmCamera.lastTime) > 500L) {
                                        toggleFlash(false)
                                        onReadBarcode(
                                            CodeEntry(
                                                source = CodeEntry.Type.Camera,
                                                barcode = barcode,
                                                code = barcode.rawValue
                                            )
                                        )
                                        vmCamera.onEvent(VMCamera.UIEvent.CodeRead(barcode.rawValue))
                                        vmCamera.onEvent(VMCamera.UIEvent.Close)
                                    }
                                }
                            }
                        }
                    } else {
                        Log.e("SCANNER", "image scan failed: ${task.exception?.message}")
                    }
                    imageProxy.close()
                }
        }
    }

    @Suppress("unused")
    fun takePhoto(
        context: Context,
        imageCapture: ImageCapture,
        onSuccess: (Uri) -> Unit,
    ) {
        val name = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraX", "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded"
                    Log.d("CameraX", msg)

                    output.savedUri?.let { onSuccess(it) }
                }
            }
        )
    }
}
