package ru.yandex.realty.componenttest.env.initializers

import ru.yandex.realty.componenttest.http.ExternalHttpStubConfigProvider
import ru.yandex.realty.componenttest.utils.PropertyUtils.setSystemPropertyIfAbsent

trait ComponentTestStaffInitializer extends ExternalHttpStubConfigProvider {

  setSystemPropertyIfAbsent(
    "COMPONENT_TEST_STAFF_URL",
    s"${externalHttpStubConfig.url}/staff"
  )

}
