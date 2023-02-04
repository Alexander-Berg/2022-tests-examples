package ru.auto.api.routes.v1.user.draft.photos

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Accept, RawHeader}
import org.mockito.Mockito._
import ru.auto.api.ApiOfferModel.Multiposting.Classified.ClassifiedName._
import ru.auto.api.ApiSuite
import ru.auto.api.ResponseModel.{PhotoSaveSuccessResponse, ResponseStatus, SuccessResponse}
import ru.auto.api.features.FeatureManager
import ru.auto.api.model.CategorySelector.Cars
import ru.auto.api.model.ModelGenerators
import ru.auto.api.services.{MockedClients, MockedPassport}

/**
  * Created by andrey on 3/23/17.
  */
class DraftPhotosHandlerTest extends ApiSuite with MockedClients with MockedPassport {

  override lazy val featureManager: FeatureManager = mock[FeatureManager]

  private val user = ModelGenerators.PrivateUserRefGen.next
  private val draftId = ModelGenerators.OfferIDGen.next
  private val photoId = ModelGenerators.PhotoIDGen.next

  private val photoSaveSuccessResponse: PhotoSaveSuccessResponse =
    PhotoSaveSuccessResponse
      .newBuilder()
      .setStatus(ResponseStatus.SUCCESS)
      .setPhotoId(photoId.toPlain)
      .build()

  private val simpleSuccessResponse = SuccessResponse
    .newBuilder()
    .setStatus(ResponseStatus.SUCCESS)
    .build()

  before {
    reset(passportManager)
    when(passportManager.getClientId(?)(?)).thenReturnF(None)
  }

  after {
    verifyNoMoreInteractions(passportManager)
  }

  test("add photo to draft") {
    pending
    when(vosClient.draftPhotoAdd(?, ?, ?, ?)(?)).thenReturnF(photoSaveSuccessResponse)

    val entity = Multipart.FormData(
      Multipart.FormData.BodyPart("file", HttpEntity(Array[Byte](1, 2, 3, 4, 5)), Map("filename" -> "test"))
    )

    val url: String = s"/1.0/user/draft/cars/$draftId/photo"
    val req = Post(url, entity).withHeaders(RawHeader("x-uid", user.uid.toString))

    checkSuccessRequest(req)
    verify(vosClient).draftPhotoAdd(eq(Cars), eq(user), eq(draftId), ?)(?)
    verify(passportManager).getClientId(eq(user))(?)
  }

  test("rotate photo (cw)") {
    when(vosClient.draftPhotoRotate(?, ?, ?, ?, ?, ?)(?)).thenReturnF(photoSaveSuccessResponse)

    val url: String = s"/1.0/user/draft/cars/$draftId/photo/$photoId/rotate/cw"
    val req = Put(url).withHeaders(RawHeader("x-uid", user.uid.toString))
    checkSuccessRequest(req)
    verify(vosClient).draftPhotoRotate(eq(Cars), eq(user), eq(draftId), eq(AUTORU), eq(photoId), eq(true))(?)
    verify(passportManager).getClientId(eq(user))(?)
  }

  test("rotate classified photo (cw)") {
    when(vosClient.draftPhotoRotate(?, ?, ?, ?, ?, ?)(?)).thenReturnF(photoSaveSuccessResponse)

    val url: String = s"/1.0/user/draft/cars/$draftId/multiposting/avito/photo/$photoId/rotate/cw"
    val req = Put(url).withHeaders(RawHeader("x-uid", user.uid.toString))
    checkSuccessRequest(req)
    verify(vosClient).draftPhotoRotate(eq(Cars), eq(user), eq(draftId), eq(AVITO), eq(photoId), eq(true))(?)
    verify(passportManager).getClientId(eq(user))(?)
  }

