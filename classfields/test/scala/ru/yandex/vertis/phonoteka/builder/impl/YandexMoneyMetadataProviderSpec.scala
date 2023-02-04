package ru.yandex.vertis.phonoteka.builder.impl

import java.time.{Instant, LocalDate, ZoneOffset}

import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxOptionId}
import ru.yandex.vertis.phonoteka.builder.impl.YandexMoneyMetadataProviderSpec.YandexMoneyMetadataTestCase
import ru.yandex.vertis.phonoteka.client.impl.HttpYandexMoneyClient
import ru.yandex.vertis.phonoteka.model.Phone
import ru.yandex.vertis.phonoteka.model.metadata.YandexMoneyMetadata
import ru.yandex.vertis.phonoteka.util.stub.StubYandexMoneyUtil._
import ru.yandex.vertis.quality.cats_utils.Awaitable.AwaitableSyntax
import ru.yandex.vertis.quality.test_utils.SpecBase
import sttp.client.{Identity, NothingT, RequestT, SttpBackend}

class YandexMoneyMetadataProviderSpec extends SpecBase {

  implicit val stubBackend = mock[SttpBackend[F, Nothing, NothingT]]
  stub(stubBackend.send(_: RequestT[Identity, String, Nothing])) { case request =>
    getResponseByUri(request.uri).pure
  }
  private val stubClient = new HttpYandexMoneyClient[F](testConfig.url)
  val provider = new YandexMoneyMetadataProvider[F](stubClient)

  val testCases: Seq[YandexMoneyMetadataTestCase] =
    Seq(
      YandexMoneyMetadataTestCase(
        "successfully get metadata",
        phone1,
        YandexMoneyMetadata(
          phone1,
          Instant.now(),
          true.some,
          true.some,
          false.some,
          instantFromString("2020-02-06").some,
          instantFromString("2020-02-06").some
        )
      ),
      YandexMoneyMetadataTestCase(
        "successfully get metadata 2",
        phone2,
        YandexMoneyMetadata(
          phone1,
          Instant.now(),
          true.some,
          true.some,
          false.some,
          instantFromString("2020-02-03").some,
          instantFromString("2020-02-03").some
        )
      ),
      YandexMoneyMetadataTestCase(
        "successfully get part metadata",
        phone3,
        YandexMoneyMetadata(phone1, Instant.now(), Some(false), None, Some(false), None, None)
      )
    )

  private def instantFromString(date: String): Instant = LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant

  "YandexMoneyMetadataProviderSpec" should {
    testCases.foreach { case YandexMoneyMetadataTestCase(description, phone, expected) =>
      description in {
        val result = provider.get(phone).await
        result.hasWallet shouldBe expected.hasWallet
        result.hasTransactions shouldBe expected.hasTransactions
        result.phoneNumberInBlackList shouldBe expected.phoneNumberInBlackList
        result.lastWalletCreated shouldBe expected.lastWalletCreated
        result.lastTransaction shouldBe expected.lastTransaction
      }
    }

    "fail caused by bad request" in {
      val caught =
        intercept[IllegalArgumentException] {
          provider.get(phone4).await
        }
      caught.getMessage == s"Error 400 when get info for phone $phone4"
    }

    "fail caused by internal error" in {
      val caught =
        intercept[RuntimeException] {
          provider.get(phone5).await
        }
      caught.getMessage == s"Error 500 when get info for phone $phone5"
    }
  }
}

object YandexMoneyMetadataProviderSpec {
  case class YandexMoneyMetadataTestCase(description: String, phone: Phone, expected: YandexMoneyMetadata)
}
