package ru.yandex.realty.clusterzier.backend.dao

import ru.yandex.realty.application.ng.ExecutionContextProvider
import ru.yandex.realty.application.ng.ydb.{
  YdbAction,
  YdbActionRunner,
  YdbClientSupplier,
  YdbConfig,
  YdbConfigSupplier,
  YdbTemplate
}
import ru.yandex.realty.componenttest.ydb.YdbProvider
import ru.yandex.vertis.ydb.skypper.YdbWrapper

import scala.concurrent.Future

trait TestYdb extends YdbClientSupplier with YdbProvider with ExecutionContextProvider with YdbConfigSupplier {
  lazy val ydbConfig: YdbConfig = buildYdbConfig()

  lazy val actionsRunner = new YdbActionRunner(tableClient)
  lazy val ydbWrapper: YdbWrapper = YdbWrapper(
    dbName = "", // only used in tracing
    tableClient = tableClient,
    tablePrefix = ydbConfig.tablePrefix
  )(ec)

  def clean(tablePrefix: String, tableName: String): Future[Unit] = {
    actionsRunner.run(
      YdbAction.executeRW(YdbTemplate.autoPrefixQuery(tablePrefix, s"DELETE FROM $tableName;")).map(_ => ())
    )
  }

}
