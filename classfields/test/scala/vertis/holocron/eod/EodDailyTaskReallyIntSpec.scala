package vertis.holocron.eod

import broker.core.inner_api.HolocronJobConfig
import com.google.protobuf.Descriptors.Descriptor
import common.clients.reactor.testkit.InMemoryReactorClient
import common.zio.logging.Logging
import org.scalatest.Assertion
import ru.vertis.holocron.common.utils.Action
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.vertis.broker.model.common.PartitionPeriods
import ru.yandex.vertis.broker.sync.UnprocessedDay
import ru.yandex.vertis.proto.util.scalaPb.ScalaPbHelp
import vertis.broker.holocron.test.holo.SimpleHoloOffer
import vertis.core.time.DateTimeUtils
import vertis.proto.converter.YtTableTestHelper
import vertis.yql.container.RealYqlTest
import vertis.yt.model.YPaths.RichYPath
import vertis.yt.zio.Aliases.TxEnv
import vertis.yt.zio.reactor.model.YtPartitionInstance
import vertis.yt.zio.reactor.model.YtPartitionInstance.YtPartitionReactorInstance
import vertis.yt.zio.reactor.{ReactorSynchronizer, TestReactorSynchronizer}
import vertis.yt.zio.wrappers.YtZio
import vertis.zio.BTask
import vertis.zio.test.ZioSpecBase.TestBody
import vertis.zio.yql.YqlClient
import zio.ZManaged
import zio.duration.{durationInt, Duration}

import java.time.{Instant, LocalDate}

class EodDailyTaskReallyIntSpec extends RealYqlTest with YtTableTestHelper {

  override protected val ioTestTimeout: Duration = 10.minutes

  override protected val tmpPath: YPath = testBasePath
  private val partitionPeriod = PartitionPeriods.byDay
  private val eodPath = tmpPath.relative(s"eod/${partitionPeriod.short}")
  private val eventsPath = tmpPath.child(s"events/${partitionPeriod.short}")

  private val descriptor: Descriptor = SimpleHoloOffer.javaDescriptor

  case class TestEnv(yt: YtZio, yqlClient: YqlClient, synchro: ReactorSynchronizer[TxEnv, YtPartitionInstance]) {

    private val task = new EodDailyTaskImpl(
      yt,
      HolocronJobConfig(
        "test",
        eventsPath.parent().name(),
        eodPath.parent.name(),
        "testMessage",
        Seq("id")
      ),
      yqlClient,
      tmpPath,
      synchro
    )

    def runEodCalc(
        day: LocalDate,
        events: Seq[SimpleHoloOffer],
        eod: Seq[SimpleHoloOffer]): BTask[(Seq[SimpleHoloOffer], Option[UnprocessedDay])] = {
      for {
        _ <- createTable(
          yt,
          day.toString,
          descriptor,
          eventsPath,
          events.map(ScalaPbHelp.toDynamic[SimpleHoloOffer])
        )
        _ <- createTable(
          yt,
          day.minusDays(1).toString,
          descriptor,
          eodPath,
          eod.map(ScalaPbHelp.toDynamic[SimpleHoloOffer])
        )
        result <- justCalcEod(day)
      } yield result
    }

    def justCalcEod(day: LocalDate): BTask[(Seq[SimpleHoloOffer], Option[UnprocessedDay])] = {
      for {
        nextDay <- task.compute(UnprocessedDay(day, 0))
        result <- readTable[SimpleHoloOffer](yt, eodPath.dayChild(day))
      } yield result.map(g => SimpleHoloOffer.parseFrom(g.toByteArray)) -> nextDay
    }
  }

  private def testEnv(f: TestEnv => TestBody): Unit = ioTest {
    val env = for {
      yt <- ytZio
      yql <- YqlClient.make(yqlConfig)
      log <- ZManaged.service[Logging.Service]
      reactorClient = new InMemoryReactorClient
      synchro <- {
        implicit val reactorInstance: YtPartitionReactorInstance = new YtPartitionReactorInstance(eodPath.toString)
        TestReactorSynchronizer
          .create[YtPartitionInstance](
            reactorClient,
            "/test",
            log
          )
          .toManaged_
      }
    } yield TestEnv(yt, yql, synchro)
    env.use(f)
  }

