package ru.yandex.vertis.moderation.httpclient.clustering.impl

import org.asynchttpclient.{DefaultAsyncHttpClient, DefaultAsyncHttpClientConfig}
import org.junit.Ignore
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.httpclient.clustering.ClusteringClient._

import scala.concurrent.ExecutionContext.Implicits.global

@Ignore
class HttpClusteringClientSpec extends SpecBase {
  val httpClient = new DefaultAsyncHttpClient()

  lazy val client =
    new HttpClusteringClient("http://user-clustering-api-int.vrts-slb.test.vertis.yandex.net", httpClient)

  "HttpClusteringClient" should {
    "neighbors" in {
      val actual = client.neighbors("897514844", Domains.Realty, ClusteringFormulas.L1Extended).futureValue
      actual.nonEmpty shouldBe true
    }
  }
}
