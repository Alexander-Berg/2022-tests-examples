package ru.yandex.market.clean.domain.usecase.ar

import com.yandex.passport.api.Passport
import com.yandex.passport.api.PassportUid
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import ru.yandex.market.common.featureconfigs.managers.Product3DModelViewerForYandexoidToggleManager
import ru.yandex.market.common.featureconfigs.managers.Product3DModelViewerToggleManager
import ru.yandex.market.common.featureconfigs.models.FeatureToggle
import ru.yandex.market.common.featureconfigs.provider.FeatureConfigsProvider
import ru.yandex.market.manager.AuthManager
import ru.yandex.market.mockResult
import ru.yandex.market.passport.model.AuthAccount
import ru.yandex.market.passport.model.AuthAccountType

class IsProduct3DPreviewEnabledUseCaseTest {

    private val product3DModelViewerToggleManager = mock<Product3DModelViewerToggleManager>()
    private val product3DModelViewerForYandexoidToggleManager = mock<Product3DModelViewerForYandexoidToggleManager>()
    private val featureConfigsProvider = mock<FeatureConfigsProvider>()
    private val authRepository = mock<AuthManager>()
    private val useCase = IsProduct3DPreviewEnabledUseCase(featureConfigsProvider, authRepository)

    @Before
    fun setup() {
        featureConfigsProvider.product3DModelViewerToggleManager.mockResult(product3DModelViewerToggleManager)
        featureConfigsProvider.product3DModelViewerForYandexoidToggleManager.mockResult(product3DModelViewerForYandexoidToggleManager)
    }

    @Test
    fun `3D preview is unavailable for regular user when feature is turned OFF globally`() {
        product3DModelViewerToggleManager.get().mockResult(FeatureToggle(false))
        product3DModelViewerForYandexoidToggleManager.get().mockResult(FeatureToggle(true))
        authRepository.account.mockResult(buildUserAccount(false))

        useCase.execute().test().assertNoErrors().assertValue(false)
    }

    @Test
    fun `3D preview is unavailable for Yandexoid when feature is turned OFF both globally and for yandexoids`() {
        product3DModelViewerToggleManager.get().mockResult(FeatureToggle(false))
        product3DModelViewerForYandexoidToggleManager.get().mockResult(FeatureToggle(false))
        authRepository.account.mockResult(buildUserAccount(true))

        useCase.execute().test().assertNoErrors().assertValue(false)
    }

    @Test
    fun `3D preview is available for Yandexoid when feature is turned ON globally`() {
        product3DModelViewerToggleManager.get().mockResult(FeatureToggle(true))
        product3DModelViewerForYandexoidToggleManager.get().mockResult(FeatureToggle(false))
        authRepository.account.mockResult(buildUserAccount(true))

        useCase.execute().test().assertNoErrors().assertValue(true)
    }

    @Test
    fun `3D preview is available for regular user when feature is turned ON globally`() {
        product3DModelViewerToggleManager.get().mockResult(FeatureToggle(true))
        product3DModelViewerForYandexoidToggleManager.get().mockResult(FeatureToggle(false))
        authRepository.account.mockResult(buildUserAccount(false))

        useCase.execute().test().assertNoErrors().assertValue(true)
    }

    @Test
    fun `3D preview is unavailable for regular users when feature is turned ON just for Yandexoids`() {
        product3DModelViewerToggleManager.get().mockResult(FeatureToggle(false))
        product3DModelViewerForYandexoidToggleManager.get().mockResult(FeatureToggle(true))
        authRepository.account.mockResult(buildUserAccount(false))

        useCase.execute().test().assertNoErrors().assertValue(false)
    }

    @Test
    fun `3D preview is available for Yandexoid when feature is turned ON just for Yandexoids`() {
        product3DModelViewerToggleManager.get().mockResult(FeatureToggle(false))
        product3DModelViewerForYandexoidToggleManager.get().mockResult(FeatureToggle(true))
        authRepository.account.mockResult(buildUserAccount(true))

        useCase.execute().test().assertNoErrors().assertValue(true)
    }

    private fun buildUserAccount(isYandexoid: Boolean): AuthAccount {
        return AuthAccount(
            uid = PassportUid.Factory.from(Passport.PASSPORT_ENVIRONMENT_PRODUCTION, 42L),
            primaryDisplayName = "primaryName",
            secondaryDisplayName = "secondaryName",
            avatarUrl = "",
            nativeDefaultEmail = "",
            isYandexoid = isYandexoid,
            hasPlus = false,
            firstName = "firstName",
            lastName = "lastName",
            isSocial = false,
            isMailish = false,
            isPhonish = false,
            isLite = false,
            isAuthorized = false,
            type = AuthAccountType.YANDEX

        )
    }
}