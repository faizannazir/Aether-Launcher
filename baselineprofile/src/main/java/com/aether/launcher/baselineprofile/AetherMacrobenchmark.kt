package com.aether.launcher.baselineprofile

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AetherMacrobenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartup() {
        benchmarkRule.measureRepeated(
            packageName = "com.aether.launcher",
            metrics = listOf(StartupTimingMetric()),
            iterations = 5,
            startupMode = StartupMode.COLD,
            setupBlock = { pressHome() }
        ) {
            startActivityAndWait()
        }
    }

    @Test
    fun drawerScrollFrameTiming() {
        benchmarkRule.measureRepeated(
            packageName = "com.aether.launcher",
            metrics = listOf(FrameTimingMetric()),
            iterations = 5,
            startupMode = StartupMode.WARM,
            setupBlock = {
                pressHome()
                startActivityAndWait()
                val centerX = device.displayWidth / 2
                val height = device.displayHeight
                device.swipe(centerX, height * 85 / 100, centerX, height * 60 / 100, 20)
                device.wait(Until.hasObject(By.text("App Drawer")), 5_000)
            }
        ) {
            val centerX = device.displayWidth / 2
            val height = device.displayHeight
            device.swipe(centerX, height * 80 / 100, centerX, height * 25 / 100, 20)
        }
    }
}
