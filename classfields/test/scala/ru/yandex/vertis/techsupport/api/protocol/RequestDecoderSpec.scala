package ru.yandex.vertis.vsquality.techsupport.api.protocol

import cats.effect.IO
import com.softwaremill.tagging.Tagger
import io.circe.{Decoder, Json}
import org.http4s.circe.jsonEncoderOf
import org.http4s.headers.`Content-Type`
import org.http4s._
import ru.yandex.vertis.vsquality.techsupport.config.MdsConfig
import ru.yandex.vertis.vsquality.techsupport.model.api.RequestMeta
import ru.yandex.vertis.vsquality.techsupport.model.{ChatProvider, Domain}
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase

import java.time.Instant

class RequestDecoderSpec extends SpecBase {
  private val mdsConfig = MdsConfig("http://host.com")
  private val requestMeta = RequestMeta(Instant.now, "some_req_id".taggedWith, deviceId = None)

  "RequestDecoder" should {
    "decode JSON" in {
      val jsonStr =
        """{
          |  "id" : "38496913-2ce0-46f4-940e-178c908e28c3",
          |  "roomId" : "kjHvwuyxohjXef2iwMgtwlywiy",
          |  "author" : "user:123456",
          |  "created": {"seconds": 1632310095, "nanos": 0},
          |  "providedId": "",
          |  "attachments": [],
          |  "isSilent": false,
          |  "isSpam": false,
          |  "properties" : {
          |    "userAppVersion" : "1.2.3",
          |    "techSupportOperatorId" : "staff:fired",
          |    "type": "CHAT_OPEN_EVENT"
          |  }
          |}""".stripMargin
      val req = makeJsonRequest(io.circe.parser.parse(jsonStr).toTry.get)

      val decoded = new RequestDecoder[IO]
        .decodeFromChat(req, ChatProvider.VertisChats, Domain.Autoru, mdsConfig)(requestMeta)
        .attempt
        .unsafeRunSync()

      println(decoded)

      decoded.isRight shouldBe true
    }
  }

  private def makeJsonRequest(json: Json): Request[F] =
    Request(
      method = Method.POST,
      uri = uri("http://url"),
      headers = jsonHeaders,
      body = jsonEncoderOf[F, Json].toEntity(json).body
    )

  private val jsonHeaders =
    Headers.of(
      Header("x-operator-yandex-id", "test_operator"),
      `Content-Type`(MediaType.application.json)
    )

  private def uri(s: String): Uri = Uri.fromString(s).toOption.get
}
