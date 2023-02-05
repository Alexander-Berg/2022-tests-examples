package ru.yandex.market.clean.presentation.navigation

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.presentation.navigation.command.Back
import ru.yandex.market.clean.presentation.navigation.command.Command
import ru.yandex.market.clean.presentation.navigation.command.CommandResult
import ru.yandex.market.clean.presentation.navigation.command.Forward
import ru.yandex.market.clean.presentation.navigation.command.ForwardTab
import ru.yandex.market.clean.presentation.navigation.command.Replace

class TabsRouterTest {

    private val navigationDispatcher = mock<NavigationDispatcher>()

    private val tabsRouter = TabsRouter(navigationDispatcher)

    @Test
    fun `check remove navigator from navigationDispatcher`() {
        tabsRouter.removeNavigator("key")

        verify(navigationDispatcher).removeNavigator(any())
    }

    @Test
    fun `check set changed navigator in navigationDispatcher`() {
        val navigator = spy(TestNavigator())
        tabsRouter.setNavigator("key", navigator)

        verify(navigationDispatcher).setNavigator(any(), argThat { this != navigator })
    }

    @Test
    fun `check back command when stack size less than 1`() {
        val navigator = spy(TestNavigator())
        var changedNavigator: Navigator? = null
        val forwardTabCommand = ForwardTab.create(Tab.CART)
        whenever(navigationDispatcher.setNavigator(any(), any())) doAnswer {
            changedNavigator = it.arguments[1] as Navigator
            Unit
        }

        tabsRouter.setNavigator("key", navigator)

        changedNavigator!!.execute(forwardTabCommand)
        checkStackHasLessItemsThanOne(changedNavigator, navigator)
    }

    @Test
    fun `check back command when stack size more than 1 and stack clear after execute`() {
        val navigator = spy(TestNavigator())
        var changedNavigator: Navigator? = null
        val backCommand = Back.create(null)
        val firstTabCommand = ForwardTab.create(Tab.CART)
        val secondTabCommand = ForwardTab.create(Tab.PROFILE)
        whenever(navigationDispatcher.setNavigator(any(), any())) doAnswer {
            changedNavigator = it.arguments[1] as Navigator
            Unit
        }

        tabsRouter.setNavigator("key", navigator)
        changedNavigator!!.execute(firstTabCommand)
        changedNavigator!!.execute(secondTabCommand)
        changedNavigator!!.execute(backCommand)
        changedNavigator!!.execute(backCommand)

        val order = Mockito.inOrder(navigator)
        order.verify(navigator).execute(firstTabCommand)
        order.verify(navigator).execute(secondTabCommand)
        order.verify(navigator).execute(firstTabCommand)
        order.verify(navigator).execute(backCommand)
    }

    @Test
    fun `no execute command if stack not empty and tab from command on top`() {
        val navigator = spy(TestNavigator())
        var changedNavigator: Navigator? = null
        val forwardTabCommand = ForwardTab.create(Tab.CART)
        whenever(navigationDispatcher.setNavigator(any(), any())) doAnswer {
            changedNavigator = it.arguments[1] as Navigator
            Unit
        }

        tabsRouter.setNavigator("key", navigator)
        changedNavigator!!.execute(forwardTabCommand)
        changedNavigator!!.execute(forwardTabCommand)

        verify(navigator).execute(forwardTabCommand)
        checkStackHasLessItemsThanOne(changedNavigator, navigator)
    }

    @Test
    fun `check that stack clear after navigation to Main tab`() {
        val navigator = spy(TestNavigator())
        var changedNavigator: Navigator? = null
        val firstTabCommand = ForwardTab.create(Tab.CART)
        val secondTabCommand = ForwardTab.create(Tab.MAIN)
        whenever(navigationDispatcher.setNavigator(any(), any())) doAnswer {
            changedNavigator = it.arguments[1] as Navigator
            Unit
        }

        tabsRouter.setNavigator("key", navigator)
        changedNavigator!!.execute(firstTabCommand)
        changedNavigator!!.execute(secondTabCommand)

        checkStackHasLessItemsThanOne(changedNavigator, navigator)
    }

    @Test
    fun `check that new command remove from stack and put it on top`() {
        val navigator = spy(TestNavigator())
        var changedNavigator: Navigator? = null
        val backCommand = Back.create(null)
        val firstTabCommand = ForwardTab.create(Tab.CART)
        val secondTabCommand = ForwardTab.create(Tab.PROFILE)
        whenever(navigationDispatcher.setNavigator(any(), any())) doAnswer {
            changedNavigator = it.arguments[1] as Navigator
            Unit
        }

        tabsRouter.setNavigator("key", navigator)
        changedNavigator!!.execute(firstTabCommand)
        changedNavigator!!.execute(secondTabCommand)
        changedNavigator!!.execute(firstTabCommand)
        changedNavigator!!.execute(backCommand)
        changedNavigator!!.execute(backCommand)

        val order = Mockito.inOrder(navigator)
        order.verify(navigator).execute(firstTabCommand)
        order.verify(navigator).execute(secondTabCommand)
        order.verify(navigator).execute(firstTabCommand)
        order.verify(navigator).execute(secondTabCommand)
        order.verify(navigator).execute(backCommand)
    }

