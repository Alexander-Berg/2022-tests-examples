package common.yt.tests

import common.yt.live.operations.BaseReducer.ReduceKey
import common.yt.live.operations.Typing.TableIndex
import common.yt.live.operations.{BaseMapper, BaseReducer, CustomYtTableEntryType}
import common.yt.operations_sugar.{MapReduceConfig, MapperOrReducerConfig, YtOperationsSpecs}
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.inside.yt.kosher.tables.YTableEntryType
import ru.yandex.inside.yt.kosher.common.{DataSize, JavaOptions}
import zio.stream.ZStream
import zio.test.Assertion._
import zio.test._

object YtOperationsSpecBuilderSpec extends DefaultRunnableSpec {

  case class InputRow(i: Int)

  case class TempRow(i: Int)

  case class OutputRow(i: Int)

  case class SimpleReduceKey(key: Int) extends ReduceKey[Int]

  implicit val inputT: YTableEntryType[InputRow] = CustomYtTableEntryType.default
  implicit val tempT: YTableEntryType[TempRow] = CustomYtTableEntryType.default
  implicit val outputT: YTableEntryType[OutputRow] = CustomYtTableEntryType.default

  private lazy val mapperOrReducerConfig =
    MapperOrReducerConfig(
      memoryLimit = DataSize.ZERO,
      memoryReserveFactor = None,
      javaOptions = JavaOptions.empty
    )

  private lazy val config: MapReduceConfig =
    MapReduceConfig(
      inputTables = Seq(YPath.simple("//home/inp")),
      outputTables = Seq(YPath.simple("//home/outp")),
      reduceBy = Seq("column"),
      sortBy = Seq("column"),
      mapJobsCount = Some(100),
      mapperConfig = Some(mapperOrReducerConfig),
      reducerConfig = Some(mapperOrReducerConfig)
    )

  sealed class NoOpMapper extends BaseMapper[InputRow, TempRow] {
    override protected def makeMap(input: InputRow): ZStream[Any, Throwable, (TableIndex, TempRow)] = ZStream.empty
  }

  sealed class NoOpReducer extends BaseReducer[TempRow, OutputRow, SimpleReduceKey] {

    override protected def makeReduce(
        key: SimpleReduceKey,
        input: ZStream[Any, Throwable, TempRow]): ZStream[Any, Throwable, (TableIndex, OutputRow)] =
      ZStream.empty

    override def key(entry: TempRow): SimpleReduceKey = SimpleReduceKey(entry.i)
  }

  // noinspection ScalaUnusedSymbol
  final class NoEmptyConstructorMapper(i: Int) extends NoOpMapper
  final class ValidMapper extends NoOpMapper

  // noinspection ScalaUnusedSymbol
  final class NoEmptyConstructorReducer(i: Int) extends NoOpReducer
  final class ValidReducer extends NoOpReducer

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("YtOperationsRunner")(
      testM("should throw error on invalid mapper") {
        assertM {
          YtOperationsSpecs
            .mapReduceSpec[InputRow, TempRow, OutputRow](
              classOf[NoEmptyConstructorMapper],
              classOf[ValidReducer],
              config
            )
            .run
        }(fails(hasMessage(startsWithString("Couldn't create instance for mapper"))))
      },
      testM("should throw error on invalid reducer") {
        assertM {
          YtOperationsSpecs
            .mapReduceSpec[InputRow, TempRow, OutputRow](
              classOf[ValidMapper],
              classOf[NoEmptyConstructorReducer],
              config
            )
            .run
        }(fails(hasMessage(startsWithString("Couldn't create instance for reducer"))))
      },
      testM("should return spec when mapper and reducer are valid") {
        assertM {
          YtOperationsSpecs
            .mapReduceSpec[InputRow, TempRow, OutputRow](classOf[ValidMapper], classOf[ValidReducer], config)
            .run
        }(succeeds(anything))
      }
    )
}
