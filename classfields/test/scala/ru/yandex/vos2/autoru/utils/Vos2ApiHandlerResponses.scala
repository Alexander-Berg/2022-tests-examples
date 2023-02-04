package ru.yandex.vos2.autoru.utils

import ru.yandex.vos2.api.utils.ErrorCode
import ru.yandex.vos2.autoru.utils.validators.ValidationErrors.{ForbiddenBooking, ValidationError}

case class ValidationErrorDescription(code: String, error_code: String, description: String, field: Option[String])

case class ValidationErrorResponse(status: String, errors: List[ValidationErrorDescription])

import play.api.libs.json.Json

case class ErrorDescription(code: String, description: String)

case class ErrorResponse(status: String, errors: List[ErrorDescription]) {

  def filter(func: String => Boolean): ErrorResponse = {
    copy(errors = errors.filter(x => func(x.description)))
  }

  def add(anotherErrors: String*): ErrorResponse =
    copy(errors = errors ::: anotherErrors.toList.map(e => {
      ErrorDescription(code = "WRONG_REQUEST", description = e)
    }))
}

case class SuccessResponse0(status: String, offerId: String)
case class SuccessResponse(status: String, offerId: String)

case class SuccessPhotoResponse(status: String, offerId: String, photoId: String)

case class StatusHistoryElem(status: String, comment: String, timestamp: Long)

case class StatusHistory(offerId: String, status_history: List[StatusHistoryElem])

case class UserChangeActionResponse(offerId: String, items: List[UserChangeActionItem])

case class UserChangeActionItem(platform: String, user: String, ip: String, timestamp: Long, status: String)

/**
  * Created by andrey on 3/6/17.
  */
object Vos2ApiHandlerResponses {
  implicit val ErrorDescriptionReader = Json.reads[ErrorDescription]
  implicit val ErrorResponseReader = Json.reads[ErrorResponse]
  implicit val ValidationErrorDescriptionReader = Json.reads[ValidationErrorDescription]
  implicit val ValidationErrorResponseReader = Json.reads[ValidationErrorResponse]
  implicit val SuccessResponseReader = Json.reads[SuccessResponse]
  implicit val SuccessPhotoResponseReader = Json.reads[SuccessPhotoResponse]
  implicit val StatusHistoryElemReader = Json.reads[StatusHistoryElem]
  implicit val StatusHistoryReader = Json.reads[StatusHistory]
  implicit val userChangeActionItemReader = Json.reads[UserChangeActionItem]
  implicit val userChangeActionResponseReader = Json.reads[UserChangeActionResponse]

  val unknownHandler: ErrorResponse = {
    ErrorResponse(
      status = "ERROR",
      errors = List(
        ErrorDescription(
          code = "NOT_FOUND",
          description = "The requested handler could not be found. Please check method and url of the request."
        )
      )
    )
  }

  val unallowedUserRef: ErrorResponse = {
    ErrorResponse(
      status = "ERROR",
      errors = List(
        ErrorDescription(
          code = "ILLEGAL_ARGUMENT",
          description = "Unallowed userRef"
        )
      )
    )
  }

  def unsupportedMethod(expected: String): ErrorResponse = {
    ErrorResponse(
      status = "ERROR",
      errors = List(
        ErrorDescription(
          code = "WRONG_REQUEST",
          description = s"Unsupported method, expected $expected"
        )
      )
    )
  }

  def validationError(description: ValidationError*): ValidationErrorResponse = {
    ValidationErrorResponse(
      status = "ERROR",
      errors = description
        .map(d =>
          ValidationErrorDescription(
            code = "VALIDATION_ERROR",
            error_code = d.code,
            description = d.description,
            field = d.field.map(_.name)
          )
        )
        .toList
    )
  }

  def illegalArgumentError(description: String*): ErrorResponse = {
    ErrorResponse(
      status = "ERROR",
      errors = description
        .map(d =>
          ErrorDescription(
            code = "ILLEGAL_ARGUMENT",
            description = d
          )
        )
        .toList
    )
  }

  val noPhotoToUpload: ErrorResponse = {
    ErrorResponse(
      status = "ERROR",
      errors = List(
        ErrorDescription(
          code = "NO_PHOTO_TO_UPLOAD",
          description = "No photo to upload"
        )
      )
    )
  }

  val unknownOfferError: ErrorResponse = {
    ErrorResponse(
      status = "ERROR",
      errors = List(
        ErrorDescription(
          code = "UNKNOWN_OFFER",
          description = "Offer Not Found"
        )
      )
    )
  }

  val unexpectedPhotoError: ErrorResponse = {
    ErrorResponse(
      status = "ERROR",
      errors = List(
        ErrorDescription(
          code = "UNEXPECTED_PHOTOS",
          description = "Unexpected photos in draft"
        )
      )
    )
  }

  val unknownSaleError: ErrorResponse = {
    ErrorResponse(
      status = "ERROR",
      errors = List(
        ErrorDescription(
          code = "UNKNOWN_OFFER",
          description = "Sale Not Found"
        )
      )
    )
  }

  val DraftNotFoundError: ErrorResponse = {
    ErrorResponse(
      status = "ERROR",
      errors = List(
        ErrorDescription(
          code = "UNKNOWN_OFFER",
          description = "Draft Not Found"
        )
      )
    )
  }

