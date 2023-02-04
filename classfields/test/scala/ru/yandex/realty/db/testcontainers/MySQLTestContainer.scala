package ru.yandex.realty.db.testcontainers

import org.testcontainers.containers.{JdbcDatabaseContainer, MySQLContainer}

import java.util.Collections.singletonMap

trait MySQLTestContainer extends TestContainer with MySQLDatabaseProfile {

  protected val dockerImageName: String = "mysql:5.7"

  protected def mysqldParams: List[String] = List(
    "--character-set-server=utf8",
    "--collation-server=utf8_general_ci",
    "--sql-mode=NO_ENGINE_SUBSTITUTION",
    "--default-time-zone=+3:00",
    "--max_allowed_packet=128M",
    "--max_connections=256",
    "--explicit_defaults_for_timestamp=1",
    "--datadir=/tmpfs"
  )

  override lazy val container: JdbcDatabaseContainer[_] = {
    val c = new MySQLContainer(dockerImageName)

    // container env
    c.addExposedPort(containerConfig.databasePort)
    c.withDatabaseName(containerConfig.databaseName)
    c.withPassword(containerConfig.databasePassword)
    c.withUsername(containerConfig.databaseUser)
    c.withTmpFs(singletonMap("/tmpfs", "rw"))

    c.setCommand(s"mysqld ${mysqldParams.mkString(" ")}")
    c.setStartupAttempts(3)
    sys.addShutdownHook {
      c.close()
    }
    c.start()
    c
  }
}

object MySQLTestContainer {

  trait V8_0 extends MySQLTestContainer {
    override protected val dockerImageName = "mysql:8.0"
    override protected def mysqldParams: List[String] =
      "--log-bin-trust-function-creators=1" ::
        super.mysqldParams
  }
}
