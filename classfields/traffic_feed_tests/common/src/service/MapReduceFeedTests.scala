package ru.vertistraf.traffic_feed_tests.common.service

import common.yt.yson.{YsonDecoder, YsonRowEncoder}
import common.yt.Yt
import common.yt.Yt.Yt
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.inside.yt.kosher.operations.map.Mapper
import ru.yandex.inside.yt.kosher.operations.reduce.Reducer
import ru.yandex.inside.yt.kosher.operations.specs.{MapReduceSpec, MapperSpec, ReducerSpec}
import zio._

import scala.jdk.CollectionConverters._

/**
 *
 * @tparam I - Тип входной таблицы
 * @tparam M - Тип строки после операции map
 * @tparam R - Тип строки после операции reduce
 * @tparam T - Тип, в который стоит сконвертировать результатирующую таблицу локально
 */
abstract class MapReduceFeedTests[
    I: YsonRowEncoder: YsonDecoder: Tag,
    M: YsonRowEncoder: YsonDecoder,
    R: YsonRowEncoder: YsonDecoder,
    T]
  extends BaseYtOperationFeedTests[I, R, T] {

  /** should be defined class, not anonymous*/
  protected def mapper: Mapper[I, M]

  /** should be defined class, not anonymous*/
  protected def reducer: Reducer[M, R]

  protected def reduceAndSortByColumns: Seq[String]

  final override protected def runOperation(inputTable: YPath, outputTable: YPath): RIO[Yt, Unit] =
    ZIO.serviceWith[Yt.Service] { yt =>
      val spec = MapReduceSpec
        .builder()
        .addInputTable(inputTable)
        .addOutputTable(outputTable)
        .setMapperSpec(MapperSpec.builder(mapper).build())
        .setReducerSpec(ReducerSpec.builder(reducer).build())
        .setSortBy(reduceAndSortByColumns.asJava)
        .setReduceBy(reduceAndSortByColumns.asJava)
        .build()

      yt.operations.runMapReduce(spec)
    }
}
