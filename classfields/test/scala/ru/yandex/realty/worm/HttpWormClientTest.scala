package ru.yandex.realty.worm

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.apache.http.entity.ContentType
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Ignore, Matchers}
import ru.yandex.realty.http.{HttpEndpoint, RemoteHttpService, TestHttpClient}
import ru.yandex.realty.tracing.Traced

import scala.language.postfixOps

@Ignore
@RunWith(classOf[JUnitRunner])
class HttpWormClientTest extends FlatSpec with Matchers with ScalaFutures with TestHttpClient {

  implicit private val traced: Traced = Traced.empty

  private val client = new HttpWormClient(
    new RemoteHttpService(
      "worm-unit-test",
      HttpEndpoint.fromUri("http://worm-api-test-int.slb.vertis.yandex.net/api/1.x/"),
      testClient
    )
  )

  private val service = WormClient.RealtySitesService

  "WormClient" should "put and get single data" in {
    val data = "123098"

    val key = client.put(service, data, ContentType.TEXT_PLAIN).futureValue

    client.get(service, key).futureValue should equal(data)
  }

  it should "put and get multiple data" in {
    val data1 = "123098"
    val data2 = "456456"

    val Seq(key1, key2) = client.put(service, Seq(data1, data2), ContentType.TEXT_PLAIN).futureValue

    client.get(service, key1).futureValue should equal(data1)
    client.get(service, key2).futureValue should equal(data2)

  }

}
