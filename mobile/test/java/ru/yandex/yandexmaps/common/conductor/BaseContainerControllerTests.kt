package ru.yandex.yandexmaps.common.conductor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.bluelinelabs.conductor.Controller
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.yandex.yandexmaps.common.utils.extensions.getValue
import ru.yandex.yandexmaps.common.utils.extensions.setValue

@RunWith(RobolectricTestRunner::class)
class BaseContainerControllerTests {

    @get:Rule
    val testActivityRule = ActivityScenarioRule(TestActivity::class.java)

    @Test
    fun `BaseContainerController removed from backstack if guest controller not handled back`() {
        testActivityRule.scenario.onActivity { activity ->

            val containerController = TestContainerController.create().also {
                it.innerController = GuestControllerNotHandledBack()
            }
            activity.router.push(containerController)

            activity.router.handleBack()

            assert(activity.router.backstackSize == 0) { "Router should be empty" }
        }
    }

    @Test
    fun `BaseContainerController don't removed from backstack if guest controller not handled back`() {

        testActivityRule.scenario.onActivity { activity ->

            val guestControllerHandledBack = GuestControllerHandledBack()
            val containerController = TestContainerController.create().also {
                it.innerController = guestControllerHandledBack
            }
            activity.router.push(containerController)

            activity.router.handleBack()

            assert(activity.router.getCurrentController() == containerController) { "Router should contain containerController" }
            assert((activity.router.getCurrentController() as? TestContainerController)?.innerController == guestControllerHandledBack) { "ContainerController should contain certain guestController" }
        }
    }

    @Test
    fun `BaseContainerController don't create new guest controller after restoring state`() {
        val guestControllerHandledBack = GuestControllerHandledBack()
        val containerController = TestContainerController.create().also {
            it.innerController = guestControllerHandledBack
        }
        testActivityRule.scenario
            .onActivity { activity ->
                activity.router.push(containerController)
            }
            .recreate()
            .onActivity { activity ->
                val restoredContainerController = activity.router.getCurrentController() as TestContainerController
                assert(restoredContainerController.creationsCount == 1) { "CreateGuestController should be called once" }
                assert(restoredContainerController.checkGuestController?.javaClass?.isInstance(guestControllerHandledBack) == true) { "GuestController should be preserved" }
            }
    }
}

private class TestContainerController private constructor() : BaseContainerController() {

    companion object {
        fun create(): TestContainerController {
            return TestContainerController(Unit)
        }
    }

    var creationsCount: Int by args

    val checkGuestController: Controller?
        get() = guestController

    constructor(crateByHand: Unit) : this() {
        print("we should use $crateByHand")
        creationsCount = 0
    }

    lateinit var innerController: Controller

    override fun createGuestController(): Controller {
        creationsCount++
        return innerController
    }

    override fun performInjection() = Unit
}

private class GuestControllerNotHandledBack : Controller() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        return View(inflater.context)
    }

    override fun handleBack(): Boolean {
        return false
    }
}

private class GuestControllerHandledBack : Controller() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        return View(inflater.context)
    }

    override fun handleBack(): Boolean {
        return true
    }
}
