package ru.yandex.vertis.billing.balance.simple.notification

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.vertis.billing.balance.simple.model.{Basket, FormNotificationSource, Notification}
import ru.yandex.vertis.billing.balance.simple.notification.SimpleBalanceNotificationsSpec._

import scala.util.{Failure, Success}

/**
  * Created by IntelliJ IDEA.
  * User: alesavin
  * Date: 17.09.14
  * Time: 16:09
  */
@RunWith(classOf[JUnitRunner])
class SimpleBalanceNotificationsSpec extends FlatSpec with Matchers {

  "Notification parser" should "fail on empty source" in {
    val source = FormNotificationSource(Seq())
    parser.parseNotification(source) match {
      case Failure(e: NoSuchElementException) => info(s"Done $e")
      case other => fail(s"Unexpected $other")
    }
  }

  it should "fail on partial source" in {
    val source = FormNotificationSource(
      Seq(
        "status" -> "ok"
      )
    )
    parser.parseNotification(source) match {
      case Failure(e: NoSuchElementException) => info(s"Done $e")
      case other => fail(s"Unexpected $other")
    }
  }

  it should "fail on partial source 2" in {
    val source = FormNotificationSource(
      Seq(
        "status" -> "success"
      )
    )
    parser.parseNotification(source) match {
      case Failure(e: NoSuchElementException) => info(s"Done $e")
      case other => fail(s"Unexpected $other")
    }
  }

  it should "pass if exist code and basket" in {
    val source = FormNotificationSource(
      Seq(
        "status" -> "success",
        "trust_payment_id" -> "test"
      )
    )
    parser.parseNotification(source) match {
      case Success(Notification("test", Basket.Statuses.Success, Notification.Modes.Result, _, _)) => info(s"Done")
      case other => fail(s"Unexpected $other")
    }
  }

  it should "pass full flash" in {
    val source = FormNotificationSource(
      Seq(
        "status" -> "refund",
        "trust_payment_id" -> "test2",
        "mode" -> "refund_result",
        "service_order_id" -> "1",
        "trust_refund_id" -> "2",
        "other" -> "smstttt"
      )
    )
    parser.parseNotification(source) match {
      case Success(
            Notification("test2", Basket.Statuses.Refund, Notification.Modes.RefundResult, Some(1L), Some("2"))
          ) =>
        info(s"Done")
      case other => fail(s"Unexpected $other")
    }
  }

  it should "fail on error" in {
    val source = FormNotificationSource(
      Seq(
        "status" -> "error",
        "other" -> "smstttt"
      )
    )
    parser.parseNotification(source) match {
      case Failure(e: IllegalStateException) => info(s"Done $e")
      case other => fail(s"Unexpected $other")
    }
  }
}

object SimpleBalanceNotificationsSpec {

  val parser = SimpleBalanceNotificationsImpl
}
