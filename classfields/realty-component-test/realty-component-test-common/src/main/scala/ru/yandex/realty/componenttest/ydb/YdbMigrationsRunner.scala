package ru.yandex.realty.componenttest.ydb

import com.yandex.ydb.core.Status
import com.yandex.ydb.table.TableClient
import ru.yandex.realty.application.ng.ydb.YdbTemplate
import ru.yandex.realty.application.ng.ydb.YdbTemplate.autoPrefixQuery
import ru.yandex.realty.componenttest.utils.FutureUtils.awaitForResult

import scala.compat.java8.FutureConverters._
import scala.concurrent.{ExecutionContext, Future}

class YdbMigrationsRunner(override val tableClient: TableClient, tablePrefix: String)(
  implicit override val ec: ExecutionContext
) extends YdbTemplate {

  def runMigrations(migrations: Seq[String]): Unit = {
    awaitForResult(Future.traverse(migrations)(executeMigration))
  }

  private def executeMigration(query: String): Future[Unit] = {
    withSession() { session =>
      session
        .executeSchemeQuery(autoPrefixQuery(tablePrefix, query))
        .toScala
        .map(processStatus)
        .map(_ => ())
    }
  }

  private def processStatus(status: Status): Unit = {
    if (status != Status.SUCCESS) {
      throw new RuntimeException(s"Failed to execute migration: status=$status")
    }
  }

}

object YdbMigrationsRunner {

  def runMigrations(tableClient: TableClient, tablePrefix: String, migrations: Seq[String])(
    implicit ec: ExecutionContext
  ): Unit = {
    new YdbMigrationsRunner(tableClient, tablePrefix).runMigrations(migrations)
  }

}
