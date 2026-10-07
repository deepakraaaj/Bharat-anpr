package com.bharatanpr

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class AppLaunchTest {
    @get:Rule val hilt=HiltAndroidRule(this)
    @Before fun setup(){hilt.inject()}
    @Test fun appStarts(){
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertTrue(scenario.state.isAtLeast(Lifecycle.State.STARTED))
        }
    }
}