  "EodDailYtTask" should {

    "simply keep the last version of offer" in testEnv { env =>
      val day = LocalDate.now().minusDays(1)
      val oldState = SimpleHoloOffer(
        id = "1",
        eventTimestamp = toTimestamp(Instant.now().minusSeconds(30)),
        action = Action.UPDATE,
        data = "old one"
      )
      val newState = oldState.copy(
        eventTimestamp = toTimestamp(Instant.now()),
        data = "some value"
      )
      val events = Seq(
        oldState,
        newState
      )
      env
        .runEodCalc(
          day,
          events,
          Nil
        )
        .checkResult { case (result, nextDay) =>
          checkTheSameEod(day, result, Seq(newState))
          nextDay.map(_.day) shouldBe Some(day.plusDays(1))
        }
    }

    "work with empty result" in testEnv { env =>
      val day = LocalDate.now().minusDays(1)
      val oldState = SimpleHoloOffer(
        id = "1",
        eventTimestamp = toTimestamp(Instant.now().minusSeconds(30)),
        action = Action.UPDATE,
        data = "old one"
      )
      val newState = oldState.copy(
        eventTimestamp = toTimestamp(Instant.now()),
        data = "some value",
        action = Action.DEACTIVATE
      )
      val events = Seq(
        oldState,
        newState
      )
      env
        .runEodCalc(
          day,
          events,
          Nil
        )
        .checkResult { case (result, nextDay) =>
          result shouldBe empty
          nextDay.map(_.day) shouldBe Some(day.plusDays(1))
        }
    }

    "remove offer on deactivate" in testEnv { env =>
      val day = LocalDate.now().minusDays(2)
      val oldState = SimpleHoloOffer(
        id = "1",
        eventTimestamp = toTimestamp(Instant.ofEpochMilli(EodQueries.eodLastSecondMicros(day.minusDays(1)) / 1000)),
        action = Action.UPDATE,
        data = "old one"
      )
      val otherOffer = SimpleHoloOffer(
        id = "2",
        eventTimestamp = toTimestamp(Instant.ofEpochMilli(EodQueries.eodLastSecondMicros(day.minusDays(1)) / 1000)),
        action = Action.UPDATE,
        data = "some other offer"
      )
      val newState = oldState.copy(
        eventTimestamp = toTimestamp(DateTimeUtils.toInstant(day).plusSeconds(234)),
        action = Action.DEACTIVATE
      )
      env
        .runEodCalc(
          day,
          Seq(newState),
          Seq(oldState, otherOffer)
        )
        .checkResult { case (result, nextDay) =>
          checkTheSameEod(day, result, Seq(otherOffer))
          nextDay.map(_.day) shouldBe Some(day.plusDays(1))
        }
    }

    "rewrite eod if new event was added" in testEnv { env =>
      val now = Instant.now()
      val day = LocalDate.now().minusDays(3)
      val oldState = SimpleHoloOffer(
        id = "1",
        eventTimestamp = toTimestamp(now.minusSeconds(10.hours.toSeconds)),
        action = Action.UPDATE,
        data = "old one"
      )

      val newOldState = oldState.copy(
        eventTimestamp = toTimestamp(now.minusSeconds(15)),
        data = "no so old value"
      )
      val newState = oldState.copy(
        eventTimestamp = toTimestamp(Instant.now()),
        data = "some value"
      )
      for {
        _ <- env
          .runEodCalc(day, Seq(oldState, newOldState), Nil)
          .checkResult { case (result, nextDay) =>
            checkTheSameEod(day, result, Seq(newOldState))
            nextDay.map(_.day) shouldBe Some(day.plusDays(1))
          }
        _ <- appendToTable(env.yt, Seq(ScalaPbHelp.toDynamic(newState)), eventsPath.dayChild(day), descriptor)
        _ <- env.justCalcEod(day).checkResult { case (result, nextDay) =>
          checkTheSameEod(day, result, Seq(newState))
          nextDay.map(_.day) shouldBe Some(day.plusDays(1))
        }
      } yield ()
    }

    "not rewrite eod if it's checksum is the same" in testEnv { env =>
      val now = Instant.now()
      val day = LocalDate.now().minusDays(4)
      val oldState = SimpleHoloOffer(
        id = "1",
        eventTimestamp = toTimestamp(now.minusSeconds(30)),
        action = Action.UPDATE,
        data = "old one"
      )

      val newOldState = oldState.copy(
        eventTimestamp = toTimestamp(now.minusSeconds(15)),
        data = "no so old value"
      )
      val newState = oldState.copy(
        eventTimestamp = toTimestamp(Instant.now()),
        data = "some value"
      )
      for {
        _ <- env
          .runEodCalc(day, Seq(oldState, newState), Nil)
          .checkResult { case (result, nextDay) =>
            checkTheSameEod(day, result, Seq(newState))
            nextDay.map(_.day) shouldBe Some(day.plusDays(1))
          }
        _ <- appendToTable(env.yt, Seq(ScalaPbHelp.toDynamic(newOldState)), eventsPath.dayChild(day), descriptor)
        _ <- env.justCalcEod(day).checkResult { case (result, nextDay) =>
          checkTheSameEod(day, result, Seq(newState))
          nextDay shouldBe None
        }
      } yield ()
    }
  }

  private def checkTheSameEod(day: LocalDate, eod: Seq[SimpleHoloOffer], expected: Seq[SimpleHoloOffer]): Assertion = {
    val ts = toTimestamp(Instant.ofEpochMilli(EodQueries.eodLastSecondMicros(day) / 1000))
    eod.size shouldBe expected.size
    eod should contain theSameElementsAs expected.map(_.copy(eventTimestamp = ts))
  }

}
