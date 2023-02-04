package ru.yandex.vertis.telepony.tasks

import org.joda.time.DateTime
import org.mockito.Mockito.{atLeastOnce, verify, verifyNoInteractions}
import org.scalacheck.Gen
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.CallbackGenerator._
import ru.yandex.vertis.telepony.model.CallbackOrder.{CallbackOrderStatus, Statuses, TargetInfo}
import ru.yandex.vertis.telepony.model.{CallPeriod, CallbackRetryingTimeouts, TypedDomains}
import ru.yandex.vertis.telepony.properties.DynamicProperties
import ru.yandex.vertis.telepony.service.{CallbackCallService, CallbackManager, CallbackOrderService}
import ru.yandex.vertis.telepony.settings.CallbackScheduleConfig
import ru.yandex.vertis.telepony.util.sliced.SimpleSlicedResult
import ru.yandex.vertis.telepony.util.{Range, TestPrometheusComponent, Threads}

import scala.concurrent.Future
import scala.concurrent.duration._

class CallbackManagerTaskSpec extends TestPrometheusComponent with SpecBase with MockitoSupport {

  implicit val ec = Threads.lightWeightTasksEc
  implicit val pr = prometheusRegistry

  private val domain = TypedDomains.autoru_def

  abstract class Test(enableCallback: Boolean) {

    val mockedCallbackService = mock[CallbackOrderService]
    val mockedCallbackManager = mock[CallbackManager]
    val mockedCallbackCallService = mock[CallbackCallService]
    val retryingTimeouts = new CallbackRetryingTimeouts(Seq(0.minutes, 5.minutes, 10.minutes))

    val mockedProperties = mock[DynamicProperties]
    when(mockedProperties.getSharedValue(DynamicProperties.IsCallbackEnabled))
      .thenReturn(enableCallback)

    val task: CallbackManagerTask = new CallbackManagerTask(
      callbackService = mockedCallbackService,
      callbackManager = mockedCallbackManager,
      callbackCallService = mockedCallbackCallService,
      callbackScheduleConfig = new CallbackScheduleConfig(TypedDomains.values.toSeq.map((_, retryingTimeouts)).toMap),
      properties = mockedProperties
    )
  }

  val targetInfoForCallNowGen: Gen[TargetInfo] = for {
    targetInfo <- TargetInfoGen
  } yield {
    targetInfo.copy(
      callPeriods = Seq(
        CallPeriod(
          openTime = DateTime.now().minusYears(1),
          closeTime = DateTime.now().plusYears(1)
        )
      )
    )
  }

