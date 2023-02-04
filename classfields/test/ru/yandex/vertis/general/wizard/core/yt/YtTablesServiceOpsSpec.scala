package ru.yandex.vertis.general.wizard.core.yt

import common.yt.{YsonDecoder, YsonRowEncoder, Yt}
import jdk.jshell.spi.ExecutionControl.NotImplementedException
import ru.yandex.inside.yt.kosher.common.GUID
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.inside.yt.kosher.impl.common.http.Compressor
import zio._
import zio.stream.ZStream
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment
import zio.test.magnolia._

import scala.concurrent.duration.FiniteDuration

object YtTablesServiceOpsSpec extends DefaultRunnableSpec {
  private case class SampleModel(key: Int, value: String)
  private case class SampleRelatedModel(sampleModelKey: Int, anotherValue: Int)

  private val leftTablePath = YPath.simple("//left")
  private val rightTablePath = YPath.simple("//right")

  private val sampleModelGen = DeriveGen[SampleModel]
  private val sampleRelatedModelGen = DeriveGen[SampleRelatedModel]

  private trait TestEnv {

    final protected val tablesImpl: Yt.Tables[Any] = new Yt.Tables[Any] {

      override def read[T: YsonDecoder](path: YPath, timeout: FiniteDuration): ZStream[Any, Throwable, T] =
        for {
          row <- if (path == leftTablePath) leftTableResult else rightTableResult
        } yield row.asInstanceOf[T]

      override def write[T: YsonRowEncoder](
          path: YPath,
          rows: ZStream[Any, Nothing, T],
          timeout: FiniteDuration,
          txIdOpt: Option[GUID],
          compressor: Option[Compressor]): ZIO[Any, Throwable, Unit] = ZIO.fail(new NotImplementedException(""))
    }

    val leftTableResult: ZStream[Any, Throwable, SampleModel]
    val rightTableResult: ZStream[Any, Throwable, SampleRelatedModel]

    protected def joinResult: ZStream[Any, Throwable, (SampleModel, Option[SampleRelatedModel])] =
      tablesImpl.applicationLeftHashJoin[SampleModel, SampleRelatedModel, Int](leftTablePath, _.key)(
        rightTablePath,
        _.sampleModelKey
      )

    def testZ: RIO[Any, TestResult]
  }

  override def spec: ZSpec[TestEnvironment, Any] = suite("TablesSpec")(
    suite("applicationLeftHashJoin")(
      testM("Returns empty chunk, when there is no data in the left table") {
        checkM(Gen.chunkOf(sampleRelatedModelGen)) { relatedModels =>
          val test = new TestEnv {
            override val leftTableResult: ZStream[Any, Throwable, SampleModel] = ZStream.empty
            override val rightTableResult: ZStream[Any, Throwable, SampleRelatedModel] =
              ZStream.fromChunk(relatedModels)

            override def testZ: RIO[Any, TestResult] =
              for {
                joinResult <- joinResult.runCollect
              } yield assert(joinResult)(isEmpty)
          }

          test.testZ
        }
      },
      testM("Returns the data from the left table and None-s, when there is no data in the right table") {
        checkM(Gen.chunkOf(sampleModelGen)) { relatedModels =>
          val test = new TestEnv {
            override val leftTableResult: ZStream[Any, Throwable, SampleModel] = ZStream.fromChunk(relatedModels)
            override val rightTableResult: ZStream[Any, Throwable, SampleRelatedModel] = ZStream.empty

            override def testZ: RIO[Any, TestResult] =
              for {
                joinResult <- joinResult.runCollect
                leftTableValues <- leftTableResult.runCollect
                expectedJoinResult = leftTableValues.map((_, Option.empty[SampleRelatedModel]))
              } yield assert(joinResult)(hasSameElements(expectedJoinResult))
          }

          test.testZ
        }
      },
      testM("""Returns data from left table performing cartesian product with corresponding values from
              |the right table""".stripMargin) {
        val test = new TestEnv {
          override val leftTableResult: ZStream[Any, Throwable, SampleModel] = ZStream.fromChunk(
            Chunk(
              SampleModel(1, "1"),
              SampleModel(2, "2"),
              SampleModel(2, "4"),
              SampleModel(5, "5"),
              SampleModel(7, "7"),
              SampleModel(10, "10"),
              SampleModel(10, "11")
            )
          )
          override val rightTableResult: ZStream[Any, Throwable, SampleRelatedModel] = ZStream.fromChunk(
            Chunk(
              SampleRelatedModel(1, 1),
              SampleRelatedModel(2, 2),
              SampleRelatedModel(3, 3),
              SampleRelatedModel(5, 5),
              SampleRelatedModel(5, 6),
              SampleRelatedModel(10, 10),
              SampleRelatedModel(10, 11)
            )
          )

          override def testZ: RIO[Any, TestResult] =
            for {
              joinResult <- joinResult.runCollect
              expectedJoinResult = Chunk(
                (SampleModel(1, "1"), Some(SampleRelatedModel(1, 1))),
                (SampleModel(2, "2"), Some(SampleRelatedModel(2, 2))),
                (SampleModel(2, "4"), Some(SampleRelatedModel(2, 2))),
                (SampleModel(5, "5"), Some(SampleRelatedModel(5, 5))),
                (SampleModel(5, "5"), Some(SampleRelatedModel(5, 6))),
                (SampleModel(7, "7"), None),
                (SampleModel(10, "10"), Some(SampleRelatedModel(10, 10))),
                (SampleModel(10, "11"), Some(SampleRelatedModel(10, 10))),
                (SampleModel(10, "10"), Some(SampleRelatedModel(10, 11))),
                (SampleModel(10, "11"), Some(SampleRelatedModel(10, 11)))
              )
            } yield assert(joinResult)(hasSameElements(expectedJoinResult))
        }

        test.testZ
      }
    )
  )
}
