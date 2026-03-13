package com.fonrouge.androidLib.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fonrouge.androidLib.viewModel.VMList
import kotlinx.coroutines.delay

@Composable
fun PeriodicUpdateList(
    vmList: VMList<*, *, *, *>,
    periodicUpdate: Boolean? = null,
    periodicInterval: Int? = null,
) {
    periodicUpdate?.let { vmList.periodicUpdate = it }
    periodicInterval?.let { vmList.periodicInterval = it }
    val refreshListCounter by vmList.refreshListCounterFlow.collectAsStateWithLifecycle()
    LaunchedEffect(key1 = refreshListCounter) {
        delay(vmList.periodicInterval.toLong())
        if (vmList.periodicUpdate) {
            ++vmList.refreshListCounter
            vmList.requestRefresh = true
        }
    }
}
