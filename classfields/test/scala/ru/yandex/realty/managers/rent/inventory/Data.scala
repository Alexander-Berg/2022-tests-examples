package ru.yandex.realty.managers.rent.inventory

import ru.yandex.realty.api.BasicSmsConfirmation.{
  BasicSmsConfirmationValidationError,
  BasicSmsConfirmationValidationErrorData,
  BasicSmsConfirmationValidationErrorResponse
}

import collection.JavaConverters._
import ru.yandex.realty.api.ProtoResponse.ApiUnit
import ru.yandex.realty.model.user.PassportUser
import ru.yandex.realty.proto.api.error.ErrorCode
import ru.yandex.realty.rent.proto.api.internal.inventory.{
  InternalAskInventorySmsConfirmationResponse,
  InternalConfirmInventoryResponse,
  InternalInventoryResponse,
  InternalModerationGetAllInventoryResponse,
  InternalModerationInventoryResponse,
  InternalModerationUpdateOrCreateInventoryResponse,
  InternalUpdateOrCreateInventoryResponse
}
import ru.yandex.realty.rent.proto.api.inventory.{
  AllModerationInventoryResponse,
  AskInventorySmsConfirmationResponse,
  ConfirmInventoryRequest,
  GetAllModerationInventoryResponse,
  GetInventoryResponse,
  GetModerationInventoryResponse,
  Inventory,
  InventoryResponse,
  InventorySmsConfirmationResponse,
  ModerationInventoryResponse,
  UpdateOrCreateInventoryError,
  UpdateOrCreateInventoryRequest,
  UpdateOrCreateInventorySuccessResponse,
  UpdateOrCreateModerationInventoryRequest,
  UpdateOrCreateModerationInventoryResponse
}
import ru.yandex.realty.rent.proto.api.sms.confirmation.{ConfirmSmsInfo, SentSmsInfo}
import ru.yandex.vertis.paging.Paging

trait Data {
  protected val passportUser: PassportUser = PassportUser(12345L)
  protected val ownerRequestId = "owrqstid"
  protected val inventoryVersion = 0
  protected val smsInfo = SentSmsInfo.newBuilder().build()

  val internalAskInventorySmsConfirmationResponse: InternalAskInventorySmsConfirmationResponse =
    InternalAskInventorySmsConfirmationResponse
      .newBuilder()
      .setSmsInfo(smsInfo)
      .build()

  val askInventorySmsConfirmationResponse: AskInventorySmsConfirmationResponse =
    AskInventorySmsConfirmationResponse
      .newBuilder()
      .setResponse(InventorySmsConfirmationResponse.newBuilder().setSmsInfo(smsInfo))
      .build()

  protected val confirmRequest =
    ConfirmInventoryRequest.newBuilder().setConfirmSmsInfo(ConfirmSmsInfo.newBuilder().build()).build()
  val unitResponse = ApiUnit.getDefaultInstance

  protected def buildInventory(
    confirmedByOwner: Boolean,
    confirmedByTenant: Boolean,
    needApproveByManager: Boolean
  ): Inventory =
    Inventory
      .newBuilder()
      .setConfirmedByOwner(confirmedByOwner)
      .setConfirmedByTenant(confirmedByTenant)
      .setNeedApproveByManager(needApproveByManager)
      .build()
  protected val confirmedInventory: Inventory =
    buildInventory(confirmedByOwner = true, confirmedByTenant = true, needApproveByManager = false)
  protected val notConfirmedInventory: Inventory =
    buildInventory(confirmedByOwner = false, confirmedByTenant = false, needApproveByManager = false)

  val internalUpdateOrCreateInventoryErrorResponse =
    InternalUpdateOrCreateInventoryResponse
      .newBuilder()
      .setError(UpdateOrCreateInventoryError.newBuilder().build())
      .build()

  val internalConfirmInventoryErrorResponse: InternalConfirmInventoryResponse =
    InternalConfirmInventoryResponse
      .newBuilder()
      .setValidationError(BasicSmsConfirmationValidationErrorData.newBuilder().build())
      .build()

