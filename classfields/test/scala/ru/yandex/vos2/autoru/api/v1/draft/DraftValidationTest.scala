package ru.yandex.vos2.autoru.api.v1.draft

import akka.http.scaladsl.model.HttpRequest
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.OptionValues
import org.scalatest.funsuite.AnyFunSuiteLike
import ru.auto.api.ApiOfferModel.{Availability, Documents}
import ru.auto.api.CarsModel.CarInfo
import ru.auto.api.CommonModel
import ru.auto.api.CommonModel.{PriceInfo, SteeringWheel}
import ru.auto.api.MotoModel.MotoInfo
import ru.auto.api.TrucksModel.TruckCategory._
import ru.auto.api.TrucksModel.{TruckCategory, TruckInfo}
import ru.yandex.vos2.autoru.utils.Vos2ApiHandlerResponses._
import ru.yandex.vos2.autoru.utils.testforms.{FormInfo, TestFormParams}
import ru.yandex.vos2.autoru.utils.validators.ValidationErrors._
import ru.yandex.vos2.autoru.utils.{ValidationErrorResponse, Vos2ApiSuite}
import ru.yandex.vos2.util.RandomUtil

/**
  * Created by andrey on 8/31/17.
  */
@RunWith(classOf[JUnitRunner])
class DraftValidationTest extends AnyFunSuiteLike with Vos2ApiSuite with OptionValues {

  private val privateCarValidationError: ValidationErrorResponse = validationError(
    NotexistSection,
    NotexistPrice,
    NotexistOwners,
    NotexistPts,
    NotexistProductionYear,
    ForbiddenPrivateNew,
    NotexistColor,
    NotexistBodyType,
    NotexistEngineType,
    NotexistTransmission,
    NotexistDrive,
    NotexistMark,
    NotexistModel,
    UnknownModification,
    NotexistPhone,
    NotexistSellerEmail,
    NotexistGeoId,
    NotexistGeneration
  )

  private val salonCarValidationError: ValidationErrorResponse = validationError(
    NotexistSection,
    NotexistPrice,
    NotexistOwners,
    NotexistPts,
    NotexistProductionYear,
    NotexistAvailability,
    NotexistColor,
    NotexistBodyType,
    NotexistEngineType,
    NotexistTransmission,
    NotexistDrive,
    NotexistMark,
    NotexistModel,
    UnknownModification,
    NotexistGeneration
  )

  private val privateTruckValidationError: ValidationErrorResponse = validationError(
    NotexistSection,
    NotexistPrice,
    NotexistOwners,
    NotexistPts,
    NotexistProductionYear,
    ForbiddenPrivateNew,
    NotexistColor,
    NotexistMark,
    NotexistModel,
    NotexistTruckCategory,
    NotexistPhone,
    NotexistSellerEmail,
    NotexistGeoId
  )

  private val salonTruckValidationError: ValidationErrorResponse = validationError(
    NotexistSection,
    NotexistPrice,
    NotexistOwners,
    NotexistPts,
    NotexistProductionYear,
    NotexistAvailability,
    NotexistColor,
    NotexistMark,
    NotexistModel,
    NotexistTruckCategory
  )

  private val privateMotoValidationError: ValidationErrorResponse = validationError(
    NotexistSection,
    NotexistPrice,
    NotexistOwners,
    NotexistPts,
    NotexistProductionYear,
    ForbiddenPrivateNew,
    NotexistColor,
    NotexistMark,
    NotexistModel,
    NotexistMotoCategory,
    NotexistPhone,
    NotexistSellerEmail,
    NotexistGeoId,
    NotexistEngineVolume
  )

  private val salonMotoValidationError: ValidationErrorResponse = validationError(
    NotexistSection,
    NotexistPrice,
    NotexistOwners,
    NotexistPts,
    NotexistProductionYear,
    NotexistAvailability,
    NotexistColor,
    NotexistMark,
    NotexistModel,
    NotexistMotoCategory,
    NotexistEngineVolume
  )

