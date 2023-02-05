package com.yandex.launcher.widget.wallpaper

import android.os.Bundle
import android.os.Looper
import androidx.test.platform.app.InstrumentationRegistry
import com.android.launcher3.CellInfo
import com.android.launcher3.Launcher
import org.mockito.kotlin.*
import com.yandex.launcher.BaseRobolectricTest
import com.yandex.launcher.common.permissions.Permissions
import com.yandex.launcher.wallpapers.WallpaperMetadata
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assume
import org.junit.Test
import org.robolectric.Shadows
import org.robolectric.util.ReflectionHelpers

class ChangeWallpaperControllerTest: BaseRobolectricTest() {

    private val permissions: Permissions = mock()
    private lateinit var launcher: Launcher
    private lateinit var loadWallpaperDelegate: LoadWallpaperDelegate
    private lateinit var controller: ChangeWallpaperController

    override fun setUp() {
        super.setUp()
        launcher = mock {
            on { applicationContext } doReturn InstrumentationRegistry.getInstrumentation().context
        }
        loadWallpaperDelegate = mock()
        ReflectionHelpers.setStaticField(Permissions.Access::class.java, "instance", permissions)
        controller = spy(ChangeWallpaperController(launcher, loadWallpaperDelegate))
    }

    @Test
    fun `save state to null bundle, no crash`() {
        controller.saveState(null)
    }

    @Test
    fun `save state non null bundle, state saved`() {
        val bundle = Bundle()
        Assume.assumeThat(bundle.getBundle(ChangeWallpaperController.KEY_ACTIVE_WIDGET_STATE), nullValue())

        controller.saveState(bundle)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(bundle.getBundle(ChangeWallpaperController.KEY_ACTIVE_WIDGET_STATE), notNullValue())
    }

    @Test
    fun `save state non null bundle, wrote state equal to origin state`() {
        val bundle = Bundle()
        Assume.assumeThat(bundle.getBundle(ChangeWallpaperController.KEY_ACTIVE_WIDGET_STATE), nullValue())

        controller.state = createDummyWidgetState().apply {
            restoredAfterRotationChange = false
            needApplyState = false
        }
        controller.saveState(bundle)
        val wroteState = ChangeWallpaperWidgetState.fromBundle(
            bundle.getBundle(ChangeWallpaperController.KEY_ACTIVE_WIDGET_STATE))

        assertThat(wroteState, equalTo(controller.state))
    }

    @Test
    fun `restore state from null bundle, state not changed`() {
        val state = controller.state
        Assume.assumeThat(state, notNullValue())

        controller.restoreState(null)

        assertThat(controller.state === state, equalTo(true))
    }

    @Test
    fun `restore state from null bundle, applyRestoredState not called`() {
        verifyNoInteractions(controller)

        controller.restoreState(null)

        verify(controller, times(0)).applyRestoredState()
    }

    @Test
    fun `restore state from empty bundle, state not changed`() {
        val state = controller.state
        Assume.assumeThat(state, notNullValue())

        controller.restoreState(Bundle())

        assertThat(controller.state === state, equalTo(true))
    }

    @Test
    fun `restore state from empty bundle, applyRestoredState not called`() {
        verifyNoInteractions(controller)

        controller.restoreState(Bundle())

        verify(controller, times(0)).applyRestoredState()
    }

    @Test
    fun `restore state from bundle with correct bundle, state changed`() {
        val state = controller.state
        Assume.assumeThat(state, notNullValue())

        val newCorrectState: ChangeWallpaperWidgetState = createDummyWidgetState()
        val bundle = Bundle()
        bundle.putParcelable(ChangeWallpaperController.KEY_ACTIVE_WIDGET_STATE, newCorrectState.toBundle())
        controller.restoreState(bundle)

        assertThat(controller.state !== state, equalTo(true))
    }

    @Test
    fun `restore state from bundle with correct bundle, state equal to correct state`() {
        val state = controller.state
        Assume.assumeThat(state, notNullValue())

        val newCorrectState: ChangeWallpaperWidgetState = createDummyWidgetState()
        val bundle = Bundle()
        bundle.putParcelable(ChangeWallpaperController.KEY_ACTIVE_WIDGET_STATE, newCorrectState.toBundle())
        controller.restoreState(bundle)

        assertThat(controller.state == newCorrectState, equalTo(true))
    }

    @Test
    fun `restore state from bundle with correct bundle, applyRestoredState called`() {
        verifyNoInteractions(controller)

        val newCorrectState: ChangeWallpaperWidgetState = createDummyWidgetState()
        val bundle = Bundle()
        bundle.putParcelable(ChangeWallpaperController.KEY_ACTIVE_WIDGET_STATE, newCorrectState.toBundle())
        controller.restoreState(bundle)

        verify(controller).applyRestoredState()
    }

    private fun createDummyWidgetState() = ChangeWallpaperWidgetState(
        ChangeWallpaperWidgetStage.STARTING,
        WallpaperMetadata("some_id", "some_url", "some_coll_id", "Some title", null),
        false,
        -1L,
        activeWidgetPosition = CellInfo(1L, 2, 3),
        cancelMainAnimation = false,
        dataLoaded = false,
        widgetRemoved = false,
        activityDestroyed = false,
        forceStopAnimation = false,
        restoredAfterRotationChange = true,
        needApplyState = true
    )
}
