package auto.dealers.match_maker.logic.clients

import auto.dealers.match_maker.logic.clients.SttpMockingUtils._
import auto.dealers.match_maker.logic.clients.SttpTeleponyClient.TeleponyConfig
import auto.dealers.match_maker.logic.clients.TeleponyClient.TeleponyClient
import ru.auto.match_maker.model.api.ApiModel.RedirectPhoneInfo
import common.zio.sttp.Sttp
import sttp.client3.{HttpError, ResponseError, ResponseException}
import sttp.model.StatusCode
import zio.test.Assertion._
import zio.test.{DefaultRunnableSpec, _}
import zio.{ULayer, ZLayer}

object SttpTeleponyClientSpec extends DefaultRunnableSpec {
  import SttpTeleponyClientSpecOps._

  def spec =
    suite("SttpTeleponyClientSpec")(
      withSttpBackendProvided(getTeleponyClient) { sttpBackend =>
        testM("Should work fine with OK response") {
          for {
            resp <- createDefaultResponse[Either[ResponseException[String, Exception], TeleponyClient.Redirect]](
              Right(TeleponyClient.Redirect("id", "objectId", null, "source", "target", None)),
              StatusCode.Ok,
              "OK"
            )

            _ <- mockSttpSend(sttpBackend, resp)

            result <-
              TeleponyClient
                .getOrCreateRedirect(
                  "object",
                  TeleponyClient.CreateRequest("+79999999999", None, None, None, None, None)
                )
                .either
          } yield assert(result)(
            isRight(hasField("redirectPhone", _.getRedirectPhone, equalTo("source")))
          )
        }
      },
      withSttpBackendProvided(getTeleponyClient) { sttpBackend =>
        testM("Should fail when response failed") {
          for {
            resp <- createDefaultResponse[Either[ResponseException[String, Exception], TeleponyClient.Redirect]](
              Left(HttpError("error", StatusCode.InternalServerError)),
              StatusCode.InternalServerError,
              "Internal server error"
            )

            _ <- mockSttpSend(sttpBackend, resp)

            result <-
              TeleponyClient
                .getOrCreateRedirect(
                  "object",
                  TeleponyClient.CreateRequest("+79999999999", None, None, None, None, None)
                )
                .either
          } yield assert(result.isLeft)(isTrue)
        }
      }
    )
}

object SttpTeleponyClientSpecOps {

  def getTeleponyClient(sttp: Sttp.Service): ULayer[TeleponyClient] =
    ZLayer.succeed {
      new SttpTeleponyClient(sttp, TeleponyConfig("url", 80, "http", "domain")): TeleponyClient.Service
    }
}
