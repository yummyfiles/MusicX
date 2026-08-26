package com.yummyfiles.musicx

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Just a test that runs on a real device or emulator.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Grabbing the context for the app we're testing.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.yummyfiles.musicx", appContext.packageName)
    }
}