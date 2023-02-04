package ru.yandex.vertis.telepony.service.impl

import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.dao.jdbc.JdbcCallbackOrderDao
import ru.yandex.vertis.telepony.dao.jdbc.api._
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.CallbackGenerator._
import ru.yandex.vertis.telepony.model.CallbackOrder._
import ru.yandex.vertis.telepony.model.{CallbackOrder, Phone, RefinedSource, Tag}
import ru.yandex.vertis.telepony.properties.DynamicProperties
import ru.yandex.vertis.telepony.properties.DynamicProperties.IsWelcomeMessageIgnoringEnabled
import ru.yandex.vertis.telepony.service.CallbackOrderService
import ru.yandex.vertis.telepony.service.sink.CallbackOrderSink
import ru.yandex.vertis.telepony.util.CallFilters.{SourcePhoneFilter, TagFilter}
import ru.yandex.vertis.telepony.util.db.SlickDb._
import ru.yandex.vertis.telepony.util.{AutomatedContext, RequestContext, Threads}
import ru.yandex.vertis.telepony.{IntegrationSpecTemplate, SpecBase}

import scala.util.Try
import scala.annotation.nowarn

class CallbackOrderServiceImplIntSpec
  extends SpecBase
  with MockitoSupport
  with IntegrationSpecTemplate
  with BeforeAndAfterEach {

  import Threads.lightWeightTasksEc

  implicit val requestContext: RequestContext = AutomatedContext(id = "id")

  private lazy val callbackOrderSink = new CallbackOrderSink(brokerClient)
  private lazy val dao = new JdbcCallbackOrderDao(dualDb)
  private lazy val dp = mock[DynamicProperties]
  private lazy val service = new CallbackOrderServiceImpl(dao, callbackOrderSink, dp)

  @nowarn
  override def beforeEach(): Unit = {
    dualDb.master.run("", JdbcCallbackOrderDao.CallbackOrderQuery.delete).futureValue
  }

  val sourcePhonesGen: Gen[SourcePhoneFilter] = for {
    count <- Gen.choose(1, 10)
    phones <- Gen.listOfN(count, PhoneGen)
  } yield SourcePhoneFilter(phones.map(RefinedSource.apply).toSet)

  private val TurkishNumber = Phone("+905340775966")

  "CallbackServiceImpl" should {
    "fail callback creation" when {
      "target is international number" in {
        val orderRequest = CallbackOrderRequestGen.map { co =>
          co.copy(target = co.target.copy(number = TurkishNumber))
        }.next
        val callbackOrderSource = CallbackOrderSourceGen.next
        when(dp.getValue(typedDomain, IsWelcomeMessageIgnoringEnabled)).thenReturn(false)
        val f = service.order(typedDomain, orderRequest, callbackOrderSource).failed.futureValue
        f shouldBe a[IllegalArgumentException]
      }
      "source is international number" in {
        val orderRequest = CallbackOrderRequestGen.map { co =>
          co.copy(source = co.source.copy(number = TurkishNumber))
        }.next
        val callbackOrderSource = CallbackOrderSourceGen.next
        when(dp.getValue(typedDomain, IsWelcomeMessageIgnoringEnabled)).thenReturn(false)
        val f = service.order(typedDomain, orderRequest, callbackOrderSource).failed.futureValue
        f shouldBe a[IllegalArgumentException]
      }
    }
    "order callback" in {
      val orderRequest = CallbackOrderRequestGen.next.copy(dryRun = false)
      val callbackOrderSource = CallbackOrderSourceGen.next
      when(dp.getValue(typedDomain, IsWelcomeMessageIgnoringEnabled)).thenReturn(false)
      val order = service.order(typedDomain, orderRequest, callbackOrderSource).futureValue
      val order2 = dao.get(order.id).futureValue.value

      order2 shouldBe order
      checkOrderFields(orderRequest, callbackOrderSource, order2)
      order2.status.value shouldBe CallbackOrder.Statuses.Scheduled
    }

    "ignore notification from api for callback order" in {
      val orderRequest = CallbackOrderRequestGen.next.copy(dryRun = false)
      val orderRequestWithNotification = orderRequest.copy(
        target = orderRequest.target.copy(notification = Some(NotificationGen.next)),
        dryRun = false
      )
      val callbackOrderSource = CallbackOrderSourceGen.next
      when(dp.getValue(typedDomain, IsWelcomeMessageIgnoringEnabled)).thenReturn(true)
      val order = service.order(typedDomain, orderRequestWithNotification, callbackOrderSource).futureValue
      val order2 = dao.get(order.id).futureValue.value

      order2 shouldBe order
      val orderRequestForCheck = orderRequestWithNotification.copy(
        target = orderRequest.target.copy(notification = None),
        dryRun = false
      )
      checkOrderFields(orderRequestForCheck, callbackOrderSource, order2)
      order2.status.value shouldBe CallbackOrder.Statuses.Scheduled
    }

    "order fake callback" in {
      val orderRequest = CallbackOrderRequestGen.next.copy(dryRun = true)
      val callbackOrderSource = CallbackOrderSourceGen.next
      when(dp.getValue(typedDomain, IsWelcomeMessageIgnoringEnabled)).thenReturn(false)
      val order = service.order(typedDomain, orderRequest, callbackOrderSource).futureValue
      val order2 = dao.get(order.id).futureValue.value

      order2 shouldBe order
      checkOrderFields(orderRequest, callbackOrderSource, order2)
      order2.status.value shouldBe CallbackOrder.Statuses.Cancelled
    }

    "cancel callbacks" in {
      val callbackCancelRequest = CallbackCancelRequestGen.next.copy(
        sourcePhones = Gen.option(sourcePhonesGen).next
      )

      val objectId = callbackCancelRequest.objectId

      val tag: Tag = callbackCancelRequest.tag
        .map {
          case TagFilter.Full(fullTag) =>
            fullTag
          case TagFilter.Part(partTag) =>
            Tag(Option(ShortStr.next + partTag + ShortStr.next))
        }
        .getOrElse(TagGen.next)

      val targetPhone: Phone = callbackCancelRequest.targetPhones
        .map { phoneFilter =>
          phoneFilter.phones.head
        }
        .getOrElse(PhoneGen.next)

      val sourcePhone: Phone = callbackCancelRequest.sourcePhones
        .flatMap { sourcePhoneFilter =>
          Try {
            Phone(sourcePhoneFilter.phones.head.callerId.value)
          }.toOption
        }
        .getOrElse(PhoneGen.next)

      val orderRequest = CallbackOrderRequestGen.next.copy(
        objectId = objectId,
        tag = tag,
        source = SourceInfoGen.next.copy(number = sourcePhone),
        target = TargetInfoGen.next.copy(number = targetPhone)
      )
      val callbackOrderSource = CallbackOrderSourceGen.next

      val order = dao.create(typedDomain, orderRequest, callbackOrderSource).futureValue

      val cancelled = service.cancel(typedDomain, callbackCancelRequest).futureValue
      cancelled.count shouldBe 1

      val changed = dao.get(order.id).futureValue
      inside(changed) { case Some(newOrder) => newOrder.status.value should ===(Statuses.Cancelled) }
    }

  }

  private def checkOrderFields(
      orderRequest: CallbackOrderService.CreateRequest,
      callbackOrderSource: CallbackOrderSource,
      order: CallbackOrder) = {
    order.domain shouldBe typedDomain
    order.objectId shouldBe orderRequest.objectId
    order.source shouldBe orderRequest.source
    order.target shouldBe orderRequest.target
    order.payload shouldBe orderRequest.payload
    order.tag shouldBe orderRequest.tag
    order.settings shouldBe orderRequest.settings
    order.callbackOrderSource shouldBe callbackOrderSource
  }

}
