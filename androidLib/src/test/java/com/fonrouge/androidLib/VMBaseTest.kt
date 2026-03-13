package com.fonrouge.androidLib

import com.fonrouge.androidLib.viewModel.VMBase
import com.fonrouge.base.state.SimpleState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VMBaseTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var vmBase: TestVMBase

    class TestVMBase : VMBase()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        vmBase = TestVMBase()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `pushSimpleState emits value to snackBarStatus`() = runTest {
        val state = SimpleState(isOk = true, msgOk = "Success")
        vmBase.pushSimpleState(state)
        assertEquals(state, vmBase.snackBarStatus.value)
    }

    @Test
    fun `pushSimpleState null clears snackBarStatus`() = runTest {
        vmBase.pushSimpleState(SimpleState(isOk = true, msgOk = "Success"))
        vmBase.pushSimpleState(null)
        assertNull(vmBase.snackBarStatus.value)
    }

    @Test
    fun `pushStateAlert sets stateAlert for error state`() = runTest {
        val errorState = SimpleState(isOk = false, msgError = "Something went wrong")
        vmBase.pushStateAlert(itemState = errorState)
        assertNotNull(vmBase.stateAlert.value)
        assertEquals(errorState, vmBase.stateAlert.value?.simpleState)
    }

    @Test
    fun `clearStateAlert removes stateAlert`() = runTest {
        val errorState = SimpleState(isOk = false, msgError = "Error")
        vmBase.pushStateAlert(itemState = errorState)
        vmBase.clearStateAlert()
        assertNull(vmBase.stateAlert.value)
    }

    @Test
    fun `pushConfirmAlert sets confirmAlert`() = runTest {
        vmBase.pushConfirmAlert(
            confirmText = "Are you sure?",
            onConfirm = {}
        )
        assertNotNull(vmBase.confirmAlert.value)
        assertEquals("Are you sure?", vmBase.confirmAlert.value?.confirmText)
    }

    @Test
    fun `clearConfirmAlert removes confirmAlert`() = runTest {
        vmBase.pushConfirmAlert(confirmText = "Confirm?", onConfirm = {})
        vmBase.clearConfirmAlert()
        assertNull(vmBase.confirmAlert.value)
    }

    @Test
    fun `pushConfirmCancelAlert sets confirmAlert with cancel type`() = runTest {
        vmBase.pushConfirmCancelAlert(
            confirmText = "Cancel this?",
            onConfirm = {}
        )
        assertNotNull(vmBase.confirmAlert.value)
        assertEquals("Cancel this?", vmBase.confirmAlert.value?.confirmText)
    }

    @Test
    fun `multiple pushSimpleState calls update value`() = runTest {
        val state1 = SimpleState(isOk = true, msgOk = "First")
        val state2 = SimpleState(isOk = false, msgError = "Second")
        vmBase.pushSimpleState(state1)
        assertEquals(state1, vmBase.snackBarStatus.value)
        vmBase.pushSimpleState(state2)
        assertEquals(state2, vmBase.snackBarStatus.value)
    }
}
