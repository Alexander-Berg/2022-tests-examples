package ru.yandex.vos2.autoru.api.v1.draft

import akka.http.scaladsl.model.Multipart.FormData.BodyPart
import akka.http.scaladsl.model._
import org.junit.runner.RunWith
import org.scalactic.source
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.OptionValues
import org.scalatest.funsuite.AnyFunSuiteLike
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.CarsModel
import ru.auto.api.CommonModel.{Photo => ApiPhoto}
import ru.auto.api.ResponseModel.{PhotoSaveSuccessResponse, ResponseStatus, StsPhotoUploadResponse}
import ru.yandex.vos2.BasicsModel.{Photo, PhotoOrBuilder}
import ru.yandex.vos2.OfferID
import ru.yandex.vos2.OfferModel.Multiposting.Classified.ClassifiedName.AUTORU
import ru.yandex.vos2.autoru.model.AutoruModelUtils._
import ru.yandex.vos2.autoru.utils._
import ru.yandex.vos2.autoru.utils.testforms.{CarTestForms, TestFormParams}
import ru.yandex.vos2.services.mds.{AutoruOrigNamespaceSettings, AutoruVosNamespaceSettings, MdsPhotoData}
import ru.yandex.vos2.util.ExternalAutoruUserRef

import scala.jdk.CollectionConverters._

/**
  * Created by andrey on 3/17/17.
  */
@RunWith(classOf[JUnitRunner])
class DraftHandlerPhotosTest extends AnyFunSuiteLike with PhotoUtilsApiSuite with OptionValues {

  import Vos2ApiHandlerResponses._

  private val carTestForms = new CarTestForms(components)

  test("add sts photo from external url") {
    val userId: Long = carTestForms.randomUser.id
    val extUserId: String = ExternalAutoruUserRef.privateRef(userId)

    // создаем черновик
    val req1 = createRequest(Post(s"/api/v1/draft/cars/$extUserId")) { builder =>
      builder.setCarInfo(CarsModel.CarInfo.newBuilder().setArmored(true))
    }
    val draftId = checkSuccessRequest(req1)
    val readForm = checkSuccessReadRequest(Get(s"/api/v1/draft/cars/$extUserId/$draftId"))
    val userRef = readForm.getUserRef

    // не передали файл
    checkErrorRequest(
      Post(s"/api/v1/draft/cars/$extUserId/$draftId/photo/sts"),
      StatusCodes.BadRequest,
      noPhotoToUpload
    )

    // передали урл, фото успешно сохраняется
    val vin = PhotoUtilsGenerator.vinGenerator.nextValue
    val orig = MdsPhotoData(AutoruOrigNamespaceSettings.namespace, imageHashGenerator.nextValue)
    val externalImageUrl = "http://example.com/skoda.jpg"
    val bodyPart = BodyPart("url", HttpEntity(externalImageUrl))
    val req2 = Post(s"/api/v1/draft/cars/$userRef/$draftId/photo/sts", Multipart.FormData(bodyPart))
    val res2 = checkSuccessProtoFromJsonRequest[StsPhotoUploadResponse](req2)
    assert(res2.getNamespace == AutoruOrigNamespaceSettings.namespace)
    assert(res2.getPhotoId == orig.toPlain)
    assert(!res2.hasSts)
    assert(!res2.hasLicensePlateNumber)
    assert(res2.getVin == vin)

    val readDraft = checkSuccessOfferRequest(Get(s"/api/v1/draft/cars/$extUserId/$draftId/proto"))
    assert(readDraft.getOfferAutoru.getModerationPhotoCount == 1)
    assert(readDraft.getOfferAutoru.getModerationPhoto(0).getName == orig.name)
    assert(readDraft.getOfferAutoru.getModerationPhoto(0).getNamespace == AutoruOrigNamespaceSettings.namespace)
    assert(readDraft.getOfferAutoru.getModerationPhoto(0).getExternalUrl == externalImageUrl)

    // добавляем еще фотку с того же урла - должна вернуться старая фотка, но вин распознается заново
    val vin3 = PhotoUtilsGenerator.vinGenerator.nextValue
    val res3 = checkSuccessProtoFromJsonRequest[StsPhotoUploadResponse](req2)
    assert(res3.getNamespace == AutoruOrigNamespaceSettings.namespace)
    assert(res3.getPhotoId == orig.toPlain)
    assert(!res3.hasSts)
    assert(!res3.hasLicensePlateNumber)
    assert(res3.getVin == vin3)

    val readDraft2 = checkSuccessOfferRequest(Get(s"/api/v1/draft/cars/$extUserId/$draftId/proto"))
    assert(readDraft2.getOfferAutoru.getModerationPhotoCount == 1)
    assert(readDraft2.getOfferAutoru.getModerationPhoto(0).getName == orig.name)
    assert(readDraft2.getOfferAutoru.getModerationPhoto(0).getNamespace == AutoruOrigNamespaceSettings.namespace)
    assert(readDraft2.getOfferAutoru.getModerationPhoto(0).getExternalUrl == externalImageUrl)
  }

