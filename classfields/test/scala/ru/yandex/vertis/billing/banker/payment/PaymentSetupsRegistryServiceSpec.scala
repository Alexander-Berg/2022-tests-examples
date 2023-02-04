package ru.yandex.vertis.billing.banker.payment

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.model.PaymentRequest.{Context, Targets}
import ru.yandex.vertis.billing.banker.model.gens.{PaymentPayloadGen, Producer}
import ru.yandex.vertis.billing.banker.model.{Payload, PaymentRequest, PaymentSystemIds}
import ru.yandex.vertis.billing.banker.service.PaymentSetupsRegistry
import ru.yandex.vertis.billing.banker.service.PaymentSystemSupport.MethodFilter
import ru.yandex.vertis.billing.banker.util.AutomatedContext

import scala.util.{Failure, Success}

/**
  * Specs on [[PaymentSetupsRegistry]]
  *
  * @author alesavin
  */
trait PaymentSetupsRegistryServiceSpec extends AnyWordSpec with Matchers with AsyncSpecBase {

  def registry: PaymentSetupsRegistry

  implicit val context = AutomatedContext("PaymentSetupsRegistryServiceSpec")

  "PaymentSystemsRegistryService" should {

    val Registered = PaymentSystemIds.YandexKassa

    "provide all payment systems" in {
      registry.all() match {
        case s if s.nonEmpty => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
    "provide registered payment system" in {
      registry.get(Registered).toTry match {
        case Success(_) => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
    "provide None for non-registered payment system" in {
      registry.get(PaymentSystemIds.Robokassa).toTry match {
        case Failure(_: NoSuchElementException) => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
    "get available for empty payload" in {
      registry
        .available(
          "1",
          MethodFilter.ForSource(
            PaymentRequest.Source(
              "1001",
              100L,
              Payload.Empty,
              PaymentRequest.Options(),
              None,
              Context(Targets.Wallet),
              None,
              None
            )
          )
        )
        .toTry match {
        case Success(response) if response.succs.nonEmpty => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
    "get avaliable for campaign payload" in {
      // TODO there registry has known about customer account
      registry
        .available(
          "2",
          MethodFilter.ForSource(
            PaymentRequest.Source(
              "1002",
              10L,
              PaymentPayloadGen.next,
              PaymentRequest.Options(
                Some("payment1"),
                Some("http://ya.ru")
              ),
              None,
              Context(Targets.Wallet),
              None,
              None
            )
          )
        )
        .toTry match {
        case Success(response) if response.succs.nonEmpty => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }

  }
}
