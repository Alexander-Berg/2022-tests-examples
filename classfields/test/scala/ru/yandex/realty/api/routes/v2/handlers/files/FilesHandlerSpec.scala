package ru.yandex.realty.api.routes.v2.handlers.files

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.{JsObject, Json}
import ru.yandex.realty.akka.http.PlayJsonSupport.playUnmarshaller
import ru.yandex.realty.api.ProtoResponse.ApiGetUploadUrlResponse
import ru.yandex.realty.api.routes._
import ru.yandex.realty.api.routes.v2.files.FilesHandler
import ru.yandex.realty.api.{ApiExceptionHandler, ApiRejectionHandler}
import ru.yandex.realty.clients.rent.{
  RentClient,
  RentContractServiceClient,
  RentDocumentServiceClient,
  RentInventoryServiceClient,
  RentKeysHandoverServiceClient,
  RentModerationClient
}
import ru.yandex.realty.clients.uploader.UploaderClient
import ru.yandex.realty.clients.uploader.UploaderClient.SignAvatarsRequest
import ru.yandex.realty.files.{EntityIdentity, GetUploadUrlRequest, RentFlatIdentity}
import ru.yandex.realty.http.{HandlerSpecBase, HttpEndpoint}
import ru.yandex.realty.managers.files.DefaultFilesManager
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class FilesHandlerSpec extends HandlerSpecBase {

  val uploaderClient: UploaderClient = mock[UploaderClient]
  val rentKeysHandoverClient: RentKeysHandoverServiceClient = mock[RentKeysHandoverServiceClient]
  val rentContractClient: RentContractServiceClient = mock[RentContractServiceClient]
  val mockRentModerationClient: RentModerationClient = mock[RentModerationClient]
  val mockRentInventoryClient: RentInventoryServiceClient = mock[RentInventoryServiceClient]
  val mockRentDocumentClient: RentDocumentServiceClient = mock[RentDocumentServiceClient]
  val mockRentClient: RentClient = mock[RentClient]

  val manager = new DefaultFilesManager(
    uploaderClient,
    "rent-host",
    rentKeysHandoverClient,
    rentContractClient,
    mockRentModerationClient,
    mockRentInventoryClient,
    mockRentDocumentClient,
    mockRentClient,
    "uploader"
  )
  val handler: FilesHandler = new FilesHandler(manager, HttpEndpoint("example.com"), "uploader")
  override protected val exceptionHandler: ExceptionHandler = ApiExceptionHandler.handler
  override protected val rejectionHandler: RejectionHandler = ApiRejectionHandler.handler

  override def routeUnderTest: Route = handler.route

  "FilesHandler" when {
    "post /file/callback" should {
      "wrap request json body to `response`" in {
        val body = Json.obj(
          "namespace" -> "arenda",
          "groupId" -> 65493,
          "name" -> "023de88015d002adcec6566f8dcd5c8e"
        )
        val entity = HttpEntity(ContentTypes.`application/json`, body.toString())
        Post("/files/callback/").withEntity(entity) ~> route ~> check {
          status shouldBe StatusCodes.OK
          (responseAs[JsObject] \ "response").get shouldBe body
        }
      }
    }

    "post /files/get-upload-url" should {
      val entity = GetUploadUrlRequest
        .newBuilder()
        .addEntities(
          EntityIdentity
            .newBuilder()
            .setRentFlat(RentFlatIdentity.newBuilder().build())
            .build()
        )
        .build()
        .toByteArray
      "send callback to uploaderClient and take url from uploadClient" in {
        val signUrl = "https://test.case.ru/url"
        (uploaderClient
          .signPost(_: SignAvatarsRequest)(_: Traced))
          .expects(where { (signRequest, _) =>
            signRequest.callback.exists {
              _.headers.exists(_.name == "X-Authorization")
            }
          })
          .returns(Future.successful(UploaderClient.SignResponse("notMatter", signUrl, None)))
          .anyNumberOfTimes()
        Post("/files/get-upload-url").withEntity(entity).acceptingProto ~> route ~> check {
          status shouldBe StatusCodes.OK
          val uploadUrl = entityAs[ApiGetUploadUrlResponse].getResponse
            .getResponseEntries(0)
            .getUploadUrl
          uploadUrl shouldBe signUrl
        }
      }
    }
  }
}
