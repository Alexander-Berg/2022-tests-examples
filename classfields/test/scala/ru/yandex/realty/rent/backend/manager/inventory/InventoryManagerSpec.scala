package ru.yandex.realty.rent.backend.manager.inventory

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import scala.jdk.CollectionConverters._
import ru.yandex.realty.errors.{ConflictApiException, ForbiddenApiException}
import ru.yandex.realty.model.util.{Page, SlicedResult}
import ru.yandex.realty.proto.PersonFullName
import ru.yandex.realty.rent.backend.manager.Wiring
import ru.yandex.realty.rent.model.{FlatShowing, Inventory, OwnerRequest, User}
import ru.yandex.realty.rent.model.enums.{FlatShowingStatus, OwnerRequestStatus, Role}
import ru.yandex.realty.rent.model.enums.Role.Role
import ru.yandex.realty.rent.proto.api.inventory.{
  ActionNameSpace,
  UpdateOrCreateInventoryRequest,
  Inventory => ApiInventory
}
import ru.yandex.realty.rent.proto.api.sms.confirmation.ConfirmSmsInfo
import ru.yandex.realty.rent.proto.model.sms.confirmation.{InventoryConfirmationPayload, SmsConfirmation}
import slick.dbio.DBIOAction

@RunWith(classOf[JUnitRunner])
class InventoryManagerSpec extends CommonInventoryManagerSpec {

  "InventoryManager.getInventory" should {

    "return confirmed inventory for tenant and owner" in new Wiring with Data {
      val confirmedInventory = generateConfirmedInventory.copy(ownerRequestId = ownerRequest.ownerRequestId)
      val resultInventory = inventoryConverter.convertToApiInventory(confirmedInventory)
      handleMock(4, Some(OwnerRequestStatus.Completed))

      inventoryDao.insert(confirmedInventory).futureValue

      suitableUsersAndRolesForGetRequest.foreach {
        case (user, role) =>
          val response = inventoryManager.getConfirmedInventory(user.uid, ownerRequest.ownerRequestId).futureValue

          response.getInventory shouldBe resultInventory
          response.getActionsList.asScala shouldBe identifyAllowedActions(
            role,
            confirmedInventory,
            ownerRequest.copy(status = OwnerRequestStatus.Completed),
            isReadOnly = false
          )
      }
    }

    "return not confirmed inventory for owner" in new Wiring with Data {
      generateNotConfirmedInventories
        .foreach { il =>
          val inventory = il.copy(ownerRequestId = ownerRequest.ownerRequestId)
          val resultInventory = inventoryConverter.convertToApiInventory(inventory)
          handleMock(callCount = 3, None)

          inventoryDao.insert(inventory).futureValue

          val ownerResponse =
            inventoryManager.getLastInventory(owner.uid, ownerRequest.ownerRequestId).futureValue

          ownerResponse.getInventory shouldBe resultInventory
          ownerResponse.getActionsList.asScala shouldBe identifyAllowedActions(
            Role.Owner,
            inventory,
            ownerRequest,
            isReadOnly = false
          )
        }
    }

    "return not confirmed inventory for tenant" in new Wiring with Data {
      generateAvailableForGetByTenantInventories
        .foreach { il =>
          val inventory = il.copy(ownerRequestId = ownerRequest.ownerRequestId)
          handleMock(callCount = 1, None)

          inventoryDao.insert(inventory).futureValue

          val response =
            inventoryManager.getLastInventory(tenant.uid, ownerRequest.ownerRequestId).futureValue

          response.getInventory shouldBe inventoryConverter.convertToApiInventory(inventory)
          response.getActionsList.asScala shouldBe identifyAllowedActions(
            Role.Tenant,
            inventory,
            ownerRequest,
            isReadOnly = false
          )
        }
    }

    "throw exception when tenant is trying to get initial inventory" in new Wiring with Data {
      val inventory = generateInitialInventory.copy(ownerRequestId = ownerRequest.ownerRequestId)
      handleMock(callCount = 2, None)

      inventoryDao.insert(inventory).futureValue

      val exception = interceptCause[ForbiddenApiException] {
        inventoryManager.getLastInventory(tenant.uid, ownerRequest.ownerRequestId).futureValue
      }
      exception.getMessage shouldEqual
        buildGetExceptionMassage(tenant, Role.Tenant, ownerRequest)
    }

    "throw exception for unsuitable roles" in new Wiring with Data {
      val inventory = generateConfirmedInventory.copy(ownerRequestId = ownerRequest.ownerRequestId)
      inventoryDao.insert(inventory).futureValue
      handleMock(callCount = unsuitableUsersAndRolesForGetRequest.size, None)

      unsuitableUsersAndRolesForGetRequest.foreach {
        case (user, role) =>
          val exception = interceptCause[ForbiddenApiException] {
            inventoryManager.getLastInventory(user.uid, ownerRequest.ownerRequestId).futureValue
          }
          exception.getMessage shouldEqual
            buildUnsuitableRoleForGetRequestExceptionMessage(role, ownerRequest)
      }
    }
  }

