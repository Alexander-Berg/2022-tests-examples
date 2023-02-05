package ru.yandex.market.domain.fintech.usecase.yacard

import io.reactivex.Single
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.common.featureconfigs.managers.YaCardToggleManager
import ru.yandex.market.common.featureconfigs.models.FeatureToggle
import ru.yandex.market.common.featureconfigs.provider.FeatureConfigsProvider

@RunWith(Parameterized::class)
class IsYaCardEnabledUseCaseTest(
    private val yandexBankEnabled: Boolean,
    private val yandexCardFeatureToggleEnabled: Boolean,
    private val expected: Boolean
) {

    private val featureToggle = mock<YaCardToggleManager> {
        on { getSingle() } doReturn Single.just(FeatureToggle(yandexCardFeatureToggleEnabled))
    }
    private val featureConfigsProvider = mock<FeatureConfigsProvider> {
        on { yaCardToggleManager } doReturn featureToggle
    }
    private val isYaBankPaymentEnabledUseCase = mock<IsYaBankPaymentEnabledUseCase> {
        on { execute() } doReturn Single.just(yandexBankEnabled)
    }
    private val useCase = IsYaCardEnabledUseCase(
        featureConfigsProvider,
        isYaBankPaymentEnabledUseCase
    )

    @Test
    fun testEnabled() {
        useCase.execute()
            .test()
            .assertValue(expected)
    }

    companion object {

        @Parameterized.Parameters(name = "{index}: банк доступен: {0}, промо доступно: {1} -> {2}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            //0
            arrayOf(false, false, false),
            //1
            arrayOf(true, false, false),
            //2
            arrayOf(false, true, false),
            //3
            arrayOf(true, true, true)
        )
    }
}
