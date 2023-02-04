package ru.vertistraf.cost_plus.builder.service

import ru.vertistraf.cost_plus.builder.model.thumb.ThumbUrlPath
import ru.vertistraf.cost_plus.model.config.Domains
import ru.vertistraf.cost_plus.builder.service.live.LiveUrlBuilder
import zio.test._

object UrlBuilderSpec extends DefaultRunnableSpec {

  private val pinnedOfferParameterName = "pinned_offer_id"

  private val domains = Domains(
    "desktop.host",
    "mobile.host",
    "https"
  )

  private def newBuilder(shouldBuildMobileSets: Boolean): UrlBuilder.Service =
    LiveUrlBuilder(domains, shouldBuildMobileSets, pinnedOfferParameterName)

  private def thumbSpec(setPath: String, thumbUrlPath: ThumbUrlPath, expected: String) =
    newBuilder(false)
      .buildThumbUrl(setPath, thumbUrlPath)
      .map { res =>
        assertTrue(res == expected)
      }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("UrlBuilder")(
      testM("should correctly build set urls") {
        checkAllM(Gen.fromIterable(Iterable(true, false))) { shouldBuildMobileSets =>
          val b = newBuilder(shouldBuildMobileSets)
          val input = "/some/url/"

          val expected =
            Seq("https://desktop.host/some/url/") ++
              Option.when(shouldBuildMobileSets)("https://mobile.host/some/url/")

          b.buildSetUrls(input).map { res =>
            assertTrue(res == expected)
          }
        }
      },
      suite("should correctly build thumb url")(
        testM("when comes default") {
          thumbSpec("/some/url/", ThumbUrlPath.Default, "https://desktop.host/some/url/")
        },
        testM("when comes url with all parameters") {
          thumbSpec(
            "/some/url/",
            ThumbUrlPath.Default.copy(
              pinnedOfferId = Some("1"),
              from = Set("1", "2"),
              utmMedium = Set("med"),
              utmCampaign = Set("camp"),
              utmSource = Set("source")
            ),
            "https://desktop.host/some/url/?pinned_offer_id=1&from=1.2&utm_campaign=camp&utm_medium=med&utm_source=source"
          )
        },
        testM("when comes url with another path") {
          thumbSpec(
            "/some/url/",
            ThumbUrlPath.Default.copy(
              overrideSetPath = Some("/another/url/")
            ),
            "https://desktop.host/another/url/"
          )
        },
        testM("when comes url with all parameters and another path") {
          thumbSpec(
            "/some/url/",
            ThumbUrlPath(
              overrideSetPath = Some("/another/url/"),
              pinnedOfferId = Some("1"),
              catalogFilter = Map("f1" -> "1", "f2" -> "2"),
              from = Set("1", "2"),
              utmMedium = Set("med"),
              utmCampaign = Set("camp"),
              utmSource = Set("source"),
              utmTerm = Some("term")
            ),
            "https://desktop.host/another/url/?pinned_offer_id=1&catalog_filter=f1%3D1%2Cf2%3D2&from=1.2&utm_campaign=camp&utm_medium=med&utm_source=source&utm_term=term"
          )
        }
      )
    )
}
