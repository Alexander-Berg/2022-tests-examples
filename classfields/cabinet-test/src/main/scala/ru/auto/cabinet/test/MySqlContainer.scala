package ru.auto.cabinet.test

import java.util.Collections
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName
import slick.jdbc.JdbcBackend
import slick.jdbc.JdbcBackend.Database
import scala.concurrent.duration._
import scala.concurrent.Await

object MySqlContainer {

  private val mySqlVersion = "registry.yandex.net/vertis/mysql:5.7.38"

  lazy val container: MySQLContainer[Nothing] = {
    val c = new MySQLContainer(
      DockerImageName.parse(mySqlVersion).asCompatibleSubstituteFor("mysql")) {

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
          "--datadir=/tmpfs",
          "--skip-ssl"
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
    s"GRANT ALL PRIVILEGES ON *.* TO '${container.getUsername}'; FLUSH PRIVILEGES"

  val queryParams: String =
    "?useUnicode=true&amp;characterEncoding=utf8&amp;autoReconnect=true&amp;useCursorFetch=true&amp;useCompression=true&rewriteBatchedStatements=true&useSSL=false&zeroDateTimeBehavior=convertToNull"

  val db: JdbcBackend.DatabaseDef = Database.forURL(
    url = container.getJdbcUrl + queryParams,
    user = "root",
    password = "test",
    driver = "com.mysql.jdbc.Driver"
  )
  import slick.jdbc.MySQLProfile.api.{offsetDateTimeColumnType => _, _}

  Await.result(
    db.run {
      DBIO.sequence(initSql.split(";").map(s => sqlu"#$s").toList)
    },
    60.seconds
  )
}
