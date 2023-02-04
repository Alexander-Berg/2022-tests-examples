package ru.yandex.realty.componenttest.env.initializers

import ru.yandex.realty.componenttest.http.ExternalHttpComponents.ExtdataComponentBasePath
import ru.yandex.realty.componenttest.http.ExternalHttpStubConfigProvider
import ru.yandex.realty.componenttest.utils.PropertyUtils.setSystemPropertyIfAbsent

trait ComponentTestExtdataInitializer {
  self: ExternalHttpStubConfigProvider =>

  {
    setSystemPropertyIfAbsent(
      "COMPONENT_TEST_EXTDATA_URL",
      s"http://${externalHttpStubConfig.host}:${externalHttpStubConfig.port}/$ExtdataComponentBasePath"
    )
  }

}
