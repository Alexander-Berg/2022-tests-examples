package ru.yandex.auto.vin.decoder

import ru.yandex.auto.vin.decoder.extdata.ApiExtDataClient

object TestExtData {

  lazy val providers = {
    ApiExtDataClient.start()
    ApiExtDataClient.Providers
  }
}
