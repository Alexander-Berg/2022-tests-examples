package ru.yandex.realty.managers.rent.inventory

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.rent.RentInventoryServiceClient
import ru.yandex.realty.rent.proto.api.internal.inventory.{
  InternalAskInventorySmsConfirmationRequest,
  InternalConfirmInventoryRequest,
  InternalConfirmInventoryResponse,
  InternalGetInventoryByVersionAndOwnerIdRequest,
  InternalGetInventoryRequest,
  InternalUpdateOrCreateInventoryRequest
}
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class RentInventoryManagerSpec extends AsyncSpecBase {
  implicit protected val traced: Traced = Traced.empty

  private val rentInventoryClient = mock[RentInventoryServiceClient]
  protected val rentInventoryManager = new DefaultRentInventoryManager(rentInventoryClient)

  "RentInventoryManager" should {

    "return confirmed inventory" in new Data {

      (rentInventoryClient
        .getConfirmedInventory(_: InternalGetInventoryRequest)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(internalInventoryResponse(confirmedInventory)))

      val resultResponse = rentInventoryManager.getConfirmedInventory(passportUser, ownerRequestId).futureValue

      resultResponse shouldBe inventoryResponse(confirmedInventory)
      resultResponse shouldBe inventoryResponse(confirmedInventory)
    }

    "return not confirmed inventory" in new Data {

      (rentInventoryClient
        .getLastInventory(_: InternalGetInventoryRequest)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(internalInventoryResponse(notConfirmedInventory)))

      val resultResponse = rentInventoryManager.getLastInventory(passportUser, ownerRequestId).futureValue

      resultResponse shouldBe inventoryResponse(notConfirmedInventory)
    }

    "return inventory by version" in new Data {
      (rentInventoryClient
        .getInventoryByVersionAndOwnerRequestId(_: InternalGetInventoryByVersionAndOwnerIdRequest)(
          _: Traced
        ))
        .expects(*, *)
        .returning(Future.successful(internalInventoryResponse(notConfirmedInventory)))

      val resultResponse =
        rentInventoryManager
          .getInventoryByVersionAndOwnerRequestId(passportUser, notConfirmedInventory.getVersion, ownerRequestId)
          .futureValue

      resultResponse shouldBe inventoryResponse(notConfirmedInventory)
    }

    "return updated inventory" in new Data {
      (rentInventoryClient
        .updateOrCreateInventory(_: InternalUpdateOrCreateInventoryRequest)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(internalUpdateOrCreateInventoryResponse(confirmedInventory)))

      val resultResponse =
        rentInventoryManager
          .updateOrCreateInventory(
            passportUser,
            ownerRequestId,
            buildUpdateOrCreateInventoryRequest(confirmedInventory)
          )
          .futureValue

      resultResponse shouldBe internalUpdateOrCreateInventoryResponse(confirmedInventory)
    }

    "return error when inventory update fail due to incorrect inventory state" in new Data {
      (rentInventoryClient
        .updateOrCreateInventory(_: InternalUpdateOrCreateInventoryRequest)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(internalUpdateOrCreateInventoryErrorResponse))

      val resultResponse =
        rentInventoryManager
          .updateOrCreateInventory(
            passportUser,
            ownerRequestId,
            buildUpdateOrCreateInventoryRequest(confirmedInventory)
          )
          .futureValue

      resultResponse shouldBe internalUpdateOrCreateInventoryErrorResponse
    }

    "return sms info" in new Data {
      (rentInventoryClient
        .askSmsConfirmation(_: InternalAskInventorySmsConfirmationRequest)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(internalAskInventorySmsConfirmationResponse))

      val resultResponse =
        rentInventoryManager.askSmsConfirmation(passportUser, ownerRequestId).futureValue

      resultResponse shouldBe askInventorySmsConfirmationResponse
    }

    "return ok when inventory was confirmed" in new Data {
      val internalConfirmInventorySuccessResponse: InternalConfirmInventoryResponse =
        InternalConfirmInventoryResponse
          .newBuilder()
          .setSuccess(unitResponse)
          .build()

      (rentInventoryClient
        .confirmInventory(_: InternalConfirmInventoryRequest)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(internalConfirmInventorySuccessResponse))

      val resultResponse =
        rentInventoryManager.confirmInventory(passportUser, ownerRequestId, confirmRequest).futureValue

      resultResponse shouldBe internalConfirmInventorySuccessResponse
    }

    "return error when inventory confirmation fail" in new Data {
      (rentInventoryClient
        .confirmInventory(_: InternalConfirmInventoryRequest)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(internalConfirmInventoryErrorResponse))

      val resultResponse =
        rentInventoryManager.confirmInventory(passportUser, ownerRequestId, confirmRequest).futureValue

      resultResponse shouldBe internalConfirmInventoryErrorResponse
    }
  }
}
