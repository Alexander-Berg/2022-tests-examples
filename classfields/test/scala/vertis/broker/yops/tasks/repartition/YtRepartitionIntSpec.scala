package vertis.broker.yops.tasks.repartition

import com.google.protobuf.Message
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.vertis.broker.model.common.PartitionPeriods
import vertis.broker.yops.tasks.repartition.model.{
  RepartitionJob,
  RepartitionStats,
  StreamRepartitionConfig,
  YtRepartitionConf
}
import vertis.broker.yops.tasks.repartition.targeting.{CachedRepartitionTargeting, RepartitionMrTargeting}
import vertis.core.time.DateTimeUtils.toLocalDate
import vertis.yt.model.YtTable
import vertis.yt.zio.Aliases.YtTask
import vertis.yt.zio.wrappers.YtZio
import vertis.zio.BTask
import vertis.zio.test.ZioSpecBase.TestBody
import zio.ZIO
import zio.duration.{durationInt, Duration}

import java.time.{Instant, LocalDate}

class YtRepartitionIntSpec extends RepartitionSpec {

  private val ts = "timestamp"
  private val MaxBatches = 5
  private val MaxDaysInBatch = 7
  private val MaxDays = MaxBatches * MaxDaysInBatch

  override protected val ioTestTimeout: Duration = 10.minutes

  "YtRepartition" should {

    Iterator(false, true).foreach { useFiltering =>
      val withFiltering = if (useFiltering) " withFiltering" else ""

      s"work$withFiltering" in {
        val ts1 = Instant.now()
        val ts2 = Instant.now().minusSeconds(3.day.getSeconds)
        val ts3 = Instant.now().minusSeconds(1.day.getSeconds)
        val messages = Seq(ts1, ts2, ts2, ts2, ts1, ts3).map(messageForTs)
        val days = Seq(ts1, ts2, ts3).map(toLocalDate)
        repartitionTest(messages, days, useFiltering = useFiltering)
      }

      /** Worst case scenario is a mesh of distinct days which splits the source day into so many ranges,
        * we can't process them in one go (or it'll be a week long transaction)
        */
      s"work with max days$withFiltering" in {
        val days = Iterator.iterate(Instant.now())(_.plus(1.day)).take(MaxDaysInBatch * MaxBatches).toSeq
        val messages = days.map(messageForTs)
        repartitionTest(messages, days.map(toLocalDate), useFiltering = useFiltering)
      }

      s"work with too many days$withFiltering" in {
        val days = Iterator.iterate(Instant.now())(_.plus(1.day)).take(MaxDaysInBatch * (MaxBatches + 3)).toSeq
        val messages = days.map(messageForTs)
        repartitionTest(messages, days.take(MaxDays).map(toLocalDate), Some(MaxDays), useFiltering = useFiltering)
      }

      s"continue repartition in case of too many days$withFiltering" in {
        val maxDaysInBatch = 3
        val maxBatches = 2
        val maxDays = maxBatches * maxDaysInBatch
        val days = Iterator.iterate(Instant.now())(_.plus(1.day)).take(3 * maxDays).toSeq
        val messages = days.map(messageForTs)
        val split = (messages.size * 2.5).toInt
        val (firstBatch, secondBatch) = messages.splitAt(split)
        withCustomRepartition(maxDaysInBatch, maxBatches, useFiltering) { env =>
          import env._
          for {
            name <- randomName
            table <- env.createDayTable(name)
            dstPath = tmpPath.child(s"${name}_dst")
            _ <- appendToTable(yt, firstBatch, table.path, descriptor)
            _ <- runRepartition(table.path.parent(), dstPath, firstBatch.size.toLong)
            targetDaysOne <- checkDays(env)(dstPath, days.take(maxDays).map(toLocalDate), "first batch head")
            _ <- validateDailyTables(env)(targetDaysOne)
            _ <- appendToTable(yt, secondBatch, table.path, descriptor)
            _ <- runRepartition(table.path.parent(), dstPath, firstBatch.size.toLong + secondBatch.size.toLong)
            targetDaysTwo <- checkDays(env)(
              dstPath,
              days.take(maxDays * 2).map(toLocalDate),
              "first batch continues"
            )
            _ <- validateDailyTables(env)(targetDaysTwo)
            _ <- runRepartition(table.path.parent(), dstPath, firstBatch.size.toLong + secondBatch.size.toLong)
            targetDaysThree <- checkDays(env)(
              dstPath,
              days.take(maxDays * 3).map(toLocalDate),
              "first batch tail + second batch"
            )
            total <- validateDailyTables(env)(targetDaysThree)
          } yield {
            total shouldBe messages.size
          }
        }
      }
    }
  }

