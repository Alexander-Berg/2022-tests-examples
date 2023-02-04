package auto.dealers.calltracking.storage.testkit

import org.testcontainers.containers.{JdbcDatabaseContainer => JavaJdbcDatabaseContainer}
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import _root_.com.dimafeng.testcontainers.SingleContainer
import _root_.com.dimafeng.testcontainers.JdbcDatabaseContainer
import java.time.Duration
import java.time.temporal.ChronoUnit.SECONDS;

private[testkit] class PostgresContainer(
    databaseName: String,
    pgUsername: String,
    pgPassword: String,
    mountPostgresDataToTmpfs: Boolean,
    urlParams: Map[String, String],
    commonJdbcParams: JdbcDatabaseContainer.CommonParams)
  extends JavaJdbcDatabaseContainer(
    new ImageFromDockerfile()
      .withFileFromClasspath("Dockerfile", "Dockerfile")
  ) {

  private val port = 5432

  urlParams.foreach { case (k, v) =>
    urlParameters.put(k, v)
  }

  withExposedPorts(port)
  withStartupTimeoutSeconds(commonJdbcParams.startupTimeout.toSeconds.toInt)
  withConnectTimeoutSeconds(commonJdbcParams.connectTimeout.toSeconds.toInt)
  commonJdbcParams.initScriptPath.foreach(withInitScript)

  if (mountPostgresDataToTmpfs) {
    val tmpfsMount = new java.util.HashMap[String, String]()
    tmpfsMount.put("/var/lib/postgresql/data", "rw")
    withTmpFs(tmpfsMount)
  }

  this.waitStrategy = new LogMessageWaitStrategy()
    .withRegEx(".*database system is ready to accept connections.*\\s")
    .withTimes(2)
    .withStartupTimeout(Duration.of(60, SECONDS));

  override def getTestQueryString(): String = "SELECT 1"

  override def getDriverClassName(): String = "org.postgresql.Driver"

  override def getUsername(): String = pgUsername

  override def getPassword(): String = pgPassword

  override def getJdbcUrl(): String =
    "jdbc:postgresql://" + getHost() + ":" +
      getMappedPort(port) + "/" + databaseName + constructUrlParameters("?", "&")

  override def configure(): Unit = {
    withUrlParam("loggerLevel", "OFF")
    addEnv("POSTGRES_DB", databaseName)
    addEnv("POSTGRES_USER", pgUsername)
    addEnv("POSTGRES_PASSWORD", pgPassword)
  }

  override def waitUntilContainerStarted(): Unit =
    getWaitStrategy().waitUntilReady(this)

}
