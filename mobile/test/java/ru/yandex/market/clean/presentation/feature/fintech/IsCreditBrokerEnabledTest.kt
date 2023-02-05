package ru.yandex.market.clean.presentation.feature.fintech

import io.reactivex.Single
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.common.experiments.experiment.fintech.CreditBrokerExperiment
import ru.yandex.market.common.experiments.manager.ExperimentManager
import ru.yandex.market.common.featureconfigs.managers.CreditBrokerToggleManager
import ru.yandex.market.common.featureconfigs.models.FeatureToggle
import ru.yandex.market.common.featureconfigs.provider.FeatureConfigsProvider
import ru.yandex.market.domain.fintech.usecase.creditBroker.IsCreditBrokerEnabledUseCase

@RunWith(Parameterized::class)
class IsCreditBrokerEnabledTest(
    private val isToggleEnabled: Boolean,
    private val isExperimentEnabled: Boolean,
) {
    private val toggle = mock<FeatureToggle>() {
        on { isEnabled } doReturn isToggleEnabled
    }

    private val toggleManager = mock<CreditBrokerToggleManager>() {
        on { getSingle() } doReturn Single.just(toggle)
    }

    private val featureConfigsProvider = mock<FeatureConfigsProvider>() {
        on { creditBrokerToggleManager } doReturn toggleManager
    }

    private val experimentManager = mock<ExperimentManager>() {
        onGeneric { getExperiment(CreditBrokerExperiment.Split::class.java) } doReturn CreditBrokerExperiment.Split(
            isExperimentEnabled
        )
    }

    @Test
    fun `test IsBnplSdkEnabledUseCase`() {
        val useCase = IsCreditBrokerEnabledUseCase(featureConfigsProvider, experimentManager)
        useCase.execute().test().assertValue(isToggleEnabled && isExperimentEnabled)
    }

    companion object {

        @Parameterized.Parameters(name = "when isCreditBrokerEnabled is {1}")
        @JvmStatic
        fun data() = listOf<Array<*>>(
            arrayOf(true, true),
            arrayOf(false, false),
            arrayOf(true, false),
            arrayOf(false, true),
        )
    }
}