  test("unknown currency") {
    // передали кривую валюту
    val req = createRequest(Post("/api/v1/draft/cars/user:1")) { builder =>
      builder.setPriceInfo(PriceInfo.newBuilder().setCurrency("XYZ"))
    }
    checkValidationErrorRequest(req, validationError(WrongPriceCur))
  }

  test("wrong purchase date") {
    // передали кривую дату покупки
    val req = createRequest(Post("/api/v1/draft/trucks/user:1")) { builder =>
      builder.setDocuments(
        Documents.newBuilder().setPurchaseDate(CommonModel.Date.newBuilder().setYear(-1).setMonth(13))
      )
    }
    checkValidationErrorRequest(req, validationError(WrongPurchaseDateYear, WrongPurchaseDateMonth))

    // кивой день покупки
    val req2 = createRequest(Post("/api/v1/draft/cars/user:1")) { builder =>
      builder.setDocuments(
        Documents.newBuilder().setPurchaseDate(CommonModel.Date.newBuilder().setYear(-1).setMonth(2).setDay(35))
      )
    }
    checkValidationErrorRequest(req2, validationError(WrongPurchaseDateYear, WrongPurchaseDateDay(28)))
  }

  test("wrong warranty expire date") {
    // передали кривую дату окончания гарантии
    val req = createRequest(Post("/api/v1/draft/moto/anon:1")) { builder =>
      builder.setDocuments(
        Documents.newBuilder().setWarrantyExpire(CommonModel.Date.newBuilder().setYear(-1).setMonth(13))
      )
    }
    checkValidationErrorRequest(req, validationError(WrongWarrantyExpireYear, WrongWarrantyExpireMonth))

    // кивой день покупки
    val req2 = createRequest(Post("/api/v1/draft/cars/user:1")) { builder =>
      builder.setDocuments(
        Documents.newBuilder().setWarrantyExpire(CommonModel.Date.newBuilder().setYear(-1).setMonth(2).setDay(35))
      )
    }
    checkValidationErrorRequest(req2, validationError(WrongWarrantyExpireYear, WrongWarrantyExpireDay(28)))
  }

  test("wrong youtube url") {
    // передали неправильный ютуб url
    val req = createRequest(Post("/api/v1/draft/trucks/user:1")) {
      _.getStateBuilder.setVideo(CommonModel.Video.newBuilder().setYoutubeUrl("bahbah"))
    }
    checkValidationErrorRequest(req, validationError(WrongVideoYoutube))
  }

  test("offer update from draft with validation errors") {
    // сохраняем полноценное объявление
    val formInfo: FormInfo = testFormGenerator.createForm("trucks", TestFormParams(isDealer = false))
    val extUserId: String = formInfo.extUserId
    val fullForm = formInfo.form
    val offerId = checkSuccessRequest(createRequest(Post(s"/api/v1/offers/trucks/$extUserId?insert_new=1")) { builder =>
      builder.mergeFrom(fullForm)
    })
    // делаем из него черновик
    val draft = checkSuccessRequestWithOffer(Post(s"/api/v1/offer/trucks/$extUserId/$offerId/draft"))
    val draftId = draft.getId

    // удаляем цвет из черновика
    checkSuccessRequest(createRequest(Put(s"/api/v1/draft/trucks/$extUserId/$draftId")) { builder =>
      builder.mergeFrom(draft)
      builder.clearColorHex()
    })

    // пытаемся обновить объявление из такого черновика
    checkValidationErrorRequest(
      Post(s"/api/v1/draft/trucks/$draftId/publish/$extUserId"),
      validationError(NotexistColor)
    )
  }

