package com.yandex.launcher.alice

import org.mockito.kotlin.*
import com.yandex.launcher.common.util.UniNotifier
import com.yandex.launcher.BaseRobolectricTest
import com.yandex.launcher.search.voice.VoiceSearchStatisticsEnvironment
import com.yandex.launcher.statistics.Statistics
import com.yandex.launcher.statistics.StoryManager
import com.yandex.launcher.statistics.StoryManagerShadow
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.*
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowPackageManager
import org.robolectric.util.ReflectionHelpers

@Config(shadows = [StoryManagerShadow::class])
class AliceLauncherTest: BaseRobolectricTest() {

    private val blocker = mock<AliceBlocker>()
    private val aliceActivity = TestAliceDialogActivity()
    private val shadowAliceActivity = shadowOf(aliceActivity)
    private val statistics = ReflectionHelpers.callConstructor(StoryManager::class.java)

    private lateinit var launcher: AliceLauncher

    private lateinit var shadowPackageManager: ShadowPackageManager

    @Before
    override fun setUp() {
        super.setUp()
        ReflectionHelpers.setStaticField(Statistics::class.java, "instance", statistics)
        launcher = spy(AliceLauncher(appContext))
        launcher.registerBlocker(blocker)
        shadowPackageManager = shadowOf(appContext.packageManager)
    }

    @After
    override fun tearDown() {
        super.tearDown()
        ReflectionHelpers.setStaticField(Statistics::class.java, "instance", null)
    }

    @Test
    fun `blocker released after launch`() {
        verifyNoInteractions(blocker)

        launcher.launchAlice(mock())

        verify(blocker).release()
    }

    @Test
    fun `spotter initialized, alice launched first, then spotter stopped`() {
        launcher.launchAlice(mock())
        val inOrder = inOrder(launcher, blocker)

        inOrder.verify(launcher).launchAliceImpl()
        inOrder.verify(blocker).release()
    }

    @Test
    fun `report performed to metrica after alice launched`() {
        launcher.launchAlice(mock())
        val inOrder = inOrder(launcher, blocker)

        inOrder.verify(launcher).launchAliceImpl()
        inOrder.verify(launcher).reportAliceOpened(any())
    }

    @Test
    fun `to metrica sent report, passed to launchAlice`() {
        val environment = mock<VoiceSearchStatisticsEnvironment>()
        val launcher = spy(AliceLauncher(appContext))

        launcher.reportAliceOpened(environment)

        verify(launcher).reportAliceOpened(environment)
    }

    @Test
    fun `on launch, AliceDialogActivity started`() {
        Assume.assumeThat(isAliceActivityStarted(), `is`(false))

        launcher.launchAlice(mock())

        assertThat(isAliceActivityStarted(), `is`(true))
    }

    @Test
    fun `on register, blocker added to list`() {
        val launcher = spy(AliceLauncher(appContext))
        Assume.assumeThat(launcher.getBlockerList().hasListeners(), `is`(false))

        launcher.registerBlocker(blocker)

        assertThat(launcher.getBlockerList().singleOrNull(), `is`(blocker))
    }

    @Test
    fun `on unregister, blocker removed from list`() {
        Assume.assumeThat(launcher.getBlockerList().singleOrNull(), `is`(blocker))

        launcher.unRegisterBlocker(blocker)

        assertThat(launcher.getBlockerList().hasListeners(), `is`(false))
    }

    private fun isAliceActivityStarted(): Boolean = shadowAliceActivity.peekNextStartedActivity() != null

}

fun AliceLauncher.getBlockerList(): UniNotifier<AliceBlocker> = ReflectionHelpers.getField(this, "blockers")
