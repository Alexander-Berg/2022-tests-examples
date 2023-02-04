package ru.yandex.auto.vin.decoder.scheduler.workers.partners.checkburo

import auto.carfax.common.utils.tracing.Traced
import io.opentracing.noop.NoopTracerFactory
import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.vin.decoder.manager.checkburo.CheckburoManager
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.partners.adaperio.AdaperioReportType
import ru.yandex.auto.vin.decoder.partners.checkburo.CheckburoReportType
import ru.yandex.auto.vin.decoder.partners.checkburo.model.CheckburoExceptions.EmptyBalance
import ru.yandex.auto.vin.decoder.partners.checkburo.model.CheckburoModels._
import ru.yandex.auto.vin.decoder.partners.checkburo.model.ReadyOrder
import ru.yandex.auto.vin.decoder.proto.SchedulerModel
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.ReportOrderState.ReportOrder
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.{CompoundState, StateUpdateHistory}
import ru.yandex.auto.vin.decoder.raw.RawToPreparedConverter
import ru.yandex.auto.vin.decoder.scheduler.models.{WatchingStateHolder, WatchingStateUpdate}
import ru.yandex.auto.vin.decoder.scheduler.workers.MixedRateLimiter
import ru.yandex.auto.vin.decoder.scheduler.workers.partners.checkburo.CheckburoVinWorkerTest.MileagesEmptyBalance
import ru.yandex.auto.vin.decoder.scheduler.workers.partners.checkburo.recovery.CheckburoReportFallback
import ru.yandex.auto.vin.decoder.scheduler.workers.queue.WorkersQueue
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import ru.yandex.auto.vin.decoder.storage.vin.VinWatchingDao
import ru.yandex.auto.vin.decoder.utils.scheduler.PartnerUtils._
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class CheckburoVinWorkerTest extends AnyWordSpecLike with Matchers with MockitoSupport with BeforeAndAfterAll {

  implicit val t: Traced = Traced.empty
  implicit val metrics = TestOperationalSupport
  implicit val tracer = NoopTracerFactory.create()
  implicit val trigger = PartnerRequestTrigger.Unknown

  val checkburoHttpManager = mock[CheckburoManager]
  val rateLimiter = MixedRateLimiter(100, 100, 1)
  val rawStorageManager = mock[RawStorageManager[VinCode]]
  val dao = mock[VinWatchingDao]
  val queue = mock[WorkersQueue[VinCode, SchedulerModel.CompoundState]]
  val enabledFeature = Feature("enabled", _ => true)

  def createWorker[R](
      reportType: CheckburoReportType[R],
      converter: RawToPreparedConverter[ReadyOrder[R]],
      fallback: CheckburoReportFallback,
      enableEmptyBalanceFallback: Feature[Boolean]) =
    new CheckburoVinWorker[R](
      reportType,
      converter,
      checkburoHttpManager,
      rateLimiter,
      rawStorageManager,
      dao,
      queue,
      Map(CheckburoReportType.Mileages -> fallback),
      enabledFeature,
      enableEmptyBalanceFallback
    )

  val vin = VinCode("X4X3D59430PS96744")

  override def beforeAll(): Unit = {
    reset(checkburoHttpManager)
    reset(rawStorageManager)
    reset(dao)
  }

  "CheckburoVinWorker" should {
    "finish report state and raise another partner on empty balance" in {
      def raiseAdaperioMileage(state: CompoundState) = {
        val b = state.toBuilder
        b.getAdaperioBuilder
          .getReportBuilder(AdaperioReportType.TechInspections)
          .setShouldProcess(true) // just to check state updated
        b.build()
      }
      val fallback = new CheckburoReportFallback {
        def apply(
            update: WatchingStateUpdate[CompoundState],
            vin: VinCode
          )(implicit t: Traced,
            trigger: PartnerRequestTrigger): WatchingStateUpdate[CompoundState] =
          update.withUpdate(raiseAdaperioMileage)
      }

      val reportType = CheckburoReportType.Mileages
      val orderId = "old_order_id"
      val converter = mock[RawToPreparedConverter[ReadyOrder[Mileages]]]
      val worker = createWorker(reportType, converter, fallback, enabledFeature)
      val now = System.currentTimeMillis()

      val compoundState = {
        val b = CompoundState.newBuilder()
        b.getCheckburoStateBuilder.addOrders(
          ReportOrder
            .newBuilder()
            .setCounter(50)
            .setOrderId(orderId)
            .setReportType("CHECKBURO_MILEAGE")
            .setRequestSent(now - 1)
            .setProcessRequested(now - 2)
            .setShouldProcess(true)
            .addStateUpdateHistory(StateUpdateHistory.getDefaultInstance)
        )
        b.build()
      }
      val state = WatchingStateHolder(vin, compoundState, now + 2.minutes.toMillis)

      when(checkburoHttpManager.getOrderResult(eq(vin), eq(reportType), eq(OrderId(orderId)))(any(), any()))
        .thenReturn(Future.failed(EmptyBalance(MileagesEmptyBalance)))

      val result = worker.action(state)

      val updated = result.updater.get.apply(state.toUpdate)
      result.reschedule shouldBe false
      updated.state shouldBe {
        val b = CompoundState.newBuilder()
        b.getAdaperioBuilder.getReportBuilder(AdaperioReportType.TechInspections).setShouldProcess(true)
        b.getCheckburoStateBuilder
        b.build()
      }
    }
  }
}

object CheckburoVinWorkerTest {

  private val MileagesEmptyBalance =
    """
        |{
        |    "total": 1,
        |    "data": [
        |        {
        |            "_id": {
        |                "$id": "629c5dc207be0686328b4616"
        |            },
        |            "vin": "JTMZE33VX0D009220",
        |            "datetime": {
        |                "sec": 1654414786,
        |                "usec": 0
        |            },
        |            "user": {
        |                "$id": "5f0f85d907be06d0178b4569"
        |            },
        |            "inspect_status": "done",
        |            "region": null,
        |            "insufficient_funds": [
        |                "run"
        |            ],
        |            "run": "У Вас закончились проверки по базе 17. Пробеги"
        |        }
        |    ]
        |}
        |""".stripMargin
}
