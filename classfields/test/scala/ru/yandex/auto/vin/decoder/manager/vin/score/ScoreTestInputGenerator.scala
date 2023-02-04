package ru.yandex.auto.vin.decoder.manager.vin.score

import java.io.{File, FileOutputStream, PrintWriter}

object ScoreTestInputGenerator extends App {

  val calculator = new HealthScoreCalculator()

  val HeadLine = List(
    "id",
    "score",
    "year",
    "mark",
    "has_fresh_certs",
    "used_in_carsharing",
    "has_legal_constrains",
    "wanted",
    "last_mileage",
    "used_in_taxi",
    "damages",
    "owners_count"
  ).mkString(";")

  val Years = List(2010, 2015) // 3 * 3 * 2^5 * 4 * 4 * 5
  val Marks = List("BMW", "CHERY", "FORD")
  val Booleans = List(false, true)
  val Mileages = List(50000, 150000)

  val DamageCodes = List(
    List(102, 11), // contains total damages
    List.empty,
    List(113, 107, 210) // first and second class damages
  )
  val OwnersCount = List(1, 2, 3, 5, 8)

  var id = 0

  val inputs = for {
    year <- Years
    mark <- Marks
    hasCerts <- Booleans
    usedInCarsharing <- Booleans
    usedInTaxi <- Booleans
    hasLegalConstraints <- Booleans
    wanted <- Booleans
    mileage <- Mileages
    damages <- DamageCodes
    ownersCount <- OwnersCount
  } yield {
    id += 1
    TestScoreInput(
      id = id,
      year = year,
      mark = mark,
      hasFreshBrandCerts = hasCerts,
      usedInCarsharing = usedInCarsharing,
      hasLegalCostraints = hasLegalConstraints,
      wanted = wanted,
      lastMileage = mileage,
      usedInTaxi = usedInTaxi,
      accidentDamageCodes = damages,
      ownersCount = ownersCount,
      res = calculator
        .getScore(
          year,
          mark,
          hasCerts,
          usedInCarsharing,
          hasLegalConstraints,
          wanted,
          mileage,
          usedInTaxi,
          damages,
          ownersCount,
          calcScoreAtCustomYear = Some(2021)
        )
        .score
    )
  }

  val file = new File(
    "/Users/sievmi/work/auto-vin-decoder/auto-vin-decoder-core/src/test/resources/score/test_input.csv"
  )
  val writer = new PrintWriter(new FileOutputStream(file))

  // val json = JsArray.apply(inputs.map(m => Json.toJson(m)))
  // writer.println(Json.prettyPrint(json))

  writer.println(HeadLine)
  inputs.foreach(i => writer.println(i.toCsvLine))

  writer.close()

}
