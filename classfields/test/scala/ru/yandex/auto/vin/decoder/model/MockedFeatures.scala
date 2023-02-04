package ru.yandex.auto.vin.decoder.model

import ru.yandex.auto.vin.decoder.model.data_provider.GibddDataProvider
import ru.yandex.auto.vin.decoder.utils.features.{
  CarfaxFeatures,
  GibddProvidersTrafficDistributionFeature => GibddFeature
}
import ru.yandex.auto.vin.decoder.utils.features.TrafficDistributionFeature.ProviderWeights
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

trait MockedFeatures {
  this: MockitoSupport =>
  val features: CarfaxFeatures = mock[CarfaxFeatures]

  val enabledFeature: Feature[Boolean] = Feature("enabled", _ => true)
  val disabledFeature: Feature[Boolean] = Feature("disabled", _ => false)

  def gibddTrafficDistributionFeature(
      distribution: ProviderWeights[GibddDataProvider] = GibddFeature.defaultValue) =
    new Feature[ProviderWeights[GibddDataProvider]] {
      def name: String = GibddFeature.name

      def value: ProviderWeights[GibddDataProvider] = distribution
    }
}