  val confirmInventoryErrorResponse = BasicSmsConfirmationValidationErrorResponse
    .newBuilder()
    .setError(
      BasicSmsConfirmationValidationError
        .newBuilder()
        .setCode(ErrorCode.VALIDATION_ERROR)
        .setData(internalConfirmInventoryErrorResponse.getValidationError)
        .build()
    )
    .build()

  val internalModerationUpdateOrCreateInventoryErrorResponse =
    InternalModerationUpdateOrCreateInventoryResponse
      .newBuilder()
      .setError(UpdateOrCreateInventoryError.newBuilder().build())
      .build()

  protected def internalInventoryResponse(inventory: Inventory): InternalInventoryResponse =
    InternalInventoryResponse
      .newBuilder()
      .setInventory(inventory)
      .build()

  protected def internalUpdateOrCreateInventoryResponse(inventory: Inventory): InternalUpdateOrCreateInventoryResponse =
    InternalUpdateOrCreateInventoryResponse
      .newBuilder()
      .setSuccess(internalInventoryResponse(inventory))
      .build()

  protected def inventoryResponse(inventory: Inventory): GetInventoryResponse =
    GetInventoryResponse
      .newBuilder()
      .setResponse(
        InventoryResponse
          .newBuilder()
          .setInventory(inventory)
          .build()
      )
      .build()

  protected def updateOrCreateInventoryResponse(inventory: Inventory): UpdateOrCreateInventorySuccessResponse =
    UpdateOrCreateInventorySuccessResponse
      .newBuilder()
      .setResponse(inventoryResponse(inventory).getResponse)
      .build()

  protected def internalModerationInventoryResponse(inventory: Inventory): InternalModerationInventoryResponse =
    InternalModerationInventoryResponse
      .newBuilder()
      .setInventory(inventory)
      .build()

  protected def moderationInventoryResponse(inventory: Inventory): GetModerationInventoryResponse =
    GetModerationInventoryResponse
      .newBuilder()
      .setResponse(
        ModerationInventoryResponse
          .newBuilder()
          .setInventory(inventory)
          .build()
      )
      .build()

  protected def internalModerationGetAllInventoryResponse(
    inventories: Seq[Inventory]
  ): InternalModerationGetAllInventoryResponse =
    InternalModerationGetAllInventoryResponse
      .newBuilder()
      .addAllInventories(inventories.asJava)
      .build()

  protected def moderationGetAllInventoryResponse(inventory: Seq[Inventory]): GetAllModerationInventoryResponse =
    GetAllModerationInventoryResponse
      .newBuilder()
      .setResponse(
        AllModerationInventoryResponse
          .newBuilder()
          .addAllInventories(inventory.asJava)
          .setPaging(Paging.newBuilder().build())
          .build()
      )
      .build()

  protected def internalModerationUpdateOrCreateInventoryResponse(
    inventory: Inventory
  ): InternalModerationUpdateOrCreateInventoryResponse =
    InternalModerationUpdateOrCreateInventoryResponse
      .newBuilder()
      .setSuccess(internalModerationInventoryResponse(inventory))
      .build()

  protected def moderationUpdateOrCreateInventoryResponse(
    inventory: Inventory
  ): UpdateOrCreateModerationInventoryResponse =
    UpdateOrCreateModerationInventoryResponse
      .newBuilder()
      .setResponse(moderationInventoryResponse(inventory).getResponse)
      .build()

  protected def buildUpdateOrCreateInventoryRequest(inventory: Inventory) = {
    UpdateOrCreateInventoryRequest
      .newBuilder()
      .addAllRooms(inventory.getRoomsList)
      .addAllDefects(inventory.getDefectsList)
      .setVersion(inventory.getVersion)
      .build()
  }

  protected def buildUpdateOrCreateModerationInventoryRequest(inventory: Inventory) = {
    UpdateOrCreateModerationInventoryRequest
      .newBuilder()
      .addAllRooms(inventory.getRoomsList)
      .addAllDefects(inventory.getDefectsList)
      .setManagerComment(inventory.getManagerComment)
      .setVersion(inventory.getVersion)
      .build()
  }
}
