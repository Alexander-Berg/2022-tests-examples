package ru.yandex.vertis.moderation.picapica.impl

import org.asynchttpclient.DefaultAsyncHttpClientConfig
import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.picapica.impl.VosPicaInfoClient.Result

import scala.concurrent.ExecutionContext

/**
  * Specs for [[VosPicaInfoClientImpl]]
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
@Ignore
class VosPicaInfoClientImplSpec extends SpecBase {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val Client =
    new VosPicaInfoClientImpl(
      "http://darl-01-sas.dev.vertis.yandex.net:3000",
      new DefaultAsyncHttpClientConfig.Builder().build()
    )

  "VosPicaInfoClientImpl" should {

    "get pica data" in {
      val oId = "1055568042-058d2"
      Client.getPicaInfo(oId).futureValue match {
        case Result(`oId`, pId, photos) if photos.size > 3 => ()
        case other                                         => fail(s"Unexpected $other")
      }
    }
  }

}
