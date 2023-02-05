package com.yandex.launcher.widget.wallpaper

import android.os.Bundle
import android.os.Looper
import com.android.launcher3.CellInfo
import com.yandex.launcher.BaseRobolectricTest
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.robolectric.Shadows

const val KEY_STATE = "state"

class ChangeWallpaperWidgetStateRestoreTest: BaseRobolectricTest() {

    @Test
    fun `undoViewAttached is false, after restore undoViewAttached is false`() {
        val state = ChangeWallpaperWidgetState(undoViewAttached = false)
        val bundleState = Bundle()

        bundleState.putBundle(KEY_STATE, state.toBundle())
        val restoredState: ChangeWallpaperWidgetState? = ChangeWallpaperWidgetState.fromBundle(bundleState.getBundle(KEY_STATE))

        assertThat(state.undoViewAttached, equalTo(restoredState!!.undoViewAttached))
    }

    @Test
    fun `undoViewAttached is true, after restore undoViewAttached is true`() {
        val state = ChangeWallpaperWidgetState(undoViewAttached = true)
        val bundleState = Bundle()

        bundleState.putBundle(KEY_STATE, state.toBundle())
        val restoredState: ChangeWallpaperWidgetState? = ChangeWallpaperWidgetState.fromBundle(bundleState.getBundle(KEY_STATE))

        assertThat(state.undoViewAttached, equalTo(restoredState!!.undoViewAttached))
    }

    @Test
    fun `activityDestroyed is false, after restore activityDestroyed is false`() {
        val state = ChangeWallpaperWidgetState(undoViewAttached = false)
        val bundleState = Bundle()

        bundleState.putBundle(KEY_STATE, state.toBundle())
        val restoredState: ChangeWallpaperWidgetState? = ChangeWallpaperWidgetState.fromBundle(bundleState.getBundle(KEY_STATE))

        assertThat(state.undoViewAttached, equalTo(restoredState!!.undoViewAttached))
    }

    @Test
    fun `widgetAnimationStage is IDLE, after restore widgetAnimationStage is IDLE`() {
        val state = ChangeWallpaperWidgetState(widgetAnimationStage = ChangeWallpaperWidgetStage.IDLE)
        val bundleState = Bundle()

        bundleState.putBundle(KEY_STATE, state.toBundle())
        val restoredState: ChangeWallpaperWidgetState? = ChangeWallpaperWidgetState.fromBundle(bundleState.getBundle(KEY_STATE))

        assertThat(state.widgetAnimationStage, equalTo(restoredState!!.widgetAnimationStage))
    }

    @Test
    fun `widgetAnimationStage is MAIN, after restore widgetAnimationStage is MAIN`() {
        val state = ChangeWallpaperWidgetState(widgetAnimationStage = ChangeWallpaperWidgetStage.MAIN)
        val bundleState = Bundle()

        bundleState.putBundle(KEY_STATE, state.toBundle())
        val restoredState: ChangeWallpaperWidgetState? = ChangeWallpaperWidgetState.fromBundle(bundleState.getBundle(KEY_STATE))

        assertThat(state.widgetAnimationStage, equalTo(restoredState!!.widgetAnimationStage))
    }

    @Test
    fun `widgetAnimationStage is STARTING, after restore widgetAnimationStage is STARTING`() {
        val state = ChangeWallpaperWidgetState(widgetAnimationStage = ChangeWallpaperWidgetStage.STARTING)
        val bundleState = Bundle()

        bundleState.putBundle(KEY_STATE, state.toBundle())
        val restoredState: ChangeWallpaperWidgetState? = ChangeWallpaperWidgetState.fromBundle(bundleState.getBundle(KEY_STATE))

        assertThat(state.widgetAnimationStage, equalTo(restoredState!!.widgetAnimationStage))
    }

    @Test
    fun `widgetAnimationStage is ENDING, after restore widgetAnimationStage is ENDING`() {
        val state = ChangeWallpaperWidgetState(widgetAnimationStage = ChangeWallpaperWidgetStage.ENDING)
        val bundleState = Bundle()

        bundleState.putBundle(KEY_STATE, state.toBundle())
        val restoredState: ChangeWallpaperWidgetState? = ChangeWallpaperWidgetState.fromBundle(bundleState.getBundle(KEY_STATE))

        assertThat(state.widgetAnimationStage, equalTo(restoredState!!.widgetAnimationStage))
    }

