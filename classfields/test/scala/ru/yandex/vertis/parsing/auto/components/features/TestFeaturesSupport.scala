package ru.yandex.vertis.parsing.auto.components.features

import ru.yandex.vertis.parsing.auto.components.bunkerconfig.BunkerConfigAware
import ru.yandex.vertis.parsing.auto.components.parsers.ParsersAware
import ru.yandex.vertis.parsing.auto.features.ParsingFeatures
import ru.yandex.vertis.parsing.features.SimpleFeatures

/**
  * TODO
  *
  * @author aborunov
  */
trait TestFeaturesSupport extends FeaturesAware with ParsersAware with BunkerConfigAware {

  val features: ParsingFeatures = new SimpleFeatures
    with ParsingFeatures
    with ParsersAwareImpl
    with BunkerConfigAwareImpl
}
