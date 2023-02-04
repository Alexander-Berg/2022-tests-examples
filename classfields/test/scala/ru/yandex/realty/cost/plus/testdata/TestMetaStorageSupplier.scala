package ru.yandex.realty.cost.plus.testdata

import eu.timepit.refined.auto._
import ru.yandex.realty.cost.plus.model.yml.WeakYmlSet
import ru.yandex.realty.model.location.{GeoPoint, Location}
import ru.yandex.realty.model.region.NodeRgid
import ru.yandex.realty.model.sites.{Photo, Site}
import ru.yandex.realty.sites.stat.SiteInfoStorage
import ru.yandex.realty.sites.stat.model.{SiteInfoItem, SiteOffersStat, SiteRooms, SiteRoomsStat}
import ru.yandex.realty.testkit.SiteMetaDataStorageSupplier
import ru.yandex.realty.traffic.model.site.SiteMetaData
import ru.yandex.realty.traffic.service.site.SiteMetaDataStorage
import ru.yandex.vertis.mockito.MockitoSupport

import scala.collection.JavaConverters._

object TestMetaStorageSupplier extends MockitoSupport {

  val SiteGeoPoint: GeoPoint = new GeoPoint(1, 1)

  private def simpleSite(id: Long, alias: String): Site = {
    val location = {
      val l = new Location()
      l.setManualPoint(SiteGeoPoint)
      l
    }
    val res = new Site(id)

    def photo(suffix: String) = {
      val p = new Photo("")
      p.setUrlPrefix(s"//fake.img/$alias/$suffix")
      p
    }

    res.setMainPhoto(photo("mainphoto"))
    res.setPhotos(Seq("add1", "add2", "add3").map(photo).asJava)
    res.setLocation(location)
    res.setName(alias)
    res.setLocativeFullName(alias)
    res
  }

  val ZhkSloboda: Site =
    simpleSite(
      893535,
      "sloboda"
    )

  val ZhkLyublinskijPark: Site = simpleSite(
    1686471,
    "lyublinskij-park"
  )

  val ZhkTest: Site = simpleSite(
    1686472,
    "test"
  )

  private val AllSites = Seq(ZhkTest, ZhkSloboda, ZhkLyublinskijPark)

