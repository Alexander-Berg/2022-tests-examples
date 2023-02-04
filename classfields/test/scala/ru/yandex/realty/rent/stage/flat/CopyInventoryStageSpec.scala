package ru.yandex.realty.rent.stage.flat

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.features.{Features, SimpleFeatures}
import ru.yandex.realty.rent.dao.InventoryDao
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.enums.OwnerRequestStatus
import ru.yandex.realty.rent.model.{Flat, Inventory}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.watching.ProcessingState

import scala.concurrent.Future
import scala.util.Random

@RunWith(classOf[JUnitRunner])
class CopyInventoryStageSpec extends AsyncSpecBase with RentModelsGen {
  implicit private val traced: Traced = Traced.empty

  private val inventoryDao = mock[InventoryDao]

  private val features: Features = {
    val f = new SimpleFeatures
    f.InventoryStage.setNewState(true)
    f
  }

  val ownerRequestWithInventoryStatuses = Seq(
    OwnerRequestStatus.Completed,
    OwnerRequestStatus.Confirmed,
    OwnerRequestStatus.LookingForTenant,
    OwnerRequestStatus.WorkInProgress,
    OwnerRequestStatus.CancelledWithoutSigning,
    OwnerRequestStatus.Denied
  )
  private val newOwnerRequest = ownerRequestGen.next.copy(status = OwnerRequestStatus.Confirmed)
  private val previousOwnerRequest = ownerRequestGen.next
    .copy(
      status = ownerRequestWithInventoryStatuses(Random.nextInt(ownerRequestWithInventoryStatuses.size)),
      createTime = newOwnerRequest.createTime.minusDays(1)
    )
  private val flat = flatGen().next.copy(ownerRequests = Seq(newOwnerRequest, previousOwnerRequest))

  "CopyInventoryStage" should {
    "create copy of inventory for new owner request" in {
      handleMock(inventory = Some(inventoryGen.next), firstMockRepeat = 1, secondMockRepeat = 1)

      invokeStage(flat)
    }

    "not create copy of inventory for new owner request when there is no inventory for previous owner request" in {
      handleMock(inventory = None, firstMockRepeat = 1, secondMockRepeat = 0)

      invokeStage(flat)
    }

    "not create copy of inventory for new owner request when it is first owner request for flat" in {
      val flat = flatGen().next.copy(ownerRequests = Seq(newOwnerRequest))

      handleMock(inventory = None, firstMockRepeat = 0, secondMockRepeat = 0)

      invokeStage(flat)
    }
  }

  private def handleMock(inventory: Option[Inventory], firstMockRepeat: Int, secondMockRepeat: Int) = {
    (inventoryDao
      .findLastByOwnerRequestId(_: String)(_: Traced))
      .expects(*, *)
      .returning(Future.successful(inventory))
      .repeat(firstMockRepeat)

    (inventoryDao
      .insert(_: Inventory)(_: Traced))
      .expects(*, *)
      .repeat(secondMockRepeat)
  }

  private def invokeStage(flat: Flat): ProcessingState[Flat] = {
    val state = ProcessingState(flat)
    val stage = new CopyInventoryStage(inventoryDao, features)
    stage.process(state).futureValue
  }
}
