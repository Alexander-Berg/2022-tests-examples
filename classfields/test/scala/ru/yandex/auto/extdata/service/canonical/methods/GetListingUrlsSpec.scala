// scalafmt: { align.tokens.add = [ {code = "="} ] }
package ru.yandex.auto.extdata.service.canonical.methods

import cats.syntax.either._
import io.circe.Decoder
import io.circe.parser.decode
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.auto.traffic.model._
import ru.yandex.auto.traffic.model.af_url.ListingUrl

class GetListingUrlsSpec extends FlatSpec with Matchers {

  it should "decode [[GetListingUrls]]'s result from a valid JSON (urls are empty)" in {
    val rawJson =
      """
      |{
      |    "urls": []
      |}
      |""".stripMargin

    implicit val decoder: Decoder[List[ListingUrl]] = GetListingUrls.method.decoder
    decode[List[ListingUrl]](rawJson) shouldBe (List.empty.asRight)
  }

  it should "decode [[GetListingUrls]]'s result from a valid JSON: /cars/all/" in {
    val rawJson =
      """
        |{
        |    "urls": [
        |        {
        |            "url": "/cars/all/",
        |            "params": {
        |                "section": "all",
        |                "category": "cars"
        |            },
        |            "route": "index"
        |        }
        |    ]
        |}
        |""".stripMargin

    val expected = List(
      ListingUrl(
        url = CanonicalUrl("/cars/all/"),
        params = ListingUrl.Params(
          route     = Some(ListingUrl.Route.Index),
          geoId     = None,
          section   = Some(Section.All),
          markCode  = None,
          modelCode = None,
          filters   = Set.empty
        )
      )
    )

    implicit val decoder: Decoder[List[ListingUrl]] = GetListingUrls.method.decoder
    decode[List[ListingUrl]](rawJson) shouldBe (expected.asRight)
  }

  it should "decode [[GetListingUrls]]'s result from a valid JSON: /himki/cars/new/" in {
    val rawJson =
      """
        |{
        |    "urls": [
        |        {
        |            "url": "/himki/cars/new/",
        |            "params": {
        |                "section": "new",
        |                "geo_id": [
        |                    10758
        |                ],
        |                "category": "cars",
        |                "on_credit": true
        |            },
        |            "route": "listing"
        |        }
        |    ]
        |}
        |""".stripMargin

    val expected = List(
      ListingUrl(
        url = CanonicalUrl("/himki/cars/new/"),
        params = ListingUrl.Params(
          route     = Some(ListingUrl.Route.Listing),
          geoId     = Some(GeoId(10758)),
          section   = Some(Section.New),
          markCode  = None,
          modelCode = None,
          filters   = Set(Filter.OnCredit)
        )
      )
    )

    implicit val decoder: Decoder[List[ListingUrl]] = GetListingUrls.method.decoder
    decode[List[ListingUrl]](rawJson) shouldBe (expected.asRight)
  }

  it should "decode [[GetListingUrls]]'s result from a valid JSON: /cars/bmw/all/drive-4x4_wheel/" in {
    val rawJson =
      """
      |{
      |    "urls": [
      |        {
      |            "url": "/cars/bmw/all/drive-4x4_wheel/",
      |            "params": {
      |                "section": "all",
      |                "category": "cars",
      |                "mark": "BMW",
      |                "gear_type": "ALL_WHEEL_DRIVE"
      |            },
      |            "route": "listing"
      |        }
      |    ]
      |}
      |""".stripMargin

    val expected = List(
      ListingUrl(
        url = CanonicalUrl("/cars/bmw/all/drive-4x4_wheel/"),
        params = ListingUrl.Params(
          route     = Some(ListingUrl.Route.Listing),
          geoId     = None,
          section   = Some(Section.All),
          markCode  = Some(Mark.Code("BMW")),
          modelCode = None,
          filters   = Set(Filter.GearType)
        )
      )
    )

    implicit val decoder: Decoder[List[ListingUrl]] = GetListingUrls.method.decoder
    decode[List[ListingUrl]](rawJson) shouldBe (expected.asRight)
  }