    @Test
    fun `check that navigator execute not back and not forwardTab command`() {
        val navigator = spy(TestNavigator(CommandResult.COMPLETELY_EXECUTED))
        var changedNavigator: Navigator? = null
        val someCommand = Replace.testInstance()
        whenever(navigationDispatcher.setNavigator(any(), any())) doAnswer {
            changedNavigator = it.arguments[1] as Navigator
            Unit
        }

        tabsRouter.setNavigator("key", navigator)
        val result = changedNavigator!!.execute(someCommand)

        assertThat(result).isEqualTo(CommandResult.COMPLETELY_EXECUTED)
        verify(navigator).execute(someCommand)
    }

    @Test
    fun `check that return false if Forward has null tab`() {
        val navigator = spy(TestNavigator(CommandResult.COMPLETELY_EXECUTED))
        var changedNavigator: Navigator? = null
        whenever(navigationDispatcher.setNavigator(any(), any())) doAnswer {
            changedNavigator = it.arguments[1] as Navigator
            Unit
        }

        tabsRouter.setNavigator("key", navigator)

        val someCommand = Forward.testInstance()
            .toBuilder()
            .setTargetTab(null)
            .build()
        val result = changedNavigator!!.execute(someCommand)

        verify(navigator, never()).execute(someCommand)
        assertThat(result).isEqualTo(CommandResult.NOT_EXECUTED)
    }

    @Test
    fun `check that command tab put in stack if stack is empty`() {
        val navigator = spy(TestNavigator(CommandResult.COMPLETELY_EXECUTED))
        var changedNavigator: Navigator? = null
        val someCommand = createForward(Tab.CART)
        whenever(navigationDispatcher.setNavigator(any(), any())) doAnswer {
            changedNavigator = it.arguments[1] as Navigator
            Unit
        }

        tabsRouter.setNavigator("key", navigator)
        val result = changedNavigator!!.execute(someCommand)
        changedNavigator!!.execute(ForwardTab.create(Tab.PROFILE))
        changedNavigator!!.execute(Back.create(null))

        assertThat(result).isEqualTo(CommandResult.COMPLETELY_EXECUTED)
        verify(navigator).execute(someCommand)
        verify(navigator).execute(ForwardTab.create(Tab.CART))
        checkStackHasLessItemsThanOne(changedNavigator, navigator)
    }

    @Test
    fun `check that command tab didn't put in stack if this tab is on top of stack`() {
        val navigator = spy(TestNavigator(CommandResult.COMPLETELY_EXECUTED))
        var changedNavigator: Navigator? = null
        whenever(navigationDispatcher.setNavigator(any(), any())) doAnswer {
            changedNavigator = it.arguments[1] as Navigator
            Unit
        }

        tabsRouter.setNavigator("key", navigator)

        val someCommand = createForward(Tab.CART)
        val result = changedNavigator!!.execute(someCommand)
        changedNavigator!!.execute(createForward(Tab.CART))

        assertThat(result).isEqualTo(CommandResult.COMPLETELY_EXECUTED)
        verify(navigator).execute(someCommand)
        checkStackHasLessItemsThanOne(changedNavigator, navigator)
    }

    @Test
    fun `forwardTab return false if it first opening screen`() {
        val navigator = spy(TestNavigator(CommandResult.COMPLETELY_EXECUTED))
        var changedNavigator: Navigator? = null
        val forwardTab = ForwardTab.create(Tab.CART)
        whenever(navigationDispatcher.setNavigator(any(), any())) doAnswer {
            changedNavigator = it.arguments[1] as Navigator
            Unit
        }

        tabsRouter.setNavigator("key", navigator)
        val result = changedNavigator!!.execute(forwardTab)

        assertThat(result).isEqualTo(CommandResult.PARTIALLY_EXECUTED)
        verify(navigator).execute(forwardTab)
    }

    @Test
    fun `forwardTab just returns COMPLETELY_EXECUTED if it has already opened`() {
        val navigator = spy(TestNavigator(CommandResult.COMPLETELY_EXECUTED))
        var changedNavigator: Navigator? = null
        val forwardTab = ForwardTab.create(Tab.CART)
        whenever(navigationDispatcher.setNavigator(any(), any())) doAnswer {
            changedNavigator = it.arguments[1] as Navigator
            Unit
        }

        tabsRouter.setNavigator("key", navigator)
        val result = changedNavigator!!.execute(forwardTab)
        val secondResult = changedNavigator!!.execute(forwardTab)

        assertThat(result).isEqualTo(CommandResult.PARTIALLY_EXECUTED)
        assertThat(secondResult).isEqualTo(CommandResult.PARTIALLY_EXECUTED)
        verify(navigator).execute(forwardTab)
    }

    private fun createForward(tab: Tab) = Forward.testInstance().toBuilder().setTargetTab(tab).build()

    private fun checkStackHasLessItemsThanOne(
        changedNavigator: Navigator?,
        navigator: TestNavigator
    ) {
        val backCommand = Back.create(null)
        changedNavigator!!.execute(backCommand)
        verify(navigator).execute(backCommand)
    }

    class TestNavigator(val result: CommandResult = CommandResult.NOT_EXECUTED) : Navigator {

        override fun execute(command: Command): CommandResult = result
    }
}