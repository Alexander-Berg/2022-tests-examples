package ru.yandex.vertis.telepony.dao.jdbc

import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.dao.jdbc.api._
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.CallbackGenerator._
import ru.yandex.vertis.telepony.model.TypedDomains
import ru.yandex.vertis.telepony.service.CallbackCallService.ByCallbackOrderIds
import ru.yandex.vertis.telepony.util.JdbcSpecTemplate
import ru.yandex.vertis.telepony.util.Range.Full

import scala.annotation.nowarn

/**
  * @author neron
  */
class JdbcCallbackDaoIntSpec extends SpecBase with BeforeAndAfterEach with JdbcSpecTemplate {

  private val domain = TypedDomains.autoru_def
  private lazy val orderDao = new JdbcCallbackOrderDao(dualDb)
  private lazy val dao = new JdbcCallbackCallDao(dualDb)

  @nowarn
  override def beforeEach(): Unit = {
    dualDb.master.run("delete-all", JdbcCallbackCallDao.CallbackHistory.delete).futureValue
  }

  "CallbackCallDao" should {
    "create new" in {
      val orderRequest = CallbackOrderRequestGen.next
      val callbackOrderSource = CallbackOrderSourceGen.next
      val order = orderDao.create(domain, orderRequest, callbackOrderSource).futureValue
      order.callbackOrderSource shouldBe callbackOrderSource

      val request = CallbackCreateRequestGen.next.copy(orderId = order.id)
      val callId = ShortStr.next
      val callback = dao.create(callId, request).futureValue

      callback.map(_.id) shouldBe Some(callId)
    }

    "not create duplicate" in {
      val orderRequest = CallbackOrderRequestGen.next
      val callbackOrderSource = CallbackOrderSourceGen.next
      val order = orderDao.create(domain, orderRequest, callbackOrderSource).futureValue

      val request = CallbackCreateRequestGen.next.copy(orderId = order.id)
      val callId = ShortStr.next
      dao.create(callId, request).futureValue.map(_.id) shouldBe Some(callId)
      dao.create(callId, request).futureValue shouldBe None
    }

    "list all" in {
      val orderRequest = CallbackOrderRequestGen.next
      val callbackOrderSource = CallbackOrderSourceGen.next
      val order = orderDao.create(domain, orderRequest, callbackOrderSource).futureValue

      val request = CallbackCreateRequestGen.next.copy(orderId = order.id)
      val callId = ShortStr.next

      dao.create(callId, request).futureValue
      val callbacks = dao.list(ByCallbackOrderIds(Set(request.orderId)), Full).futureValue
      callbacks should have size 1
      inside(callbacks.headOption) {
        case Some(c) =>
          c.id shouldBe callId
          c.externalId shouldBe request.externalId
          c.time shouldBe request.time
          c.sourceCallerId shouldBe request.sourceCallerId
          c.sourceCallResult shouldBe request.sourceCallResult
          c.targetCallerId shouldBe request.targetCallerId
          c.targetCallResult shouldBe request.targetCallResult
          c.duration shouldBe request.duration
          c.talkDuration shouldBe request.talkDuration
          c.hasRecord shouldBe request.hasRecord
          c.order shouldBe order
      }
    }

  }

}