  test("publish dealer draft with not unique vin validation errors") {
    // сохраняем полноценное объявление
    val formInfo: FormInfo = testFormGenerator.createForm("trucks", TestFormParams(isDealer = true))
    val extUserId: String = formInfo.extUserId
    val fullForm = formInfo.form
    val offerId = checkSuccessRequest(createRequest(Post(s"/api/v1/offers/trucks/$extUserId?insert_new=1")) { builder =>
      builder.mergeFrom(fullForm)
    })
    // делаем из него черновик
    val draft = checkSuccessRequestWithOffer(Post(s"/api/v1/offer/trucks/$extUserId/$offerId/draft"))
    val draftId = draft.getId

    // пытаемся опубликовать объявление из такого черновика + передаем параметр, что вин не уникален
    checkValidationErrorRequest(
      Post(s"/api/v1/draft/trucks/$draftId/publish/$extUserId?vin_unique=0"),
      validationError(ForbiddenNotUniqueVinCommercial)
    )
  }

  test("publish user draft with not unique vin validation errors") {
    // сохраняем полноценное объявление
    val formInfo: FormInfo = testFormGenerator.createForm("trucks", TestFormParams(isDealer = false))
    val extUserId: String = formInfo.extUserId
    val fullForm = formInfo.form
    val offerId = checkSuccessRequest(createRequest(Post(s"/api/v1/offers/trucks/$extUserId?insert_new=1")) { builder =>
      builder.mergeFrom(fullForm)
    })
    // делаем из него черновик
    val draft = checkSuccessRequestWithOffer(Post(s"/api/v1/offer/trucks/$extUserId/$offerId/draft"))
    val draftId = draft.getId

    // пытаемся опубликовать объявление из такого черновика + передаем параметр, что вин не уникален
    checkSuccessRequestWithOffer(Post(s"/api/v1/draft/trucks/$draftId/publish/$extUserId?vin_unique=0"))
  }

  for {
    isDealer <- Seq(false, true)
    category <- Seq("cars", "trucks", "moto")
  } {
    test(s"empty draft validation (isDealer = $isDealer, category = $category)") {
      val (_, extOwnerId: String, _) = testFormGenerator.randomOwnerIds(isDealer)
      val req1 = minimalDraftCreationRequest(category, extOwnerId)
      val draft1 = checkSuccessRequestWithOffer(req1)

      // пробуем опубликовать, проверяем ошибки

      checkValidationErrors(isDealer, category, draft1.getId, extOwnerId)
    }
  }

  //scalastyle:off line.size.limit

  for {
    region <- RandomUtil.chooseN(5, components.regionTree.children(1).toSeq)
    mark <- RandomUtil.chooseN(2, Seq("VOLKSWAGEN", "AUDI", "SKODA", "RENAULT", "CHEVROLET", "LEXUS", "HYUNDAI"))
  } test(
    s"VIN or license_plate required required for users in Moscow region for several marks (region = ${region.ruName}(${region.id}), mark = $mark)"
  ) {
    // https://st.yandex-team.ru/AUTORUAPI-3137
    val category = "cars"
    val (_, extUserId: String, _) = testFormGenerator.randomOwnerIds(isDealer = false)
    val req: HttpRequest = createRequest(Post(s"/api/v1/draft/$category/$extUserId")) { builder =>
      builder.getCarInfoBuilder.setMark(mark)
      builder.getDocumentsBuilder.setCustomCleared(true)
      builder.getSellerBuilder.getLocationBuilder.setGeobaseId(region.id)
      builder.getDocumentsBuilder.setYear(2001)
    }

    val draft1 = checkSuccessRequestWithOffer(req)
    val draftId1 = draft1.getId
    checkValidationErrorRequestContains(
      Post(s"/api/v1/draft/$category/$draftId1/publish/$extUserId"),
      RequiredVinOrLicensePlate
    )
  }

