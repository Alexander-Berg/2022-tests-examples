package ru.yandex.auto.garage.api.handlers.user.card

import akka.http.scaladsl.testkit.ScalatestRouteTest
import auto.carfax.common.utils.avatars.{AutoruReviewsNamespace, AvatarsExternalUrlsBuilder}
import auto.carfax.common.utils.tracing.Traced
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.internal.Mds.MdsPhotoInfo
import ru.auto.api.vin.Common.{File, Photo, S3FileInfo}
import ru.auto.api.vin.garage.GarageApiModel
import ru.auto.api.vin.garage.GarageApiModel.CardTypeInfo.CardType
import ru.auto.api.vin.garage.RequestModel.ChangeCardTypeRequest
import ru.auto.api.vin.garage.ResponseModel.ErrorCode
import ru.yandex.auto.garage.api.handlers.exceptions.{
  CardValidationException,
  InvalidCardId,
  InvalidChangeCardTypeRequestException
}
import ru.yandex.auto.garage.exceptions.{CardNotFound, ProvenOwnerResolutionNotFound, UnmodifiedCardError}
import ru.yandex.auto.garage.managers.CardsManager
import ru.yandex.auto.garage.managers.GetCardOptions.Default
import ru.yandex.auto.garage.managers.validation.{FailedValidation, RequiredMarkError, VinEditForbiddenError}
import ru.yandex.auto.vin.decoder.amazon.{FileExternalUrlsBuilder, MdsS3StorageFactory, S3Storage}
import ru.yandex.auto.vin.decoder.exceptions.InvalidUser
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.auto.vin.decoder.utils.EmptyRequestInfo
import ru.yandex.vertis.hobo.proto.Common.CarfaxProvenOwnerResolution
import ru.yandex.vertis.hobo.proto.Common.CarfaxProvenOwnerResolution.Value
import ru.yandex.vertis.hobo.proto.Model.{QueueId, Resolution, Task}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future
import scala.jdk.CollectionConverters.MapHasAsJava

