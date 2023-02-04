package ru.auto.api.services.web

import org.scalatest.{Ignore, OptionValues}
import ru.auto.api.features.FeatureManager
import ru.auto.api.http.HttpClientConfig
import ru.auto.api.services.HttpClientSuite
import ru.auto.api.ui.UiModel.BodyTypeGroup
import ru.auto.api.util.search.SearchMappings
import ru.auto.api.util.search.mappers.DefaultsMapper
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters._

@Ignore
class DefaultWebClientIntTest extends HttpClientSuite with OptionValues with MockitoSupport {

  override protected def config: HttpClientConfig = {
    HttpClientConfig("https", "test.avto.ru", 443)
  }

  val featureManager: FeatureManager = mock[FeatureManager]
  val feature: Feature[Boolean] = mock[Feature[Boolean]]
  when(feature.value).thenReturn(false)
  val defaultsMapper = new DefaultsMapper(featureManager)
  val searchMappings: SearchMappings = new SearchMappings(defaultsMapper, featureManager)

  val client = new DefaultWebClient(http)

  test("parse search link") {
    val link = "/moskva/cars/audi/a4/used/?price_from=1000000&body_type_group=SEDAN"

    val resp = client.parseSearchLink(link).futureValue.params

    resp.getMarkModelNameplateList.asScala.toSet shouldEqual Set("AUDI#A4")
    resp.getPriceFrom shouldBe 1000000
    resp.getRidList.asScala.head shouldBe 213
    resp.getCarsParams.getBodyTypeGroupList.asScala.toSet shouldBe Set(BodyTypeGroup.SEDAN)
  }
}
