package ru.auto.api.managers.geo

import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.auth.Application
import ru.auto.api.geo.{GeoSuggestList, Tree}
import ru.auto.api.model.{ModelGenerators, RequestParams}
import ru.auto.api.services.geobase.GeobaseClient
import ru.auto.api.services.searcher.SearcherClient
import ru.auto.api.testkit.TestData
import ru.auto.api.util.{Request, RequestImpl}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters._

class GeoManagerSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks with BeforeAndAfter {
  val geoBaseClient: GeobaseClient = mock[GeobaseClient]
  val searcherClient: SearcherClient = mock[SearcherClient]
  val tree: Tree = TestData.tree
  val geoSuggestList: GeoSuggestList = TestData.geoSuggestListing

  val geoManager: GeoManager = new GeoManager(geoBaseClient, searcherClient, tree, geoSuggestList)

  implicit val trace: Traced = Traced.empty

  implicit val request: Request = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Some(Gen.identifier.next)))
    r.setTrace(trace)
    r.setApplication(Application.iosApp)
    r
  }

  before {
    reset(geoBaseClient, searcherClient)
  }

  "GeoManager" should {
    "suggest by letters" in {
      val params = GeoSuggestParams(letters = Some("letters"), coordinates = None)
      val regions = tree.children(tree.root.get).map(_.id)
      when(searcherClient.regionPrompt(?, ?, ?, ?)(?)).thenReturnF(regions.toSeq)
      val res = geoManager.suggest(params).futureValue

      verify(searcherClient).regionPrompt(
        Some("letters"),
        notOnlyWithOffers = true,
        onlyCities = false,
        withVillages = true
      )(trace)

      res.getRegionsList.asScala.map(_.getId).toSet shouldEqual regions
    }

    "suggest by coordinates" in {
      val coordinates = ModelGenerators.GeoPointGen.next
      val params = GeoSuggestParams(letters = None, coordinates = Some(coordinates))
      val region = 213 // Москва
      when(geoBaseClient.regionIdByLocation(?, ?)(?)).thenReturnF(region)
      val res = geoManager.suggest(params).futureValue

      verify(geoBaseClient).regionIdByLocation(coordinates.getLatitude, coordinates.getLongitude)

      res.getRegionsList.asScala.map(_.getId).toSet shouldEqual Set(213, 1) // Москва и московска область
    }

    "suggest by coordinates with region filter" in {
      val coordinates = ModelGenerators.GeoPointGen.next
      val params = GeoSuggestParams(letters = None, coordinates = Some(coordinates))
      val region = 98599 // Одинцовский район
      when(geoBaseClient.regionIdByLocation(?, ?)(?)).thenReturnF(region)
      val res = geoManager.suggest(params).futureValue

      verify(geoBaseClient).regionIdByLocation(coordinates.getLatitude, coordinates.getLongitude)

      res.getRegionsList.asScala.map(_.getId).toSet shouldEqual Set(1) // Москва и московская область
    }

    "suggest by coordinates with cities filter" in {
      val coordinates = ModelGenerators.GeoPointGen.next
      val params = GeoSuggestParams(letters = None, coordinates = Some(coordinates), onlyCities = true)
      val region = 213 // Москва
      when(geoBaseClient.regionIdByLocation(?, ?)(?)).thenReturnF(region)
      val res = geoManager.suggest(params).futureValue

      verify(geoBaseClient).regionIdByLocation(coordinates.getLatitude, coordinates.getLongitude)

      res.getRegionsList.asScala.map(_.getId).toSet shouldEqual Set(213) // Москва
    }

    "suggest by ip" in {
      val params = GeoSuggestParams(letters = None, coordinates = None)
      val region = 213
      when(geoBaseClient.regionIdByIp(?)(?)).thenReturnF(region)
      val res = geoManager.suggest(params).futureValue

      verify(geoBaseClient).regionIdByIp(request.requestParams.ip)

      res.getRegionsList.asScala.map(_.getId).toSet shouldEqual Set(213, 1)
    }

    "suggest by popular when ip is malformed" in {
      val params = GeoSuggestParams(letters = None, coordinates = None)
      val regions = tree.children(tree.root.get).map(_.id)
      when(geoBaseClient.regionIdByIp(?)(?)).thenThrowF(new IllegalArgumentException)
      when(searcherClient.regionPrompt(?, ?, ?, ?)(?)).thenReturnF(regions.toSeq)
      val res = geoManager.suggest(params).futureValue

      verify(geoBaseClient).regionIdByIp(request.requestParams.ip)
      verify(searcherClient).regionPrompt(None, notOnlyWithOffers = false, onlyCities = false, withVillages = false)(
        trace
      )

      res.getRegionsList.asScala.map(_.getId).toSet shouldEqual regions
    }

    "suggest by popular when region by ip is not city" in {
      val params = GeoSuggestParams(letters = None, coordinates = None, onlyCities = true)
      val regions = tree.children(tree.root.get).map(_.id)
      when(geoBaseClient.regionIdByIp(?)(?)).thenReturnF(regions.head)
      when(searcherClient.regionPrompt(?, ?, ?, ?)(?)).thenReturnF(regions.toSeq)
      val res = geoManager.suggest(params).futureValue

      verify(geoBaseClient).regionIdByIp(request.requestParams.ip)
      verify(searcherClient).regionPrompt(None, notOnlyWithOffers = false, onlyCities = true, withVillages = false)(
        trace
      )

      res.getRegionsList.asScala.map(_.getId).toSet shouldEqual regions
    }
  }
}