  "InventoryManager.getInventoryByVersionAndOwnerRequestId" should {

    "return editable inventory with required version" in new Wiring with Data {
      val confirmedInventory = generateConfirmedInventory.copy(ownerRequestId = ownerRequest.ownerRequestId)
      val responseInventory = inventoryConverter.convertToApiInventory(confirmedInventory)
      handleMock(1, Some(OwnerRequestStatus.Completed))

      inventoryDao.insert(confirmedInventory).futureValue
      val response = inventoryManager
        .getInventoryByVersionAndOwnerRequestId(owner.uid, confirmedInventory.version, ownerRequest.ownerRequestId)
        .futureValue

      response.getInventory shouldBe responseInventory
      response.getActionsList.asScala shouldBe identifyAllowedActions(
        Role.Owner,
        confirmedInventory,
        ownerRequest,
        isReadOnly = false
      )
    }

    "return read only inventory with required version" in new Wiring with Data {
      val confirmedInventory = generateConfirmedInventory.copy(ownerRequestId = ownerRequest.ownerRequestId)
      val responseInventory = inventoryConverter.convertToApiInventory(confirmedInventory)
      val inventory = generateAfterConfirmInventory.copy(ownerRequestId = ownerRequest.ownerRequestId)
      handleMock(1, Some(OwnerRequestStatus.Completed))

      inventoryDao.insert(confirmedInventory).futureValue
      inventoryDao.insert(inventory).futureValue
      val response = inventoryManager
        .getInventoryByVersionAndOwnerRequestId(owner.uid, confirmedInventory.version, ownerRequest.ownerRequestId)
        .futureValue

      response.getInventory shouldBe responseInventory
      response.getActionsList.asScala shouldBe identifyAllowedActions(
        Role.Owner,
        confirmedInventory,
        ownerRequest,
        isReadOnly = true
      )
    }
  }

