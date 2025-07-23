package com.fonrouge.androidlib.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fonrouge.androidlib.viewModel.VMBase

@Suppress("unused")
@Composable
fun ScreenStateAlert(vmBase: VMBase) {
    vmBase.stateAlert.collectAsStateWithLifecycle().value?.let { itemAlert ->
        AlertDialog(
            onDismissRequest = itemAlert.onDismissRequest,
            confirmButton = {
                TextButton(
                    onClick = {
                        itemAlert.type.onAccept?.invoke()
                        vmBase.clearStateAlert()
                    }
                ) {
                    Text(
                        text = if (itemAlert.type.canRetry) "Retry" else "Accept"
                    )
                }
            },
            dismissButton = if (itemAlert.type.canRetry) {
                {
                    TextButton(
                        onClick = {
                            itemAlert.type.onCancel?.invoke()
                            vmBase.clearStateAlert()
                        }
                    ) {
                        Text(text = "Cancel")
                    }
                }
            } else {
                null
            },
            icon = if (itemAlert.simpleState.hasError.not()) {
                {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "info")
                }
            } else {
                {
                    Icon(imageVector = Icons.Default.Error, contentDescription = "error")
                }
            },
            title = {
                Text(text = "${itemAlert.simpleState.state}")
            },
            text = {
                Text(text = if (itemAlert.simpleState.hasError.not()) "${itemAlert.simpleState.msgOk}" else "${itemAlert.simpleState.msgError}")
            },
            iconContentColor = if (itemAlert.simpleState.hasError.not()) Color.Green else Color.Red
        )
    }
}