  test("add sts photo from data") {
    val userId: Long = carTestForms.randomUser.id
    val extUserId: String = ExternalAutoruUserRef.privateRef(userId)

    // создаем черновик
    val req1 = createRequest(Post(s"/api/v1/draft/cars/$extUserId")) { builder =>
      builder.setCarInfo(CarsModel.CarInfo.newBuilder().setArmored(true))
    }
    val draftId = checkSuccessRequest(req1)
    val readForm = checkSuccessReadRequest(Get(s"/api/v1/draft/cars/$extUserId/$draftId"))
    val userRef = readForm.getUserRef

    // не передали файл
    checkErrorRequest(
      Post(s"/api/v1/draft/cars/$extUserId/$draftId/photo/sts"),
      StatusCodes.BadRequest,
      noPhotoToUpload
    )

    // передали данные загруженной картинки, фото успешно сохраняется
    val vin = PhotoUtilsGenerator.vinGenerator.nextValue
    val orig = MdsPhotoData(AutoruOrigNamespaceSettings.namespace, imageHashGenerator.generateValue)
    val bodyPart =
      BodyPart("data", HttpEntity(s"""{"namespace":"autoru-orig", "groupId":${orig.groupId}, "name":"${orig.id}"}"""))
    val req2 = Post(s"/api/v1/draft/cars/$userRef/$draftId/photo/sts", Multipart.FormData(bodyPart))
    val res2 = checkSuccessProtoFromJsonRequest[StsPhotoUploadResponse](req2)
    assert(res2.getNamespace == AutoruOrigNamespaceSettings.namespace)
    assert(res2.getPhotoId == orig.toPlain)
    assert(!res2.hasSts)
    assert(!res2.hasLicensePlateNumber)
    assert(res2.getVin == vin)

    val readDraft = checkSuccessOfferRequest(Get(s"/api/v1/draft/cars/$extUserId/$draftId/proto"))
    assert(readDraft.getOfferAutoru.getModerationPhotoCount == 1)
    assert(readDraft.getOfferAutoru.getModerationPhoto(0).getName == orig.name)
    assert(readDraft.getOfferAutoru.getModerationPhoto(0).getNamespace == AutoruOrigNamespaceSettings.namespace)

    // снова эти же данные передаем - должна вернуться старая фотка, но вин распознается заново
    val vin3 = PhotoUtilsGenerator.vinGenerator.nextValue
    val res3 = checkSuccessProtoFromJsonRequest[StsPhotoUploadResponse](req2)
    assert(res3.getNamespace == AutoruOrigNamespaceSettings.namespace)
    assert(res3.getPhotoId == orig.toPlain)
    assert(!res3.hasSts)
    assert(!res3.hasLicensePlateNumber)
    assert(res3.getVin == vin3)

    val readDraft2 = checkSuccessOfferRequest(Get(s"/api/v1/draft/cars/$extUserId/$draftId/proto"))
    assert(readDraft2.getOfferAutoru.getModerationPhotoCount == 1)
    assert(readDraft2.getOfferAutoru.getModerationPhoto(0).getName == orig.name)
    assert(readDraft2.getOfferAutoru.getModerationPhoto(0).getNamespace == AutoruOrigNamespaceSettings.namespace)
  }

  test("add sts photo from file") {
    val userId: Long = carTestForms.randomUser.id
    val extUserId: String = ExternalAutoruUserRef.privateRef(userId)

    // создаем черновик
    val req1 = createRequest(Post(s"/api/v1/draft/cars/$extUserId")) { builder =>
      builder.setCarInfo(CarsModel.CarInfo.newBuilder().setArmored(true))
    }
    val draftId = checkSuccessRequest(req1)
    val readForm = checkSuccessReadRequest(Get(s"/api/v1/draft/cars/$extUserId/$draftId"))
    val userRef = readForm.getUserRef

    // не передали файл
    checkErrorRequest(
      Post(s"/api/v1/draft/cars/$extUserId/$draftId/photo/sts"),
      StatusCodes.BadRequest,
      noPhotoToUpload
    )

    // передали файл, фото успешно сохраняется
    val vin = PhotoUtilsGenerator.vinGenerator.nextValue
    val orig = MdsPhotoData(AutoruOrigNamespaceSettings.namespace, imageHashGenerator.nextValue)
    val httpEntity = HttpEntity(ContentTypes.`application/octet-stream`, Array[Byte](1, 2, 3, 4, 5))
    val bodyPart = BodyPart("file", httpEntity)
    val req2 = Post(s"/api/v1/draft/cars/$userRef/$draftId/photo/sts", Multipart.FormData(bodyPart))
    val res2 = checkSuccessProtoFromJsonRequest[StsPhotoUploadResponse](req2)
    assert(res2.getNamespace == AutoruOrigNamespaceSettings.namespace)
    assert(res2.getPhotoId == orig.toPlain)
    assert(!res2.hasSts)
    assert(!res2.hasLicensePlateNumber)
    assert(res2.getVin == vin)

    val readDraft = checkSuccessOfferRequest(Get(s"/api/v1/draft/cars/$extUserId/$draftId/proto"))
    assert(readDraft.getOfferAutoru.getModerationPhotoCount == 1)
    assert(readDraft.getOfferAutoru.getModerationPhoto(0).getName == orig.name)
    assert(readDraft.getOfferAutoru.getModerationPhoto(0).getNamespace == AutoruOrigNamespaceSettings.namespace)
  }

