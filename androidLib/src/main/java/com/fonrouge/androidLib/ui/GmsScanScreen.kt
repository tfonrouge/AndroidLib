package com.fonrouge.androidLib.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fonrouge.androidLib.viewModel.VMCamera
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

@Composable
fun GmsScanScreen(
    vmCamera: VMCamera = viewModel(),
    context: Context = LocalContext.current,
    onFailure: (Exception) -> Unit = {},
    onCanceled: () -> Unit = {},
    onReadBarcode: (CodeEntry) -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    if (vmCamera.uiState.collectAsState().value.scannerOpen) {
        val scanner = GmsBarcodeScanning
            .getClient(context, vmCamera.gmsBarcodeScannerOptions)
        scanner.startScan()
            .addOnSuccessListener { barcode: Barcode ->
                onReadBarcode(
                    CodeEntry(
                        source = CodeEntry.Type.Camera,
                        barcode = barcode,
                        code = barcode.rawValue
                    )
                )
                vmCamera.onEvent(VMCamera.UIEvent.CodeRead(barcode.rawValue))
            }
            .addOnCanceledListener {
                onCanceled()
            }
            .addOnFailureListener { exception: Exception ->
                onFailure(exception)
            }
            .addOnCompleteListener {
                vmCamera.onEvent(VMCamera.UIEvent.Close)
            }
    } else {
        content()
    }
}