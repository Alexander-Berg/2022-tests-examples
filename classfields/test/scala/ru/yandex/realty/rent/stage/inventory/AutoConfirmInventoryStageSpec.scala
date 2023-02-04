package ru.yandex.realty.rent.stage.inventory

import com.google.protobuf.Timestamp
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.features.FeaturesStubComponent
import ru.yandex.realty.watching.ProcessingState
import ru.yandex.realty.rent.dao.{FlatShowingDao, InventoryDao, RentContractDao}
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.{FlatShowing, Inventory}
import ru.yandex.realty.rent.model.enums.ContractStatus
import ru.yandex.realty.rent.model.enums.ContractStatus.ContractStatus
import ru.yandex.realty.rent.stage.inventory.AutoConfirmInventoryStage.AutoConfirmInventoryConfig
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.protobuf.BasicProtoFormats.DateTimeFormat
import ru.yandex.vertis.util.time.DateTimeUtil

import java.time.Duration
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class AutoConfirmInventoryStageSpec extends AsyncSpecBase with RentModelsGen with FeaturesStubComponent {

  features.AutoConfirmInventory.setNewState(true)

  "AutoConfirmInventoryStage" should {
    "confirm tenant inventory version" in new Data {
      doMock(ContractStatus.Active, needAutoConfirmDate, tenantVersionInventory, None, 1)
      (inventoryDao
        .confirm(_: String, _: Boolean)(_: Inventory => Inventory)(_: Traced))
        .expects(*, false, *, *)
        .onCall((_, _, confirm: Inventory => Inventory, _) => Future.successful(Some(confirm(tenantVersionInventory))))
        .once()

      val result = invokeStage(tenantVersionInventory)

      result.entry.isConfirmedByTenant shouldBe true
    }

    "not confirm owner inventory version" in new Data {
      doMock(ContractStatus.Signing, needAutoConfirmDate, tenantVersionInventory, None, 1)

      val result = invokeStage(ownerVersionInventory)

      result.entry shouldBe ownerVersionInventory.copy(visitTime = result.entry.visitTime)
    }

    "reschedule confirmation by check in date" in new Data {
      doMock(ContractStatus.Active, needTenantConfirmDate, tenantVersionInventory, None, 1)

      val result = invokeStage(tenantVersionInventory)

      result.entry.isConfirmedByTenant shouldBe false
      result.entry.visitTime shouldBe Some(revisitDate)
    }

    "reschedule confirmation by confirmed by owner date" in new Data {
      doMock(ContractStatus.Active, needAutoConfirmDate, confirmedByOwnerAfterCheckInDateInventory, None, 1)

      val result = invokeStage(confirmedByOwnerAfterCheckInDateInventory)

      result.entry.isConfirmedByTenant shouldBe false
      result.entry.visitTime shouldBe Some(confirmedByOwnerRevisitDate)
    }

    "reschedule confirmation when rent contract is not active" in new Data {
      doMock(ContractStatus.Signing, needAutoConfirmDate, tenantVersionInventory, None, 1)

      val result = invokeStage(tenantVersionInventory)

      result.entry.isConfirmedByTenant shouldBe false
      result.entry.visitTime.map(_.formatted("yyyy.MM.dd.hh")) shouldBe Some(
        revisitByContractStatusDate.formatted("yyyy.MM.dd.hh")
      )
    }

    "reschedule confirmation when rent contract is absent" in new Data {
      (rentContractDao
        .findByOwnerRequestIdOpt(_: String)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(None))
        .once()

      val result = invokeStage(tenantVersionInventory)

      result.entry.isConfirmedByTenant shouldBe false
      result.entry.visitTime.map(_.formatted("yyyy.MM.dd.hh")) shouldBe Some(
        revisitByContractStatusDate.formatted("yyyy.MM.dd.hh")
      )
    }

    "stop reschedule when contract is cancelled" in new Data {
      doMock(ContractStatus.Cancelled, needAutoConfirmDate, tenantVersionInventory, None, 1)

      val result = invokeStage(tenantVersionInventory)

      result.entry.isConfirmedByTenant shouldBe false
      result.entry.visitTime shouldBe None
    }

    "not confirm when confirmed inventory exist" in new Data {
      doMock(ContractStatus.Signing, needAutoConfirmDate, tenantVersionInventory, Some(tenantVersionInventory), 0)

      val result = invokeStage(tenantVersionInventory)

      result.entry shouldBe tenantVersionInventory.copy(visitTime = None)
    }
  }

  trait Data {
    implicit val traced: Traced = Traced.empty

    protected val rentContractDao: RentContractDao = mock[RentContractDao]
    protected val inventoryDao: InventoryDao = mock[InventoryDao]
    protected val flatShowingDao: FlatShowingDao = mock[FlatShowingDao]

    val confirmAfter = 2
    val now = DateTimeUtil.now()
    val needAutoConfirmDate = DateTimeFormat.write(now.minusDays(confirmAfter).minusMinutes(10))
    val needTenantConfirmDate = DateTimeFormat.write(now.minusDays(confirmAfter).plusMinutes(10))
    val confirmedByOwnerBeforeCheckInDate = DateTimeFormat.write(now.minusDays(confirmAfter).minusMinutes(15))
    val confirmedByOwnerAfterCheckInDate = DateTimeFormat.write(now.minusDays(confirmAfter).plusMinutes(5))
    val rescheduleDelay = 2
    val config = AutoConfirmInventoryConfig(Duration.ofDays(confirmAfter))
    val revisitDate = DateTimeFormat.read(needTenantConfirmDate).plusDays(confirmAfter)
    val confirmedByOwnerRevisitDate = DateTimeFormat.read(confirmedByOwnerAfterCheckInDate).plusDays(confirmAfter)
    val revisitByContractStatusDate = now.plusHours(rescheduleDelay)

    val ownerVersionInventory = {
      val c = inventoryGen.next.confirmByOwner
      c.copy(data = c.data.toBuilder.setConfirmedByOwnerDate(confirmedByOwnerBeforeCheckInDate).build())
    }
    val tenantVersionInventory = ownerVersionInventory.upVersion()

    val confirmedByOwnerAfterCheckInDateInventory =
      tenantVersionInventory.copy(
        data = tenantVersionInventory.data.toBuilder.setConfirmedByOwnerDate(confirmedByOwnerAfterCheckInDate).build()
      )

    protected def rentContract(contractStatus: ContractStatus, tenantCheckInDate: Timestamp) = {
      val c = rentContractGen(contractStatus).next

      c.copy(data = c.data.toBuilder.setTenantCheckInDate(tenantCheckInDate).build())
    }

    protected def doMock(
      contractStatus: ContractStatus,
      tenantCheckInDate: Timestamp,
      inventory: Inventory,
      confirmedInventory: Option[Inventory],
      findLastInventoryCallCount: Int
    ) = {
      (rentContractDao
        .findByOwnerRequestIdOpt(_: String)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(Some(rentContract(contractStatus, tenantCheckInDate))))
        .once()
      (inventoryDao
        .findLastConfirmedByOwnerRequestId(_: String)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(confirmedInventory))
        .once()
      (inventoryDao
        .findLastByOwnerRequestId(_: String)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(Some(inventory)))
        .repeat(findLastInventoryCallCount)
    }

    protected def invokeStage(inventory: Inventory)(implicit traced: Traced): ProcessingState[Inventory] = {
      val state = ProcessingState(inventory)
      val stage = new AutoConfirmInventoryStage(rentContractDao, inventoryDao, config, features)
      stage.process(state).futureValue
    }
  }
}
