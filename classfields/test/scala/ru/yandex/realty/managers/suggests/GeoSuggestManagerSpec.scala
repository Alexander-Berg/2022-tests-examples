package ru.yandex.realty.managers.suggests

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.api.ProtoResponse.GeoSuggestResponse
import ru.yandex.realty.suggest.geo.{GeoSuggestItem => ProtoSuggestItem}
import ru.yandex.realty.clients.suggest.{GeoSuggestClient, GeoSuggestItem, GeoSuggestTitle, SuggestGeoResponse}
import ru.yandex.realty.converters.GeoPointProtoConverter
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.model.location.GeoPoint
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class GeoSuggestManagerSpec extends AsyncSpecBase {

  implicit private val traced: Traced = Traced.empty
  val regionGraphProvider: Provider[RegionGraph] = mock[Provider[RegionGraph]]
  val geoSuggestClient: GeoSuggestClient = mock[GeoSuggestClient]

  trait GeoSuggestManagerFixture {
    val manager = new GeoSuggestManager(regionGraphProvider, geoSuggestClient)
  }

  "GeoSuggestManager" should {
    "return suggest for request" in new GeoSuggestManagerFixture {

      val clientResponse = SuggestGeoResponse(
        Seq(
          GeoSuggestItem(
            `type` = "toponym",
            title = GeoSuggestTitle("Санкт-Петербург"),
            subtitle = GeoSuggestTitle("Россия"),
            text = "Россия, Санкт-Петербург ",
            geoId = 2,
            tags = Seq("province"),
            action = Some("search"),
            uri = Some("ymapsbm1:\\/\\/geo?ll=30.238497%2C59.852081&spn=0.168634%2C0.015728&text=..."),
            distance = None,
            rectLower = None,
            rectUpper = None
          )
        )
      )

      (geoSuggestClient
        .suggestGeo(_: String, _: Seq[Int], _: Option[GeoPoint], _: Boolean)(_: Traced))
        .expects("санк", Seq.empty[Int], None, false, *)
        .returning(Future.successful(clientResponse))

      val result: GeoSuggestResponse = manager
        .suggest(
          GeoSuggestRequest(
            text = "санк",
            regions = Seq.empty[Int],
            point = None
          )
        )
        .futureValue

      result.getResponse.getItemsCount shouldBe 1
      val item: ProtoSuggestItem = result.getResponse.getItemsList.get(0)
      item.getTitle shouldBe "Санкт-Петербург"
      item.getSubtitle shouldBe "Россия"
      val expectedPoint = GeoPointProtoConverter.toProto(GeoPoint.fromDelimitedString("30.238497,59.852081", ","))
      item.getCoordinates shouldEqual expectedPoint
      item.getHighlightPositionCount shouldBe 0
    }
  }

}