  "CallbackManagerTask.forCallNow" should {
    "return two order with different targets" in new Test(true) {
      val order1 = CallbackOrderGen.next.copy(
        createTime = DateTime.now().minusHours(1),
        status = CallbackOrderStatus(Statuses.Scheduled, DateTime.now().minusHours(1)),
        target = targetInfoForCallNowGen.next,
        domain = domain
      )
      val firstTarget = order1.target.number.value
      val order2 = CallbackOrderGen.next.copy(
        id = CallbackOrderIdGen.suchThat(_.value != order1.id.value).next,
        createTime = DateTime.now().minusHours(1),
        status = CallbackOrderStatus(Statuses.Scheduled, DateTime.now().minusHours(1)),
        target = targetInfoForCallNowGen.suchThat(_.number.value != firstTarget).next,
        domain = domain
      )
      val orders = Seq(order1, order2)

      val calls = Future.successful(SimpleSlicedResult(Iterable.empty, Range.Full))
      when(mockedCallbackCallService.list(?, ?)(?)).thenReturn(calls)

      val ordersForCallNow = task.forCallNow(orders).futureValue
      ordersForCallNow.map(_.id) should contain theSameElementsAs orders.map(_.id)
    }

    "return order without calls" in new Test(true) {
      val targetInfo: TargetInfo = targetInfoForCallNowGen.next

      val order1 = CallbackOrderGen.next.copy(
        createTime = DateTime.now().minusHours(1),
        status = CallbackOrderStatus(Statuses.Scheduled, DateTime.now().minusHours(1)),
        target = targetInfo,
        domain = domain
      )
      val order2 = CallbackOrderGen.next.copy(
        id = CallbackOrderIdGen.suchThat(_.value != order1.id.value).next,
        createTime = DateTime.now().minusHours(1),
        status = CallbackOrderStatus(Statuses.Scheduled, DateTime.now().minusHours(1)),
        target = targetInfo,
        domain = domain
      )
      val orders = Seq(order1, order2)

      val call = CallbackGen.next.copy(
        time = DateTime.now,
        order = order1
      )
      val calls = Future.successful(SimpleSlicedResult(Iterable(call), Range.Full))
      when(mockedCallbackCallService.list(?, ?)(?)).thenReturn(calls)

      val ordersForCallNow = task.forCallNow(orders).futureValue
      ordersForCallNow.map(_.id) should contain theSameElementsAs Iterable(order2.id)
    }

    "return more priority order " in new Test(true) {
      val targetInfo: TargetInfo = targetInfoForCallNowGen.next

      val order1 = CallbackOrderGen.next.copy(
        createTime = DateTime.now().minusHours(1),
        status = CallbackOrderStatus(Statuses.Scheduled, DateTime.now().minusHours(1)),
        target = targetInfo,
        domain = domain
      )
      val order2 = CallbackOrderGen.next.copy(
        id = CallbackOrderIdGen.suchThat(_.value != order1.id.value).next,
        createTime = DateTime.now().minusHours(1),
        status = CallbackOrderStatus(Statuses.Scheduled, DateTime.now().minusHours(1)),
        target = targetInfo,
        domain = domain
      )
      val orders = Seq(order1, order2)

      val call1 = CallbackGen.next.copy(
        time = DateTime.now.minusHours(2),
        order = order1
      )
      val call2 = CallbackGen.next.copy(
        time = DateTime.now.minusHours(1),
        order = order1
      )
      val call3 = CallbackGen.next.copy(
        time = DateTime.now,
        order = order2
      )
      val calls = Future.successful(SimpleSlicedResult(Iterable(call1, call2, call3), Range.Full))
      when(mockedCallbackCallService.list(?, ?)(?)).thenReturn(calls)

      val ordersForCallNow = task.forCallNow(orders).futureValue
      ordersForCallNow.map(_.id) should contain theSameElementsAs Iterable(order1.id)
    }

  }

  "CallbackManagerTask.payload" when {
    s"""dynamic property "${DynamicProperties.IsCallbackEnabled.name}" is false""" should {
      "not make any calls" in new Test(false) {
        // Initialize callBackService with sample calls
        val targetInfo: TargetInfo = targetInfoForCallNowGen.next
        val order1 = CallbackOrderGen.next.copy(
          createTime = DateTime.now().minusHours(1),
          status = CallbackOrderStatus(Statuses.Scheduled, DateTime.now.minusHours(1)),
          target = targetInfo
        )
        val orders = Future.successful(Iterable(order1))
        val call = CallbackGen.next.copy(
          time = DateTime.now.minusHours(1),
          order = order1
        )
        val calls = Future.successful(SimpleSlicedResult(Iterable(call), Range.Full))
        when(mockedCallbackService.list(?, ?)(?)).thenReturn(orders)
        when(mockedCallbackCallService.list(?, ?)(?)).thenReturn(calls)
        when(mockedCallbackManager.makeCallback(?)(?)).thenReturn(Future.unit)

        // Run the task
        task.payload().futureValue

        // Check that it has not interacted with CallbackManager and, hence, no calls were performed
        verifyNoInteractions(mockedCallbackManager)
      }
    }

    s"""dynamic property "${DynamicProperties.IsCallbackEnabled.name}" is true""" should {
      "make calls" in new Test(true) {
        // Initialize callBackService with sample calls
        val targetInfo: TargetInfo = targetInfoForCallNowGen.next
        val order1 = CallbackOrderGen.next.copy(
          createTime = DateTime.now().minusHours(1),
          status = CallbackOrderStatus(Statuses.Scheduled, DateTime.now.minusHours(1)),
          target = targetInfo
        )
        val orders = Future.successful(Iterable(order1))
        val call = CallbackGen.next.copy(
          time = DateTime.now.minusHours(1),
          order = order1
        )
        val calls = Future.successful(SimpleSlicedResult(Iterable(call), Range.Full))
        when(mockedCallbackService.list(?, ?)(?)).thenReturn(orders)
        when(mockedCallbackCallService.list(?, ?)(?)).thenReturn(calls)
        when(mockedCallbackManager.makeCallback(?)(?)).thenReturn(Future.unit)

        // Run the task
        task.payload().futureValue

        // Check that it has a call was performed
        verify(mockedCallbackManager, atLeastOnce()).makeCallback(?)(?)
      }
    }
  }
}
