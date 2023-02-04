package ru.auto.cabinet.test.gens

import java.time.OffsetDateTime

import org.scalacheck.Gen

object DateTimeGens {

  val offsetDateTimeGen: Gen[OffsetDateTime] =
    Gen.choose(-1000000, 1000000).map(OffsetDateTime.now().plusSeconds(_))
}
