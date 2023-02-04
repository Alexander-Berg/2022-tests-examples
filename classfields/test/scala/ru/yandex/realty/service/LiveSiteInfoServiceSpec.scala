package ru.yandex.realty.service

import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import org.junit.runner.RunWith
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.method.SiteStatistic
import ru.yandex.realty.model.location.{GeoPoint, Location}
import ru.yandex.realty.model.offer.Rooms
import ru.yandex.realty.model.sites.{Photo, Site}
import ru.yandex.realty.service.LiveSiteInfoServiceSpec.{StubCanonicals, StubStatisticsService}
import ru.yandex.realty.service.SiteInfoService.ErrorType
import ru.yandex.realty.service.storage.SupportCanonicalsUrlStorage
import ru.yandex.realty.sites.stat.SiteInfoStorage
import ru.yandex.realty.sites.stat.model.{SiteInfoItem, SiteOffersStat, SiteRooms, SiteRoomsStat}
import ru.yandex.realty.traffic.model
import ru.yandex.realty.traffic.model.offer.OfferRooms
import ru.yandex.realty.traffic.model.site.SiteMetaData.MoreThan2Elems
import ru.yandex.realty.traffic.model.site.StatItem.StatItemValue
import ru.yandex.realty.traffic.model.site.{SiteByRoomInfo, SiteMetaData, StatItem}
import zio.test.Assertion._
import zio.test._
import zio.test.junit.{JUnitRunnableSpec, ZTestJUnitRunner}
import zio.{IO, Task, ZIO, ZLayer}

import java.time.Instant
import scala.collection.JavaConverters._

@RunWith(classOf[ZTestJUnitRunner])
class LiveSiteInfoServiceSpec extends JUnitRunnableSpec {

  private val now = Instant.ofEpochMilli(Instant.now().toEpochMilli)

  private val geoPoint = Some(new GeoPoint(1f, 1f))
  private val emptyStat = SiteInfoItem(
    byRoomsStat = Map.empty,
    isPaid = true,
    populatedRgid = None,
    kitchenSpaceMin = None,
    kitchenSpaceMax = None
  )
  private val emptyStat2 = SiteInfoItem(
    byRoomsStat = Map(SiteRooms.OneRoom -> SiteRoomsStat(None, None)),
    isPaid = true,
    populatedRgid = None,
    kitchenSpaceMin = None,
    kitchenSpaceMax = None
  )

  private val offersStat =
    SiteOffersStat(
      priceFrom = 1000L,
      priceTo = 100000L,
      areaFrom = 10f,
      areaTo = 100f,
      offersCount = 4
    )

  private def getInfo(site: Site, stat: Provider[SiteInfoStorage]) = {
    val serviceL =
      ZLayer.succeed(stat) ++
        ZLayer.succeed[SupportCanonicalsUrlStorage.Service](StubCanonicals) ++
        ZLayer.succeed[SiteStatisticsService.Service](StubStatisticsService) >>> SiteInfoService.live

    SiteInfoService
      .getInfo(site)
      .provideLayer(serviceL)
  }

  private def failureTest(site: Site, stat: Provider[SiteInfoStorage], expected: ErrorType) =
    assertM(
      getInfo(site, stat).run
    )(fails(equalTo(expected)))

  private def succeedTest(site: Site, stat: Provider[SiteInfoStorage], expected: SiteMetaData) =
    getInfo(site, stat).map(res => assertTrue(res.copy(stat = res.stat.copy(updated = now)) == expected))

  //noinspection ScalaStyle
  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("SiteInfoService")(
      failureTests(),
      testM("should correctly build result") {
        val site = makeSite(
          1,
          mainPhoto = Some(photo("/main")),
          additionalPhotos = Seq(photo("/add1"), photo("/add2"), photo("/add3")),
          geoPoint = geoPoint,
          locativeFullName = Some("Valo")
        )

        val stat = SiteInfoItem(
          byRoomsStat = Map(
            SiteRooms.OneRoom -> SiteRoomsStat(
              fromDeveloperStat = Some(offersStat),
              secondaryStat = None
            ),
            SiteRooms.TwoRooms -> SiteRoomsStat(
              fromDeveloperStat = None,
              secondaryStat = Some(offersStat)
            ),
            SiteRooms.Studio -> SiteRoomsStat(
              fromDeveloperStat = Some(offersStat),
              secondaryStat = Some(offersStat)
            )
          ),
          isPaid = true,
          populatedRgid = None,
          kitchenSpaceMin = None,
          kitchenSpaceMax = None
        )

        val expected =
          SiteMetaData(
            byRooms = Map(
              OfferRooms._1 -> SiteByRoomInfo(
                StatItem.PrimaryStat(
                  StatItemValue(
                    1000L,
                    4L,
                    10
                  )
                ),
                "/site/1/mainpage/?roomsTotal=1",
                "/site/1/listing/1/"
              ),
              OfferRooms._2 -> model.site.SiteByRoomInfo(
                StatItem.SecondaryStat(
                  StatItemValue(
                    1000L,
                    4L,
                    10
                  )
                ),
                "/site/1/mainpage/?roomsTotal=2",
                "/site/1/listing/2/"
              ),
              OfferRooms.Studio -> model.site.SiteByRoomInfo(
                StatItem.AllStat(
                  primaryS = StatItemValue(
                    1000L,
                    4L,
                    10
                  ),
                  secondaryS = StatItemValue(
                    1000L,
                    4L,
                    10
                  )
                ),
                "/site/1/mainpage/?roomsTotal=STUDIO",
                "/site/1/listing/STUDIO/"
              )
            ),
            isPaid = true,
            mainPhoto = "https:/main/wiz_t2",
            additionalImages = Refined.unsafeApply[Seq[String], MoreThan2Elems](
              Seq("https:/add1/app_middle", "https:/add2/app_middle", "https:/add3/app_middle")
            ),
            tableTitle = "ЖК «Valo» — официальная информация, цены на квартиры",
            description = "Цены, планировки и наличие квартир. Актуальные предложения Valo. ",
            latitude = geoPoint.value.getLatitude,
            longitude = geoPoint.value.getLongitude,
            total = model.site.SiteByRoomInfo(
              StatItem.AllStat(
                primaryS = StatItemValue(
                  1000L,
                  8L,
                  10
                ),
                secondaryS = StatItemValue(
                  1000L,
                  8L,
                  10
                )
              ),
              "/site/1/mainpage/",
              "/site/1/listing/"
            ),
            SiteMetaData.Stat(now, 1, 1)
          )

        succeedTest(site, () => SiteInfoStorage(Map(1L -> stat)), expected)
      }
    )

