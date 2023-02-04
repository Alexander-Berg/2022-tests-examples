package ru.yandex.realty.rent.backend.manager.inventory

import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.features.FeaturesStubComponent
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.rent.backend.converter.InventoryConverter
import ru.yandex.realty.rent.backend.inventory.InventoryPdfManager
import ru.yandex.realty.rent.backend.manager.Wiring
import ru.yandex.realty.rent.dao.{CleanSchemaBeforeEach, RentSpecBase}
import ru.yandex.realty.rent.dao.actions.{FlatShowingDbActions, InventoryDbActions}
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.Inventory
import ru.yandex.realty.rent.dao.impl.InventoryDaoImpl
import ru.yandex.realty.rent.model.enums.OwnerRequestStatus.OwnerRequestStatus
import ru.yandex.realty.rent.model.enums.Role
import ru.yandex.realty.rent.proto.api.inventory.InventoryStateErrorNameSpace
import ru.yandex.vertis.protobuf.BasicProtoFormats.DateTimeFormat
import ru.yandex.realty.util.Mappings.MapAny

import scala.jdk.CollectionConverters._
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

class CommonInventoryManagerSpec
  extends AsyncSpecBase
  with RequestAware
  with CleanSchemaBeforeEach
  with FeaturesStubComponent
  with RentSpecBase
  with RentModelsGen {

  trait Data { self: Wiring =>

    features.AllowEditConfirmedInventory.setNewState(true)

    protected val mockInventoryDbActions: InventoryDbActions = mock[InventoryDbActions]
    protected val flatShowingDbActionsMock: FlatShowingDbActions = mock[FlatShowingDbActions]
    protected val inventoryDao =
      new InventoryDaoImpl(inventoryDbActions, flatShowingDbActionsMock, masterSlaveDb2, daoMetrics)
    protected val inventoryDaoMock =
      new InventoryDaoImpl(mockInventoryDbActions, flatShowingDbActionsMock, masterSlaveDb2, daoMetrics)

    val inventoryConverter = new InventoryConverter(mockImageConverter)
    protected val inventoryPdfManager = mock[InventoryPdfManager]

    val commonInventoryManager = new CommonInventoryManager(inventoryDao)

    protected val inventoryManager =
      new DefaultInventoryManager(
        inventoryDao,
        flatDao,
        ownerRequestDao,
        smsConfirmationManager,
        inventoryConverter,
        commonInventoryManager,
        features
      )

    protected val moderationInventoryManager =
      new DefaultModerationInventoryManager(
        inventoryDao,
        inventoryConverter,
        commonInventoryManager,
        features
      )

    protected val commonInventoryManagerMock = new CommonInventoryManager(inventoryDaoMock)

    protected val inventoryManagerMock =
      new DefaultInventoryManager(
        inventoryDaoMock,
        flatDao,
        ownerRequestDao,
        smsConfirmationManager,
        inventoryConverter,
        commonInventoryManagerMock,
        features
      )

    protected val moderationInventoryManagerMock =
      new DefaultModerationInventoryManager(
        inventoryDaoMock,
        inventoryConverter,
        commonInventoryManagerMock,
        features
      )

    protected val tenant = userGen().next.copy(
      phone = Some(readableString.next),
      name = Some(readableString.next),
      surname = Some(readableString.next),
      patronymic = Some(readableString.next),
      fullName = Some(readableString.next),
      email = Some(readableString.next)
    )
    protected val owner = userGen().next.copy(
      phone = Some(readableString.next),
      name = Some(readableString.next),
      surname = Some(readableString.next),
      patronymic = Some(readableString.next),
      fullName = Some(readableString.next),
      email = Some(readableString.next)
    )
    protected val previousTenant = userGen().next
    protected val previousOwner = userGen().next
    protected val tenantCandidate = userGen().next
    protected val additionalTenant = userGen().next
    protected val confidant = userGen().next
    protected val unknown = userGen().next

    protected val sampleFlat = flatGen().next
    protected val ownerRequest = ownerRequestGen.next.copy(flatId = sampleFlat.flatId)

    protected val errors = Seq(
      InventoryStateErrorNameSpace.ErrorCode.EMPTY_ROOM_NAME_ERROR,
      InventoryStateErrorNameSpace.ErrorCode.EMPTY_ITEM_NAME_ERROR,
      InventoryStateErrorNameSpace.ErrorCode.EMPTY_DEFECT_DESCRIPTION_ERROR
    )

    protected val flatWithAssignedUsers =
      sampleFlat.copy(
        assignedUsers = Map(
          Role.Owner -> Seq(owner),
          Role.Tenant -> Seq(tenant),
          Role.PreviousTenant -> Seq(previousTenant),
          Role.PreviousOwner -> Seq(previousOwner),
          Role.TenantCandidate -> Seq(tenantCandidate),
          Role.AdditionalTenant -> Seq(additionalTenant),
          Role.Confidant -> Seq(confidant),
          Role.Unknown -> Seq(unknown)
        )
      )

    protected val allUsersAndRoles =
      Seq(
        (owner, Role.Owner),
        (tenant, Role.Tenant),
        (previousOwner, Role.PreviousOwner),
        (previousTenant, Role.PreviousTenant),
        (tenantCandidate, Role.TenantCandidate),
        (additionalTenant, Role.AdditionalTenant),
        (confidant, Role.Confidant),
        (unknown, Role.Unknown)
      )

    protected val suitableUsersAndRolesForGetRequest = Seq(
      (owner, Role.Owner),
      (tenant, Role.Tenant),
      (previousOwner, Role.PreviousOwner),
      (previousTenant, Role.PreviousTenant)
    )

    protected val unsuitableUsersAndRolesForGetRequest =
      allUsersAndRoles.filter(userAndRole => !suitableUsersAndRolesForGetRequest.contains(userAndRole))

    protected val suitableUsersAndRolesForUpdateRequest = Seq(
      (owner, Role.Owner)
    )

    protected val unsuitableUsersAndRolesForUpdateRequest =
      allUsersAndRoles.filter(userAndRole => !suitableUsersAndRolesForUpdateRequest.contains(userAndRole))

    def handleMock(callCount: Int, ownerRequestStatus: Option[OwnerRequestStatus]) = {
      (flatDao
        .findByIdOpt(_: String)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(Some(flatWithAssignedUsers)))
        .repeat(callCount)

      (ownerRequestDao
        .findByIdOpt(_: String)(_: Traced))
        .expects(*, *)
        .returning(
          Future.successful(
            if (ownerRequestStatus.isDefined) Some(ownerRequest.copy(status = ownerRequestStatus.get))
            else Some(ownerRequest)
          )
        )
        .repeat(callCount)
    }
  }

  def buildResetInventoryWithDocumentTemplateVersion(inventory: Inventory) = {
    inventory.copy(
      data = inventory.data.toBuilder
        .setVersion(inventory.version + 1)
        .clearConfirmedByOwnerDate()
        .clearConfirmedByTenantDate()
        .build(),
      version = inventory.version + 1
    )
  }

  def buildUpdatedInventory(inventory: Inventory) = {
    val updated = inventoryGen.next

    updated.copy(
      version = inventory.version,
      data = updated.data.toBuilder
        .setVersion(inventory.version)
        .applyTransformIf(
          inventory.data.hasConfirmedByOwnerDate,
          _.setConfirmedByOwnerDate(inventory.data.getConfirmedByOwnerDate)
        )
        .applyTransformIf(
          inventory.data.hasConfirmedByTenantDate,
          _.setConfirmedByOwnerDate(inventory.data.getConfirmedByTenantDate)
        )
        .setManagerComment("")
        .build()
    )
  }

  def buildInventoryWithComment(inventory: Inventory, comment: String) = {
    inventory.copy(data = inventory.data.toBuilder.setManagerComment(comment).build())
  }

  def generateAvailableForGetByTenantInventories = {
    Seq(generateConfirmedByOwnerInventory, generateAfterConfirmInventory)
  }

  def buildInventoryWithIncorrectState = {
    val items = Seq(inventoryItemGen.next.toBuilder.setItemName("").clearPhotos().build()).asJava
    val rooms = Seq(inventoryRoomGen.next.toBuilder.setRoomName("").addAllItems(items).build()).asJava
    val defects = Seq(inventoryDefectGen.next.toBuilder.setDescription("").build()).asJava

    val inventory = generateInitialInventory

    inventory.copy(data = inventory.data.toBuilder.addAllRooms(rooms).addAllDefects(defects).build())
  }

  def generateInventory(
    version: Int,
    confirmedByOwner: Boolean,
    confirmedByTenant: Boolean,
    needApproveByManager: Boolean = false
  ): Inventory = {
    val sample = inventoryGen.next
    val sampleData = sample.data
    val data = sampleData.toBuilder
      .setVersion(version)
      .applySideEffectIf(
        confirmedByOwner,
        _.setConfirmedByOwnerDate(DateTimeFormat.defaultInstance)
          .applySideEffect(_.getPdfDocumentInfoBuilder.setDocumentId("docId"))
      )
      .applySideEffectIf(
        confirmedByTenant,
        _.setConfirmedByTenantDate(DateTimeFormat.defaultInstance)
          .applySideEffect(_.getPdfDocumentInfoBuilder.setDocumentId("docId"))
      )
      .setNeedApproveByManager(needApproveByManager)
      .build()

    sample.copy(
      data = data,
      version = version
    )
  }

  def generateNeedToApproveByManagerInventory =
    generateInventory(version = 50, confirmedByOwner = false, confirmedByTenant = false, needApproveByManager = true)

  def generateConfirmedByOwnerInventory =
    generateInventory(version = 10, confirmedByOwner = true, confirmedByTenant = false)

  def generateAvailableForConfirmByOwnerInventories = {
    Seq(generateInitialInventory)
  }

  def generateNotAvailableForConfirmByTenantInventories = {
    Seq(generateInitialInventory, generateConfirmedInventory, generateAfterConfirmInventory)
  }

  def generateNotAvailableForUpdateByOwnerAndManagerInventories = {
    Seq(generateConfirmedByOwnerInventory)
  }

  def generateNotConfirmedInventories = {
    Seq(generateInitialInventory, generateConfirmedByOwnerInventory, generateAfterConfirmInventory)
  }

  def generateConfirmedInventory =
    generateInventory(version = 20, confirmedByOwner = true, confirmedByTenant = true)

  def generateAfterConfirmInventory =
    generateInventory(version = 30, confirmedByOwner = false, confirmedByTenant = true)

  def generateInitialInventory =
    generateInventory(version = 0, confirmedByOwner = false, confirmedByTenant = false)

}
