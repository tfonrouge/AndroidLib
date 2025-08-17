package com.fonrouge.androidLib.configCommon

import android.net.Uri
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.fonrouge.base.api.ApiItem
import com.fonrouge.base.api.CrudTask
import com.fonrouge.base.api.IApiFilter
import com.fonrouge.base.api.IApiItem
import com.fonrouge.base.api.setMasterItemId
import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.base.common.toIApiItem
import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.state.ItemState
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.json.Json
import kotlin.reflect.KSuspendFunction1

val ICommonContainer<*, *, *>.routeItem: String get() = "ViewItem$name?apiItem={apiItem}"
val ICommonContainer<*, *, *>.routeList: String get() = "ViewList$name?apiFilter={apiFilter}"

@Composable
fun <CC : ICommonContainer<T, ID, FILT>, T : BaseDoc<ID>, ID : Any, FILT : IApiFilter<*>> CC.DecodeRouteItemParams(
    navBackStackEntry: NavBackStackEntry,
    function: @Composable (apiItem: ApiItem.Query<T, ID, FILT>) -> Unit,
) {
    val iApiItem = navBackStackEntry.arguments?.getString("apiItem")?.let {
        if (it != "\"null\"") Json.decodeFromString(
            IApiItem.Query.serializer(itemSerializer, idSerializer, apiFilterSerializer),
            it.removePrefix("\"").removeSuffix("\"")
        ) else null
    }
    val apiItem = iApiItem?.asApiItem(cc = this, call = null) as? ApiItem.Query<T, ID, FILT>
    apiItem?.let { function(apiItem) }
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
    apiItem: ApiItem.Query<T, ID, FILT>,
) {
    val serializedApiItem = Json.encodeToString(
        IApiItem.serializer(itemSerializer, idSerializer, apiFilterSerializer),
        toIApiItem(apiItem)
    )
    navHostController.navigate(
        "ViewItem$name?apiItem=\"${Uri.encode(serializedApiItem)}\""
    )
}

fun <CC : ICommonContainer<T, ID, FILT>, T : BaseDoc<ID>, ID : Any, FILT : IApiFilter<*>> CC.navigateCreateItem(
    navHostController: NavHostController,
    apiFilter: FILT = apiFilterInstance(),
) {
    val serializedApiItem = Json.encodeToString(
        IApiItem.serializer(itemSerializer, idSerializer, apiFilterSerializer),
        toIApiItem(ApiItem.Query.Create(apiFilter = apiFilter))
    )
    navHostController.navigate(
        "ViewItem$name?apiItem=\"${Uri.encode(serializedApiItem)}\""
    )
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

/**
 * Defines a composable screen for displaying or editing a single item within a navigation graph.
 *
 * This function simplifies the creation of a navigation destination that handles
 * receiving an `ApiItem.Query` object (representing an item to be displayed or a
 * new item to be created) as a navigation argument. It automatically decodes
 * the argument and provides it to your composable content.
 *
 * @param CC The type of the `ICommonContainer` which provides configuration for the item.
 * @param T The type of the data item (`BaseDoc`).
 * @param ID The type of the ID of the data item.
 * @param FILT The type of the API filter associated with this item.
 * @param commonContainer An instance of `ICommonContainer` that holds configuration
 *   details such as the route name and serializers for the item and its filter.
 *   The `routeItem` property of this container will be used to define the
 *   navigation route (e.g., "ViewItemMyEntity?apiItem={apiItem}").
 * @param function1 A composable lambda that receives an `AnimatedContentScope` and
 *   the decoded `ApiItem.Query<T, ID, FILT>` as parameters. This lambda is
 *   responsible for defining the UI content of the screen for the given item.
 *   The `ApiItem.Query` can represent an existing item fetched for display/editing
 *   or a new item scaffold if the user is creating a new entry.
 *
 * @see ICommonContainer.routeItem
 * @see ICommonContainer.DecodeRouteItemParams
 * @see navigateItem
 * @see navigateCreateItem
 *
 * @usage
 *
 *
 */
@Suppress("unused")
fun <CC : ICommonContainer<T, ID, FILT>, T : BaseDoc<ID>, ID : Any, FILT : IApiFilter<*>> NavGraphBuilder.composableItem(
    commonContainer: CC,
    function1: @Composable AnimatedContentScope.(ApiItem.Query<T, ID, FILT>) -> Unit,
) {
    composable(commonContainer.routeItem) { navBackStackEntry ->
        commonContainer.DecodeRouteItemParams(
            navBackStackEntry = navBackStackEntry,
        ) { it ->
            function1(it)
        }
    }
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
