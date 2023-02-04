package ru.yandex.realty.rent.backend.inventory

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import realty.dochub.palma.Dochub
import realty.dochub.palma.Dochub.RendererSettings.RendererTemplate
import realty.palma.rent_user.RentUser
import realty.pdfprinter.templates.Templates.DocumentTemplate
import ru.yandex.extdata.core.{Controller, DataType, TaskId}
import ru.yandex.extdata.core.event.{Event, EventListener}
import ru.yandex.extdata.core.service.ExtDataService
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.application.ng.palma.client.PalmaClient
import ru.yandex.realty.clients.dochub.DocumentServiceClient
import ru.yandex.realty.context.v2.{DochubRendererSettingsProvider, DochubRendererSettingsStorage}
import ru.yandex.realty.dochub.DocumentResponse
import ru.yandex.realty.features.FeaturesStubComponent
import ru.yandex.realty.files.GetDownloadUrlResponse
import ru.yandex.realty.picapica.MdsUrlBuilder
import ru.yandex.realty.rent.backend.converter.ImageConverter
import ru.yandex.realty.rent.dao.{FlatDao, InventoryDao, OwnerRequestDao, UserDao, UserFlatDao}
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.Inventory
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.rent.backend.ExtendedUserManager
import ru.yandex.realty.rent.model.enums.Role
import ru.yandex.realty.rent.model.enums.Role.Role
import ru.yandex.realty.tracing.Traced

import scala.concurrent.{ExecutionContext, Future}
import ru.yandex.realty.util.Mappings.MapAny

@RunWith(classOf[JUnitRunner])
class InventoryPdfManagerSpec extends AsyncSpecBase with RequestAware with RentModelsGen with FeaturesStubComponent {

  features.NewInventoryPdfCreationFlow.setNewState(true)

  private val inventoryDao = mock[InventoryDao]
  private val userDao = mock[UserDao]
  private val userFlatDao = mock[UserFlatDao]
  private val flatDao = mock[FlatDao]
  private val ownerRequestDao = mock[OwnerRequestDao]
  private val documentClient: DocumentServiceClient = mock[DocumentServiceClient]
  private val mockMdsUrlBuilder: MdsUrlBuilder = new MdsUrlBuilder("//")
  private val mockImageConverter: ImageConverter = new ImageConverter(mockMdsUrlBuilder)
  private val palmaRentUserClient = mock[PalmaClient[RentUser]]
  private val rentInventoryBuilder = new RentInventoryDocumentBuilder(mockImageConverter)
  private val extendedUserManager = new ExtendedUserManager(userDao, palmaRentUserClient)
  private val dochubRendererSettingsStorage = mock[DochubRendererSettingsStorage]

  val rendererSettingsController: Controller = new Controller {
    override def start(): Unit = {}
    override def close(): Unit = {}
    override def replicate(dataType: DataType): Unit = {}
    override def register(listener: EventListener): Unit = {}
    override def onEvent(e: Event): Unit = {}
    override def dispatch(id: TaskId, weight: Int, payload: () => Unit): Unit = {}
    override def extDataService: ExtDataService = mock[ExtDataService]
  }
  private val dochubRendererSettingsClient = new DochubRendererSettingsProvider(rendererSettingsController) {
    override def get(): DochubRendererSettingsStorage = dochubRendererSettingsStorage
  }

  private val inventoryPdfManager = new DefaultInventoryPdfManager(
    inventoryDao,
    flatDao,
    userDao,
    userFlatDao,
    ownerRequestDao,
    documentClient,
    rentInventoryBuilder,
    dochubRendererSettingsClient,
    extendedUserManager,
    features
  )

