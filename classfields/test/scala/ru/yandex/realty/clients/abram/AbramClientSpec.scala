package ru.yandex.realty.clients.abram

import akka.http.scaladsl.model.HttpMethods.POST
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.abram.proto.api.call.prices._
import ru.yandex.realty.{AsyncSpecBase, SpecBase}
import ru.yandex.realty.http.{HttpClientMock, RequestAware}
import ru.yandex.vertis.generators.ProducerProvider
import scala.collection.JavaConverters._

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class AbramClientSpec
  extends AsyncSpecBase
  with PropertyChecks
  with RequestAware
  with HttpClientMock
  with ProducerProvider {

  val client = new AbramClientImpl(httpService)

  "AbramClient" should {
    "get price successfully" in {
      val req = CallPriceRequest
        .newBuilder()
        .setTeleponyTag("tuzParamRgid=741964#tuzParamType=SELL#tuzParamCategory=APARTMENT")
        .build()
      val response = CallPriceResponse
        .newBuilder()
        .setPrice(CallPrice.newBuilder().setBasePrice(30000))
        .build()
      httpClient.expect(POST, "/api/1.x/call/price")
      httpClient.expectProto(req)
      httpClient.respondWith(response)
      client.getCallPrice(req).futureValue should be(response)
    }

    "get prices batch successfully" in {
      val req1 = CallPriceRequest
        .newBuilder()
        .setTeleponyTag("tuzParamRgid=741964#tuzParamType=SELL#tuzParamCategory=APARTMENT")
        .build()

      val req2 = CallPriceRequest
        .newBuilder()
        .setTeleponyTag("tuzParamRgid=741965#tuzParamType=SELL#tuzParamCategory=APARTMENT")
        .build()

      val req = CallPricesRequest
        .newBuilder()
        .addAllPriceRequests(Seq(req1, req2).asJava)
        .build()
      val resp1 = CallPrice.newBuilder().setBasePrice(30000).build
      val resp2 = CallPrice.newBuilder().setBasePrice(20000).build
      val response = CallPricesResponse
        .newBuilder()
        .addAllPrices(Seq(resp1, resp2).asJava)
        .build()
      httpClient.expect(POST, "/api/1.x/call/prices")
      httpClient.expectProto(req)
      httpClient.respondWith(response)
      client.getCallPrices(req).futureValue should be(response)
    }
  }
}
