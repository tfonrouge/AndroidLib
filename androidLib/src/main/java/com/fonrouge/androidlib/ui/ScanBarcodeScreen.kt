package com.fonrouge.androidlib.ui

import androidx.compose.runtime.Composable
import com.fonrouge.androidlib.viewModel.VMCamera
import com.google.mlkit.vision.barcode.common.Barcode

@Composable
fun ScanBarcodeScreen(
    vmCamera: VMCamera,
    onReadBarcode: (CodeEntry) -> Unit = {},
    onFilter: ((Barcode) -> Boolean)? = null,
    content: @Composable () -> Unit,
) {
    when (vmCamera.selectedCameraType.value) {
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