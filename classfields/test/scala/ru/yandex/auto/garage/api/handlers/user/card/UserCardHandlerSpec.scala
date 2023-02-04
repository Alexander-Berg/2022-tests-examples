package ru.yandex.auto.garage.api.handlers.user.card

import akka.http.scaladsl.model.headers.CustomHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import auto.carfax.common.utils.avatars.{AutoruReviewsNamespace, AvatarsExternalUrlsBuilder}
import auto.carfax.common.utils.tracing.Traced
import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.internal.Mds.MdsPhotoInfo
import ru.auto.api.vin.Common.{File, Photo, S3FileInfo}
import ru.auto.api.vin.garage.ResponseModel.{
  GarageUploadedFileResponse,
  GarageUploadedPhotoResponse,
  GetCardResponse,
  ResponseStatus
}
import ru.yandex.auto.garage.api.handlers.exceptions.{CardValidationException, InvalidCardId}
import ru.yandex.auto.garage.exceptions.{CardNotFound, UnmodifiedCardError}
import ru.yandex.auto.vin.decoder.amazon.{FileExternalUrlsBuilder, MdsS3StorageFactory, S3Storage}
import ru.yandex.auto.vin.decoder.api.RequestDirectives
import ru.yandex.auto.vin.decoder.utils.{EmptyRequestInfo, RequestInfo}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future
import scala.jdk.CollectionConverters.MapHasAsJava

