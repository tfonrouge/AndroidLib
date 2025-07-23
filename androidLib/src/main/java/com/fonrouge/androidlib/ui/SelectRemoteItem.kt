package com.fonrouge.androidlib.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.fonrouge.fsLib.model.base.BaseDoc
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Suppress("unused")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : BaseDoc<ID>, ID : Any> SelectRemoteItem(
    itemIdSet: Set<ID>? = null,
    multiSelect: Boolean,
    selectorFun: suspend (idSet: Set<ID>?, search: String) -> List<T>,
    onValueChange: ((Set<ID>) -> Unit)? = null,
    chipLabel: @Composable (T) -> Unit,
    chipAvatar: (@Composable (T) -> Unit)? = null,
    chipOnClick: () -> Unit = {},
    itemListLabel: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: (@Composable () -> Unit)? = null,
) {
    var itemSet: Set<T> by remember { mutableStateOf(emptySet()) }
    LaunchedEffect(key1 = itemIdSet) {
        selectorFun(itemIdSet, "").also { itemList ->
            itemSet = if (multiSelect) itemList.toSet() else {
                itemList.firstOrNull()?.let { setOf(it) } ?: emptySet()
            }
        }
    }
    var expanded by remember { mutableStateOf(false) }
    var clearClicked by remember { mutableStateOf(false) }
    var fieldValue by remember { mutableStateOf("") }
    val mutableItemList: MutableState<List<T>> = remember { mutableStateOf(emptyList()) }
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
        val coroutineScope = rememberCoroutineScope()
        TextField(
            value = fieldValue,
            onValueChange = { search ->
                fieldValue = search
                if (fieldValue.isBlank()) {
                    mutableItemList.value = emptyList()
                } else {
                    coroutineScope.launch {
                        delay(1000)
                        mutableItemList.value = selectorFun(itemSet.map { it._id }.toSet(), search)
                    }
                }
            },
            modifier = Modifier
                .menuAnchor(),
            enabled = enabled,
            label = label,
            leadingIcon = {
                if (itemSet.isNotEmpty()) {
                    Column {
                        itemSet.forEach { item ->
                            InputChip(
                                selected = true,
                                onClick = chipOnClick,
                                label = { chipLabel(item) },
                                enabled = enabled,
                                avatar = { chipAvatar?.invoke(item) },
                                trailingIcon = {
                                    if (enabled) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            modifier = Modifier
                                                .size(InputChipDefaults.AvatarSize)
                                                .clickable {
                                                    itemSet = itemSet
                                                        .filter { it._id != item._id }
                                                        .toSet()
                                                    onValueChange?.invoke(
                                                        itemSet
                                                            .map { it._id }
                                                            .toSet()
                                                    )
                                                }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            },
        )
        ExposedDropdownMenu(
            expanded = expanded && !clearClicked,
            onDismissRequest = { expanded = false },
            modifier = Modifier.requiredSizeIn(maxHeight = LocalConfiguration.current.screenHeightDp.dp / 5),
            content = {
                mutableItemList.value.forEach { item ->
                    DropdownMenuItem(
                        leadingIcon = {
                            if (itemSet.any { it._id == item._id }) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                )
                            }
                        },
                        text = { itemListLabel(item) },
                        onClick = {
                            itemSet = if (itemSet.any { it._id == item._id }) {
                                if (multiSelect)
                                    itemSet.filter { it._id != item._id }.toSet()
                                else
                                    emptySet()
                            } else {
                                if (multiSelect)
                                    itemSet + item
                                else
                                    setOf(item)
                            }
                            onValueChange?.invoke(itemSet.map { it._id }.toSet())
                            fieldValue = ""
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        )
    }
}
