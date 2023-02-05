package ru.yandex.market.clean.presentation.navigation

import android.os.Build
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.beru.android.R
import ru.yandex.market.clean.presentation.main.TabContainerFragment
import ru.yandex.market.clean.presentation.navigation.command.Back
import ru.yandex.market.clean.presentation.navigation.command.BackTo
import ru.yandex.market.clean.presentation.navigation.command.CommandResult
import ru.yandex.market.clean.presentation.navigation.command.Forward
import ru.yandex.market.clean.presentation.navigation.command.ForwardTab
import ru.yandex.market.clean.presentation.navigation.command.Replace
import ru.yandex.market.ui.view.bottomnavigation.NavigationTabBar
import javax.inject.Provider

class TabsNavigatorTest {

    private val transaction = mock<FragmentTransaction>()
    private val fragmentManager = mock<FragmentManager> {
        on { findFragmentByTag(any()) } doReturn null
        on { beginTransaction() } doReturn transaction
        on { fragments } doReturn emptyList()
    }
    private val tabBar = mock<NavigationTabBar>()
    private val sdkProvider = mock<() -> Int> {
        onGeneric { invoke() } doReturn Build.VERSION_CODES.N
    }
    private val tabFragmentCreator = mock<(Tab) -> TabContainerFragment> {
        onGeneric { invoke(any()) } doReturn mock()
    }

    private val selectedTabListener = mock<(tabId: Int) -> Unit> {
        onGeneric { invoke(any()) } doReturn mock()
    }

    private val onTabChangeListener = mock<(tab: Tab) -> Unit> {
        onGeneric { invoke(any()) } doReturn mock()
    }

    private val navigator = TabsNavigator(
        fragmentManager = fragmentManager,
        tabBarProvider = Provider { tabBar },
        sdkProvider = sdkProvider,
        tabFragmentFactory = tabFragmentCreator,
        actualizedSelectedTab = selectedTabListener,
        onTabChangeListener = onTabChangeListener,
    )

    @Test
    fun `forward just returns false if targetTab is null`() {
        val result = navigator.execute(createForward(null))

        assertThat(result).isEqualTo(CommandResult.NOT_EXECUTED)
    }

    @Test
    fun `forward change tab and returns PARTIALLY_EXECUTED`() {
        val result = navigator.execute(createForward(Tab.CART))

        assertThat(result).isEqualTo(CommandResult.PARTIALLY_EXECUTED)
        verify(tabBar).selectedItem = R.id.nav_cart
        verify(fragmentManager).beginTransaction()
        verify(transaction).add(any(), any<Fragment>(), any())
        verify(transaction).commit()
    }

    @Test
    fun `forward main tab`() {
        val result = navigator.execute(ForwardTab.create(Tab.MAIN))

        assertThat(result).isEqualTo(CommandResult.COMPLETELY_EXECUTED)
        verify(tabBar).selectedItem = R.id.nav_main
    }

    @Test
    fun `forward catalog tab`() {
        val result = navigator.execute(ForwardTab.create(Tab.CATALOG))

        assertThat(result).isEqualTo(CommandResult.COMPLETELY_EXECUTED)
        verify(tabBar).selectedItem = R.id.nav_catalog
    }

    @Test
    fun `forward discounts tab`() {
        val result = navigator.execute(ForwardTab.create(Tab.DISCOUNTS))

        assertThat(result).isEqualTo(CommandResult.COMPLETELY_EXECUTED)
        verify(tabBar).selectedItem = R.id.nav_discounts
    }

    @Test
    fun `forward cart tab`() {
        val result = navigator.execute(ForwardTab.create(Tab.CART))

        assertThat(result).isEqualTo(CommandResult.COMPLETELY_EXECUTED)
        verify(tabBar).selectedItem = R.id.nav_cart
    }

    @Test
    fun `forward profile tab`() {
        val result = navigator.execute(ForwardTab.create(Tab.PROFILE))

        assertThat(result).isEqualTo(CommandResult.COMPLETELY_EXECUTED)
        verify(tabBar).selectedItem = R.id.nav_profile
    }

    @Test
    fun `search old fragment`() {
        val fragment = mock<TabContainerFragment>()
        whenever(fragmentManager.findFragmentByTag(Tab.PROFILE.name)).doReturn(fragment)
        whenever(fragment.host).doReturn(null)

        val result = navigator.execute(ForwardTab.create(Tab.PROFILE))

        assertThat(result).isEqualTo(CommandResult.COMPLETELY_EXECUTED)
        verify(transaction).add(any(), argThat<Fragment> { this === fragment }, any())
    }

    @Test
    fun `detach all current fragments`() {
        val fragment1 = mock<TabContainerFragment>()
        val fragment2 = mock<TabContainerFragment>()
        whenever(fragmentManager.fragments).doReturn(listOf(fragment1, fragment2))
        whenever(fragment1.childFragmentManager).doReturn(fragmentManager)
        whenever(fragment2.childFragmentManager).doReturn(fragmentManager)

        val result = navigator.execute(ForwardTab.create(Tab.PROFILE))

        assertThat(result).isEqualTo(CommandResult.COMPLETELY_EXECUTED)
        verify(transaction).detach(fragment1)
        verify(transaction).detach(fragment2)
    }

    @Test
    fun `attach fragment if it has already attached`() {
        val host = Any()
        val fragment = mock<TabContainerFragment>()
        whenever(fragmentManager.findFragmentByTag(Tab.PROFILE.name)).doReturn(fragment)
        whenever(fragment.host).doReturn(host)

        val result = navigator.execute(ForwardTab.create(Tab.PROFILE))

        assertThat(result).isEqualTo(CommandResult.COMPLETELY_EXECUTED)
        verify(transaction).attach(fragment)
    }

    @Test
    fun `allow reordering if sdk more or equals than nougat`() {
        whenever(sdkProvider.invoke()).thenReturn(Build.VERSION_CODES.N)

        val result = navigator.execute(ForwardTab.create(Tab.PROFILE))

        assertThat(result).isEqualTo(CommandResult.COMPLETELY_EXECUTED)
        verify(transaction).setReorderingAllowed(true)
    }

    @Test
    fun `not allow reordering if sdk less than nougat`() {
        whenever(sdkProvider.invoke()).thenReturn(Build.VERSION_CODES.M)

        val result = navigator.execute(ForwardTab.create(Tab.PROFILE))

        assertThat(result).isEqualTo(CommandResult.COMPLETELY_EXECUTED)
        verify(transaction, never()).setReorderingAllowed(any())
    }

    @Test
    fun `another commands return false`() {
        val backResult = navigator.execute(Back.create(null))
        val replaceResult = navigator.execute(Replace.testInstance())
        val backToResult = navigator.execute(BackTo.testInstance())

        assertThat(backResult).isEqualTo(CommandResult.NOT_EXECUTED)
        assertThat(replaceResult).isEqualTo(CommandResult.NOT_EXECUTED)
        assertThat(backToResult).isEqualTo(CommandResult.NOT_EXECUTED)
    }


    private fun createForward(tab: Tab? = Tab.CART) = Forward.testInstance().toBuilder().setTargetTab(tab).build()

}
