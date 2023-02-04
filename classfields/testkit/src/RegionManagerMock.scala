package auto.common.manager.region.testkit

import auto.common.manager.region.RegionManager
import common.geobase.Region
import common.geobase.RegionTypes.Type
import common.geobase.model.RegionIds
import zio.test.mock.mockable
import zio.{IO, UIO, ZLayer}

import java.time.ZoneOffset

@mockable[RegionManager]
object RegionManagerMock

object RegionManagerEmpty {

  val empty = ZLayer.succeed(new RegionManager {
    override def getRegion(regionId: RegionIds.RegionId): UIO[Option[Region]] = ???

    override def getRegions(regionIds: Seq[RegionIds.RegionId]): IO[RegionManager.RegionManagerError, Seq[Region]] = ???

    override def getParentRegions(
        regionIds: Seq[RegionIds.RegionId]): IO[RegionManager.RegionManagerError, Seq[Option[Region]]] = ???

    override def getParentWithType(
        regionId: RegionIds.RegionId,
        `type`: Type): IO[RegionManager.RegionManagerError, Option[Region]] = ???

    override def getZoneOffset(regionId: RegionIds.RegionId): IO[RegionManager.RegionManagerError, ZoneOffset] = ???

    override def getFederalSubject(city: Region): UIO[Option[Region]] = ???

    override def getFederalSubjectId(
        regionId: RegionIds.RegionId): IO[RegionManager.RegionManagerError, RegionIds.RegionId] = ???

    override def isSubregion(regionId: RegionIds.RegionId, parent: RegionIds.RegionId): UIO[Boolean] = ???

    override def isInside(rawRegionId: Long, targetRegionsRawIds: Set[Long]): UIO[Boolean] = ???

    override def getCity(regionId: RegionIds.RegionId): IO[RegionManager.RegionManagerError, Option[Region]] = ???

    override def getSettlements(region: Region): UIO[Set[Region]] = ???

    override def findNearestCities(latFrom: Double, latTo: Double, lonFrom: Double, lonTo: Double): UIO[Set[Region]] =
      ???

    override def findNearestSettlements(rawRegionIds: Seq[Int], geoRadius: Option[Int]): UIO[Set[Int]] = ???
  })
}
