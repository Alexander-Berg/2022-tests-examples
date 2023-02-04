package ru.yandex.vertis.scoring.client.passport.impl

import cats.implicits.catsSyntaxApplicativeId
import eu.timepit.refined.auto._
import io.circe.DecodingFailure
import ru.yandex.vertis.quality.cats_utils.Awaitable.AwaitableSyntax
import ru.yandex.vertis.quality.http_client_utils.HttpClientUtils.HttpCodeException
import ru.yandex.vertis.quality.test_utils.SpecBase
import ru.yandex.vertis.quality.tvm_utils.TvmTicketProvider
import ru.yandex.vertis.scoring.client.passport.impl.HttpPassportClientSpec.{TestCaseFailure, TestCaseSuccess}
import ru.yandex.vertis.scoring.model.PassportUid
import ru.yandex.vertis.scoring.model.exceptions.HttpPassportClientException
import ru.yandex.vertis.scoring.model.passport.PassportResponseDataItem.PassportUserInfo
import ru.yandex.vertis.scoring.util.CommonPassportInfo._
import ru.yandex.vertis.scoring.util.PassportResponseUtil.getResponse
import sttp.client.{Identity, NothingT, RequestT, SttpBackend}

class HttpPassportClientSpec extends SpecBase {

  implicit private val stubBackend = mock[SttpBackend[F, Nothing, NothingT]]
  stub(stubBackend.send(_: RequestT[Identity, String, Nothing])) { case request =>
    getResponse(request.uri).pure
  }

  private val tvmTicketProvider = mock[TvmTicketProvider[F]]
  when(tvmTicketProvider.getTvmTicket(?)).thenReturn("ticket".pure)

  private val stubClient = new HttpPassportClient("http://test.test.net/test", tvmTicketProvider, 1)

  private val testCasesSuccess: Seq[TestCaseSuccess] =
    Seq(
      TestCaseSuccess(
        "correctly return passport valid user info",
        3000000001L,
        passportUserInfo1
      ),
      TestCaseSuccess(
        "correctly return passport info with empty phones",
        3000000002L,
        passportUserInfo2
      ),
      TestCaseSuccess(
        "correctly return passport info with missing phones and attributes",
        3000000003L,
        passportUserInfo3
      ),
      TestCaseSuccess(
        "correctly return passport info with empty uid",
        3000000005L,
        passportUserInfo4
      )
    )

  private val testCasesFailure1: Seq[TestCaseFailure] =
    Seq(
      TestCaseFailure(
        "fail if passport returns 404",
        3000000404L
      ),
      TestCaseFailure(
        "fail if passport returns 500",
        3000000500L
      )
    )

  private val testCasesFailure2: Seq[TestCaseFailure] =
    Seq(
      TestCaseFailure(
        "fail with in case of DB_EXCEPTION",
        3000000004L
      ),
      TestCaseFailure(
        "fail with exception in case of any error besides DB_EXCEPTION",
        3000000008L
      ),
      TestCaseFailure(
        "fail with exception in case of passport response with empty users",
        9990000000L
      )
    )

  private val testCasesFailure3: Seq[TestCaseFailure] =
    Seq(
      TestCaseFailure(
        "fail with exception in case of missing karma",
        3000000006L
      ),
      TestCaseFailure(
        "fail with exception in case of missing uid info",
        3000000007L
      )
    )

  "HttpPassportClient" should {

    testCasesSuccess.foreach { case TestCaseSuccess(description, uid, expected) =>
      description in {
        stubClient.getInfo(uid, "127.0.0.1").await shouldBe expected
      }
    }

    testCasesFailure1.foreach { case TestCaseFailure(description, uid) =>
      description in {
        assertThrows[HttpCodeException] {
          stubClient.getInfo(uid, "127.0.0.1").await
        }
      }
    }

    testCasesFailure2.foreach { case TestCaseFailure(description, uid) =>
      description in {
        assertThrows[HttpPassportClientException] {
          stubClient.getInfo(uid, "127.0.0.1").await
        }
      }
    }

    testCasesFailure3.foreach { case TestCaseFailure(description, uid) =>
      description in {
        assertThrows[DecodingFailure] {
          stubClient.getInfo(uid, "127.0.0.1").await
        }
      }
    }
  }
}

object HttpPassportClientSpec {

  case class TestCaseSuccess(description: String, uid: PassportUid, expected: PassportUserInfo)
  case class TestCaseFailure(description: String, uid: PassportUid)
}
