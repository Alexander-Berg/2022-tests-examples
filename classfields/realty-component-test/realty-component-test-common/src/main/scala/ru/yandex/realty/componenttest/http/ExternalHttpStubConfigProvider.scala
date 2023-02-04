package ru.yandex.realty.componenttest.http

import ru.yandex.realty.componenttest.utils.RandomPortProvider

trait ExternalHttpStubConfigProvider {

  def externalHttpStubConfig: ExternalHttpStubConfig

}

trait DefaultExternalHttpStubConfigProvider extends ExternalHttpStubConfigProvider {
  self: RandomPortProvider =>

  override lazy val externalHttpStubConfig: ExternalHttpStubConfig =
    ExternalHttpStubConfig(
      host = "localhost",
      port = getRandomPort()
    )

}
