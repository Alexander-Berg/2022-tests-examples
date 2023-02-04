package ru.yandex.realty.rent.backend.manager

import org.junit.runner.RunWith
import com.google.protobuf.Empty
import org.joda.time.DateTime
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.rent.backend.manager.flat.{DefaultFlatDocumentsManager, FlatDocumentsBuilder}
import ru.yandex.realty.rent.dao.{FlatDao, InventoryDao, KeysHandoverDao, OwnerRequestDao, RentContractDao}
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.enums.Role.Role
import ru.yandex.realty.rent.model.{ContractParticipant, Inventory, KeysHandover, RentContract, User}
import ru.yandex.realty.util.Mappings.MapAny
import scala.collection.JavaConverters._
import ru.yandex.realty.rent.model.enums.{ContractStatus, KeysHandoverDirection, Role}
import ru.yandex.realty.rent.proto.api.flat_document.{IntervalDocuments, InventoryPayload, RentContractPayload}
import ru.yandex.realty.rent.proto.api.flat_document.IntervalDocuments.Document
import ru.yandex.realty.rent.proto.model.contract.UserSign
import ru.yandex.realty.rent.util.DefaultTokenManager
import ru.yandex.realty.rent.util.TokenManager.Config
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.protobuf.BasicProtoFormats.DateTimeFormat
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class FlatDocumentsSpec extends AsyncSpecBase with RequestAware with RentModelsGen {

  "FlatDocumentsManager" should {
    "collect owner documents" in new Data {
      mocks(Role.Owner, owner)

      val documents: Seq[IntervalDocuments] = flatDocumentsManager.getAllFlatDocuments(owner.uid, "").futureValue

      documents.size shouldBe 2
      documents.foldLeft(0)((size, document) => size + document.getDocumentsList.size()) shouldBe 8

      val expected = flatDocumentsBuilder
        .buildIntervalDocuments(
          Role.Owner,
          ownerRequests.sortBy(-_.createTime.getMillis).map(_.ownerRequestId),
          Map(
            (actualOwnerRequest.ownerRequestId, Seq(actualRentContract)),
            (previousOwnerRequest.ownerRequestId, Seq(previousRentContract))
          ),
          Map(
            (actualOwnerRequest.ownerRequestId, Seq(actualConfirmedInventory)),
            (previousOwnerRequest.ownerRequestId, previousInventories.sortBy(-_.version).headOption.toSeq)
          ),
          Map(
            (actualOwnerRequest.ownerRequestId, actualOwnerKeysHandovers.sortBy(_.createTime.getMillis)),
            (previousOwnerRequest.ownerRequestId, previousOwnerKeysHandovers.sortBy(_.createTime.getMillis))
          )
        )

      clearTokens(expected) shouldBe clearTokens(documents)
    }

    "collect tenant documents" in new Data {
      mocks(Role.Tenant, tenant)

      val documents = flatDocumentsManager.getAllFlatDocuments(tenant.uid, "").futureValue

      documents.size shouldBe 1
      documents.foldLeft(0)((size, document) => size + document.getDocumentsList.size()) shouldBe 4

      val expected = flatDocumentsBuilder
        .buildIntervalDocuments(
          Role.Tenant,
          Seq(actualOwnerRequest.ownerRequestId),
          Map((actualOwnerRequest.ownerRequestId, Seq(actualRentContract))),
          Map((actualOwnerRequest.ownerRequestId, Seq(actualConfirmedInventory))),
          Map((actualOwnerRequest.ownerRequestId, actualTenantKeysHandovers.sortBy(_.createTime.getMillis)))
        )

      clearTokens(expected) shouldBe clearTokens(documents)
    }

    "collect previous tenant documents" in new Data {
      mocks(Role.PreviousTenant, previousTenant)

      val documents = flatDocumentsManager.getAllFlatDocuments(previousTenant.uid, "").futureValue

      documents.size shouldBe 1
      documents.foldLeft(0)((size, document) => size + document.getDocumentsList.size()) shouldBe 4

      val expected = flatDocumentsBuilder
        .buildIntervalDocuments(
          Role.Tenant,
          Seq(previousOwnerRequest.ownerRequestId),
          Map((previousOwnerRequest.ownerRequestId, Seq(previousRentContract))),
          Map((previousOwnerRequest.ownerRequestId, previousInventories.sortBy(-_.version).headOption.toSeq)),
          Map((previousOwnerRequest.ownerRequestId, previousTenantKeysHandovers.sortBy(_.createTime.getMillis)))
        )

      clearTokens(expected) shouldBe clearTokens(documents)
    }
  }

  "FlatDocumentsBuilder" should {
    "build flat documents" in new Data {
      val documents = flatDocumentsBuilder.buildIntervalDocuments(
        Role.Owner,
        ownerRequests.sortBy(-_.createTime.getMillis).map(_.ownerRequestId),
        Map(
          (actualOwnerRequest.ownerRequestId, Seq(actualRentContract)),
          (previousOwnerRequest.ownerRequestId, Seq(previousRentContract))
        ),
        Map((previousOwnerRequest.ownerRequestId, previousInventories.headOption.toSeq)),
        Map((actualOwnerRequest.ownerRequestId, actualOwnerKeysHandovers.headOption.toSeq))
      )

      clearTokens(documents) should contain theSameElementsAs expectedDocuments
    }
  }

  private def intervalDate(date: DateTime): String = {
    s"${date.getYear}-%02d-%02d".format(date.getMonthOfYear, date.getDayOfMonth)
  }

  private def subtitleDate(date: DateTime): String = {
    s"%02d.%02d.${date.getYear}".format(date.getDayOfMonth, date.getMonthOfYear)
  }

  private def rentContractDocumentSubtitle(actualRentContract: RentContract): String = {
    s"${actualRentContract.contractNumber} от ${actualRentContract.getRentStartDate.map(subtitleDate).getOrElse("")}"
  }

  private def buildInventorySubtitle(rentContract: RentContract, previousInventories: Seq[Inventory]): String = {
    s"${rentContract.contractNumber} от ${previousInventories.head.getConfirmDate.map(subtitleDate).getOrElse("")}"
  }

  private def buildDocument(title: String, subtitle: String, analyticsType: String): Document.Builder = {
    Document
      .newBuilder()
      .setTitle(title)
      .setSubtitle(subtitle)
      .setAnalyticsType(analyticsType)
      .applySideEffect(_.getFallbackBuilder)
  }

  private def buildRentContractDocument(
    title: String,
    subtitle: String,
    analyticsType: String,
    rentContract: RentContract
  ): Document = {
    buildDocument(title, subtitle, analyticsType)
      .setRentContract(RentContractPayload.newBuilder().setId(rentContract.contractId))
      .build()
  }

  private def buildConfirmedInventoryDocument(
    title: String,
    subtitle: String,
    analyticsType: String,
    inventory: Inventory
  ): Document = {
    buildDocument(title, subtitle, analyticsType)
      .setConfirmedInventory(
        InventoryPayload.newBuilder().setVersion(inventory.version).setOwnerRequestId(inventory.ownerRequestId)
      )
      .build()
  }

  private def buildKeysHandoverDocument(
    title: String,
    subtitle: String,
    analyticsType: String,
    keysHandover: KeysHandover
  ): Document = {
    buildDocument(title, subtitle, analyticsType).setCommon(Empty.getDefaultInstance).build()
  }

  private def clearTokens(documents: Seq[IntervalDocuments]): Seq[IntervalDocuments] = // because of random encipher.getIV
    documents.map(
      d =>
        d.toBuilder
          .clearDocuments()
          .addAllDocuments(d.getDocumentsList.asScala.map(_.toBuilder.clearDownloadToken().build()).asJava)
          .build()
    )

  trait Data {
    protected val now = DateTimeUtil.now()
    protected val rentStartDate = DateTimeFormat.write(now)
    protected val previousRentStartDate = DateTimeFormat.write(now.minusDays(50))
    protected val terminationDate = now.minusDays(5)
    protected val owner = userGen().next
    protected val tenant = userGen().next
    protected val previousTenant = userGen().next
    protected def contractParticipant(user: User) = ContractParticipant(uid = Some(user.uid), None, None, None)
    protected val flat = flatGen().next
    protected val (actualOwnerRequest, previousOwnerRequest) = ownerRequestGen.next(2).toList match {
      case actual :: previous :: Nil => (actual, previous)
    }
    protected val ownerRequests = Seq(actualOwnerRequest, previousOwnerRequest)

    protected val actualRentContract = {
      val c = rentContractGen(ContractStatus.Active).next.copy(
        ownerRequestId = Some(actualOwnerRequest.ownerRequestId),
        tenant = contractParticipant(tenant)
      )
      c.copy(
        data = c.data.toBuilder
          .setRentStartDate(rentStartDate)
          .setOwnerSign(UserSign.getDefaultInstance)
          .setTenantSign(UserSign.getDefaultInstance)
          .build()
      )
    }
    protected val previousRentContract = {
      val c = rentContractGen(ContractStatus.Terminated).next
        .copy(
          ownerRequestId = Some(previousOwnerRequest.ownerRequestId),
          terminationDate = Some(terminationDate),
          tenant = contractParticipant(previousTenant)
        )
      c.copy(
        data = c.data.toBuilder
          .setRentStartDate(previousRentStartDate)
          .setOwnerSign(UserSign.getDefaultInstance)
          .setTenantSign(UserSign.getDefaultInstance)
          .build()
      )
    }
    protected val rentContracts = Seq(actualRentContract, previousRentContract)

    protected val actualConfirmedInventory =
      inventoryGen.next.copy(ownerRequestId = actualOwnerRequest.ownerRequestId).confirmByOwner.confirmByTenant
    protected val actualDraftInventory = actualConfirmedInventory.reset()
    protected val actualInventories = Seq(actualConfirmedInventory, actualDraftInventory)
    protected val previousInventories =
      inventoryGen
        .next(2)
        .map(i => i.copy(ownerRequestId = previousOwnerRequest.ownerRequestId).confirmByOwner.confirmByTenant)
        .toSeq
    protected val inventories = actualInventories ++ previousInventories

    protected val actualOwnerKeysHandovers = Seq(
      keysHandoverGen.next.copy(direction = KeysHandoverDirection.FromManagerToOwner),
      keysHandoverGen.next.copy(direction = KeysHandoverDirection.FromOwnerToManager)
    ).map(_.copy(contractId = Some(actualRentContract.contractId)))
    protected val previousOwnerKeysHandovers =
      actualOwnerKeysHandovers.map(_.copy(contractId = Some(previousRentContract.contractId)))

    protected val actualTenantKeysHandovers = Seq(
      keysHandoverGen.next.copy(direction = KeysHandoverDirection.FromManagerToTenant),
      keysHandoverGen.next.copy(direction = KeysHandoverDirection.FromTenantToManager)
    ).map(_.copy(contractId = Some(actualRentContract.contractId)))
    protected val previousTenantKeysHandovers =
      actualTenantKeysHandovers.map(_.copy(contractId = Some(previousRentContract.contractId)))

    protected val tenantKeysHandovers = actualTenantKeysHandovers ++ previousTenantKeysHandovers
    protected val ownerKeysHandovers = actualOwnerKeysHandovers ++ previousOwnerKeysHandovers
    protected val keysHandovers = ownerKeysHandovers ++ tenantKeysHandovers

    protected val expectedDocuments = Seq(
      IntervalDocuments
        .newBuilder()
        .setBeginning(intervalDate(now))
        .addAllDocuments(
          Seq(
            buildRentContractDocument(
              title = "Договор аренды",
              subtitle = rentContractDocumentSubtitle(actualRentContract),
              analyticsType = "Договор аренды",
              actualRentContract
            ),
            buildKeysHandoverDocument(
              title = "Акт передачи ключей",
              subtitle = rentContractDocumentSubtitle(actualRentContract),
              analyticsType = "Акт передачи ключей",
              actualOwnerKeysHandovers.head
            )
          ).asJava
        )
        .setContractCounterparty(actualRentContract.tenant.name.getOrElse(""))
        .build(),
      IntervalDocuments
        .newBuilder()
        .setBeginning(intervalDate(DateTimeFormat.read(previousRentStartDate)))
        .setEnd(intervalDate(terminationDate))
        .addAllDocuments(
          Seq(
            buildRentContractDocument(
              title = "Договор аренды",
              subtitle = rentContractDocumentSubtitle(previousRentContract),
              analyticsType = "Договор аренды",
              previousRentContract
            ),
            buildConfirmedInventoryDocument(
              title = "Опись имущества",
              subtitle = buildInventorySubtitle(previousRentContract, previousInventories),
              analyticsType = "Опись имущества",
              previousInventories.head
            )
          ).asJava
        )
        .setContractCounterparty(previousRentContract.tenant.name.getOrElse(""))
        .build()
    )

    protected val rentContractDao = mock[RentContractDao]
    protected val ownerRequestDao = mock[OwnerRequestDao]
    protected val inventoryDao = mock[InventoryDao]
    protected val keysHandoverDao = mock[KeysHandoverDao]
    protected val flatDao = mock[FlatDao]

    protected val config = Config("keyWithLength16b")
    protected val tokenManager = new DefaultTokenManager(config)
    protected val flatDocumentsBuilder = new FlatDocumentsBuilder(tokenManager)
    protected val flatDocumentsManager =
      new DefaultFlatDocumentsManager(
        rentContractDao,
        flatDao,
        keysHandoverDao,
        ownerRequestDao,
        inventoryDao,
        flatDocumentsBuilder
      )

    protected def mocks(role: Role, user: User) = {
      (flatDao
        .findByIdOpt(_: String)(_: Traced))
        .expects(*, *)
        .returns(Future.successful(Some(flat.copy(assignedUsers = Map((role, Seq(user)))))))
        .once
      (ownerRequestDao
        .findAllByFlatId(_: String)(_: Traced))
        .expects(*, *)
        .returns(Future.successful(ownerRequests))
        .once()
      (rentContractDao
        .findByOwnerRequestIds(_: Seq[String])(_: Traced))
        .expects(*, *)
        .returns(Future.successful(rentContracts))
        .once()
      (inventoryDao
        .findByOwnerRequestIds(_: Seq[String])(_: Traced))
        .expects(*, *)
        .returns(Future.successful(inventories))
        .once()
      (keysHandoverDao
        .findByFlatId(_: String)(_: Traced))
        .expects(*, *)
        .returns(Future.successful(keysHandovers))
        .once()
    }
  }
}