  "InventoryPdfManager" should {
    "return url to get inventory pdf and create document" in new Data {
      mock(inventory, Role.Owner, 1, 1, 0, 1, 1)
      (inventoryDao
        .findLastConfirmedBeforeVersion(_: Int, _: String)(_: Traced))
        .expects(*, *, *)
        .returning(Future.successful(Some(inventory)))
        .once()
      (dochubRendererSettingsStorage
        .getVersion(_: RendererTemplate))
        .expects(*)
        .returning(documentTemplateVersion)
        .once()

      val response = inventoryPdfManager.getInventoryPdfUrl(ownerRequestId, None).futureValue

      response shouldBe documentUrl
    }

    "return url to get inventory pdf and supplement document with owner info" in new Data {
      mock(confirmedByOwnerInventory, Role.Owner, 1, 1, 1, 1, 1)
      (inventoryDao
        .findLastConfirmedBeforeVersion(_: Int, _: String)(_: Traced))
        .expects(*, *, *)
        .returning(Future.successful(Some(inventory)))
        .once()
      (dochubRendererSettingsStorage
        .getVersion(_: RendererTemplate))
        .expects(*)
        .returning(documentTemplateVersion)
        .once()

      val response = inventoryPdfManager.getInventoryPdfUrl(ownerRequestId, None).futureValue

      response shouldBe documentUrl
    }

    "return url to get inventory pdf and supplement document with tenant info" in new Data {
      mock(confirmedInventory, Role.Tenant, 1, 0, 1, 1, 1)
      (documentClient
        .getDocument(_: String)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(Some(document.getDocument)))
        .once()
      val response = inventoryPdfManager.getInventoryPdfUrl(ownerRequestId, None).futureValue

      response shouldBe documentUrl
    }

    "not delete owner document" in new Data {
      mock(confirmedInventory, Role.Tenant, 0, 0, 0, 0, 0)
      (inventoryDao
        .findByVersionAndOwnerRequestId(_: Int, _: String)(_: Traced))
        .expects(*, *, *)
        .returning(Future.successful(Some(confirmedByOwnerInventory)))
      (documentClient
        .getDocument(_: String)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(Some(document.getDocument)))
        .once()

      val response = inventoryPdfManager.createTenantDocument(confirmedInventoryWithoutDocument).futureValue

      response shouldBe document.getDocument
    }

    "throw exception when there is no document id in owner document" in new Data {
      (inventoryDao
        .findByVersionAndOwnerRequestId(_: Int, _: String)(_: Traced))
        .expects(*, *, *)
        .returning(Future.successful(Some(confirmedByOwnerInventoryWithoutDocumentId)))
      interceptCause[IllegalStateException] {
        inventoryPdfManager.createTenantDocument(confirmedInventoryWithoutDocument).futureValue
      }.getMessage shouldBe s"Unfilled document id for inventory ${confirmedByOwnerInventoryWithoutDocumentId.id}"
    }

    "return document from inventory when exist" in new Data {
      (inventoryDao
        .findLastByOwnerRequestId(_: String)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(Some(inventoryWithDocument)))
        .once()
      (documentClient
        .getDocumentUrl(_: String)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(documentUrl))
        .once()

      val response = inventoryPdfManager.getInventoryPdfUrl(ownerRequestId, None).futureValue

      response shouldBe documentUrl
    }

    "recreate document when inventory is initial" in new Data {
      mock(initialInventory, Role.Owner, 1, 1, 1, 1, 1)
      (inventoryDao
        .findLastConfirmedBeforeVersion(_: Int, _: String)(_: Traced))
        .expects(*, *, *)
        .returning(Future.successful(Some(initialInventory)))
        .once()
      (dochubRendererSettingsStorage
        .getVersion(_: RendererTemplate))
        .expects(*)
        .returning(documentTemplateVersion)
        .once()

      val response = inventoryPdfManager.getInventoryPdfUrl(ownerRequestId, None).futureValue

      response shouldBe documentUrl
    }

    "set pdf template version if not exist" in new Data {
      mock(inventory, Role.Owner, 0, 1, 0, 0, 0)
      (inventoryDao
        .findLastConfirmedBeforeVersion(_: Int, _: String)(_: Traced))
        .expects(*, *, *)
        .returning(Future.successful(Some(inventory)))
        .once()
      (dochubRendererSettingsStorage
        .getVersion(_: RendererTemplate))
        .expects(*)
        .returning(documentTemplateVersion)
        .once()

      inventoryPdfManager.createOwnerDocument(inventory).futureValue
    }

    "not set pdf template version if exist" in new Data {
      mock(inventory, Role.Owner, 0, 1, 0, 0, 0)
      (inventoryDao
        .findLastConfirmedBeforeVersion(_: Int, _: String)(_: Traced))
        .expects(*, *, *)
        .returning(Future.successful(None))
        .once()
      (dochubRendererSettingsStorage
        .getVersion(_: RendererTemplate))
        .expects(*)
        .returning(documentTemplateVersion)
        .never()

      inventoryPdfManager.createOwnerDocument(inventoryWithTemplateVersion).futureValue
    }
  }

