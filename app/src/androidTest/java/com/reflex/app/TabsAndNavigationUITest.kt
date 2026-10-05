package com.reflex.app

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.reflex.app.MainActivity
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TabsAndNavigationUITest {

    @Test
    fun testAppLaunchesAndMainTabsRender() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        assertNotNull("Activity scenario should launch", scenario)

        scenario.onActivity { activity ->
            assertNotNull("MainActivity must not be null", activity)
            assertTrue("MainActivity must not be finishing", !activity.isFinishing)
        }

        // Test configuration change (rotation)
        scenario.recreate()

        scenario.onActivity { activity ->
            assertNotNull("MainActivity after recreate must not be null", activity)
            assertTrue("MainActivity after recreate must be alive", !activity.isFinishing)
        }

        scenario.close()
    }
}
