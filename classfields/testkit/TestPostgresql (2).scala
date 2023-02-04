package common.zio.doobie.testkit

import cats.effect.Blocker
import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.util.transactor.Transactor
import org.testcontainers.utility.DockerImageName
import zio.blocking.Blocking
import zio.interop.catz._
import zio.{Has, Task, ZIO, ZLayer}

/** Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 20/01/2020
  */
object TestPostgresql {
  private val DefaultImageName = DockerImageName.parse("postgres")
  private val DefaultVersion = "9.6.12" // todo(darl) up default version

  val managedContainer: ZLayer[Any, Nothing, Has[PostgreSQLContainer]] =
    managedContainer(DefaultVersion)

  def managedContainer(version: String): ZLayer[Any, Nothing, Has[PostgreSQLContainer]] =
    ZLayer
      .fromAcquireRelease {
        ZIO
          .effectTotal(
            new PostgreSQLContainer(
              dockerImageNameOverride = Some(DefaultImageName.withTag(version)),
              mountPostgresDataToTmpfs = true
            )
          )
          .tap(c => ZIO.effect(c.start()).orDie)
      }(c => ZIO.effect(c.stop()).orDie)

  val managedTransactor: ZLayer[Blocking, Nothing, Has[Transactor[Task]]] =
    managedTransactor(DefaultVersion)

  def managedTransactor(version: String): ZLayer[Blocking, Nothing, Has[Transactor[Task]]] = {
    val transactor =
      ZLayer.fromServices[PostgreSQLContainer, Blocking.Service, Transactor[Task]] { (container, blocking) =>
        Transactor.fromDriverManager[Task](
          container.driverClassName,
          container.jdbcUrl,
          container.username,
          container.password,
          Blocker.liftExecutionContext(blocking.blockingExecutor.asEC)
        )
      }
    (Blocking.any ++ managedContainer(version)) >>> transactor
  }

}
