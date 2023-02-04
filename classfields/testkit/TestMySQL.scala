package common.zio.doobie.testkit

import cats.effect.Blocker
import com.dimafeng.testcontainers.MySQLContainer
import doobie.util.transactor.Transactor
import org.testcontainers.utility.DockerImageName
import zio.blocking.Blocking
import zio.interop.catz._
import zio.{Has, Task, ZIO, ZLayer}

object TestMySQL {
  private val DefaultImageName = DockerImageName.parse("mysql")
  private val DefaultVersion = "8.0.28"

  val managedContainer: ZLayer[Any, Nothing, Has[MySQLContainer]] =
    managedContainer(DefaultVersion)

  def managedContainer(version: String): ZLayer[Any, Nothing, Has[MySQLContainer]] = {
    ZLayer
      .fromAcquireRelease {
        ZIO
          .effectTotal(
            new MySQLContainer(
              mysqlImageVersion = Some(DefaultImageName.withTag(version))
            )
          )
          .tap(c => ZIO.effect(c.start()).orDie)
      }(c => ZIO.effect(c.stop()).orDie)
  }

  val managedTransactor: ZLayer[Blocking, Nothing, Has[Transactor[Task]]] =
    managedTransactor(DefaultVersion)

  def managedTransactor(version: String): ZLayer[Blocking, Nothing, Has[Transactor[Task]]] = {
    val transactor =
      ZLayer.fromServices[MySQLContainer, Blocking.Service, Transactor[Task]] { (container, blocking) =>
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
