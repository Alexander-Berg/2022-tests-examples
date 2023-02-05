package ru.yandex.market.clean.presentation.navigation

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.beru.android.R
import ru.yandex.market.clean.presentation.feature.tabs.TabsFragment
import ru.yandex.market.clean.presentation.navigation.command.Back
import ru.yandex.market.clean.presentation.navigation.command.BackTo
import ru.yandex.market.clean.presentation.navigation.command.CommandResult
import ru.yandex.market.clean.presentation.navigation.command.Forward
import ru.yandex.market.clean.presentation.navigation.command.ForwardTab
import ru.yandex.market.clean.presentation.navigation.command.Replace

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class MainNavigatorTest {

    private val activity = mock<AppCompatActivity>()
    private val activityIntentsFactory = mock<ActivityIntentsFactory>()
    private val fragmentManager = mock<FragmentManager>()
    private val backgroundReplacer = mock<MainNavigator.BackgroundController>()

    private val navigator = MainNavigator(
        activity = activity,
        activityIntentsFactory = activityIntentsFactory,
        fragmentManager = fragmentManager,
        backgroundController = backgroundReplacer
    )

    @Test
    fun `returns NOT_EXECUTED when backTo because it is not implemented`() {
        val result = navigator.execute(BackTo.testInstance())

        assertThat(result).isEqualTo(CommandResult.NOT_EXECUTED)
    }

    @Test
    fun `startActivity for some screens`() {
        val targetScreen = Screen.DIALER
        whenever(activityIntentsFactory.getIntent(any(), eq(targetScreen), any())) doReturn mock()
        val command = Forward.testInstance()
            .toBuilder()
            .setTargetScreen(targetScreen)
            .build()

        val result = navigator.execute(command)

        assertThat(result).isEqualTo(CommandResult.COMPLETELY_EXECUTED)
        verify(activityIntentsFactory, times(1)).getIntent(any(), any(), any())
    }

    @Test
    fun `forwardTab opens TabsFragment and returns PARTIALLY_EXECUTED`() {
        val transaction = mock<FragmentTransaction>()
        whenever(fragmentManager.fragments).thenReturn(emptyList())
        whenever(fragmentManager.beginTransaction()).thenReturn(transaction)
        val result = navigator.execute(ForwardTab.create(Tab.CART))

        assertThat(result).isEqualTo(CommandResult.PARTIALLY_EXECUTED)
        verify(fragmentManager, times(2)).beginTransaction()
        verify(transaction).add(any<Int>(), argThat { this is TabsFragment })
        verify(transaction).commitAllowingStateLoss()
    }

    @Test
    fun `forwardTab only returns PARTIALLY_EXECUTED if TabsFragment has already opened`() {
        val transaction = mock<FragmentTransaction>()
        whenever(fragmentManager.beginTransaction()).thenReturn(transaction)
        navigator.execute(ForwardTab.create(Tab.CART))
        val result = navigator.execute(ForwardTab.create(Tab.CART))

        assertThat(result).isEqualTo(CommandResult.PARTIALLY_EXECUTED)
        verify(fragmentManager, times(2)).beginTransaction()
    }

    @Test
    fun `forwardTab opens TabsFragment if some Fragment added`() {
        val transaction = mock<FragmentTransaction>()
        val addedFragment = mock<Fragment>()
        whenever(fragmentManager.fragments).thenReturn(listOf(addedFragment))
        whenever(fragmentManager.beginTransaction()).thenReturn(transaction)
        val result = navigator.execute(ForwardTab.create(Tab.CART))

        assertThat(result).isEqualTo(CommandResult.PARTIALLY_EXECUTED)
        verify(fragmentManager, times(2)).beginTransaction()
        verify(transaction).remove(addedFragment)
        verify(transaction).add(any(), argThat<TabsFragment> { this is TabsFragment })
        verify(transaction).commitAllowingStateLoss()
    }

    @Test
    fun `back finish activity if back stack is empty`() {
        whenever(fragmentManager.backStackEntryCount).thenReturn(0)

        val result = navigator.execute(Back.create(null))

        assertThat(result).isEqualTo(CommandResult.COMPLETELY_EXECUTED)
        verify(fragmentManager, never()).beginTransaction()
        verify(activity).finish()
    }

    @Test
    fun `back pop back stack if it is not empty`() {
        whenever(fragmentManager.backStackEntryCount).thenReturn(1)

        val result = navigator.execute(Back.create(null))

        assertThat(result).isEqualTo(CommandResult.COMPLETELY_EXECUTED)
        verify(fragmentManager).popBackStack()
        verify(activity, never()).finish()
    }

    @Test
    fun `replace pop back stack and navigate new fragment`() {
        val transaction = mock<FragmentTransaction>()
        val tabsFragment = mock<Fragment>()
        whenever(fragmentManager.fragments).thenReturn(listOf(tabsFragment))
        whenever(fragmentManager.beginTransaction()).thenReturn(transaction)
        whenever(fragmentManager.backStackEntryCount).thenReturn(1).thenReturn(0)

        val result = navigator.execute(Replace.testInstance())

        assertThat(result).isEqualTo(CommandResult.PARTIALLY_EXECUTED)
        verify(fragmentManager).popBackStack()
        verify(fragmentManager, times(2)).beginTransaction()
        verify(backgroundReplacer).replaceWindowBackground(R.color.white)
        verify(transaction).add(any(), argThat<TabsFragment> { this is TabsFragment })
        verify(transaction).commitAllowingStateLoss()
    }

    @Test
    fun `replace only open new fragment if back stack is empty (not close activity)`() {
        val transaction = mock<FragmentTransaction>()
        whenever(fragmentManager.fragments).thenReturn(emptyList())
        whenever(fragmentManager.beginTransaction()).thenReturn(transaction)
        whenever(fragmentManager.backStackEntryCount).thenReturn(0)

        val result = navigator.execute(Replace.testInstance())

        assertThat(result).isEqualTo(CommandResult.PARTIALLY_EXECUTED)
        verify(backgroundReplacer).replaceWindowBackground(R.color.white)
        verify(activity, never()).finish()
        verify(fragmentManager, times(2)).beginTransaction()
        verify(transaction).add(any<Int>(), argThat { this is TabsFragment })
        verify(transaction).commitAllowingStateLoss()
    }

    @Test
    fun `replace not open new fragment if it already opened`() {
        val transaction = mock<FragmentTransaction>()
        whenever(fragmentManager.beginTransaction()).thenReturn(transaction)
        whenever(fragmentManager.backStackEntryCount).thenReturn(1)

        navigator.execute(Replace.testInstance())
        val result = navigator.execute(Replace.testInstance())

        assertThat(result).isEqualTo(CommandResult.PARTIALLY_EXECUTED)
        verify(fragmentManager, times(2)).beginTransaction()
    }

    @Test
    fun `forward just opens new fragment`() {
        val transaction = mock<FragmentTransaction>()
        whenever(fragmentManager.fragments).thenReturn(emptyList())
        whenever(fragmentManager.beginTransaction()).thenReturn(transaction)
        whenever(fragmentManager.backStackEntryCount).thenReturn(0)

        val result = navigator.execute(Forward.testInstance())

        assertThat(result).isEqualTo(CommandResult.PARTIALLY_EXECUTED)
        verify(fragmentManager, times(2)).beginTransaction()
        verify(backgroundReplacer).replaceWindowBackground(R.color.white)
        verify(transaction).add(any<Int>(), argThat { this is TabsFragment })
        verify(transaction).commitAllowingStateLoss()
    }

    @Test
    fun `forward not open new fragment if it already opened`() {
        val transaction = mock<FragmentTransaction>()
        whenever(fragmentManager.beginTransaction()).thenReturn(transaction)
        whenever(fragmentManager.backStackEntryCount).thenReturn(1)

        navigator.execute(Forward.testInstance())
        val result = navigator.execute(Forward.testInstance())

        assertThat(result).isEqualTo(CommandResult.PARTIALLY_EXECUTED)
        verify(fragmentManager, times(2)).beginTransaction()
    }
}