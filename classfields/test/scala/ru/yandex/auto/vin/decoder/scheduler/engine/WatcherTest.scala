package ru.yandex.auto.vin.decoder.scheduler.engine

import auto.carfax.common.storages.zookeeper.CommonTokenDistributionHelper
import org.scalatest._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import ru.yandex.auto.vin.decoder.model.LicensePlate
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.scheduler.models.{DefaultDelay, Delay, WatchingStateUpdate}
import ru.yandex.auto.vin.decoder.scheduler.storage.WatchingDao
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.duration._

class WatcherTest extends AnyWordSpecLike with MockitoSugar with BeforeAndAfter with Matchers {

  private val engine = mock[WatchingEngine[LicensePlate, CompoundState]]
  private val tokenDHelper = mock[CommonTokenDistributionHelper]
  private val updateDao = mock[WatchingDao[LicensePlate, CompoundState]]

  def createCompoundState(lastVisit: Long): CompoundState = {
    CompoundState
      .newBuilder()
      .setLastVisited(lastVisit)
      .build()
  }

  def createUpdate(lastVisit: Long, delay: Delay): WatchingStateUpdate[CompoundState] = {
    WatchingStateUpdate(createCompoundState(lastVisit), delay)
  }

  implicit val m = TestOperationalSupport

  "watcher" should {
    "not correct visit if it was set to infiniti" in {
      val watcher = new Watcher[LicensePlate, CompoundState]("vin", updateDao, tokenDHelper, engine, 2)
      val upd = createUpdate(0, DefaultDelay(5.millis))
      val res = watcher.correctTimestampVisit(upd.state.getLastVisited, Long.MaxValue, upd)
      res shouldBe upd
    }

    "not correct visit if it was bigger then default delay" in {
      val watcher = new Watcher("vin", updateDao, tokenDHelper, engine, 2)
      val lastVisit = System.currentTimeMillis() - 500
      val upd = createUpdate(lastVisit, DefaultDelay(5.millis))
      val res = watcher.correctTimestampVisit(lastVisit, System.currentTimeMillis() + 500.days.toMillis, upd)
      res shouldBe upd
    }

    "not correct visit if current is less then old one" in {
      val watcher = new Watcher("vin", updateDao, tokenDHelper, engine, 2)
      val lastVisit = System.currentTimeMillis() - 500
      val upd = createUpdate(lastVisit, DefaultDelay(5.millis))
      val res = watcher.correctTimestampVisit(
        lastVisit,
        lastVisit + WatchingStateUpdate.DefaultSyncDelay.toDuration.toMillis,
        upd
      )
      res shouldBe upd
    }

    "correct visit if it was bigger then delay" in {
      val watcher = new Watcher("vin", updateDao, tokenDHelper, engine, 2)
      val lastVisit = System.currentTimeMillis() - 500
      val upd = createUpdate(lastVisit, DefaultDelay(600.millis))
      val res = watcher.correctTimestampVisit(lastVisit, lastVisit + 1000, upd)
      res.delay.min(upd.delay) shouldBe res.delay
    }

  }

}