  test("add photo") {
    val userId: Long = carTestForms.randomUser.id
    val extUserId: String = ExternalAutoruUserRef.privateRef(userId)

    // создаем черновик
    val req1 = createRequest(Post(s"/api/v1/draft/cars/$extUserId")) { builder =>
      builder.setCarInfo(CarsModel.CarInfo.newBuilder().setArmored(true))
    }
    val draftId = checkSuccessRequest(req1)
    val readForm = checkSuccessReadRequest(Get(s"/api/v1/draft/cars/$extUserId/$draftId"))
    val userRef = readForm.getUserRef

    // не передали файл
    checkErrorRequest(Post(s"/api/v1/draft/cars/$extUserId/$draftId/photo"), StatusCodes.BadRequest, noPhotoToUpload)

    // передали файл, фото успешно сохраняется
    val orig = MdsPhotoData(AutoruOrigNamespaceSettings.namespace, imageHashGenerator.nextValue)
    val blurred = MdsPhotoData(AutoruVosNamespaceSettings.namespace, nextHashForPrefix(orig))
    val httpEntity = HttpEntity(ContentTypes.`application/octet-stream`, Array[Byte](1, 2, 3, 4, 5))
    val bodyPart = BodyPart("file", httpEntity)
    val req2 = Post(s"/api/v1/draft/cars/$userRef/$draftId/photo", Multipart.FormData(bodyPart))
    val res2 = checkSuccessProtoFromJsonRequest[PhotoSaveSuccessResponse](req2)
    checkBlurPhotoResponse(res2, blurred, angle = 0, blur = true)

    // Draft not found
    checkErrorRequest(
      Put(s"/api/v1/draft/cars/$userRef/100500-aaaa/photo/${orig.toPlain}/rotate/cw"),
      StatusCodes.NotFound,
      DraftNotFoundError
    )

    // поворот на 90 градусов
    val rotated90 = MdsPhotoData(AutoruVosNamespaceSettings.namespace, nextHashForPrefix(blurred))
    val req3 = Put(s"/api/v1/draft/cars/$userRef/$draftId/photo/${orig.toPlain}/rotate/cw")
    val res3 = checkSuccessProtoFromJsonRequest[PhotoSaveSuccessResponse](req3)
    checkBlurPhotoResponse(res3, rotated90, angle = 90, blur = true)

    // Photo Not Found
    checkErrorRequest(
      Put(s"/api/v1/draft/cars/$userRef/$draftId/photo/abcdefgh/rotate/cw"),
      StatusCodes.NotFound,
      unknownPhoto
    )

    // поворот еще на 90 градусов
    val rotated180 = MdsPhotoData(AutoruVosNamespaceSettings.namespace, nextHashForPrefix(rotated90))
    val req4 = Put(s"/api/v1/draft/cars/$userRef/$draftId/photo/${orig.toPlain}/rotate/cw")
    val res4 = checkSuccessProtoFromJsonRequest[PhotoSaveSuccessResponse](req4)
    checkBlurPhotoResponse(res4, rotated180, angle = 180, blur = true)

    // замазываем фото
    //  val blurOrig = imageHashGenerator.nextValue
    // val blurRotated180 = imageHashGenerator.nextValue
    val req5 = Put(s"/api/v1/draft/cars/$userRef/$draftId/photo/${orig.toPlain}/blur")
    val res5 = checkSuccessProtoFromJsonRequest[PhotoSaveSuccessResponse](req5)
    checkBlurPhotoResponse(res5, rotated180, angle = 180, blur = true)
    checkOffer(
      userRef,
      draftId,
      1,
      rotated180,
      orig,
      (180, true),
      (orig, 0, false),
      (blurred, 0, true),
      (rotated90, 90, true),
      (rotated180, 180, true)
    )

    // вертим обратно
    val req6 = Put(s"/api/v1/draft/cars/$userRef/$draftId/photo/${orig.toPlain}/rotate/ccw")
    val res6 = checkSuccessProtoFromJsonRequest[PhotoSaveSuccessResponse](req6)
    checkBlurPhotoResponse(res6, rotated90, angle = 90, blur = true)
    checkOffer(
      userRef,
      draftId,
      1,
      rotated90,
      orig,
      (90, true),
      (orig, 0, false),
      (blurred, 0, true),
      (rotated90, 90, true),
      (rotated180, 180, true)
    )

    // размазываем
    val req7 = Put(s"/api/v1/draft/cars/$userRef/$draftId/photo/${orig.toPlain}/blur/undo")
    val notBlurredRotated90 = MdsPhotoData(AutoruVosNamespaceSettings.namespace, nextHashForPrefix(rotated180))
    val res7 = checkSuccessProtoFromJsonRequest[PhotoSaveSuccessResponse](req7)
    checkBlurPhotoResponse(res7, notBlurredRotated90, angle = 90, blur = false)
    checkOffer(
      userRef,
      draftId,
      1,
      notBlurredRotated90,
      orig,
      (90, false),
      (orig, 0, false),
      (blurred, 0, true),
      (rotated90, 90, true),
      (rotated180, 180, true),
      (notBlurredRotated90, 90, false)
    )

    // восстанавливаем оригинал
    val req8 = Put(s"/api/v1/draft/cars/$userRef/$draftId/photo/${orig.toPlain}/restore")
    val restoredOrig = MdsPhotoData(AutoruVosNamespaceSettings.namespace, nextHashForPrefix(notBlurredRotated90))
    val res8 = checkSuccessProtoFromJsonRequest[PhotoSaveSuccessResponse](req8)
    checkBlurPhotoResponse(res8, restoredOrig, angle = 0, blur = false)
    checkOffer(
      userRef,
      draftId,
      1,
      restoredOrig,
      orig,
      (0, false),
      (orig, 0, false),
      (blurred, 0, true),
      (rotated90, 90, true),
      (rotated180, 180, true),
      (notBlurredRotated90, 90, false),
      (restoredOrig, 0, false)
    )

    // и удаляем
    val req9 = Delete(s"/api/v1/draft/cars/$userRef/$draftId/photo/${orig.toPlain}")
    checkSimpleSuccessRequest(req9)
    val offer = checkSuccessOfferRequest(Get(s"/api/v1/draft/cars/$userRef/$draftId/proto"))
    assert(offer.getOfferAutoru.getPhotoList.asScala.count(!_.getDeleted) == 0)
    val form = checkSuccessReadRequest(Get(s"/api/v1/draft/cars/$userRef/$draftId"))
    assert(form.getState.getImageUrlsCount == 1)
    assert(form.getState.getImageUrlsList.asScala.exists(_.getIsDeleted))

    // Tried to update a real offer, not a draft!
    // сохраняем полноценное объявление
    val fullForm = carTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(userId))).form
    checkSuccessRequest(createRequest(Put(s"/api/v1/draft/cars/$userRef/$draftId")) { builder =>
      builder.mergeFrom(fullForm)
    })
    // пишем его в старую базу и заодно оно пишется в vos
    val offerId = checkSuccessRequest(Post(s"/api/v1/draft/cars/$draftId/publish/$extUserId"))
    // и пытаемся обновить. Пока что нельзя
    val req10 = Post(s"/api/v1/draft/cars/$extUserId/$offerId/photo", Multipart.FormData(bodyPart))
    checkErrorRequest(req10, StatusCodes.NotFound, DraftNotFoundError)
  }

  test("add photo batch") {
    val userId: Long = carTestForms.randomUser.id
    val extUserId: String = ExternalAutoruUserRef.privateRef(userId)

    // создаем черновик
    val req1 = createRequest(Post(s"/api/v1/draft/cars/$extUserId")) { builder =>
      builder.setCarInfo(CarsModel.CarInfo.newBuilder().setArmored(true))
    }
    val draftId = checkSuccessRequest(req1)
    val readForm = checkSuccessReadRequest(Get(s"/api/v1/draft/cars/$extUserId/$draftId"))
    val userRef = readForm.getUserRef

    // не передали файл
    checkErrorRequest(
      Post(s"/api/v1/draft/cars/$extUserId/$draftId/photo/from-url-list"),
      StatusCodes.BadRequest,
      noPhotoToUpload
    )

    // передали файл, фото успешно сохраняется
    val req2 = Post(
      s"/api/v1/draft/cars/$userRef/$draftId/photo/from-url-list",
      Multipart.FormData(
        BodyPart("urls", HttpEntity(ContentTypes.`text/plain(UTF-8)`, "http://example.com/skoda.jpg"))
      )
    )

    checkSuccessProtoFromJsonRequest[PhotoSaveSuccessResponse](req2)

    val readFormUpdated = checkSuccessReadRequest(Get(s"/api/v1/draft/cars/$extUserId/$draftId"))
    assert(readForm.getState.getImageUrlsCount == 0)
    assert(readFormUpdated.getState.getImageUrlsCount == 1)
  }

  test("do not lose photo transformations on draft move and publication") {
    val userRef = "user:1"
    // создаем черновик
    val req1 = createRequest(Post(s"/api/v1/draft/cars/$userRef")) { builder =>
      builder.setCarInfo(CarsModel.CarInfo.newBuilder().setArmored(true))
    }
    val draftId = checkSuccessRequest(req1)

    // передали файл, фото успешно сохраняется
    val orig = MdsPhotoData(AutoruOrigNamespaceSettings.namespace, imageHashGenerator.nextValue)
    val blurred = MdsPhotoData(AutoruVosNamespaceSettings.namespace, nextHashForPrefix(orig))
    val httpEntity = HttpEntity(ContentTypes.`application/octet-stream`, Array[Byte](1, 2, 3, 4, 5))
    val bodyPart = BodyPart("file", httpEntity)
    val req2 = Post(s"/api/v1/draft/cars/$userRef/$draftId/photo", Multipart.FormData(bodyPart))
    val res2 = checkSuccessProtoFromJsonRequest[PhotoSaveSuccessResponse](req2)
    checkBlurPhotoResponse(res2, blurred, angle = 0, blur = true)

    // поворот на 90 градусов
    val rotated90 = MdsPhotoData(AutoruVosNamespaceSettings.namespace, nextHashForPrefix(blurred))
    val req3 = Put(s"/api/v1/draft/cars/$userRef/$draftId/photo/${orig.toPlain}/rotate/cw")
    val res3 = checkSuccessProtoFromJsonRequest[PhotoSaveSuccessResponse](req3)
    checkBlurPhotoResponse(res3, rotated90, angle = 90, blur = true)

    // поворот еще на 90 градусов
    val rotated180 = MdsPhotoData(AutoruVosNamespaceSettings.namespace, nextHashForPrefix(rotated90))
    val req4 = Put(s"/api/v1/draft/cars/$userRef/$draftId/photo/${orig.toPlain}/rotate/cw")
    val res4 = checkSuccessProtoFromJsonRequest[PhotoSaveSuccessResponse](req4)
    checkBlurPhotoResponse(res4, rotated180, angle = 180, blur = true)

    // замазываем
    // val blurOrig = imageHashGenerator.nextValue
    // val blurRotated180 = imageHashGenerator.nextValue
    val req5 = Put(s"/api/v1/draft/cars/$userRef/$draftId/photo/${orig.toPlain}/blur")
    val res5 = checkSuccessProtoFromJsonRequest[PhotoSaveSuccessResponse](req5)
    checkBlurPhotoResponse(res5, rotated180, angle = 180, blur = true)
    checkOffer(
      userRef,
      draftId,
      1,
      rotated180,
      orig,
      (180, true),
      (orig, 0, false),
      (blurred, 0, true),
      (rotated90, 90, true),
      (rotated180, 180, true)
    )

    // перемещаем черновик
    val userId: Long = carTestForms.randomUser.id
    val extUserId: String = ExternalAutoruUserRef.privateRef(userId)

    val req6 = Post(s"/api/v1/draft/cars/$userRef/$draftId/move/$extUserId")
    val draftId2 = checkSuccessRequest(req6)

    checkOffer(
      extUserId,
      draftId2,
      1,
      rotated180,
      orig,
      (180, true),
      (orig, 0, false),
      (blurred, 0, true),
      (rotated90, 90, true),
      (rotated180, 180, true)
    )

    // сохраняем полноценное объявление
    val fullFormBuilder: Offer.Builder =
      carTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(userId))).form.toBuilder
    fullFormBuilder.getStateBuilder.addImageUrls(
      ApiPhoto
        .newBuilder()
        .setName(rotated180.name)
        .setNamespace(rotated180.namespace)
    )
    val fullForm = fullFormBuilder.build()
    checkSuccessRequest(createRequest(Put(s"/api/v1/draft/cars/$extUserId/$draftId2")) { builder =>
      builder.mergeFrom(fullForm)
    })

    // пишем его в старую базу и заодно оно пишется в vos
    val offerId = checkSuccessRequest(Post(s"/api/v1/draft/cars/$draftId2/publish/$extUserId"))

    // трансформации не потерялись при чтении из старой базы
    val offer1 = checkSuccessOfferRequest(Get(s"/api/v1/offer/cars/$offerId/proto"))
    assert(offer1.getOfferAutoru.getPhotoCount == fullForm.getState.getImageUrlsCount)
    val photo1 = PhotoUtils.findPhotoById(offer1.toBuilder, AUTORU, rotated180).value
    checkPhoto(
      photo1,
      rotated180,
      orig,
      (180, true),
      (orig, 0, false),
      (blurred, 0, true),
      (rotated90, 90, true),
      (rotated180, 180, true)
    )

    // трансформации не потерялись при чтении из vos
    val offer2 = checkSuccessOfferRequest(Get(s"/api/v1/offer/cars/$offerId/proto?fromvos=1"))
    assert(offer2.getOfferAutoru.getPhotoCount == fullForm.getState.getImageUrlsCount)
    val photo2 = PhotoUtils.findPhotoById(offer2.toBuilder, AUTORU, rotated180).value
    checkPhoto(
      photo2,
      rotated180,
      orig,
      (180, true),
      (orig, 0, false),
      (blurred, 0, true),
      (rotated90, 90, true),
      (rotated180, 180, true)
    )
  }

  test("photo add from url") {
    // создаем черновик
    val userId: Long = carTestForms.randomUser.id
    val extUserId: String = ExternalAutoruUserRef.privateRef(userId)
    val fullFormBuilder = carTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(userId))).form.toBuilder
    fullFormBuilder.getStateBuilder.clearImageUrls()
    val fullForm = fullFormBuilder.build()
    val draftId = checkSuccessRequest(createRequest(Post(s"/api/v1/draft/cars/$extUserId")) { builder =>
      builder.mergeFrom(fullForm)
    })

    val orig = imageHashGenerator.nextValue // consume original name
    val blurred = MdsPhotoData(AutoruVosNamespaceSettings.namespace, nextHashForPrefix(MdsPhotoData("", orig)))
    val httpEntity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, "http://yandex.ru")
    val bodyPart = BodyPart("url", httpEntity)
    val req2 = Post(s"/api/v1/draft/cars/$extUserId/$draftId/photo", Multipart.FormData(bodyPart))
    val res2 = checkSuccessProtoFromJsonRequest[PhotoSaveSuccessResponse](req2)
    checkBlurPhotoResponse(res2, blurred, angle = 0, blur = true)

    // проверяем что external url сохранился
    {
      val draftProto = checkSuccessOfferRequest(Get(s"/api/v1/draft/cars/$extUserId/$draftId/proto"))
      assert(draftProto.getOfferAutoru.getPhoto(0).getExternalUrl == "http://yandex.ru")
    }

    // апдейтим и проверяем что external_url на месте
    {
      val draft = checkSuccessReadRequest(Get(s"/api/v1/draft/cars/$extUserId/$draftId"))
      checkSuccessRequest(createRequest(Put(s"/api/v1/draft/cars/$extUserId/$draftId")) { builder =>
        builder.mergeFrom(draft).getStateBuilder.setMileage(56001)
      })
      val draftProto = checkSuccessOfferRequest(Get(s"/api/v1/draft/cars/$extUserId/$draftId/proto"))
      assert(draftProto.getOfferAutoru.getPhoto(0).getExternalUrl == "http://yandex.ru")
    }

    // грузим еще раз эту же картинку - новой загрузки быть не должно, вернем существующую фотку
    val res3 = checkSuccessProtoFromJsonRequest[PhotoSaveSuccessResponse](req2)
    checkBlurPhotoResponse(res3, blurred, angle = 0, blur = true)

    // если не удалось загрузить картинку
    val req4 = Post(
      s"/api/v1/draft/cars/$extUserId/$draftId/photo",
      Multipart.FormData(
        BodyPart("url", HttpEntity(ContentTypes.`text/plain(UTF-8)`, "http://yandex.com"))
      )
    )
    PhotoUtilsGenerator.urlDownloaderThrowError.set(true)
    checkErrorRequest(req4, StatusCodes.BadRequest, invalidExternalImage)
    PhotoUtilsGenerator.urlDownloaderThrowError.set(false)

    // публикуем объявление
    val offerId = checkSuccessRequest(Post(s"/api/v1/draft/cars/$draftId/publish/$extUserId?insert_new=1"))

    // проверяем, что external url сохранился в объявлении
    {
      val offer = checkSuccessOfferRequest(Get(s"/api/v1/offer/cars/$offerId/proto"))
      assert(offer.getOfferAutoru.getPhoto(0).getExternalUrl == "http://yandex.ru")
    }

    // проверим, что external url не теряется при редактировании
    {
      // делаем черновик из объявления
      val draftId = checkSuccessRequest(Post(s"/api/v1/offer/cars/$extUserId/$offerId/draft"))
      // читаем
      val draft = checkSuccessReadRequest(Get(s"/api/v1/draft/cars/$extUserId/$draftId"))
      // немножко апдейтим
      checkSuccessRequest(createRequest(Put(s"/api/v1/draft/cars/$extUserId/$draftId")) { builder =>
        builder.mergeFrom(draft).getStateBuilder.setMileage(56001)
      })
      // публикуем
      val offerId2 = checkSuccessRequest(Post(s"/api/v1/draft/cars/$draftId/publish/$extUserId"))
      assert(offerId2 == offerId)
      // проверяем, что external url не потерялся
      val offer = checkSuccessOfferRequest(Get(s"/api/v1/offer/cars/$offerId2/proto"))
      assert(offer.getOfferAutoru.getPhoto(0).getExternalUrl == "http://yandex.ru")
    }
  }

  test("photo add from data") {
    val userRef = "user:1"
    // создаем черновик
    val req1 = createRequest(Post("/api/v1/draft/cars/" + userRef)) { builder =>
      builder.setCarInfo(CarsModel.CarInfo.newBuilder().setArmored(true))
    }
    val draftId = checkSuccessRequest(req1)

    // проверим ошибки
    val req2 = Post(
      s"/api/v1/draft/cars/$userRef/$draftId/photo",
      Multipart.FormData(
        BodyPart("data", HttpEntity(ContentTypes.`application/json`, """fgsfds"""))
      )
    )
    checkErrorRequest(
      req2,
      StatusCodes.BadRequest,
      invalidUploaderData(
        """JsonParseException: Unrecognized token 'fgsfds': was expecting 'null', 'true', 'false' or NaN
          | at [Source: (byte[])"fgsfds"; line: 1, column: 13]""".stripMargin
      )
    )

    val req3 = Post(
      s"/api/v1/draft/cars/$userRef/$draftId/photo",
      Multipart.FormData(
        BodyPart("data", HttpEntity(ContentTypes.`application/json`, """{"pewpew":1}"""))
      )
    )
    checkErrorRequest(
      req3,
      StatusCodes.BadRequest,
      invalidUploaderData(
        "JsResultException: JsResultException(errors:List((/namespace,List(" +
          "JsonValidationError(List(error.path.missing),List())))," +
          " (/name,List(JsonValidationError(List(error.path.missing),List())))," +
          " (/groupId,List(JsonValidationError(List(error.path.missing),List())))))"
      )
    )

    // успешно сохраним
    val requestPhotoId = imageHashGenerator.generateValue
    val groupId = requestPhotoId.split("-")(0).toInt
    val name = requestPhotoId.split("-")(1)
    val data = s"""{"namespace": "autoru-all", "groupId": $groupId, "name": "$name"}"""
    val req4 = Post(
      s"/api/v1/draft/cars/$userRef/$draftId/photo",
      Multipart.FormData(
        BodyPart("data", HttpEntity(ContentTypes.`application/json`, data))
      )
    )
    val origNamespaceId = nextHashForPrefix(MdsPhotoData("", requestPhotoId))
    val vosNamespaceId = nextHashForPrefix(MdsPhotoData("", origNamespaceId))
    val blurred = MdsPhotoData(AutoruVosNamespaceSettings.namespace, vosNamespaceId)
    val res4 = checkSuccessProtoFromJsonRequest[PhotoSaveSuccessResponse](req4)
    checkBlurPhotoResponse(res4, blurred, angle = 0, blur = true)

    checkOffer(
      userRef,
      draftId,
      1,
      blurred,
      MdsPhotoData(AutoruOrigNamespaceSettings.namespace, origNamespaceId),
      (0, true),
      (MdsPhotoData(AutoruOrigNamespaceSettings.namespace, origNamespaceId), 0, false),
      (MdsPhotoData(AutoruVosNamespaceSettings.namespace, vosNamespaceId), 0, true)
    )
  }

  test("add and delete photo") {
    val userId: Long = carTestForms.randomUser.id
    val extUserId: String = ExternalAutoruUserRef.privateRef(userId)
    // создаем объявление
    val formInfo = carTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(userId)))
    val req = Post(s"/api/v1/offers/cars/$extUserId?insert_new=1")
      .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo.json))
    val offerId = withClue(formInfo.json) {
      checkSuccessRequest(req)
    }
    // делаем из него черновик
    val draftId = checkSuccessRequest(Post(s"/api/v1/offer/cars/$extUserId/$offerId/draft"))
    // добавляем в него фото
    val orig = MdsPhotoData(AutoruOrigNamespaceSettings.namespace, imageHashGenerator.generateValue)
    val groupId = orig.name.split("-")(0).toInt
    val name = orig.name.split("-")(1)
    val data = s"""{"namespace": "autoru-orig", "groupId": $groupId, "name": "$name"}"""
    checkSimpleSuccessRequest(
      Post(
        s"/api/v1/draft/cars/$extUserId/$draftId/photo",
        Multipart.FormData(
          BodyPart("data", HttpEntity(ContentTypes.`application/json`, data))
        )
      )
    )
    // удаляем фотку
    val deleteReq = Delete(s"/api/v1/draft/cars/$extUserId/$draftId/photo/${orig.toPlain}")
    checkSimpleSuccessRequest(deleteReq)
    // обновляем объявление из черновика
    checkSuccessRequest(Post(s"/api/v1/draft/cars/$draftId/publish/$extUserId"))
    // читаем объявление
    val offer = checkSuccessOfferRequest(Get(s"/api/v1/offer/cars/$offerId/proto"))
    // в объявлении есть удаленная фотография
    assert(offer.getOfferAutoru.getPhotoList.asScala.exists(_.getDeleted))
  }

  test("add and update photo during offer editing") {
    val userId: Long = carTestForms.randomUser.id
    val extUserId: String = ExternalAutoruUserRef.privateRef(userId)
    // создаем объявление
    val formInfo = carTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(userId)))
    val req = Post(s"/api/v1/offers/cars/$extUserId?insert_new=1")
      .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo.json))
    val offerId = withClue(formInfo.json) {
      checkSuccessRequest(req)
    }
    // делаем из него черновик
    val draftId = checkSuccessRequest(Post(s"/api/v1/offer/cars/$extUserId/$offerId/draft"))
    // добавляем в него фото
    val orig = MdsPhotoData(AutoruOrigNamespaceSettings.namespace, imageHashGenerator.generateValue)
    val blurred = MdsPhotoData(AutoruVosNamespaceSettings.namespace, nextHashForPrefix(orig))
    val groupId = orig.name.split("-")(0).toInt
    val name = orig.name.split("-")(1)
    val data = s"""{"namespace": "autoru-orig", "groupId": $groupId, "name": "$name"}"""
    checkSimpleSuccessRequest(
      Post(
        s"/api/v1/draft/cars/$extUserId/$draftId/photo",
        Multipart.FormData(
          BodyPart("data", HttpEntity(ContentTypes.`application/json`, data))
        )
      )
    )
    // вертим фотку
    val rotated90 = MdsPhotoData(AutoruVosNamespaceSettings.namespace, nextHashForPrefix(blurred))
    checkSimpleSuccessRequest(Put(s"/api/v1/draft/cars/$extUserId/$draftId/photo/${orig.toPlain}/rotate/cw"))
    // обновляем объявление из черновика
    checkSuccessRequest(Post(s"/api/v1/draft/cars/$draftId/publish/$extUserId"))
    // читаем объявление
    val offer = checkSuccessOfferRequest(Get(s"/api/v1/offer/cars/$offerId/proto"))
    // в объявлении есть залитая повернутая фотка
    val photo = PhotoUtils.findPhotoById(offer.toBuilder, AUTORU, rotated90).value
    // и в ней есть все изменения
    checkPhoto(photo, rotated90, orig, (90, true), (orig, 0, false), (blurred, 0, true), (rotated90, 90, true))

    // снова делаем черновик
    val draftId2 = checkSuccessRequest(Post(s"/api/v1/offer/cars/$extUserId/$offerId/draft"))
    // еще вертим фотку в черновике
    val rotated180 = MdsPhotoData(AutoruVosNamespaceSettings.namespace, nextHashForPrefix(rotated90))
    checkSimpleSuccessRequest(Put(s"/api/v1/draft/cars/$extUserId/$draftId2/photo/${orig.toPlain}/rotate/cw"))
    // обновляем объявление из черновика
    checkSuccessRequest(Post(s"/api/v1/draft/cars/$draftId2/publish/$extUserId"))
    // читаем объявление
    val offer2 = checkSuccessOfferRequest(Get(s"/api/v1/offer/cars/$offerId/proto"))
    // ничего не потерялось
    val photo2 = PhotoUtils.findPhotoById(offer2.toBuilder, AUTORU, rotated180).value
    checkPhoto(
      photo2,
      rotated180,
      orig,
      (180, true),
      (orig, 0, false),
      (blurred, 0, true),
      (rotated90, 90, true),
      (rotated180, 180, true)
    )
  }

  test("adding photo to moved draft") {
    val anonRef = "anon:a"
    // создаем черновик
    val req1 = createRequest(Post(s"/api/v1/draft/cars/$anonRef")) {
      _.setCarInfo(CarsModel.CarInfo.newBuilder().setArmored(true))
    }
    val draftId = checkSuccessRequest(req1)
    // перемещаем его
    val userId: Long = carTestForms.randomUser.id
    val extUserId: String = ExternalAutoruUserRef.privateRef(userId)

    val req2 = Post(s"/api/v1/draft/cars/$anonRef/$draftId/move/$extUserId")
    val draftId2 = checkSuccessRequest(req2)
    assert(draftId2 != draftId)

    // предыдущий вариант черновика удален
    checkErrorRequest(Get(s"/api/v1/draft/cars/$anonRef/$draftId"), StatusCodes.NotFound, DraftNotFoundError)

    def checkAddPhoto(url: String)(implicit pos: source.Position): Unit = {
      val requestPhoto = MdsPhotoData(AutoruOrigNamespaceSettings.namespace, imageHashGenerator.generateValue)
      val origNamespacePhoto = MdsPhotoData(AutoruOrigNamespaceSettings.namespace, nextHashForPrefix(requestPhoto))
      val blurred = nextHashForPrefix(origNamespacePhoto)
      val groupId = requestPhoto.name.split("-")(0).toInt
      val name = requestPhoto.name.split("-")(1)
      val data = s"""{"namespace": "autoru-vos", "groupId": $groupId, "name": "$name"}"""
      val res = checkSuccessProtoFromJsonRequest[PhotoSaveSuccessResponse](
        Post(
          url,
          Multipart.FormData(
            BodyPart("data", HttpEntity(ContentTypes.`application/json`, data))
          )
        )
      )
      assert(res.getPhotoId == s"autoru-vos:$blurred")
      assert(res.getStatus == ResponseStatus.SUCCESS)
    }

    // пробуем добавить фото в старый удаленный черновик по старому userId
    checkAddPhoto(s"/api/v1/draft/cars/$anonRef/$draftId/photo")

    // пробуем добавить фото в старый удаленный черновик по новому userId
//    checkAddPhoto(s"/api/v1/draft/cars/$extUserId/$draftId/photo")
  }

  private def checkBlurPhotoResponse(
      res: PhotoSaveSuccessResponse,
      photoId: MdsPhotoData,
      angle: Int,
      blur: Boolean
  )(implicit pos: source.Position): Unit = {
    assert(res.getPhotoId == photoId.toPlain)
    assert(res.getPhoto.getTransform.getAngle == angle)
    assert(res.getPhoto.getTransform.getBlur == blur)
    res.getPhoto.getSizesMap
    assert(res.getPhoto.getSizesMap.size() == AutoruVosNamespaceSettings.photoSizes.length + 1)
  }

  private def checkOffer(
      userRef: String,
      draftId: OfferID,
      photoCount: Int,
      name: MdsPhotoData,
      origName: MdsPhotoData,
      curTransform: (Int, Boolean),
      transforms: (MdsPhotoData, Int, Boolean)*
  )(implicit pos: source.Position): Unit = {
    val offer = checkSuccessOfferRequest(Get(s"/api/v1/draft/cars/$userRef/$draftId/proto"))
    assert(offer.getOfferAutoru.getPhotoCount == 1)
    val photo: Photo = offer.getOfferAutoru.getPhoto(0)
    checkPhoto(photo, name, origName, curTransform, transforms: _*)
  }

  private def checkPhoto(
      photo: PhotoOrBuilder,
      id: MdsPhotoData,
      origId: MdsPhotoData,
      curTransform: (Int, Boolean),
      transforms: (MdsPhotoData, Int, Boolean)*
  )(implicit pos: source.Position): Unit = {
    assert(photo.getId == id, "id is wrong")
    assert(photo.getOrigId == origId, "origId is wrong")
    assert(photo.getCurrentTransform.getAngle == curTransform._1, "current angle is wrong")
    assert(photo.getCurrentTransform.getBlur == curTransform._2, "current blur is wrong")
    assert(photo.getTransformHistoryCount == transforms.length, "transform length is wrong")
    transforms.zipWithIndex.foreach {
      case ((transformId, tAngle, tBlur), idx) =>
        assert(photo.getTransformHistory(idx).getId == transformId, s"$idx transform id is wrong")
        assert(photo.getTransformHistory(idx).getTransform.getAngle == tAngle, s"$idx transform angle is wrong")
        assert(photo.getTransformHistory(idx).getTransform.getBlur == tBlur, s"$idx transform blur is wrong")
    }
  }

  private def nextHashForPrefix(oldPhotoId: MdsPhotoData): String = {
    PhotoUtilsGenerator.replaceNamePrefix(imageHashGenerator.nextValue, oldPhotoId.prefix)
  }
}
