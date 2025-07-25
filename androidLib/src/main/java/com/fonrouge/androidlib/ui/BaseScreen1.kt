package com.fonrouge.androidlib.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.fonrouge.androidlib.viewModel.VMBase
import com.fonrouge.androidlib.viewModel.VMList
import com.fonrouge.fsLib.common.ICommonContainer
import com.fonrouge.fsLib.model.apiData.IApiFilter
import com.fonrouge.fsLib.model.base.BaseDoc
import eu.bambooapps.material3.pullrefresh.PullRefreshIndicator
import eu.bambooapps.material3.pullrefresh.PullRefreshState
import eu.bambooapps.material3.pullrefresh.pullRefresh
import eu.bambooapps.material3.pullrefresh.rememberPullRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <CC : ICommonContainer<T, ID, FILT>, T : BaseDoc<ID>, ID : Any, FILT : IApiFilter<*>> BodyList(
    paddingValues: PaddingValues? = null,
    vmList: VMList<CC, T, ID, FILT>,
    pullRefreshState: PullRefreshState,
    content: @Composable (T?) -> Unit,
) {
    Box(
        modifier = Modifier
            .pullRefresh(state = pullRefreshState)
            .let { modifier -> paddingValues?.let { modifier.padding(it) } ?: modifier }
    ) {
        val items: LazyPagingItems<T> = vmList.flowPagingData.collectAsLazyPagingItems()
        PullRefreshIndicator(
            refreshing = vmList.refreshingList.value,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(1f)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
        ) {
            if (vmList.requestRefresh) {
                vmList.requestRefresh = false
                items.refresh()
            }
            items(
                count = items.itemCount,
                key = items.itemKey(),
                contentType = items.itemContentType()
            ) { index ->
                ItemCard2(index, items, content)
            }
        }
    }
}

@Composable
fun <T : Any> ItemCard2(
    index: Int,
    items: LazyPagingItems<T>,
    content: @Composable (T?) -> Unit,
) {
    content(items[index])
}

@Suppress("unused")
@Composable
fun ItemCard(
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(bottom = 5.dp, top = 5.dp, start = 5.dp, end = 5.dp)
            .fillMaxWidth()
            .let { modifier -> onClick?.let { modifier.clickable(onClick = it) } ?: modifier },
        shape = RoundedCornerShape(5.dp),
        elevation = CardDefaults.cardElevation(12.dp),
        content = content
    )
}

@Suppress("unused")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DismissBackgroundDelete(
    dismissState: SwipeToDismissBoxState,
    dismissDirection: SwipeToDismissBoxValue,
) {
    var vibrate by remember { mutableStateOf(false) }
    val color =
        if (dismissState.dismissDirection == dismissDirection && dismissState.progress < 1) {
            if (!vibrate) {
                vibrate = true
                val haptic = LocalHapticFeedback.current
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            Color(0xFFFF1744)
        } else {
            vibrate = false
            Color.Transparent
        }
    val direction = dismissState.dismissDirection
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(12.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(modifier = Modifier)
        if (direction == dismissDirection) Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "delete"
        )
    }
}

@Composable
fun snackbarHostState(vmBase: VMBase): SnackbarHostState {
    val snackbarHostState = remember { SnackbarHostState() }
    val simpleState by vmBase.snackBarStatus.collectAsState()
//    LaunchedEffect(key1 = simpleState?.dateTime) {
    LaunchedEffect(key1 = simpleState) {
        simpleState?.let {
            when (it.hasError.not()) {
                true -> it.msgOk?.let { it1 ->
                    snackbarHostState.showSnackbar(
                        message = it1,
                        actionLabel = "Ok",
                        duration = SnackbarDuration.Short
                    )
                }

                false -> it.msgError?.let { it1 ->
                    snackbarHostState.showSnackbar(
                        message = it1,
                        actionLabel = "Accept",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
        vmBase.pushSimpleState(null)
    }
    return snackbarHostState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : BaseDoc<*>> pullRefreshState(vmList: VMList<*, T, *, *>): PullRefreshState {
    return rememberPullRefreshState(
        refreshing = vmList.refreshingList.value,
        onRefresh = {
            vmList.requestRefresh = true
        }
    )
}
