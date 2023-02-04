package ru.yandex.realty.db.testcontainers

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import ru.yandex.realty.application.ng.db.DatabaseFactory
import ru.yandex.realty.application.ng.db.MasterSlaveJdbcDatabase2.DbWithProperties
import ru.yandex.realty.db.testcontainers.TestContainerDatasource.DatasourceConfig
import slick.util.AsyncExecutor

import javax.sql.DataSource
import scala.concurrent.duration.FiniteDuration

trait TestContainerDatasource {
  this: TestContainer =>

  def datasourceConfig: DatasourceConfig = DatasourceConfig.DefaultConfig

  final lazy val dataSource: DataSource = {
    val config = new HikariConfig()
    config.setPoolName(container.getDatabaseName)
    config.setDriverClassName(container.getDriverClassName)
    config.setJdbcUrl(container.getJdbcUrl)
    config.setUsername(container.getUsername)
    config.setPassword(container.getPassword)
    datasourceConfig.poolIdleTimeout.map(_.toMillis).foreach(config.setIdleTimeout)
    config.setMinimumIdle(datasourceConfig.minIdleConnections)
    config.setMaximumPoolSize(datasourceConfig.poolMaxSize)
    datasourceConfig.props.foreach {
      case (name, value) => config.addDataSourceProperty(name, value)
    }
    config.setRegisterMbeans(datasourceConfig.registerMBean)

    new HikariDataSource(config)
  }

  final lazy val database: slick.jdbc.JdbcBackend.Database = {
    val executorName = container.getDatabaseName
    val executorMaxThreads: Int = datasourceConfig.poolMaxSize
    val executorMinThreads: Int = executorMaxThreads
    val executorQueueSize = 1000
    val executorMaxConnections = datasourceConfig.poolMaxSize
    val executorRegisterMBeans = datasourceConfig.registerMBean

    val executor = {
      val executor = AsyncExecutor(
        name = executorName,
        minThreads = executorMinThreads,
        maxThreads = executorMaxThreads,
        queueSize = executorQueueSize,
        maxConnections = executorMaxConnections,
        registerMbeans = executorRegisterMBeans
      )
      executor
    }

    DatabaseFactory.create(dataSource, executor, None, containerConfig.databaseName)
  }

  final lazy val databaseWithProperties: DbWithProperties = DbWithProperties(
    database,
    container.getDatabaseName
  )
}

object TestContainerDatasource {

  case class DatasourceConfig(
    minIdleConnections: Int = DatasourceConfig.DefaultMinIdleConnections,
    poolMaxSize: Int = DatasourceConfig.DefaultPoolMaxSize,
    registerMBean: Boolean = DatasourceConfig.DefaultRegisterMBean,
    poolIdleTimeout: Option[FiniteDuration] = DatasourceConfig.DefaultPoolIdleTimeout,
    props: Map[String, String] = DatasourceConfig.DefaultProps
  )

  object DatasourceConfig {
    val DefaultMinIdleConnections: Int = 0

    val DefaultPoolMaxSize: Int = 10

    val DefaultRegisterMBean: Boolean = false

    val DefaultPoolIdleTimeout: Option[FiniteDuration] = None

    val DefaultProps = Map(
      "useConfigs" -> "maxPerformance",
      "useSSL" -> "false",
      "useUnicode" -> "true",
      "useCompression" -> "true",
      "useServerPrepStmts" -> "true",
      "logSlowQueries" -> "true",
      "slowQueryThresholdMillis" -> "500",
      "autoSlowLog" -> "true",
      "logger" -> "com.mysql.jdbc.log.Slf4JLogger",
      "characterEncoding" -> "utf8",
      "TC_DAEMON" -> "true"
      //    "profileSQL" -> "true" // for profiling SQL
      // Uncomment for more detailed performance logging
      //    "explainSlowQueries" -> "true"
      //    "useUsageAdvisor" -> "true"
      //    "profileSQL" -> "true"
    )

    val DefaultConfig: DatasourceConfig = DatasourceConfig()
  }
}
