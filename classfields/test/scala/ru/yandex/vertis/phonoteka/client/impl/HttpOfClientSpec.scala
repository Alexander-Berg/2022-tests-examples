package ru.yandex.vertis.phonoteka.client.impl

import cats.implicits.catsSyntaxApplicativeId
import ru.yandex.vertis.phonoteka.client.impl.HttpOfClientSpec.{TestCaseFailure, TestCaseSuccess}
import ru.yandex.vertis.phonoteka.model.Phone
import ru.yandex.vertis.phonoteka.model.response.OfResponse.Variable
import ru.yandex.vertis.phonoteka.model.response.OfResponse.Variable.Status
import ru.yandex.vertis.phonoteka.util.stub.StubOfUtil._
import ru.yandex.vertis.quality.cats_utils.Awaitable.AwaitableSyntax
import ru.yandex.vertis.quality.test_utils.SpecBase
import sttp.client.{Identity, NothingT, RequestT, SttpBackend}

class HttpOfClientSpec extends SpecBase {

  implicit val stubBackend = mock[SttpBackend[F, Nothing, NothingT]]
  stub(stubBackend.send(_: RequestT[Identity, String, Nothing])) { case request =>
    getResponse(request.body).pure
  }
  private val stubClient = new HttpOfClient[F](testConfig.url)

  private val testCasesSuccess: Seq[TestCaseSuccess] =
    Seq(
      TestCaseSuccess(
        "successfully get variables",
        phone1,
        Seq(Variable("SCORE_V12", Status.Success, Some("2.0")), Variable("SCORE_V11", Status.Success, Some("5.0")))
      ),
      TestCaseSuccess(
        "successfully get variables 2",
        phone2,
        Seq(Variable("SCORE_V12", Status.Success, Some("4.0")), Variable("SCORE_V11", Status.Success, Some("1.0")))
      ),
      TestCaseSuccess(
        "successfully get variables 3",
        phone3,
        Seq(Variable("SCORE_V12", Status.Success, Some("3.2")), Variable("SCORE_V11", Status.Success, Some("2.0")))
      ),
      TestCaseSuccess(
        "correctly parse variables with NOT_FOUND status",
        phone4,
        Seq(Variable("SCORE_V12", Status.NotFound, None), Variable("SCORE_V11", Status.Success, Some("1.0")))
      )
    )

  private val testCasesFailure: Seq[TestCaseFailure] =
    Seq(
      TestCaseFailure(
        "fail while parse variables with unexpected status",
        phone5,
        "not a member of enum"
      ),
      TestCaseFailure(
        "fail for unexpected json body",
        phone6,
        "failed cursor"
      )
    )

  "HttpOfClient" should {
    testCasesSuccess.foreach { case TestCaseSuccess(description, phone, expectedResult) =>
      description in {
        val result = stubClient.getInfo(phone).await
        result shouldBe expectedResult
      }
    }

    testCasesFailure.foreach { case TestCaseFailure(description, phone, expected) =>
      description in {
        val caught =
          intercept[Exception] {
            stubClient.getInfo(phone).await
          }
        caught.getMessage == expected
      }
    }
  }
}

object HttpOfClientSpec {
  private case class TestCaseSuccess(description: String, phone: Phone, expected: Seq[Variable])
  private case class TestCaseFailure(description: String, phone: Phone, expected: String)
}
