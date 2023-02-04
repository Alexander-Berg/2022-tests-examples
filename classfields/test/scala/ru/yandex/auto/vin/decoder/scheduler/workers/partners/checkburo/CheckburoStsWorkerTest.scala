package ru.yandex.auto.vin.decoder.scheduler.workers.partners.checkburo

import auto.carfax.common.utils.misc.ResourceUtils
import auto.carfax.common.utils.tracing.Traced
import io.opentracing.noop.NoopTracerFactory
import org.mockito.Mockito.{never, reset, verify}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.vin.decoder.manager.checkburo.CheckburoManager
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.partners.checkburo.CheckburoReportType
import ru.yandex.auto.vin.decoder.partners.checkburo.converter.FinesToPreparedConverter
import ru.yandex.auto.vin.decoder.partners.checkburo.model.CheckburoExceptions.InvalidStsException
import ru.yandex.auto.vin.decoder.partners.checkburo.model.CheckburoModels.{Fines, OrderId}
import ru.yandex.auto.vin.decoder.partners.checkburo.model.CheckburoOrderResponse
import ru.yandex.auto.vin.decoder.proto.SchedulerModel
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.ReportOrderState.{ReportOrder, StsReportOrder}
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import ru.yandex.auto.vin.decoder.scheduler.models.{WatchingStateHolder, WatchingStateUpdate}
import ru.yandex.auto.vin.decoder.scheduler.workers.MixedRateLimiter
import ru.yandex.auto.vin.decoder.scheduler.workers.partners.checkburo.recovery.CheckburoReportFallback
import ru.yandex.auto.vin.decoder.scheduler.workers.queue.WorkersQueue
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import ru.yandex.auto.vin.decoder.storage.vin.VinWatchingDao
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.ListHasAsScala

class CheckburoStsWorkerTest extends AnyWordSpecLike with MockitoSupport with BeforeAndAfterEach {

  implicit val t: Traced = Traced.empty
  implicit val metrics = TestOperationalSupport
  implicit val tracer = NoopTracerFactory.create()

  val reportType = CheckburoReportType.Fines
  val converter = mock[FinesToPreparedConverter]
  val checkburoHttpManager = mock[CheckburoManager]
  val rateLimiter = MixedRateLimiter(100, 100, 1)
  val rawStorageManager = mock[RawStorageManager[VinCode]]
  val dao = mock[VinWatchingDao]
  val queue = mock[WorkersQueue[VinCode, SchedulerModel.CompoundState]]
  val feature = mock[Feature[Boolean]]
  val newOrderOnEmptyBalance = mock[Feature[Boolean]]

  val vin = VinCode("X4X3D59430PS96744")

  val worker = new CheckburoStsWorker[Fines](
    reportType,
    converter,
    checkburoHttpManager,
    rateLimiter,
    rawStorageManager,
    dao,
    queue,
    new CheckburoReportFallback {

      override def apply(
          update: WatchingStateUpdate[CompoundState],
          vin: VinCode
        )(implicit t: Traced,
          trigger: PartnerRequestTrigger): WatchingStateUpdate[CompoundState] = update
    },
    feature,
    newOrderOnEmptyBalance
  )

  override def beforeEach(): Unit = {
    reset(checkburoHttpManager)
    reset(rawStorageManager)
    reset(dao)
  }

  "CheckburoStsWorker" should {
    "make multiple orders for different sts" in {

      val stsOrder = StsReportOrder
        .newBuilder()
        .setSts("123")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("CHECKBURO_FINES")
            .setShouldProcess(true)
        )

      val stsOrder2 = StsReportOrder
        .newBuilder()
        .setSts("1234")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("CHECKBURO_FINES")
            .setShouldProcess(true)
        )
      val b = CompoundState.newBuilder()
      b.getCheckburoStateBuilder
        .addStsOrders(0, stsOrder)
        .addStsOrders(0, stsOrder2)

      val state = WatchingStateHolder(vin, b.build(), 1)
      when(checkburoHttpManager.postOrder(?, ?)(?, ?)).thenReturn(Future.successful(OrderId("444")))

      val res = worker.action(WatchingStateHolder(vin, b.build(), 1))

      assert(res.updater.nonEmpty)
      assert(!res.updater.get.delay().isDefault)

