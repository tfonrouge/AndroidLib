package com.fonrouge.androidLib.viewModel

import android.media.ToneGenerator
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.fonrouge.androidLib.barcode.BarcodeCamera
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

class VMCamera : ViewModel() {

    companion object {
        var defaultCameraType: CameraType = CameraType.GooglePlay
    }

    private val _uiState = MutableStateFlow(State())
    val uiState = _uiState.asStateFlow()
    val selectedCameraType: MutableState<CameraType> = mutableStateOf(defaultCameraType)
    var lastTime: Long = 0L

    val manualEntry = mutableStateOf("")

    val gmsBarcodeScannerOptions by lazy {
        GmsBarcodeScannerOptions.Builder()
            .allowManualInput()
//            .enableAutoZoom()
            .build()
    }

    val barcodeCamera = mutableStateOf(BarcodeCamera())

    val torchState = mutableStateOf(false)

    data class State(
        val scannerOpen: Boolean = false,
        val codeScanned: String? = null,
    )

    fun onEvent(uiEvent: UIEvent) {
        when (uiEvent) {
            UIEvent.Open -> _uiState.value = _uiState.value.copy(scannerOpen = true)
            UIEvent.Close -> {
                barcodeCamera.value.toggleFlash(false)
                torchState.value = false
                _uiState.value = _uiState.value.copy(scannerOpen = false)
            }

            is UIEvent.CodeRead -> {
                _uiState.value = _uiState.value.copy(codeScanned = uiEvent.codeScanned)
                lastTime = System.currentTimeMillis()
                ToneGenerator(0, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_PROP_PROMPT)
            }

            UIEvent.ManualEntry -> {
                onEvent(UIEvent.Close)
                onEvent(UIEvent.CodeRead(manualEntry.value))
            }
        }
    }

    sealed class UIEvent {
        data object Open : UIEvent()
        data object Close : UIEvent()
        data class CodeRead(val codeScanned: String?) : UIEvent()
        data object ManualEntry : UIEvent()
    }

    @Serializable
    enum class CameraType {
        GooglePlay,
        CameraX,
    }
}
