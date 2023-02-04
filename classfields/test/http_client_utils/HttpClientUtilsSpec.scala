package ru.yandex.vertis.vsquality.utils.http_client_utils

import java.nio.charset.StandardCharsets

import cats.effect.Sync
import ru.yandex.vertis.vsquality.utils.test_utils.SpecBase
import sttp.client3.Response
import sttp.model.StatusCode
import io.circe.generic.auto._
import HttpClientUtils._
import com.google.protobuf.wrappers.BoolValue
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._

class HttpClientUtilsSpec extends SpecBase {

  sealed trait MyAdt
  case class ActualJson(error: String)
  case class My200Proto(proto: BoolValue) extends MyAdt
  case class My404Json(json: ActualJson) extends MyAdt

  "HttpClientUtils.RichResponse" should {
    "resolve types in parse adt" in {
      val boolValue = BoolValue.of(true)
      val json = ActualJson("1000 lines stack trace")
      val jsonBytes: Array[Byte] = """{ "error": "1000 lines stack trace" }""".getBytes(StandardCharsets.UTF_8)
      val protoBytes = boolValue.toByteArray

      def test(bytes: Array[Byte], statusCode: StatusCode): F[MyAdt] = {
        Sync[F]
          .pure(Response(bytes, statusCode))
          .parseAdt {
            case code if code.isSuccess =>
              _.getProtobuf(BoolValue).map(My200Proto)
            case code if code == StatusCode.NotFound =>
              _.byteResponseAsUtf8Response().getJsonAs[ActualJson].map(My404Json)
          }
      }

      test(protoBytes, StatusCode.Ok).await shouldBe My200Proto(BoolValue.of(true))
      test(jsonBytes, StatusCode.NotFound).await shouldBe My404Json(json)
      a[HttpCodeException] should be thrownBy test(jsonBytes, StatusCode.UpgradeRequired).await
    }
  }
}
