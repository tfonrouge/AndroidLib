package com.fonrouge.fslib.android.barcode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fonrouge.fslib.android.barcode.VMCamera
import com.google.mlkit.vision.barcode.common.Barcode

@Composable
fun ScanBarcodeScreen(
    vmCamera: VMCamera,
    onReadBarcode: (CodeEntry) -> Unit = {},
    onFilter: ((Barcode) -> Boolean)? = null,
    content: @Composable () -> Unit,
) {
    val cameraType by vmCamera.selectedCameraType.collectAsStateWithLifecycle()
    when (cameraType) {
        VMCamera.CameraType.GooglePlay -> {
            GmsScanScreen(
                vmCamera = vmCamera,
                onReadBarcode = onReadBarcode,
                content = content
            )
        }

        VMCamera.CameraType.CameraX -> {
            CameraXCoreReaderScreen1(
                vmCamera = vmCamera,
                onReadBarcode = onReadBarcode,
                onFilter = onFilter,
                content = content
            )
        }
    }
}

data class CodeEntry(
    val source: Type,
    val barcode: Barcode? = null,
    val code: String? = null,
) {
    enum class Type {
        Camera,
        Keyboard,
    }
}