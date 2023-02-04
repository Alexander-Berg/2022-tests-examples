package ru.yandex.realty.favorites

import ru.yandex.realty.application.ng.AppConfig
import ru.yandex.realty.componenttest.env.DefaultComponentTestEnvironment
import ru.yandex.realty.favorites.application.FavoritesAppConfig

object FavoritesEnvironment extends DefaultComponentTestEnvironment[FavoritesComponent, FavoritesAppConfig] {

  override protected def buildComponent(): FavoritesComponent = {
    new FavoritesComponent
  }

  override def config: FavoritesAppConfig = {
    FavoritesConfigBuilder.config
  }

  override def appConfig: AppConfig = {
    config.appConfig
  }

}
