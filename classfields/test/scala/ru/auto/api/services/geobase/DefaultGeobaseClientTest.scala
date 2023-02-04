package ru.auto.api.services.geobase

import org.scalacheck.Gen
import ru.auto.api.BaseSpec
import ru.auto.api.auth.Application
import ru.auto.api.model.RequestParams
import ru.auto.api.services.CachedMockedHttpClient
import ru.auto.api.util.{Request, RequestImpl}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 13.12.17
  */
class DefaultGeobaseClientTest extends BaseSpec with MockitoSupport with CachedMockedHttpClient {

  private val geobaseClient = new DefaultGeobaseClient(cachedhttp)

  implicit val trace: Traced = Traced.empty

  implicit val request: Request = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Some(Gen.identifier.next)))
    r.setTrace(trace)
    r.setApplication(Application.iosApp)
    r
  }

  "GeobaseClient" should {
    "resolve region by ip" in {
      cachedhttp.expectUrl("/v1/region_by_ip?ip=92.36.94.80")
      cachedhttp.respondWithJsonFrom("/geobase/region_by_ip.json")

      val region = geobaseClient.regionIdByIp("92.36.94.80").futureValue
      region shouldBe 213
    }

    "resolve region by coordinates" in {
      cachedhttp.expectUrl("/v1/region_id_by_location?lat=55.733684&lon=37.588496")
      cachedhttp.respondWith("120542")

      val region = geobaseClient.regionIdByLocation(latitude = 55.733684, longitude = 37.588496).futureValue
      region shouldBe 120542
    }
  }
}
