package ru.yandex.realty.componenttest.env.initializers

import ru.yandex.realty.componenttest.http.ExternalHttpComponents.TeleponyComponentBasePath
import ru.yandex.realty.componenttest.http.ExternalHttpStubConfigProvider
import ru.yandex.realty.componenttest.utils.PropertyUtils.setSystemPropertyIfAbsent

trait ComponentTestTeleponyInitializer extends ExternalHttpStubConfigProvider {

  {
    setSystemPropertyIfAbsent(
      "COMPONENT_TEST_TELEPONY_HOST",
      externalHttpStubConfig.host
    )
    setSystemPropertyIfAbsent(
      "COMPONENT_TEST_TELEPONY_PORT",
      externalHttpStubConfig.port.toString
    )
    setSystemPropertyIfAbsent(
      "COMPONENT_TEST_TELEPONY_PATH_PREFIX",
      s"/$TeleponyComponentBasePath"
    )
  }

}
