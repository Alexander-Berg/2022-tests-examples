package ru.yandex.yandexmaps.common.conductor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.isNotNull
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import kotlin.reflect.KClass

@RunWith(RobolectricTestRunner::class)
class ConductorBackstackMatcherTest {
    @Test
    fun `test add controller to empty stack`() {
        val router = routerOf()
        router.matchAndAssert(listOf(of(Test1Controller::class)))
        verify(router, times(1)).pushController(any())
    }

    @Test
    fun `test add controllers to empty stack`() {
        val router = routerOf()
        router.matchAndAssert(
            listOf(
                of(Test1Controller::class),
                of(Test2Controller::class),
            )
        )
        verify(router, times(2)).pushController(any())
    }

    @Test
    fun `test add controller`() {
        val router = routerOf(of(Test1Controller::class))
        router.matchAndAssert(
            listOf(
                of(Test1Controller::class),
                of(Test2Controller::class),
            )
        )
        verify(router, times(1)).pushController(any())
    }

    @Test
    fun `test add two controllers`() {
        val router = routerOf(of(Test1Controller::class))
        router.matchAndAssert(
            listOf(
                of(Test1Controller::class),
                of(Test2Controller::class),
                of(TestUniqueController::class, 1),
            )
        )
        verify(router, times(2)).pushController(any())
    }

    @Test
    fun `test remove controller`() {
        val router = routerOf(
            of(Test1Controller::class),
            of(Test2Controller::class),
            of(TestUniqueController::class, 1),
        )
        router.matchAndAssert(
            listOf(
                of(Test1Controller::class),
                of(Test2Controller::class),
            )
        )
        verify(router, times(1)).popCurrentController()
    }

    @Test
    fun `test remove two controllers`() {
        val router = routerOf(
            of(Test1Controller::class),
            of(Test2Controller::class),
            of(TestUniqueController::class, 1),
        )
        router.matchAndAssert(
            listOf(
                of(Test1Controller::class),
            )
        )
        verify(router, times(2)).popCurrentController()
    }

    @Test
    fun `test remove controller at bottom`() {
        val router = routerOf(
            of(Test1Controller::class),
            of(TestUniqueController::class, 1),
            of(TestUniqueController::class, 2),
        )
        router.matchAndAssert(
            listOf(
                of(TestUniqueController::class, 1),
                of(TestUniqueController::class, 2),
            )
        )
        verify(router, times(1)).popController(any())
    }

    @Test
    fun `test remove two controllers at bottom`() {
        var router = routerOf(
            of(Test1Controller::class),
            of(Test2Controller::class),
            of(TestUniqueController::class, 2),
        )
        router.matchAndAssert(
            listOf(
                of(TestUniqueController::class, 2),
            )
        )
        verify(router, times(2)).popController(any())

        router = routerOf(
            of(TestUniqueController::class, 1),
            of(TestUniqueController::class, 2),
            of(TestUniqueController::class, 3),
        )
        router.matchAndAssert(
            listOf(
                of(TestUniqueController::class, 3),
            )
        )
        verify(router, times(2)).popController(any())
    }

    @Test
    fun `test remove controller at bottom and add at top`() {
        val router = routerOf(
            of(TestUniqueController::class, 1),
            of(TestUniqueController::class, 2),
        )
        router.matchAndAssert(
            listOf(
                of(TestUniqueController::class, 2),
                of(TestUniqueController::class, 3),
            )
        )
        verify(router, times(1)).popController(any())
        verify(router, times(1)).pushController(any())
    }

    @Test
    fun `test remove controllers at top and bottom`() {
        val router = routerOf(
            of(TestUniqueController::class, 1),
            of(TestUniqueController::class, 2),
            of(TestUniqueController::class, 3),
            of(TestUniqueController::class, 4),
        )
        router.matchAndAssert(
            listOf(
                of(TestUniqueController::class, 3),
            )
        )
        verify(router, times(2)).popController(any())
        verify(router, times(1)).popCurrentController()
        verify(router, times(0)).pushController(any())
    }

