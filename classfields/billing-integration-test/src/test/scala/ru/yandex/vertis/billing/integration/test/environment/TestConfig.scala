package ru.yandex.vertis.billing.integration.test.environment

import com.typesafe.config.{ConfigFactory, ConfigParseOptions, ConfigResolveOptions}
import ru.yandex.vertis.billing.dao.impl.jdbc.JdbcContainerSpec

object TestConfig {

  def load(databaseName: String) = {
    val databaseUrl = JdbcContainerSpec.getUrl(databaseName)
    val properties = ConfigFactory.parseString(s"""
         |AUTORU_MYSQL_READ_URL = "$databaseUrl"
         |AUTORU_MYSQL_READ_USERNAME = test
         |AUTORU_MYSQL_READ_PASSWORD = test
         |AUTORU_MYSQL_WRITE_URL = "$databaseUrl"
         |AUTORU_MYSQL_WRITE_USERNAME = test
         |AUTORU_MYSQL_WRITE_PASSWORD = test
         |AUTORU_BALANCE_MYSQL_READ_URL = "$databaseUrl"
         |AUTORU_BALANCE_MYSQL_READ_USER = test
         |AUTORU_BALANCE_MYSQL_READ_PASSWORD = test
         |AUTORU_BALANCE_MYSQL_WRITE_URL = "$databaseUrl"
         |AUTORU_BALANCE_MYSQL_WRITE_USER = test
         |AUTORU_BALANCE_MYSQL_WRITE_PASSWORD = test
         |""".stripMargin)

    ConfigFactory
      .load(
        "test-storage.conf",
        ConfigParseOptions.defaults(),
        ConfigResolveOptions.defaults().setAllowUnresolved(true)
      )
      .withFallback(properties)
      .resolve()
  }
}
