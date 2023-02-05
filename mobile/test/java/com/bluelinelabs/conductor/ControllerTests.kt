package com.bluelinelabs.conductor

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.bluelinelabs.conductor.utils.ROOT_ID
import com.bluelinelabs.conductor.utils.TestActivity
import com.bluelinelabs.conductor.utils.TestController
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class ControllerTests {

    @get:Rule
    val testActivityRule = ActivityScenarioRule(TestActivity::class.java)

    @Test
    fun `No NPE if pop in postCreateView`() {
        testActivityRule.scenario.onActivity { activity ->
            val router = Conductor.attachRouter(activity, activity.findViewById(ROOT_ID), null)

            val controller = TestController()
            controller.addLifecycleListener(object : Controller.LifecycleListener() {
                override fun postCreateView(controller: Controller, view: View, savedViewState: Bundle?) {
                    super.postCreateView(controller, view, savedViewState)
                    router.popCurrentController()
                }
            })
            router.setRoot(RouterTransaction.with(controller))
        }
    }

    @Test
    fun `onRestoreViewState called after postCreateView`() {
        testActivityRule.scenario.onActivity { activity ->

            val router = Conductor.attachRouter(activity, activity.findViewById(ROOT_ID), null)

            val controller = TestController()
            router.setRoot(RouterTransaction.with(controller))
            router.pushController(RouterTransaction.with(TestController()))

            var postCreateViewCalled = false
            var onRestoreViewStateCalled = false
            controller.addLifecycleListener(object : Controller.LifecycleListener() {
                override fun postCreateView(controller: Controller, view: View, savedViewState: Bundle?) {
                    super.postCreateView(controller, view, savedViewState)
                    postCreateViewCalled = true
                    Assert.assertFalse(onRestoreViewStateCalled)
                }

                override fun onRestoreViewState(controller: Controller, savedViewState: Bundle) {
                    super.onRestoreViewState(controller, savedViewState)
                    onRestoreViewStateCalled = true
                    Assert.assertTrue(postCreateViewCalled)
                }
            })
            router.popCurrentController()
        }
    }

    @Test
    fun `empty router after recreation if empty bundle passed`() {

        testActivityRule.scenario
            .onActivity { activity ->
                val router = Conductor.attachRouter(activity, activity.findViewById(ROOT_ID), null)
                val controller = TestController()
                router.setRoot(RouterTransaction.with(controller))
            }
            .recreate()
            .onActivity { activity ->
                val router = Conductor.attachRouter(activity, activity.findViewById(ROOT_ID), null)
                Assert.assertTrue("Backstack should be empty, if you do not restore your state", router.backstack.isEmpty)
            }
    }

    @Test
    fun `restore router after recreation if bundle passed`() {

        testActivityRule.scenario
            .onActivity { activity ->
                val router = Conductor.attachRouter(activity, activity.findViewById(ROOT_ID), null)
                val controller = TestController()
                router.setRoot(RouterTransaction.with(controller))
            }
            .recreate()
            .onActivity { activity ->
                val router = Conductor.attachRouter(activity, activity.findViewById(ROOT_ID), activity.bundle)
                Assert.assertTrue("Controller in backstack should be of TestController type", router.backstack.peek()?.controller is TestController)
            }
    }

    @Test
    fun `instance of controller should be different after restore state`() {

        var controllerBeforeRecreation: Controller? = null

        testActivityRule.scenario
            .onActivity { activity ->
                val router = Conductor.attachRouter(activity, activity.findViewById(ROOT_ID), null)
                controllerBeforeRecreation = TestController()
                router.setRoot(RouterTransaction.with(controllerBeforeRecreation!!))
            }
            .recreate()
            .onActivity { activity ->
                val router = Conductor.attachRouter(activity, activity.findViewById(ROOT_ID), activity.bundle)
                val controllerAfterRecreation = router.backstack.peek()?.controller
                Assert.assertTrue("Controller in backstack should not be same instance as before", controllerBeforeRecreation != null && controllerAfterRecreation != null && controllerAfterRecreation != controllerBeforeRecreation)
            }
    }
}
