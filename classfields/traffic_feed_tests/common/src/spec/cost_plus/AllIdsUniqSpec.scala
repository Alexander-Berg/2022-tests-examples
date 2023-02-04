package ru.vertistraf.traffic_feed_tests.common.spec.cost_plus

import common.yt.Yt.Yt
import common.yt.live.operations.BaseReducer.ReduceKey
import common.yt.live.operations.Typing.TableIndex
import common.yt.live.operations.{BaseMapper, BaseReducer, CustomYtTableEntryType}
import ru.vertistraf.common.model.yml.ExportedYmlOffer
import ru.vertistraf.common.util.yt.YtTable
import ru.vertistraf.traffic_feed_tests.common.model.yt.YtTempDir.YtTempDir
import ru.vertistraf.traffic_feed_tests.common.service.MapReduceFeedTests
import ru.vertistraf.traffic_feed_tests.common.spec.cost_plus.AllIdsUniqSpecModels.{YmlIdSource, YmlIdSources}
import ru.yandex.inside.yt.kosher.operations.map.Mapper
import ru.yandex.inside.yt.kosher.operations.reduce.Reducer
import ru.yandex.inside.yt.kosher.tables.YTableEntryType
import zio.{Cause, Has, Task}
import zio.stream.ZStream
import zio.test.{failed, ZSpec}

object AllIdsUniqSpec extends MapReduceFeedTests[ExportedYmlOffer, YmlIdSource, YmlIdSources, Seq[YmlIdSources]] {

  final override protected def mapper: Mapper[ExportedYmlOffer, YmlIdSource] =
    new AllIdsUniqSpecModels.AllIdsUniqSpecMapper

  final override protected def reducer: Reducer[YmlIdSource, YmlIdSources] =
    new AllIdsUniqSpecModels.AllIdsUniqSpecReducer

  final override protected def reduceAndSortByColumns: Seq[String] = Seq("id")

  final override protected def suiteLabel: String = "AllIdsUniqSpec"

  final override protected def consumeRows(rows: ZStream[Any, Throwable, YmlIdSources]): Task[Seq[YmlIdSources]] =
    rows.runCollect

  final override protected def suiteTests(
      operationResult: Seq[
        YmlIdSources
      ]): Task[Iterable[ZSpec[Has[YtTable[ExportedYmlOffer]] with Yt with Has[YtTempDir], Any]]] =
    Task.effectTotal {

      operationResult.map { res =>
        testM(s"offer ${res.id} should be only in one file") {
          failed(Cause.fail(s"Duplications found at ${res.files.mkString("[", ",", "]")}"))
        }
      }

    }
}

object AllIdsUniqSpecModels {
  case class YmlIdSource(id: String, file: String)

  case class YmlIdSources(id: String, files: List[String])

  case class Key(key: String) extends ReduceKey[String]

  implicit val sourceType: YTableEntryType[YmlIdSource] = CustomYtTableEntryType.default
  implicit val sourcesType: YTableEntryType[YmlIdSources] = CustomYtTableEntryType.default

  class AllIdsUniqSpecMapper extends BaseMapper[ExportedYmlOffer, YmlIdSource] {

    override protected def makeMap(input: ExportedYmlOffer): ZStream[Any, Throwable, (TableIndex, YmlIdSource)] =
      withDefaultTableIndex(ZStream.succeed(YmlIdSource(input.offer.id, input.fileName)))
  }

  class AllIdsUniqSpecReducer extends BaseReducer[YmlIdSource, YmlIdSources, Key] {

    override protected def makeReduce(
        key: Key,
        input: ZStream[Any, Throwable, YmlIdSource]): ZStream[Any, Throwable, (TableIndex, YmlIdSources)] =
      withDefaultTableIndex {
        ZStream.fromEffect {
          input.runCollect
            .map {
              case result if result.size < 2 => None
              case result => Some(YmlIdSources(key.key, result.map(_.file).toList))
            }
        }.collectSome
      }

    override def key(entry: YmlIdSource): Key = Key(entry.id)
  }

}
