package ru.yandex.market.clean.presentation.navigation

import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.junit.Test
import org.mockito.Mockito
import ru.yandex.market.analitycs.AnalyticsUniqueIdHolder
import ru.yandex.market.clean.presentation.navigation.command.Back
import ru.yandex.market.clean.presentation.navigation.command.Command
import ru.yandex.market.clean.presentation.navigation.command.CommandResult
import ru.yandex.market.clean.presentation.navigation.command.Forward
import ru.yandex.market.clean.presentation.navigation.command.Replace

class NavigationDispatcherTest {

    private val analyticsUniqueIdHolder = mock<AnalyticsUniqueIdHolder>()

    private val navigationAnalytics = mock<NavigationAnalytics>()

    private val navigationDispatcher = NavigationDispatcher(analyticsUniqueIdHolder, navigationAnalytics)

    @Test
    fun `check navigators order`() {
        val firstNavigator = spy(TestNavigator())
        val secondNavigator = spy(TestNavigator(CommandResult.COMPLETELY_EXECUTED))
        val command = anyCommand()

        navigationDispatcher.setNavigator("key1", firstNavigator)
        navigationDispatcher.setNavigator("key2", secondNavigator)
        navigationDispatcher.dispatch(command)

        verify(secondNavigator).execute(command)
        verify(firstNavigator, never()).execute(command)
    }

    @Test
    fun `check remove navigator by key`() {
        val navigator = spy(TestNavigator())
        val key = "key"
        val command = anyCommand()

        navigationDispatcher.setNavigator(key, navigator)
        navigationDispatcher.removeNavigator(key)
        navigationDispatcher.dispatch(command)

        verify(navigator, never()).execute(command)
    }

    @Test
    fun `dispatch calls refreshUniqueId`() {
        val command = anyCommand()

        navigationDispatcher.dispatch(command)

        verify(analyticsUniqueIdHolder).refreshUniqueId()
    }

    @Test
    fun `dispatch put command in queue`() {
        val command = anyCommand()
        val navigator = spy(TestNavigator())

        navigationDispatcher.dispatch(command)
        navigationDispatcher.setNavigator("key", navigator)

        verify(navigator).execute(command)
    }

    @Test
    fun `addResultListener add listener`() {
        val targetScreen = Screen.PROFILE
        val result = "result"
        val listener = mock<ResultListener>()

        navigationDispatcher.addResultListener(Screen.CART, targetScreen, listener)
        navigationDispatcher.notifyOnResult(targetScreen, result)

        verify(listener).onResult(result)
    }

    @Test
    fun `removeResultListeners remove listener by source screen`() {
        val sourceScreen = Screen.CART
        val targetScreen = Screen.PROFILE
        val result = "result"
        val listener = mock<ResultListener>()

        navigationDispatcher.addResultListener(sourceScreen, targetScreen, listener)
        navigationDispatcher.removeResultListeners(sourceScreen)
        navigationDispatcher.notifyOnResult(targetScreen, result)

        verify(listener, never()).onResult(result)
    }

    @Test
    fun `notifyOnResult call all listeners`() {
        val targetScreen = Screen.PROFILE
        val result = "result"
        val listener1 = mock<ResultListener>()
        val listener2 = mock<ResultListener>()

        navigationDispatcher.addResultListener(Screen.CART, targetScreen, listener1)
        navigationDispatcher.addResultListener(Screen.HOME, targetScreen, listener2)
        navigationDispatcher.notifyOnResult(targetScreen, result)

        verify(listener1).onResult(result)
        verify(listener2).onResult(result)
    }

    @Test
    fun `execute all commands after setting navigator`() {
        val command1 = Forward.testInstance()
        val command2 = Replace.testInstance()
        val navigator = spy(TestNavigator(CommandResult.COMPLETELY_EXECUTED))

        navigationDispatcher.dispatch(command1)
        navigationDispatcher.dispatch(command2)
        navigationDispatcher.setNavigator("key", navigator)

        val order = Mockito.inOrder(navigator)
        order.verify(navigator).execute(command1)
        order.verify(navigator).execute(command2)
    }

    @Test
    fun `execute stop if navigator cannot execute command`() {
        val command1 = Forward.testInstance()
        val command2 = Replace.testInstance()
        val navigator = spy(TestNavigator(CommandResult.NOT_EXECUTED))

        navigationDispatcher.dispatch(command1)
        navigationDispatcher.dispatch(command2)
        navigationDispatcher.setNavigator("key", navigator)

        val order = Mockito.inOrder(navigator)
        order.verify(navigator).execute(command1)
        order.verify(navigator, never()).execute(command2)
    }

