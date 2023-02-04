package ru.yandex.realty.favorites

import ru.yandex.realty.application.ng.DefaultConfigProvider
import ru.yandex.realty.componenttest.env.config.ComponentTestConfigBuilder
import ru.yandex.realty.favorites.application.FavoritesAppConfig

object FavoritesConfigBuilder extends ComponentTestConfigBuilder[FavoritesAppConfig] {

  override protected def buildConfig(): FavoritesAppConfig = {
    FavoritesAppConfig(DefaultConfigProvider.provideForName("favorites.component-test"))
  }

}
