package com.fonrouge.fslib.android.barcode

import android.media.ToneGenerator
import androidx.lifecycle.ViewModel
import com.fonrouge.fslib.android.barcode.BarcodeCamera
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

class VMCamera : ViewModel() {

    companion object {
        var defaultCameraType: CameraType = CameraType.GooglePlay
    }

    private val _uiState = MutableStateFlow(State())
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    private val _selectedCameraType = MutableStateFlow(defaultCameraType)
    val selectedCameraType: StateFlow<CameraType> = _selectedCameraType.asStateFlow()
    fun setSelectedCameraType(value: CameraType) { _selectedCameraType.value = value }

    var lastTime: Long = 0L

    private val _manualEntry = MutableStateFlow("")
    val manualEntry: StateFlow<String> = _manualEntry.asStateFlow()
    fun setManualEntry(value: String) { _manualEntry.value = value }

    val gmsBarcodeScannerOptions by lazy {
        GmsBarcodeScannerOptions.Builder()
            .allowManualInput()
            .build()
    }

    private val _barcodeCamera = MutableStateFlow(BarcodeCamera())
    val barcodeCamera: StateFlow<BarcodeCamera> = _barcodeCamera.asStateFlow()

    private val _torchState = MutableStateFlow(false)
    val torchState: StateFlow<Boolean> = _torchState.asStateFlow()
    fun setTorchState(value: Boolean) { _torchState.value = value }

    private var toneGenerator: ToneGenerator? = null

    data class State(
        val scannerOpen: Boolean = false,
        val codeScanned: String? = null,
    )

    fun onEvent(uiEvent: UIEvent) {
        when (uiEvent) {
            UIEvent.Open -> _uiState.value = _uiState.value.copy(scannerOpen = true)
            UIEvent.Close -> {
                _barcodeCamera.value.toggleFlash(false)
                _torchState.value = false
                _uiState.value = _uiState.value.copy(scannerOpen = false)
            }

            is UIEvent.CodeRead -> {
                _uiState.value = _uiState.value.copy(codeScanned = uiEvent.codeScanned)
                lastTime = System.currentTimeMillis()
                if (toneGenerator == null) {
                    toneGenerator = ToneGenerator(0, ToneGenerator.MAX_VOLUME)
                }
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT)
            }

            UIEvent.ManualEntry -> {
                onEvent(UIEvent.Close)
                onEvent(UIEvent.CodeRead(_manualEntry.value))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        toneGenerator?.release()
        toneGenerator = null
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