  private def repartitionTest(
      messages: Seq[Message],
      expectedTargetDays: Seq[LocalDate],
      expectedTargetMessages: Option[Int] = None,
      useFiltering: Boolean): Unit =
    withCustomRepartition(useFiltering = useFiltering) { env =>
      import env._
      for {
        name <- randomName
        table <- createDayTable(name)
        dstPath = tmpPath.child(s"${name}_dst")
        _ <- appendToTable(yt, messages, table.path, descriptor)
        _ <- runRepartition(table.path.parent(), dstPath, messages.size.toLong)
        targetDays <- checkDays(env)(dstPath, expectedTargetDays)
        total <- validateDailyTables(env)(targetDays)
      } yield {
        total shouldBe expectedTargetMessages.getOrElse(messages.size)
      }
    }

  private def validateDailyTables(env: Env)(tables: Map[LocalDate, YPath]): BTask[Int] =
    ZIO
      .foreachPar(tables.toSeq) { case (day, path) =>
        env.yt.tables.readAllToYson(None, path).flatMap { rows =>
          check {
            val days =
              rows
                .map(_.getLong(ts))
                .map(v => toLocalDate(Instant.ofEpochMilli(v / 1000)))
                .distinct
            days.size shouldBe 1
            days.head shouldBe day
          }.as(rows.size)
        }
      }
      .map(_.sum)

  private def checkDays(
      env: Env
    )(path: YPath,
      expectedDays: Seq[LocalDate],
      clue: String = ""): BTask[Map[LocalDate, YPath]] =
    env.yt.cypressNoTx.listDays(path) >>= { days =>
      check(clue) {
        days.keySet.toSeq.sorted should contain theSameElementsAs expectedDays
      }.as(days)
    }

  private def withCustomRepartition(
      maxDaysInBatch: Int = MaxDaysInBatch,
      maxBatches: Int = MaxBatches,
      useFiltering: Boolean,
      testTimeout: Duration = ioTestTimeout
    )(io: Env => TestBody): Unit =
    customIoTest(testTimeout) {
      ytZio.use { yt =>
        val targeting = new RepartitionMrTargeting(yt, tmpPath, tmpPath) with CachedRepartitionTargeting
        val repartition =
          new YtRepartition(
            yt,
            targeting,
            tmpPath,
            YtRepartitionConf(maxBatches, maxDaysInBatch, None, useFiltering)
          )
        io(Env(repartition, yt))
      }
    }

  case class Env(repartition: YtRepartition, yt: YtZio) {

    val today: LocalDate = LocalDate.now()

    def createDayTable(name: String, day: LocalDate = today): BTask[YtTable] = {
      createTable(yt, s"$name/1d/$day", descriptor)
    }

    def runRepartition(
        srcBase: YPath,
        dstBase: YPath,
        totalRows: Long,
        day: LocalDate = today): YtTask[Seq[RepartitionStats]] =
      yt.tx.withTx("test") {
        repartition.repartition(
          RepartitionJob(
            StreamRepartitionConfig(
              "test",
              10,
              "timestamp",
              PartitionPeriods.byDay,
              srcBase,
              dstBase,
              "test-dst"
            ),
            srcBase.child(day.toString),
            ts,
            day,
            totalRows
          ),
          dstBase
        )
      }
  }
}
