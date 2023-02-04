package ru.yandex.realty.componenttest.env.initializers

import ru.yandex.realty.componenttest.http.ExternalHttpStubConfigProvider
import ru.yandex.realty.componenttest.utils.PropertyUtils.setSystemPropertyIfAbsent

trait ComponentTestTelegramInitializer extends ExternalHttpStubConfigProvider {

  setSystemPropertyIfAbsent(
    "COMPONENT_TEST_TELEGRAM_URL",
    s"${externalHttpStubConfig.url}/telegram"
  )

}
