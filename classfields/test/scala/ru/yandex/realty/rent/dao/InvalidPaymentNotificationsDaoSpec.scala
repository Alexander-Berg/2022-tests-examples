package ru.yandex.realty.rent.dao

import org.junit.runner.RunWith
import org.scalatest.WordSpecLike
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.clients.tinkoff.eacq.PaymentNotification
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class InvalidPaymentNotificationsDaoSpec
  extends WordSpecLike
  with RentSpecBase
  with RentModelsGen
  with CleanSchemaBeforeEach {

  implicit val trace: Traced = Traced.empty

  "Invalid Payment Notifications DAOs" should {

    "insert each notification with new id" in {
      val notifications =
        for (i <- 0 until 10)
          yield i -> PaymentNotification(
            TerminalKey = "1",
            OrderId = "2",
            Success = true,
            Status = "Ok",
            PaymentId = 4,
            Amount = 1234567,
            Token = "abacaba"
          )

      val ids = Future
        .sequence(
          notifications.map { case (ix, n) => invalidPaymentNotificationsDao.insert(n, ix.toString) }
        )
        .futureValue
        .toSet

      assert(ids.size == 10)
      assert(invalidPaymentNotificationsDao.countUnprocessed().futureValue == 10)
    }

  }

}
