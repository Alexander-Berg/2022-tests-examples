package ru.yandex.realty.componenttest.env.initializers

import java.nio.file.Paths

import ru.yandex.realty.application.ng.ExecutionContextProvider
import ru.yandex.realty.application.ng.ydb.{YdbClientSupplier, YdbConfig, YdbConfigSupplier, YdbEndpoint, YdbHostPort}
import ru.yandex.realty.componenttest.utils.PropertyUtils.setSystemPropertyIfAbsent
import ru.yandex.realty.componenttest.utils.ResourceUtils
import ru.yandex.realty.componenttest.ydb.{YdbMigrationsRunner, YdbProvider}

trait ComponentTestYdbInitializer extends YdbProvider with YdbClientSupplier with YdbConfigSupplier {
  self: ExecutionContextProvider =>

  override lazy val ydbConfig: YdbConfig = buildYdbConfig()

  ydbConfig.address match {
    case YdbEndpoint(endpoint, database) =>
      setSystemPropertyIfAbsent("COMPONENT_TEST_YDB_ENDPOINT", endpoint)
      setSystemPropertyIfAbsent("COMPONENT_TEST_YDB_DATABASE", database)
    case YdbHostPort(host, port) =>
      setSystemPropertyIfAbsent("COMPONENT_TEST_YDB_HOST", host)
      setSystemPropertyIfAbsent("COMPONENT_TEST_YDB_PORT", port.toString)
  }

  setSystemPropertyIfAbsent("COMPONENT_TEST_YDB_TOKEN", ydbConfig.token)
  setSystemPropertyIfAbsent("COMPONENT_TEST_YDB_TABLE_PREFIX", ydbConfig.tablePrefix)

  def migrations: Seq[String] = {
    val defaultMigrationsFolder = "/db/migration"
    ResourceUtils
      .listResources(defaultMigrationsFolder)
      .sorted
      .map(Paths.get(defaultMigrationsFolder, _).toString)
      .flatMap(ResourceUtils.getResourceAsString)
  }

  schemeClient.makeDirectory(ydbConfig.tablePrefix).join()
  YdbMigrationsRunner.runMigrations(tableClient, ydbConfig.tablePrefix, migrations)

}
