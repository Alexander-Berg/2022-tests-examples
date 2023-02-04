package ru.yandex.realty.managers.rent.inventory

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.rent.RentInventoryServiceClient
import ru.yandex.realty.model.util.Page
import ru.yandex.realty.rent.proto.api.internal.inventory.{
  InternalModerationApproveInventoryRequest,
  InternalModerationGetAllInventoryRequest,
  InternalModerationGetInventoryByVersionAndOwnerIdRequest,
  InternalModerationGetInventoryRequest,
  InternalModerationResetInventoryRequest,
  InternalModerationUpdateOrCreateInventoryRequest
}
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class RentModerationInventoryManagerSpec extends AsyncSpecBase {

  implicit protected val traced: Traced = Traced.empty

  private val rentInventoryClient = mock[RentInventoryServiceClient]
  protected val rentInventoryManager = new DefaultRentInventoryManager(rentInventoryClient)

  "RentModerationInventoryManager" should {

    "return confirmed moderation inventory" in new Data {
      (rentInventoryClient
        .getConfirmedModerationInventory(_: InternalModerationGetInventoryRequest)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(internalModerationInventoryResponse(confirmedInventory)))

      val resultResponse =
        rentInventoryManager.getConfirmedModerationInventory(ownerRequestId).futureValue

      resultResponse shouldBe moderationInventoryResponse(confirmedInventory)
    }

    "return not confirmed moderation inventory" in new Data {
      (rentInventoryClient
        .getLastModerationInventory(_: InternalModerationGetInventoryRequest)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(Some(internalModerationInventoryResponse(notConfirmedInventory))))

      val resultResponse =
        rentInventoryManager.getLastModerationInventory(ownerRequestId).futureValue

      resultResponse shouldBe moderationInventoryResponse(notConfirmedInventory)
    }

    "return inventory by version" in new Data {
      (rentInventoryClient
        .getModerationInventoryByVersionAndOwnerRequestId(_: InternalModerationGetInventoryByVersionAndOwnerIdRequest)(
          _: Traced
        ))
        .expects(*, *)
        .returning(Future.successful(internalModerationInventoryResponse(notConfirmedInventory)))

      val resultResponse =
        rentInventoryManager
          .getModerationInventoryByVersionAndOwnerRequestId(notConfirmedInventory.getVersion, ownerRequestId)
          .futureValue

      resultResponse shouldBe moderationInventoryResponse(notConfirmedInventory)
    }

    "return all inventory" in new Data {
      val inventories = Seq(notConfirmedInventory, confirmedInventory)

      (rentInventoryClient
        .getAllModerationInventory(_: InternalModerationGetAllInventoryRequest)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(internalModerationGetAllInventoryResponse(inventories)))

      val resultResponse =
        rentInventoryManager.getAllModerationInventory(ownerRequestId, Page(0, 10)).futureValue

      resultResponse shouldBe moderationGetAllInventoryResponse(inventories)
    }

    "return updated inventory" in new Data {
      (rentInventoryClient
        .updateOrCreateModerationInventory(_: InternalModerationUpdateOrCreateInventoryRequest)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(internalModerationUpdateOrCreateInventoryResponse(confirmedInventory)))

      val resultResponse =
        rentInventoryManager
          .updateOrCreateModerationInventory(
            ownerRequestId,
            buildUpdateOrCreateModerationInventoryRequest(confirmedInventory)
          )
          .futureValue

      resultResponse shouldBe internalModerationUpdateOrCreateInventoryResponse(confirmedInventory)
    }

    "return error when inventory update fail due to incorrect inventory state" in new Data {
      (rentInventoryClient
        .updateOrCreateModerationInventory(_: InternalModerationUpdateOrCreateInventoryRequest)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(internalModerationUpdateOrCreateInventoryErrorResponse))

      val resultResponse =
        rentInventoryManager
          .updateOrCreateModerationInventory(
            ownerRequestId,
            buildUpdateOrCreateModerationInventoryRequest(confirmedInventory)
          )
          .futureValue

      resultResponse shouldBe internalModerationUpdateOrCreateInventoryErrorResponse
    }

    "return reset inventory" in new Data {
      (rentInventoryClient
        .resetInventory(_: InternalModerationResetInventoryRequest)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(internalModerationInventoryResponse(notConfirmedInventory)))

      val resultResponse =
        rentInventoryManager.resetInventory(ownerRequestId).futureValue

      resultResponse shouldBe moderationInventoryResponse(notConfirmedInventory)
    }

    "return approved inventory" in new Data {
      (rentInventoryClient
        .approveInventory(_: InternalModerationApproveInventoryRequest)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(internalModerationInventoryResponse(notConfirmedInventory)))

      val resultResponse =
        rentInventoryManager.approveInventory(ownerRequestId).futureValue

      resultResponse shouldBe moderationInventoryResponse(notConfirmedInventory)
    }
  }
}
