package ru.yandex.yandexmaps.common.conductor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import io.reactivex.Observable
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DisposeBesidesConfigurationChangeTest {

    @Test
    fun `Second controller in backstack should not subscribe second time after back`() {
        // Not using [ActivityScenarioRule] because it waits for view attachment and we don't want that
        val activity = Robolectric.buildActivity(ConfigurationChangeAwareTestActivity::class.java).run {
            create()
            get()
        }

        var subscriptionCount = 0

        val testObservable = Observable.create<Unit> { /* Infinite observable */ }
            .doOnSubscribe { ++subscriptionCount }
            .doOnDispose { --subscriptionCount }

        activity.router.push(TestController(testObservable))
        activity.router.push(DummyController())
        activity.triggerConfigurationChange()
        activity.router.handleBack()
        Assert.assertEquals(1, subscriptionCount)
    }

    private class TestController(private val testObservable: Observable<Unit>) : BaseController(), ControllerDisposer by ControllerDisposer.create() {

        @Suppress("UNREACHABLE_CODE")
        constructor() : this(error("This constructor should not be called"))

        init {
            initControllerDisposer()
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
            disposeWithViewBesidesConfigurationChange {
                testObservable.subscribe()
            }

            return View(inflater.context)
        }

        override fun performInjection() = Unit
    }

    private class DummyController : Controller() {

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
            return View(inflater.context)
        }
    }
}
