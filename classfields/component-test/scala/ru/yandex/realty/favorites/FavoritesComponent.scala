package ru.yandex.realty.favorites

import ru.yandex.realty.favorites.application.FavoritesApp
import ru.yandex.realty.favorites.backend.link.ShortLinkGenerator

class FavoritesComponent extends FavoritesApp(FavoritesConfigBuilder.config) {

  def getShortLinkGenerator(): ShortLinkGenerator = {
    shortLinkGenerator
  }

}
