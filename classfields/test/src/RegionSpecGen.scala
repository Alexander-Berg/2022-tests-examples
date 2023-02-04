package auto.common.manager.region.test

import common.geobase.{Region, RegionTypes}
import zio.random.Random
import zio.test.Gen._
import zio.test.{Gen, Sized}

object RegionSpecGen {

  def anyRegion[R <: Random with Sized](): Gen[R, Region] =
    for {
      id <- anyInt.map(Math.abs)
      parentId <- anyInt.map(Math.abs)
      chiefRegionId <- anyInt.map(Math.abs)
      regionType <- Gen.elements(
        RegionTypes.Village,
        RegionTypes.City,
        RegionTypes.Country,
        RegionTypes.FederalSubject
      )
      ruName <- anyString
      ruPreposition <- anyString
      ruNamePrepositional <- anyString
      ruNameGenitive <- anyString
      ruNameDative <- anyString
      ruNameAccusative <- anyString
      ruNameInstrumental <- anyString
      latitude <- anyDouble
      longitude <- anyDouble
      tzOffset <- anyInt
      population <- anyLong
    } yield Region(
      id,
      parentId,
      chiefRegionId,
      regionType,
      ruName,
      ruPreposition,
      ruNamePrepositional,
      ruNameGenitive,
      ruNameDative,
      ruNameAccusative,
      ruNameInstrumental,
      latitude,
      longitude,
      tzOffset,
      population
    )

  def anyGeoFilter[R <: Random with Sized](): Gen[R, (Seq[Int], Option[Int])] =
    for {
      geoRadius <- option(anyInt)
      rids <- oneOf(listOf1(anyInt), listOfBounded(1, 5)(anyInt))
    } yield (rids, geoRadius)
}
