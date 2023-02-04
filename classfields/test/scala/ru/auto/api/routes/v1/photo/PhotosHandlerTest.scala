package ru.auto.api.routes.v1.photo

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Accept
import org.mockito.Mockito._
import ru.auto.api.ApiSuite
import ru.auto.api.exceptions.PhotoNotFoundException
import ru.auto.api.model.ModelGenerators.PhotoIDGen
import ru.auto.api.model.{BinaryResponse, ModelGenerators, PhotoID, SignedData}
import ru.auto.api.services.mds.DefaultPrivateMdsClient.MediaTypeImageHeic
import ru.auto.api.services.{MockedClients, MockedPassport}
import ru.auto.api.util.JwtUtils
import ru.auto.api.util.StringUtils.PathPart

class PhotosHandlerTest extends ApiSuite with MockedClients with MockedPassport {

  private val user = ModelGenerators.PrivateUserRefGen.next

  private val photo = Array[Byte](10, -32, 17, 22)
  private val binaryJpegResponse = BinaryResponse(MediaTypes.`image/jpeg`, photo)
  private val binaryHeicResponse = BinaryResponse(MediaTypeImageHeic, photo)

  before {
    reset(passportManager)
    when(passportManager.getClientId(?)(?)).thenReturnF(None)
  }

  after {
    verifyNoMoreInteractions(passportManager)
  }

  test("get raw photo as image/jpeg") {
    val photoId = PhotoIDGen.next
    when(privateMdsClient.getOrigPhoto(?)(?)).thenReturnF(binaryJpegResponse)

    val url: String = s"/1.0/photos/raw-photo${makeQuery(Map("sign" -> makeSign(photoId)))}"
    Get(url) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[Array[Byte]]) {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentType(MediaTypes.`image/jpeg`)
          responseAs[Array[Byte]] shouldBe photo
        }
        verify(privateMdsClient).getOrigPhoto(eq(photoId))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("get raw photo as image/heic") {
    val photoId = PhotoIDGen.next
    when(privateMdsClient.getOrigPhoto(?)(?)).thenReturnF(binaryHeicResponse)

    val url: String = s"/1.0/photos/raw-photo${makeQuery(Map("sign" -> makeSign(photoId)))}"
    Get(url) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[Array[Byte]]) {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentType(MediaTypeImageHeic)
          responseAs[Array[Byte]] shouldBe photo
        }
        verify(privateMdsClient).getOrigPhoto(eq(photoId))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("get raw photo failed (photo is not found)") {
    val photoId = PhotoIDGen.next
    when(privateMdsClient.getOrigPhoto(?)(?)).thenThrowF(new PhotoNotFoundException)

    val url: String = s"/1.0/photos/raw-photo${makeQuery(Map("sign" -> makeSign(photoId)))}"
    Get(url) ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.NotFound
          contentType shouldBe ContentTypes.`application/json`
        }

        responseAs[String] should matchJson("""{
                                              |  "error": "PHOTO_NOT_FOUND",
                                              |  "status": "ERROR",
                                              |  "detailed_error": "PHOTO_NOT_FOUND"
                                              |}""".stripMargin)

        verify(privateMdsClient).getOrigPhoto(eq(photoId))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  private def makeQuery(params: Map[String, String]): Serializable =
    PathPart(params.transform((k, v) => k + "=" + v).values.mkString("?", "&", ""))

  private def makeSign(photoId: PhotoID): String =
    JwtUtils.sign(SignedData(photoId.namespace, photoId.groupId.toString, photoId.hash))

}
