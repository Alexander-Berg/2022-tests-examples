package common.yt.tests.suites

import common.yt.Yt.Yt
import common.yt.live.operations.BaseReducer.ReduceKey
import common.yt.live.operations.Typing.TableIndex
import common.yt.live.operations.{BaseMapper, BaseReducer, CustomYtTableEntryType}
import common.yt.tests.Typing.YtBasePath
import common.yt.Yt
import common.yt.yson.{YsonDecoder, YsonRowEncoder}
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.inside.yt.kosher.operations.specs.{MapReduceSpec, MapSpec, MapperSpec, ReducerSpec, SortSpec}
import ru.yandex.inside.yt.kosher.tables.YTableEntryType
import zio.stream.ZStream
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment
import zio._
import zio.test.TestAspect.sequential

import java.util.UUID

object OperationsSuite extends YtSuite {

  private val DivModule: Int = 5
  private val MaxElement: Int = 50

  final case class InputData(value: Int)

  final case class MappedData(div: Int, value: Int)

  final case class ReducedData(div: Int, valuesSum: Int)

  implicit val inputType: YTableEntryType[InputData] = CustomYtTableEntryType.default
  implicit val mappedType: YTableEntryType[MappedData] = CustomYtTableEntryType.default
  implicit val reducedType: YTableEntryType[ReducedData] = CustomYtTableEntryType.default

  case class Key(key: Int) extends ReduceKey[Int]

  final class TestMapper extends BaseMapper[InputData, MappedData] {

    override protected def makeMap(input: InputData): ZStream[Any, Throwable, (TableIndex, MappedData)] =
      withDefaultTableIndex {
        ZStream.succeed(MappedData(input.value % DivModule, input.value))
      }
  }

  final class TestReducer extends BaseReducer[MappedData, ReducedData, Key] {

    override protected def makeReduce(
        key: Key,
        input: ZStream[Any, Throwable, MappedData]): ZStream[Any, Throwable, (TableIndex, ReducedData)] =
      withDefaultTableIndex {
        ZStream.fromEffect {
          input.map(_.value).runSum.map(ReducedData(key.key, _))
        }
      }

    override def key(entry: MappedData): Key = Key(entry.div)
  }

  private val TestMapperSpec = MapperSpec
    .builder(new TestMapper)
    .build()

  private val TestReducerSpec = ReducerSpec
    .builder(new TestReducer)
    .build()

  override def ytSuite: ZSpec[TestEnvironment with Yt with Has[YtBasePath], Any] =
    suite("YtOperations should correctly do")(
      mapTest,
      sortTest,
      mapReduceTest
    ) @@ sequential

  private def createTable(namePrefix: String) = {
    for {
      basePath <- ZIO.service[YtBasePath]
      table <- UIO.effectTotal(basePath.child(s"${namePrefix}_${UUID.randomUUID.toString}"))
      _ <- ZIO.accessM[Yt](_.get.cypress.createTable(table, ignoreExisting = false))
    } yield table
  }

  private def fillDataAndGetTable[T: YsonRowEncoder](data: Iterable[T]) =
    createTable("test_input")
      .tap { table =>
        ZIO.accessM[Yt](_.get.tables.write[T](table, ZStream.fromIterable(data)))
      }

  private def readTable[T: YsonDecoder](path: YPath) =
    ZIO.accessM[Yt](_.get.tables.read[T](path).runCollect)

  private def removeTables(paths: YPath*) =
    ZIO.foreach_(paths) { path =>
      ZIO.accessM[Yt](_.get.cypress.remove(path))
    }

  private def operationTest[I: YsonRowEncoder, O: YsonDecoder](
      name: String,
      inputData: Iterable[I],
      expected: Iterable[O]
    )(runOp: (Yt.Operations, YPath, YPath) => Task[Unit]) =
    testM(s"$name operation") {
      for {
        inputTable <- fillDataAndGetTable(inputData)
        outputTable <- createTable(s"${name}_output")
        operations <- ZIO.access[Yt](_.get.operations)
        _ <- runOp(operations, inputTable, outputTable)
        operated <- readTable[O](outputTable)
        _ <- removeTables(inputTable, outputTable)
      } yield assert(operated)(hasSameElements(expected))
    }

  private def mapTest = {
    val input = (0 to MaxElement).map(InputData)
    val expected = input.map(x => MappedData(x.value % DivModule, x.value))

    operationTest(
      "map",
      input,
      expected
    ) { case (ops, inputTable, outputTable) =>
      ops.runMap(
        MapSpec
          .builder()
          .addInputTable(inputTable)
          .addOutputTable(outputTable)
          .setMapperSpec(TestMapperSpec)
          .build()
      )
    }
  }

  private def sortTest = {
    val input = (0 to MaxElement).map(x => MappedData(x % DivModule, x))
    val expected = input.sortBy(_.div)

    operationTest(
      "sort",
      input,
      expected
    ) { case (ops, inputTable, outputTable) =>
      ops.runSort(
        SortSpec
          .builder()
          .addInputTable(inputTable)
          .setOutputTable(outputTable)
          .setSortBy("div")
          .build()
      )
    }
  }

  private def mapReduceTest = {
    val input = (0 to MaxElement).map(InputData)
    val expected = input
      .groupBy(_.value % DivModule)
      .toSeq
      .map { case (div, elems) =>
        ReducedData(div, elems.map(_.value).sum)
      }

    operationTest(
      "mapReduce",
      input,
      expected
    ) { case (ops, inputTable, outputTable) =>
      ops.runMapReduce(
        MapReduceSpec
          .builder()
          .addInputTable(inputTable)
          .addOutputTable(outputTable)
          .setSortBy("div")
          .setReduceBy("div")
          .setMapperSpec(TestMapperSpec)
          .setReducerSpec(TestReducerSpec)
          .build()
      )
    }
  }
}
