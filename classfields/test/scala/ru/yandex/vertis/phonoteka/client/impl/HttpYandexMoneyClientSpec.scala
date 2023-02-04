package ru.yandex.vertis.phonoteka.client.impl

import java.time.LocalDate

import cats.implicits.catsSyntaxApplicativeId
import ru.yandex.vertis.phonoteka.client.impl.HttpYandexMoneyClientSpec.TestCase
import ru.yandex.vertis.phonoteka.model.Phone
import ru.yandex.vertis.phonoteka.model.response.YandexMoneyResponse
import ru.yandex.vertis.phonoteka.model.response.YandexMoneyResponse.Error
import ru.yandex.vertis.phonoteka.util.stub.StubYandexMoneyUtil._
import ru.yandex.vertis.quality.cats_utils.Awaitable.AwaitableSyntax
import ru.yandex.vertis.quality.test_utils.SpecBase
import sttp.client.{Identity, NothingT, RequestT, SttpBackend}

class HttpYandexMoneyClientSpec extends SpecBase {

  implicit val stubBackend = mock[SttpBackend[F, Nothing, NothingT]]
  stub(stubBackend.send(_: RequestT[Identity, String, Nothing])) { case request =>
    getResponseByUri(request.uri).pure
  }

  private val stubClient = new HttpYandexMoneyClient[F](testConfig.url)

  private val testCases: Seq[TestCase] =
    Seq(
      TestCase(
        "successfully get data",
        phone1,
        YandexMoneyResponse.Success(
          hasWallet = Some(true),
          Some(true),
          phoneNumberInBlackList = Some(false),
          Some(LocalDate.parse("2020-02-06")),
          Some(LocalDate.parse("2020-02-06"))
        )
      ),
      TestCase(
        "successfully get data 2",
        phone2,
        YandexMoneyResponse.Success(
          hasWallet = Some(true),
          Some(true),
          phoneNumberInBlackList = Some(false),
          Some(LocalDate.parse("2020-02-03")),
          Some(LocalDate.parse("2020-02-03"))
        )
      ),
      TestCase(
        "successfully get part-data",
        phone3,
        YandexMoneyResponse.Success(hasWallet = Some(false), None, phoneNumberInBlackList = Some(false), None, None)
      ),
      TestCase("error", phone4, Error(400)),
      TestCase("error 2", phone5, Error(500))
    )

  "HttpYandexMoneyClient" should {
    testCases.foreach { case TestCase(description, phone, expectedResult) =>
      description in {
        val result = stubClient.getInfo(phone).await
        result shouldBe expectedResult
      }
    }
  }
}

object HttpYandexMoneyClientSpec {
  private case class TestCase(description: String, phone: Phone, expected: YandexMoneyResponse)
}
