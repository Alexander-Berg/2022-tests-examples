package ru.yandex.vertis.billing.banker.config

import com.typesafe.config.ConfigFactory
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import pureconfig.ConfigSource
import ru.yandex.vertis.billing.banker.model.gens.{
  paymentRequestSourceGen,
  refundRequestSourceGen,
  PaymentRequestSourceParams,
  Producer,
  RefundRequestSourceParams
}
import pureconfig.generic.auto._
import ru.yandex.vertis.billing.banker.config.ReceiptDeliverySettings.SourceCheckerTypes
import ru.yandex.vertis.billing.banker.model.PaymentRequest.{Context, Targets}
import ru.yandex.vertis.billing.banker.model.{PaymentRequest, SourceWithReceiptData}

import scala.jdk.CollectionConverters._

/**
  * Spec on [[InvariantChecker]]
  *
  * @author ruslansd
  */
class InvariantCheckerSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {

  def safeCheck(checker: InvariantChecker, checkerType: SourceCheckerTypes.Value, source: SourceWithReceiptData): Unit =
    checkerType match {
      case _ if source.context.target == Targets.SecurityDeposit =>
        checker.check(source)
      case SourceCheckerTypes.Email if !source.isReceiptEmailDefined =>
        intercept[IllegalArgumentException] {
          checker.check(source)
        }
        ()
      case SourceCheckerTypes.EmailOrPhone if !source.isReceiptEmailOrPhoneDefined =>
        intercept[IllegalArgumentException] {
          checker.check(source)
        }
        ()
      case _ =>
        checker.check(source)
    }

  def test(checker: InvariantChecker, checkerType: SourceCheckerTypes.Value): Unit = {
    val paymentParams = PaymentRequestSourceParams(context = Some(contextWithoutSecurityDeposit.next))
    forAll(paymentRequestSourceGen(paymentParams)) { source =>
      safeCheck(checker, checkerType, source)
    }

    forAll(paymentRequestSourceGen(paymentParams.copy(withReceipt = Some(true)))) { source =>
      safeCheck(checker, checkerType, source)
    }

    forAll(paymentRequestSourceGen(paymentParams.copy(withReceipt = Some(false)))) { source =>
      safeCheck(checker, checkerType, source)
    }

    val refundParams = RefundRequestSourceParams(context = Some(contextWithoutSecurityDeposit.next))
    forAll(refundRequestSourceGen(refundParams)) { source =>
      safeCheck(checker, checkerType, source)
    }

    val withSecurityDeposit = PaymentRequestSourceParams(
      context = Some(contextWithSecurityDeposit),
      withReceipt = Some(false)
    )
    forAll(paymentRequestSourceGen(withSecurityDeposit)) { source =>
      safeCheck(checker, checkerType, source)
    }
  }

  private val contextWithoutSecurityDeposit: Gen[Context] = for {
    target <- Gen.oneOf((Targets.values - Targets.SecurityDeposit).toSeq)
  } yield Context(target)

  private val contextWithSecurityDeposit = Context(Targets.SecurityDeposit)

  "InvariantChecker" should {
    "work on default receipt settings" in {
      val settings = ConfigSource.fromConfig(ConfigFactory.empty()).loadOrThrow[ReceiptDeliverySettings]
      settings.sourceCheckerType shouldBe SourceCheckerTypes.AllowAll
      val checker = new InvariantChecker(settings)
      test(checker, SourceCheckerTypes.AllowAll)
    }

    "work on with email checker" in {
      val configMap = Map(
        "source-checker-type" -> "email"
      )
      val config = ConfigFactory.parseMap(configMap.asJava)
      val settings = ConfigSource.fromConfig(config).loadOrThrow[ReceiptDeliverySettings]
      settings.sourceCheckerType shouldBe SourceCheckerTypes.Email
      val checker = new InvariantChecker(settings)
      test(checker, SourceCheckerTypes.Email)
    }

    "work on with email or phone checker" in {
      val configMap = Map(
        "source-checker-type" -> "email-or-phone"
      )
      val config = ConfigFactory.parseMap(configMap.asJava)
      val settings = ConfigSource.fromConfig(config).loadOrThrow[ReceiptDeliverySettings]
      settings.sourceCheckerType shouldBe SourceCheckerTypes.EmailOrPhone
      val checker = new InvariantChecker(settings)
      test(checker, SourceCheckerTypes.EmailOrPhone)
    }

  }

}
