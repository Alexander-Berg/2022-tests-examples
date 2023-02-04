package auto.common.manager.region.test

import auto.common.manager.region.RegionManager.RegionNotFound
import RegionSpecGen.{anyGeoFilter, anyRegion}
import auto.common.manager.region.testkit.{RegionManagerMock, RegionManagerStub}
import auto.common.manager.region.RegionManager
import common.geobase.model.RegionIds
import common.geobase.model.RegionIds.RegionId
import zio.test.Assertion._
import zio.test.mock.Expectation.value
import zio.test._
import zio.ZIO

object RegionManagerSpec extends DefaultRunnableSpec {

  private val Moscow = "Москва"
  private val MoscowAndMoscowRegion = "Москва и Московская область"
  private val Earth = "Земля"
  private val EarthId = RegionId(10000)
  private val FakeRegionId = RegionId(-18)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("RegionManager")(
      testM("Region was found")(
        for {
          moscow <- ZIO.serviceWith[RegionManager](_.getRegion(RegionIds.Moscow))
        } yield assert(moscow.map(_.ruName))(equalTo(Some(Moscow)))
      ),
      testM("Region was not found return None")(
        for {
          moscow <- ZIO.serviceWith[RegionManager](_.getRegion(FakeRegionId))
        } yield assert(moscow.map(_.ruName))(isNone)
      ),
      testM("Regions was not found causes error")(
        assertM(ZIO.serviceWith[RegionManager](_.getRegions(Seq(FakeRegionId))).run)(
          fails(isSubtype[RegionNotFound](anything))
        )
      ),
      testM("Moscow is a subregion of MoscowAndMoscowRegion")(
        for {
          isSubregion <- ZIO.serviceWith[RegionManager](
            _.isSubregion(RegionIds.Moscow, RegionIds.MoscowAndMoscowRegion)
          )
        } yield assert(isSubregion)(isTrue)
      ),
      testM("Moscow is not a subregion of non-existent region")(
        for {
          isSubregion <- ZIO.serviceWith[RegionManager](_.isSubregion(RegionIds.Moscow, FakeRegionId))
        } yield assert(isSubregion)(isFalse)
      ),
      testM("Subregion relation is reflexive")(
        for {
          isSubregion <- ZIO.serviceWith[RegionManager](_.isSubregion(RegionIds.Moscow, RegionIds.Moscow))
        } yield assert(isSubregion)(isTrue)
      ),
      testM("Subregion relation is not commutative")(
        for {
          isSubregion <- ZIO.serviceWith[RegionManager](
            _.isSubregion(RegionIds.MoscowAndMoscowRegion, RegionIds.Moscow)
          )
        } yield assert(isSubregion)(isFalse)
      ),
      testM("Get parent regions") {
        for {
          parentRegions <-
            ZIO.serviceWith[RegionManager](
              _.getParentRegions(Seq(RegionIds.Moscow, RegionIds.MoscowAndMoscowRegion, EarthId))
            )
          parentIds = parentRegions.map(_.map(_.id))
        } yield assert(parentIds)(equalTo(Seq(Some(RegionIds.MoscowAndMoscowRegion.id), Some(3), None)))
      },
      testM("Fail to get parent regions if incorrect id is present") {
        for {
          parentRegions <-
            ZIO
              .serviceWith[RegionManager](
                _.getParentRegions(Seq(RegionIds.Moscow, RegionIds.MoscowAndMoscowRegion, EarthId, FakeRegionId))
              )
              .run
        } yield assert(parentRegions)(fails(equalTo(RegionNotFound(FakeRegionId))))
      },
      testM("Get regions") {
        for {
          parentRegions <- ZIO.serviceWith[RegionManager](
            _.getRegions(Seq(RegionIds.Moscow, RegionIds.MoscowAndMoscowRegion, EarthId))
          )
          parentIds = parentRegions.map(_.ruName)
        } yield assert(parentIds)(equalTo(Seq(Moscow, MoscowAndMoscowRegion, Earth)))
      },
      testM("Fail to get regions if incorrect id is present") {
        for {
          parentRegions <-
            ZIO
              .serviceWith[RegionManager](
                _.getRegions(Seq(RegionIds.Moscow, RegionIds.MoscowAndMoscowRegion, EarthId, FakeRegionId))
              )
              .run
        } yield assert(parentRegions)(fails(equalTo(RegionNotFound(FakeRegionId))))
      },
      testM("should find by radius") {
        checkM(anyRegion(), anyGeoFilter()) { case (region, (rids, geoRadius)) =>
          val getRegionMock = RegionManagerMock
            .GetRegion(equalTo(RegionId(rids.head)), value(Some(region)))
            .atMost(1)

          val findRegionsMock = RegionManagerMock
            .FindNearestCities(anything, value(Set(region)))
            .optional

          val getSettlementsMock = RegionManagerMock
            .GetSettlements(equalTo(region), value(Set(region)))
            .optional

          for {
            manager <- ZIO.service[RegionManager]
            rids <- manager
              .findNearestSettlements(rids, geoRadius)
              .provideCustomLayer(getRegionMock ++ findRegionsMock ++ getSettlementsMock)
          } yield assert(rids.size)(equalTo(rids.size))
        }
      }
    ).provideCustomLayerShared(RegionManagerStub.live)
}
