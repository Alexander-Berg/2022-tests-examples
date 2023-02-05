package ru.yandex.yandexbus.inhouse.account.profile

import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.account.profile.view.ProfileMenuItem
import ru.yandex.yandexbus.inhouse.account.profile.view.ProfileMenuItem.FEEDBACK
import ru.yandex.yandexbus.inhouse.account.profile.view.ProfileMenuItem.PASSENGER_INFO
import ru.yandex.yandexbus.inhouse.account.profile.view.ProfileMenuItem.SETTINGS
import ru.yandex.yandexbus.inhouse.account.profile.view.ProfileMenuItem.TOP_UP_TRAVEL_CARDS
import ru.yandex.yandexbus.inhouse.feature.Feature.NEW_FROM_FEEDBACK
import ru.yandex.yandexbus.inhouse.feature.FeatureManager
import ru.yandex.yandexbus.inhouse.model.TestCityLocationInfo
import ru.yandex.yandexbus.inhouse.promocode.PromoCodesExperiment
import ru.yandex.yandexbus.inhouse.service.award.AwardService
import ru.yandex.yandexbus.inhouse.service.settings.RegionSettings
import ru.yandex.yandexbus.inhouse.whenever
import rx.Observable
import rx.subjects.PublishSubject

class ProfilePresenterTest : BaseTest() {

    @Mock
    lateinit var profileContentPresenter: ProfileContentPresenter
    @Mock
    lateinit var featureManager: FeatureManager
    @Mock
    lateinit var achievementsRepo: AwardService
    @Mock
    lateinit var navigator: ProfileContract.Navigator
    @Mock
    lateinit var regionSettings: RegionSettings
    @Mock
    lateinit var regionProperty: RegionSettings.RegionProperty
    @Mock
    lateinit var analyticsSender: ProfileAnalyticsSender
    @Mock
    lateinit var promoCodesExperiment: PromoCodesExperiment
    @Mock
    lateinit var view: ProfileContract.View
    @Mock
    lateinit var viewHeader: ProfileContract.View.Header

    private val menuItemClicksSubject = PublishSubject.create<ProfileMenuItem>()

    @Before
    override fun setUp() {
        super.setUp()

        whenever(regionSettings.currentRegion()).thenReturn(regionProperty)
        whenever(regionProperty.value()).thenReturn(Observable.just(TestCityLocationInfo.MOSCOW))

        whenever(view.header).thenReturn(viewHeader)

        whenever(viewHeader.itemClicks).thenReturn(menuItemClicksSubject)
    }

    @Test
    fun `processes settings click`() {

        createAttachStart(view)

        menuItemClicksSubject.onNext(SETTINGS)

        verify(analyticsSender).settingsOpened()
        verify(navigator).toSettingsView()
    }

    @Test
    fun `processes new feedback click`() {

        whenever(featureManager.isFeatureEnabled(NEW_FROM_FEEDBACK)).thenReturn(true)

        createAttachStart(view)

        menuItemClicksSubject.onNext(FEEDBACK)

        verify(analyticsSender).feedbackViewOpened()
        verify(navigator).toFeedbackView()
    }

    @Test
    fun `processes old feedback click`() {

        whenever(featureManager.isFeatureEnabled(NEW_FROM_FEEDBACK)).thenReturn(false)

        createAttachStart(view)

        menuItemClicksSubject.onNext(FEEDBACK)

        verify(analyticsSender, never()).feedbackViewOpened()
        verify(navigator).toReportError()
    }

    @Test
    fun `processes top up travel card click`() {
        whenever(regionProperty.value()).thenReturn(Observable.just(TestCityLocationInfo.MOSCOW))

        createAttachStart(view)

        menuItemClicksSubject.onNext(TOP_UP_TRAVEL_CARDS)
        verify(navigator).toTopUpTravelCard()
    }

    @Test
    fun `processes passenger info click`() {

        whenever(regionProperty.value()).thenReturn(Observable.just(TestCityLocationInfo.SAINT_PETERSBURG))

        createAttachStart(view)

        menuItemClicksSubject.onNext(PASSENGER_INFO)
        verify(navigator).toPassengerInfo()
    }

    private fun createAttachStart(view: ProfileContract.View) = createPresenter().apply {
        onCreate()
        onAttach(view)
        onViewStart()
    }

    private fun createPresenter() = ProfilePresenter(
        profileContentPresenter,
        featureManager,
        achievementsRepo,
        navigator,
        regionSettings,
        analyticsSender,
        promoCodesExperiment
    )
}
