package auto.dealers.match_maker.logic.clients

import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.ResponseModel.{ErrorResponse, OfferListingResponse, ResponseStatus}
import ru.auto.api.search.SearchModel.SearchRequestParameters
import auto.dealers.match_maker.logic.clients.PublicApiClient.PublicApiClient
import auto.dealers.match_maker.logic.clients.SttpMockingUtils._
import auto.dealers.match_maker.logic.clients.SttpPublicApiClient.{PublicApiConfig, PublicApiException}
import common.zio.sttp.Sttp
import sttp.model.StatusCode
import zio.test.Assertion._
import zio.test.{DefaultRunnableSpec, _}
import zio.{ULayer, ZLayer}

object SttpPublicApiClientSpec extends DefaultRunnableSpec {
  import SttpPublicApiClientSpecOps._

  def spec =
    suite("SttpPublicApiClient")(
      withSttpBackendProvided(getPublicApiClient) { sttpBackend =>
        testM("Should work fine when returns OK") {
          for {
            resp <- createDefaultResponse[Either[ErrorResponse, OfferListingResponse]](
              Right(OfferListingResponse.newBuilder().addOffers(Offer.getDefaultInstance).build()),
              StatusCode.Ok,
              "OK"
            )

            _ <- mockSttpSend(sttpBackend, resp)

            result <- PublicApiClient.search(SearchRequestParameters.getDefaultInstance, 10).either
          } yield assert(result)(isRight(hasSize(equalTo(1))))
        }
      },
      withSttpBackendProvided(getPublicApiClient) { sttpBackend =>
        testM("Work correct with error") {
          for {
            resp <- createDefaultResponse[Either[ErrorResponse, OfferListingResponse]](
              Left(ErrorResponse.newBuilder().setDetailedError("Bad case!").setStatus(ResponseStatus.ERROR).build()),
              StatusCode.InternalServerError,
              "Internal Server Error"
            )

            _ <- mockSttpSend(sttpBackend, resp)

            result <- PublicApiClient.search(SearchRequestParameters.getDefaultInstance, 10).either
          } yield assert(result)(isLeft(isSubtype[PublicApiException](Assertion.anything)))
        }
      }
    )
}

object SttpPublicApiClientSpecOps {

  def getPublicApiClient(sttp: Sttp.Service): ULayer[PublicApiClient] =
    ZLayer.succeed {
      new SttpPublicApiClient(sttp, PublicApiConfig("url", 80, "http", "very_complicated_token"))
    }
}