  test("rotate photo (ccw)") {
    when(vosClient.draftPhotoRotate(?, ?, ?, ?, ?, ?)(?)).thenReturnF(photoSaveSuccessResponse)

    val url2: String = s"/1.0/user/draft/cars/$draftId/photo/$photoId/rotate/ccw"
    val req2 = Put(url2).withHeaders(RawHeader("x-uid", user.uid.toString))
    checkSuccessRequest(req2)
    verify(vosClient).draftPhotoRotate(eq(Cars), eq(user), eq(draftId), eq(AUTORU), eq(photoId), eq(false))(?)
    verify(passportManager).getClientId(eq(user))(?)
  }

  test("rotate classified photo (ccw)") {
    when(vosClient.draftPhotoRotate(?, ?, ?, ?, ?, ?)(?)).thenReturnF(photoSaveSuccessResponse)

    val url2: String = s"/1.0/user/draft/cars/$draftId/multiposting/avito/photo/$photoId/rotate/ccw"
    val req2 = Put(url2).withHeaders(RawHeader("x-uid", user.uid.toString))
    checkSuccessRequest(req2)
    verify(vosClient).draftPhotoRotate(eq(Cars), eq(user), eq(draftId), eq(AVITO), eq(photoId), eq(false))(?)
    verify(passportManager).getClientId(eq(user))(?)
  }

  test("restore photo") {
    when(vosClient.draftPhotoRestore(?, ?, ?, ?, ?)(?)).thenReturnF(photoSaveSuccessResponse)

    val url: String = s"/1.0/user/draft/cars/$draftId/photo/$photoId/restore"
    val req = Put(url).withHeaders(RawHeader("x-uid", user.uid.toString))
    checkSuccessRequest(req)
    verify(vosClient).draftPhotoRestore(eq(Cars), eq(user), eq(draftId), eq(AUTORU), eq(photoId))(?)
    verify(passportManager).getClientId(eq(user))(?)
  }

  test("restore classified photo") {
    when(vosClient.draftPhotoRestore(?, ?, ?, ?, ?)(?)).thenReturnF(photoSaveSuccessResponse)

    val url: String = s"/1.0/user/draft/cars/$draftId/multiposting/drom/photo/$photoId/restore"
    val req = Put(url).withHeaders(RawHeader("x-uid", user.uid.toString))
    checkSuccessRequest(req)
    verify(vosClient).draftPhotoRestore(eq(Cars), eq(user), eq(draftId), eq(DROM), eq(photoId))(?)
    verify(passportManager).getClientId(eq(user))(?)
  }

  test("delete photo") {
    when(vosClient.draftPhotoDelete(?, ?, ?, ?, ?)(?)).thenReturnF(simpleSuccessResponse)

    val url: String = s"/1.0/user/draft/cars/$draftId/photo/$photoId"
    val req = Delete(url).withHeaders(RawHeader("x-uid", user.uid.toString))
    checkSuccessRequest(req)
    verify(vosClient).draftPhotoDelete(eq(Cars), eq(user), eq(draftId), eq(AUTORU), eq(photoId))(?)
    verify(passportManager).getClientId(eq(user))(?)
  }

  test("delete classified photo") {
    when(vosClient.draftPhotoDelete(?, ?, ?, ?, ?)(?)).thenReturnF(simpleSuccessResponse)

    val url: String = s"/1.0/user/draft/cars/$draftId/multiposting/drom/photo/$photoId"
    val req = Delete(url).withHeaders(RawHeader("x-uid", user.uid.toString))
    checkSuccessRequest(req)
    verify(vosClient).draftPhotoDelete(eq(Cars), eq(user), eq(draftId), eq(DROM), eq(photoId))(?)
    verify(passportManager).getClientId(eq(user))(?)
  }

  private def checkSuccessRequest(req: HttpRequest): Unit = {
    req ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-authorization", "Vertis mobile-b5ae229026745eb72110371c992ba567") ~>
      route ~>
      check {
        val response = responseAs[String]
        withClue(response) {
          status shouldBe StatusCodes.OK
        }
      }
  }
}
