package ru.yandex.auto.vin.decoder.partners.adaperio

import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.partners.adaperio.model.report.block.DtpBlock.Dtp
import ru.yandex.auto.vin.decoder.partners.adaperio.model.report.block.MvdRestrictionBlock.MvdRestriction
import ru.yandex.auto.vin.decoder.proto.VinHistory.Constraint

import java.time.{LocalDate, LocalDateTime, LocalTime}
import scala.jdk.CollectionConverters.ListHasAsScala

class AdaperioReportBlockTest extends AnyFunSuite {
  private val localTime = LocalTime.of(0, 0)
  private val localDate = LocalDate.of(1989, 9, 21)
  private val localDateTime = LocalDateTime.of(localDate, localTime)

  test("MvdRestriction") {
    val constraint = Constraint.newBuilder().setDate(622324800000L).setConType("type").build()
    val restriction = MvdRestriction(localDateTime, Some("type"), None, None, None, None)

    assert(restriction.toConstraint.contains(constraint))
    assert(restriction.copy(registrationDate = localDateTime).toConstraint.contains(constraint))
  }

  test("Dtp") {
    val acc = Dtp(
      crashDate = localDateTime,
      accidentType = "",
      region = "",
      carMark = "",
      carModel = "",
      carYear = 12344,
      damageState = "",
      error = None,
      damagePoints = Set("1", "2"),
      accidentNumber = Some("1"),
      status = false
    )
    val accident = acc.toAccident
    assert(accident.getDate == 622324800000L)
    assert(accident.getDamagePointsList.asScala == Seq(1, 2))

    val acc1 = Dtp(
      crashDate = localDateTime,
      accidentType = "",
      region = "",
      carMark = "",
      carModel = "",
      carYear = 12344,
      damageState = "",
      error = None,
      damagePoints = Set("3", "4"),
      accidentNumber = Some("1"),
      status = false
    )
    val accident1 = acc1.toAccident
    assert(accident1.getDate == 622324800000L)
    assert(accident1.getDamagePointsList.asScala == Seq(3, 4))
  }
}
