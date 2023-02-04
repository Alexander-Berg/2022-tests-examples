package ru.yandex.vertis.billing.tasks

import billing.common.testkit.zio.ZIOSpecBase
import org.joda.time.DateTime
import org.mockito.{Answers, Mockito}
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.OrderDao
import ru.yandex.vertis.billing.model_core.balance.SpendingUnits
import ru.yandex.vertis.billing.model_core.gens.{OrderGen, Producer}
import ru.yandex.vertis.billing.model_core.{Epoch, EpochValue, Order}
import ru.yandex.vertis.billing.service.metered.MeteredStub
import ru.yandex.vertis.billing.service.{BalanceApi, EpochService, OrderService}
import ru.yandex.vertis.billing.settings.BalanceSettings
import ru.yandex.vertis.billing.tasks.BalanceSyncTask.Modes
import ru.yandex.vertis.billing.tasks.BalanceSyncTaskSpec.TestSetup
import ru.yandex.vertis.billing.util.DateTimeUtils.now
import ru.yandex.vertis.billing.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => ~~}

import scala.util.{Failure, Success, Try}

/**
  * Runnable spec on [[BalanceSyncTask]]
  *
  * @author alex-kovalenko
  */
class BalanceSyncTaskSpec
  extends AnyWordSpec
  with Matchers
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with ZIOSpecBase {

  val startOfDay = now().withTimeAtStartOfDay()
  val startOfDayMs = startOfDay.getMillis

  val orders = Seq(1, 3, 2).map(epoch => EpochValue(OrderGen.next, Some(epoch)))

  implicit val rc: RequestContext = AutomatedContext("BalanceSyncTask")

  "BalanceSyncTask" when {
    "executed in any mode" should {
      "use zero epoch for the first time" in new TestSetup {
        when(epochService.getTry(?)).thenReturn(Failure(new NoSuchElementException))
        Modes.values.foreach { implicit mode =>
          task.getEpoch should matchPattern { case Success(0) => }
        }
      }
      "pass pre-calculated send time to syncOrder" in new TestSetup {
        when(orderService.getOrders(?, ?, ?)(?)).thenReturn(Success(orders))
        Modes.values.foreach { mode =>
          val time = now()
          task.modeAndSyncTime = Some(mode -> time)
          task.execute().unsafeRun()

          Mockito
            .verify(balanceApi, Mockito.times(orders.size))
            .sendCampaignSpending(?, ~~(time), ?, ?)
        }
      }

      "not stop progress if fail" in new TestSetup {
        when(orderService.getOrders(?, ?, ?)(?)).thenReturn(Success(orders))

        stub(balanceApi.sendCampaignSpending _) { case (orderId, _, _, _) =>
          if (orderId.serviceOrderId == orders.head.value.id) {
            Failure(new RuntimeException())
          } else if (orderId.serviceOrderId == orders.last.value.id) {
            Failure(new IllegalArgumentException())
          } else {
            Success(())
          }
        }
        intercept[RuntimeException] {
          task.execute().unsafeRun()
        }

        Mockito
          .verify(balanceApi, Mockito.times(orders.size))
          .sendCampaignSpending(?, ?, ?, ?)
      }

    }
    "executed in Regular mode" should {
      implicit val mode = Modes.Regular

      "use epoch from epoch service" in new TestSetup {
        forAll(Gen.chooseNum(startOfDayMs - 100, startOfDayMs + 100)) { epoch =>
          when(epochService.getTry(?)).thenReturn(Success(epoch))
          task.getEpoch should matchPattern { case Success(`epoch`) => }
        }
      }
      "update epoch" in new TestSetup {
        task.updateEpoch(orders)
        Mockito.verify(epochService).setTry(?, ~~(3L))
      }
      "get orders for sync in simple way" in new TestSetup {
        when(orderService.getOrders(?, ?, ?)(?)).thenReturn(Success(orders))
        task.getOrdersForSync.get
        Mockito.verify(orderService).getOrders(~~(0L), ~~(None), ?)(?)
      }
    }

    "executed in Strict mode" should {
      implicit val mode = Modes.Strict

      "use epoch from service if it is before end-of-yesterday" in new TestSetup {
        forAll(Gen.chooseNum(0, startOfDayMs)) { epoch =>
          when(epochService.getTry(?)).thenReturn(Success(epoch))
          task.getEpoch should matchPattern { case Success(`epoch`) => }
        }
      }
      "use end-of-yesterday if it is after epoch from service" in new TestSetup {
        forAll(Gen.chooseNum(startOfDayMs, Long.MaxValue)) { epoch =>
          when(epochService.getTry(?)).thenReturn(Success(epoch))
          task.getEpoch should matchPattern { case Success(`startOfDayMs`) => }
        }
      }
      "not update epoch" in new TestSetup {
        task.updateEpoch(orders)(Modes.Strict)
        Mockito.verifyNoInteractions(epochService)
      }
      "get adjusted orders for sync" in new TestSetup {
        when(orderService.getOrders(?, ?, ?)(?)).thenReturn(Success(orders))
        task.getOrdersForSync.get
        Mockito.verify(orderService).getOrders(~~(0L), ~~(Some(startOfDay)), ?)(?)
      }
    }
  }
}

object BalanceSyncTaskSpec extends MockitoSupport {

  trait TestSetup {

    val balanceSettings: BalanceSettings = {
      val m = mock[BalanceSettings](Answers.RETURNS_DEEP_STUBS)
      when(m.ServiceId).thenReturn(0)
      when(m.UnitsForSpending).thenReturn(SpendingUnits.Bucks)
      m
    }
    val orderDao: OrderDao = mock[OrderDao]
    val orderService: OrderService = mock[OrderService]

    val epochService: EpochService = {
      val m = mock[EpochService]
      when(m.getTry(?)).thenReturn(Success(0L))
      when(m.setTry(?, ?)).thenReturn(Success(()))
      m
    }

    val balanceApi: BalanceApi = {
      val m = mock[BalanceApi]
      when(m.sendCampaignSpending(?, ?, ?, ?)).thenReturn(Success(()))
      m
    }

    val task: TestBalanceSyncTask =
      new BalanceSyncTaskSpec.TestBalanceSyncTask(orderService, epochService, balanceApi, balanceSettings)

  }

  class TestBalanceSyncTask(
      orderService: OrderService,
      epochService: EpochService,
      balanceApi: BalanceApi,
      balanceSettings: BalanceSettings)
    extends BalanceSyncTask(orderService, epochService, balanceApi, balanceSettings)
    with MeteredStub {

    override def getOrdersForSync(implicit mode: Modes.Value): Try[Iterable[EpochValue[Order]]] =
      super.getOrdersForSync

    var modeAndSyncTime: Option[(Modes.Value, DateTime)] = None

    override def getModeAndSyncTime: (Modes.Value, DateTime) =
      modeAndSyncTime.getOrElse(super.getModeAndSyncTime)

    override def getEpoch(implicit mode: Modes.Value): Try[Epoch] =
      super.getEpoch

    override def updateEpoch(orders: Iterable[EpochValue[Order]])(implicit mode: Modes.Value): Unit = {
      super.updateEpoch(orders)
    }
  }

}
