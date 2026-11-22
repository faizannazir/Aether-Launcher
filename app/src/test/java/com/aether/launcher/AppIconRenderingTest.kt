package com.aether.launcher

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import com.aether.launcher.data.renderAppIcon
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppIconRenderingTest {
    @Test
    fun `wide legacy icons stay centered without stretching`() {
        val drawable = RecordingIcon(width = 80, height = 40)
        drawable.setBounds(1, 2, 3, 4)

        renderAppIcon(drawable, 100)

        assertEquals(Rect(0, 25, 100, 75), drawable.drawnBounds)
        assertEquals(Rect(1, 2, 3, 4), drawable.bounds)
    }

    @Test
    fun `tall legacy icons stay centered without stretching`() {
        val drawable = RecordingIcon(width = 40, height = 80)

        renderAppIcon(drawable, 100)

        assertEquals(Rect(25, 0, 75, 100), drawable.drawnBounds)
    }

    private class RecordingIcon(private val width: Int, private val height: Int) : ColorDrawable(Color.WHITE) {
        var drawnBounds: Rect? = null

        override fun getIntrinsicWidth(): Int = width
        override fun getIntrinsicHeight(): Int = height

        override fun draw(canvas: Canvas) {
            drawnBounds = copyBounds()
        }
    }
}
