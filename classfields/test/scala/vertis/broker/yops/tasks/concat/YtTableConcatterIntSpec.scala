package vertis.broker.yops.tasks.concat

import common.yt.Yt.Attribute.ModificationTime
import common.yt.Yt.Attributes
import org.scalatest.ParallelTestExecution
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.vertis.broker.model.common.PartitionPeriods.byDay
import ru.yandex.vertis.broker.model.common.ReactorPaths
import vertis.broker.yops.model.PartitionTable
import vertis.broker.yops.tasks.concat.YtTableConcatterIntSpec._
import vertis.broker.yops.tasks.concate.{ConcatConfig, YtTableConcatter}
import vertis.broker.yops.testkit.YtTasksTestSupport
import vertis.broker.yops.testkit.YtTasksTestSupport.{getAllAttributesMap, YtTouchAttribute}
import vertis.yt.model.attributes.YtAttribute
import vertis.yt.util.support.YsonSupport
import vertis.yt.zio.Aliases.YtTask
import vertis.yt.zio.YtZioTest
import vertis.yt.zio.wrappers.YtZio
import vertis.zio.test.ZioSpecBase.TestBody
import vertis.zio.{BTask, BaseEnv}
import zio._
import zio.clock.Clock
import zio.duration.{Duration => ZioDuration}
import java.time.LocalDate
import scala.concurrent.duration._

/** @author kusaeva
  */
class YtTableConcatterIntSpec extends YtZioTest with YtTasksTestSupport with YsonSupport with ParallelTestExecution {

  override def basePath: YPath = testBasePath

  override protected val ioTestTimeout: ZioDuration = ZioDuration.fromScala(5.minute)

  "YtTableConcatter" should {
    "concat if silence period was elapsed" in concatTest(defaultConfig.copy(silence = 100.millis)) { test =>
      import test._

      resources.yt.tx.withTx("silence period") {
        for {
          _ <- sleepSilence
          res <- concat
          _ <- check("concat:") {
            res shouldBe true
          }
        } yield ()
      }
    }
    "not concat if silence period was not elapsed" in concatTest() { test =>
      import test._

      resources.yt.tx.withTx("silence period") {
        for {
          res <- concat
          _ <- check("concat:") {
            res shouldBe false
          }
        } yield ()
      }
    }
    "not change modification time" ignore concatTest(defaultConfig.copy(silence = 100.millis)) { test =>
      import test._

      resources.yt.tx.withTx("concat") {
        for {
          mtime <- allAttributes.map(_.apply(ModificationTime))
          _ <- sleepSilence
          _ <- concat
          newMtime <- allAttributes.map(_.apply(ModificationTime))
          _ <- check("after concat:") {
            newMtime shouldBe mtime
          }
        } yield ()
      }
    }
  }

  def concatTest(
      config: ConcatConfig = defaultConfig,
      day: LocalDate = LocalDate.now(),
      attrs: Seq[YtAttribute] = Nil
    )(testIo: ConcatTest => TestBody): Unit =
    ioTest {
      makeResources.use { resources =>
        import resources._
        for {
          base <- randomName
          ytTable = createDayTable(base, day, attrs)
          _ <- yt.cypressNoTx.createTable(ytTable)
          tableAttrs <- getAllAttributesMap(yt, ytTable.path)
          table = PartitionTable(
            day,
            ytTable.path,
            ReactorPaths.toReactorPath(config.basePath, config.partitioning),
            tableAttrs
          )
          _ <- testIo(ConcatTest(resources, table, day, config.silence))
        } yield ()
      }
    }

  private def makeResources: ZManaged[BaseEnv, Throwable, ConcatTestResources] =
    for {
      yt <- ytResources.map(_.yt)
      concatter = new YtTableConcatter(yt, 128L * 1024 * 1024)
    } yield ConcatTestResources(yt, concatter)
}

object YtTableConcatterIntSpec {

  private val defaultConfig: ConcatConfig =
    ConcatConfig(
      basePath = "subscriptions/notification_event",
      partitioning = byDay,
      silence = 3.hours,
      sortBy = Seq("user"),
      untouchablePeriod = None
    )

  case class ConcatTest(
      resources: ConcatTestResources,
      table: PartitionTable,
      day: LocalDate,
      silence: FiniteDuration) {

    import resources._

    val allAttributes: YtTask[Attributes] = getAllAttributesMap(yt, table.ytPath)

    val modifyTable: YtTask[Unit] =
      zio.clock.instant >>= { now =>
        yt.cypressNoTx.setAttribute(table.ytPath, now)(YtTouchAttribute)
      }

    val concat: BTask[Boolean] =
      allAttributes >>= { freshAttrs =>
        concatter.concat(day, table.copy(attributes = freshAttrs), defaultConfig.copy(silence = silence))
      }

    val sleepSilence: URIO[Clock, Unit] =
      ZIO.sleep(ZioDuration.fromScala(silence.plus(3.millis)))
  }

  case class ConcatTestResources(yt: YtZio, concatter: YtTableConcatter)
}
