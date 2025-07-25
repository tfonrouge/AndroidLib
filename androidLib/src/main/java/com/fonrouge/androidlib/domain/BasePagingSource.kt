package com.fonrouge.androidlib.domain

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.fonrouge.androidlib.viewModel.VMList
import com.fonrouge.fsLib.common.ICommonContainer
import com.fonrouge.fsLib.model.apiData.IApiFilter
import com.fonrouge.fsLib.model.base.BaseDoc
import com.fonrouge.fsLib.model.state.SimpleState
import java.io.IOException

class BasePagingSource<CC : ICommonContainer<T, ID, FILT>, T : BaseDoc<ID>, ID : Any, FILT : IApiFilter<*>>(
    val vmList: VMList<CC, T, ID, FILT>,
) : PagingSource<Int, T>() {
    override fun getRefreshKey(state: PagingState<Int, T>): Int? {
        return state.anchorPosition?.let {
            state.closestPageToPosition(anchorPosition = it)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        return try {
            val nextPage = params.key ?: 1
            vmList.refreshingList.value = true
            val listState = vmList.listStateGetter(nextPage)
            vmList.refreshingList.value = false
            LoadResult.Page(
                data = listState.data,
                prevKey = if (nextPage == 1) null else nextPage - 1,
                nextKey = listState.last_page?.let { if (nextPage < it) nextPage + 1 else null }
            )
        } catch (e: IOException) {
            vmList.pushSimpleState(SimpleState(isOk = false, msgError = e.localizedMessage))
//            return LoadResult.Error(e)
            return LoadResult.Page(
                data = listOf(),
                prevKey = null,
                nextKey = null,
            )
        } finally {
            vmList.refreshingList.value = false
        }
    }
}
