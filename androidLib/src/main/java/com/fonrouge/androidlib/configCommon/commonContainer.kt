package com.fonrouge.androidlib.configCommon

import android.net.Uri
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.fonrouge.fsLib.common.ICommonContainer
import com.fonrouge.fsLib.common.toIApiItem
import com.fonrouge.fsLib.model.apiData.*
import com.fonrouge.fsLib.model.apiData.ApiItem
import com.fonrouge.fsLib.model.base.BaseDoc
import com.fonrouge.fsLib.model.state.ItemState
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.json.Json
import kotlin.reflect.KSuspendFunction1

val ICommonContainer<*, *, *>.routeItem: String get() = "ViewItem$name?apiItem={apiItem}"
val ICommonContainer<*, *, *>.routeList: String get() = "ViewList$name?apiFilter={apiFilter}"

@Composable
fun <CC : ICommonContainer<T, ID, FILT>, T : BaseDoc<ID>, ID : Any, FILT : IApiFilter<*>> CC.DecodeRouteItemParams(
    navBackStackEntry: NavBackStackEntry,
    function: @Composable (apiItem: ApiItem<T, ID, FILT>) -> Unit,
) {
    /*
        val iApiItem = navBackStackEntry.arguments?.getString("apiItem")?.let {
            if (it != "\"null\"") Json.decodeFromString(
                IApiItem.Query.serializer(itemSerializer, idSerializer, apiFilterSerializer),
                it.removePrefix("\"").removeSuffix("\"")
            ) else null
        }
        val apiItem = iApiItem?.asApiItem(this) as? ApiItem.Query<T, ID, FILT>
        apiItem?.let { function(apiItem) }
    */
}

@Composable
fun <CC : ICommonContainer<T, ID, FILT>, T : BaseDoc<ID>, ID : Any, FILT : IApiFilter<*>> CC.DecodeRouteListParams(
    navBackStackEntry: NavBackStackEntry,
    function: @Composable (apiFilter: FILT) -> Unit,
) {
    val serializedApiFilter =
        navBackStackEntry.arguments?.getString("apiFilter")?.removePrefix("\"")?.removeSuffix("\"")
    function(
        serializedApiFilter?.let {
            Json.decodeFromString(
                apiFilterSerializer.nullable,
                it
            )
        } ?: apiFilterInstance()
    )
}

/**
 * Navigates to the detailed view of the given API item using the provided NavHostController.
 *
 * @param navHostController The NavHostController to perform the navigation.
 * @param apiItem The API item to be navigated to.
 * @param CC The common container type.
 * @param T The base document type.
 * @param ID The ID type of the base document.
 * @param FILT The API filter type.
 */
@Suppress("unused")
fun <CC : ICommonContainer<T, ID, FILT>, T : BaseDoc<ID>, ID : Any, FILT : IApiFilter<*>> CC.navigateItem(
    navHostController: NavHostController,
//    apiItem: ApiItem.Query<T, ID, FILT>,
) {
    /*
        val serializedApiItem = Json.encodeToString(
            IApiItem.serializer(itemSerializer, idSerializer, apiFilterSerializer),
            toIApiItem(apiItem)
        )
        navHostController.navigate(
            "ViewItem$name?apiItem=\"${Uri.encode(serializedApiItem)}\""
        )
    */
}

fun <CC : ICommonContainer<T, ID, FILT>, T : BaseDoc<ID>, ID : Any, FILT : IApiFilter<*>> CC.navigateCreateItem(
    navHostController: NavHostController,
    apiFilter: FILT = apiFilterInstance(),
) {
    /*
        val serializedApiItem = Json.encodeToString(
            IApiItem.serializer(itemSerializer, idSerializer, apiFilterSerializer),
            toIApiItem(ApiItem.Query.Upsert.Create(apiFilter = apiFilter))
        )
        navHostController.navigate(
            "ViewItem$name?apiItem=\"${Uri.encode(serializedApiItem)}\""
        )
    */
}

