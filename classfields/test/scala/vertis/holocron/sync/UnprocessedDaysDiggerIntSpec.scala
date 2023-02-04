package vertis.holocron.sync

import broker.core.inner_api.HolocronJobConfig
import common.clients.reactor.testkit.InMemoryReactorClient
import ru.yandex.vertis.broker.model.common.{PartitionPeriods, ReactorPaths}
import vertis.broker.holocron.test.holo.SimpleHoloOffer
import vertis.core.time.DateTimeUtils
import vertis.holocron.sync.UnprocessedDaysDiggerIntSpec.TestEnv
import vertis.proto.converter.YtTableTestHelper
import vertis.yt.model.YPaths.RichYPath
import vertis.yt.storage.{AtomicStorage, YtAttributeStorage}
import vertis.yt.zio.Aliases.{TxEnv, TxRIO}
import vertis.yt.zio.YtZioTest
import vertis.yt.zio.wrappers.YtZio
import vertis.zio.BaseEnv
import zio.RIO

import java.time.{Instant, LocalDate}

class UnprocessedDaysDiggerIntSpec extends YtZioTest with YtTableTestHelper {

  private val descriptor = SimpleHoloOffer.javaDescriptor

  private val ytBasePath = testBasePath
  private val streamPath = ytBasePath.child("test")
  private val eventsPath = streamPath.relative("events/1d")
  private val eodPath = streamPath.child("eod/1d")

  private val jobConfig =
    HolocronJobConfig("test", "test/events", "test/eod", "testMessage", Seq.empty)
  private val reactor = new InMemoryReactorClient

  def testEnv(f: TestEnv => RIO[BaseEnv, _]): Unit = {
    ioTest {
      val env =
        for {
          yt <- ytZio
          _ <- initDirs(yt).toManaged_
          storage <- YtAttributeStorage.withExternalTx
            .fromJson[BaseEnv, EodUnprocessedDays](
              yt,
              eodPath,
              "unprocessed",
              EodUnprocessedDays.Empty
            )
            .toManaged_
        } yield TestEnv(
          yt,
          new UnprocessedDaysDigger(
            yt,
            reactor,
            jobConfig,
            ytBasePath,
            storage
          ),
          storage
        )
      env.use { e =>
        f(e).unit
      }
    }
  }

  // sequential test, each case depends on previous
  "UnprocessedDaysDigger" should {
    "work on empty everything" in testEnv { env =>
      env.digger.syncUnprocessed *> {
        env.inTx(env.unprocessedStorage.get).flatMap { state =>
          check {
            state.unprocessedDays.days shouldBe empty
            state.reactorLastSync shouldBe defined
          }
        }
      }
    }
    "sync from yt if reactorLastSync is empty" in testEnv { env =>
      val eventDay = LocalDate.of(2012, 1, 2)
      val eodDay = LocalDate.of(2022, 3, 2)
      val now = Instant.now()
      for {
        _ <- env.inTx(env.unprocessedStorage.update(_ => (EodUnprocessedDays.Empty, ())))
        _ <- createTable(env.yt, eventDay.toString, descriptor, eventsPath)
        _ <- createTable(env.yt, eodDay.toString, descriptor, eodPath)
        _ <- env.digger.syncUnprocessed
        state <- env.inTx(env.unprocessedStorage.get)
        _ <- check {
          state.reactorLastSync shouldBe defined
          state.reactorLastSync.get should be > now
          state.unprocessedDays.days.keys should contain theSameElementsAs Seq(eventDay, eodDay.plusDays(1))
        }
      } yield ()
    }

    "not sync from yt if reactorLastSync is defined" in testEnv { env =>
      val d1 = LocalDate.of(2014, 3, 2)
      for {
        _ <- createTable(env.yt, d1.toString, descriptor, eventsPath)
        _ <- env.digger.syncUnprocessed
        state <- env.inTx(env.unprocessedStorage.get)
        _ <- check {
          state.reactorLastSync shouldBe defined
          state.unprocessedDays.days.keys should not contain d1
        }
      } yield ()
    }

    "sync from reactor" in testEnv { env =>
      val eventDay = LocalDate.of(2012, 1, 5)
      val eodDay = LocalDate.of(2022, 3, 5)
      val now = Instant.now()
      for {
        _ <- env.inTx(env.unprocessedStorage.update(_ => (EodUnprocessedDays.Empty, ())))
        _ <- createTable(env.yt, eventDay.toString, descriptor, eventsPath)
        _ <- createTable(env.yt, eodDay.toString, descriptor, eodPath)
        partitionPeriod = PartitionPeriods.byDay
        _ <- reactor.createInstanceSimple(
          ReactorPaths.toReactorPath(jobConfig.eventsBase, partitionPeriod),
          "//tmp",
          DateTimeUtils.toInstant(eventDay)
        )
        _ <- env.digger.syncUnprocessed
        state <- env.inTx(env.unprocessedStorage.get)
        _ <- check {
          state.reactorLastSync shouldBe defined
          state.reactorLastSync.get should be > now
          state.unprocessedDays.days.keys should contain(eventDay)
          state.unprocessedDays.days.keys should not contain eodDay
        }
        _ <- reactor.createInstanceSimple(
          ReactorPaths.toReactorPath(jobConfig.eodBase, partitionPeriod),
          "//tmp",
          DateTimeUtils.toInstant(eodDay)
        )
        _ <- env.digger.syncUnprocessed
        _ <- check {
          state.unprocessedDays.days.keys should contain(eventDay)
          state.unprocessedDays.days.keys should contain(eodDay.plusDays(1))
        }
      } yield ()
    }
  }

  private def initDirs(yt: YtZio) = {
    yt.cypress.createDir(None, eodPath, Map.empty) *>
      yt.cypress.createDir(None, eventsPath, Map.empty)
  }
}

object UnprocessedDaysDiggerIntSpec {

  case class TestEnv(
      yt: YtZio,
      digger: UnprocessedDaysDigger,
      unprocessedStorage: AtomicStorage[TxEnv, EodUnprocessedDays]) {

    def inTx[T](f: => TxRIO[BaseEnv, T]): RIO[BaseEnv, T] =
      yt.tx.withTxNoRetry("test")(f)

  }
}
