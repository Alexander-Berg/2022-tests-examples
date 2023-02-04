package ru.yandex.auto.vin.decoder.scheduler.workers.partners.checkburo

import auto.carfax.common.utils.misc.ResourceUtils
import auto.carfax.common.utils.tracing.Traced
import io.opentracing.noop.NoopTracerFactory
import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.manager.checkburo.CheckburoManager
import ru.yandex.auto.vin.decoder.manager.vin.TechInspectionUpdateManager
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.partners.checkburo.CheckburoReportType
import ru.yandex.auto.vin.decoder.partners.checkburo.converter.TechDataToPreparedConverter
import ru.yandex.auto.vin.decoder.partners.checkburo.model.CheckburoExceptions.{
  InvalidVinException,
  UnexpectedInspectionStatusException
}
import ru.yandex.auto.vin.decoder.partners.checkburo.model.CheckburoModels._
import ru.yandex.auto.vin.decoder.partners.checkburo.model.NotReadyOrder
import ru.yandex.auto.vin.decoder.proto.SchedulerModel
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateHolder
import ru.yandex.auto.vin.decoder.scheduler.workers.MixedRateLimiter
import ru.yandex.auto.vin.decoder.scheduler.workers.partners.checkburo.recovery.CheckburoReportFallback
import ru.yandex.auto.vin.decoder.scheduler.workers.queue.WorkersQueue
import ru.yandex.auto.vin.decoder.storage.vin.VinWatchingDao
import ru.yandex.auto.vin.decoder.utils.scheduler.PartnerUtils._
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class CheckburoGibddWorkerTest extends AnyFunSuite with MockitoSupport with BeforeAndAfterAll {

  implicit val t: Traced = Traced.empty
  implicit val metrics = TestOperationalSupport
  implicit val tracer = NoopTracerFactory.create()

  val reportType = CheckburoReportType.TechData
  val converter = mock[TechDataToPreparedConverter]
  val checkburoHttpManager = mock[CheckburoManager]
  val rateLimiter = MixedRateLimiter(100, 100, 1)
  val rawStorageManager = mock[RawStorageManager[VinCode]]
  val dao = mock[VinWatchingDao]
  val queue = mock[WorkersQueue[VinCode, SchedulerModel.CompoundState]]
  val mileagesFallback = mock[CheckburoReportFallback]
  val feature = mock[Feature[Boolean]]
  val newOrderOnEmptyBalance = mock[Feature[Boolean]]

  val vin = VinCode("X4X3D59430PS96744")

  val worker = new CheckburoGibddWorker(
    reportType,
    converter,
    checkburoHttpManager,
    rateLimiter,
    rawStorageManager,
    dao,
    queue,
    Map.empty,
    feature,
    newOrderOnEmptyBalance
  )

  override def beforeAll(): Unit = {
    reset(checkburoHttpManager)
    reset(converter)
    reset(rawStorageManager)
    reset(dao)
  }

  test("send request if should process and not sent") {
    val b = CompoundState.newBuilder()
    b.getCheckburoStateBuilder
      .getReportBuilder(reportType)
      .setShouldProcess(true)

    val state = WatchingStateHolder(vin, b.build(), 1)

    when(checkburoHttpManager.postOrder(?, ?)(?, ?)).thenReturn(Future.successful(OrderId("545")))

    val res = worker.action(WatchingStateHolder(vin, b.build(), 1))

    assert(res.updater.nonEmpty)
    assert(!res.updater.get.delay().isDefault)

    val updated = res.updater.get(state.toUpdate)
    val reportOpt = updated.state.getCheckburoState.findReport(reportType.toString)

    assert(reportOpt.exists(_.getRequestSent != 0))
    assert(reportOpt.exists(_.getOrderId == "545"))
    assert(reportOpt.exists(_.getShouldProcess))
    assert(reportOpt.exists(_.getCounter == 1))
    assert(reportOpt.exists(_.getInvalid == false))
  }

  test("send request if should process even if already received") {
    val b = CompoundState.newBuilder()
    b.getCheckburoStateBuilder
      .getReportBuilder(reportType)
      .setOrderId("434")
      .setShouldProcess(true)
      .setProcessRequested(99)
      .setRequestSent(100)
      .setReportArrived(101)

    val state = WatchingStateHolder(vin, b.build(), 1)

    when(checkburoHttpManager.postOrder(?, ?)(?, ?)).thenReturn(Future.successful(OrderId("545")))

    val res = worker.action(WatchingStateHolder(vin, b.build(), 1))

    assert(res.updater.nonEmpty)
    assert(!res.updater.get.delay().isDefault)

    val updated = res.updater.get(state.toUpdate)
    val reportOpt = updated.state.getCheckburoState.findReport(reportType.toString)

    assert(reportOpt.exists(_.getRequestSent > 100))
    assert(reportOpt.exists(_.getReportArrived == 101))
    assert(reportOpt.exists(r => r.getReportArrived < r.getRequestSent))
    assert(reportOpt.exists(_.getOrderId == "545"))
    assert(reportOpt.exists(_.getShouldProcess))
    assert(reportOpt.exists(_.getCounter == 1))
    assert(reportOpt.exists(_.getInvalid == false))
  }

  test("get report if should process and report_arrived < request_sent") {
    val b = CompoundState.newBuilder()
    b.getCheckburoStateBuilder
      .getReportBuilder(reportType)
      .setOrderId("434")
      .setShouldProcess(true)
      .setProcessRequested(99)
      .setReportArrived(100)
      .setRequestSent(101)

    val state = WatchingStateHolder(vin, b.build(), 1)
    val data = {
      val raw = ResourceUtils.getStringFromResources("/checkburo/vinData/non_empty_200.json")
      reportType.parse(vin, 200, raw)
    }

    when(checkburoHttpManager.getOrderResult[TechData](?, ?, ?)(?, ?)).thenReturn(Future.successful(data))
    when(converter.convert(?)(?)).thenReturn(Future.successful(VinInfoHistory.getDefaultInstance))
    when(rawStorageManager.upsert(?)(?)).thenReturn(Future.unit)

    val res = worker.action(WatchingStateHolder(vin, b.build(), 1))

    assert(res.updater.nonEmpty)
    assert(res.updater.get.delay().isDefault)

    val updated = res.updater.get(state.toUpdate)
    val reportOpt = updated.state.getCheckburoState.findReport(reportType.toString)

    assert(reportOpt.exists(_.getRequestSent == 101))
    assert(reportOpt.exists(_.getReportArrived > 100))
    assert(reportOpt.exists(r => r.getReportArrived > r.getRequestSent))
    assert(reportOpt.exists(_.getOrderId == "434"))
    assert(reportOpt.exists(_.getShouldProcess == false))
    assert(reportOpt.exists(_.getCounter == 0))
    assert(reportOpt.exists(_.getInvalid == false))
  }

  test("reschedule if report is not ready yet") {
    val b = CompoundState.newBuilder()
    b.getCheckburoStateBuilder
      .getReportBuilder(reportType)
      .setOrderId("434")
      .setShouldProcess(true)
      .setProcessRequested(99)
      .setCounter(21)
      .setRequestSent(100)

    val state = WatchingStateHolder(vin, b.build(), 1)
    when(checkburoHttpManager.getOrderResult[TechData](?, ?, ?)(?, ?))
      .thenReturn(Future.successful(NotReadyOrder))

    val res = worker.action(WatchingStateHolder(vin, b.build(), 1))

    assert(res.updater.nonEmpty)
    assert(!res.updater.get.delay().isDefault)
    assert(res.updater.get.delay().toDuration == 120.minutes)

    val updated = res.updater.get(state.toUpdate)
    val reportOpt = updated.state.getCheckburoState.findReport(reportType.toString)

    assert(reportOpt.exists(_.getProcessRequested == 99))
    assert(reportOpt.exists(_.getRequestSent == 100))
    assert(reportOpt.exists(_.getReportArrived == 0))
    assert(reportOpt.exists(_.getOrderId == "434"))
    assert(reportOpt.exists(_.getShouldProcess))
    assert(reportOpt.exists(_.getCounter == 22))
    assert(reportOpt.exists(_.getInvalid == false))
  }

  test("reschedule if unexpected status") {
    val b = CompoundState.newBuilder()
    b.getCheckburoStateBuilder
      .getReportBuilder(reportType)
      .setOrderId("434")
      .setShouldProcess(true)
      .setProcessRequested(99)
      .setCounter(12)
      .setRequestSent(100)

    val state = WatchingStateHolder(vin, b.build(), 1)
    when(checkburoHttpManager.getOrderResult[TechData](?, ?, ?)(?, ?))
      .thenReturn(Future.failed(UnexpectedInspectionStatusException("")))

    val res = worker.action(WatchingStateHolder(vin, b.build(), 1))

    assert(res.updater.nonEmpty)
    assert(!res.updater.get.delay().isDefault)
    assert(res.updater.get.delay().toDuration == 6.minutes)

    val updated = res.updater.get(state.toUpdate)
    val reportOpt = updated.state.getCheckburoState.findReport(reportType.toString)

    assert(reportOpt.exists(_.getProcessRequested == 99))
    assert(reportOpt.exists(_.getRequestSent == 100))
    assert(reportOpt.exists(_.getReportArrived == 0))
    assert(reportOpt.exists(_.getOrderId == "434"))
    assert(reportOpt.exists(_.getShouldProcess))
    assert(reportOpt.exists(_.getCounter == 13))
    assert(reportOpt.exists(_.getInvalid == false))
  }

  test("handle invalid vin exception") {
    val b = CompoundState.newBuilder()
    b.getCheckburoStateBuilder
      .getReportBuilder(reportType)
      .setShouldProcess(true)

    val state = WatchingStateHolder(vin, b.build(), 1)

    when(checkburoHttpManager.postOrder(?, ?)(?, ?)).thenReturn(Future.failed(InvalidVinException("")))

    val res = worker.action(WatchingStateHolder(vin, b.build(), 1))

    assert(res.updater.nonEmpty)
    assert(res.updater.get.delay().isDefault)

    val updated = res.updater.get(state.toUpdate)
    val reportOpt = updated.state.getCheckburoState.findReport(reportType.toString)

    assert(reportOpt.exists(_.getRequestSent == 0))
    assert(reportOpt.exists(_.getOrderId == ""))
    assert(reportOpt.exists(_.getShouldProcess == false))
    assert(reportOpt.exists(_.getCounter == 0))
    assert(reportOpt.exists(_.getInvalid == true))
  }
}
