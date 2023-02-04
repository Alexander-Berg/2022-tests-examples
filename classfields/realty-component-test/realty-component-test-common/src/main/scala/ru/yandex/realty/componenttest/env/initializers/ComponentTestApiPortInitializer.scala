package ru.yandex.realty.componenttest.env.initializers

import ru.yandex.realty.componenttest.utils.PropertyUtils.setSystemPropertyIfAbsent
import ru.yandex.realty.componenttest.utils.RandomPortProvider

trait ComponentTestApiPortInitializer {
  self: RandomPortProvider =>

  val TestHttpPort: Int = getRandomPort()
  val TestOpsPort: Int = getRandomPort()

  {
    setSystemPropertyIfAbsent("COMPONENT_TEST_API_PORT", TestHttpPort.toString)
    setSystemPropertyIfAbsent("COMPONENT_TEST_OPS_PORT", TestOpsPort.toString)
  }

}
