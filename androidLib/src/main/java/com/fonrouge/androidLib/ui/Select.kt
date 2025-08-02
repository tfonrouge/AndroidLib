package com.fonrouge.androidLib.ui

import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Suppress("unused")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Select(
    itemId: String?,
    options: List<Pair<String, String>>,
    onValueChange: ((Pair<String, String>?) -> Unit)? = null,
    itemListLabel: @Composable (Pair<String, String>) -> Unit = {
        Text(text = it.second)
    },
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: (@Composable () -> Unit)? = null,
) {
    var item by remember { mutableStateOf<Pair<String, String>?>(null) }
    var fieldValue by remember { mutableStateOf("") }
    onValueChange?.invoke(item)
    LaunchedEffect(key1 = itemId) {
        item = options.firstOrNull { it.first == itemId }
        fieldValue = item?.second ?: ""
    }
    var expanded by remember { mutableStateOf(false) }
    var clearClicked by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            if (clearClicked) {
                clearClicked = false
            } else {
                if (enabled)
                    expanded = !expanded
            }
        },
        modifier = modifier
    ) {
        TextField(
            value = fieldValue,
            onValueChange = { search ->
                fieldValue = search
            },
            modifier = Modifier
                .menuAnchor(),
            enabled = enabled,
            readOnly = true,
            label = label,
            singleLine = true,
        )
        ExposedDropdownMenu(
            expanded = expanded && !clearClicked,
            onDismissRequest = { expanded = false },
            modifier = Modifier.requiredSizeIn(maxHeight = LocalConfiguration.current.screenHeightDp.dp / 5),
            content = {
                options.forEach { item1 ->
                    DropdownMenuItem(
                        leadingIcon = {
                            if (item?.first == item1.first) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                )
                            }
                        },
                        text = { itemListLabel(item1) },
                        onClick = {
                            item = if (item?.first == item1.first) {
                                fieldValue = ""
                                null
                            } else {
                                fieldValue = item1.second
                                item1
                            }
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        )
    }
}
