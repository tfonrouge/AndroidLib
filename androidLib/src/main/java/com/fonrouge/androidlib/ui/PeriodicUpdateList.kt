package com.fonrouge.androidlib.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.fonrouge.androidlib.viewModel.VMList
import kotlinx.coroutines.delay

@Suppress("unused")
@Composable
fun PeriodicUpdateList(
    vmList: VMList<*, *, *, *>,
    periodicUpdate: Boolean? = null,
    periodicInterval: Int? = null,
) {
    periodicUpdate?.let { vmList.periodicUpdate = it }
    periodicInterval?.let { vmList.periodicInterval = it }
    LaunchedEffect(key1 = vmList.refreshListCounter) {
        delay(vmList.periodicInterval.toLong())
        if (vmList.periodicUpdate) {
            ++vmList.refreshListCounter
        }
//        Log.d("LaunchedEffect", "${viewModel.refreshListCounter}")
        vmList.requestRefresh = true
    }
}