package ru.yandex.market.clean.domain.usecase.payment

import io.reactivex.Single
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.common.experiments.experiment.payment.TinkoffCreditsExperiment
import ru.yandex.market.common.experiments.manager.ExperimentManager
import ru.yandex.market.common.featureconfigs.managers.CreditBrokerToggleManager
import ru.yandex.market.common.featureconfigs.managers.PaymentSdkToggleManager
import ru.yandex.market.common.featureconfigs.managers.TinkoffCreditsConfigManager
import ru.yandex.market.common.featureconfigs.models.FeatureToggle
import ru.yandex.market.common.featureconfigs.models.TinkoffCreditsConfig
import ru.yandex.market.common.featureconfigs.provider.FeatureConfigsProvider
import ru.yandex.market.domain.fintech.usecase.nativepayment.NativePaymentEnabledUseCase
import ru.yandex.market.domain.fintech.usecase.tinkoff.IsTinkoffCreditsEnabledUseCase

@RunWith(Parameterized::class)
class CreditsUseCaseTest(
    private val isSdkEnabled: Boolean,
    isTinkoffEnabled: Boolean,
    private val isCreditBrokerEnabled: Boolean,
    private val tinkoffCreditsResult: Boolean,
    private val nativePaymentResult: Boolean
) {

    private val tinkoffCreditsConfigManager = mock<TinkoffCreditsConfigManager>()
    private val paymentSdkToggleManager = mock<PaymentSdkToggleManager>()
    private val creditBrokerToggleManager = mock<CreditBrokerToggleManager>()
    private val experimentManager = mock<ExperimentManager>()
    private val featureConfigsProvider = mock<FeatureConfigsProvider>().also { provider ->
        whenever(provider.tinkoffCreditsConfigManager) doReturn tinkoffCreditsConfigManager
        whenever(provider.paymentSdkToggleManager) doReturn paymentSdkToggleManager
        whenever(provider.creditBrokerToggleManager) doReturn creditBrokerToggleManager
    }

    private val tinkoffExperimentSplit = object : TinkoffCreditsExperiment.Split {
        override val isEnabled: Boolean = true
    }
    private val isTinkoffCreditsEnabledUseCase = IsTinkoffCreditsEnabledUseCase(
        featureConfigsProvider = featureConfigsProvider,
        experimentManager = experimentManager
    )
    private val nativePaymentEnabledUseCase = NativePaymentEnabledUseCase(
        featureConfigsProvider = featureConfigsProvider
    )

    private val tinkoffConfig = TinkoffCreditsConfig(
        isEnabled = isTinkoffEnabled,
        upperBound = TinkoffCreditsConfig.Money(200000.toBigDecimal(), "RUR"),
        lowerBound = TinkoffCreditsConfig.Money(3500.toBigDecimal(), "RUR")
    )

    @Test
    fun checkUseCasesOutput() {
        whenever(experimentManager.getExperiment(TinkoffCreditsExperiment.Split::class.java))
            .thenReturn(tinkoffExperimentSplit)
        whenever(tinkoffCreditsConfigManager.getSingle())
            .thenReturn(Single.just(tinkoffConfig))
        whenever(paymentSdkToggleManager.getSingle())
            .thenReturn(Single.just(FeatureToggle(isEnabled = isSdkEnabled)))
        whenever(creditBrokerToggleManager.getSingle())
            .thenReturn(Single.just(FeatureToggle(isEnabled = isCreditBrokerEnabled)))

        isTinkoffCreditsEnabledUseCase.execute()
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValue(tinkoffCreditsResult)

        nativePaymentEnabledUseCase.isNativePaymentEnabled()
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValue(nativePaymentResult)
    }

    companion object {
        @Parameterized.Parameters(
            name = "when isSdkEnabled is {1} " +
                "isTinkoffEnabled is {2} " +
                "isCreditBrokerEnabled is {3} " +
                "tinkoffCreditsResult is {4} " +
                "nativePaymentResult is {5}"
        )
        @JvmStatic
        fun data() = listOf<Array<*>>(
            //0
            arrayOf(
                true, true, true, true, true
            ),
            //2
            arrayOf(
                false, false, false, false, false
            ),
            //3
            arrayOf(
                false, true, false, false, false
            ),
            //4
            arrayOf(
                true, false, false, false, true
            )
        )
    }
}
