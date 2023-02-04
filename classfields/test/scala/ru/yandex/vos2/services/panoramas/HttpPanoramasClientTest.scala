package ru.yandex.vos2.services.panoramas

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.util.http.MockHttpClientHelper

@RunWith(classOf[JUnitRunner])
class HttpPanoramasClientTest extends AnyFunSuite with MockHttpClientHelper {
  implicit val trace: Traced = Traced.empty
  val host = "panoramas-api.vrts-slb.test.vertis.yandex.net"
  val port = 80

  val client = new HttpPanoramasClient(host, port)

  ignore("get panorama") {
    val id = "510797948-1571661666202-q4hLb2"
    val res = client.get(id)
    println(res)
  }

  ignore("bypass exterior panorama") {
    val id = "510797948-1571661666202-q4hLb2"
    val res = client.bypassExteriorPanorama(Set(id))
    println(res)
  }

  ignore("bypass interior panorama") {
    val id = "510797948-1571661666202-q4hLb2"
    val res = client.bypassInteriorPanorama(Set(id))
    println(res)
  }

}
