package ru.yandex.yandexmaps.navi.adapters.test

import android.os.Looper
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.gojuno.koptional.Optional
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.calls
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import io.reactivex.Observable
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode
import org.robolectric.annotation.LooperMode.Mode
import ru.yandex.yandexmaps.navi.adapters.search.api.dependencies.ScreenCallbacksAdapter
import ru.yandex.yandexmaps.navi.adapters.search.internal.CallbacksManager
import ru.yandex.yandexmaps.navi.adapters.search.internal.SearchControllerDeterminer
import ru.yandex.yandexmaps.navi.adapters.search.internal.placecard.PoiPlacemarkManager
import ru.yandex.yandexmaps.navi.adapters.test.env.ActivityForTests
import ru.yandex.yandexmaps.navi.adapters.test.env.StubController

@LooperMode(Mode.PAUSED)
@RunWith(RobolectricTestRunner::class)
class CallbacksManagerTests {

    @get:Rule
    val testActivityScenario = ActivityScenarioRule(ActivityForTests::class.java)

    private lateinit var callbacksAdapter: ScreenCallbacksAdapter
    private lateinit var callbacksManager: CallbacksManager

    @Before
    fun beforeRun() {
        callbacksAdapter = mock()

        val searchControllerDeterminer = mock<SearchControllerDeterminer> {
            val argumentCaptorController = argumentCaptor<StubController>()
            on { isSearchController(argumentCaptorController.capture()) } doAnswer { argumentCaptorController.lastValue.tag == SEARCH_TAG }
        }

        val poiPlacemarkManager = object : PoiPlacemarkManager {
            override val selectedPoi: Observable<Optional<PoiPlacemarkManager.PoiPlacemarkMetadata>> = Observable.empty()
            override fun selectPoi(metadata: PoiPlacemarkManager.PoiPlacemarkMetadata) = Unit
            override fun deselectPoi(metadata: PoiPlacemarkManager.PoiPlacemarkMetadata) = Unit
        }

        callbacksManager = CallbacksManager(callbacksAdapter, searchControllerDeterminer, poiPlacemarkManager)

        testActivityScenario.scenario.onActivity { activity ->
            callbacksManager.bind(activity.mainRouter, activity.placecardRouter)
        }
    }

    @Test
    fun `callbacks are not called with no action`() {
        testActivityScenario.scenario.onActivity {
            verify(callbacksAdapter, never()).onSearchSessionStart()
            verify(callbacksAdapter, never()).onSearchSessionEnd()
            verify(callbacksAdapter, never()).onSearchClosed()
            verify(callbacksAdapter, never()).onPoiCardClosed()
        }
    }

    @Test
    fun `onSearchSessionStart after push search controller`() {
        testActivityScenario.scenario.onActivity { activity ->
            activity.mainRouter.pushWaitAnimation(createSearchController())

            verify(callbacksAdapter, times(1)).onSearchSessionStart()
        }
    }

    @Test
    fun `onSearchSessionEnd and than onSearchClosed after pop search controller`() {
        testActivityScenario.scenario.onActivity { activity ->
            activity.mainRouter.pushWaitAnimation(createSearchController())
            activity.mainRouter.popCurrentWaitAnimation()

            inOrder(callbacksAdapter) {
                verify(callbacksAdapter, calls(1)).onSearchSessionEnd()
                verify(callbacksAdapter, calls(1)).onSearchClosed()
            }
        }
    }

    @Test
    fun `onSearchSessionEnd and than onSearchSessionStart after restart search`() {
        testActivityScenario.scenario.onActivity { activity ->
            activity.mainRouter.pushWaitAnimation(createSearchController())
            activity.mainRouter.replaceTopWaitAnimation(createSearchController())

            inOrder(callbacksAdapter) {
                verify(callbacksAdapter, calls(1)).onSearchSessionStart()
                verify(callbacksAdapter, calls(1)).onSearchSessionEnd()
                verify(callbacksAdapter, calls(1)).onSearchSessionStart()
                verifyNoMoreInteractions()
            }
        }
    }

    @Test
    fun `onPoiCardClosed when controller pop from placecardRouter`() {
        testActivityScenario.scenario.onActivity { activity ->
            activity.placecardRouter.pushWaitAnimation(StubController())
            activity.placecardRouter.popCurrentWaitAnimation()

            verify(callbacksAdapter, times(1)).onPoiCardClosed()
        }
    }

    @Test
    fun `onPoiCardClosed not called when controller changes in placecardRouter`() {
        testActivityScenario.scenario.onActivity { activity ->
            activity.placecardRouter.pushWaitAnimation(StubController())
            activity.placecardRouter.replaceTopWaitAnimation(StubController())

            verify(callbacksAdapter, never()).onPoiCardClosed()
        }
    }

    private fun Router.pushWaitAnimation(controller: Controller) {
        awaitAnimation { pushController(RouterTransaction.with(controller)) }
    }

    private fun Router.replaceTopWaitAnimation(controller: Controller) {
        awaitAnimation { replaceTopController(RouterTransaction.with(controller)) }
    }

    private fun Router.popCurrentWaitAnimation() {
        awaitAnimation { popCurrentController() }
    }

    private fun awaitAnimation(changeControllerBlock: () -> Unit) {
        changeControllerBlock()
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun createSearchController(): StubController {
        return StubController().apply {
            tag = SEARCH_TAG
        }
    }

    companion object {
        private const val SEARCH_TAG = "search_tag"
    }
}