    @Test
    fun `test replace top controller`() {
        val router = routerOf(
            of(Test1Controller::class),
            of(Test2Controller::class),
            of(TestUniqueController::class, 1),
        )
        router.matchAndAssert(
            listOf(
                of(Test1Controller::class),
                of(Test2Controller::class),
                of(TestUniqueController::class, 2),
            )
        )
        verify(router, times(0)).popController(any())
        verify(router, times(0)).popCurrentController()
        verify(router, times(1)).setBackstack(any(), any())
    }

    @Test
    fun `test clear stack with single controller`() {
        val router = routerOf(
            of(Test1Controller::class),
        )
        router.matchAndAssert(emptyList())
        verify(router, times(0)).popController(any())
        verify(router, times(1)).popCurrentController()
        verify(router, times(0)).setBackstack(any(), any())
    }

    @Test
    fun `test clear stack`() {
        val router = routerOf(
            of(Test1Controller::class),
            of(TestUniqueController::class, 2),
        )
        router.matchAndAssert(emptyList())
        verify(router, times(0)).popController(any())
        verify(router, times(2)).popCurrentController()
        verify(router, times(0)).setBackstack(any(), any())
    }

    @Test
    fun `test match empty stacks`() {
        val router = routerOf()
        router.matchAndAssert(emptyList())
        verify(router, times(0)).popController(any())
        verify(router, times(0)).popCurrentController()
        verify(router, times(0)).setBackstack(any(), any())
    }
}

private class Test1Controller : Controller() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        TODO("Not yet implemented")
    }

    override fun toString() = this::class.java.simpleName
}

private class Test2Controller : Controller() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        TODO("Not yet implemented")
    }

    override fun toString() = this::class.java.simpleName
}

internal class TestUniqueController : Controller(), UniqueController by UniqueController() {
    init {
        initHostController()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        TODO("Not yet implemented")
    }

    override fun toString() = "${this::class.java.simpleName}:$uniqueControllerId"
}

private fun of(c: KClass<out Controller>) = ClassControllerProvider(c)

private fun <C> of(c: KClass<C>, id: Long) where C : Controller, C : UniqueController =
    UniqueClassControllerProvider(c, id)

private fun Router.matchAndAssert(expected: List<ControllerProvider>) {
    matchWithExternalBackstackWithProviders(expected, { null }, { null })
    assertEquals("Check backstack size", expected.size, backstack.size)
    backstack.forEachIndexed { index, controller ->
        assert(expected[index].match(controller.controller())) {
            "Expected ${expected[index]} on position $index, but found ${controller.controller()}"
        }
    }
}

private fun routerOf(vararg initialStack: ControllerProvider): Router {
    val impl = RouterMock(*initialStack)
    val m = mock(Router::class.java)
    whenever(m.getBackstack()).thenAnswer { impl.backstack }
    whenever(m.backstackSize).thenAnswer { impl.backstackSize }
    whenever(m.popCurrentController()).thenAnswer { impl.popCurrentController() }
    whenever(m.popController(any())).thenAnswer { impl.popController(it.getArgument(0)) }
    whenever(m.pushController(any())).thenAnswer { impl.pushController(it.getArgument(0)) }
    whenever(m.setBackstack(isNotNull(), isNull())).thenAnswer { impl.setBackstack(it.getArgument(0)) }
    return m
}

private class RouterMock(
    vararg initialStack: ControllerProvider,
) {
    var stack = initialStack.asSequence().map { it.createController() }.toMutableList()

    val backstackSize: Int
        get() = stack.size
    val backstack: List<RouterTransaction>
        get() = stack.map { RouterTransaction.with(it) }

    fun popCurrentController(): Boolean {
        stack.removeLast()
        return true
    }

    fun popController(controller: Controller): Boolean {
        return stack.remove(controller)
    }

    fun pushController(controller: RouterTransaction) {
        stack.add(controller.controller())
    }

    fun setBackstack(backstack: List<RouterTransaction>) {
        stack = backstack.map { it.controller() }.toMutableList()
    }
}