  trait Data {
    protected val url = "url"
    protected val ownerRequestId = "owner_request_id"
    protected val documentId = "documentId"
    protected val version = 1

    protected val document = DocumentResponse.newBuilder
      .setDocument(Dochub.Document.newBuilder().setId("id"))
      .build()
    protected val documentUrl = GetDownloadUrlResponse
      .newBuilder()
      .setUrl(url)
      .build()

    protected val inventory = inventoryGen.next
    protected val inventoryWithTemplateVersion =
      inventory.copy(
        data = inventory.data.toBuilder.applySideEffect(_.getPdfDocumentInfoBuilder.setTemplateVersion(10)).build()
      )
    protected val documentTemplateVersion = 5

    protected val confirmedByOwnerInventory =
      inventory.addPdfDocument(document.getDocument).confirmByOwner

    protected val confirmedInventory = confirmedByOwnerInventory.confirmByTenant

    protected val confirmedInventoryWithoutDocument = confirmedInventory.copy(
      data = confirmedInventory.data.toBuilder.applySideEffect(_.getPdfDocumentInfoBuilder.clearDocumentId()).build()
    )

    protected val confirmedByOwnerInventoryWithoutDocumentId = confirmedByOwnerInventory.copy(
      data =
        confirmedByOwnerInventory.data.toBuilder.applySideEffect(_.getPdfDocumentInfoBuilder.clearDocumentId()).build()
    )

    protected val initialInventory = inventory.addPdfDocument(document.getDocument)

    protected val inventoryWithDocument = confirmedInventory.addPdfDocument(document.getDocument)

    protected def mock(
      inventory: Inventory,
      role: Role,
      findLastInventoryMockCallCount: Int,
      getPalmaUserCount: Int,
      deleteIFExistsMockCallCount: Int,
      getDocumentUrlMockCallCount: Int,
      updateInvntoryMockCallCount: Int
    ) = {
      (inventoryDao
        .findLastByOwnerRequestId(_: String)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(Some(inventory)))
        .repeat(findLastInventoryMockCallCount)
      (userDao
        .findByUidOpt(_: Long, _: Boolean)(_: Traced))
        .expects(*, *, *)
        .returning(Future.successful(Some(userGen().next)))
        .once()
      (palmaRentUserClient
        .get(_: String)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(Some(RentUser.defaultInstance)))
        .repeat(getPalmaUserCount)
      (flatDao
        .findJustFlatByIdOpt(_: String)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(Some(flatGen().next)))
        .once()
      (userFlatDao
        .findByFlat(_: String)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(Seq(userFlatGen.next.copy(role = role))))
        .once()
      (documentClient
        .createDocument(_: String, _: DocumentTemplate)(_: Traced))
        .expects(*, *, *)
        .returning(Future.successful(document))
        .once()
      (documentClient
        .removeDocumentIfExists(_: String)(_: Traced, _: ExecutionContext))
        .expects(*, *, *)
        .returning(Future.unit)
        .repeat(deleteIFExistsMockCallCount)
      (documentClient
        .getDocumentUrl(_: String)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(documentUrl))
        .repeat(getDocumentUrlMockCallCount)
      (ownerRequestDao
        .findByIdOpt(_: String)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(Some(ownerRequestGen.next)))
        .once()
      (inventoryDao
        .update(_: String, _: Int)(_: Inventory => Inventory)(_: Traced))
        .expects(*, *, *, *)
        .returning(Future.successful(Some(inventory)))
        .repeat(updateInvntoryMockCallCount)
    }
  }
}
