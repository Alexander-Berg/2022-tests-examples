package ru.vertistraf.traffic_feed_tests.common.service

import common.yt.yson.{YsonDecoder, YsonRowEncoder}
import common.yt.Yt
import common.yt.Yt.Yt
import ru.vertistraf.common.util.yt.YtTable
import ru.vertistraf.traffic_feed_tests.common.model.yt.YtTempDir.YtTempDir
import ru.yandex.inside.yt.kosher.cypress.YPath
import zio._
import zio.clock.Clock
import zio.stream.ZStream
import zio.test.{TestFailure, ZSpec}

import java.util.UUID

/**
 * @tparam I - тип строки входной таблицы
 * @tparam O - тип строки выходной таблицы, после операции
 * @tparam T - тип, в которой преобразовать строки таблицы локально
 */
abstract class BaseYtOperationFeedTests[I: YsonRowEncoder: YsonDecoder: Tag, O: YsonRowEncoder: YsonDecoder, T]
  extends FeedTestSpec[Has[YtTable[I]] with Yt with Has[YtTempDir] with Clock] {

  protected def suiteLabel: String

  protected def runOperation(inputTable: YPath, outputTable: YPath): RIO[Yt with Clock, Unit]

  protected def consumeRows(rows: ZStream[Any, Throwable, O]): Task[T]

  protected def suiteTests(operationResult: T): Task[Iterable[ZSpec[Has[YtTable[I]] with Yt with Has[YtTempDir], Any]]]

  override def spec: ZSpec[Has[YtTable[I]] with Yt with Has[YtTempDir] with Clock, Any] =
    suiteM(suiteLabel) {
      (for {
        ytTemp <- ZIO.service[YtTempDir]
        inputTable <- ZIO.service[YtTable[I]]
        yt <- ZIO.service[Yt.Service]
        outputTable <- Task.effect(ytTemp.child(UUID.randomUUID().toString))
        _ <- runOperation(inputTable.path, outputTable)
        consumed <- consumeRows(yt.tables.read[O](outputTable))
        tests <- suiteTests(consumed)
      } yield tests).mapError(e => TestFailure.Runtime(Cause.fail(e)))
    }
}
