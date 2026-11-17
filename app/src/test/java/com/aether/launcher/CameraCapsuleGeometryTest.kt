package com.aether.launcher

import android.graphics.RectF
import com.aether.launcher.notification.cameraCapsuleGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class CameraCapsuleGeometryTest {
    @Test
    fun `camera pill centers on the physical Pixel camera path`() {
        val camera = RectF(506f, 34f, 574f, 102f)
        val geometry = cameraCapsuleGeometry(1080, 3.375f, camera)

        assertEquals(0, geometry.offsetX)
        assertEquals(216, geometry.width)
        assertEquals(24, geometry.top)
        assertEquals(88, geometry.height)
        assertTrue(geometry.top <= camera.top)
        assertTrue(geometry.top + geometry.height >= camera.bottom)
    }

    @Test
    fun `left corner camera keeps the capsule inside the screen`() {
        val geometry = cameraCapsuleGeometry(1080, 3f, RectF(30f, 30f, 90f, 90f))
        assertEquals(-444, geometry.offsetX)
        assertEquals(21, geometry.top)
    }

    @Test
    @Config(sdk = [26])
    fun `fallback pill stays centered when cutout information is missing`() {
        val geometry = cameraCapsuleGeometry(1080, 3f, null)
        assertEquals(0, geometry.offsetX)
        assertEquals(0, geometry.top)
        assertEquals(192, geometry.width)
    }
}
