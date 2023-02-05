package ru.yandex.market.clean.presentation.feature.onboarding

import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.analytics.facades.OnboardingAnalytics
import ru.yandex.market.clean.domain.model.AutoDetectedRegion
import ru.yandex.market.clean.domain.model.deliveryAvailabilityTestInstance
import ru.yandex.market.clean.presentation.feature.onboarding.region.RegionOnboardingStepPresenter
import ru.yandex.market.clean.presentation.feature.onboarding.region.RegionOnboardingStepView
import ru.yandex.market.clean.presentation.feature.region.choose.RegionChooseTargetScreen
import ru.yandex.market.clean.presentation.feature.region.confirm.RegionConfirmUseCases
import ru.yandex.market.clean.presentation.feature.region.nearby.NearbyRegionFormatter
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.datetime.DateTimeProvider
import ru.yandex.market.domain.models.region.deliveryLocalityTestInstance
import ru.yandex.market.presentationSchedulersMock

class RegionOnboardingStepPresenterTest {

    private val deliveryAvailability = deliveryAvailabilityTestInstance().copy(
        locality = deliveryLocalityTestInstance().copy(
            name = REGION_NAME
        ),
    )

    private val useCases = mock<RegionConfirmUseCases>() {
        on { getAutoDetectedRegion() } doReturn Single.just(AutoDetectedRegion())
        on { getRegionDeliveryInformation(any()) } doReturn Single.just(deliveryAvailability)
        on { getDefaultDeliveryAvailable() } doReturn deliveryAvailability
        on { getRegionByCurrentLocality(any(), any()) } doReturn Single.just(deliveryAvailability)
        on { saveSelectedRegion(any()) } doReturn Completable.complete()
    }
    private val schedulers = presentationSchedulersMock()
    private val router = mock<Router>()
    private val nearbyRegionFormatter = mock<NearbyRegionFormatter>()
    private val onboardingAnalytics = mock<OnboardingAnalytics>()
    private val dateTimeProvider = mock<DateTimeProvider>()

    private val presenter = RegionOnboardingStepPresenter(
        schedulers = schedulers,
        router = router,
        useCases = useCases,
        nearbyRegionFormatter = nearbyRegionFormatter,
        onboardingAnalytics = onboardingAnalytics,
        dateTimeProvider = dateTimeProvider
    )

    private val view = mock<RegionOnboardingStepView>()

    @Test
    fun `show region name`() {
        presenter.attachView(view)
        verify(view).showRegionName(REGION_NAME)
    }

    @Test
    fun `choose region click`() {
        presenter.attachView(view)
        presenter.onChooseRegionClick()
        verify(router).navigateForResult(isA<RegionChooseTargetScreen>(),
            argThat { actualListener ->
                actualListener.onResult(true)
                true
            })
    }

    @Test
    fun `agree region click`() {
        presenter.attachView(view)
        presenter.onAgreeButtonClick()
        verify(view).openNextStep()
    }

    @Test
    fun `interrupt gps detection click`() {
        presenter.attachView(view)
        presenter.onChooseRegionClick()
        presenter.onInterruptGpsDetectingClick()
        verify(router).navigateForResult(isA<RegionChooseTargetScreen>(),
            argThat { actualListener ->
                actualListener.onResult(true)
                true
            })
    }

    @Test
    fun `gps detection error`() {
        whenever(useCases.getRegionByCurrentLocality(any(), any())) doReturn Single.error(RuntimeException())
        presenter.attachView(view)
        presenter.onChooseRegionClick()
        verify(router).navigateForResult(isA<RegionChooseTargetScreen>(),
            argThat { actualListener ->
                actualListener.onResult(true)
                true
            })
    }

    companion object {
        private const val REGION_NAME = "name"
    }

}