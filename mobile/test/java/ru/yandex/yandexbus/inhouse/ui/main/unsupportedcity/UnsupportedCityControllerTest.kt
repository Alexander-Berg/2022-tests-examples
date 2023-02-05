package ru.yandex.yandexbus.inhouse.ui.main.unsupportedcity

import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.service.system.RequestDispatcher
import ru.yandex.yandexbus.inhouse.ui.main.unsupportedcity.UnsupportedCityDetector.CityInfo
import ru.yandex.yandexbus.inhouse.whenever
import rx.Single
import rx.subjects.PublishSubject

class UnsupportedCityControllerTest : BaseTest() {

    @Mock
    lateinit var detector: UnsupportedCityDetector

    @Mock
    lateinit var storage: UnsupportedCityStorage

    @Mock
    lateinit var navigator: UnsupportedCityNavigator

    private lateinit var controller: UnsupportedCityController

    private lateinit var cityInfo: CityInfo

    @Before
    override fun setUp() {
        super.setUp()

        whenever(detector.start()).thenReturn(Single.defer { Single.just(cityInfo) })
        whenever(navigator.showDialog(ArgumentMatchers.anyString()))
            .thenReturn(Single.just(RequestDispatcher.Response(0, null)))

        controller = UnsupportedCityController(detector, navigator, storage)
    }

    @Test
    fun dialogIsShownForKnownCityName() {
        cityInfo = CityInfo(supported = false, name = "city", id = 0)

        controller.checkAndShowWarning().subscribe()

        verify(navigator).showDialog(cityInfo.name)
        verify(storage).isWarningShown(cityInfo.id)
        verify(storage).saveWarningShown(cityInfo.id)
    }

    @Test
    fun dialogIsShownForUnknownCityName() {
        cityInfo = CityInfo(supported = false, name = "", id = 0)

        controller.checkAndShowWarning().subscribe()

        verify(navigator).showDialog(cityInfo.name)
        verify(storage).isWarningShown(cityInfo.id)
        verify(storage).saveWarningShown(cityInfo.id)
    }

    @Test
    fun dialogIsNotShownForSupportedCity() {
        cityInfo = CityInfo(supported = true, name = "city", id = 0)

        controller.checkAndShowWarning().subscribe()

        verifyNoMoreInteractions(navigator)
        verifyNoMoreInteractions(storage)
    }

    @Test
    fun dialogIsNotShownSecondTime() {
        cityInfo = CityInfo(supported = false, name = "city", id = 0)
        whenever(storage.isWarningShown(0)).thenReturn(true)

        controller.checkAndShowWarning().subscribe()

        verify(storage).isWarningShown(cityInfo.id)
        verifyNoMoreInteractions(storage)
        verifyNoMoreInteractions(navigator)
    }


    @Test
    fun dialogShownAndThenCanceled() {
        cityInfo = CityInfo(supported = false, name = "city", id = 0)
        val subject = PublishSubject.create<RequestDispatcher.Response>()
        whenever(navigator.showDialog(ArgumentMatchers.anyString())).thenReturn(subject.toSingle())

        controller.checkAndShowWarning().subscribe()
        controller.hideDialogAndMarkAsNotShown()

        verify(navigator).showDialog("city")

        val inOrder = inOrder(storage)
        inOrder.verify(storage).isWarningShown(cityInfo.id)
        inOrder.verify(storage).saveWarningShown(cityInfo.id)
        inOrder.verify(storage).saveWarningNotShown(cityInfo.id)
    }
}