    @Test
    fun `execute command on all navigators`() {
        val command1 = anyCommand()
        val firstNavigator = spy(TestNavigator(CommandResult.NOT_EXECUTED))
        val secondNavigator = spy(TestNavigator(CommandResult.NOT_EXECUTED))

        navigationDispatcher.setNavigator("key", firstNavigator)
        navigationDispatcher.setNavigator("key1", secondNavigator)
        navigationDispatcher.dispatch(command1)

        val order = Mockito.inOrder(firstNavigator, secondNavigator)
        order.verify(secondNavigator).execute(command1)
        order.verify(firstNavigator).execute(command1)
    }

    @Test
    fun `replace command execute on all navigators`() {
        val command1 = Replace.testInstance()
        val badNavigator = spy(TestNavigator(CommandResult.NOT_EXECUTED))
        val goodNavigator = spy(TestNavigator(CommandResult.COMPLETELY_EXECUTED))

        navigationDispatcher.setNavigator("key", badNavigator)
        navigationDispatcher.setNavigator("key1", goodNavigator)
        navigationDispatcher.dispatch(command1)

        val order = Mockito.inOrder(badNavigator, goodNavigator)
        order.verify(goodNavigator).execute(command1)
        order.verify(badNavigator).execute(command1)
    }

    @Test
    fun `replace try to execute Back if any navigator hasn't executed one`() {
        val command1 = Replace.testInstance()
        val firstNavigator =
            spy(TestNavigator(result = CommandResult.NOT_EXECUTED, replaceResult = CommandResult.NOT_EXECUTED))
        val secondNavigator =
            spy(TestNavigator(result = CommandResult.NOT_EXECUTED, replaceResult = CommandResult.NOT_EXECUTED))

        navigationDispatcher.setNavigator("key", firstNavigator)
        navigationDispatcher.setNavigator("key1", secondNavigator)
        navigationDispatcher.dispatch(command1)

        val order = Mockito.inOrder(firstNavigator, secondNavigator)
        order.verify(secondNavigator).execute(command1)
        order.verify(firstNavigator).execute(command1)
        order.verify(secondNavigator).execute(argThat { this is Back })
        order.verify(firstNavigator).execute(argThat { this is Back })
    }

    @Test
    fun `if back hasn't executed by any navigator then replace does not transform`() {
        val command1 = Replace.testInstance()
        val firstNavigator =
            spy(TestNavigator(result = CommandResult.NOT_EXECUTED, replaceResult = CommandResult.NOT_EXECUTED))
        val secondNavigator =
            spy(TestNavigator(result = CommandResult.NOT_EXECUTED, replaceResult = CommandResult.NOT_EXECUTED))

        navigationDispatcher.setNavigator("key", firstNavigator)
        navigationDispatcher.setNavigator("key1", secondNavigator)
        navigationDispatcher.dispatch(command1)

        val order = Mockito.inOrder(firstNavigator, secondNavigator)
        order.verify(secondNavigator).execute(command1)
        order.verify(firstNavigator).execute(command1)
        order.verify(secondNavigator).execute(argThat { this is Back })
        order.verify(firstNavigator).execute(argThat { this is Back })

        val newNavigator =
            spy(TestNavigator(result = CommandResult.NOT_EXECUTED, replaceResult = CommandResult.NOT_EXECUTED))
        navigationDispatcher.setNavigator("key2", newNavigator)

        verify(newNavigator).execute(argThat { this is Replace })
    }

    @Test
    fun `if some navigator executed back then replace transform to forward and add to first positions of commands`() {
        val replaceCommand = Replace.testInstance()
        val navigator =
            spy(TestNavigator(result = CommandResult.COMPLETELY_EXECUTED, replaceResult = CommandResult.NOT_EXECUTED))

        navigationDispatcher.dispatch(replaceCommand)
        navigationDispatcher.dispatch(anyCommand())
        navigationDispatcher.dispatch(anyCommand())
        navigationDispatcher.setNavigator("key", navigator)

        val order = Mockito.inOrder(navigator)
        order.verify(navigator).execute(replaceCommand)
        order.verify(navigator).execute(argThat { this is Back })
        order.verify(navigator).execute(argThat { this is Forward })
    }

    private fun anyCommand() = Back.create(null)

    class TestNavigator(
        val result: CommandResult = CommandResult.NOT_EXECUTED,
        private val replaceResult: CommandResult = CommandResult.NOT_EXECUTED
    ) : Navigator {

        override fun execute(command: Command): CommandResult {
            return when (command) {
                is Replace -> replaceResult
                else -> result
            }
        }
    }
}