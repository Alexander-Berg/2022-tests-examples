package ru.yandex.realty2.extdataloader.loaders.special

import org.joda.time.DateTime
import play.api.libs.json.{JsArray, Json, Reads}
import ru.yandex.extdata.core.ServerController
import ru.yandex.realty.context.ProviderAdapter
import ru.yandex.realty.context.v2.FakeController
import ru.yandex.realty.context.v2.bunker.BunkerStorage
import ru.yandex.realty.graph.core.Node
import ru.yandex.realty.model.phone.PhoneRedirect
import ru.yandex.realty.phone.RedirectPhoneServiceTestComponents
import ru.yandex.realty.telepony.TeleponyClient.Domain.`billing_realty`
import ru.yandex.realty.AsyncSpecBase

import scala.concurrent.duration._
import scala.util.Try
import scala.collection.JavaConverters._

class PinnedSpecialProjectFetcherSpec extends AsyncSpecBase {
  private val RealBunkerDataWithoutRedirects = Json
    .parse(
      """[ {"params": {"geoId": [1, 10174], "startDate": "2020-12-01", "endDate": "3021-12-31"},
        |"data": {"developerId": 102320, "developerName": "Группа «Самолет»",
        |"pinnedSiteIds": [57547, 280521]}}, 
        |{"params": {"geoId": [51, 65], "startDate": "2020-12-01", "endDate": "3021-12-31"},
        |"data": {"developerId": 102322, "developerName": "Группа «Вертолет»",
        |"pinnedSiteIds": [57548, 280522]}}]""".stripMargin
    )
    .as[JsArray]

  private val RealBunkerDataWithDifferentOrder = Json
    .parse(
      """[ {"params": {"geoId": [1, 10174], "startDate": "2020-12-01", "endDate": "3021-12-31"},
        |"data": {"developerId": 102320, "developerName": "Группа «Самолет»",
        |"pinnedSiteIds": [280521, 57547]}}]""".stripMargin
    )
    .as[JsArray]

  private def realBunkerDataWithRedirects(geoId: Int, objectId: String, target: String, tag: String) =
    Json
      .parse(
        s"""[ 
        |{"params": {"geoId": [51, 65], "startDate": "2020-12-01", "endDate": "3021-12-31"},
        |"data": {"redirectPhones": [{"geoId": $geoId, "objectId": "$objectId", "target": "$target", "tag": "$tag"}], "developerId": 102322, "developerName": "Группа «Вертолет»",
        |"pinnedSiteIds": [57548, 280522]}}]""".stripMargin
      )
      .as[JsArray]

  "PinnedSpecialProjectFetcher" should {
    "build with empty bunker" in new PinnedSpecialProjectsFetcherFixture {
      (bunkerStorage
        .get[JsArray](_: String)(_: Reads[JsArray]))
        .expects("/realty-www/site_special_projects_second_package", *)
        .returns(Try(JsArray()))

      pinnedSpecialProjectsFetcher.build() shouldBe Seq.empty
    }

    "build with real bunker data and no redirects" in new PinnedSpecialProjectsFetcherFixture {
      (bunkerStorage
        .get[JsArray](_: String)(_: Reads[JsArray]))
        .expects("/realty-www/site_special_projects_second_package", *)
        .returns(Try(RealBunkerDataWithoutRedirects))

      val res = pinnedSpecialProjectsFetcher.build()
      res.size shouldBe 2
    }

    "build with real bunker data and redirects" in new PinnedSpecialProjectsFetcherFixture {
      val geoId = 1
      val objectId = "test_object_id"
      val target = "+79523991438"
      val tag = "some_tag"
      (bunkerStorage
        .get[JsArray](_: String)(_: Reads[JsArray]))
        .expects("/realty-www/site_special_projects_second_package", *)
        .returns(Try(realBunkerDataWithRedirects(geoId, objectId, target, tag)))

      val redirect = PhoneRedirect(
        billing_realty,
        "some_id",
        objectId,
        Some(tag),
        DateTime.now(),
        None,
        "8800",
        target,
        None,
        Some(geoId),
        Some(10.days)
      )

      expectTeleponyCall(redirect)
      val res = pinnedSpecialProjectsFetcher.build()
      res.size shouldBe 1
      val phones = res.head.getData.getPhoneList
      phones.size() shouldBe 1
      phones.get(0).getPhone.getSource shouldBe "8800"
    }

    "build with real bunker data with different order" in new PinnedSpecialProjectsFetcherFixture {
      (bunkerStorage
        .get[JsArray](_: String)(_: Reads[JsArray]))
        .expects("/realty-www/site_special_projects_second_package", *)
        .returns(Try(RealBunkerDataWithDifferentOrder))

      val res = pinnedSpecialProjectsFetcher.build()

      res.size shouldBe 1
      res.head.getData.getSiteIdList.asScala shouldBe Seq(280521L, 57547L)
    }
  }

  trait PinnedSpecialProjectsFetcherFixture extends RedirectPhoneServiceTestComponents with AsyncSpecBase {

    val controller: ServerController = new FakeController()
    val bunkerStorage: BunkerStorage = mock[BunkerStorage]

    val node = new Node()
    node.setId(1234341L)
    node.setGeoId(1)

    val pinnedSpecialProjectsFetcher =
      new PinnedSpecialProjectFetcher(controller, ProviderAdapter.create(bunkerStorage), redirectPhoneService)
  }
}