  private def photo(alias: String): Photo = {
    val p = new Photo(alias)
    p.setUrlPrefix(alias)
    p
  }

  private def makeSite(
    id: Long,
    mainPhoto: Option[Photo] = None,
    additionalPhotos: Seq[Photo] = Seq.empty,
    geoPoint: Option[GeoPoint] = None,
    locativeFullName: Option[String] = None
  ): Site = {
    val res = new Site(id)

    mainPhoto.foreach(res.setMainPhoto)
    res.setPhotos(additionalPhotos.asJava)
    locativeFullName.foreach { n =>
      res.setLocativeFullName(n)
      res.setName(n)
    }

    val loc = new Location
    geoPoint.foreach(loc.setManualPoint)
    res.setLocation(loc)

    res
  }

  //noinspection ScalaStyle
  private def failureTests() = {
    suite("should correctly fail when")(
      testM("no site main photo") {
        val site = makeSite(1)

        failureTest(site, () => SiteInfoStorage(Map.empty), ErrorType.NoMainPhoto)
      },
      testM("no additional photos") {
        val site = makeSite(
          1,
          mainPhoto = Some(photo("/main"))
        )

        failureTest(site, () => SiteInfoStorage(Map.empty), ErrorType.ToFewAdditionalPhotos)
      },
      testM("less than 3 additional photos") {
        val site = makeSite(
          1,
          mainPhoto = Some(photo("/main")),
          additionalPhotos = Seq(photo("/add1"))
        )

        val site2 = makeSite(
          1,
          mainPhoto = Some(photo("/main")),
          additionalPhotos = Seq(photo("/add1"), photo("/add2"))
        )

        ZIO
          .collectAll {
            Iterable(
              failureTest(site, () => SiteInfoStorage(Map.empty), ErrorType.ToFewAdditionalPhotos),
              failureTest(site2, () => SiteInfoStorage(Map.empty), ErrorType.ToFewAdditionalPhotos)
            )
          }
          .map(_.reduce(_ && _))
      },
      testM("no geo point") {
        val site = makeSite(
          1,
          mainPhoto = Some(photo("/main")),
          additionalPhotos = Seq(photo("/add1"), photo("/add2"), photo("/add3"))
        )

        failureTest(site, () => SiteInfoStorage(Map.empty), ErrorType.NoGeoPoint)
      },
      testM("no locative name") {
        val site = makeSite(
          1,
          mainPhoto = Some(photo("/main")),
          additionalPhotos = Seq(photo("/add1"), photo("/add2"), photo("/add3")),
          geoPoint = geoPoint
        )

        failureTest(site, () => SiteInfoStorage(Map.empty), ErrorType.NoDescription)
      },
      testM("no live stat") {
        val site = makeSite(
          1,
          mainPhoto = Some(photo("/main")),
          additionalPhotos = Seq(photo("/add1"), photo("/add2"), photo("/add3")),
          geoPoint = geoPoint,
          locativeFullName = Some("Valo")
        )

        failureTest(site, () => SiteInfoStorage(Map.empty), ErrorType.NoLiveStat)
      },
      testM("empty live stat") {
        val site = makeSite(
          1,
          mainPhoto = Some(photo("/main")),
          additionalPhotos = Seq(photo("/add1"), photo("/add2"), photo("/add3")),
          geoPoint = geoPoint,
          locativeFullName = Some("Valo")
        )

        ZIO
          .foreach(Iterable(emptyStat, emptyStat2)) { stat =>
            failureTest(site, () => SiteInfoStorage(Map(1L -> stat)), ErrorType.DetailedInfoBuildError)
          }
          .map(_.reduce(_ && _))
      }
    )
  }
}

object LiveSiteInfoServiceSpec {

  object StubCanonicals extends SupportCanonicalsUrlStorage.Service {
    override def findForSite(site: Site): IO[Option[Nothing], String] =
      IO.succeed(s"/site/${site.getId}/mainpage/")

    override def findForSiteOffersListing(site: Site, rooms: Option[Rooms]): IO[Option[Nothing], String] =
      IO.succeed(s"/site/${site.getId}/listing/${rooms.map(Rooms.asString).map(_ + "/").getOrElse("")}")
  }

  object StubStatisticsService extends SiteStatisticsService.Service {
    override def monthly(siteId: Long, siteStat: SiteStatistic): Task[Int] = Task(1)
  }
}
