package com.aether.launcher.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startupAndDrawerJourney() = baselineProfileRule.collect(
        packageName = "com.aether.launcher",
        profileBlock = {
            startActivityAndWait()
            device.waitForIdle()

            val centerX = device.displayWidth / 2
            val height = device.displayHeight
            device.swipe(centerX, height * 85 / 100, centerX, height * 60 / 100, 20)
            device.wait(Until.hasObject(By.text("App Drawer")), 5_000)
            device.swipe(centerX, height * 80 / 100, centerX, height * 25 / 100, 20)
            device.waitForIdle()
            device.pressBack()
        }
    )
}
