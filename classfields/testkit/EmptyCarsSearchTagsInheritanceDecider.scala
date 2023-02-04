package ru.auto.catalog.core.testkit

import ru.auto.catalog.core.model.raw.layers.cars.CarsSearchTagsInheritanceDecider
import ru.auto.catalog.core.model.raw.layers.cars.CarsSearchTagsInheritanceDecider.Decision
import ru.auto.catalog.core.model.raw.layers.cars.configuration.ConfigurationLayerBuilder.ConfigurationSearchTags
import ru.auto.catalog.core.model.raw.layers.cars.mark_model.MarkModelLayerBuilder.{MarkSearchTags, ModelSearchTags}
import ru.auto.catalog.core.model.raw.layers.cars.super_gen.SuperGenLayerBuilder.SuperGenerartionSearchTags

object EmptyCarsSearchTagsInheritanceDecider extends CarsSearchTagsInheritanceDecider {

  override def decide(
      markSearchTags: Set[MarkSearchTags],
      modelSearchTags: Set[ModelSearchTags],
      superGenerationSearchTags: Set[SuperGenerartionSearchTags],
      configurationSearchTags: Set[ConfigurationSearchTags]): Set[Decision] =
    Set.empty
}
