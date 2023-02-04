package ru.yandex.realty.doobie

import cats.effect.IO
import doobie.util.transactor.Transactor
import doobie.util.update.Update0
import cats.implicits._
import doobie.ConnectionIO
import doobie.implicits._
import ru.yandex.realty.db.testcontainers.TestContainer.ContainerConfig
import ru.yandex.realty.db.testcontainers.{SqlUtils, TestContainer, TestContainerDatasource}
import ru.yandex.realty.monitoring.StubTracedMonitor
import ru.yandex.realty.tracing.Traced

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait DoobieTestDatabase extends TestContainerDatasource {
  this: TestContainer =>

  lazy val doobieDatabase: DoobieDatabase = {
    implicit val ec = ExecutionContext.global

    val masterTransactor = Transactor.fromDataSource[IO](dataSource, ec)
    val replicaTransactor = Transactor.fromDataSource[IO](dataSource, ec)

    val masterDatabaseInstance = DatabaseInstance(masterTransactor, new StubTracedMonitor)
    val replicaDatabseInstance = DatabaseInstance(replicaTransactor, new StubTracedMonitor)

    DoobieDatabase(masterDatabaseInstance, replicaDatabseInstance)
  }

  def transaction[R](dbAction: ConnectionIO[R]): Future[R] =
    doobieDatabase.masterTransaction(_ => dbAction)(Traced.empty)

  def executeSqlScript(path: String): ConnectionIO[Unit] = {
    SqlUtils.loadScript(path) match {
      case Failure(exception) =>
        doobie.free.connection.raiseError(new RuntimeException(s"Can't execute script $path", exception))
      case Success(statements) => statements.map(Update0(_, None).run).toList.sequence.as(())
    }
  }

  override def containerConfig: ContainerConfig = ContainerConfig(
    databasePort = 3306,
    databaseName = "test_db",
    databaseUser = "test_db",
    databasePassword = "test_db"
  )
}
