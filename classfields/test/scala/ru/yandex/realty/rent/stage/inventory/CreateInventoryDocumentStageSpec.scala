package ru.yandex.realty.rent.stage.inventory

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import realty.dochub.palma.Dochub
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.rent.backend.inventory.InventoryPdfManager
import ru.yandex.realty.watching.ProcessingState
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.Inventory
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class CreateInventoryDocumentStageSpec extends AsyncSpecBase with RentModelsGen {

  "CreateInventoryDocumentStage" should {
    "create document for owner" in new Data {
      (inventoryPdfManager
        .createOwnerDocument(_: Inventory)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(ownerDocument))
        .once()

      val result = invokeStage(inventoryWithoutOwnerDocument)

      result.entry.getPdfDocumentId shouldBe Some(ownerDocumentId)
    }

    "create document for tenant" in new Data {
      (inventoryPdfManager
        .createTenantDocument(_: Inventory)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(tenantDocument))
        .once()

      val result = invokeStage(inventoryWithoutTenantDocument)

      result.entry.getPdfDocumentId shouldBe Some(tenantDocumentId)
    }

    "not create document" in new Data {
      val result = invokeStage(inventoryWithDocument)

      result.entry shouldBe inventoryWithDocument
    }
  }

  trait Data {
    implicit val traced: Traced = Traced.empty

    protected val inventoryPdfManager: InventoryPdfManager = mock[InventoryPdfManager]

    val ownerDocumentId = "ownerDocId"
    val tenantDocumentId = "tenantDocId"
    val ownerDocument = Dochub.Document.newBuilder().setId(ownerDocumentId).build()
    val tenantDocument = Dochub.Document.newBuilder().setId(tenantDocumentId).build()
    val inventoryWithoutOwnerDocument = inventoryGen.next.confirmByOwner
    val inventoryWithoutTenantDocument = inventoryWithoutOwnerDocument.addPdfDocument(ownerDocument).confirmByTenant
    val inventoryWithDocument = inventoryWithoutTenantDocument.addPdfDocument(tenantDocument).copy(visitTime = None)

    protected def invokeStage(inventory: Inventory)(implicit traced: Traced): ProcessingState[Inventory] = {
      val state = ProcessingState(inventory)
      val stage = new CreateInventoryDocumentStage(inventoryPdfManager)
      stage.process(state).futureValue
    }
  }
}
