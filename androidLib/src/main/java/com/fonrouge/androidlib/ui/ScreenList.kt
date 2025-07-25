package com.fonrouge.androidlib.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.fonrouge.androidlib.viewModel.VMList
import com.fonrouge.fsLib.common.ICommonContainer
import com.fonrouge.fsLib.model.apiData.IApiFilter
import com.fonrouge.fsLib.model.base.BaseDoc

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("unused")
@Composable
inline fun <reified VML : VMList<CC, T, ID, FILT>, CC : ICommonContainer<T, ID, FILT>, T : BaseDoc<ID>, ID : Any, FILT : IApiFilter<*>> ScreenList(
    navHostController: NavHostController = NavHostController(LocalContext.current),
    vmList: VML = viewModel(),
    noinline topBarTitle: @Composable () -> Unit = {},
    noinline topBarNavigationIcon: @Composable () -> Unit = {
        IconButton(onClick = { navHostController.popBackStack() }) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
    },
    noinline topBarActions: @Composable RowScope.() -> Unit = {},
    noinline bottomBar: @Composable () -> Unit = {},
    noinline floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    noinline modalNavigationDrawerContent: @Composable ColumnScope.(DrawerState) -> Unit = {},
    drawerState: DrawerState,
    noinline bodyListContent: @Composable (T?) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = topBarTitle,
                navigationIcon = topBarNavigationIcon,
                actions = topBarActions
            )
        },
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState(vmList)) },
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
    ) { paddingValues ->
        ScreenStateAlert(vmBase = vmList)
        ScreenConfirmAlert(viewBase = vmList)
        ModalNavigationDrawer(
            modifier = Modifier.padding(paddingValues),
            drawerState = drawerState,
            drawerContent = {
                ScreenFilter1(
                    drawerState = drawerState,
                    content = modalNavigationDrawerContent
                )
            }
        ) {
            BodyList(
                vmList = vmList,
                pullRefreshState = pullRefreshState(vmList = vmList),
                content = bodyListContent
            )
        }
    }
}

@Preview
@Composable
fun ScreenFilter1(
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
    content: @Composable ColumnScope.(DrawerState) -> Unit = {},
) {
    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxSize(0.8f)
            .padding(top = 10.dp),
    ) {
        Spacer(modifier = Modifier.size(20.dp))
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = { content(drawerState) }
        )
    }
}
