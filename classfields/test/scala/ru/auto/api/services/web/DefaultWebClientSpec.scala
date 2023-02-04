package ru.auto.api.services.web

import org.scalatest.OptionValues
import ru.auto.api.MotoModel.MotoCategory
import ru.auto.api.TrucksModel.TruckCategory
import ru.auto.api.features.FeatureManager
import ru.auto.api.model.CategorySelector.{Cars, Moto, Trucks}
import ru.auto.api.model.searcher
import ru.auto.api.search.SearchModel.{MotoSearchRequestParameters, SearchRequestParameters, TrucksSearchRequestParameters}
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.api.ui.UiModel.BodyTypeGroup
import ru.auto.api.util.search.SearchMappings
import ru.auto.api.util.search.mappers.DefaultsMapper
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters._

class DefaultWebClientSpec extends HttpClientSpec with MockedHttpClient with OptionValues with MockitoSupport {

  val featureManager: FeatureManager = mock[FeatureManager]
  val feature: Feature[Boolean] = mock[Feature[Boolean]]
  when(feature.value).thenReturn(false)

  val defaultsMapper = new DefaultsMapper(featureManager)
  val searchMappings: SearchMappings = new SearchMappings(defaultsMapper, featureManager)

  val client: DefaultWebClient = new DefaultWebClient(http)

  "Default Web Client" should {
    "parse search link" in {
      http.expectUrl(
        "/convert-listing-url-to-publicapi-params/?url=%2Fmoskva%2Fcars%2Faudi%2Fa4%2Fused%2F%3Fprice_from%3D1000000%26autoru_body_type%3DSEDAN"
      )
      http.respondWithJson(
        """
        {
          "category": "cars",
          "query": {
            "body_type_group": ["SEDAN"],
            "price_from": "1000"
          }
        }
        """.stripMargin
      )

      val resp =
        client.parseSearchLink("/moskva/cars/audi/a4/used/?price_from=1000000&autoru_body_type=SEDAN").futureValue

      resp.category shouldBe Cars
      resp.params.getCarsParams.getBodyTypeGroupList.asScala.toSet shouldEqual Set(BodyTypeGroup.SEDAN)
      resp.params.getPriceFrom shouldBe 1000
    }
  }

  "form saved search title for cars search" in {
    http.expectUrl("/subscriptions/title/?category=cars&http_query=mark_model_nameplate%3DMITSUBISHI%2523ECLIPSE")
    val title = "Автомобили Mitsubishi Eclipse"
    http.respondWithJson(s"""{ "title": "$title"}""")

    val params = SearchRequestParameters
      .newBuilder()
      .addMarkModelNameplate("MITSUBISHI#ECLIPSE")
      .build()
    val request = searcher.ApiSearchRequest(Cars, params)
    val resp = client.savedSearchTitle(request).futureValue
    resp shouldBe title
  }

  "form saved search title for trucks search" in {
    http.expectUrl(
      "/subscriptions/title/?category=trucks&http_query=trucks_category%3DTRUCK%26mark_model_nameplate%3DMITSUBISHI%2523ECLIPSE"
    )
    val title = "Автомобили Mitsubishi Eclipse"
    http.respondWithJson(s"""{ "title": "$title"}""")

    val params = SearchRequestParameters
      .newBuilder()
      .addMarkModelNameplate("MITSUBISHI#ECLIPSE")
      .setTrucksParams(TrucksSearchRequestParameters.newBuilder().setTrucksCategory(TruckCategory.TRUCK))
      .build()
    val request = searcher.ApiSearchRequest(Trucks, params)
    val resp = client.savedSearchTitle(request).futureValue
    resp shouldBe title
  }

  "form saved search title for moto search" in {
    http.expectUrl(
      "/subscriptions/title/?category=moto&http_query=moto_category%3Dmotorcycle%26mark_model_nameplate%3DMITSUBISHI%2523ECLIPSE"
    )
    val title = "Автомобили Mitsubishi Eclipse"
    http.respondWithJson(s"""{ "title": "$title"}""")

    val params = SearchRequestParameters
      .newBuilder()
      .addMarkModelNameplate("MITSUBISHI#ECLIPSE")
      .setMotoParams(MotoSearchRequestParameters.newBuilder().setMotoCategory(MotoCategory.MOTORCYCLE))
      .build()
    val request = searcher.ApiSearchRequest(Moto, params)
    val resp = client.savedSearchTitle(request).futureValue
    resp shouldBe title
  }
}
