package ru.yandex.market.domain.fintech.usecase.yacard

import io.reactivex.subjects.SingleSubject
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.common.experiments.experiment.fintech.YaBankPaymentExperiment
import ru.yandex.market.common.experiments.manager.ExperimentManager
import ru.yandex.market.common.featureconfigs.managers.YaBankPaymentToggleManager
import ru.yandex.market.common.featureconfigs.models.YandexPaymentFeatureConfig
import ru.yandex.market.common.featureconfigs.provider.FeatureConfigsProvider

internal class IsYaBankPaymentEnabledUseCaseTest {

    private val toggleSubject: SingleSubject<YandexPaymentFeatureConfig> = SingleSubject.create()
    private val yaBankPaymentToggleManager = mock<YaBankPaymentToggleManager> {
        on { getSingle() } doReturn toggleSubject
    }
    private val featureConfigsProvider = mock<FeatureConfigsProvider> {
        on { yaBankPaymentToggleManager } doReturn yaBankPaymentToggleManager
    }
    private val experimentSplit = mock<YaBankPaymentExperiment.Split>()
    private val experimentManager = mock<ExperimentManager> {
        on { getExperiment(YaBankPaymentExperiment.Split::class.java) } doReturn experimentSplit
    }

    private val useCase = IsYaBankPaymentEnabledUseCase(
        featureConfigsProvider,
        experimentManager
    )

    @Test
    fun `enabled when toggle and exp enabled`() {
        whenever(experimentSplit.isEnabled) doReturn true
        toggleSubject.onSuccess(YandexPaymentFeatureConfig(isEnabled = true, ignoreExperiment = false))

        useCase.execute()
            .test()
            .assertValue(true)
    }

    @Test
    fun `disabled when toggle enabled and exp disabled`() {
        whenever(experimentSplit.isEnabled) doReturn false
        toggleSubject.onSuccess(YandexPaymentFeatureConfig(isEnabled = true, ignoreExperiment = false))

        useCase.execute()
            .test()
            .assertValue(false)
    }

    @Test
    fun `disabled when exp enabled and toggle disabled`() {
        whenever(experimentSplit.isEnabled) doReturn true
        toggleSubject.onSuccess(YandexPaymentFeatureConfig(isEnabled = false, ignoreExperiment = false))

        useCase.execute()
            .test()
            .assertValue(false)
    }

    @Test
    fun `enabled when toggle ignored exp and exp disabled`() {
        whenever(experimentSplit.isEnabled) doReturn false
        toggleSubject.onSuccess(YandexPaymentFeatureConfig(isEnabled = true, ignoreExperiment = true))

        useCase.execute()
            .test()
            .assertValue(true)
    }
}
