package ru.yandex.realty.rent.dao

import ru.yandex.realty.application.ng.ExecutionContextProvider
import ru.yandex.realty.application.ng.ydb.{YdbClientSupplier, YdbConfigSupplier}
import ru.yandex.realty.componenttest.ydb.YdbProvider
import ru.yandex.realty.rent.application.DefaultYdbWrapperSupplier

trait TestYdbSupplier extends YdbProvider with YdbClientSupplier with YdbConfigSupplier with DefaultYdbWrapperSupplier {
  self: ExecutionContextProvider =>

  override lazy val ydbConfig = buildYdbConfig()
}
