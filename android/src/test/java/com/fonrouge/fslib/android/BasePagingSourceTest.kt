package com.fonrouge.fslib.android

import com.fonrouge.fslib.android.commonServices.AppApi
import com.fonrouge.fslib.android.commonServices.appApi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BasePagingSourceTest {

    @Before
    fun setup() {
        appApi = AppApi
    }

    @Test
    fun `appApi defaults to AppApi singleton`() {
        assertEquals(AppApi, appApi)
    }

    @Test
    fun `AppApi default values are correct`() {
        assertEquals("localhost", AppApi.urlBase)
        assertEquals("appRoute", AppApi.appRoute)
        assertEquals("0.0", AppApi.version)
        assertEquals(false, AppApi.logged)
    }

    @Test
    fun `AppApi clearHttpClient does not throw`() {
        AppApi.clearHttpClient()
    }
}
