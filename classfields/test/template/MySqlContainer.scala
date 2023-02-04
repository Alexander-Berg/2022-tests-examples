package ru.auto.salesman.test.template

import java.util.Collections

import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName
import ru.auto.salesman.test.docker.TestDocker

import scala.slick.jdbc.JdbcBackend.Database
import scala.slick.jdbc.{JdbcBackend, StaticQuery}

object MySqlContainer {

  val testDockerNetworkHost = "mysql-test-docker-network-host"

  private val compatibleImageName = "mysql:5.7.22"
  private val imageName = "registry.yandex.net/vertis/mysql:5.7.22"

  lazy val container: MySQLContainer[Nothing] = {
    val c = new MySQLContainer(
      DockerImageName
        .parse(imageName)
        .asCompatibleSubstituteFor(compatibleImageName)
    ) {

      override def configure(): Unit = {
        optionallyMapResourceParameterAsVolume(
          "TC_MY_CNF",
          "/etc/mysql/conf.d",
          "mysql-default-conf"
        )
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
          "--max_connections=512",
          "--datadir=/tmpfs"
        )
        setStartupAttempts(3)
        setNetwork(TestDocker.network)
        withNetworkAliases(testDockerNetworkHost)
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

  val db: JdbcBackend.DatabaseDef = Database.forURL(
    url = container.getJdbcUrl + queryParams,
    user = "root",
    password = "test",
    driver = "com.mysql.jdbc.Driver"
  )
  db.withTransaction { implicit session =>
    initSql.split(";").foreach(StaticQuery.updateNA(_).execute)
  }
}
