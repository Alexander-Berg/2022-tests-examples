package ru.yandex.vertis.billing.banker.config

import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import pureconfig.generic.auto._
import ru.yandex.vertis.billing.banker.config.ReceiptDeliverySettings.SourceCheckerTypes
import ru.yandex.vertis.billing.banker.util.DateTimeUtils
import ru.yandex.vertis.billing.receipt.model.TaxTypes

import scala.jdk.CollectionConverters._
import scala.util.Success

class ReceiptSettingsSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {
  val actualFrom = new DateTime("2020-12-14T00:00:00.000+03:00")
  val actualFromString = DateTimeUtils.IsoDateTimeFormatter.print(actualFrom)

  val receiptOldParameters = Map(
    "firm-inn" -> "123",
    "taxation-type" -> "OSN",
    "purchase-tax" -> TaxTypes.NdsNone.toString,
    "wallet-tax" -> TaxTypes.NdsNone.toString,
    "reply-email" -> "test",
    "firm-url" -> "test.ru"
  )

  val receiptActualParameters = Map(
    "firm-inn" -> "123",
    "taxation-type" -> "OSN",
    "purchase-tax" -> TaxTypes.Nds20.toString,
    "wallet-tax" -> TaxTypes.`Nds20/120`.toString,
    "reply-email" -> "test",
    "firm-url" -> "test.ru",
    "relevant-from" -> actualFromString
  )

  val MainConfig = Map[String, Any](
    "ws.url" -> "url",
    "ws.serviceId" -> "1",
    "ds.url" -> "url",
    "ds.serviceId" -> "1",
    "renderer.url" -> "url",
    "renderer.serviceId" -> "1",
    "parameters" -> Seq(receiptOldParameters.asJava, receiptActualParameters.asJava).asJava
  )

  val checkerConfig =
    Map("delivery.source-checker-type" -> "email")

  val defaultCheckerConfigMap =
    Map.empty[String, String]

  val emailCheckerConfigMap =
    Map("delivery.source-checker-type" -> "email")

  val emailOrPhoneCheckerConfigMap =
    Map("delivery.source-checker-type" -> "email-or-phone")

  val checkers = Seq(
    defaultCheckerConfigMap,
    emailCheckerConfigMap,
    emailOrPhoneCheckerConfigMap
  )

  val smsConfigMap = Map(
    "delivery.sms.client.route" -> "test",
    "delivery.sms.client.sender" -> "test",
    "delivery.sms.client.url" -> "test",
    "delivery.sms.url-shortener.url" -> "url"
  )

  val emailConfigMap = Map(
    "delivery.email.mailer.host" -> "test",
    "delivery.email.mailer.port" -> "123",
    "delivery.email.fallback-email" -> "test"
  )

  val proxyConfigMap = Map(
    "proxy.host" -> "best.proxy.com",
    "proxy.port" -> "3128"
  )

  val ReceiptConfig = MainConfig ++ checkerConfig ++ smsConfigMap ++ emailConfigMap ++ proxyConfigMap

  "ReceiptSettings" should {
    "build" when {
      "full config" in {
        new ReceiptSettings(ConfigFactory.parseMap(ReceiptConfig.asJava))
      }
      "without checker" in {
        val config = MainConfig ++ smsConfigMap ++ emailConfigMap ++ proxyConfigMap
        val settings = new ReceiptSettings(ConfigFactory.parseMap(config.asJava))
        settings.delivery.sourceCheckerType shouldBe SourceCheckerTypes.AllowAll
      }
      "without sms settings with default or email checker" in {
        Seq(defaultCheckerConfigMap, emailCheckerConfigMap).foreach { checker =>
          val config = MainConfig ++ checker ++ emailConfigMap ++ proxyConfigMap
          new ReceiptSettings(ConfigFactory.parseMap(config.asJava))
        }
      }
      "without proxy settings" in {
        val config = MainConfig ++ checkerConfig ++ smsConfigMap ++ emailConfigMap
        new ReceiptSettings(ConfigFactory.parseMap(config.asJava))
      }
      "provider works correctly" in {
        val settings = new ReceiptSettings(ConfigFactory.parseMap(ReceiptConfig.asJava))
        settings.receiptParametersProvider.parameters(actualFrom).map(_.purchaseTax) shouldBe Success(
          TaxTypes.Nds20
        )
        settings.receiptParametersProvider.parameters(actualFrom.minus(1)).map(_.purchaseTax) shouldBe Success(
          TaxTypes.NdsNone
        )
      }
    }
    "not build" when {
      "without email settings with any checker" in {
        checkers.foreach { checker =>
          val config = MainConfig ++ checker ++ smsConfigMap ++ proxyConfigMap
          intercept[IllegalArgumentException] {
            new ReceiptSettings(ConfigFactory.parseMap(config.asJava))
          }
        }
      }
      "without sms settings with email or phone checker" in {
        val config = MainConfig ++ emailOrPhoneCheckerConfigMap ++ emailConfigMap ++ proxyConfigMap
        intercept[IllegalArgumentException] {
          new ReceiptSettings(ConfigFactory.parseMap(config.asJava))
        }
      }
    }

  }

}
