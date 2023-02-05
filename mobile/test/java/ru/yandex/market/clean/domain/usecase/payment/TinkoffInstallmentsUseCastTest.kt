package ru.yandex.market.clean.domain.usecase.payment

import io.reactivex.Single
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.common.experiments.experiment.payment.TinkoffInstallmentsExperiment
import ru.yandex.market.common.experiments.manager.ExperimentManager
import ru.yandex.market.common.featureconfigs.managers.PaymentSdkToggleManager
import ru.yandex.market.common.featureconfigs.managers.TinkoffInstallmentsToggleManager
import ru.yandex.market.common.featureconfigs.models.FeatureToggle
import ru.yandex.market.common.featureconfigs.provider.FeatureConfigsProvider
import ru.yandex.market.domain.fintech.usecase.tinkoff.IsTinkoffInstallmentsEnabledUseCase

@RunWith(Parameterized::class)
class TinkoffInstallmentsUseCaseTest(
    private val isSdkEnabled: Boolean,
    private val isTinkoffEnabled: Boolean,
    isTinkoffExpEnabled: Boolean,
    private val tinkoffInstallmentsResult: Boolean
) {

    private val tinkoffInstallmentsConfigManager = mock<TinkoffInstallmentsToggleManager>()
    private val paymentSdkToggleManager = mock<PaymentSdkToggleManager>()
    private val experimentManager = mock<ExperimentManager>()
    private val featureConfigsProvider = mock<FeatureConfigsProvider>().also { provider ->
        whenever(provider.tinkoffInstallmentsToggleManager) doReturn tinkoffInstallmentsConfigManager
        whenever(provider.paymentSdkToggleManager) doReturn paymentSdkToggleManager
    }


    private val tinkoffExperimentSplit = object : TinkoffInstallmentsExperiment.Split {
        override val isEnabled: Boolean = isTinkoffExpEnabled
    }
    private val isTinkoffInstallmentsEnabledUseCase = IsTinkoffInstallmentsEnabledUseCase(
        featureConfigsProvider = featureConfigsProvider,
        experimentManager = experimentManager
    )

    @Test
    fun checkUseCasesOutput() {
        whenever(tinkoffInstallmentsConfigManager.getSingle())
            .thenReturn(Single.just(FeatureToggle(isEnabled = isTinkoffEnabled)))
        whenever(experimentManager.getExperiment(TinkoffInstallmentsExperiment.Split::class.java))
            .thenReturn(tinkoffExperimentSplit)
        whenever(paymentSdkToggleManager.getSingle())
            .thenReturn(Single.just(FeatureToggle(isEnabled = isSdkEnabled)))

        isTinkoffInstallmentsEnabledUseCase.execute()
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValue(tinkoffInstallmentsResult)
    }

    companion object {
        @Parameterized.Parameters(
            name = "when isSdkEnabled is {1} " +
                    "isTinkoffEnabled is {2} " +
                    "isTinkoffExpEnabled is {3} " +
                    "tinkoffInstallmentsResult is {4}"
        )
        @JvmStatic
        fun data() = listOf<Array<*>>(
            //0
            arrayOf(
                true, true, true, true
            ),
            //2
            arrayOf(
                false, false, false, false
            ),
            //3
            arrayOf(
                true, false, false, false
            ),
            //4
            arrayOf(
                false, true, false, false
            ),
            //5
            arrayOf(
                false, false, true, false
            ),
            //6
            arrayOf(
                false, true, true, false
            ),
            //7
            arrayOf(
                true, false, true, false
            ),
            //8
            arrayOf(
                true, true, false, false
            )
        )
    }
}