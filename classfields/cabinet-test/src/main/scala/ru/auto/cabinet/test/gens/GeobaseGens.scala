package ru.auto.cabinet.test.gens

import org.scalacheck.Gen
import ru.auto.cabinet.model.{CityId, RegionId}

object GeobaseGens {

  val regionIdGen: Gen[RegionId] = Gen.posNum[RegionId]
  val cityIdGen: Gen[CityId] = Gen.posNum[CityId]
}