  private val infoStorage = SiteInfoStorage(
    Map(
      ZhkSloboda.getId -> SiteInfoItem(
        Map(
          SiteRooms.OneRoom -> SiteRoomsStat(
            fromDeveloperStat = Some(
              SiteOffersStat(
                priceFrom = 1000000L,
                priceTo = 10000000L,
                areaFrom = 30f,
                areaTo = 100f,
                offersCount = 20
              )
            ),
            secondaryStat = Some(
              SiteOffersStat(
                priceFrom = 1000000L,
                priceTo = 10000000L,
                areaFrom = 30f,
                areaTo = 100f,
                offersCount = 3
              )
            )
          ),
          SiteRooms.TwoRooms -> SiteRoomsStat(
            fromDeveloperStat = Some(
              SiteOffersStat(
                priceFrom = 2000000L,
                priceTo = 20000000L,
                areaFrom = 40f,
                areaTo = 100f,
                offersCount = 3
              )
            ),
            secondaryStat = None
          ),
          SiteRooms.ThreeRooms -> SiteRoomsStat(
            fromDeveloperStat = Some(
              SiteOffersStat(
                priceFrom = 3000000L,
                priceTo = 10000000L,
                areaFrom = 50f,
                areaTo = 100f,
                offersCount = 30
              )
            ),
            secondaryStat = Some(
              SiteOffersStat(
                priceFrom = 3003000L,
                priceTo = 10000000L,
                areaFrom = 30f,
                areaTo = 100f,
                offersCount = 3
              )
            )
          ),
          SiteRooms.Studio -> SiteRoomsStat(
            fromDeveloperStat = Some(
              SiteOffersStat(
                priceFrom = 1000000L,
                priceTo = 10000000L,
                areaFrom = 12f,
                areaTo = 100f,
                offersCount = 10
              )
            ),
            secondaryStat = Some(
              SiteOffersStat(
                priceFrom = 1000000L,
                priceTo = 10000000L,
                areaFrom = 30f,
                areaTo = 100f,
                offersCount = 3
              )
            )
          ),
          SiteRooms.FourPlusRooms -> SiteRoomsStat(
            fromDeveloperStat = None,
            secondaryStat = Some(
              SiteOffersStat(
                priceFrom = 1000000L,
                priceTo = 10000000L,
                areaFrom = 12f,
                areaTo = 100f,
                offersCount = 100
              )
            )
          )
        ),
        isPaid = true,
        Some(NodeRgid.MOSCOW),
        kitchenSpaceMin = None,
        kitchenSpaceMax = None
      ),
      ZhkLyublinskijPark.getId ->
        SiteInfoItem(
          Map(
            SiteRooms.OneRoom -> SiteRoomsStat(
              fromDeveloperStat = None,
              secondaryStat = Some(
                SiteOffersStat(
                  priceFrom = 1000000L,
                  priceTo = 10000000L,
                  areaFrom = 30f,
                  areaTo = 100f,
                  offersCount = 3
                )
              )
            ),
            SiteRooms.TwoRooms -> SiteRoomsStat(
              secondaryStat = Some(
                SiteOffersStat(
                  priceFrom = 2000000L,
                  priceTo = 20000000L,
                  areaFrom = 40f,
                  areaTo = 100f,
                  offersCount = 3
                )
              ),
              fromDeveloperStat = None
            ),
            SiteRooms.ThreeRooms -> SiteRoomsStat(
              fromDeveloperStat = None,
              secondaryStat = Some(
                SiteOffersStat(
                  priceFrom = 3003000L,
                  priceTo = 10000000L,
                  areaFrom = 30f,
                  areaTo = 100f,
                  offersCount = 3
                )
              )
            ),
            SiteRooms.Studio -> SiteRoomsStat(
              fromDeveloperStat = None,
              secondaryStat = Some(
                SiteOffersStat(
                  priceFrom = 1000000L,
                  priceTo = 10000000L,
                  areaFrom = 30f,
                  areaTo = 100f,
                  offersCount = 3
                )
              )
            ),
            SiteRooms.FourPlusRooms -> SiteRoomsStat(
              fromDeveloperStat = None,
              secondaryStat = None
            )
          ),
          isPaid = false,
          Some(NodeRgid.MOSCOW),
          kitchenSpaceMin = None,
          kitchenSpaceMax = None
        ),
      ZhkTest.getId ->
        SiteInfoItem(
          Map(
            SiteRooms.OneRoom -> SiteRoomsStat(
              fromDeveloperStat = None,
              secondaryStat = Some(
                SiteOffersStat(
                  priceFrom = 1000000L,
                  priceTo = 10000000L,
                  areaFrom = 30f,
                  areaTo = 100f,
                  offersCount = 3
                )
              )
            )
          ),
          isPaid = true,
          None,
          kitchenSpaceMin = None,
          kitchenSpaceMax = None
        )
    )
  )

  lazy val metaStorage: SiteMetaDataStorage.Service = zio.Runtime.default.unsafeRun {
    SiteMetaDataStorageSupplier.supply(AllSites, infoStorage)
  }

  def buildHeaderSet(siteMeta: SiteMetaData): Option[WeakYmlSet] =
    Some(siteMeta)
      .map(_.total.primaryUrlPath)
      .filter(!_.contains("?"))
      .map(WeakYmlSet(_, siteMeta.tableTitle))

  lazy val SlobodaHeaderSet: WeakYmlSet =
    buildHeaderSet(metaStorage.getForSite(ZhkSloboda.getId).flatMap(_.metaData).get).get

}
