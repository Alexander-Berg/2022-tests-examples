package ru.yandex.vertis.phonoteka.api.v1

import java.time.Instant

import cats.data.Kleisli
import cats.effect.{Blocker, IO}
import io.circe.Printer
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.headers.{`Content-Type`, Accept}
import org.scalatest.Assertion
import ru.yandex.vertis.phonoteka.api.{ApiController, RoutesBuilder}
import ru.yandex.vertis.phonoteka.model.Arbitraries._
import ru.yandex.vertis.phonoteka.model.api.{ApiRequest, ApiResponse, RequestContext}
import ru.yandex.vertis.phonoteka.model.metadata.{Metadata, MetadataType}
import ru.yandex.vertis.phonoteka.proto.ProtoFormatInstances._
import ru.yandex.vertis.phonoteka.util.RequestContextSupport
import ru.yandex.vertis.quality.cats_utils.Awaitable._
import ru.yandex.vertis.quality.scalapb_utils.ProtoJson._
import ru.yandex.vertis.quality.test_utils.MockitoUtil._
import ru.yandex.vertis.quality.test_utils.SpecBase

import scala.concurrent.ExecutionContext.Implicits.global

class ApiSpec extends SpecBase {

  import ApiSpec._

  private val mockedApiController: ApiController[F] = mock[ApiController[F]]

  implicit private val httpApp: HttpApp[F] =
    new RoutesBuilder(mockedApiController, Blocker.liftExecutionContext(global)).httpApp

  "Api handlers" should {
    "POST metadata" in {
      val requestMeta = generate[RequestContext]()
      val metadata = generate[Metadata]()
      val metadataSet = Set(metadata)
      val phones = Set(metadata.phone)
      val since = generate[Some[Instant]]()
      val greedy = generate[Boolean]()
      when(mockedApiController.get(?)(?))
        .thenReturnOnly(IO.pure(ApiResponse.GetMetadata(Map(metadata.phone.value -> metadataSet))))
      val request =
        ApiRequest.GetMetadata(
          phones,
          properties =
            Set(
              ApiRequest.GetMetadata.Property(MetadataType.Of, updatedSince = since, greedy)
            )
        )
      val print = request.asJson.printWith(Printer.noSpaces)
      val headers =
        Headers.of(
          Header(RequestContextSupport.`X-Request-Id`, requestMeta.requestId),
          `Content-Type`.apply(MediaType.application.json),
          Accept.apply(MediaType.application.json)
        )
      val actualResponse =
        runRequest(
          method = Method.POST,
          path = ApiMetadataPath,
          body = EntityEncoder.stringEncoder.toEntity(print).body,
          headers = headers
        )
      check(
        actualResponse,
        expectedStatus = Status.Ok,
        expectedBody = Some(ApiResponse.GetMetadata(Map(metadata.phone.value -> metadataSet)).asJson)
      )
    }
  }

  private def check[A](actual: IO[Response[IO]], expectedStatus: Status, expectedBody: Option[A])(
      implicit ev: EntityDecoder[IO, A]
  ): Assertion = {
    val actualResp = actual.await
    val statusCheck = actualResp.status == expectedStatus
    val bodyCheck =
      expectedBody.forall { expected =>
        actualResp.as[A].await == expected
      }
    (statusCheck && bodyCheck) shouldBe true
  }

}

object ApiSpec {

  private val ApiPath: String = "/api/v1"
  private def ApiMetadataPath: String = s"$ApiPath/metadata"

  private def toUri(s: String): Uri = Uri.fromString(s).toOption.get

  private def runRequest[F[_]](method: Method,
                               path: String,
                               body: EntityBody[F] = EmptyBody,
                               headers: Headers = Headers.empty
                              )(implicit httpApp: Kleisli[F, Request[F], Response[F]]): F[Response[F]] =
    httpApp.run(Request(method, toUri(path), body = body, headers = headers))
}