      val updated = res.updater.get(state.toUpdate)
      val stsOrders = updated.state.getCheckburoState.getStsOrdersList
      assert(stsOrders.asScala.forall(_.getReport.getOrderId == "444"))
      assert(stsOrders.asScala.forall(_.getReport.getShouldProcess))
      assert(stsOrders.asScala.forall(_.getReport.getRequestSent != 0))
    }

    "make multiple orders for errors/successful sts" in {
      val stsOrder = StsReportOrder
        .newBuilder()
        .setSts("9921369389")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("CHECKBURO_FINES")
            .setShouldProcess(true)
        )

      val stsOrder2 = StsReportOrder
        .newBuilder()
        .setSts("9921369390")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("CHECKBURO_FINES")
            .setShouldProcess(true)
        )
      val b = CompoundState.newBuilder()
      b.getCheckburoStateBuilder
        .addStsOrders(0, stsOrder)
        .addStsOrders(1, stsOrder2)

      val state = WatchingStateHolder(vin, b.build(), 1)
      when(checkburoHttpManager.postOrder(?, ?)(?, ?))
        .thenReturn(Future.successful(OrderId("444")), Future.failed(new RuntimeException))

      val res = worker.action(WatchingStateHolder(vin, b.build(), 1))

      assert(res.updater.nonEmpty)
      assert(!res.updater.get.delay().isDefault)

      val updated = res.updater.get(state.toUpdate)
      val stsOrders = updated.state.getCheckburoState.getStsOrdersList

      val successfulOrder = stsOrders.get(0)
      assert(0 != successfulOrder.getReport.getRequestSent)
      assert(successfulOrder.getReport.getShouldProcess)
      assert("444" == successfulOrder.getReport.getOrderId)

      val failedOrder = stsOrders.get(1)
      assert(0 == failedOrder.getReport.getRequestSent)
      assert(failedOrder.getReport.getShouldProcess)
      assert(failedOrder.getReport.getOrderId.isEmpty)
    }

    "get data for multiple sts'es" in {

      val stsOrder = StsReportOrder
        .newBuilder()
        .setSts("123")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("CHECKBURO_FINES")
            .setOrderId("466")
            .setRequestSent(1)
            .setShouldProcess(true)
        )

      val stsOrder2 = StsReportOrder
        .newBuilder()
        .setSts("1234")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("CHECKBURO_FINES")
            .setOrderId("522")
            .setRequestSent(1)
            .setShouldProcess(true)
        )
      val b = CompoundState.newBuilder()
      b.getCheckburoStateBuilder
        .addStsOrders(0, stsOrder)
        .addStsOrders(0, stsOrder2)

      val state = WatchingStateHolder(vin, b.build(), 1)
      val raw = ResourceUtils.getStringFromResources("/checkburo/vinData/non_empty_200.json")
      val response = CheckburoOrderResponse.parse[Fines](vin, raw, 200, "stAllSts")
      when(checkburoHttpManager.postOrder(?, ?)(?, ?)).thenReturn(Future.successful(OrderId("444")))
      when(checkburoHttpManager.getOrderResult[Fines](?, ?, ?)(?, ?)).thenReturn(Future.successful(response))

      val res = worker.action(WatchingStateHolder(vin, b.build(), 1))

      assert(res.updater.nonEmpty)
      assert(!res.updater.get.delay().isDefault)

      val updated = res.updater.get(state.toUpdate)
      val stsOrders = updated.state.getCheckburoState.getStsOrdersList
      assert(stsOrders.asScala.forall(_.getReport.getShouldProcess))
      assert(stsOrders.asScala.forall(_.getReport.getRequestSent != 0))
    }

    "handle error/successes" in {

      val stsOrder = StsReportOrder
        .newBuilder()
        .setSts("123")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("CHECKBURO_FINES")
            .setOrderId("466")
            .setRequestSent(1)
            .setShouldProcess(true)
        )

      val stsOrder2 = StsReportOrder
        .newBuilder()
        .setSts("1234")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("CHECKBURO_FINES")
            .setOrderId("522")
            .setRequestSent(1)
            .setShouldProcess(true)
        )
      val b = CompoundState.newBuilder()
      b.getCheckburoStateBuilder
        .addStsOrders(0, stsOrder)
        .addStsOrders(0, stsOrder2)

      val state = WatchingStateHolder(vin, b.build(), 1)
      val raw = ResourceUtils.getStringFromResources("/checkburo/vinData/non_empty_200.json")
      val response = CheckburoOrderResponse.parse[Fines](vin, raw, 200, "stAllSts")
      when(checkburoHttpManager.getOrderResult[Fines](?, ?, ?)(?, ?))
        .thenReturn(Future.successful(response), Future.failed(new RuntimeException))
      when(converter.convert(?)(?)).thenReturn(Future.successful(VinInfoHistory.getDefaultInstance))
      when(rawStorageManager.upsert(?)(?)).thenReturn(Future.unit)
      val res = worker.action(WatchingStateHolder(vin, b.build(), 1))

      assert(res.updater.nonEmpty)
      assert(!res.updater.get.delay().isDefault)

      val updated = res.updater.get(state.toUpdate)
      val stsOrders = updated.state.getCheckburoState.getStsOrdersList

      val successfulOrder = stsOrders.get(0)
      assert(!successfulOrder.getReport.getShouldProcess)
      assert(successfulOrder.getReport.getReportArrived != 0)
      assert(successfulOrder.getReport.getCounter == 0)

      val failedOrder = stsOrders.get(1)

      assert(failedOrder.getReport.getShouldProcess)
      assert(failedOrder.getReport.getReportArrived == 0)
      assert(failedOrder.getReport.getCounter == 1)
    }

    "should not get the report if it is considered invalid during first attempt" in {
      val stsOrder = StsReportOrder
        .newBuilder()
        .setSts("123")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("CHECKBURO_FINES")
            .setShouldProcess(true)
        )

      val b = CompoundState.newBuilder()
      b.getCheckburoStateBuilder
        .addStsOrders(0, stsOrder)

      val state = WatchingStateHolder(vin, b.build(), 1)
      when(checkburoHttpManager.postOrder(?, ?)(?, ?))
        .thenReturn(Future.failed(InvalidStsException("invalid sts")))
      val firstAttempt = worker.action(WatchingStateHolder(vin, b.build(), 1))

      assert(firstAttempt.updater.nonEmpty)
      assert(!firstAttempt.updater.get.delay().isDefault)

      val updated = firstAttempt.updater.get(state.toUpdate)

      val secondAttempt = worker.action(WatchingStateHolder(vin, updated.state, 1))
      val secondState = secondAttempt.updater.get(updated)

      val failedOrder = secondState.state.getCheckburoState.getStsOrdersList.get(0)

      verify(checkburoHttpManager, never).getOrderResult(?, ?, ?)(?, ?)
      assert(!failedOrder.getReport.getShouldProcess)
      assert(failedOrder.getReport.getReportArrived == 0)
      assert(failedOrder.getReport.getCounter == 0)
      assert(failedOrder.getReport.getInvalid)
    }
  }
}
