package ru.yandex.realty.componenttest.postgres

import com.dimafeng.testcontainers.PostgreSQLContainer
import ru.yandex.realty.application.ng.db.{
  DatabaseConfig,
  DatabaseFactory,
  HikariDataSourceFactory,
  SlickAsyncExecutorFactory
}
import ru.yandex.realty.componenttest.utils.RandomPortProvider

import scala.util.Try

trait PostgresProvider extends RandomPortProvider {

  private val Container: Try[PostgreSQLContainer] =
    Try {
      val c = PostgreSQLContainer()
      c.container.start()
      c.container.setStartupAttempts(3)
      c.container.start()
      sys.addShutdownHook {
        c.container.close()
      }
      c
    }

  {
    val database = createDatabase(databaseConfig(Container.get.container.getDatabaseName))
  }

  private def databaseConfig(databaseName: String): DatabaseConfig =
    DatabaseConfig(
      url = Container.get.jdbcUrl,
      username = Container.get.username,
      password = Container.get.password,
      driverClassName = Container.get.driverClassName,
      poolSize = 10,
      queueSize = 1000,
      minIdleConnections = 0,
      idleTimeout = None,
      properties = DatabaseConfig.DefaultProps + ("TC_DAEMON" -> "true") + ("profileSQL" -> "true"),
      registerMBeans = false,
      name = databaseName
    )

  private def createDatabase(config: DatabaseConfig) = {
    val ds = new HikariDataSourceFactory().createDataSource(config)
    val executor = SlickAsyncExecutorFactory.create(config)
    DatabaseFactory.create(ds, executor, None, config.name)
  }

  protected val dbContainerConfig: DatabaseContainerConfig =
    DatabaseContainerConfig(Container.get)

  case class DatabaseContainerConfig(
    driverClassName: String,
    jdbcUrl: String,
    username: String,
    password: String,
    name: String
  )

  private object DatabaseContainerConfig {

    def apply(container: PostgreSQLContainer): DatabaseContainerConfig =
      DatabaseContainerConfig(
        driverClassName = container.driverClassName,
        jdbcUrl = container.jdbcUrl,
        username = container.username,
        password = container.password,
        name = container.container.getDatabaseName
      )

  }

}
