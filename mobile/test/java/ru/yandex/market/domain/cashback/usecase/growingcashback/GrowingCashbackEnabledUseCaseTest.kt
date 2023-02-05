package ru.yandex.market.domain.cashback.usecase.growingcashback

import io.reactivex.subjects.SingleSubject
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.common.featureconfigs.managers.GrowingCashbackToggleManager
import ru.yandex.market.common.featureconfigs.models.FeatureToggle
import ru.yandex.market.common.featureconfigs.provider.FeatureConfigsProvider

class GrowingCashbackEnabledUseCaseTest {

    private val toggleSubject: SingleSubject<FeatureToggle> = SingleSubject.create()

    private val growingCashbackToggleManager = mock<GrowingCashbackToggleManager> {
        on { getSingle() } doReturn toggleSubject
    }
    private val featureConfigsProvider = mock<FeatureConfigsProvider> {
        on { growingCashbackToggleManager } doReturn growingCashbackToggleManager
    }
    private val useCase = GrowingCashbackEnabledUseCase(featureConfigsProvider)

    @Test
    fun `growing cashback enabled when toggle enabled`() {
        toggleSubject.onSuccess(FeatureToggle(true))

        useCase.execute()
            .test()
            .assertValue(true)
    }

    @Test
    fun `growing cashback disabled when toggle disabled`() {
        toggleSubject.onSuccess(FeatureToggle(false))

        useCase.execute()
            .test()
            .assertValue(false)
    }

    @Test
    fun `useCase does not handle exceptions`() {
        val error = RuntimeException()
        toggleSubject.onError(error)

        useCase.execute()
            .test()
            .assertError(error)
    }
}