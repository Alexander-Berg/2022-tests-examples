package ru.yandex.vertis.parsing.clients.geocoder

import java.net.URLEncoder

import org.apache.http.message.BasicHeader
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.clients.MockedHttpClientSupport
import ru.yandex.vertis.parsing.clients.tvm.TvmClientWrapper
import ru.yandex.vertis.parsing.components.TestCatalogsComponents
import yandex.maps.proto.common2.response.ResponseOuterClass.Response

@RunWith(classOf[JUnitRunner])
class GeocoderTvmTest extends FunSuite with MockedHttpClientSupport with MockitoSupport {

  private val geocoder = new GeocoderTvm(
    "autoru",
    http,
    TestCatalogsComponents.regionTree,
    "/search/stable/yandsearch"
  )

  test("getRegion: Russia prefix") {
    val text = URLEncoder.encode("Россия Армавир", "UTF-8")
    http.expect(
      "GET",
      "/search/stable/yandsearch?origin=autoru&ms=pb&lang=ru_RU&text=" + text
    )
    val response = Response.newBuilder()
    /*response.getReplyBuilder
      .addGeoObjectBuilder()
      .addMetadataBuilder().addExtension(Geocoder.gEOOBJECTMETADATA)*/
    http.respondWith[Response](response.build())
    val result = geocoder.getRegion("Армавир").futureValue
    assert(result.isEmpty)
  }
}
