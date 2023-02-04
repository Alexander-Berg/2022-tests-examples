package ru.auto.api.managers.photo

import akka.http.scaladsl.model.MediaTypes
import org.mockito.Mockito.{reset, verify}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.{BeforeAndAfter, OptionValues}
import ru.auto.api.BaseSpec
import ru.auto.api.exceptions.PhotoNotFoundException
import ru.auto.api.model.ModelGenerators.PhotoIDGen
import ru.auto.api.model.{BinaryResponse, PhotoID, SignedData}
import ru.auto.api.services.mds.PrivateMdsClient
import ru.auto.api.services.searcher.SearcherClient
import ru.auto.api.services.uploader.UploaderClient
import ru.auto.api.util.JwtUtils
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future

class PhotosManagerSpec
  extends BaseSpec
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with BeforeAndAfter
  with OptionValues {
  private val privateMdsClient: PrivateMdsClient = mock[PrivateMdsClient]
  private val uploaderClient: UploaderClient = mock[UploaderClient]
  private val searchClient: SearcherClient = mock[SearcherClient]

  after {
    reset(privateMdsClient)
  }

  implicit private val trace: Traced = Traced.empty

  private val photosManager: PhotosManager = new PhotosManager(
    uploaderClient,
    privateMdsClient,
    searchClient
  )

  "PhotosManager.getOfferRawPhoto" should {
    "load orig photo from mdsClient as byte array" in {
      val photoId = PhotoIDGen.next
      val sign = makeSign(photoId)
      val photo = Array[Byte](10, -32, 17, 22)
      val binaryResponse = BinaryResponse(MediaTypes.`image/jpeg`, photo)
      when(privateMdsClient.getOrigPhoto(?)(?)).thenReturnF(binaryResponse)

      val result = photosManager.getOfferRawPhoto(sign).futureValue
      result shouldBe binaryResponse

      verify(privateMdsClient).getOrigPhoto(
        photoId
      )(trace)
    }
    "throw PhotoNotFoundException in case of photo is not found" in {
      val photoId = PhotoIDGen.next
      val sign = makeSign(photoId)
      when(privateMdsClient.getOrigPhoto(?)(?)).thenReturn(Future.failed(new PhotoNotFoundException()))

      intercept[PhotoNotFoundException] {
        photosManager.getOfferRawPhoto(sign).await
      }

      verify(privateMdsClient).getOrigPhoto(
        photoId
      )(trace)
    }
  }

  private def makeSign(photoId: PhotoID): String =
    JwtUtils.sign(SignedData(photoId.namespace, photoId.groupId.toString, photoId.hash))
}
