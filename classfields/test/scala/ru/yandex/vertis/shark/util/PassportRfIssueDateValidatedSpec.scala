package ru.yandex.vertis.shark.util

import java.time.LocalDate
import cats.data.Validated.{Invalid, Valid}
import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.zio_baker.scalapb_utils.Validation.Result
import ru.yandex.vertis.shark.model.Block.{BirthDateBlock, PassportRfBlock}
import ru.yandex.vertis.shark.model.Entity.PassportRfEntity
import ru.yandex.vertis.shark.model.PersonProfileImpl
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.util.RichModel.RichPersonalProfile
import zio.test.{assertTrue, DefaultRunnableSpec, ZSpec}
import zio.test.environment.TestEnvironment

object PassportRfIssueDateValidatedSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("passport rf issue date")(
      test("cofemiss") {
        checkIssued(
          birthday = LocalDate.of(1987, 10, 25),
          issued = LocalDate.of(2008, 4, 2),
          now = LocalDate.of(2021, 3, 5),
          validCheck = true
        )
      },
      test("be valid for 15 y.o. 14 y. issued") {
        checkIssued(
          birthday = LocalDate.of(2000, 1, 1),
          issued = LocalDate.of(2014, 1, 2),
          now = LocalDate.of(2015, 1, 1),
          validCheck = true
        )
      },
      test("be invalid for 13 y.o.") {
        checkIssued(
          birthday = LocalDate.of(2002, 1, 1),
          issued = LocalDate.of(2014, 1, 1),
          now = LocalDate.of(2015, 1, 1),
          validCheck = false
        )
      },
      test("be valid for 35 y.o. 30 y. issued") {
        checkIssued(
          birthday = LocalDate.of(2000, 1, 1),
          issued = LocalDate.of(2030, 1, 1),
          now = LocalDate.of(2035, 1, 1),
          validCheck = true
        )
      },
      test("be invalid for 35 y.o. 19 y. issued") {
        checkIssued(
          birthday = LocalDate.of(2000, 1, 1),
          issued = LocalDate.of(2019, 1, 1),
          now = LocalDate.of(2035, 1, 1),
          validCheck = false
        )
      },
      test("be valid for 69 y.o. 45 y. issued") {
        checkIssued(
          birthday = LocalDate.of(2000, 1, 1),
          issued = LocalDate.of(2045, 1, 2),
          now = LocalDate.of(2069, 1, 1),
          validCheck = true
        )
      },
      test("be invalid for 69 y.o. 20 y. issued") {
        checkIssued(
          birthday = LocalDate.of(2000, 1, 1),
          issued = LocalDate.of(2020, 1, 1),
          now = LocalDate.of(2069, 1, 1),
          validCheck = false
        )
      },
      test("be invalid for 69 y.o. 14 y. issued") {
        checkIssued(
          birthday = LocalDate.of(2000, 1, 1),
          issued = LocalDate.of(2014, 1, 1),
          now = LocalDate.of(2069, 1, 1),
          validCheck = false
        )
      },
      test("be invalid for older than 1997 y. issued") {
        checkIssued(
          birthday = LocalDate.of(1980, 1, 1),
          issued = LocalDate.of(1914, 1, 1),
          now = LocalDate.of(1999, 1, 1),
          validCheck = false
        )
      },
      test("be invalid for day to day . issued") {
        checkIssued(
          birthday = LocalDate.of(2001, 1, 1),
          issued = LocalDate.of(2021, 1, 1),
          now = LocalDate.of(2021, 5, 1),
          validCheck = false
        )
      },
      test("be invalid for issued in the future") {
        checkIssued(
          birthday = LocalDate.of(1975, 8, 3),
          issued = LocalDate.of(2021, 8, 13),
          now = LocalDate.of(2021, 5, 1),
          validCheck = false
        )
      }
    )
  }

  private def checkIssued(
      birthday: LocalDate,
      issued: LocalDate,
      now: LocalDate,
      validCheck: Boolean) = {
    val result = validatedIssueDate(issued, birthday, now)
    if (validCheck) {
      assertTrue(result == Valid(issued))
    } else {
      assertTrue(result.isInstanceOf[Invalid[_]])
    }
  }

  private def validatedIssueDate(issued: LocalDate, birthday: LocalDate, now: LocalDate): Result[LocalDate] = {
    val entity = PassportRfEntity(
      series = "3605".taggedWith,
      number = "186284".taggedWith,
      issueDate = issued,
      departCode = "632-033".taggedWith,
      departName = "АВТОЗАВОДСКИМ РУВД Г. ТОЛЬЯТТИ САМАРСКОЙ ОБЛ.".taggedWith
    )

    val profile = PersonProfileImpl.forTest(
      passportRf = PassportRfBlock(entity).some,
      birthDate = BirthDateBlock(birthday).some
    )
    profile.validatedPassportRfIssueDate(now)
  }
}
