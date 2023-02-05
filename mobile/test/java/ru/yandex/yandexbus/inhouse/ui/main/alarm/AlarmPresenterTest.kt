package ru.yandex.yandexbus.inhouse.ui.main.alarm

import com.yandex.mapkit.geometry.Point
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.MockitoAnnotations
import ru.yandex.yandexbus.inhouse.activity.BusActivity
import ru.yandex.yandexbus.inhouse.model.route.RouteModel.RouteStop
import ru.yandex.yandexbus.inhouse.service.alarm.GuidanceAlarmController
import ru.yandex.yandexbus.inhouse.service.alarm.GuidanceAlarmController.StopType
import ru.yandex.yandexbus.inhouse.service.alarm.GuidanceAlarmController.StopUpdateEvent
import ru.yandex.yandexbus.inhouse.service.system.RequestDispatcher
import ru.yandex.yandexbus.inhouse.whenever
import rx.Single
import rx.subjects.BehaviorSubject


class AlarmPresenterTest {

    companion object {
        private const val STOP_ONE = "stop_one"
        private const val STOP_TWO = "stop_two"
    }

    @Mock
    private lateinit var navigator: AlarmNavigator

    @Mock
    private lateinit var guidanceAlarmController: GuidanceAlarmController

    @Mock
    private lateinit var view: BusActivity

    private lateinit var isWorkingSubject: BehaviorSubject<Boolean>
    private lateinit var stopUpdatesSubject: BehaviorSubject<StopUpdateEvent>

    private lateinit var presenter: AlarmPresenter

    @Before
    fun setUp() {

        isWorkingSubject = BehaviorSubject.create(false)
        stopUpdatesSubject = BehaviorSubject.create()

        MockitoAnnotations.initMocks(this)

        whenever(navigator.showExitSoonDialog(anyString())).thenReturn(
                Single.just(RequestDispatcher.Response(0, null)))
        whenever(navigator.showExitNowDialog(anyString())).thenReturn(
                Single.just(RequestDispatcher.Response(0, null)))

        whenever(guidanceAlarmController.isWorkingUpdates()).thenReturn(isWorkingSubject)
        whenever(guidanceAlarmController.stopsUpdates()).thenReturn(stopUpdatesSubject)

        presenter = AlarmPresenter(navigator, guidanceAlarmController)
        presenter.onCreate()
        presenter.onAttach(view)

        enableAlarm()
    }

    @Test
    fun doNothingUntilViewStarted() {
        stopReached(STOP_ONE, StopType.BEFORE_LAST_IN_SECTION)
        stopReached(STOP_TWO, StopType.LAST_IN_SECTION)

        verifyNoMoreInteractions(navigator)
    }

    @Test
    fun doNothingWhenViewStopped() {
        with(presenter) {
            onViewStart()
            onViewStop()
        }

        stopReached(STOP_ONE, StopType.BEFORE_LAST_IN_SECTION)
        stopReached(STOP_TWO, StopType.LAST_IN_SECTION)

        verifyNoMoreInteractions(navigator)
    }

    @Test
    fun showProperDialogs() {
        presenter.onViewStart()
        verifyNoMoreInteractions(navigator)

        stopReached(STOP_ONE, StopType.REGULAR)
        verifyNoMoreInteractions(navigator)

        stopReached(STOP_ONE, StopType.BEFORE_LAST_IN_SECTION)
        verify<AlarmNavigator>(navigator).showExitSoonDialog(STOP_ONE)

        stopReached(STOP_ONE, StopType.LAST_IN_SECTION)
        verify<AlarmNavigator>(navigator).showExitNowDialog(STOP_ONE)

        stopReached(STOP_TWO, StopType.LAST_IN_ROUTE)
        verify<AlarmNavigator>(navigator).showExitNowDialog(STOP_TWO)

        verifyNoMoreInteractions(navigator)
    }

    private fun enableAlarm() = isWorkingSubject.onNext(true)

    private fun stopReached(stopName: String, type: StopType) {
        stopUpdatesSubject.onNext(StopUpdateEvent(type, RouteStop(stopName, stopName, Point(.0, .0))))
    }
}