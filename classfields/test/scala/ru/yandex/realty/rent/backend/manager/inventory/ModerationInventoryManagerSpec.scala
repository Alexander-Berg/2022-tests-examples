package ru.yandex.realty.rent.backend.manager.inventory

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.errors.{ConflictApiException, ForbiddenApiException}
import ru.yandex.realty.model.util.Page
import ru.yandex.realty.rent.backend.manager.Wiring
import ru.yandex.realty.rent.model.Inventory
import ru.yandex.realty.rent.proto.api.inventory.{UpdateOrCreateModerationInventoryRequest, Inventory => ApiInventory}
import ru.yandex.realty.rent.proto.model.inventory.InventoryData.PdfDocumentInfo
import slick.dbio.DBIOAction
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ModerationInventoryManagerSpec extends CommonInventoryManagerSpec {

  "InventoryManager.getModerationInventory" should {

    "return confirmed inventory for manager" in new Wiring with Data {
      val inventory = generateConfirmedInventory.copy(ownerRequestId = ownerRequest.ownerRequestId)
      val responseInventory = inventoryConverter.convertToApiInventory(inventory)

      inventoryDao.insert(inventory).futureValue
      val response = moderationInventoryManager.getConfirmedModerationInventory(ownerRequest.ownerRequestId).futureValue

      response.getInventory shouldBe responseInventory
    }

    "return not confirmed inventory for manager" in new Wiring with Data {
      generateNotConfirmedInventories
        .foreach { il =>
          val inventory = il.copy(ownerRequestId = ownerRequest.ownerRequestId)
          val responseInventory = inventoryConverter.convertToApiInventory(inventory)

          inventoryDao.insert(inventory).futureValue
          val response =
            moderationInventoryManager.getLastModerationInventory(ownerRequest.ownerRequestId).futureValue

          response.getInventory shouldBe responseInventory
        }
    }
  }

  "InventoryManager.updateOrCreateModerationInventory" should {

    "create inventory by manager in" in new Wiring with Data {
      val inventory = generateInitialInventory
      val apiInventory = inventoryConverter.convertToApiInventory(inventory)
      handleMock(2, None)

      moderationInventoryManager
        .updateOrCreateModerationInventory(
          ownerRequest.ownerRequestId,
          buildUpdateOrCreateModerationInventoryRequest(apiInventory)
        )
        .futureValue
      val response = moderationInventoryManager.getLastModerationInventory(ownerRequest.ownerRequestId).futureValue

      response.getInventory shouldBe apiInventory
    }

    "update inventory by manager" in new Wiring with Data {
      val inventory = generateInitialInventory
      val inventoryToInsert = inventoryConverter.convertToApiInventory(buildInventoryWithComment(inventory, ""))
      val updatedInventory = inventoryConverter.convertToApiInventory(buildUpdatedInventory(inventory))
      handleMock(2, None)

      moderationInventoryManager
        .updateOrCreateModerationInventory(
          ownerRequest.ownerRequestId,
          buildUpdateOrCreateModerationInventoryRequest(inventoryToInsert)
        )
        .futureValue
      val updateResponse = moderationInventoryManager
        .updateOrCreateModerationInventory(
          ownerRequest.ownerRequestId,
          buildUpdateOrCreateModerationInventoryRequest(updatedInventory)
        )
        .futureValue

      updateResponse.getSuccess.getInventory shouldBe updatedInventory
    }

    "reset inventory when manager updates confirmed inventory" in new Wiring with Data {
      val inventory = generateConfirmedInventory
      val updatedInventory = inventory.copy(data = buildUpdatedInventory(inventory).data)
      val updatedApiInventory = inventoryConverter.convertToApiInventory(updatedInventory)
      handleMock(2, None)

      (mockInventoryDbActions
        .selectForUpdate(_: String))
        .expects(*)
        .returning(DBIOAction.successful(Some(inventory)))
        .repeat(1)

      (mockInventoryDbActions
        .insert(_: Inventory))
        .expects(buildResetInventoryWithDocumentTemplateVersion(updatedInventory))
        .returning(DBIOAction.successful(updatedInventory))
        .repeat(1)

      val resetInventory = moderationInventoryManagerMock
        .updateOrCreateModerationInventory(
          ownerRequest.ownerRequestId,
          buildUpdateOrCreateModerationInventoryRequest(updatedApiInventory)
        )
        .futureValue

      resetInventory.getSuccess.getInventory.getVersion shouldBe updatedInventory.version + 1
      resetInventory.getSuccess.getInventory.getConfirmedByOwner shouldBe false
      resetInventory.getSuccess.getInventory.getConfirmedByTenant shouldBe false
    }

    "throw exception when inventory version was changed" in new Wiring with Data {
      val inventory = generateInitialInventory.copy(ownerRequestId = ownerRequest.ownerRequestId)
      val apiInventoryWithChangedConstantParameters =
        inventoryConverter.convertToApiInventory(inventory).toBuilder.setVersion(5).build()
      handleMock(2, None)

      (mockInventoryDbActions
        .selectForUpdate(_: String))
        .expects(*)
        .returning(DBIOAction.successful(Some(inventory)))
        .repeat(1)

      val exception = interceptCause[ConflictApiException] {
        moderationInventoryManagerMock
          .updateOrCreateModerationInventory(
            ownerRequest.ownerRequestId,
            buildUpdateOrCreateModerationInventoryRequest(apiInventoryWithChangedConstantParameters)
          )
          .futureValue
      }
      exception.getMessage shouldEqual buildMatchInventoryVersionExceptionMassege
    }

    "return list of errors when send inventory with incorrect state" in new Wiring with Data {
      val inventory = generateInitialInventory
      val incorrectStateInventory = buildInventoryWithIncorrectState
      val inventoryToUpdate =
        inventoryConverter.convertToApiInventory(buildInventoryWithComment(incorrectStateInventory, ""))
      handleMock(1, None)

      (mockInventoryDbActions
        .selectForUpdate(_: String))
        .expects(*)
        .never

      val response =
        moderationInventoryManagerMock
          .updateOrCreateModerationInventory(
            ownerRequest.ownerRequestId,
            buildUpdateOrCreateModerationInventoryRequest(inventoryToUpdate)
          )
          .futureValue
      val responseErrors = response.getError.getData.getValidationErrorsList.asScala.map(_.getCode)

      responseErrors should contain theSameElementsAs errors
    }

    "throw exception when manager is trying to update confirmed or confirmed by owner inventory" in new Wiring
    with Data {
      generateNotAvailableForUpdateByOwnerAndManagerInventories
        .foreach { il =>
          val inventory = il.copy(ownerRequestId = ownerRequest.ownerRequestId)
          val inventoryToUpdate = inventoryConverter.convertToApiInventory(inventory)

          handleMock(callCount = 1, None)

          (mockInventoryDbActions
            .selectForUpdate(_: String))
            .expects(*)
            .returning(DBIOAction.successful(Some(inventory)))
            .repeat(1)

          val exception = interceptCause[ForbiddenApiException] {
            moderationInventoryManagerMock
              .updateOrCreateModerationInventory(
                ownerRequest.ownerRequestId,
                buildUpdateOrCreateModerationInventoryRequest(inventoryToUpdate)
              )
              .futureValue
          }
          exception.getMessage shouldEqual buildModerationUpdateExceptionMessage
        }
    }
  }

  "InventoryManager.getModerationInventoryByVersionAndOwnerRequestId" should {

    "return inventory with required version for manager" in new Wiring with Data {
      val inventory = generateConfirmedInventory.copy(ownerRequestId = ownerRequest.ownerRequestId)
      val responseInventory = inventoryConverter.convertToApiInventory(inventory)

      inventoryDao.insert(inventory).futureValue
      val response = moderationInventoryManager
        .getModerationInventoryByVersionAndOwnerRequestId(inventory.version, ownerRequest.ownerRequestId)
        .futureValue

      response.getInventory shouldBe responseInventory
    }
  }

  "InventoryManager.getAllModerationInventory" should {

    "return all inventory" in new Wiring with Data {
      val inventories = Seq(
        generateInitialInventory.copy(ownerRequestId = ownerRequest.ownerRequestId),
        generateConfirmedInventory.copy(ownerRequestId = ownerRequest.ownerRequestId),
        generateAfterConfirmInventory.copy(ownerRequestId = ownerRequest.ownerRequestId)
      ).sortBy(-_.version)

      val responseInventories = inventories.map(il => inventoryConverter.convertToApiInventory(il))

      inventories.foreach(il => inventoryDao.insert(il).futureValue)
      val response = moderationInventoryManager
        .getAllModerationInventory(ownerRequest.ownerRequestId, Page(0, 10))
        .futureValue

      response.getInventoriesList.asScala shouldBe responseInventories
      response.getPaging.getTotal shouldBe inventories.size
    }
  }

  "InventoryManager.resetInventory" should {

    "reset inventory" in new Wiring with Data {
      generateAvailableForGetByTenantInventories.foreach { il =>
        val documentId = "documentId"
        val pdfDocumentInfo = PdfDocumentInfo.newBuilder().setDocumentId(documentId).build()
        val inventory = il.copy(
          ownerRequestId = ownerRequest.ownerRequestId,
          data = il.data.toBuilder.setPdfDocumentInfo(pdfDocumentInfo).build()
        )

        inventoryDao.insert(inventory).futureValue
        val resetInventory =
          moderationInventoryManager.resetInventory(ownerRequest.ownerRequestId).futureValue.getInventory

        resetInventory.getVersion shouldBe inventory.version + 1
        resetInventory.getConfirmedByOwner shouldBe false
        resetInventory.getConfirmedByTenant shouldBe false

        val internalResetInventory = inventoryDao.findLastByOwnerRequestId(ownerRequest.ownerRequestId).futureValue

        internalResetInventory.map(_.data.hasPdfDocumentInfo) shouldBe Some(false)
      }
    }

    "throw exception when manager is trying to reset confirmed or initial inventory which need approve by manager" in
      new Wiring with Data {
        Seq(generateInitialInventory, generateConfirmedInventory).foreach { il =>
          val inventory = il.copy(
            ownerRequestId = ownerRequest.ownerRequestId,
            data = il.data.toBuilder.setNeedApproveByManager(true).build()
          )
          inventoryDao.insert(inventory).futureValue

          val exception = interceptCause[ConflictApiException] {
            moderationInventoryManager.resetInventory(ownerRequest.ownerRequestId).futureValue
          }
          exception.getMessage shouldEqual buildModerationResetExceptionMessage
        }
      }
  }

  "InventoryManager.approveInventory" should {

    "approve inventory" in new Wiring with Data {
      val inventory = generateNeedToApproveByManagerInventory.copy(ownerRequestId = ownerRequest.ownerRequestId)

      inventoryDao.insert(inventory).futureValue
      val approvedInventory =
        moderationInventoryManager.approveInventory(ownerRequest.ownerRequestId).futureValue.getInventory

      approvedInventory.getVersion shouldBe inventory.version + 1
      approvedInventory.getConfirmedByOwner shouldBe false
      approvedInventory.getConfirmedByTenant shouldBe false
      approvedInventory.getNeedApproveByManager shouldBe false
    }

    "throw exception when manager is trying to approve inventory that is already approved" in new Wiring with Data {
      val inventory = generateInitialInventory.copy(ownerRequestId = ownerRequest.ownerRequestId)
      inventoryDao.insert(inventory).futureValue

      val exception = interceptCause[ConflictApiException] {
        moderationInventoryManager.approveInventory(ownerRequest.ownerRequestId).futureValue
      }
      exception.getMessage shouldEqual buildModerationApproveExceptionMessage
    }
  }

  private def buildMatchInventoryVersionExceptionMassege = {
    s"Версии описей не совпадают"
  }

  private def buildModerationApproveExceptionMessage = {
    s"Опись уже подтверждена менеджером"
  }

  private def buildModerationResetExceptionMessage = {
    s"Подтверждения описи уже сброшены или опись подписана"
  }

  private def buildModerationUpdateExceptionMessage = {
    s"Опись не может быть изменена во время процесса подтверждения"
  }

  private def buildUpdateOrCreateModerationInventoryRequest(inventory: ApiInventory) = {
    UpdateOrCreateModerationInventoryRequest
      .newBuilder()
      .addAllRooms(inventory.getRoomsList)
      .addAllDefects(inventory.getDefectsList)
      .setManagerComment(inventory.getManagerComment)
      .setVersion(inventory.getVersion)
      .build()
  }
}