class UserCardHandlerRequestProcessorTest
  extends AnyWordSpecLike
  with Matchers
  with ScalatestRouteTest
  with MockitoSupport {

  implicit val t = Traced.empty

  private val manager = mock[CardsManager]
  private val avatarsExternalUrlsBuilder = new AvatarsExternalUrlsBuilder("avatars.mdst.yandex.net")

  private val garageMdsS3Storage: S3Storage = {
    val url = "s3-private.mds.yandex.net"
    val bucket = "garage"
    val accessKey = "some_access_key"
    val secretKey = "some_secret_key"
    MdsS3StorageFactory(url, bucket, accessKey, secretKey)
  }

  private val fileExternalUrlsBuilder = new FileExternalUrlsBuilder(garageMdsS3Storage)
  implicit val r = EmptyRequestInfo

  val processor = new UserCardHandlerRequestProcessor(
    manager,
    avatarsExternalUrlsBuilder,
    fileExternalUrlsBuilder
  )

  "update card" should {
    "throw CardValidationError" in {
      val apiCard = GarageApiModel.Card.newBuilder().setUserId("user:123").build()
      val errors = List(RequiredMarkError, VinEditForbiddenError)

      when(manager.updateCard(?, ?, ?)(?)).thenReturn(Future.successful(Left(new FailedValidation(errors))))
      when(manager.getUserCard(?, ?, ?)(?))
        .thenReturn(Future.successful(Some(apiCard)))

      intercept[CardValidationException] {
        processor.updateCard("1", "user:123", apiCard).await
      }
    }
    "throw CardNotFound because of not found in db" in {
      val apiCard = GarageApiModel.Card.newBuilder().build()
      when(manager.updateCard(?, ?, ?)(?)).thenReturn(Future.failed(CardNotFound("1")))
      when(manager.getUserCard(?, ?, ?)(?))
        .thenReturn(Future.successful(Some(apiCard)))

      intercept[CardNotFound] {
        processor.updateCard("1", "user:123", apiCard).await
      }
    }
    "throw CardNotFound because of non-owner user" in {
      val apiCard = GarageApiModel.Card.newBuilder().setUserId("user:124").build()
      when(manager.getUserCard(?, ?, ?)(?))
        .thenReturn(Future.successful(Some(apiCard)))

      intercept[CardNotFound] {
        processor.updateCard("1", "user:123", apiCard).await
      }
    }
    "throw UnmodifiedCardError" in {
      val apiCard = GarageApiModel.Card.newBuilder().build()
      when(manager.updateCard(?, ?, ?)(?)).thenReturn(Future.failed(UnmodifiedCardError("1")))
      when(manager.getUserCard(?, ?, ?)(?))
        .thenReturn(Future.successful(Some(apiCard.toBuilder.setStatus(GarageApiModel.Card.Status.DELETED).build())))

      intercept[UnmodifiedCardError] {
        processor.updateCard("1", "user:123", apiCard).await
      }
    }
    "return success response model" in {
      val apiCard = GarageApiModel.Card.newBuilder().setUserId("user:123").build()
      when(manager.getUserCard(?, ?, ?)(?)).thenReturn(Future.successful(Some(apiCard)))
      when(manager.updateCard(?, ?, ?)(?)).thenReturn(Future.successful(Right(apiCard)))

      val result = processor.updateCard("1", "user:123", apiCard).await

      assert(result.getCard == apiCard)
      assert(result.getError == ErrorCode.UNKNOWN_ERROR_CODE)
    }

  }

  "delete card" should {
    "throw InvalidUser" in {
      when(manager.updateStatus(?, ?, ?)(?)).thenReturn(Future.successful(GarageApiModel.Card.newBuilder().build()))

      intercept[InvalidUser] {
        processor.deleteCard("1", "123").await
      }
    }
    "throw InvalidCardId" in {
      when(manager.updateStatus(?, ?, ?)(?)).thenReturn(Future.successful(GarageApiModel.Card.newBuilder().build()))

      intercept[InvalidCardId] {
        processor.deleteCard("a", "user:123").await
      }
    }
    "throw CardNotFound" in {
      when(manager.updateStatus(?, ?, ?)(?)).thenReturn(Future.failed(CardNotFound("1")))

      intercept[CardNotFound] {
        processor.deleteCard("1", "user:123").await
      }
    }
  }

  "get card" should {
    "successfully return card" in {
      val apiCard = GarageApiModel.Card.newBuilder().setId("123").setUserId("user:456").build()

      when(manager.getUserCard(?, ?, ?)(?)).thenReturn(Future.successful(Some(apiCard)))

      val result = processor.getCard("123", Default).await
      assert(result.getCard == apiCard)
      assert(result.getError == ErrorCode.UNKNOWN_ERROR_CODE)
      assert(result.getDetailedError.isEmpty)
    }
    "throw CardNotFound" in {
      when(manager.getUserCard(?, ?, ?)(?)).thenReturn(Future.successful(None))

      intercept[CardNotFound] {
        processor.getCard("123", Default).await
      }
    }
    "throw InvalidId" in {
      intercept[InvalidCardId] {
        processor.getCard("ab", Default).await
      }
    }
  }

  "post photoFromMdsInfo" should {
    "successfully return GarageUploadedPhotoResponse" in {

      val mdsPhotoInfo = MdsPhotoInfo
        .newBuilder()
        .setNamespace(AutoruReviewsNamespace.Name)
        .setName("name-1")
        .setGroupId(5)
        .build()

      val sizes = avatarsExternalUrlsBuilder.getUrls(mdsPhotoInfo)

      val photo = Photo
        .newBuilder()
        .setMdsPhotoInfo(mdsPhotoInfo)
        .putAllSizes(sizes.asJava)
        .build()

      val result = processor.photoFromMdsInfo(mdsPhotoInfo)

      assert(result.getPhoto == photo)
    }
  }

  "post fileFromS3FileInfo" should {
    "successfully return GarageUploadedFileResponse" in {

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

      val result = processor.fileFromS3FileInfo(s3FileInfo)

      assert(result.getFile.getS3FileInfo == file.getS3FileInfo)
      assert(result.getFile.getUrl.split("\\?")(0) == file.getUrl.split("\\?")(0))
    }
  }

  "build card" should {
    "throw InvalidUser" in {
      intercept[InvalidUser] {
        processor.buildCardFromVin("1", "user", CardType.CURRENT_CAR).await
      }
    }
  }

  "build card from Offer" should {
    "throw InvalidUser" in {
      intercept[InvalidUser] {
        processor.buildCardFromOffer("offer_id", "user", CardType.CURRENT_CAR).await
      }
    }
  }

  "set proven owner resolution" should {
    "throw InvalidUser" in {
      when(manager.updateVerdict(?, ?, ?)(?)).thenReturn(Future.unit)

      intercept[InvalidUser] {
        processor.setProvenOwnerResolution("123", "1", Task.getDefaultInstance).await
      }
    }
    "throw InvalidCardId" in {
      when(manager.updateVerdict(?, ?, ?)(?)).thenReturn(Future.unit)

      intercept[InvalidCardId] {
        processor.setProvenOwnerResolution("user:123", "a", Task.getDefaultInstance).await
      }
    }
    "throw CardNotFound" in {
      when(manager.updateVerdict(?, ?, ?)(?)).thenReturn(Future.failed(CardNotFound("1")))

      val builder =
        Task
          .newBuilder()
          .setVersion(1)
          .setQueue(QueueId.CARFAX_PROVEN_OWNER)
          .setKey("key")
          .setResolution(
            Resolution
              .newBuilder()
              .setVersion(1)
              .setCarfaxProvenOwner(
                CarfaxProvenOwnerResolution
                  .newBuilder()
                  .addValues(
                    Value
                      .newBuilder()
                      .setVerdict(CarfaxProvenOwnerResolution.Value.Verdict.CARFAX_PROVEN_OWNER_OK)
                      .build()
                  )
              )
          )
      intercept[CardNotFound] {
        processor.setProvenOwnerResolution("user:123", "1", builder.build()).await
      }
    }

    "throw ProvenOwnerResolutionNotFound" in {
      when(manager.updateVerdict(?, ?, ?)(?)).thenReturn(Future.unit)

      intercept[ProvenOwnerResolutionNotFound] {
        processor.setProvenOwnerResolution("user:123", "1", Task.getDefaultInstance).await
      }
    }
  }

  "change card type" should {
    "throw InvalidUser" in {
      intercept[InvalidUser] {
        processor.changeCardType("123", "1", ChangeCardTypeRequest.newBuilder().build()).await
      }
    }
    "throw InvalidCardId" in {
      intercept[InvalidCardId] {
        processor.changeCardType("user:123", "1", ChangeCardTypeRequest.newBuilder().build()).await
      }
    }
    "throw InvalidChangeCardTypeRequestException" in {
      intercept[InvalidChangeCardTypeRequestException] {
        processor.changeCardType("123", "user:1", ChangeCardTypeRequest.newBuilder().build()).await
      }
    }
  }
}
