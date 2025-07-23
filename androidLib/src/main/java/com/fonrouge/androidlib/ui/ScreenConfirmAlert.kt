package com.fonrouge.androidlib.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fonrouge.androidlib.viewModel.ConfirmAlert
import com.fonrouge.androidlib.viewModel.VMBase

@Suppress("unused")
@Composable
fun ScreenConfirmAlert(viewBase: VMBase) {
    viewBase.confirmAlert.collectAsStateWithLifecycle().value?.let { confirmAlert: ConfirmAlert ->
        AlertDialog(
            onDismissRequest = confirmAlert.onDismissRequest,
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmAlert.type.onConfirm.invoke()
                        viewBase.clearConfirmAlert()
                    }
                ) {
                    Text(
                        text = "Confirm"
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        when (confirmAlert.type) {
                            is ConfirmAlert.Type.YesCancelConfirm -> confirmAlert.type.onCancel?.invoke()
                            is ConfirmAlert.Type.YesNoConfirm -> confirmAlert.type.onNo?.invoke()
                        }
                        viewBase.clearConfirmAlert()
                    }
                ) {
                    Text(
                        text = when (confirmAlert.type) {
                            is ConfirmAlert.Type.YesCancelConfirm -> "Cancel"
                            is ConfirmAlert.Type.YesNoConfirm -> "No"
                        }
                    )
                }
            },
            icon = {
                Icon(imageVector = Icons.Default.QuestionMark, contentDescription = "q")
            },
            title = {
                Text(text = "Please confirm")
            },
            text = {
                Text(text = confirmAlert.confirmText)
            },
        )
    }
}
