package com.android.launcher3

import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.yandex.launcher.BaseRobolectricTest
import com.yandex.launcher.alice.AliceLauncher
import com.yandex.launcher.alice.TestAliceLauncherProxyActivity
import com.yandex.launcher.di.ApplicationComponent
import org.junit.Test
import org.mockito.kotlin.*

class TryToStartAliceDirectlyIfNeededTest: BaseRobolectricTest() {

    private val expectedSuffix = TestAliceLauncherProxyActivity::class.java.simpleName

    private val aliceLauncher = mock<AliceLauncher>()
    private val applicationComponent = mock<ApplicationComponent> {
        on { aliceLauncher } doReturn aliceLauncher
    }
    private lateinit var launcher: Launcher

    override fun setUp() {
        super.setUp()
        launcher = spy(Launcher())
        doAnswer { ApplicationProvider.getApplicationContext<Context>() }.whenever(launcher).applicationContext
        whenever(launcher.applicationComponent).doReturn(applicationComponent)
    }

    @Test
    fun `component name is null, alice not started`() {
        launcher.tryToStartAliceDirectlyIfNeeded(null)

        verifyNoInteractions(aliceLauncher)
    }

    @Test
    fun `component name is empty, alice not started`() {
        launcher.tryToStartAliceDirectlyIfNeeded(ComponentName("", ""))

        verifyNoInteractions(aliceLauncher)
    }

    @Test
    fun `component name has unknown class name, alice not started`() {
        launcher.tryToStartAliceDirectlyIfNeeded(ComponentName("", "com.yandex.unknown.SomeClass"))

        verifyNoInteractions(aliceLauncher)
    }

    @Test
    fun `component name not ends with expected suffix`() {
        launcher.tryToStartAliceDirectlyIfNeeded(ComponentName("", "${expectedSuffix}extra"))

        verifyNoInteractions(aliceLauncher)
    }

    @Test
    fun `component name ends with expected suffix, alice started`() {
        launcher.tryToStartAliceDirectlyIfNeeded(ComponentName("", "com.yandex.launcher.$expectedSuffix"))

        verify(aliceLauncher).launchAlice(anyOrNull())
    }
}
