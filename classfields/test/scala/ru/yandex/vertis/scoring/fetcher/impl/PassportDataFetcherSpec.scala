package ru.yandex.vertis.scoring.fetcher.impl

import java.time.Instant

import cats.effect.Clock
import cats.implicits.{catsSyntaxApplicativeErrorId, catsSyntaxApplicativeId}
import eu.timepit.refined.auto._
import io.circe.CursorOp.{DownArray, DownField}
import io.circe.DecodingFailure
import ru.yandex.vertis.quality.cats_utils.Awaitable.AwaitableSyntax
import ru.yandex.vertis.quality.test_utils.SpecBase
import ru.yandex.vertis.scoring.client.passport.impl.StubPassportClient
import ru.yandex.vertis.scoring.model.passport.PassportResponseDataItem.PassportUserInfo
import ru.yandex.vertis.scoring.model.{PassportUid, UserData}
import ru.yandex.vertis.scoring.fetcher.impl.PassportDataFetcherSpec.TestCase
import ru.yandex.vertis.scoring.model.exceptions.{
  HttpPassportClientException,
  PassportDataValidationException,
  UserNotFoundException
}
import ru.yandex.vertis.scoring.util.CommonPassportInfo._

class PassportDataFetcherSpec extends SpecBase {

  implicit protected def clock: Clock[F] = Clock.create

  private def getInfo(uid: PassportUid): F[PassportUserInfo] =
    uid.value match {
      case 3000000001L => passportUserInfo1.pure
      case 3000000002L => passportUserInfo2.pure
      case 3000000003L =>
        PassportDataValidationException(s"Missing karma.allowUntil or attributes info in userInfo: TESTCASE").raiseError
      case 3000000004L => HttpPassportClientException(s"Failed with DB_EXCEPTION for request: TESTCASE").raiseError
      case 3000000005L => UserNotFoundException(s"User not found for uid: 3000000005").raiseError
      case 3000000006L =>
        DecodingFailure(
          "Attempt to decode value on failed cursor",
          List(DownField("exception"), DownArray, DownField("users"))
        ).raiseError
      case 9000000000L =>
        PassportDataValidationException(
          s"Uids do not match. Request uid: 9000000000, response uid: 9900000000"
        ).raiseError
    }

  private val stubClient = new StubPassportClient(getInfo)
  private val passportDataFetcher = new PassportDataFetcher[F](stubClient)

  private val passportDataItem1 =
    UserData.Passport(
      3000000001L,
      karmaInfo1,
      accountAttributes,
      false,
      true,
      Instant.now()
    )

  private val passportDataItem2 =
    passportDataItem1.copy(
      uid = 3000000002L,
      isAnyPhoneConfirmed = false,
      isAnyPhoneBound = false
    )

  private val testCasesSuccess: Seq[TestCase] =
    Seq(
      TestCase(
        "correctly return passport data item for valid user info",
        3000000001L,
        passportDataItem1
      ),
      TestCase(
        "correctly return passport data item for user info with empty phones",
        3000000002L,
        passportDataItem2
      )
    )

  "PassportDataFetcher" should {
    testCasesSuccess.foreach { case TestCase(description, uid, expected) =>
      description in {
        val result = passportDataFetcher.getData(uid, "127.0.0.1").await
        result.uid shouldBe expected.uid
        result.karmaInfo shouldBe expected.karmaInfo
        result.attributes shouldBe expected.attributes
        result.isAnyPhoneConfirmed shouldBe expected.isAnyPhoneConfirmed
        result.isAnyPhoneBound shouldBe expected.isAnyPhoneBound
      }
    }

    "fail with exception in case of missing attributes" in {
      assertThrows[PassportDataValidationException] {
        stubClient.getInfo(3000000003L, "127.0.0.1").await
      }
    }

    "fail with DB_EXCEPTION" in {
      assertThrows[HttpPassportClientException] {
        stubClient.getInfo(3000000004L, "127.0.0.1").await
      }
    }

    "fail in case no user found" in {
      assertThrows[UserNotFoundException] {
        stubClient.getInfo(3000000005L, "127.0.0.1").await
      }
    }

    "fail with exception in case of missing karma" in {
      assertThrows[DecodingFailure] {
        stubClient.getInfo(3000000006L, "127.0.0.1").await
      }
    }

    "fail with exception in case different uid" in {
      assertThrows[PassportDataValidationException] {
        stubClient.getInfo(9000000000L, "127.0.0.1").await
      }
    }
  }
}

object PassportDataFetcherSpec {

  case class TestCase(description: String, uid: PassportUid, expected: UserData.Passport)
}
