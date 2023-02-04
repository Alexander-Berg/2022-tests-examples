package ru.auto.api.services.web

import ru.auto.api.features.FeatureManager
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

trait MockedFeatureManager extends MockitoSupport {
  private val coeff: Feature[Float] = mock[Feature[Float]]
  when(coeff.value).thenReturn(1.1f)

  protected val featureManager: FeatureManager = mock[FeatureManager]
  when(featureManager.dealerBoostCoefficient).thenReturn(coeff)
}
