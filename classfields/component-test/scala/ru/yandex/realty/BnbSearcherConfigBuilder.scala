package ru.yandex.realty

import ru.yandex.realty.application.BnbSearcherConfig
import ru.yandex.realty.application.ng.DefaultConfigProvider
import ru.yandex.realty.componenttest.env.config.ComponentTestConfigBuilder

object BnbSearcherConfigBuilder extends ComponentTestConfigBuilder[BnbSearcherConfig] {

  override protected def buildConfig(): BnbSearcherConfig = {
    BnbSearcherConfig(DefaultConfigProvider.provideForName("realty-bnb-searcher.component-test"))
  }

}
