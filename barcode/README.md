# :barcode

Optional barcode scanning module for [fslib-android](../README.md). Provides ready-to-use Compose screens for barcode scanning via CameraX + ML Kit or Google Play Services code scanner.

This module is **fully independent** -- it has no dependency on the `:android` core module and can be used standalone in any Compose app.

## Installation

```kotlin
dependencies {
    implementation("com.fonrouge.fslib:barcode:2.0.0")
}
```

## Quick Start

```kotlin
import com.fonrouge.fslib.android.barcode.VMCamera
import com.fonrouge.fslib.android.barcode.ScanBarcodeScreen
import com.fonrouge.fslib.android.barcode.CodeEntry

@Composable
fun InventoryScreen() {
    val vmCamera: VMCamera = viewModel()

    ScanBarcodeScreen(
        vmCamera = vmCamera,
        onReadBarcode = { codeEntry ->
            println("Scanned: ${codeEntry.code} via ${codeEntry.source}")
        },
    ) {
        // Content shown when scanner is closed
        Button(onClick = { vmCamera.onEvent(VMCamera.UIEvent.Open) }) {
            Text("Scan Barcode")
        }
    }
}
```

## Camera Types

The module supports two scanner implementations, selectable at runtime:

### Google Play Services (default)

Uses the built-in Google code scanner. No camera permission required -- Google handles the UI and permission flow.

```kotlin
VMCamera.defaultCameraType = VMCamera.CameraType.GooglePlay
```

### CameraX + ML Kit

Full camera preview with a viewfinder overlay, torch control, and manual text entry fallback. Requires `CAMERA` permission (handled automatically via accompanist-permissions).

```kotlin
VMCamera.defaultCameraType = VMCamera.CameraType.CameraX
```

You can also change the camera type per-instance:

```kotlin
vmCamera.setSelectedCameraType(VMCamera.CameraType.CameraX)
```

## Supported Barcode Formats

When using CameraX, the following formats are scanned:

- QR Code
- Aztec
- Code 128, Code 39, Code 93
- EAN-8, EAN-13
- UPC-A, UPC-E
- PDF417

The Google Play Services scanner supports all formats by default and allows manual input.

## API Reference

### VMCamera

ViewModel managing scanner state. Create via `viewModel()` in Compose.

| Property / Method | Type | Description |
|---|---|---|
| `uiState` | `StateFlow<State>` | `scannerOpen`, `codeScanned` |
| `selectedCameraType` | `StateFlow<CameraType>` | Current scanner implementation |
| `torchState` | `StateFlow<Boolean>` | Flashlight on/off |
| `manualEntry` | `StateFlow<String>` | Text entered manually |
| `barcodeCamera` | `StateFlow<BarcodeCamera>` | Internal CameraX wrapper |
| `onEvent(UIEvent)` | -- | Dispatch scanner events |
| `setTorchState(Boolean)` | -- | Toggle flashlight |
| `setManualEntry(String)` | -- | Set manual text |
| `setSelectedCameraType(CameraType)` | -- | Switch scanner type |

### UIEvent

| Event | Description |
|---|---|
| `UIEvent.Open` | Open the scanner |
| `UIEvent.Close` | Close the scanner, turn off torch |
| `UIEvent.CodeRead(String?)` | A barcode was scanned (plays beep tone) |
| `UIEvent.ManualEntry` | Submit the manual entry text, then close |

### CodeEntry

Data class returned by `onReadBarcode`:

```kotlin
data class CodeEntry(
    val source: Type,          // Camera or Keyboard
    val barcode: Barcode?,     // Raw ML Kit Barcode (null for manual entry)
    val code: String?,         // The scanned/entered string
)
```

### Composables

| Composable | Description |
|---|---|
| `ScanBarcodeScreen` | Router that picks GooglePlay or CameraX based on `vmCamera.selectedCameraType` |
| `GmsScanScreen` | Google Play Services scanner |
| `CameraXCoreReaderScreen1` | CameraX scanner with viewfinder overlay, torch, and manual entry |

## Module Structure

```
barcode/
  src/main/java/com/fonrouge/fslib/android/barcode/
    VMCamera.kt              -- ViewModel (state, events, camera type)
    BarcodeCamera.kt         -- CameraX + ML Kit image analysis
    ScanBarcodeScreen.kt     -- Router composable + CodeEntry data class
    GmsScanScreen.kt         -- Google Play Services scanner
    CameraXScanScreen1.kt   -- CameraX scanner UI (overlay, torch, manual entry)
  src/main/res/drawable-xxhdpi/
    close.png                -- Close button icon
    ic_torch.png             -- Torch off icon
    ic_torch_on.png          -- Torch on icon
  src/test/java/com/fonrouge/fslib/android/barcode/
    VMCameraTest.kt          -- Unit tests for VMCamera
```

## Dependencies

| Dependency | Purpose |
|---|---|
| CameraX 1.5.3 | Camera preview and image analysis |
| ML Kit Barcode 17.3.0 | On-device barcode detection |
| GMS Code Scanner 16.1.0 | Google Play Services barcode scanner |
| Accompanist Permissions 0.37.3 | Runtime permission handling |
| Guava 33.5.0 | Resolves CameraX transitive conflict |

## Building

```bash
./gradlew :barcode:test      # Unit tests
./gradlew :barcode:assemble  # Build AAR
```
