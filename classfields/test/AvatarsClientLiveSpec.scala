package common.clients.avatars.test

import common.clients.avatars.AvatarsClientLive.AvatarsEndpoints
import common.clients.avatars.model.{AvatarsCoordinates, ImageMeta, OrigSize}
import common.clients.avatars.{AvatarsClient, AvatarsClientLive}
import common.zio.sttp.endpoint.Endpoint
import common.zio.sttp.Sttp
import sttp.client3.Response
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.model.StatusCode
import zio.ZLayer
import zio.test.Assertion._
import zio.test._

import scala.io.Source

object AvatarsClientLiveSpec extends DefaultRunnableSpec {

  private def parseResource(path: String): String = Source.fromResource(path)(scala.io.Codec.UTF8).getLines().mkString

  private val sttpStub = AsyncHttpClientZioBackend.stub.whenRequestMatchesPartial {
    case req if req.uri.path.mkString("/") == "getinfo-oyandex/123456/image-name-123/meta" =>
      Response.ok(parseResource("get_meta_response.json"))
    case _ =>
      Response("This case was not mocked", StatusCode.NotImplemented)
  }

  private val endpoint = AvatarsEndpoints(Endpoint("read-host", port = 1), Endpoint("write-host", port = 2))

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("AvatarsClientLive")(
      testM("getMeta") {
        for {
          response <- AvatarsClient.getMeta(AvatarsCoordinates("oyandex", 123456, "image-name-123"))
        } yield assert(response.copy(content = ""))(
          equalTo(
            ImageMeta(OrigSize(1264, 1280), Some("MCFCCB4345E23263D"), "", true, None)
          )
        )
      }
    ).provideCustomLayerShared(
      (Endpoint.testEndpointLayer ++ Sttp.fromStub(sttpStub) ++ ZLayer.succeed(endpoint)) >>> AvatarsClientLive.live
    )
  }
}
