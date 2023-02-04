package ru.yandex.realty.componenttest.env.initializers

import java.util.UUID.randomUUID

import ru.yandex.realty.componenttest.utils.PropertyUtils.setSystemPropertyIfAbsent

trait ComponentTestUsernameInitializer {

  setSystemPropertyIfAbsent("COMPONENT_TEST_USERNAME", s"component-test-${randomUUID().toString}")

}
