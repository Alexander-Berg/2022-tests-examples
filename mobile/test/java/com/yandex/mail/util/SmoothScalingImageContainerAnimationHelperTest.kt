package com.yandex.mail.util

import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.yandex.mail.runners.UnitTestRunner
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset.offset
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(UnitTestRunner::class)
class SmoothScalingImageContainerAnimationHelperTest {

    lateinit var animationListener: ImageContainerAnimator.OnAnimationListener

    lateinit var animationView: ClippingImageView

    @Test
    fun `initAnimationParams fill phone vertical`() {
        val thumbRect = Rect(100, 100, 200, 200)
        val bitmapRect = RectF(0f, 0f, 320f, 480f)
        val expandedViewRect = Rect(0, 48, 320, 480)
        val containerOffset = Point(0, 48)
        val animationHelper = mockImageContainerAnimationHelper(thumbRect, bitmapRect, expandedViewRect, containerOffset)

        assertThat(animationHelper.startScale).isEqualTo(0.3472f, offset(0.0001f))
        assertThat(animationHelper.visibleStartBounds.left).isEqualTo(94.4f, offset(0.1f))
        assertThat(animationHelper.visibleStartBounds.right).isEqualTo(205.5f, offset(0.1f))
        assertThat(animationHelper.visibleStartBounds.top).isEqualTo(27f, offset(0.1f))
        assertThat(animationHelper.visibleStartBounds.bottom).isEqualTo(177f, offset(0.1f))
        assertThat(animationHelper.clipLeft).isEqualTo(5.5f, offset(0.1f))
        assertThat(animationHelper.clipRight).isEqualTo(5.5f, offset(0.1f))
        assertThat(animationHelper.clipVertical).isEqualTo(25f, offset(0.1f))
    }

    @Test
    fun `initAnimationParams fill phone horizontal`() {
        val thumbRect = Rect(100, 100, 200, 200)
        val bitmapRect = RectF(0f, 0f, 480f, 320f)
        val expandedViewRect = Rect(0, 48, 320, 480)
        val containerOffset = Point(0, 48)
        val animationHelper = mockImageContainerAnimationHelper(thumbRect, bitmapRect, expandedViewRect, containerOffset)

        assertThat(animationHelper.startScale).isEqualTo(0.4687f, offset(0.0001f))
        assertThat(animationHelper.visibleStartBounds.left).isEqualTo(75f, offset(0.1f))
        assertThat(animationHelper.visibleStartBounds.right).isEqualTo(225f, offset(0.1f))
        assertThat(animationHelper.visibleStartBounds.top).isEqualTo(0.75f, offset(0.1f))
        assertThat(animationHelper.visibleStartBounds.bottom).isEqualTo(203.25f, offset(0.1f))
        assertThat(animationHelper.clipLeft).isEqualTo(25f, Assertions.offset(0.1f))
        assertThat(animationHelper.clipRight).isEqualTo(25f, Assertions.offset(0.1f))
        assertThat(animationHelper.clipVertical).isEqualTo(0f, Assertions.offset(0.1f))
    }

    @Test
    fun `initAnimationParams fill tablet vertical`() {
        val thumbRect = Rect(100, 100, 200, 200)
        val bitmapRect = RectF(0f, 0f, 1536f, 1024f)
        val expandedViewRect = Rect(0, 56, 1280, 800)
        val containerOffset = Point(0, 56)
        val animationHelper = mockImageContainerAnimationHelper(thumbRect, bitmapRect, expandedViewRect, containerOffset)

        assertThat(animationHelper.startScale).isEqualTo(0.1344f, offset(0.0001f))
        assertThat(animationHelper.visibleStartBounds.left).isEqualTo(63.98f, offset(0.1f))
        assertThat(animationHelper.visibleStartBounds.right).isEqualTo(236.02f, offset(0.1f))
        assertThat(animationHelper.visibleStartBounds.top).isEqualTo(44f, offset(0.1f))
        assertThat(animationHelper.visibleStartBounds.bottom).isEqualTo(144f, offset(0.1f))
        assertThat(animationHelper.clipLeft).isEqualTo(36.02f, Assertions.offset(0.1f))
        assertThat(animationHelper.clipRight).isEqualTo(36.02f, Assertions.offset(0.1f))
        assertThat(animationHelper.clipVertical).isEqualTo(0f, Assertions.offset(0.1f))
    }

    @Test
    fun `initAnimationParams fill tablet horizontal`() {
        val thumbRect = Rect(100, 100, 200, 200)
        val bitmapRect = RectF(0f, 0f, 2048f, 1024f)
        val expandedViewRect = Rect(0, 56, 1280, 800)
        val containerOffset = Point(0, 56)
        val animationHelper = mockImageContainerAnimationHelper(thumbRect, bitmapRect, expandedViewRect, containerOffset)

        assertThat(animationHelper.startScale).isEqualTo(0.1562f, offset(0.0001f))
        assertThat(animationHelper.visibleStartBounds.left).isEqualTo(50f, offset(0.1f))
        assertThat(animationHelper.visibleStartBounds.right).isEqualTo(250f, offset(0.1f))
        assertThat(animationHelper.visibleStartBounds.top).isEqualTo(35.9f, offset(0.1f))
        assertThat(animationHelper.visibleStartBounds.bottom).isEqualTo(152.1f, offset(0.1f))
        assertThat(animationHelper.clipLeft).isEqualTo(50f, Assertions.offset(0.1f))
        assertThat(animationHelper.clipRight).isEqualTo(50f, Assertions.offset(0.1f))
        assertThat(animationHelper.clipVertical).isEqualTo(0f, Assertions.offset(0.1f))
    }

