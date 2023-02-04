package ru.yandex.vertis.scoring.calculator.impl

import java.time.Instant

import cats.implicits.catsSyntaxApplicativeId
import eu.timepit.refined.auto._
import ru.yandex.vertis.quality.cats_utils.Awaitable.AwaitableSyntax
import ru.yandex.vertis.quality.test_utils.SpecBase
import ru.yandex.vertis.scoring.aggregator.impl.StubUserDataAggregator
import ru.yandex.vertis.scoring.calculator.Calculator._
import ru.yandex.vertis.scoring.calculator.impl.CalculatorSpec.TestCase
import ru.yandex.vertis.scoring.model.{PassportUid, Summary, UserData}
import ru.yandex.vertis.scoring.util.CommonPassportInfo.{accountAttributes, karmaInfo1, karmaInfo2}
import vertis.scoring.model.Badge

class CalculatorSpec extends SpecBase {

  private def getSummary(uid: PassportUid): F[Summary] =
    uid.value match {
      case 1L  => summary1.pure
      case 2L  => summary2.pure
      case 3L  => summary3.pure
      case 4L  => summary4.pure
      case 5L  => summary5.pure
      case 6L  => summary6.pure
      case 7L  => summary7.pure
      case 8L  => summary8.pure
      case 9L  => summary9.pure
      case 10L => summary10.pure
    }

  private val stubAggregator = new StubUserDataAggregator[F](getSummary)
  private val calculator = new CalculatorImpl[F](stubAggregator)

  private val updateTime1 = Instant.ofEpochMilli(1)

  private val passportData1 =
    UserData.Passport(
      uid = 1L,
      karmaInfo = karmaInfo2,
      attributes = accountAttributes,
      isAnyPhoneConfirmed = false,
      isAnyPhoneBound = true,
      updateTime = updateTime1
    )

  private val passportData2 =
    passportData1.copy(
      karmaInfo = karmaInfo1
    )

  private val passportData3 =
    passportData1.copy(
      isAnyPhoneBound = false
    )

  private val analystData1 =
    UserData.Analyst(
      uid = 1L,
      diskActivity = true,
      edaActivity = true,
      edaUser = true,
      edaUserBlocked = false,
      kinopoiskActivity = true,
      kinopoiskUser = true,
      lavkaActivity = true,
      lavkaUser = true,
      lavkaUserBlocked = false,
      musicActivity = true,
      personalPhoneIds = None,
      reviewsActivity = true,
      reviewsUser = true,
      taxiActivity = true,
      taxiUser = true,
      taxiUserBlocked = false,
      updateTime = updateTime1
    )

  private val analystData2 =
    analystData1.copy(
      edaActivity = false,
      lavkaActivity = false,
      taxiActivity = false
    )

  private val analystData3 =
    UserData.Analyst(
      uid = 1L,
      diskActivity = false,
      edaActivity = false,
      edaUser = false,
      edaUserBlocked = false,
      kinopoiskActivity = false,
      kinopoiskUser = false,
      lavkaActivity = false,
      lavkaUser = false,
      lavkaUserBlocked = false,
      musicActivity = false,
      personalPhoneIds = None,
      reviewsActivity = false,
      reviewsUser = false,
      taxiActivity = false,
      taxiUser = false,
      taxiUserBlocked = false,
      updateTime = updateTime1
    )

  private val analystData4 =
    analystData1.copy(
      taxiUserBlocked = true
    )

  private val summary1 =
    Summary(
      uid = 1L,
      maybePassportData = Some(passportData1),
      maybeAnalystData = Some(analystData1)
    )

  private val summary2 =
    summary1.copy(
      uid = 2L,
      maybeAnalystData = Some(analystData2)
    )

  private val summary3 =
    summary1.copy(
      uid = 3L,
      maybeAnalystData = Some(analystData3)
    )

  private val summary4 =
    summary1.copy(
      uid = 4L,
      maybeAnalystData = Some(analystData4)
    )

  private val summary5 =
    Summary(
      uid = 5L,
      maybePassportData = Some(passportData2),
      maybeAnalystData = Some(analystData1)
    )

  private val summary6 =
    Summary(
      uid = 6L,
      maybePassportData = Some(passportData3),
      maybeAnalystData = Some(analystData1)
    )

  private val summary7 =
    Summary(
      uid = 7L,
      maybePassportData = Some(passportData1),
      maybeAnalystData = None
    )

  private val summary8 =
    Summary(
      uid = 8L,
      maybePassportData = Some(passportData3),
      maybeAnalystData = None
    )

  private val summary9 =
    Summary(
      uid = 9L,
      maybePassportData = None,
      maybeAnalystData = Some(analystData1)
    )

  private val summary10 =
    Summary(
      uid = 10L,
      maybePassportData = None,
      maybeAnalystData = None
    )

  private val testCases: Seq[TestCase] =
    Seq(
      TestCase(
        "correctly calculate LevelThirtyBadge",
        1L,
        LevelThirtyBadge
      ),
      TestCase(
        "correctly calculate LevelTwentyBadge",
        2L,
        LevelTwentyBadge
      ),
      TestCase(
        "correctly calculate LevelTenBadge",
        3L,
        LevelTenBadge
      ),
      TestCase(
        "correctly calculate NoBadge if user blocked",
        4L,
        NoBadge
      ),
      TestCase(
        "correctly calculate NoBadge if no phone bound",
        5L,
        NoBadge
      ),
      TestCase(
        "correctly calculate NoBadge if user spammer",
        6L,
        NoBadge
      ),
      TestCase(
        "correctly calculate LevelTenBadge with simple decision",
        7L,
        LevelTenBadge
      ),
      TestCase(
        "correctly calculate NoBadge with simple decision user spammer",
        8L,
        NoBadge
      ),
      TestCase(
        "correctly calculate NoBadge if no passport data",
        9L,
        NoBadge
      ),
      TestCase(
        "correctly calculate NoBadge if no data",
        10L,
        NoBadge
      )
    )

  "Calculator" should {
    testCases.foreach { case TestCase(description, uid, expectedBadge) =>
      description in {
        calculator.calculate(uid, "127.0.0.1").await shouldBe expectedBadge
      }
    }
  }
}

object CalculatorSpec {

  case class TestCase(description: String, uid: PassportUid, expectedBadge: Badge)
}
