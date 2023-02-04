package ru.yandex.realty.mortgages

import ru.yandex.realty.mortgages.application.MortgagesAppConfig
import ru.yandex.realty.application.ng.DefaultConfigProvider
import ru.yandex.realty.componenttest.env.config.ComponentTestConfigBuilder

object MortgagesConfigBuilder extends ComponentTestConfigBuilder[MortgagesAppConfig] {

  override protected def buildConfig(): MortgagesAppConfig = {
    MortgagesAppConfig(DefaultConfigProvider.provideForName("mortgages.component-test"))
  }

}