    @Test
    fun `initAnimationParams image partially hidden left`() {
        val thumbGlobalRect = Rect(0, 100, 50, 200)
        val thumbLocalRect = Rect(50, 0, 100, 100)
        val thumbSize = Point(100, 100)
        val bitmapRect = RectF(0f, 0f, 320f, 480f)
        val expandedViewRect = Rect(0, 48, 320, 480)
        val containerOffset = Point(0, 48)
        val animationHelper =
            mockImageContainerAnimationHelper(thumbGlobalRect, thumbLocalRect, thumbSize, bitmapRect, expandedViewRect, containerOffset)

        assertThat(animationHelper.startScale).isEqualTo(0.3472f, offset(0.0001f))
        assertThat(animationHelper.visibleStartBounds.left).isEqualTo(-55.5f, offset(0.1f))
        assertThat(animationHelper.visibleStartBounds.right).isEqualTo(55.5f, offset(0.1f))
        assertThat(animationHelper.visibleStartBounds.top).isEqualTo(27f, offset(0.1f))
        assertThat(animationHelper.visibleStartBounds.bottom).isEqualTo(177f, offset(0.1f))
        assertThat(animationHelper.clipLeft).isEqualTo(55.5f, offset(0.1f))
        assertThat(animationHelper.clipRight).isEqualTo(5.5f, offset(0.1f))
        assertThat(animationHelper.clipVertical).isEqualTo(25f, offset(0.1f))
    }

    @Test
    fun `initAnimationParams image partially hidden right`() {
        val thumbGlobalRect = Rect(270, 100, 320, 200)
        val thumbLocalRect = Rect(0, 100, 50, 100)
        val thumbSize = Point(100, 100)
        val bitmapRect = RectF(0f, 0f, 320f, 480f)
        val expandedViewRect = Rect(0, 48, 320, 480)
        val containerOffset = Point(0, 48)
        val animationHelper =
            mockImageContainerAnimationHelper(thumbGlobalRect, thumbLocalRect, thumbSize, bitmapRect, expandedViewRect, containerOffset)

        assertThat(animationHelper.startScale).isEqualTo(0.3472f, offset(0.0001f))
        assertThat(animationHelper.visibleStartBounds.left).isEqualTo(264.4f, offset(0.1f))
        assertThat(animationHelper.visibleStartBounds.right).isEqualTo(375.5f, offset(0.1f))
        assertThat(animationHelper.visibleStartBounds.top).isEqualTo(27f, offset(0.1f))
        assertThat(animationHelper.visibleStartBounds.bottom).isEqualTo(177f, offset(0.1f))
        assertThat(animationHelper.clipLeft).isEqualTo(5.5f, offset(0.1f))
        assertThat(animationHelper.clipRight).isEqualTo(55.5f, offset(0.1f))
        assertThat(animationHelper.clipVertical).isEqualTo(25f, offset(0.1f))
    }

    private fun mockImageContainerAnimationHelper(
        thumbRect: Rect,
        bitmapRect: RectF,
        expandedRect: Rect,
        containerOffset: Point
    ): SmoothScalingImageContainerAnimationHelper {
        val localRect = Rect(thumbRect)
        localRect.offsetTo(0, 0)

        val thumbSize = Point(thumbRect.width(), thumbRect.height())

        return mockImageContainerAnimationHelper(thumbRect, localRect, thumbSize, bitmapRect, expandedRect, containerOffset)
    }

    private fun mockImageContainerAnimationHelper(
        thumbGlobalRect: Rect,
        thumbLocalRect: Rect,
        thumbSize: Point,
        bitmapRect: RectF,
        expandedRect: Rect,
        containerOffset: Point
    ): SmoothScalingImageContainerAnimationHelper {
        val thumbView = mock<ImageView> {
            on { getGlobalVisibleRect(Rect()) } doAnswer {
                (it.arguments[0] as Rect).set(thumbGlobalRect)
                true
            }

            on { getLocalVisibleRect(Rect()) } doAnswer {
                (it.arguments[0] as Rect).set(thumbLocalRect)
                true
            }

            on { width } doReturn thumbSize.x
        }

        val rootView = mock<FrameLayout>()

        animationView = mock {
            on { getBitmapRect() } doReturn bitmapRect
        }

        val scaledView = mock<View> {
            on { getGlobalVisibleRect(Rect(), Point()) } doAnswer {
                (it.arguments[0] as Rect).set(expandedRect)
                (it.arguments[1] as Point).set(containerOffset.x, containerOffset.y)
                true
            }
        }

        animationListener = mock()

        return SmoothScalingImageContainerAnimationHelper(animationListener, thumbView, rootView, scaledView, animationView)
    }
}
