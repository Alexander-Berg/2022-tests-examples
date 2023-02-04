package ru.yandex.vertis.passport.integration.uploader

import akka.stream.ActorMaterializer
import org.scalatest.WordSpec
import ru.yandex.vertis.passport.AkkaSupport
import ru.yandex.vertis.passport.test.SpecBase
import ru.yandex.vertis.passport.util.http.AkkaHttpClient

import scala.concurrent.duration.DurationLong

/**
  *
  * @author zvez
  */
class UploaderIntSpec extends WordSpec with SpecBase with AkkaSupport {

  val client =
    new UploaderClientImpl(
      UploaderConfig("http://uploader-api.vrts-slb.test.vertis.yandex.net:80"),
      new AkkaHttpClient(ActorMaterializer())
    )

  "UploaderClient" should {
    "return upload url" in {
      val url = client.sign("autoru-users", 1.minute, "http://localhost/test").futureValue
      url should not be empty
    }
  }

}