  it should "decode [[GetListingUrls]]'s result from a valid JSON: /moskva/cars/mercedes/gls_klasse-350/2016-year/20712531/20712577/20748214/used/transmission-automatic/" in {
    val rawJson =
      """
        |{
        |    "urls": [
        |        {
        |            "url": "/moskva/cars/mercedes/gls_klasse-350/2016-year/20712531/20712577/20748214/used/transmission-automatic/",
        |            "params": {
        |                "section": "used",
        |                "geo_id": [
        |                    213
        |                ],
        |                "category": "cars",
        |                "mark": "MERCEDES",
        |                "model": "GLS_KLASSE",
        |                "nameplate_name": "350",
        |                "super_gen": 20712531,
        |                "transmission": [
        |                    "AUTO",
        |                    "AUTOMATIC",
        |                    "ROBOT",
        |                    "VARIATOR"
        |                ],
        |                "tech_param_id": 20748214,
        |                "configuration_id": 20712577,
        |                "year_from": 2016,
        |                "year_to": 2016
        |            },
        |            "route": "listing"
        |        }
        |    ]
        |}
        |""".stripMargin

    val expected = List(
      ListingUrl(
        url = CanonicalUrl(
          "/moskva/cars/mercedes/gls_klasse-350/2016-year/20712531/20712577/20748214/used/transmission-automatic/"
        ),
        params = ListingUrl.Params(
          route     = Some(ListingUrl.Route.Listing),
          geoId     = Some(GeoId(213)),
          section   = Some(Section.Used),
          markCode  = Some(Mark.Code("MERCEDES")),
          modelCode = Some(Model.Code("GLS_KLASSE")),
          filters = Set(
            Filter.NameplateName,
            Filter.SuperGen,
            Filter.Transmission,
            Filter.TechParamId,
            Filter.ConfigurationId,
            Filter.YearFrom,
            Filter.YearTo
          )
        )
      )
    )

    implicit val decoder: Decoder[List[ListingUrl]] = GetListingUrls.method.decoder
    decode[List[ListingUrl]](rawJson) shouldBe (expected.asRight)
  }

  it should "decode [[GetListingUrls]]'s result from a valid JSON: /moskva/cars/mercedes/gls_klasse-350/2016-year/20712531/20712577/20748214/used/color-chernyj/" in {
    val rawJson =
      """
        |{
        |    "urls": [
        |        {
        |            "url": "/moskva/cars/mercedes/gls_klasse-350/2016-year/20712531/20712577/20748214/used/color-chernyj/",
        |            "params": {
        |                "section": "used",
        |                "geo_id": [
        |                    213
        |                ],
        |                "category": "cars",
        |                "mark": "MERCEDES",
        |                "model": "GLS_KLASSE",
        |                "nameplate_name": "350",
        |                "super_gen": 20712531,
        |                "tech_param_id": 20748214,
        |                "configuration_id": 20712577,
        |                "color": "040001",
        |                "year_from": 2016,
        |                "year_to": 2016
        |            },
        |            "route": "listing"
        |        }
        |    ]
        |}
        |""".stripMargin

    val expected = List(
      ListingUrl(
        url =
          CanonicalUrl("/moskva/cars/mercedes/gls_klasse-350/2016-year/20712531/20712577/20748214/used/color-chernyj/"),
        params = ListingUrl.Params(
          route     = Some(ListingUrl.Route.Listing),
          geoId     = Some(GeoId(213)),
          section   = Some(Section.Used),
          markCode  = Some(Mark.Code("MERCEDES")),
          modelCode = Some(Model.Code("GLS_KLASSE")),
          filters = Set(
            Filter.NameplateName,
            Filter.SuperGen,
            Filter.TechParamId,
            Filter.ConfigurationId,
            Filter.Color,
            Filter.YearFrom,
            Filter.YearTo
          )
        )
      )
    )

    implicit val decoder: Decoder[List[ListingUrl]] = GetListingUrls.method.decoder
    decode[List[ListingUrl]](rawJson) shouldBe (expected.asRight)
  }

