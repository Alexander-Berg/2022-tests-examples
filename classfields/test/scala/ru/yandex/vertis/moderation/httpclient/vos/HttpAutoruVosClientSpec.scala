package ru.yandex.vertis.moderation.httpclient.vos

import org.asynchttpclient.{DefaultAsyncHttpClient, DefaultAsyncHttpClientConfig}
import org.junit.Ignore
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.client.impl.http.VosAutoruHttpClient

import scala.concurrent.ExecutionContext

/**
  * @author devreggs
  */
@RunWith(classOf[JUnitRunner])
@Ignore("For manually run")
class HttpAutoruVosClientSpec extends SpecBase {

  import ExecutionContext.Implicits.global
  private val httpClient = new DefaultAsyncHttpClient()

  lazy val client =
    new VosAutoruHttpClient("http://vos2-autoru-api-server.vrts-slb.test.vertis.yandex.net:80", httpClient)

  "AutoruVosClient" should {
    "offerHistory" in {
      client.offerHistory("1094331436-515bc7e6").futureValue.statuses.size should be > 0
    }
  }
}
