package ru.yandex.realty.extdata

import play.api.libs.json.{JsArray, JsObject, Json, Reads}
import ru.yandex.extdata.core.Controller
import ru.yandex.realty.{CommonConstants, SpecBase}
import ru.yandex.realty.context.v2.FakeController
import ru.yandex.realty.context.v2.bunker.BunkerStorage
import ru.yandex.realty.storage.SiteSpecialProjectsBunkerStorage

import scala.util.Try

class SiteSpecialProjectsBunkerProviderSpec extends SpecBase {

  private val RealBunkerData = Json
    .parse(
      """[ { "params" : { "geoId": [ 1, 10174, 10398 ], "startDate": "2019-12-01", "endDate": "3021-12-31" },
      "data" : { "developerId": 52308, "developerName": "ПИК", "showTab": true, "hasLogoInTab": true, "tabUrl": "/pik/?from=main_menu", "showPin": true, "hideAds": true, "showFilter": true, "filterText": "Только ЖК от ПИК", "developerFullName": "ПИК", "sideMenuText": "Квартиры от ПИК" } },
      { "params" : { "geoId": 11176, "startDate": "2020-11-09", "endDate": "3021-02-10" },
      "data" : { "developerId": 650032, "developerName": "ПСК ДОМ", "showTab": true, "tabUrl": "/tyumenskaya_oblast/zastroyschik/psk-dom-development-650032/", "showPin": false, "hideAds": true, "showFilter": true, "filterText": "Только ЖК от ПСК ДОМ", "developerFullName": "ПСК ДОМ", "sideMenuText": "Квартиры от ПСК ДОМ" } } ]"""
    )
    .as[JsArray]
  private val BunkerDataWithNoPik = Json
    .parse(
      """[ { "params" : { "geoId": [ 1, 10174 ], "startDate": "2019-12-01", "endDate": "3021-12-31" },
      "data" : { "developerId": 48308, "developerName": "ПК", "showTab": true, "hasLogoInTab": true, "tabUrl": "/pk/?from=main_menu", "showPin": true, "hideAds": true, "showFilter": true, "filterText": "Только ЖК от ПК", "developerFullName": "ПК", "sideMenuText": "Квартиры от ПК" } },
      { "params" : { "geoId": 11176, "startDate": "2020-11-09", "endDate": "3021-02-10" },
      "data" : { "developerId": 650032, "developerName": "ПСК ДОМ", "showTab": true, "tabUrl": "/tyumenskaya_oblast/zastroyschik/psk-dom-development-650032/", "showPin": false, "hideAds": true, "showFilter": true, "filterText": "Только ЖК от ПСК ДОМ", "developerFullName": "ПСК ДОМ", "sideMenuText": "Квартиры от ПСК ДОМ" } } ]"""
    )
    .as[JsArray]
  private val BunkerWithDatesBefore = Json
    .parse(
      """[ { "params" : { "geoId": [ 1, 10174 ], "startDate": "2019-12-01", "endDate": "2021-08-01" },
      "data" : { "developerId": 52308, "developerName": "ПИК", "showTab": true, "hasLogoInTab": true, "tabUrl": "/pik/?from=main_menu", "showPin": true, "hideAds": true, "showFilter": true, "filterText": "Только ЖК от ПИК", "developerFullName": "ПИК", "sideMenuText": "Квартиры от ПИК" } } ]"""
    )
    .as[JsArray]
  private val BunkerWithDatesAfter = Json
    .parse(
      """[ { "params" :{ "geoId": [ 1, 10174 ], "startDate": "3019-12-01", "endDate": "3021-08-01" },
      "data" : { "developerId": 52308, "developerName": "ПИК", "showTab": true, "hasLogoInTab": true, "tabUrl": "/pik/?from=main_menu", "showPin": true, "hideAds": true, "showFilter": true, "filterText": "Только ЖК от ПИК", "developerFullName": "ПИК", "sideMenuText": "Квартиры от ПИК" } } ]"""
    )
    .as[JsArray]

  "SiteSpecialProjectsBunkerProviderSpec" should {
    "build with empty bunker" in new SiteSpecialProjectsBunkerProviderFixture {
      val empty: JsArray = JsArray()
      (bunkerStorage
        .get[JsArray](_: String)(_: Reads[JsArray]))
        .expects("/realty-www/site-special-projects", *)
        .returns(Try(empty))

      val storage: SiteSpecialProjectsBunkerStorage = siteSpecialProjectsBunkerProvider.build().get

      storage.siteSpecialProjects shouldBe Seq.empty
    }

    "build from real bunker data" in new SiteSpecialProjectsBunkerProviderFixture {
      (bunkerStorage
        .get[JsArray](_: String)(_: Reads[JsArray]))
        .expects("/realty-www/site-special-projects", *)
        .returns(Try(RealBunkerData))

      val storage: SiteSpecialProjectsBunkerStorage = siteSpecialProjectsBunkerProvider.build().get

      storage.siteSpecialProjects.size shouldBe 2
    }

    "build from bunker data without pik" in new SiteSpecialProjectsBunkerProviderFixture {
      (bunkerStorage
        .get[JsArray](_: String)(_: Reads[JsArray]))
        .expects("/realty-www/site-special-projects", *)
        .returns(Try(BunkerDataWithNoPik))

      val storage: SiteSpecialProjectsBunkerStorage = siteSpecialProjectsBunkerProvider.build().get
      val pikProjects: Option[SiteSpecialProject] = storage.siteSpecialProjects.find(
        project => project.data.developerId == CommonConstants.PIK_DEVELOPER_ID
      )
      pikProjects shouldBe None
    }

    "build from bunker data with dates before" in new SiteSpecialProjectsBunkerProviderFixture {
      (bunkerStorage
        .get[JsArray](_: String)(_: Reads[JsArray]))
        .expects("/realty-www/site-special-projects", *)
        .returns(Try(BunkerWithDatesBefore))

      val storage: SiteSpecialProjectsBunkerStorage = siteSpecialProjectsBunkerProvider.build().get

      storage.siteSpecialProjects shouldBe empty
    }

    "build from bunker data with dates after" in new SiteSpecialProjectsBunkerProviderFixture {
      (bunkerStorage
        .get[JsArray](_: String)(_: Reads[JsArray]))
        .expects("/realty-www/site-special-projects", *)
        .returns(Try(BunkerWithDatesAfter))

      val storage: SiteSpecialProjectsBunkerStorage = siteSpecialProjectsBunkerProvider.build().get

      storage.siteSpecialProjects shouldBe empty
    }
  }

  trait SiteSpecialProjectsBunkerProviderFixture {
    val controller: Controller = new FakeController()
    val bunkerStorage: BunkerStorage = mock[BunkerStorage]

    val siteSpecialProjectsBunkerProvider = new SiteSpecialProjectsBunkerProvider(
      controller,
      () => bunkerStorage
    )
  }
}
