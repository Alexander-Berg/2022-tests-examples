package auto.c2b.common.testkit

import auto.c2b.common.model.ApplicationTypes.{UserId, VIN}
import auto.c2b.common.proposition.Proposition
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.string.Url
import ru.auto.api.api_offer_model.{Documents, Offer}
import scalapb.UnknownFieldSet
import zio.test.Gen
import zio.test.magnolia.DeriveGen

import java.time.temporal.ChronoUnit
import java.time._

object CommonGenerators {

  implicit val offsetDataTimeDeriveGen: DeriveGen[OffsetDateTime] = DeriveGen.instance {
    (Gen.int(2000, 2030) <&>
      Gen.int(1, 12) <&> Gen.int(1, 28) <&> Gen.int(0, 23) <&> Gen.int(0, 59) <&> Gen.int(0, 59)).map {
      case (((((year, month), day), hour), minute), second) =>
        OffsetDateTime.of(year, month, day, hour, minute, second, 0, ZoneOffset.UTC)
    }
  }

  implicit val instantDeriveGen: DeriveGen[Instant] = DeriveGen.instance {
    val now = Instant.now()
    val twentyYears = 20 * 365
    Gen.instant(now.minus(twentyYears, ChronoUnit.DAYS), now.plus(twentyYears, ChronoUnit.DAYS))
  }

  implicit val localDateDeriveGen: DeriveGen[LocalDate] = DeriveGen.instance {
    for {
      year <- Gen.int(2000, 2050).map(Year.of)
      month <- Gen.int(1, 12)
      maxLen = if (!year.isLeap && month == 2) 28 else Month.of(month).maxLength
      day <- Gen.int(1, maxLen)
    } yield LocalDate.of(year.getValue, month, day)
  }

  implicit val localTimeDeriveGen: DeriveGen[LocalTime] = DeriveGen.instance {
    for {
      hour <- Gen.int(0, 23)
      minute <- Gen.int(0, 59)
      second <- Gen.int(0, 59)
    } yield LocalTime.of(hour, minute, second)
  }

  implicit val unknownFieldGen: DeriveGen[UnknownFieldSet] = DeriveGen.instance(Gen.const(UnknownFieldSet.empty))

  implicit val refinedNonNegativeIntDeriveGen: DeriveGen[Refined[Int, NonNegative]] =
    DeriveGen.instance(Gen.int(0, Int.MaxValue).map(Refined.unsafeApply))

  implicit val refinedNonNegativeLongDeriveGen: DeriveGen[Refined[Long, NonNegative]] =
    DeriveGen.instance(Gen.long(0, Long.MaxValue).map(Refined.unsafeApply))

  implicit val refinedVINGen: DeriveGen[VIN] =
    DeriveGen.instance(
      Gen.stringN(17)(Gen.elements("ABCDEFGHJKLMNPRSTUVWXYZ0123456789": _*)).map(VIN.apply).collect { case Right(vin) =>
        vin
      }
    )

  implicit val refinedUrlDeriveGen: DeriveGen[Refined[String, Url]] =
    DeriveGen.instance(Gen.anyUUID.map(id => s"http://s3.yandex.ru/$id").map(Refined.unsafeApply))

  implicit val offerGen: DeriveGen[Offer] = DeriveGen.instance {
    refinedVINGen.derive.flatMap(validVin => Gen.const(Offer(documents = Some(Documents(vin = validVin.value)))))
  }

  implicit val propositionGen: DeriveGen[Proposition] = DeriveGen.instance {
    val genPositiveLong = DeriveGen.genLong.derive.map(num => if (num < 0) -num else num)
    val genAnyString = DeriveGen.genString.derive
    (genAnyString <*> genPositiveLong).map { case (anyString, value) => Proposition.of(anyString, value, anyString) }
  }

  implicit val userIdAny: DeriveGen[UserId] = DeriveGen.instance {
    Gen
      .oneOf(Gen.const("user"), Gen.const("dealer"))
      .flatMap(prefix => Gen.anyLong.map(l => s"$prefix:$l"))
      .map(UserId(_))
  }
}