  "InventoryManager.updateOrCreateInventory" should {

    "create inventory by owner in" in new Wiring with Data {
      val inventory = generateInitialInventory
      val apiInventory = inventoryConverter.convertToApiInventory(buildInventoryWithComment(inventory, ""))
      handleMock(2, None)

      inventoryManager
        .updateOrCreateInventory(
          owner.uid,
          ownerRequest.ownerRequestId,
          buildUpdateOrCreateInventoryRequest(apiInventory)
        )
        .futureValue

      val response = inventoryManager.getLastInventory(owner.uid, ownerRequest.ownerRequestId).futureValue
      response.getInventory shouldBe apiInventory
    }

    "update inventory by owner" in new Wiring with Data {
      val inventory = generateInitialInventory
      val inventoryToInsert = inventoryConverter.convertToApiInventory(buildInventoryWithComment(inventory, ""))
      val updatedInventory = inventoryConverter.convertToApiInventory(buildUpdatedInventory(inventory))
      handleMock(2, None)

      inventoryManager
        .updateOrCreateInventory(
          owner.uid,
          ownerRequest.ownerRequestId,
          buildUpdateOrCreateInventoryRequest(inventoryToInsert)
        )
        .futureValue
      val updateResponse = inventoryManager
        .updateOrCreateInventory(
          owner.uid,
          ownerRequest.ownerRequestId,
          buildUpdateOrCreateInventoryRequest(updatedInventory)
        )
        .futureValue

      updateResponse.getSuccess.getInventory shouldBe updatedInventory
      updateResponse.getSuccess.getActionsList.asScala shouldBe identifyAllowedActions(
        Role.Owner,
        inventory,
        ownerRequest,
        isReadOnly = false
      )
      updateResponse.getSuccess.getActionsList.asScala shouldBe identifyAllowedActions(
        Role.Owner,
        inventory,
        ownerRequest,
        isReadOnly = false
      )
    }

    "reset inventory when owner updates confirmed inventory" in new Wiring with Data {
      val inventory = generateConfirmedInventory
      val updatedInventory = inventory.copy(data = buildUpdatedInventory(inventory).data)
      val updatedApiInventory = inventoryConverter.convertToApiInventory(updatedInventory)
      handleMock(1, None)

      (mockInventoryDbActions
        .selectForUpdate(_: String))
        .expects(*)
        .returning(DBIOAction.successful(Some(inventory)))
        .repeat(1)

      (mockInventoryDbActions
        .findAllByOwnerRequestId(_: String, _: Page))
        .expects(*, *)
        .returning(DBIOAction.successful(SlicedResult(Iterable(inventory), 1, Page(0, 1))))
        .repeat(1)

      (mockInventoryDbActions
        .insert(_: Inventory))
        .expects(buildResetInventoryWithDocumentTemplateVersion(updatedInventory))
        .returning(DBIOAction.successful(updatedInventory))
        .repeat(1)

      val resetInventory = inventoryManagerMock
        .updateOrCreateInventory(
          owner.uid,
          ownerRequest.ownerRequestId,
          buildUpdateOrCreateInventoryRequest(updatedApiInventory)
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
      handleMock(1, None)

      (mockInventoryDbActions
        .selectForUpdate(_: String))
        .expects(*)
        .returning(DBIOAction.successful(Some(inventory)))
        .repeat(1)

      val exception = interceptCause[ConflictApiException] {
        inventoryManagerMock
          .updateOrCreateInventory(
            owner.uid,
            ownerRequest.ownerRequestId,
            buildUpdateOrCreateInventoryRequest(apiInventoryWithChangedConstantParameters)
          )
          .futureValue
      }
      exception.getMessage shouldEqual buildMatchInventoryVersionExceptionMassage
    }

    "throw exception when owner is trying to update confirmed by owner inventory" in new Wiring with Data {
      generateNotAvailableForUpdateByOwnerAndManagerInventories.foreach { il =>
        val inventory = il.copy(ownerRequestId = ownerRequest.ownerRequestId)
        val inventoryToUpdate = inventoryConverter.convertToApiInventory(buildInventoryWithComment(inventory, ""))
        handleMock(callCount = 1, None)

        (mockInventoryDbActions
          .selectForUpdate(_: String))
          .expects(*)
          .returning(DBIOAction.successful(Some(inventory)))
          .repeat(1)

        val exception = interceptCause[ForbiddenApiException] {
          inventoryManagerMock
            .updateOrCreateInventory(
              owner.uid,
              ownerRequest.ownerRequestId,
              buildUpdateOrCreateInventoryRequest(inventoryToUpdate)
            )
            .futureValue
        }
        exception.getMessage shouldEqual
          buildUpdateInventoryStatusExceptionMassage(owner, Role.Owner, ownerRequest)
      }
    }

    "throw exception when tenant is trying to update any status inventory" in new Wiring with Data {
      generateNotConfirmedInventories
        .foreach { il =>
          val inventory = il.copy(ownerRequestId = ownerRequest.ownerRequestId)
          val inventoryToUpdate = inventoryConverter.convertToApiInventory(inventory)
          handleMock(callCount = 3, None)

          (mockInventoryDbActions
            .selectForUpdate(_: String))
            .expects(*)
            .returning(DBIOAction.successful(Some(inventory)))
            .repeat(3)

          val exception = interceptCause[ForbiddenApiException] {
            inventoryManagerMock
              .updateOrCreateInventory(
                tenant.uid,
                ownerRequest.ownerRequestId,
                buildUpdateOrCreateInventoryRequest(inventoryToUpdate)
              )
              .futureValue
          }
          exception.getMessage shouldEqual
            buildUpdateUserRoleExceptionMessage(Role.Tenant, ownerRequest)
        }
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
        inventoryManagerMock
          .updateOrCreateInventory(
            owner.uid,
            ownerRequest.ownerRequestId,
            buildUpdateOrCreateInventoryRequest(inventoryToUpdate)
          )
          .futureValue
      val responseErrors = response.getError.getData.getValidationErrorsList.asScala.map(_.getCode)

      responseErrors should contain theSameElementsAs errors
    }

    "throw exception for unsuitable roles" in new Wiring with Data {
      val inventory = generateInitialInventory.copy(ownerRequestId = ownerRequest.ownerRequestId)
      inventoryDao.insert(inventory).futureValue
      val updatedInventory = inventoryConverter.convertToApiInventory(buildUpdatedInventory(inventory))
      handleMock(callCount = unsuitableUsersAndRolesForUpdateRequest.size, None)

      unsuitableUsersAndRolesForUpdateRequest.foreach {
        case (user, role) =>
          val exception = interceptCause[ForbiddenApiException] {
            inventoryManagerMock
              .updateOrCreateInventory(
                user.uid,
                ownerRequest.ownerRequestId,
                buildUpdateOrCreateInventoryRequest(updatedInventory)
              )
              .futureValue
          }
          exception.getMessage shouldEqual
            buildUnsuitableRoleForUpdateExceptionMassage(role, ownerRequest)
      }
    }
  }

  "InventoryManager.askInventorySmsConfirmation and .confirmInventory" should {

    "return confirmation sms info and confirm inventory for owner" in new Wiring with Data {
      val askCount = Range(0, 3)

      generateAvailableForConfirmByOwnerInventories
        .foreach { il =>
          val inventory = il.copy(ownerRequestId = ownerRequest.ownerRequestId)
          handleMock(4, None)

          inventoryDao.insert(inventory).futureValue

          askCount.map { _ =>
            inventoryManager
              .askSmsConfirmation(owner.uid, ownerRequest.ownerRequestId)
              .futureValue
          }

          val inventoryWithConfirmation =
            inventoryDao.findLastByOwnerRequestId(ownerRequest.ownerRequestId).futureValue
          val confirmations = getConfirmations(inventoryWithConfirmation)

          confirmations.size shouldBe askCount.size

          val lastConfirmation = confirmations.head
          val payload = lastConfirmation.getPayload.getInventoryConfirmation

          payloadAssertions(inventory, payload, owner)

          val confirmSmsInfo = buildConfirmSmsInfo(lastConfirmation, lastConfirmation.getConfirmationCode)
          inventoryManager.confirmInventory(owner.uid, ownerRequest.ownerRequestId, confirmSmsInfo).futureValue
          val resultConfirmedInventory =
            inventoryDao.findLastByOwnerRequestId(ownerRequest.ownerRequestId).futureValue
          val resultConfirmations = getConfirmations(resultConfirmedInventory)

          resultConfirmations.size shouldBe 3
          resultConfirmedInventory.map(_.isConfirmedByOwner) shouldBe Some(true)
          resultConfirmedInventory.map(_.isConfirmedByTenant) shouldBe Some(false)
          resultConfirmedInventory.map(_.needCreateOwnerDocument) shouldBe Some(true)
        }
    }

    "throw exception when owner is trying to ask sms confirmation for inventory confirmed by owner" in new Wiring
    with Data {
      val inventory = generateConfirmedByOwnerInventory.copy(ownerRequestId = ownerRequest.ownerRequestId)
      handleMock(callCount = 1, None)

      inventoryDao.insert(inventory).futureValue

      val exception = interceptCause[ForbiddenApiException] {
        inventoryManager
          .askSmsConfirmation(owner.uid, ownerRequest.ownerRequestId)
          .futureValue
      }
      exception.getMessage shouldEqual
        buildConfirmationExceptionMessage(owner.uid, Role.Owner, ownerRequest)
    }

    "return confirmation sms info and confirm for tenant" in new Wiring with Data {
      val askCount = Range(0, 1)
      val inventory = generateConfirmedByOwnerInventory.copy(ownerRequestId = ownerRequest.ownerRequestId)
      val flatShowing = {
        val sh = flatShowingGen.next
        sh.copy(status = FlatShowingStatus.Settled)
      }

      (flatShowingDbActionsMock
        .findApprovedByOwnerRequests(_: Set[String]))
        .expects(*)
        .returning(DBIOAction.successful(Seq(flatShowing)))
        .once()
      (flatShowingDbActionsMock
        .selectForUpdate(_: String))
        .expects(*)
        .returning(DBIOAction.successful(flatShowing))
        .once()
      (flatShowingDbActionsMock
        .update(_: FlatShowing))
        .expects(*)
        .returning(DBIOAction.successful(1))
        .once()

      handleMock(2, None)
      inventoryDao.insert(inventory).futureValue

      askCount.map { _ =>
        inventoryManager
          .askSmsConfirmation(tenant.uid, ownerRequest.ownerRequestId)
          .futureValue
      }

      val inventoryWithConfirmation =
        inventoryDao.findLastByOwnerRequestId(ownerRequest.ownerRequestId).futureValue
      val confirmations = getConfirmations(inventoryWithConfirmation)

      confirmations.size shouldBe askCount.size

      val lastConfirmation = confirmations.head
      val payload = lastConfirmation.getPayload.getInventoryConfirmation

      payloadAssertions(inventory, payload, tenant)

      val confirmSmsInfo = buildConfirmSmsInfo(lastConfirmation, lastConfirmation.getConfirmationCode)
      inventoryManager.confirmInventory(tenant.uid, ownerRequest.ownerRequestId, confirmSmsInfo).futureValue
      val resultConfirmedInventory =
        inventoryDao.findLastConfirmedByOwnerRequestId(ownerRequest.ownerRequestId).futureValue
      val resultConfirmations = getConfirmations(resultConfirmedInventory)

      resultConfirmations.size shouldBe askCount.size
      resultConfirmedInventory.map(_.isConfirmedByOwner) shouldBe Some(true)
      resultConfirmedInventory.map(_.isConfirmedByTenant) shouldBe Some(true)
      resultConfirmedInventory.map(_.needCreateTenantDocument) shouldBe Some(true)
    }

    """
          |throw exception when tenant is trying to ask sms confirmation
          |for not confirmed by owner or confirmed by tenant inventory
    """.stripMargin in new Wiring with Data {
      generateNotAvailableForConfirmByTenantInventories
        .foreach { il =>
          val inventory = il.copy(ownerRequestId = ownerRequest.ownerRequestId)
          handleMock(callCount = 2, None)

          inventoryDao.insert(inventory).futureValue

          val exception = interceptCause[ForbiddenApiException] {
            inventoryManager
              .askSmsConfirmation(tenant.uid, ownerRequest.ownerRequestId)
              .futureValue
          }
          exception.getMessage shouldEqual
            buildConfirmationExceptionMessage(tenant.uid, Role.Tenant, ownerRequest)
        }
    }
  }

  private def buildConfirmSmsInfo(lastConfirmation: SmsConfirmation, code: String) = {
    ConfirmSmsInfo.newBuilder().setRequestId(lastConfirmation.getRequestId).setCode(code).build()
  }

  private def getConfirmations(inventoryWithConfirmation: Option[Inventory]) = {
    inventoryWithConfirmation
      .map(_.data.getSmsConfirmationsList.asScala)
      .getOrElse(Seq.empty)
  }

  private def payloadAssertions(inventory: Inventory, payload: InventoryConfirmationPayload, user: User) = {
    payload.getUserUid shouldBe user.uid
    Some(payload.getPhone) shouldBe user.phone
    payload.getInventoryVersion shouldBe inventory.version
    payload.getPerson shouldBe buildPerson(user)
  }

  private def buildPerson(owner: User) = {
    PersonFullName
      .newBuilder()
      .setName(owner.name.getOrElse(""))
      .setSurname(owner.surname.getOrElse(""))
      .setPatronymic(owner.patronymic.getOrElse(""))
      .build()
  }

  private def buildMatchInventoryVersionExceptionMassage = {
    s"Versions do not match"
  }

  private def buildConfirmationExceptionMessage(uid: Long, role: Role, ownerRequest: OwnerRequest) = {
    s"User $uid with role $role can't confirm inventory for owner request ${ownerRequest.ownerRequestId} now"
  }

  private def buildUpdateUserRoleExceptionMessage(role: Role, ownerRequest: OwnerRequest) = {
    s"Incorrect user role $role to modify inventory for owner request ${ownerRequest.ownerRequestId}"
  }

  private def buildGetExceptionMassage(tenant: User, role: Role, ownerRequest: OwnerRequest) = {
    s"Inventory is not completed for user ${tenant.uid} with role $role and owner request ${ownerRequest.ownerRequestId}"
  }

  private def buildUpdateInventoryStatusExceptionMassage(owner: User, role: Role, ownerRequest: OwnerRequest) = {
    s"User ${owner.uid} with role $role can't edit inventory for owner request ${ownerRequest.ownerRequestId} now"
  }

  private def buildUnsuitableRoleForGetRequestExceptionMessage(role: Role, ownerRequest: OwnerRequest) = {
    s"Incorrect user role $role to get inventory for owner request ${ownerRequest.ownerRequestId}"
  }

  private def buildUnsuitableRoleForUpdateExceptionMassage(role: Role, ownerRequest: OwnerRequest) = {
    s"Incorrect user role $role to modify inventory for owner request ${ownerRequest.ownerRequestId}"
  }

  private def identifyAllowedActions(
    role: Role,
    inventory: Inventory,
    ownerRequest: OwnerRequest,
    isReadOnly: Boolean
  ): Seq[ActionNameSpace.Action] = {
    if (isReadOnly) Seq.empty
    else
      role match {
        case Role.Owner =>
          if (inventory.isConfirmedByOwnerOnly || inventory.isConfirmedByTenantOnly) Seq.empty
          else if (inventory.isInitial) {
            if (OwnerRequestStatus.BeforeLookingForTenantConfirmedStatuses.contains(ownerRequest.status))
              Seq(ActionNameSpace.Action.EDIT)
            else if (OwnerRequestStatus.AfterLookingForTenantConfirmedStatuses.contains(ownerRequest.status))
              Seq(ActionNameSpace.Action.EDIT, ActionNameSpace.Action.CONFIRM)
            else Seq.empty
          } else Seq(ActionNameSpace.Action.EDIT, ActionNameSpace.Action.DOWNLOAD)
        case Role.Tenant =>
          if (ownerRequest.status == OwnerRequestStatus.Completed) {
            if (inventory.isConfirmedByOwnerOnly) {
              Seq(ActionNameSpace.Action.CONFIRM)
            } else if (inventory.isConfirmed) {
              Seq(ActionNameSpace.Action.DOWNLOAD)
            } else Seq.empty
          } else Seq.empty
        case _ => Seq.empty
      }
  }

  private def buildUpdateOrCreateInventoryRequest(inventory: ApiInventory) = {
    UpdateOrCreateInventoryRequest
      .newBuilder()
      .addAllRooms(inventory.getRoomsList)
      .addAllDefects(inventory.getDefectsList)
      .setVersion(inventory.getVersion)
      .build()
  }
}