/**
 * Navigates to a child list view with the specified API filter.
 *
 * @param navHostController the Navigation Host Controller
 * @param masterItem the master item to serialize its ID for the API filter
 * @throws Exception if an error occurs while creating the API filter instance
 */
@Suppress("unused")
inline fun <MI : BaseDoc<MID>, reified MID : Any, CC : ICommonContainer<T, ID, FILT>, T : BaseDoc<ID>, ID : Any, FILT : IApiFilter<MID>> CC.navigateChildList(
    navHostController: NavHostController,
    masterItem: MI?,
) {
    return navigateList(
        navHostController = navHostController,
        apiFilterFactory = {
            it.setMasterItemId(masterItem?._id)
        }
    )
}

/**
 * Navigate to the list view with the specified API filter.
 *
 * @param navHostController the Navigation Host Controller
 * @param apiFilterFactory to allow to refactor the apiFilter instance
 *
 * @throws Exception if an error occurs while creating the API filter instance
 */
fun <CC : ICommonContainer<T, ID, FILT>, T : BaseDoc<ID>, ID : Any, FILT : IApiFilter<*>> CC.navigateList(
    navHostController: NavHostController,
    apiFilterFactory: ((FILT) -> FILT)? = null,
) {
    val apiFilter: FILT = apiFilterFactory?.let { it(apiFilterInstance()) } ?: apiFilterInstance()
    val serializedApiFilter = Json.encodeToString(apiFilterSerializer, apiFilter)
    navHostController.navigate(
        "ViewList$name?apiFilter=\"${Uri.encode(serializedApiFilter)}\""
    )
}

/**
 * Calls the item API using the provided function.
 *
 * @param function the suspend function that takes an [ApiItem] and returns an [ItemState]
 * @param id the ID of the item
 * @param item the item object
 * @param callType the type of API call, default is [ApiItem.CallType.Query]
 * @param crudTask the CRUD task, default is [CrudTask.Read]
 * @param apiFilter the API filter object, default is the instance created by [apiFilterInstance]
 * @param onResponse optional callback function to handle the [ItemState] response
 * @return the [ItemState] result of the function call
 */
@Suppress("unused")
suspend fun <CC : ICommonContainer<T, ID, FILT>, T : BaseDoc<ID>, ID : Any, FILT : IApiFilter<*>> CC.callItemApi(
    function: KSuspendFunction1<IApiItem<T, ID, FILT>, ItemState<T>>,
    apiItem: ApiItem<T, ID, FILT>,
    onResponse: (CC.(ItemState<T>) -> Unit)? = null,
): ItemState<T> {
    val itemState = function(toIApiItem(apiItem))
    onResponse?.let { it(itemState) }
    return itemState
}

@Suppress("unused")
fun <CC : ICommonContainer<T, ID, FILT>, T : BaseDoc<ID>, ID : Any, FILT : IApiFilter<*>> NavGraphBuilder.composableItem(
    commonContainer: CC,
//    function: @Composable AnimatedContentScope.(ApiItem.Query<T, ID, FILT>) -> Unit,
) {
    /*
        composable(commonContainer.routeItem) { navBackStackEntry ->
            commonContainer.DecodeRouteItemParams(
                navBackStackEntry = navBackStackEntry,
            ) {
                function(it)
            }
        }
    */
}

@Suppress("unused")
fun <CC : ICommonContainer<T, ID, FILT>, T : BaseDoc<ID>, ID : Any, FILT : IApiFilter<*>> NavGraphBuilder.composableList(
    commonContainer: CC,
    function: @Composable AnimatedContentScope.(FILT) -> Unit,
) {
    composable(commonContainer.routeList) { navBackStackEntry ->
        commonContainer.DecodeRouteListParams(
            navBackStackEntry = navBackStackEntry,
        ) {
            function(it)
        }
    }
}
