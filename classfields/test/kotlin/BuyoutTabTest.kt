package ru.auto.feature.auction_flow_v2

import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.auto.ara.interactor.ILocationAutoDetectInteractor
import ru.auto.data.model.AutoruUserProfile
import ru.auto.data.model.User
import ru.auto.data.model.UserProfile
import ru.auto.data.model.geo.SuggestGeoItem
import ru.auto.data.repository.user.IUserRepository
import ru.auto.data.util.CITY_ID_MOSCOW
import ru.auto.data.util.CITY_ID_SPB
import ru.auto.data.util.REGION_ID_MOSCOW_REGION
import ru.auto.experiments.Experiments
import ru.auto.experiments.ExperimentsManager
import ru.auto.experiments.isBuyoutTabEnabled
import ru.auto.feature.auction_flow_v2.auction_main.BuyoutTabVisibilityRepository
import ru.auto.testextension.kotlinFixtureDefault
import ru.auto.testextension.prepareParameter
import ru.auto.testextension.testWithSubscriber
import rx.Observable
import rx.Single

@RunWith(AllureRunner::class)
class BuyoutTabTest {
    private val fixture = kotlinFixtureDefault()

    private val moscowGeoItem = prepareParameter("moscow_geo_item", fixture<SuggestGeoItem>().copy(id = CITY_ID_MOSCOW))
    private val moscowRegionGeoItem =
        prepareParameter("moscow_region_geo_item", fixture<SuggestGeoItem>().copy(id = REGION_ID_MOSCOW_REGION))
    private val spbGeoItem = prepareParameter("spb_geo_item", fixture<SuggestGeoItem>().copy(id = CITY_ID_SPB))
    private val moscowRegionUserProfile =
        prepareParameter("moscow_region_user_profile", fixture<AutoruUserProfile>().copy(geoItem = moscowRegionGeoItem))
    private val spbUserProfile =
        prepareParameter("spb_user_profile", fixture<AutoruUserProfile>().copy(geoItem = spbGeoItem))
    private val moscowRegionUser =
        prepareParameter("moscow_region_user",
            fixture<User.Authorized>().copy(userProfile = UserProfile(moscowRegionUserProfile)))
    private val spbUser =
        prepareParameter("spb_user", fixture<User.Authorized>().copy(userProfile = UserProfile(spbUserProfile)))

    private val mockLocationInteractor = mock<ILocationAutoDetectInteractor> {
        on { detectedLocation(any()) } doReturn Single.just(null)
    }
    private val mockUserRepository = mock<IUserRepository> {
        on { observeUser() } doReturn Observable.just(User.Unauthorized)
    }
    private val buyoutRepository = BuyoutTabVisibilityRepository(mockLocationInteractor)

    init {
        val mockedExperiments: Experiments = mock()
        whenever(mockedExperiments.isBuyoutTabEnabled()).thenReturn(true)
        ExperimentsManager.setInstance(mockedExperiments)
    }

    @Test
    fun `should not show buyout tab when experiment is disabled`() {
        step("when buyout experiment is disabled") {
            val mockedExperiments: Experiments = mock()
            whenever(mockedExperiments.isBuyoutTabEnabled()).thenReturn(false)
            ExperimentsManager.setInstance(mockedExperiments)
        }

        step("even if user region is moscow") {
            whenever(mockLocationInteractor.detectedLocation(any())).thenReturn(Single.just(moscowRegionGeoItem))
            whenever(mockUserRepository.observeUser()).thenReturn(Observable.just(moscowRegionUser))
        }

        step("should not allow showing buyout tab") {
            testWithSubscriber(buyoutRepository.shouldShowBuyoutTab()) { sub ->
                sub.assertValue(false)
            }
        }
    }

    @Test
    fun `should not show buyout tab when region id is not moscow or region`() {
        testWithSubscriber(buyoutRepository.shouldShowBuyoutTab()) { sub ->
            sub.assertValue(false)
        }
    }

    @Test
    fun `should show buyout tab when user detected location is moscow`() {
        step("when buyout tab experiment is enabled and user detected location is moscow") {
            whenever(mockLocationInteractor.detectedLocation(any())).thenReturn(Single.just(moscowGeoItem))
        }
        step("should allow showing buyout tab") {
            testWithSubscriber(buyoutRepository.shouldShowBuyoutTab()) { sub ->
                sub.assertValue(true)
            }
        }
    }

    @Test
    fun `should show buyout tab when user detected location is moscow region`() {
        step("when buyout tab experiment is enabled and user detected location is moscow region") {
            whenever(mockLocationInteractor.detectedLocation(any())).thenReturn(Single.just(moscowRegionGeoItem))
        }
        step("should allow showing buyout tab") {
            testWithSubscriber(buyoutRepository.shouldShowBuyoutTab()) { sub ->
                sub.assertValue(true)
            }
        }
    }

    @Test
    fun `should show buyout tab when user detected location is moscow but profile is not`() {
        step("when buyout tab experiment is enabled and user detected location is moscow but profile is not") {
            whenever(mockLocationInteractor.detectedLocation(any())).thenReturn(Single.just(moscowGeoItem))
            whenever(mockUserRepository.observeUser()).thenReturn(Observable.just(spbUser))
        }
        step("should allow showing buyout tab") {
            testWithSubscriber(buyoutRepository.shouldShowBuyoutTab()) { sub ->
                sub.assertValue(true)
            }
        }
    }

    @Test
    fun `should not show buyout tab when user profile is moscow but detected location is not`() {
        step("when buyout tab experiment is enabled and user profile is moscow region but detected location is not") {
            whenever(mockUserRepository.observeUser()).thenReturn(Observable.just(moscowRegionUser))
            whenever(mockLocationInteractor.detectedLocation(any())).thenReturn(Single.just(spbGeoItem))
        }
        step("should allow showing buyout tab") {
            testWithSubscriber(buyoutRepository.shouldShowBuyoutTab()) { sub ->
                sub.assertValue(false)
            }
        }
    }
}
