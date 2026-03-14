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
    val enabled = vmList.periodicUpdate
    val interval = vmList.periodicInterval

    LaunchedEffect(enabled, interval) {
        if (!enabled) return@LaunchedEffect
        while (true) {
            delay(interval.toLong())
            if (vmList.periodicUpdate) {
                vmList.requestRefresh = true
            }
        }
    }
}