  it should "decode [[GetListingUrls]]'s result from a valid JSON: /moskva/cars/vendor-foreign/2016-year/used/engine-dizel/" in {
    val rawJson =
      """
        |{
        |    "urls": [
        |        {
        |            "url": "/moskva/cars/vendor-foreign/2016-year/used/engine-dizel/",
        |            "params": {
        |                "section": "used",
        |                "geo_id": [
        |                    213
        |                ],
        |                "category": "cars",
        |                "engine_group": "DIESEL",
        |                "year_from": 2016,
        |                "year_to": 2016,
        |                "catalog_filter": [
        |                    {
        |                        "vendor": "VENDOR2"
        |                    }
        |                ]
        |            },
        |            "route": "listing"
        |        }
        |    ]
        |}
        |""".stripMargin

    val expected = List(
      ListingUrl(
        url = CanonicalUrl("/moskva/cars/vendor-foreign/2016-year/used/engine-dizel/"),
        params = ListingUrl.Params(
          route     = Some(ListingUrl.Route.Listing),
          geoId     = Some(GeoId(213)),
          section   = Some(Section.Used),
          markCode  = None,
          modelCode = None,
          filters = Set(
            Filter.EngineGroup,
            Filter.YearFrom,
            Filter.YearTo,
            Filter.CatalogFilter
          )
        )
      )
    )

    implicit val decoder: Decoder[List[ListingUrl]] = GetListingUrls.method.decoder
    decode[List[ListingUrl]](rawJson) shouldBe (expected.asRight)
  }

  it should "correctly decode dealer net url" in {
    val rawJson =
      """
        |{
        |    "urls": [
        |        {
        |            "url": "/dealer-net/rolf/",
        |            "params": {
        |                "dealer_net_semantic_url": "rolf"
        |            }
        |        }
        |    ]
        |}
        |""".stripMargin

    val expected = List(
      ListingUrl(
        url = CanonicalUrl("/dealer-net/rolf/"),
        params = ListingUrl.Params(
          route     = None,
          geoId     = None,
          section   = None,
          markCode  = None,
          modelCode = None,
          filters = Set(
            Filter.DealerNetSemanticUrl
          )
        )
      )
    )

    implicit val decoder: Decoder[List[ListingUrl]] = GetListingUrls.method.decoder
    decode[List[ListingUrl]](rawJson) shouldBe (expected.asRight)
  }

  it should "decode [[GetListingUrls]]'s result from a valid JSON: /cars/new/group/porsche/panamera/22481288-22481337/" in {
    val rawJson =
      """
        |{
        |    "urls": [
        |        {
        |            "url": "/cars/new/group/porsche/panamera/22481288-22481337/",
        |            "params": {
        |                "category": "cars",
        |                "section": "new",
        |                "mark": "PORSCHE",
        |                "model": "PANAMERA",
        |                "super_gen": 22481288,
        |                "configuration_id": 22481337
        |            },
        |            "route": "card-group"
        |        }
        |    ]
        |}
        |""".stripMargin

    val expected = List(
      ListingUrl(
        url = CanonicalUrl("/cars/new/group/porsche/panamera/22481288-22481337/"),
        params = ListingUrl.Params(
          route     = Some(ListingUrl.Route.CardGroup),
          geoId     = None,
          section   = Some(Section.New),
          markCode  = Some(Mark.Code("PORSCHE")),
          modelCode = Some(Model.Code("PANAMERA")),
          filters = Set(
            Filter.SuperGen,
            Filter.ConfigurationId
          )
        )
      )
    )

    implicit val decoder: Decoder[List[ListingUrl]] = GetListingUrls.method.decoder
    decode[List[ListingUrl]](rawJson) shouldBe (expected.asRight)
  }

}
