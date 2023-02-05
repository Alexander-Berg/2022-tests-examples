package ru.yandex.market.domain.fintech.usecase.yacard

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.common.experiments.experiment.fintech.YaBankPaymentExperiment
import ru.yandex.market.common.experiments.manager.ExperimentManager
import ru.yandex.market.common.featureconfigs.managers.YaBankPaymentToggleManager
import ru.yandex.market.common.featureconfigs.managers.YaCardToggleManager
import ru.yandex.market.common.featureconfigs.models.FeatureToggle
import ru.yandex.market.common.featureconfigs.models.YandexPaymentFeatureConfig
import ru.yandex.market.common.featureconfigs.provider.FeatureConfigsProvider

internal class CanShowBankSdkUseCaseTest {

    private val yaPaymentToggle = mock<YandexPaymentFeatureConfig>()
    private val yaBankPaymentToggleManager = mock<YaBankPaymentToggleManager> {
        on { getFromCacheOrDefault() } doReturn yaPaymentToggle
    }
    private val yanSdkToggle = mock<FeatureToggle>()
    private val yaSdkToggleManager = mock<YaCardToggleManager> {
        on { getFromCacheOrDefault() } doReturn yanSdkToggle
    }
    private val featureConfigsProvider = mock<FeatureConfigsProvider> {
        on { yaBankPaymentToggleManager } doReturn yaBankPaymentToggleManager
        on { yaCardToggleManager } doReturn yaSdkToggleManager
    }
    private val yandexPaymentSplit = mock<YaBankPaymentExperiment.Split>()
    private val experimentManager = mock<ExperimentManager> {
        on { getExperiment(YaBankPaymentExperiment.Split::class.java) } doReturn yandexPaymentSplit
    }

    private val useCase = CanShowBankSdkUseCase(
        featureConfigsProvider,
        experimentManager
    )

    @Test
    fun `enabled when both toggles and exp enabled`() {
        whenever(yandexPaymentSplit.isEnabled) doReturn true
        whenever(yaPaymentToggle.isEnabled) doReturn true
        whenever(yanSdkToggle.isEnabled) doReturn true

        assertThat(useCase.execute()).isTrue
    }

    @Test
    fun `enabled when sdk toggle enabled, payment toggle force exp and exp disabled`() {
        whenever(yandexPaymentSplit.isEnabled) doReturn false
        whenever(yaPaymentToggle.isEnabled) doReturn true
        whenever(yaPaymentToggle.ignoreExperiment) doReturn true
        whenever(yanSdkToggle.isEnabled) doReturn true

        assertThat(useCase.execute()).isTrue
    }

    @Test
    fun `disabled when sdk toggle disabled`() {
        whenever(yandexPaymentSplit.isEnabled) doReturn true
        whenever(yaPaymentToggle.isEnabled) doReturn true
        whenever(yanSdkToggle.isEnabled) doReturn false

        assertThat(useCase.execute()).isFalse
    }

    @Test
    fun `disabled when payment toggle disabled`() {
        whenever(yandexPaymentSplit.isEnabled) doReturn true
        whenever(yaPaymentToggle.isEnabled) doReturn false
        whenever(yanSdkToggle.isEnabled) doReturn true

        assertThat(useCase.execute()).isFalse
    }

    @Test
    fun `disabled when exp disabled`() {
        whenever(yandexPaymentSplit.isEnabled) doReturn false
        whenever(yaPaymentToggle.isEnabled) doReturn true
        whenever(yaPaymentToggle.ignoreExperiment) doReturn false
        whenever(yanSdkToggle.isEnabled) doReturn true

        assertThat(useCase.execute()).isFalse
    }
}
