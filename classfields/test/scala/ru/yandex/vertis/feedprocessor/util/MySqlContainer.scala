package ru.yandex.vertis.feedprocessor.util

import java.util.Collections
import com.typesafe.config.ConfigFactory
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName
import ru.yandex.vertis.feedprocessor.app.TestApplication
import ru.yandex.vertis.feedprocessor.dao.utils.{Database, DatabaseConfig}

object MySqlContainer extends DummyOpsSupport with TestApplication with Logging {

  private val mysqlVersion = "mysql:5.7.22"
  private val imageName = s"registry.yandex.net/vertis/$mysqlVersion"

  lazy val container: MySQLContainer[Nothing] = {
    val c = new MySQLContainer(DockerImageName.parse(imageName).asCompatibleSubstituteFor(mysqlVersion)) {

      override def configure(): Unit = {
        optionallyMapResourceParameterAsVolume("TC_MY_CNF", "/etc/mysql/conf.d", "mysql-default-conf")
        addExposedPort(MySQLContainer.MYSQL_PORT)
        addEnv("MYSQL_DATABASE", "test")
        addEnv("MYSQL_USER", "test")
        addEnv("MYSQL_PASSWORD", "test")
        addEnv("MYSQL_ROOT_PASSWORD", "test")
        withTmpFs(Collections.singletonMap("/tmpfs", "rw"))
        setCommand(
          "mysqld",
          "--character-set-server=utf8mb4",
          "--sql-mode=NO_ENGINE_SUBSTITUTION",
          "--default-time-zone=+03:00",
          "--datadir=/tmpfs"
        )
        setStartupAttempts(3)
      }

      override def getJdbcUrl: String =
        s"jdbc:mysql://127.0.0.1:${getMappedPort(MySQLContainer.MYSQL_PORT)}"
    }
    c.start()
    c
  }

  def initSql: String =
    s"GRANT ALL PRIVILEGES ON *.* TO '${container.getUsername}'@'%';FLUSH PRIVILEGES"

  val queryParams: String =
    "?useUnicode=true&amp;characterEncoding=utf8&amp;autoReconnect=true&amp;useCursorFetch=true&amp;useCompression=true&rewriteBatchedStatements=true&useSSL=false&zeroDateTimeBehavior=convertToNull"

  val url = MySqlContainer.container.getJdbcUrl

  val configStr =
    s"""
        username = "root"
        password = "test"
        driverClass = "${MySqlContainer.container.getDriverClassName}"
      """
  val config = ConfigFactory.parseString(configStr)
  val dbConfig = DatabaseConfig.fromConfig(url, config, readOnly = false, "test")
  val db0 = new Database(dbConfig)
  initSql.split(";").foreach(db0.jdbc.update)
}
