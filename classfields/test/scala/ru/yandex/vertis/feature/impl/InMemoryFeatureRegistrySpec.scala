package ru.yandex.vertis.feature.impl

import ru.yandex.vertis.feature.model.FeatureTypes

/**
  * Base specs for [[InMemoryFeatureRegistry]]
  *
  * @author frenki
  */
class InMemoryFeatureRegistrySpec
  extends TypedFeatureRegistrySpecBase {

  def registry(ft: FeatureTypes): TypedValueFeatureRegistry =
    new InMemoryFeatureRegistry(ft)
}