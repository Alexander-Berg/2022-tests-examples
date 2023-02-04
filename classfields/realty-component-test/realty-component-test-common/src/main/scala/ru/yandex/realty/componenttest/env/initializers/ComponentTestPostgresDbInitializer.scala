package ru.yandex.realty.componenttest.env.initializers

import ru.yandex.realty.componenttest.postgres.PostgresProvider
import ru.yandex.realty.componenttest.utils.PropertyUtils.setSystemPropertyIfAbsent

trait ComponentTestPostgresDbInitializer extends PostgresProvider {

  val TestDatabaseDriveClassName: String = dbContainerConfig.driverClassName
  val TestDatabaseUrl: String = dbContainerConfig.jdbcUrl
  val TestDatabaseUserName: String = dbContainerConfig.username
  val TestDatabaseUserPassword: String = dbContainerConfig.password
  val TestDatabaseName: String = dbContainerConfig.name

  setSystemPropertyIfAbsent("COMPONENT_TEST_DB_DRIVER_CLASSNAME", TestDatabaseDriveClassName)
  setSystemPropertyIfAbsent("COMPONENT_TEST_DB_URL", TestDatabaseUrl)
  setSystemPropertyIfAbsent("COMPONENT_TEST_DB_USERNAME", TestDatabaseUserName)
  setSystemPropertyIfAbsent("COMPONENT_TEST_DB_PASSWORD", TestDatabaseUserPassword)
  setSystemPropertyIfAbsent("COMPONENT_TEST_DB_NAME", TestDatabaseName)

}
