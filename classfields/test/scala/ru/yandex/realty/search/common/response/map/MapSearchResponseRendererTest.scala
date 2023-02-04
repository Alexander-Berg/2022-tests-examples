package ru.yandex.realty.search.common.response.map

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.model.location.GeoPoint
import ru.yandex.realty.model.sites.Site
import ru.yandex.realty.proto.village.Village
import ru.yandex.realty.search.common.request.ViewportInfo
import ru.yandex.realty.search.common.response.map.BaseMapSearchResponseRenderer.{
  MinBoundingBoxHeight,
  MinBoundingBoxWidth
}
import ru.yandex.realty.sites.SitesGroupingService
import ru.yandex.realty.util.geo.GeoUtils
import ru.yandex.realty.villages.VillagesStorage

/**
  * @see [[BaseMapSearchResponseRenderer]]
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class MapSearchResponseRendererTest extends FlatSpec with Matchers with MockFactory with BeforeAndAfterEach {

  private val sitesServiceMock = mock[SitesGroupingService]
  private val villagesProviderStub = new Provider[VillagesStorage] {
    override def get(): VillagesStorage = {
      new VillagesStorage {
        override def get(id: Long): Option[Village] = None

        override def getAll: Seq[Village] = Seq()
      }
    }
  }

  private def mockSite(id: Long): Site = {
    val result = new Site(id)
    result.setName(id.toString)
    result
  }

  val renderer = new OfferMapSearchResponseRenderer(sitesServiceMock, villagesProviderStub)
  val rendererSite = new SiteMapSearchResponseRenderer

  override def beforeEach(): Unit = {
    withExpectations {
      (sitesServiceMock.getSiteById _)
        .expects(*)
        .onCall((siteId: Long) => mockSite(siteId))
        .anyNumberOfTimes()
    }
  }

  "MapSearchResponseRenderer" should "correctly render clusterStats for secondary" in {
    val viewport = createViewport(
      topLat = 60.01605584472078f,
      bottomLat = 59.825802220929795f,
      leftLon = 30.21226157300802f,
      rightLon = 30.455878656239634f,
      width = 1440,
      height = 2240,
      dpi = 640,
      cellSize = 0.4f
    )
    val offer = createOffer(
      lat = 60.031174f,
      lon = 30.44059f,
      offerId = 123456789L,
      price = 3003003L
    )
    val house = createHouse(
      lat = 59.92924f,
      lon = 30.357002f,
      offerCount = 3,
      price = 5005005L,
      multiHouseId = 987654321L
    )
    val cluster = createCluster(
      lat = 59.992603f,
      lon = 30.41659f,
      offerCount = 2,
      price = 2002002L,
      minLat = 59.984997f,
      maxLat = 60.005264f,
      minLon = 30.413801f,
      maxLon = 30.42097f
    )
    val minBoundingBoxOffer = GeoUtils.getBoundingBox(
      GeoPoint.getPoint(offer.latitude(), offer.longitude()),
      MinBoundingBoxHeight,
      MinBoundingBoxWidth
    )
    val maxOfferLatitudeLengthHalf: Float = (minBoundingBoxOffer.getMaxLatitude - minBoundingBoxOffer.getMinLatitude) / 2.0f
    val maxOfferLongitudeLengthHalf: Float = (minBoundingBoxOffer.getMaxLongitude - minBoundingBoxOffer.getMinLongitude) / 2.0f

    val minBoundingBoxHouse = GeoUtils.getBoundingBox(
      GeoPoint.getPoint(house.latitude(), house.longitude()),
      MinBoundingBoxHeight,
      MinBoundingBoxWidth
    )
    val maxHouseLatitudeLengthHalf: Float = (minBoundingBoxHouse.getMaxLatitude - minBoundingBoxHouse.getMinLatitude) / 2.0f
    val maxHouseLongitudeLengthHalf: Float = (minBoundingBoxHouse.getMaxLongitude - minBoundingBoxHouse.getMinLongitude) / 2.0f

    val clusters = Array(offer, house, cluster)
    val mapSearchResult = MapSearchResult(detailed = false, clusters = Array(clusters), viewportOfferCount = 6)
    val resp = renderer.renderMapSearchResponse(viewport, mapSearchResult)
    resp.clusters.isDefined should be(true)
    resp.clusters.get.size should be(3)
    resp.clusters.get should be(
      Seq(
        MapClusterStatResponse(
          latitude = offer.latitude(),
          longitude = offer.longitude(),
          leftTop =
            MapGeoPoint(offer.latitude() + maxOfferLatitudeLengthHalf, offer.longitude() - maxOfferLongitudeLengthHalf),
          rightBottom =
            MapGeoPoint(offer.latitude() - maxOfferLatitudeLengthHalf, offer.longitude() + maxOfferLongitudeLengthHalf),
          itemCount = 1
        ),
        MapClusterStatResponse(
          latitude = cluster.latitude(),
          longitude = cluster.longitude(),
          leftTop = MapGeoPoint(cluster.maxLatitude, cluster.minLongitude),
          rightBottom = MapGeoPoint(cluster.minLatitude, cluster.maxLongitude),
          itemCount = cluster.offerCount
        ),
        MapClusterStatResponse(
          latitude = house.latitude(),
          longitude = house.longitude(),
          leftTop =
            MapGeoPoint(house.latitude() + maxHouseLatitudeLengthHalf, house.longitude() - maxHouseLongitudeLengthHalf),
          rightBottom =
            MapGeoPoint(house.latitude() - maxHouseLatitudeLengthHalf, house.longitude() + maxHouseLongitudeLengthHalf),
          itemCount = house.offerCount
        )
      )
    )

    resp.offers.isEmpty should be(true)
    resp.multiHouses.isEmpty should be(true)
    resp.sites.isEmpty should be(true)
  }

  "MapSearchResponseRenderer" should "correctly render clusterStats for sites" in {
    val viewport = createViewport(
      topLat = 60.01605584472078f,
      bottomLat = 59.825802220929795f,
      leftLon = 30.21226157300802f,
      rightLon = 30.455878656239634f,
      width = 1440,
      height = 2240,
      dpi = 640,
      cellSize = 0.4f
    )
    val site = createSite(
      lat = 60.031174f,
      lon = 30.44059f,
      siteId = 31213L,
      price = 3003003L
    )
    val cluster = createCluster(
      lat = 59.992603f,
      lon = 30.41659f,
      offerCount = 2,
      price = 2002002L,
      minLat = 59.984997f,
      maxLat = 60.005264f,
      minLon = 30.413801f,
      maxLon = 30.42097f
    )
    val clusters = Array(site, cluster)
    val mapSearchResult = MapSearchResult(detailed = false, clusters = Array(clusters), 2)
    val resp = renderer.renderMapSearchResponse(viewport, mapSearchResult)
    val minBoundingBox = GeoUtils.getBoundingBox(
      GeoPoint.getPoint(site.latitude(), site.longitude()),
      MinBoundingBoxHeight,
      MinBoundingBoxWidth
    )
    val maxLatitudeLengthHalf: Float = (minBoundingBox.getMaxLatitude - minBoundingBox.getMinLatitude) / 2.0f
    val maxLongitudeLengthHalf: Float = (minBoundingBox.getMaxLongitude - minBoundingBox.getMinLongitude) / 2.0f

    resp.clusters.isDefined should be(true)
    resp.clusters.get.size should be(2)
    resp.clusters.get should be(
      Seq(
        MapClusterStatResponse(
          latitude = site.latitude(),
          longitude = site.longitude(),
          leftTop = MapGeoPoint(site.latitude() + maxLatitudeLengthHalf, site.longitude() - maxLongitudeLengthHalf),
          rightBottom = MapGeoPoint(site.latitude() - maxLatitudeLengthHalf, site.longitude() + maxLongitudeLengthHalf),
          itemCount = 1
        ),
        MapClusterStatResponse(
          latitude = cluster.latitude(),
          longitude = cluster.longitude(),
          leftTop = MapGeoPoint(cluster.maxLatitude, cluster.minLongitude),
          rightBottom = MapGeoPoint(cluster.minLatitude, cluster.maxLongitude),
          itemCount = cluster.offerCount
        )
      )
    )

    resp.offers.isEmpty should be(true)
    resp.multiHouses.isEmpty should be(true)
    resp.sites.isEmpty should be(true)
  }

  "MapSearchResponseRenderer" should "correctly render offers and houses for secondary" in {
    val viewport = createViewport(
      topLat = 59.94621105841496f,
      bottomLat = 59.94071955429965f,
      leftLon = 30.342247030424687f,
      rightLon = 30.349283597361847f,
      width = 1440,
      height = 2240,
      dpi = 640,
      cellSize = 0.4f
    )

    val offer = createOffer(
      lat = 59.944946f,
      lon = 30.344767f,
      offerId = 123456789L,
      price = 18500000L
    )

    val house = createHouse(
      lat = 59.94296f,
      lon = 30.347263f,
      offerCount = 2,
      price = 25000000L,
      multiHouseId = 987654321L
    )

    val clusters = Array(offer, house)
    val mapSearchResult = MapSearchResult(detailed = false, clusters = Array(clusters), 3)
    val resp = renderer.renderMapSearchResponse(viewport, mapSearchResult)
    resp.clusters.isEmpty should be(true)
    resp.offers should be(
      Seq(
        MapOfferResponse(
          id = offer.offerId.toString,
          latitude = offer.latitude(),
          longitude = offer.longitude(),
          price = offer.price,
          siteId = None,
          villageId = None,
          isFavorite = None
        )
      )
    )
    resp.multiHouses should be(
      Seq(
        MapMultiHouseResponse(
          id = house.multiHouseId.toString,
          latitude = house.latitude(),
          longitude = house.longitude(),
          leftTop = MapGeoPoint(house.maxLatitude, house.minLongitude),
          rightBottom = MapGeoPoint(house.minLatitude, house.maxLongitude),
          minPrice = house.price,
          siteId = None,
          site = None,
          villageId = None,
          village = None,
          offerCount = house.offerCount,
          favoriteOfferIds = Set()
        )
      )
    )
    resp.sites.isEmpty should be(true)
  }

  "MapSearchResponseRenderer" should "correctly render sites" in {
    val viewport = createViewport(
      topLat = 59.94621105841496f,
      bottomLat = 59.94071955429965f,
      leftLon = 30.342247030424687f,
      rightLon = 30.349283597361847f,
      width = 1440,
      height = 2240,
      dpi = 640,
      cellSize = 0.4f
    )

    val site = createSite(
      lat = 59.944946f,
      lon = 30.344767f,
      siteId = 777777L,
      price = 18500000L
    )
    val clusters = Array(site)
    val mapSearchResult = MapSearchResult(detailed = false, clusters = Array(clusters), 0)
    val resp = rendererSite.renderMapSearchResponse(viewport, mapSearchResult)
    resp.clusters.isEmpty should be(true)
    resp.sites should be(
      Seq(
        MapSiteResponse(
          siteId = site.siteId.toString,
          latitude = site.latitude(),
          longitude = site.longitude(),
          minPrice = Some(site.price)
        )
      )
    )
    resp.multiHouses.isEmpty should be(true)
    resp.offers.isEmpty should be(true)
  }

  private def createOffer(lat: Float, lon: Float, offerId: Long, price: Long): MapCluster = {
    val res = new MapCluster
    res.offerCount = 1
    res.offerLatitudeSum = lat
    res.offerLongitudeSum = lon
    res.offerId = offerId
    res.multiHouseId = offerId.hashCode()
    res.price = price
    res.minLatitude = lat
    res.maxLatitude = lat
    res.minLongitude = lon
    res.maxLongitude = lon
    res
  }

  private def createHouse(lat: Float, lon: Float, offerCount: Int, price: Long, multiHouseId: Long): MapCluster = {
    val res = new MapCluster
    res.offerCount = offerCount
    res.multiHouseId = multiHouseId
    res.offerLatitudeSum = lat * offerCount
    res.offerLongitudeSum = lon * offerCount
    res.price = price
    res.minLatitude = lat
    res.maxLatitude = lat
    res.minLongitude = lon
    res.maxLongitude = lon
    res
  }

  private def createCluster(
    lat: Float,
    lon: Float,
    offerCount: Int,
    price: Long,
    minLat: Float,
    minLon: Float,
    maxLat: Float,
    maxLon: Float
  ) = {
    val res = new MapCluster
    res.offerCount = offerCount
    res.offerLatitudeSum = lat * offerCount
    res.offerLongitudeSum = lon * offerCount
    res.price = price
    res.minLatitude = minLat
    res.maxLatitude = maxLat
    res.minLongitude = minLon
    res.maxLongitude = maxLon
    res
  }

  private def createSite(lat: Float, lon: Float, siteId: Long, price: Long) = {
    val res = new MapCluster
    res.offerCount = 1
    res.offerLatitudeSum = lat
    res.offerLongitudeSum = lon
    res.siteId = siteId
    res.price = price
    res.minLatitude = lat
    res.maxLatitude = lat
    res.minLongitude = lon
    res.maxLongitude = lon
    res
  }

  private def createViewport(
    bottomLat: Float,
    topLat: Float,
    leftLon: Float,
    rightLon: Float,
    height: Int,
    width: Int,
    dpi: Int,
    cellSize: Float
  ): ViewportInfo = {
    val viewportInfo = new ViewportInfo
    viewportInfo.setViewportBottomLatitude(bottomLat)
    viewportInfo.setViewportTopLatitude(topLat)
    viewportInfo.setViewportLeftLongitude(leftLon)
    viewportInfo.setViewportRightLongitude(rightLon)
    viewportInfo.setViewportHeight(height)
    viewportInfo.setViewportWidth(width)
    viewportInfo.setViewportDPI(dpi)
    viewportInfo.setCellSizeInches(cellSize)
    viewportInfo
  }
}