    @Test
    fun `widgetAnimationStage is HIDING, after restore widgetAnimationStage is HIDING`() {
        val state = ChangeWallpaperWidgetState(widgetAnimationStage = ChangeWallpaperWidgetStage.HIDING)
        val bundleState = Bundle()

        bundleState.putBundle(KEY_STATE, state.toBundle())
        val restoredState: ChangeWallpaperWidgetState? = ChangeWallpaperWidgetState.fromBundle(bundleState.getBundle(KEY_STATE))

        assertThat(state.widgetAnimationStage, equalTo(restoredState!!.widgetAnimationStage))
    }

    @Test
    fun `undoViewScheduleTime is 100500, after restore undoViewScheduleTime is 100500`() {
        val state = ChangeWallpaperWidgetState(undoViewScheduleTime = 100500L)
        val bundleState = Bundle()

        bundleState.putBundle(KEY_STATE, state.toBundle())
        val restoredState: ChangeWallpaperWidgetState? = ChangeWallpaperWidgetState.fromBundle(bundleState.getBundle(KEY_STATE))

        assertThat(state.undoViewScheduleTime, equalTo(restoredState!!.undoViewScheduleTime))
    }

    @Test
    fun `activeWidgetPosition is null, after restore activeWidgetPosition is null`() {
        val state = ChangeWallpaperWidgetState(activeWidgetPosition = null)
        val bundleState = Bundle()

        bundleState.putBundle(KEY_STATE, state.toBundle())
        val restoredState: ChangeWallpaperWidgetState? = ChangeWallpaperWidgetState.fromBundle(bundleState.getBundle(KEY_STATE))
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(restoredState!!.activeWidgetPosition, nullValue())
    }

    @Test
    fun `activeWidgetPosition is 1,2,3, after restore activeWidgetPosition is 1,2,3`() {
        val state = ChangeWallpaperWidgetState(activeWidgetPosition = CellInfo(1L, 2, 3))
        val bundleState = Bundle()

        bundleState.putBundle(KEY_STATE, state.toBundle())
        val restoredState: ChangeWallpaperWidgetState? = ChangeWallpaperWidgetState.fromBundle(bundleState.getBundle(KEY_STATE))

        assertThat(restoredState!!.activeWidgetPosition, equalTo(state.activeWidgetPosition))
    }

    @Test
    fun `cancelMainAnimation is false, after restore cancelMainAnimation is false`() {
        val state = ChangeWallpaperWidgetState(cancelMainAnimation = false)
        val bundleState = Bundle()

        bundleState.putBundle(KEY_STATE, state.toBundle())
        val restoredState: ChangeWallpaperWidgetState? = ChangeWallpaperWidgetState.fromBundle(bundleState.getBundle(KEY_STATE))

        assertThat(state.cancelMainAnimation, equalTo(restoredState!!.cancelMainAnimation))
    }

    @Test
    fun `cancelMainAnimation is true, after restore cancelMainAnimation is true`() {
        val state = ChangeWallpaperWidgetState(cancelMainAnimation = true)
        val bundleState = Bundle()

        bundleState.putBundle(KEY_STATE, state.toBundle())
        val restoredState: ChangeWallpaperWidgetState? = ChangeWallpaperWidgetState.fromBundle(bundleState.getBundle(KEY_STATE))

        assertThat(state.cancelMainAnimation, equalTo(restoredState!!.cancelMainAnimation))
    }

    @Test
    fun `widgetRemoved is false, after restore widgetRemoved is false`() {
        val state = ChangeWallpaperWidgetState(cancelMainAnimation = false)
        val bundleState = Bundle()

        bundleState.putBundle(KEY_STATE, state.toBundle())
        val restoredState: ChangeWallpaperWidgetState? = ChangeWallpaperWidgetState.fromBundle(bundleState.getBundle(KEY_STATE))

        assertThat(state.widgetRemoved, equalTo(restoredState!!.widgetRemoved))
    }

    @Test
    fun `widgetRemoved is true, after restore widgetRemoved is true`() {
        val state = ChangeWallpaperWidgetState(cancelMainAnimation = true)
        val bundleState = Bundle()

        bundleState.putBundle(KEY_STATE, state.toBundle())
        val restoredState: ChangeWallpaperWidgetState? = ChangeWallpaperWidgetState.fromBundle(bundleState.getBundle(KEY_STATE))

        assertThat(state.widgetRemoved, equalTo(restoredState!!.widgetRemoved))
    }

}
