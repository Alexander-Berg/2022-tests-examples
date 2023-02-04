package ru.auto.ara.interactor

import io.qameta.allure.kotlin.junit4.AllureRunner
 import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
 import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.auto.data.interactor.IProfileInteractor
import ru.auto.data.interactor.YaPlusInteractor
import ru.auto.data.interactor.YaPlusLoginInfo
import ru.auto.data.interactor.YaPlusPromoAvailability
import ru.auto.data.model.AutoruUserProfile
import ru.auto.data.model.SocialNet
import ru.auto.data.model.User
import ru.auto.data.model.UserProfile
import ru.auto.data.model.UserSocialProfile
import ru.auto.data.model.data.offer.ALL
import ru.auto.data.model.geo.SuggestGeoItem
import ru.auto.data.repository.IGeoRepository
import ru.auto.data.repository.IUserOffersRepository
import ru.auto.data.repository.IYaPlusRepository
import ru.auto.data.repository.YaPlusPointsConfig
import ru.auto.data.repository.user.IUserRepository
import ru.auto.data.util.DEFAULT_PAGE_SIZE
import ru.auto.test.core.RxTestAndroid
import rx.Observable
import rx.Single
import kotlin.test.assertEquals

@RunWith(AllureRunner::class) class YaPlusInteractoruTest : RxTestAndroid() {

    private val geoRepo: IGeoRepository = mock()
    private val profileInteractor: IProfileInteractor = mock()
    private val userRepository: IUserRepository = mock()
    private val yaPlusRepo: IYaPlusRepository = mock()
    private val userOffersRepo: IUserOffersRepository = mock()

    private lateinit var yaPlusInteractor: YaPlusInteractor

    @Before
    fun setUp() {
        whenever(geoRepo.getGeoSuggest()).thenReturn(Single.just(emptyList()))
        whenever(profileInteractor.fetchAndUpdateAutoruProfile()).thenReturn(
            Single.just(
                AutoruUserProfile()
            )
        )
        whenever(userRepository.observeUser()).thenReturn(Observable.just(User.Unauthorized))
        whenever(userRepository.user).thenReturn(User.Unauthorized)
        whenever(yaPlusRepo.getYaPlusPointsConfig()).thenReturn(YaPlusPointsConfig.Unavailable)
        whenever(userOffersRepo.loadOffers(
            category = ALL,
            page = 1,
            pageSize = DEFAULT_PAGE_SIZE,
        )).thenReturn(Observable.just(emptyList()))

        yaPlusInteractor = YaPlusInteractor(
            geoRepo = geoRepo,
            profileInteractor = profileInteractor,
            userRepository = userRepository,
            yaPlusRepo = yaPlusRepo,
            userOffersRepo = userOffersRepo,
            logError = { _, _ -> }
        )
    }

    @Test
    fun shouldReturnNotAuthorizedLoginInfo() {
        assertEquals<YaPlusLoginInfo>(yaPlusInteractor.getLoginInfo().toBlocking().value(), YaPlusLoginInfo.NotAuthorized)
    }

    private fun userRepoToReturnProfile(profile: AutoruUserProfile) {
        val mockUser = User.Authorized(
            id = "id00000",
            userProfile = UserProfile(
                autoruUserProfile = profile
            )
        )
        whenever(userRepository.observeUser()).thenReturn(Observable.just(mockUser))
        whenever(userRepository.user).thenReturn(mockUser)
    }

    @Test
    fun shouldReturnAuthorizedLoginInfoOnOtherUser() {
        val testUserName = "TestName"
        val testUserProfile = AutoruUserProfile(fullName = testUserName)
        userRepoToReturnProfile(testUserProfile)
        whenever(profileInteractor.fetchAndUpdateAutoruProfile()).thenReturn(
            Single.just(
                testUserProfile
            )
        )
        val emptyUser = YaPlusLoginInfo.Authorized.Other(
            userImageUrl = null,
            name = testUserName,
            isNotReseller = true
        )
        yaPlusInteractor.getLoginInfo().test().assertValue(emptyUser)
    }

    @Test
    fun shouldReturnAuthorizedLoginInfoOnYandexUser() {
        val testUserName = "TestName"
        val testUserProfile = AutoruUserProfile(
            fullName = testUserName,
            socialProfiles = listOf(UserSocialProfile(socialNet = SocialNet.YANDEX, socialUserId = "id00000"))
        )
        userRepoToReturnProfile(testUserProfile)
        whenever(profileInteractor.fetchAndUpdateAutoruProfile()).thenReturn(
            Single.just(
                testUserProfile
            )
        )
        val emptyYaUser = YaPlusLoginInfo.Authorized.Ya(
            userImageUrl = null,
            name = testUserName,
            isNotReseller = true
        )
        yaPlusInteractor.getLoginInfo().test().assertValue(emptyYaUser)
    }

    private fun setupAvailablePromotion(amount: Int) {
        val testRegion = 112
        val testDetailsLink = "promo_details.com"
        whenever(geoRepo.getGeoSuggest()).thenReturn(Single.just(listOf(SuggestGeoItem(id = testRegion.toString()))))
        whenever(yaPlusRepo.getYaPlusPointsConfig()).thenReturn(YaPlusPointsConfig.Available(
            regions = listOf(testRegion),
            amount = amount,
            detailsLink = testDetailsLink,
        ))
    }

    @Test
    fun shouldReturnAvailablePromotion() {
        val testAmount = 500000
        setupAvailablePromotion(testAmount)
        val promoAvailabilityAvailable = YaPlusPromoAvailability.Available(testAmount)
        yaPlusInteractor.isPromotionAvailable().test().assertValue(promoAvailabilityAvailable)
    }

    @Test
    fun shouldReturnUnavailablePromotion() {
        val testRegion = 112
        whenever(geoRepo.getGeoSuggest()).thenReturn(Single.just(listOf(SuggestGeoItem(id = testRegion.toString()))))
        whenever(yaPlusRepo.getYaPlusPointsConfig()).thenReturn(YaPlusPointsConfig.Unavailable)
        val promoAvailabilityAvailable = YaPlusPromoAvailability.Unavailable
        yaPlusInteractor.isPromotionAvailable().test().assertValue(promoAvailabilityAvailable)
    }

    @Test
    fun shouldReturnAvailableOnUserEvents() {
        val testUserName = "TestName"
        val testAmount = 500000
        val testUserProfile = AutoruUserProfile(fullName = testUserName)
        userRepoToReturnProfile(testUserProfile)
        setupAvailablePromotion(testAmount)
        val promoAvailabilityAvailable = YaPlusPromoAvailability.Available(testAmount)
        yaPlusInteractor.observePromotionAvailabilityOnUserEvents().first().test().assertValue(promoAvailabilityAvailable)
    }

    @Test
    fun shouldReturnUnavailableOnUserEvents() {
        val testUserName = "TestName"
        val testUserProfile = AutoruUserProfile(fullName = testUserName)
        userRepoToReturnProfile(testUserProfile)
        whenever(yaPlusRepo.getYaPlusPointsConfig()).thenReturn(YaPlusPointsConfig.Unavailable)
        val promoAvailabilityAvailable = YaPlusPromoAvailability.Unavailable
        yaPlusInteractor.observePromotionAvailabilityOnUserEvents().first().test().assertValue(promoAvailabilityAvailable)
    }
}
