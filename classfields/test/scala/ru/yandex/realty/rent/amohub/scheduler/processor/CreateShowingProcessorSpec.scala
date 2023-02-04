package ru.yandex.realty.rent.amohub.scheduler.processor

import com.google.protobuf.Timestamp
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.{AsyncSpecBase, SpecBase}
import ru.yandex.realty.amohub.clients.amocrm.AmocrmClient.AccessToken
import ru.yandex.realty.amohub.clients.amocrm.model.{
  ContactEmbedded,
  ContactSummary,
  CreateContactEntry,
  CreateEntityResponse,
  CreateLeadEntry,
  EmbeddedSummary,
  LeadSummary,
  LinkRequest,
  UpdateLeadRequest,
  Contact => AmoContact
}
import ru.yandex.realty.amohub.clients.amocrm.{AmocrmClient, PipelineConfig}
import ru.yandex.realty.amohub.dao.{ContactDao, ContactLeadDao, CrmUserDao}
import ru.yandex.realty.amohub.model.Contact
import ru.yandex.realty.amohub.proto.model.payload.{ActionPayload, CreateShowingPayload}
import ru.yandex.realty.clients.rent.{RentModerationClient, RentOwnerRequestServiceClient}
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.rent.amohub.application.AmocrmPipelineConfig
import ru.yandex.realty.rent.amohub.backend.manager.AmocrmManager
import ru.yandex.realty.rent.amohub.backend.manager.responsible.ResponsibleUserManager
import ru.yandex.realty.rent.amohub.dao._
import ru.yandex.realty.rent.amohub.model.{CrmAction, CrmActionStatus, CrmActionType, ShowingLead}
import ru.yandex.realty.rent.proto.api.internal.InternalGetFlatByOwnerRequest
import ru.yandex.realty.rent.proto.api.moderation.{FlatRequestFeature, GetModerationFlatResponse}
import ru.yandex.realty.rent.proto.api.showing.FlatShowingStatusNamespace.FlatShowingStatus
import ru.yandex.realty.rent.proto.api.showing.FlatShowingTypeNamespace.FlatShowingType
import ru.yandex.realty.rent.proto.model.flat.showing.FlatShowingData
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.protobuf.ProtobufFormats.DateTimeFormat
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.Future

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class CreateShowingProcessorSpec extends SpecBase with AsyncSpecBase with RequestAware {
  "CreateShowingProcessor" should {
    "create showing lead with move out date" in new Wiring with Data {
      mockGetFlatByOwnerRequest()
      mockGetModerationFlat()

      //CreateShowingProcessor.createLeadIfNotCreatedEarlier
      mockFindShowingLead()
      mockFindContactByUnifiedPhone()
      mockChooseShowingResponsibleUser()
      mockCreateLead()
      mockCreateShowingLead()

      //CreateShowingProcessor.tryDoAdditionalActions
      mockFindContactByUnifiedPhone()
      mockCreateContact()
      mockGetContact()
      mockContactDaoCreateContact()
      mockLinkLead()
      mockUpdateLead()

      val result = processor.process(action).futureValue
      val expectedResult = ProcessedWithUpdate(
        ActionPayload
          .newBuilder()
          .setCreateShowing(payload.toBuilder.setCreatedLeadId(newLeadId))
          .build()
      )
      result shouldEqual expectedResult
    }
  }

  trait Wiring {
    self: Data =>

    val rentModerationClient: RentModerationClient = mock[RentModerationClient]
    val rentOwnerRequestClient: RentOwnerRequestServiceClient = mock[RentOwnerRequestServiceClient]
    val amoCrmClient: AmocrmClient = mock[AmocrmClient]
    val mockContactDao: ContactDao = mock[ContactDao]
    val mockLeadDao: LeadDao = mock[LeadDao]
    val mockContactLeadDao: ContactLeadDao = mock[ContactLeadDao]
    val mockCrmUserDao: CrmUserDao = mock[CrmUserDao]
    val mockShowingLeadDao: ShowingLeadDao = mock[ShowingLeadDao]

    val pipelineConfig: PipelineConfig = PipelineConfig(
      pipelineId = 42,
      responsibleUserId = None
    )

    val pipelinesConfig: AmocrmPipelineConfig = AmocrmPipelineConfig(
      requests = pipelineConfig,
      offers = pipelineConfig,
      showings = pipelineConfig,
      moveOuts = pipelineConfig
    )
    implicit val accessToken: AccessToken =
      AmocrmClient.AccessToken("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08")

    val crmManager: AmocrmManager = new AmocrmManager(
      mockContactDao,
      mockLeadDao,
      mockContactLeadDao,
      amoCrmClient,
      pipelinesConfig
    )

    val responsibleUserManager: ResponsibleUserManager = mock[ResponsibleUserManager]

    val processor = new CreateShowingProcessor(
      rentModerationClient,
      rentOwnerRequestClient,
      amoCrmClient,
      crmManager,
      mockContactDao,
      mockCrmUserDao,
      mockShowingLeadDao,
      responsibleUserManager,
      pipelinesConfig
    )

    def mockGetFlatByOwnerRequest() = {
      val response = InternalGetFlatByOwnerRequest.Response
        .newBuilder()
        .setFlatId(flatId)
        .build()
      (rentOwnerRequestClient
        .getFlatByOwnerRequest(_: String)(_: Traced))
        .expects(ownerRequestId, *)
        .returning(Future.successful(Some(response)))
    }

    def mockGetModerationFlat() = {
      val response = GetModerationFlatResponse
        .newBuilder()
        .build()
      (rentModerationClient
        .getModerationFlat(_: String, _: Set[FlatRequestFeature])(_: Traced))
        .expects(flatId, *, *)
        .returning(Future.successful(response))
    }

    def mockFindShowingLead() = {
      (mockShowingLeadDao
        .findOpt(_: String)(_: Traced))
        .expects(showingId, *)
        .returning(Future.successful(None))
    }

    def mockFindContactByUnifiedPhone() = {
      (mockContactDao
        .findByUnifiedPhone(_: String, _: Boolean)(_: Traced))
        .expects(tenantPhone, true, *)
        .returning(Future.successful(Iterable.empty))
    }

    def mockChooseShowingResponsibleUser() = {
      (responsibleUserManager
        .chooseShowingResponsibleUser(
          _: FlatShowingStatus,
          _: FlatShowingType,
          _: Option[String],
          _: Option[Long],
          _: Option[Long]
        )(_: Traced))
        .expects(FlatShowingStatus.NEW_SHOWING, FlatShowingType.ONLINE, *, *, *, *)
        .returning(Future.successful(responsibleManagerId))
    }

    def mockCreateLead() = {
      (amoCrmClient
        .createLead(_: Seq[CreateLeadEntry])(_: AccessToken, _: Traced))
        .expects(where {
          case (Seq(request), _, _) =>
            request.customFieldsValues.exists { cfv =>
              cfv.fieldId.contains(MoveOutDateFieldId) &&
              cfv.values.exists(_.toString.contains(moveOutDateStr))
            }
          case _ => false
        })
        .returning(Future.successful(createLeadResponse))
    }

    def mockCreateShowingLead() =
      (mockShowingLeadDao
        .create(_: ShowingLead)(_: Traced))
        .expects(ShowingLead(showingId, newLeadId), *)
        .returning(Future.unit)

    def mockCreateContact() =
      (amoCrmClient
        .createContact(_: Seq[CreateContactEntry])(_: AccessToken, _: Traced))
        .expects(where {
          case (Seq(createContactEntry), _, _) =>
            createContactEntry.customFieldsValues.exists { cfv =>
              cfv.fieldCode.contains("PHONE") &&
              cfv.values.exists(_.toString.contains(tenantPhone))
            }
          case _ => false
        })
        .returning(Future.successful(createContactResponse))

    def mockGetContact() =
      (amoCrmClient
        .getContact(_: Long, _: Iterable[AmocrmClient.WithParam])(_: AccessToken, _: Traced))
        .expects(contactId, *, *, *)
        .returning(Future(Some(amoContact)))

    def mockContactDaoCreateContact() =
      (mockContactDao
        .create(_: Iterable[Contact])(_: Traced))
        .expects(*, *)
        .returning(Future.unit)

    def mockLinkLead() =
      (amoCrmClient
        .linkLead(_: Long, _: Seq[LinkRequest])(_: AccessToken, _: Traced))
        .expects(newLeadId, *, *, *)
        .returning(Future.unit)

    def mockUpdateLead() =
      (amoCrmClient
        .updateLead(_: Seq[UpdateLeadRequest])(_: AccessToken, _: Traced))
        .expects(*, *, *)
        .returning(Future.unit)
  }

  trait Data {

    val showingId = "showing-id"
    val flatId = "flat-id"
    val ownerRequestId = "owner-request-id"
    val moveOutDateStr = "2022-06-02"
    val moveOutDate: Timestamp = DateTimeFormat.write(DateTime.parse(moveOutDateStr))
    val tenantPhone = "tenant-phone"
    val tenantName = "tenant-name"
    val responsibleManagerId = 312L
    val MoveOutDateFieldId = 686047

    val payload = CreateShowingPayload
      .newBuilder()
      .setOwnerRequestId(ownerRequestId)
      .setShowingId(showingId)
      .setMoveOutDate(moveOutDate)
      .setInitialStatus(FlatShowingStatus.NEW_SHOWING)
      .setShowingType(FlatShowingType.ONLINE)
      .setInitialTenantInfo(
        FlatShowingData.InitialTenantInfo
          .newBuilder()
          .setTenantPhone(tenantPhone)
          .setTenantName(tenantName)
      )
      .build()

    val action = CrmAction(
      actionType = CrmActionType.CreateShowing,
      idempotencyKey = "1234",
      groupId = "4321",
      status = CrmActionStatus.New,
      retriesCount = 0,
      createTime = DateTimeUtil.now(),
      lastAttemptTime = None,
      payload = ActionPayload.newBuilder().setCreateShowing(payload).build(),
      visitTime = Some(DateTimeUtil.now()),
      shardKey = 1
    )

    val newLeadId: Long = 672987988L

    val createLeadResponse = CreateEntityResponse(
      EmbeddedSummary(
        contacts = None,
        leads = Some(Seq(LeadSummary(newLeadId)))
      )
    )

    val contactId = 1234567L

    val amoContact: AmoContact = AmoContact(
      id = contactId,
      name = tenantName,
      firstName = None,
      lastName = None,
      responsibleUserId = Some(responsibleManagerId),
      createdAt = DateTimeUtil.now().getMillis,
      updatedAt = DateTimeUtil.now().getMillis,
      customFieldsValues = None,
      _embedded = ContactEmbedded(Seq.empty, None)
    )

    val createContactResponse = CreateEntityResponse(
      EmbeddedSummary(
        contacts = Some(Seq(ContactSummary(amoContact.id))),
        leads = None
      )
    )
  }
}
