package ru.yandex.auto.vin.decoder.manager.vin.score

import play.api.libs.json.{Json, OFormat}

case class TestScoreInput(
    id: Int,
    res: Float,
    year: Int,
    mark: String,
    hasFreshBrandCerts: Boolean,
    usedInCarsharing: Boolean,
    hasLegalCostraints: Boolean,
    wanted: Boolean,
    lastMileage: Int,
    usedInTaxi: Boolean,
    accidentDamageCodes: List[Int],
    ownersCount: Int) {

  def toCsvLine: String = {
    s"$id;$res;$year;$mark;$hasFreshBrandCerts;$usedInCarsharing;$hasLegalCostraints;$wanted;$lastMileage;$usedInTaxi;${accidentDamageCodes
        .mkString(",")};$ownersCount"
  }
}

object TestScoreInput {
  implicit val testScoreInputReads: OFormat[TestScoreInput] = Json.format[TestScoreInput]

  def fromCsvLine(csv: String): TestScoreInput = {
    csv.split(";") match {
      case Array(
            id,
            res,
            year,
            mark,
            hasFreshBrandCerts,
            usedInCarsharing,
            hasLegalCostraints,
            wanted,
            lastMileage,
            usedInTaxi,
            rawDamages,
            ownersCount
          ) =>
        val damages = rawDamages.split(",").filter(_.nonEmpty).map(_.toInt)
        TestScoreInput(
          id.toInt,
          res.toFloat,
          year.toInt,
          mark,
          hasFreshBrandCerts.toBoolean,
          usedInCarsharing.toBoolean,
          hasLegalCostraints.toBoolean,
          wanted.toBoolean,
          lastMileage.toInt,
          usedInTaxi.toBoolean,
          damages.toList,
          ownersCount.toInt
        )
      case _ => throw new RuntimeException("Can't parse csv line")
    }
  }
}