  for {
    region <- RandomUtil.chooseN(5, components.regionTree.children(1).toSeq)
    mark <- RandomUtil.chooseN(2, Seq("VOLKSWAGEN", "AUDI", "SKODA", "RENAULT", "CHEVROLET", "LEXUS", "HYUNDAI"))
  } test(s"VIN  required for old clients (region = ${region.ruName}(${region.id}), mark = $mark)") {
    // https://st.yandex-team.ru/AUTORUAPI-3137
    val category = "cars"
    val (_, extUserId: String, _) = testFormGenerator.randomOwnerIds(isDealer = false)
    val req: HttpRequest = createRequest(Post(s"/api/v1/draft/$category/$extUserId")) { builder =>
      builder.getCarInfoBuilder.setMark(mark)
      builder.getDocumentsBuilder.setCustomCleared(true)
      builder.getSellerBuilder.getLocationBuilder.setGeobaseId(region.id)
      builder.getDocumentsBuilder.setYear(2001)
    }

    val draft1 = checkSuccessRequestWithOffer(req)
    val draftId1 = draft1.getId
    checkValidationErrorRequestContains(
      Post(s"/api/v1/draft/$category/$draftId1/publish/$extUserId?no_license_plate_support=true"),
      RequiredVin
    )
  }

  for {
    region <- RandomUtil.chooseN(2, components.regionTree.children(1).toSeq)
    mark <- Seq("VOLKSWAGEN", "AUDI", "SKODA", "RENAULT", "CHEVROLET", "LEXUS", "HYUNDAI")
  } test(
    s"VIN decoder resolution failed for users in Moscow region for several marks (region = ${region.ruName}(${region.id}), mark = $mark)"
  ) {
    // https://st.yandex-team.ru/AUTORUAPI-3137
    val category = "cars"
    val (_, extUserId: String, _) = testFormGenerator.randomOwnerIds(isDealer = false)
    val req: HttpRequest = createRequest(Post(s"/api/v1/draft/$category/$extUserId")) { builder =>
      builder.getCarInfoBuilder.setMark(mark)
      builder.getDocumentsBuilder.setCustomCleared(true)
      builder.getSellerBuilder.getLocationBuilder.setGeobaseId(region.id)
      builder.getDocumentsBuilder.setYear(2001).setVin("VF7XS9HHCEZ005623")
    }

    val draft1 = checkSuccessRequestWithOffer(req)
    val draftId1 = draft1.getId
    val publishReq1: HttpRequest = Post(s"/api/v1/draft/$category/$draftId1/publish/$extUserId")
    checkValidationErrorRequestNotContains(publishReq1, WrongVin)
    val publishReq2: HttpRequest =
      Post(s"/api/v1/draft/$category/$draftId1/publish/$extUserId?vin_decoder_resolution=STOLEN_VIN")
    if (mark == "AUDI" || mark == "VOLKSWAGEN") {
      checkValidationErrorRequestNotContains(publishReq2, WrongVin)
    } else {
      checkValidationErrorRequestContains(publishReq2, WrongVinFromDecoder)
    }
  }

