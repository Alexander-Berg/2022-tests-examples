package ru.yandex.market.clean.presentation.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.clean.presentation.feature.cart.CartParams
import ru.yandex.market.clean.presentation.feature.cart.CartTargetScreen
import ru.yandex.market.clean.presentation.feature.smartshopping.SmartCoinsFragment
import ru.yandex.market.clean.presentation.navigation.FragmentBackStackNavigator
import ru.yandex.market.clean.presentation.navigation.Navigator
import ru.yandex.market.clean.presentation.navigation.RootScreenAdapter
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.clean.presentation.navigation.ScreenPredicate
import ru.yandex.market.clean.presentation.navigation.Tab
import ru.yandex.market.clean.presentation.navigation.command.CommandResult
import ru.yandex.market.clean.presentation.navigation.command.Forward
import ru.yandex.market.feature.manager.FmcgRedesignFeatureManager
import ru.yandex.market.internal.containers.BaseFragment

class TabFragmentNavigatorTest {

    private val transaction = mock<FragmentTransaction> {
        on { add(any<Int>(), any()) } doAnswer { this.mock }
    }
    private val fragmentManager = mock<FragmentManager> {
        on { findFragmentById(any()) } doAnswer { null }
        on { popBackStackImmediate(any<Int>(), any()) } doAnswer { false }
        on { beginTransaction() } doAnswer { transaction }
        on { fragments } doAnswer { emptyList() }
    }
    private val rootScreenAdapter = mock<RootScreenAdapter> {
        on { getRootScreenForTab(any()) } doAnswer { CartTargetScreen(CartParams(false)) }
    }
    private val fragmentBackStackNavigator = mock<FragmentBackStackNavigator> {
        on { createFragment(any(), any()) } doAnswer { mock() }
    }
    private val screenPredicate = mock<ScreenPredicate>()
    private val fmcgRedesignFeatureManager = mock<FmcgRedesignFeatureManager>()

    @Test
    fun `skip forward command if targetTab is not current`() {
        val command = createForward(Screen.CART)

        val result = createNavigator(Tab.MAIN).execute(command)

        assertThat(result).isEqualTo(CommandResult.NOT_EXECUTED)
        verify(fragmentBackStackNavigator, never()).navigateToFragment(any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `handleOnNewArguments if currentFragment supports it`() {
        val targetScreen = Screen.SKU
        val fragment = mock<SmartCoinsFragment>()
        whenever(fragmentManager.findFragmentById(any())).thenReturn(fragment)
        whenever(fragmentBackStackNavigator.handleOnNewArguments(any(), any(), any())).thenReturn(true)
        whenever(fragment.isSupportArgumentsAndScreen(any(), eq(targetScreen))).thenReturn(true)
        val command = createForward(targetScreen)
        val result = createNavigator().execute(command)

        assertThat(result).isEqualTo(CommandResult.COMPLETELY_EXECUTED)
        verify(fragmentBackStackNavigator, never()).navigateToFragment(any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `check that fragment support arguments and screen`() {
        val targetScreen = Screen.SKU
        val fragment = mock<SmartCoinsFragment>()
        whenever(fragmentManager.findFragmentById(any())).thenReturn(fragment)
        whenever(fragment.isSupportArgumentsAndScreen(any(), eq(targetScreen))).thenReturn(false)
        whenever(fragmentBackStackNavigator.navigateToFragment(any(), any(), any(), any(), anyOrNull()))
            .thenReturn(true)
        val command = createForward(targetScreen)
        val result = createNavigator().execute(command)

        assertThat(result).isEqualTo(CommandResult.COMPLETELY_EXECUTED)
        verify(fragment, never()).onNewArguments(any())
    }

    @Test
    fun `if target screen equals to root screen for tab than clear back stack and add root screen`() {
        val fragment = mock<Fragment>()
        val targetFragment = mock<BaseFragment>()
        val targetScreen = Screen.CART
        val command = createForward(targetScreen)
        whenever(rootScreenAdapter.getRootScreenForTab(Tab.CART)).thenReturn(CartTargetScreen(CartParams(false)))
        whenever(fragmentManager.fragments).thenReturn(listOf(fragment))
        whenever(fragmentBackStackNavigator.createFragment(any(), any())).thenReturn(targetFragment)

        val result = createNavigator(Tab.CART).execute(command)

        assertThat(result).isEqualTo(CommandResult.COMPLETELY_EXECUTED)
        verify(fragmentBackStackNavigator).clearBackStack()
        verify(transaction).remove(fragment)
        verify(transaction).add(any(), eq(targetFragment))
        verify(transaction).commitNowAllowingStateLoss()
    }

    private fun createNavigator(tab: Tab = Tab.CART): Navigator {
        return TabFragmentNavigator(
            fragmentManager,
            tab,
            rootScreenAdapter,
            fragmentBackStackNavigator,
            screenPredicate,
            fmcgRedesignFeatureManager,
        )
    }

    private fun createForward(targetScreen: Screen = Screen.CART): Forward {
        return Forward.testInstance()
            .toBuilder()
            .setTargetScreen(targetScreen)
            .build()
    }
}