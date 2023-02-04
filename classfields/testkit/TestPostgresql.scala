package auto.dealers.calltracking.storage.testkit

import org.testcontainers.containers.{JdbcDatabaseContainer => JavaJdbcDatabaseContainer}
import auto.dealers.calltracking.storage.testkit.PostgresContainer
import com.dimafeng.testcontainers.{JdbcDatabaseContainer, PostgreSQLContainer, SingleContainer}
import doobie.util.transactor.Transactor
import common.zio.doobie.schema
import zio._
import zio.interop.catz._
import zio.blocking.Blocking
import cats.effect.Blocker
import org.testcontainers.utility.DockerImageName

private class TestPostgresql(
    databaseName: Option[String] = None,
    pgUsername: Option[String] = None,
    pgPassword: Option[String] = None,
    mountPostgresDataToTmpfs: Boolean = false,
    urlParams: Map[String, String] = Map.empty,
    commonJdbcParams: JdbcDatabaseContainer.CommonParams = JdbcDatabaseContainer.CommonParams())
  extends SingleContainer[JavaJdbcDatabaseContainer[_]]
  with JdbcDatabaseContainer {

  override val container: JavaJdbcDatabaseContainer[_] =
    new PostgresContainer(
      databaseName.getOrElse("test"),
      pgUsername.getOrElse("test"),
      pgPassword.getOrElse("test"),
      mountPostgresDataToTmpfs,
      urlParams,
      commonJdbcParams
    )

}

object TestPostgresql {

  private val postgresImage = DockerImageName
    .parse("registry.yandex.net/vertis/postgres-12-rum-hunspell:stable")
    .asCompatibleSubstituteFor("postgres")

  val initSchema: RIO[Has[Transactor[Task]], Unit] =
    ZIO.accessM[Has[Transactor[Task]]](xa => schema.InitSchema("/schema.sql", xa.get))

  val managedTransactor: ZLayer[Blocking, Nothing, Has[Transactor[Task]]] = {
    val transactor =
      ZLayer.fromServices[PostgreSQLContainer, Blocking.Service, Transactor[Task]]((container, blocking) =>
        Transactor.fromDriverManager[Task](
          container.driverClassName,
          container.jdbcUrl,
          container.username,
          container.password,
          Blocker.liftExecutionContext(blocking.blockingExecutor.asEC)
        )
      )
    (Blocking.any ++ managedContainer) >>> transactor
  }

  val managedTransactorDockerfile: ZLayer[Blocking, Nothing, Has[Transactor[Task]]] = {
    val transactor =
      ZLayer.fromServices[TestPostgresql, Blocking.Service, Transactor[Task]] { (container, blocking) =>
        Transactor.fromDriverManager[Task](
          container.driverClassName,
          container.jdbcUrl,
          container.username,
          container.password,
          Blocker.liftExecutionContext(blocking.blockingExecutor.asEC)
        )
      }
    (Blocking.any ++ managedContainerDockerfile) >>> transactor
  }

  private lazy val managedContainer: ZLayer[Any, Nothing, Has[PostgreSQLContainer]] =
    ZLayer
      .fromAcquireRelease {
        ZIO
          .effectTotal(
            new PostgreSQLContainer(dockerImageNameOverride = Some(postgresImage), mountPostgresDataToTmpfs = true)
          )
          .tap(c => ZIO.effect(c.start()).orDie)
      }(c => ZIO.effect(c.stop()).orDie)

  private lazy val managedContainerDockerfile: ZLayer[Any, Nothing, Has[TestPostgresql]] =
    ZLayer
      .fromAcquireRelease {
        ZIO
          .effectTotal(new TestPostgresql(mountPostgresDataToTmpfs = true))
          .tap(c => ZIO.effect(c.start()).orDie)
      }(c => ZIO.effect(c.stop()).orDie)

}
