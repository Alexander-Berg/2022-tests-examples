package ru.yandex.vertis.parsing.realty.components.features

import ru.yandex.vertis.parsing.features.SimpleFeatures
import ru.yandex.vertis.parsing.realty.components.bunkerconfig.BunkerConfigAware
import ru.yandex.vertis.parsing.realty.components.parsers.ParsersAware
import ru.yandex.vertis.parsing.realty.features.ParsingRealtyFeatures

/**
  * TODO
  *
  * @author aborunov
  */
trait TestFeaturesSupport extends FeaturesAware with ParsersAware with BunkerConfigAware {

  val features: ParsingRealtyFeatures = new SimpleFeatures
    with ParsingRealtyFeatures
    with ParsersAwareImpl
    with BunkerConfigAwareImpl
}
