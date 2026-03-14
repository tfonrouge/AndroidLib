package com.fonrouge.fslib.android.barcode

import android.media.ToneGenerator
import com.fonrouge.fslib.android.barcode.VMCamera
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VMCameraTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var vmCamera: VMCamera

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkConstructor(ToneGenerator::class)
        every { anyConstructed<ToneGenerator>().startTone(any()) } returns true
        every { anyConstructed<ToneGenerator>().release() } returns Unit
        vmCamera = VMCamera()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkConstructor(ToneGenerator::class)
    }

    @Test
    fun `initial state has scannerOpen false and codeScanned null`() = runTest {
        val state = vmCamera.uiState.value
        assertFalse(state.scannerOpen)
        assertNull(state.codeScanned)
    }

    @Test
    fun `UIEvent Open sets scannerOpen to true`() = runTest {
        vmCamera.onEvent(VMCamera.UIEvent.Open)
        assertTrue(vmCamera.uiState.value.scannerOpen)
    }

    @Test
    fun `UIEvent Close sets scannerOpen to false and torchState to false`() = runTest {
        vmCamera.onEvent(VMCamera.UIEvent.Open)
        vmCamera.setTorchState(true)
        assertTrue(vmCamera.uiState.value.scannerOpen)
        assertTrue(vmCamera.torchState.value)

        vmCamera.onEvent(VMCamera.UIEvent.Close)
        assertFalse(vmCamera.uiState.value.scannerOpen)
        assertFalse(vmCamera.torchState.value)
    }

    @Test
    fun `UIEvent CodeRead updates codeScanned and lastTime`() = runTest {
        val timeBefore = System.currentTimeMillis()
        vmCamera.onEvent(VMCamera.UIEvent.CodeRead("ABC-123"))

        assertEquals("ABC-123", vmCamera.uiState.value.codeScanned)
        assertTrue(vmCamera.lastTime >= timeBefore)
        assertTrue(vmCamera.lastTime <= System.currentTimeMillis())
    }

    @Test
    fun `setManualEntry updates manualEntry flow`() = runTest {
        assertEquals("", vmCamera.manualEntry.value)
        vmCamera.setManualEntry("MANUAL-001")
        assertEquals("MANUAL-001", vmCamera.manualEntry.value)
    }

    @Test
    fun `setTorchState updates torchState flow`() = runTest {
        assertFalse(vmCamera.torchState.value)
        vmCamera.setTorchState(true)
        assertTrue(vmCamera.torchState.value)
        vmCamera.setTorchState(false)
        assertFalse(vmCamera.torchState.value)
    }

    @Test
    fun `setSelectedCameraType updates selectedCameraType flow`() = runTest {
        vmCamera.setSelectedCameraType(VMCamera.CameraType.CameraX)
        assertEquals(VMCamera.CameraType.CameraX, vmCamera.selectedCameraType.value)
        vmCamera.setSelectedCameraType(VMCamera.CameraType.GooglePlay)
        assertEquals(VMCamera.CameraType.GooglePlay, vmCamera.selectedCameraType.value)
    }

    @Test
    fun `UIEvent ManualEntry triggers Close then CodeRead with manualEntry value`() = runTest {
        vmCamera.onEvent(VMCamera.UIEvent.Open)
        vmCamera.setTorchState(true)
        vmCamera.setManualEntry("MANUAL-999")

        vmCamera.onEvent(VMCamera.UIEvent.ManualEntry)

        // Close side-effects
        assertFalse(vmCamera.uiState.value.scannerOpen)
        assertFalse(vmCamera.torchState.value)
        // CodeRead side-effects
        assertEquals("MANUAL-999", vmCamera.uiState.value.codeScanned)
        assertTrue(vmCamera.lastTime > 0L)
    }
}