  for {
    region <- RandomUtil.chooseN(
      5,
      components.regionTree.children(1).toSeq ++ components.regionTree.children(10174L).toSeq
    )
  } test(
    s"VIN required for truck dealers in Moscow and St Petersburg regions (region = ${region.ruName}(${region.id}))"
  ) {
    // https://st.yandex-team.ru/VOS-1875
    // https://st.yandex-team.ru/AUTORUAPI-3912
    val category = "trucks"
    val extDealerId: String = "dealer:21029"
    val req: HttpRequest = createRequest(Post(s"/api/v1/draft/$category/$extDealerId")) { builder =>
      builder.getSellerBuilder.getLocationBuilder.setGeobaseId(region.id)
      builder.getDocumentsBuilder.setYear(1996)
      builder.setAvailability(Availability.IN_STOCK)
    }
    val draft1 = checkSuccessRequestWithOffer(req)
    val draftId1 = draft1.getId
    checkValidationErrorRequestContains(Post(s"/api/v1/draft/$category/$draftId1/publish/$extDealerId"), RequiredVin)

    // если на заказ, то не требуем vin
    val req2: HttpRequest = createRequest(Put(s"/api/v1/draft/$category/$extDealerId/$draftId1")) { builder =>
      builder.mergeFrom(draft1)
      builder.setAvailability(Availability.ON_ORDER)
    }
    checkSuccessRequest(req2)
    checkValidationErrorRequestNotContains(Post(s"/api/v1/draft/$category/$draftId1/publish/$extDealerId"), RequiredVin)

    // в спецтехнике не требуем vin
    for {
      truckCategory <- Seq(
        AGRICULTURAL,
        CONSTRUCTION,
        AUTOLOADER,
        CRANE,
        DREDGE,
        BULLDOZERS,
        CRANE_HYDRAULICS,
        MUNICIPAL
      )
    } {
      val req3: HttpRequest = createRequest(Put(s"/api/v1/draft/$category/$extDealerId/$draftId1")) { builder =>
        builder.mergeFrom(draft1)
        builder.setAvailability(Availability.IN_STOCK)
        builder.getTruckInfoBuilder.setTruckCategory(TruckCategory.AGRICULTURAL)
      }
      checkSuccessRequest(req3)
      checkValidationErrorRequestNotContains(
        Post(s"/api/v1/draft/$category/$draftId1/publish/$extDealerId"),
        RequiredVin
      )
    }
  }

  for {
    canCreateRedirect <- Seq(1, 0)
    redirectPhones <- Seq(true, false)
  } test(s"forbidden phones redirects creation (canCreateRedirect=$canCreateRedirect, redirectPhones=$redirectPhones)") {
    val category = "cars"
    val (_, extUserId: String, _) = testFormGenerator.randomOwnerIds(isDealer = false)
    val req: HttpRequest = createRequest(Post(s"/api/v1/draft/$category/$extUserId")) { builder =>
      builder.getSellerBuilder.setRedirectPhones(redirectPhones)
    }
    val draft1 = checkSuccessRequestWithOffer(req)
    val draftId1 = draft1.getId
    val publishReq1: HttpRequest = Post(s"/api/v1/draft/$category/$draftId1/publish/$extUserId")
    checkValidationErrorRequestNotContains(publishReq1, ForbiddenPhoneRedirectCreation)
    val publishReq2: HttpRequest =
      Post(s"/api/v1/draft/$category/$draftId1/publish/$extUserId?can_create_redirect=$canCreateRedirect")
    if (redirectPhones && canCreateRedirect == 0) {
      checkValidationErrorRequestContains(publishReq2, ForbiddenPhoneRedirectCreation)
    } else {
      checkValidationErrorRequestNotContains(publishReq2, ForbiddenPhoneRedirectCreation)
    }
  }

  for {
    category <- Seq("cars", "trucks", "moto")
    fieldName <- Seq("custom_location", "custom_phones")
  } {
    test(s"forbidden set $fieldName in $category") {
      val testFormParams = TestFormParams(isDealer = true, optOwnerId = Some(8514))
      val formInfo = testFormGenerator.createForm(category, testFormParams)
      val extUserId = formInfo.extUserId
      val formBuilder = formInfo.form.toBuilder
      val sellerBuilder = formBuilder.getSellerBuilder

      val fiedlDescriptor = sellerBuilder.getDescriptorForType.findFieldByName(fieldName)
      sellerBuilder.setField(fiedlDescriptor, true)

      val error = fieldName match {
        case "custom_location" =>
          sellerBuilder.getLocationBuilder.setAddress("Инициативная улица, 3В")
          ForbiddenSalonEditAddress
        case "custom_phones" =>
          sellerBuilder.clearPhones().addPhonesBuilder().setPhone("79000000000")
          ForbiddenSalonEditPhones(Seq("79000000000"), Seq("74952254487", "74951250722"))
      }

      val draft = checkSuccessRequestWithOffer(createRequest(Post(s"/api/v1/draft/$category/$extUserId")) { builder =>
        builder.mergeFrom(formBuilder.build)
      })

      checkValidationErrorRequest(
        Post(s"/api/v1/draft/$category/${draft.getId}/publish/$extUserId"),
        validationError(error)
      )
    }
  }

