package ru.auto.api.services.catalog

import org.scalacheck.Gen
import ru.auto.api.auth.Application
import ru.auto.api.features.FeatureManager
import ru.auto.api.http.HttpClientConfig
import ru.auto.api.model.{CategorySelector, RequestParams}
import ru.auto.api.services.HttpClientSuite
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.catalog.model.api.ApiModel.CatalogByTagRequest
import ru.yandex.vertis.mockito.MockitoSupport

class DefaultCatalogClientTest extends HttpClientSuite with MockitoSupport {

  override protected def config: HttpClientConfig =
    HttpClientConfig("autoru-catalog-api2-main.vrts-slb.test.vertis.yandex.net", 80)

  val featureManager: FeatureManager = mock[FeatureManager]

  private val client = new DefaultCatalogClient(http, featureManager)

  implicit val request: Request = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Some(Gen.identifier.next)))
    r.setTrace(trace)
    r.setApplication(Application.iosApp)
    r
  }
  test("simple request") {
    val res = client
      .getCardsWithoutOffers(
        CategorySelector.Cars,
        CatalogByTagRequest.newBuilder().addTags("SomeTagThatNotExists").build()
      )
      .futureValue
    res.getCatalogByTagCount shouldBe 0
  }

  test("error with category not car") {
    val res = client
      .getCardsWithoutOffers(
        CategorySelector.Trucks,
        CatalogByTagRequest.newBuilder().addTags("SomeTagThatNotExists").build()
      )
      .failed
      .futureValue
    res shouldBe a[IllegalArgumentException]
  }

}