class UserCardHandlerSpec
  extends AnyWordSpecLike
  with Matchers
  with ScalatestRouteTest
  with MockitoSupport
  with BeforeAndAfter {
  implicit val requestInfo: RequestInfo = EmptyRequestInfo

  implicit val t = Traced.empty

  private val processor = mock[UserCardHandlerRequestProcessor]
  val handler = new UserCardHandler(processor)
  val route: Route = RequestDirectives.wrapRequest(handler.route)
  private val avatarsExternalUrlsBuilder = new AvatarsExternalUrlsBuilder("avatars.mdst.yandex.net")

  private val garageMdsS3Storage: S3Storage = {
    val url = "s3-private.mds.yandex.net"
    val bucket = "garage"
    val accessKey = "some_access_key"
    val secretKey = "some_secret_key"
    MdsS3StorageFactory(url, bucket, accessKey, secretKey)
  }
  private val fileExternalUrlsBuilder = new FileExternalUrlsBuilder(garageMdsS3Storage)

  private val defaultUserHeader = new CustomHeader {
    override def name(): String = "x-user-id"

    override def value(): String = "user:123"

    override def renderInRequests(): Boolean = true

    override def renderInResponses(): Boolean = true
  }

  private val defaultGetCardByIdRequest = {
    Get("/1").addHeader(defaultUserHeader)
  }

  private val defaultApiCard = "{}"

  private val defaultUpdateCardRequest = {
    Put("/1")
      .withEntity(ContentTypes.`application/json`, defaultApiCard)
      .addHeader(defaultUserHeader)
  }

  before {
    reset(processor)
  }

  "get card" should {
    "return 404" when {
      "processor throw CardNotFoundException" in {
        when(processor.getCard(?, ?)(?)).thenReturn(Future.failed(CardNotFound("123")))

        defaultGetCardByIdRequest ~> route ~> check {
          status shouldBe StatusCodes.NotFound
          contentType shouldBe ContentTypes.`application/json`
          responseAs[
            String
          ] shouldBe "{\n  \"error\": \"CARD_NOT_FOUND\",\n  \"status\": \"ERROR\",\n  \"detailedError\": \"Card not found: id 123\"\n}"
        }
      }
    }
    "return 400" when {
      "processor throw InvalidCardIdException" in {
        when(processor.getCard(?, ?)(?)).thenReturn(Future.failed(InvalidCardId("123")))

        defaultGetCardByIdRequest ~> route ~> check {
          status shouldBe StatusCodes.BadRequest
          contentType shouldBe ContentTypes.`application/json`
          responseAs[
            String
          ] shouldBe "{\n  \"error\": \"BAD_REQUEST\",\n  \"status\": \"ERROR\",\n  \"detailedError\": \"Invalid card id: 123\"\n}"
        }
      }
    }
    "return 200" in {
      val protoResponse = GetCardResponse.newBuilder().build()
      when(processor.getCard(?, ?)(?)).thenReturn(Future.successful(protoResponse))

      defaultGetCardByIdRequest ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "update card" should {
    "return 400 validation error" in {
      when(processor.updateCard(?, ?, ?)(?)).thenReturn(Future.failed(CardValidationException(List.empty)))

      defaultUpdateCardRequest ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[
          String
        ] shouldBe "{\n  \"error\": \"VALIDATION_ERROR\",\n  \"status\": \"ERROR\",\n  \"detailedError\": \"Validation error\"\n}"
      }
    }
    "404 not found" in {
      when(processor.updateCard(?, ?, ?)(?)).thenReturn(Future.failed(CardNotFound("123")))

      defaultUpdateCardRequest ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        contentType shouldBe ContentTypes.`application/json`
        responseAs[
          String
        ] shouldBe "{\n  \"error\": \"CARD_NOT_FOUND\",\n  \"status\": \"ERROR\",\n  \"detailedError\": \"Card not found: id 123\"\n}"
      }
    }
    "return 400 card has deleted status" in {
      when(processor.updateCard(?, ?, ?)(?)).thenReturn(Future.failed(UnmodifiedCardError("123")))

      defaultUpdateCardRequest ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[
          String
        ] shouldBe "{\n  \"error\": \"BAD_REQUEST\",\n  \"status\": \"ERROR\",\n  \"detailedError\": \"Card[123] has DELETED status and cannot be modified\"\n}"
      }
    }
  }

  "post photoFromMdsInfo" should {
    "return 200" in {

      val mdsPhoto = MdsPhotoInfo
        .newBuilder()
        .setNamespace(AutoruReviewsNamespace.Name)
        .setName("name-1")
        .setGroupId(5)
        .build()

      val sizes = avatarsExternalUrlsBuilder.getUrls(mdsPhoto)

      val photo = Photo
        .newBuilder()
        .setMdsPhotoInfo(mdsPhoto)
        .putAllSizes(sizes.asJava)
        .build()

      val mayResponse = GarageUploadedPhotoResponse
        .newBuilder()
        .setPhoto(photo)
        .setResponseStatus(ResponseStatus.SUCCESS)
        .build()

      when(processor.photoFromMdsInfo(?)(?)).thenReturn(mayResponse)

      val correctBodyRequest: HttpEntity.Strict = HttpEntity.apply(
        ContentTypes.`application/json`,
        s"""{"namespace": "${AutoruReviewsNamespace.Name}",  "groupId": 5,  "name": "name-1"}"""
      )

      (Post("/mds_photo", correctBodyRequest).addHeader(defaultUserHeader)) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldBe """{"photo":{"mds_photo_info":{"namespace":"autoru-reviews","group_id":5,"name":"name-1"},"sizes":{"thumb_m":"//avatars.mdst.yandex.net/get-autoru-reviews/5/name-1/thumb_m","full":"//avatars.mdst.yandex.net/get-autoru-reviews/5/name-1/full","320x240":"//avatars.mdst.yandex.net/get-autoru-reviews/5/name-1/320x240","1200x900":"//avatars.mdst.yandex.net/get-autoru-reviews/5/name-1/1200x900","small":"//avatars.mdst.yandex.net/get-autoru-reviews/5/name-1/small"}},"response_status":"SUCCESS"}"""
      }
    }
  }

  "post fileFromS3FileInfo" should {
    "return 200" in {

      val s3FileInfo = S3FileInfo
        .newBuilder()
        .setNamespace(AutoruReviewsNamespace.Name)
        .setName("name-1")
        .build()

      val url = fileExternalUrlsBuilder.getPreSignedUrl(s3FileInfo, S3Storage.TTLPreSignedUrl)

      val file = File
        .newBuilder()
        .setUrl(url)
        .setS3FileInfo(s3FileInfo)
        .build()

      val mayResponse = GarageUploadedFileResponse
        .newBuilder()
        .setFile(file)
        .setResponseStatus(ResponseStatus.SUCCESS)
        .build()

      when(processor.fileFromS3FileInfo(?)(?)).thenReturn(mayResponse)

      val correctBodyRequest: HttpEntity.Strict = HttpEntity.apply(
        ContentTypes.`application/json`,
        s"""{"namespace": "${AutoruReviewsNamespace.Name}", "groupId": 5,  "name": "name-1"}"""
      )

      (Post("/s3_file", correctBodyRequest).addHeader(defaultUserHeader)) ~> route ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[String]
        (response should startWith).regex(
          """\{"file":\{"s3_file_info":\{"namespace":"autoru-reviews","name":"name-1"\},"url":"https://garage.s3-private.mds.yandex.net/name-1?.*"\},"response_status":"SUCCESS"\}"""
        )
      }
    }
  }

  "post setProvenOwnerStateResolution" should {
    "return 200" in {

      when(processor.setProvenOwnerResolution(?, ?, ?)(?)).thenReturn(Future.unit)

      val correctBodyRequest: HttpEntity.Strict = HttpEntity.apply(
        ContentTypes.`application/json`,
        """{
            |  "version": 1,
            |  "key": "key",
            |  "queue": "CARFAX_PROVEN_OWNER",
            |  "resolution" : {
            |    "version": 1,
            |      "carfax_proven_owner" : {
            |         "values" : [
            |           { "verdict": "CARFAX_PROVEN_OWNER_OK" }
            |            ]
            |         }
            |    }
            |}""".stripMargin
      )
      (Post("/hobo/resolution/user:1/123", correctBodyRequest).addHeader(defaultUserHeader)) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }
  }
}
