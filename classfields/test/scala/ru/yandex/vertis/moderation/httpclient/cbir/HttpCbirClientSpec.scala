package ru.yandex.vertis.moderation.httpclient.cbir

import org.asynchttpclient.{DefaultAsyncHttpClient, DefaultAsyncHttpClientConfig}
import org.junit.Ignore
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.httpclient.cbir.impl.HttpCbirClient

import scala.concurrent.ExecutionContext

@RunWith(classOf[JUnitRunner])
@Ignore("For manually run")
class HttpCbirClientSpec extends SpecBase {

  import ExecutionContext.Implicits.global
  val httpClient = new DefaultAsyncHttpClient()

  lazy val client = new HttpCbirClient("https://yandex.ru/images-xml/cbir", httpClient)

  "HttpWizardClient" should {
    "getImageCloneTitles" in {
      // image has clones
      client
        .getImageClones(
          "http://avatars.mds.yandex.net/get-realty/758502/add.1528991740285515276630b.realty-api-vos/orig"
        )
        .futureValue
        .size should be > 0
    }
  }
}
