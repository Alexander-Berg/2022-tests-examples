package com.yandex.launcher.wallpapers

import android.app.ActivityManager
import android.content.ComponentName
import android.os.Build
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.android.launcher3.Launcher
import com.android.launcher3.ShortcutInfo
import com.android.launcher3.Workspace
import com.yandex.launcher.BaseRobolectricTest
import com.yandex.launcher.app.GlobalAppState
import com.yandex.launcher.app.TestApplication
import com.yandex.launcher.badges.BadgeManager
import com.yandex.launcher.preferences.IPreferenceProvider
import com.yandex.launcher.preferences.Preference
import com.yandex.launcher.preferences.PreferencesManager
import com.yandex.launcher.util.BitUtils
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.*
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.util.ReflectionHelpers

class WallpaperExperimentHelperTest: BaseRobolectricTest() {

    private val engine: AutoChangeWallpaperEngine = mock()
    private val prefProvider: IPreferenceProvider = mock()
    private val mockedWorkspace: Workspace = mock()
    private val launcher: Launcher = mock {
        on { workspace } doReturn mockedWorkspace
        on { applicationContext } doReturn ApplicationProvider.getApplicationContext<TestApplication>()
    }
    private val wallpaperIconShortcutInfo: ShortcutInfo = mock {
        on { targetComponent } doReturn ComponentName("com.yandex.launcher", WallpaperProxyActivity::class.java.name)
    }
    private val nullIcon: View? = null
    private val mockedExistingIcon: View = mock {
        on { tag } doReturn wallpaperIconShortcutInfo
    }
    private val badgeManager: BadgeManager = mock()

    @Before
    override fun setUp() {
        super.setUp()
        val globalAppState: GlobalAppState = mock {
            on { autoChangeWallpaperEngine } doReturn engine
            on { badgeManager } doReturn this@WallpaperExperimentHelperTest.badgeManager
        }
        PreferencesManager.provider = prefProvider
        ReflectionHelpers.setStaticField(GlobalAppState::class.java, "instance", globalAppState)
    }

    @Ignore("Temporary ignore test until 4.3.2 robolectric")
    @Test
    @Config(minSdk = Build.VERSION_CODES.O_MR1, shadows = [ShadowLowRamActivityManager::class])
    fun `if GO device - widget disabled`() {
        assertThat(WallpaperExperimentHelper.isChangeWallpaperWidgetEnabled(), `is`(false))
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.O, shadows = [ShadowNonLowRamActivityManager::class])
    fun `if not GO device - widget enabled`() {
        assertThat(WallpaperExperimentHelper.isChangeWallpaperWidgetEnabled(), `is`(true))
    }

    @Test
    fun `promote wallpaper icon on update, icon not on desktop, icon not promoted`() {
        verifyNoInteractions(badgeManager)
        whenever(mockedWorkspace.findViewInContainers(any())).doReturn(nullIcon)    // null means icon not found on desktop

        WallpaperExperimentHelper.promoteWallpaperShortcutIfPresented(
            launcher,
            true /* this means promotion attempt happened on update */
        )

        verifyNoInteractions(badgeManager)
    }

    @Test
    fun `promote wallpaper icon on update, icon on desktop, icon wasn't launched before, icon badged`() {
        verifyNoInteractions(badgeManager)
        whenever(mockedWorkspace.findViewInContainers(any())).doReturn(mockedExistingIcon)

        WallpaperExperimentHelper.promoteWallpaperShortcutIfPresented(
            launcher,
            true /* this means promotion attempt happened on update */
        )

        verify(badgeManager, times(1)).setBadgeTypeBits(
            any(),
            eq(WallpaperProxyActivity::class.java.name),
            any(),
            eq(BadgeManager.BadgeType.STAR),
            eq(true)
        )
    }

    @Test
    fun `promote wallpaper icon on update, icon on desktop, icon was launched before, icon not badged`() {
        verifyNoInteractions(badgeManager)
        whenever(mockedWorkspace.findViewInContainers(any())).doReturn(mockedExistingIcon)
        `when`(prefProvider.getInt(eq(Preference.WALLPAPER_SHORTCUT_PROMOTION_STATE_MASK))).thenReturn(
            BitUtils.setBits(
                0,
                WallpaperExperimentHelper.WallpaperIconPromoFlag.FLAG_PROMOTED_WALLPAPER_ICON_LAUNCHED,
                true
            )
        )

        WallpaperExperimentHelper.promoteWallpaperShortcutIfPresented(
            launcher,
            true /* this means promotion attempt happened on update */
        )

        verifyNoInteractions(badgeManager)
    }

    @Test
    fun `promote wallpaper icon on general launch, icon was not promoted, icon not badged`() {
        verifyNoInteractions(badgeManager)
        whenever(mockedWorkspace.findViewInContainers(any())).doReturn(mockedExistingIcon)

        WallpaperExperimentHelper.promoteWallpaperShortcutIfPresented(
            launcher,
            false /* this means promotion attempt happened on update */
        )

        verifyNoInteractions(badgeManager)
    }

    @Test
    fun `promote wallpaper icon on general launch, icon was promoted, icon badged`() {
        verifyNoInteractions(badgeManager)
        whenever(mockedWorkspace.findViewInContainers(any())).doReturn(mockedExistingIcon)
        `when`(prefProvider.getInt(eq(Preference.WALLPAPER_SHORTCUT_PROMOTION_STATE_MASK))).thenReturn(
            BitUtils.setBits(
                0,
                WallpaperExperimentHelper.WallpaperIconPromoFlag.FLAG_WALLPAPER_ICON_PROMOTED,
                true
            )
        )

        WallpaperExperimentHelper.promoteWallpaperShortcutIfPresented(
            launcher,
            false /* this means promotion attempt happened on update */
        )

        verify(badgeManager, times(1)).setBadgeTypeBits(
            any(),
            eq(WallpaperProxyActivity::class.java.name),
            any(),
            eq(BadgeManager.BadgeType.STAR),
            eq(true)
        )
    }

    @Test
    fun `stop promoting icon, icon marked as launched and promoted`() {
        verifyNoInteractions(prefProvider)

        WallpaperExperimentHelper.stopPromotingWallpaperShortcutIfNecessary(launcher)

        verify(prefProvider, times(1)).put(
            eq(Preference.WALLPAPER_SHORTCUT_PROMOTION_STATE_MASK), eq(
                BitUtils.setBits(
                    0,
                    WallpaperExperimentHelper.WallpaperIconPromoFlag.FLAG_PROMOTED_WALLPAPER_ICON_LAUNCHED or WallpaperExperimentHelper.WallpaperIconPromoFlag.FLAG_WALLPAPER_ICON_PROMOTED,
                    true
                )
            )
        )
    }

    @Test
    fun `stop promoting icon, icon not badged`() {
        verifyNoInteractions(badgeManager)

        WallpaperExperimentHelper.stopPromotingWallpaperShortcutIfNecessary(launcher)

        verify(badgeManager, times(1)).setBadgeTypeBits(
            any(),
            eq(WallpaperProxyActivity::class.java.name),
            any(),
            eq(BadgeManager.BadgeType.STAR),
            eq(false)
        )
    }

    @Implements(ActivityManager::class)
    class ShadowLowRamActivityManager {
        @Implementation fun isLowRamDevice() = true
    }

    @Implements(ActivityManager::class)
    class ShadowNonLowRamActivityManager {
        @Implementation fun isLowRamDevice() = false
    }
}
