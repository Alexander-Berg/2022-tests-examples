package ru.yandex.vertis.scoring.converters

import java.time.Instant

import eu.timepit.refined.auto._
import io.circe.CursorOp.DownField
import io.circe.DecodingFailure
import io.circe.parser.decode
import ru.yandex.vertis.quality.test_utils.SpecBase
import ru.yandex.vertis.scoring.converters.PassportFormat._
import ru.yandex.vertis.scoring.converters.PassportFormatSpec.TestCase
import ru.yandex.vertis.scoring.model.passport.PassportResponse
import ru.yandex.vertis.scoring.model.passport.PassportResponseDataItem.{PassportUserError, PassportUserInfo}
import ru.yandex.vertis.scoring.model.passport.info._
import ru.yandex.vertis.scoring.util.PassportResponseUtil._

class PassportFormatSpec extends SpecBase {

  private val uidInfo1 = UidInfo(3000000001L, false)
  private val uidInfo2 = UidInfo(3000000002L, false)
  private val uidInfo3 = UidInfo(3000000003L, false)
  private val karmaInfo1 = KarmaInfo(85, Some(Instant.ofEpochSecond(1321965947L)))
  private val karmaInfo2 = KarmaInfo(0, None)
  private val karmaStatusInfo1 = KarmaStatusInfo(3085)
  private val karmaStatusInfo2 = KarmaStatusInfo(0)
  private val accountAttributes = AccountAttributes(Instant.ofEpochSecond(1294999198L))
  private val phoneAttributes = PhoneAttributes(false, true)
  private val phoneInfo = PhoneInfo(2, phoneAttributes)
  private val passportUserInfo1 =
    PassportUserInfo(
      3000000001L,
      uidInfo1,
      karmaInfo1,
      karmaStatusInfo1,
      Some(accountAttributes),
      Some(List(phoneInfo))
    )
  private val passportUserInfo2 =
    passportUserInfo1.copy(
      id = 3000000002L,
      uid = uidInfo2,
      phones = Some(List.empty)
    )
  private val passportUserInfo3 =
    passportUserInfo1.copy(
      id = 3000000003L,
      uid = uidInfo3,
      attributes = None,
      phones = None
    )
  private val passportUserInfo4 =
    PassportUserInfo(
      3000000005L,
      EmptyUid,
      karmaInfo2,
      karmaStatusInfo2,
      None,
      None
    )
  private val passportExceptionInfo1 = PassportExceptionInfo("DB_EXCEPTION", 10)
  private val passportExceptionInfo2 = PassportExceptionInfo("INVALID_PARAMS", 2)
  private val passportUserErrorMessage =
    PassportUserError(
      3000000004L,
      passportExceptionInfo1,
      "Fatal BlackBox error: dbpool exception in sezam dbfields fetch"
    )
  private val passportErrorMessage =
    PassportResponse.Failure(
      passportExceptionInfo2,
      "BlackBox error: Missing userip argument"
    )

  private val decodingFailure =
    DecodingFailure("Attempt to decode value on failed cursor", List(DownField("exception")))

  private val testCases: Seq[TestCase] =
    Seq(
      TestCase(
        "correctly convert valid passport response",
        validSuperfluousPassportResponse,
        Right(
          PassportResponse.Success(
            Seq(passportUserInfo1)
          )
        )
      ),
      TestCase(
        "correctly convert passport response with empty phones",
        passportResponseWithEmptyPhones,
        Right(
          PassportResponse.Success(
            Seq(passportUserInfo2)
          )
        )
      ),
      TestCase(
        "correctly convert passport response with missing phones and attributes",
        passportMessageMissingPhonesAndAttributes,
        Right(
          PassportResponse.Success(
            Seq(passportUserInfo3)
          )
        )
      ),
      TestCase(
        "correctly convert passport response with DB_EXCEPTION error message",
        passportResponseErrorDBExceptionMessage,
        Right(
          PassportResponse.Success(
            Seq(passportUserErrorMessage)
          )
        )
      ),
      TestCase(
        "correctly convert passport response with empty uidInfo",
        passportResponseUserNotFound,
        Right(
          PassportResponse.Success(
            Seq(passportUserInfo4)
          )
        )
      ),
      TestCase(
        "correctly convert passport response with INVALID_PARAMS error message",
        passportResponseErrorInvalidParamsMessage,
        Right(
          passportErrorMessage
        )
      ),
      TestCase(
        "fail in case of missing karma",
        passportResponseMissingKarma,
        Left(decodingFailure)
      ),
      TestCase(
        "fail in case of missing uidInfo",
        passportResponseMissingUidInfo,
        Left(decodingFailure)
      )
    )

  "PassportFormat" should {
    testCases.foreach { case TestCase(description, response, expectedResult) =>
      description in {
        decode[PassportResponse](response) shouldBe expectedResult
      }
    }
  }
}

object PassportFormatSpec {
  case class TestCase(description: String, response: String, expectedResult: Either[DecodingFailure, PassportResponse])
}
