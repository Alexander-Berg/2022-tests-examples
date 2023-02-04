package ru.yandex.auto.vin.decoder.scheduler

import ru.yandex.auto.vin.decoder.utils.features.CarfaxFeatures
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

trait MockedFeatures {
  this: MockitoSupport =>
  val features: CarfaxFeatures = mock[CarfaxFeatures]

  val enabledFeature: Feature[Boolean] = Feature("enabled", _ => true)
  val disabledFeature: Feature[Boolean] = Feature("disabled", _ => false)
}
