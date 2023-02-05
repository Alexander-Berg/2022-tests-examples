package ru.yandex.metro.scheme.presentation.overlay

import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import ru.yandex.metro.ClassSpek
import ru.yandex.metro.scheme.domain.overlay.EmergencyAlertState
import ru.yandex.metro.scheme.domain.overlay.SchemeShadeOverlayController
import ru.yandex.metro.scheme.domain.overlay.ToastStateRepo
import ru.yandex.metro.scheme.domain.overlay.TopOverlayState
import ru.yandex.metro.scheme.domain.overlay.TopOverlayStateRepo
import ru.yandex.metro.scheme.domain.overlay.UpdateBarState
import ru.yandex.metro.utils.android.rx.CustomRxPlugins
import ru.yandex.metro.utils.android.statusbar.StatusBarController

class SchemeOverlayPresenterSpec : ClassSpek(SchemeOverlayPresenter::class.java, {
    RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
    CustomRxPlugins.setImmediateMainThreadSchedulerHandler(Function { Schedulers.trampoline() })

    val view by memoized {
        mock<SchemeOverlayView> {
            on { goToPreferencesIntent() } doReturn Observable.never<Unit>()
            on { toastClicks() } doReturn Observable.never()
        }
    }

    val statusBarController by memoized {
        mock<StatusBarController>()
    }

    val schemeShadeController by memoized {
        mock<SchemeShadeOverlayController> {
            on { alphaValues() } doReturn Flowable.empty()
        }
    }

    val toastStateRepo by memoized {
        mock<ToastStateRepo> {
            on { changes() } doReturn Observable.create { emitter -> emitter.onNext(ToastState.None) }
        }
    }
    val orderTaxiForCurrentSelectedStation by memoized {
        mock<OrderTaxiForCurrentRouteUseCase> {
            on { request() } doReturn Completable.complete()
        }
    }

    val navigator by memoized { mock<SchemeOverlayNavigator>() }
    val initialStateFactory by memoized { InitialStateFactory(savedState = null) }

    val testCases = mapOf(
            TopOverlayState(EmergencyAlertState.ABSENT, UpdateBarState.ABSENT) to TopViewState.ABSENT,
            TopOverlayState(EmergencyAlertState.ABSENT, UpdateBarState.COLLAPSED) to TopViewState.UPDATE_BAR_COLLAPSED,
            TopOverlayState(EmergencyAlertState.ABSENT, UpdateBarState.EXPANDED) to TopViewState.UPDATE_BAR_EXPANDED,

            TopOverlayState(EmergencyAlertState.COLLAPSED, UpdateBarState.ABSENT) to TopViewState.EMERGENCY_COLLAPSED,
            TopOverlayState(EmergencyAlertState.COLLAPSED, UpdateBarState.COLLAPSED) to TopViewState.UPDATE_BAR_COLLAPSED,
            TopOverlayState(EmergencyAlertState.COLLAPSED, UpdateBarState.EXPANDED) to TopViewState.UPDATE_BAR_EXPANDED,

            TopOverlayState(EmergencyAlertState.EXPANDED, UpdateBarState.ABSENT) to TopViewState.EMERGENCY_EXPANDED,
            TopOverlayState(EmergencyAlertState.EXPANDED, UpdateBarState.COLLAPSED) to TopViewState.EMERGENCY_EXPANDED,
            TopOverlayState(EmergencyAlertState.EXPANDED, UpdateBarState.EXPANDED) to TopViewState.EMERGENCY_EXPANDED
    )

    for (testCase in testCases) {
        val topOverlayState = testCase.key
        context("presenter gets notification $topOverlayState") {
            val topOverlayRepo = mock<TopOverlayStateRepo> {
                on { state() } doReturn Observable.just(topOverlayState)
            }

            val presenter by memoized {
                SchemeOverlayPresenter(
                        statusBarController,
                        schemeShadeController,
                        topOverlayRepo,
                        toastStateRepo,
                        navigator,
                        orderTaxiForCurrentSelectedStation,
                        initialStateFactory
                )
            }

            val expected = testCase.value

            it("should send state with $expected to view") {
                presenter.attachView(view)
                verify(view).render(argWhere { state -> state.topViewState == expected })
            }
        }
    }
})