  val unknownDraftOriginError: ErrorResponse = {
    ErrorResponse(
      status = "ERROR",
      errors = List(
        ErrorDescription(
          code = "UNKNOWN_DRAFT_ORIGIN",
          description = "Cannot determine which offer must be updated from this draft"
        )
      )
    )
  }

  val unknownPhoto: ErrorResponse = {
    ErrorResponse(
      status = "ERROR",
      errors = List(
        ErrorDescription(
          code = "UNKNOWN_PHOTO",
          description = "Photo Not Found"
        )
      )
    )
  }

  val invalidTagsError: ErrorResponse = {
    ErrorResponse(
      status = "ERROR",
      errors = List(
        ErrorDescription(
          code = "INVALID_TAGS",
          description = "Tags are invalid"
        )
      )
    )
  }

  def internalError(description: String): ErrorResponse = {
    ErrorResponse(
      status = "ERROR",
      errors = List(
        ErrorDescription(
          code = "INTERNAL_ERROR",
          description = description
        )
      )
    )
  }

  def invalidUploaderData(description: String): ErrorResponse = {
    ErrorResponse(
      status = "ERROR",
      errors = List(
        ErrorDescription(
          code = "INVALID_UPLOADER_DATA",
          description = description
        )
      )
    )
  }

  val invalidExternalImage: ErrorResponse = {
    ErrorResponse(
      status = "ERROR",
      errors = List(
        ErrorDescription(
          code = "INVALID_EXTERNAL_IMAGE",
          description = "RuntimeException: urlDownloader error"
        )
      )
    )
  }

  val invalidOfferIRef: ErrorResponse = {
    ErrorResponse(
      status = "ERROR",
      errors = List(
        ErrorDescription(
          code = "ILLEGAL_ARGUMENT",
          description = "Offer iRef is invalid"
        )
      )
    )
  }

  val longUserRefError: ErrorResponse = {
    ErrorResponse(
      status = "ERROR",
      errors = List(
        ErrorDescription(
          code = "LONG_USER_REF",
          description = "UserRef too long"
        )
      )
    )
  }

  def unsupportedUserType(message: String): ErrorResponse =
    ErrorResponse(
      status = "ERROR",
      errors = List(
        ErrorDescription(
          code = "UNSUPPORTED_USER_TYPE",
          description = message
        )
      )
    )

  def unsupportedServiceType(message: String): ErrorResponse =
    ErrorResponse(
      status = "ERROR",
      errors = List(
        ErrorDescription(
          code = "UNSUPPORTED_SERVICE_TYPE",
          description = message
        )
      )
    )

  val updateStatusError: ErrorResponse =
    ErrorResponse(
      status = "ERROR",
      errors = List(
        ErrorDescription(
          code = "ILLEGAL_ARGUMENT",
          description = "Failed to update status"
        )
      )
    )

  val oldOfferError: ErrorResponse =
    ErrorResponse(
      status = "ERROR",
      errors = List(
        ErrorDescription(
          code = "OLD_OFFER",
          description = "Failed to update status: old offer"
        )
      )
    )

  val noPhoneOfferError: ErrorResponse =
    ErrorResponse(
      status = "ERROR",
      errors = List(
        ErrorDescription(
          code = "NO_PHONE",
          description = "Failed to update status: no phone"
        )
      )
    )

  val salonNotFoundError: ErrorResponse = {
    ErrorResponse(
      status = "ERROR",
      errors = List(
        ErrorDescription(
          code = "SALON_NOT_FOUND",
          description = "Salon Not Found"
        )
      )
    )
  }

  val invalidOfferPriceError: ValidationErrorResponse = {
    ValidationErrorResponse(
      status = "ERROR",
      errors = List(
        ValidationErrorDescription(
          code = "VALIDATION_ERROR",
          error_code = "wrong.price.rub",
          description = "Цена в рублях должна быть в диапазоне 1500 - 1000000000",
          field = Some("price")
        )
      )
    )
  }

  val invalidDiscountOptionsError: ValidationErrorResponse = {
    ValidationErrorResponse(
      status = "ERROR",
      errors = List(
        ValidationErrorDescription(
          code = "VALIDATION_ERROR",
          error_code = "nonempty.tradin.discount",
          description = "Скидка при покупке по программе Trade-In не применима для данной категории автомобилей",
          field = Some("price")
        )
      )
    )
  }

  val failedToUpdatePriceError: ErrorResponse =
    ErrorResponse(
      status = "ERROR",
      errors = List(
        ErrorDescription(
          code = "FAILED_TO_UPDATE_ATTRIBUTE",
          description = "price is already 300000 and currency is USD"
        )
      )
    )

  val failedToUpdatePriceInOldDbError: ErrorResponse =
    ErrorResponse(
      status = "ERROR",
      errors = List(
        ErrorDescription(
          code = "FAILED_TO_UPDATE_ATTRIBUTE",
          description = "Failed to update offer price in old db"
        )
      )
    )

  def wrongFeatureValueFormat(description: String): ErrorResponse =
    ErrorResponse(
      status = "ERROR",
      errors = List(
        ErrorDescription(
          code = "ILLEGAL_ARGUMENT",
          description = description
        )
      )
    )

  val forbiddenBooking: ErrorResponse =
    ErrorResponse(
      status = "ERROR",
      errors = List(
        ErrorDescription(
          code = ErrorCode.ForbiddenEditField.name,
          description = ForbiddenBooking.description
        )
      )
    )
}