  test(s"WrongFrameFormat instead of WrongVinFormat") {
    // https://st.yandex-team.ru/AUTORUAPI-3137
    val category = "cars"
    val (_, extUserId: String, _) = testFormGenerator.randomOwnerIds(isDealer = false)
    val req: HttpRequest = createRequest(Post(s"/api/v1/draft/$category/$extUserId")) { builder =>
      builder.getDocumentsBuilder.setCustomCleared(true)
      builder.getCarInfoBuilder.setSteeringWheel(SteeringWheel.RIGHT)
      builder.getDocumentsBuilder.setYear(2001).setVin("#!?VF7XS9HHCEZ005623-012")
    }

    val draft1 = checkSuccessRequestWithOffer(req)
    val draftId1 = draft1.getId
    val publishReq1: HttpRequest = Post(s"/api/v1/draft/$category/$draftId1/publish/$extUserId")
    checkValidationErrorRequestNotContains(publishReq1, WrongVin)
    val publishReq2: HttpRequest =
      Post(s"/api/v1/draft/$category/$draftId1/publish/$extUserId?vin_decoder_resolution=STOLEN_VIN")

    checkValidationErrorRequestNotContains(publishReq2, WrongVin)
    checkValidationErrorRequestContains(publishReq2, WrongFrameNumber)
  }

  test(s"At least calls or chats should be enabled") {
    val category = "cars"
    val (_, extUserId: String, _) = testFormGenerator.randomOwnerIds(isDealer = false)
    val req: HttpRequest = createRequest(Post(s"/api/v1/draft/$category/$extUserId")) { builder =>
      builder.getAdditionalInfoBuilder.setChatOnly(true)
      builder.getSellerBuilder.setChatsEnabled(false)
    }

    val draft1 = checkSuccessRequestWithOffer(req)
    val draftId1 = draft1.getId
    val publishReq1: HttpRequest = Post(s"/api/v1/draft/$category/$draftId1/publish/$extUserId")
    checkValidationErrorRequestNotContains(publishReq1, AtLeastCallsOrChats)
  }

  private def minimalDraftCreationRequest(category: String, userRef: String): HttpRequest = {
    createRequest(Post(s"/api/v1/draft/$category/$userRef")) { builder =>
      category match {
        case "cars" =>
          builder.setCarInfo(
            CarInfo.newBuilder().setHorsePower(140)
          )
        case "trucks" =>
          builder.setTruckInfo(
            TruckInfo.newBuilder().setAxis(6)
          )
        case "moto" =>
          builder.setMotoInfo(
            MotoInfo.newBuilder().setHorsePower(150)
          )
        case _ => sys.error(s"unexpected category $category")
      }
    }
  }

  private def checkValidationErrors(isDealer: Boolean, category: String, draftId1: String, extUserId: String): Unit = {
    category match {
      case "cars" =>
        checkValidationErrorRequest(
          Post(s"/api/v1/draft/$category/$draftId1/publish/$extUserId"),
          if (isDealer) salonCarValidationError else privateCarValidationError
        )
      case "trucks" =>
        checkValidationErrorRequest(
          Post(s"/api/v1/draft/$category/$draftId1/publish/$extUserId"),
          if (isDealer) salonTruckValidationError else privateTruckValidationError
        )
      case "moto" =>
        checkValidationErrorRequest(
          Post(s"/api/v1/draft/$category/$draftId1/publish/$extUserId"),
          if (isDealer) salonMotoValidationError else privateMotoValidationError
        )
    }
  }
}
