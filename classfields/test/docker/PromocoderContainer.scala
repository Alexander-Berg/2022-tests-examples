package ru.auto.salesman.test.docker

import org.testcontainers.containers.MySQLContainer.MYSQL_PORT
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import ru.auto.salesman.dao.impl.jdbc.database.Database
import ru.auto.salesman.test.template.{MySqlContainer, PromocoderJdbcSpecTemplate}

object PromocoderContainer extends PromocoderJdbcSpecTemplate {

  private val dockerImageName =
    "registry.yandex.net/vertis/yandex-vertis-promocoder-api:0.0.53"

  private val apiPort = 80

  private val container = createContainer()

  container.start()

  val address =
    s"http://${container.getContainerIpAddress}:${container.getMappedPort(apiPort)}"

  private def createContainer(): GenericContainer[Nothing] =
    new GenericContainer(DockerImageName.parse(dockerImageName)) {

      override def configure(): Unit = {
        addExposedPort(apiPort)
        addMysqlEnv()
        addContainerRuntimeProviderEnv()
        addEnv("API_PORT", apiPort.toString)
        setNetwork(TestDocker.network)
      }

      private def addMysqlEnv(): Unit = {
        val mysqlHost = MySqlContainer.testDockerNetworkHost
        def jdbcUrl(database: Database) =
          s"jdbc:mysql://$mysqlHost:$MYSQL_PORT/${database.databaseName}"
        addEnv("AUTORU_MYSQL_URL", jdbcUrl(autoruDatabase))
        addEnv("AUTORU_MYSQL_USERNAME", user)
        addEnv("AUTORU_MYSQL_PASSWORD", password)
        addEnv("REALTY_MYSQL_URL", jdbcUrl(realtyDatabase))
        addEnv("REALTY_MYSQL_USERNAME", user)
        addEnv("REALTY_MYSQL_PASSWORD", password)
        addEnv("AUTORU_USERS_MYSQL_URL", jdbcUrl(autoruUsersDatabase))
        addEnv("AUTORU_USERS_MYSQL_USERNAME", user)
        addEnv("AUTORU_USERS_MYSQL_PASSWORD", password)
      }

      /** ContainerRuntimeProvider из scala-common требует эти переменные
        * окружения, иначе происходит фоллбек на датасорсы, и настройки
        * подключения из базы из других переменных окружения не подхватываются.
        */
      private def addContainerRuntimeProviderEnv(): Unit = {
        addEnv("ENVIRONMENT", "testing")
        addEnv("HOSTNAME", "localhost")
        addEnv("DC", "test")
        addEnv("NOMAD_ALLOC_ID", "test")
        addEnv("NOMAD_ALLOC_INDEX", "0")
      }
    }
}
