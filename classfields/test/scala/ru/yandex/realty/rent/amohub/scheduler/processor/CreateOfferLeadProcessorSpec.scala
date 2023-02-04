package ru.yandex.realty.rent.amohub.scheduler.processor

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.{AsyncSpecBase, SpecBase}
import ru.yandex.realty.amohub.clients.amocrm.AmocrmClient.{AccessToken, WithParam}
import ru.yandex.realty.amohub.clients.amocrm.model.{
  ContactEmbedded,
  ContactSummary,
  CreateContactEntry,
  CreateEntityResponse,
  CreateLeadEntry,
  CreateNoteRequest,
  EmbeddedSummary,
  LeadEmbedded,
  LeadSummary,
  LinkMetadata,
  LinkRequest,
  Contact => AmoContact,
  Lead => AmoLead
}
import ru.yandex.realty.amohub.clients.amocrm.{AmocrmClient, PipelineConfig}
import ru.yandex.realty.amohub.dao.{ContactDao, ContactLeadDao}
import ru.yandex.realty.amohub.model.{Contact, ContactLead, ContactPhone, ContactPhoneStatus}
import ru.yandex.realty.amohub.proto.model.payload.{ActionPayload, CreateOfferLeadPayload}
import ru.yandex.realty.clients.maps.RentPolygon
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.rent.amohub.application.AmocrmPipelineConfig
import ru.yandex.realty.rent.amohub.backend.converters.LeadStatus
import ru.yandex.realty.rent.amohub.backend.manager.AmocrmManager
import ru.yandex.realty.rent.amohub.dao.LeadDao
import ru.yandex.realty.rent.amohub.gen.AmohubModelsGen
import ru.yandex.realty.rent.amohub.model.{CrmAction, CrmActionStatus, CrmActionType, RentLead => Lead}
import ru.yandex.realty.rent.proto.api.crm.CreateRentRequest
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.util.time.DateTimeUtil

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class CreateOfferLeadProcessorSpec extends SpecBase with AsyncSpecBase with RequestAware {

  "CreateOfferLeadProcessor" should {
    "REALTYBACK-6136 create new lead if old lead older than 60 days" in new Wiring with Data with MockHelper {
      mockCreateRentRequestByOffer(findByUnifiedPhoneTimes = 2, leadAgeDays = 61)

      (mockCrmClient
        .getContact(_: Long, _: Iterable[WithParam])(_: AccessToken, _: Traced))
        .expects(contactId, List.empty[WithParam], accessToken, Traced.empty)
        .returning(Future.successful(Some(amoContact)))

      (mockCrmClient
        .createLead(_: Seq[CreateLeadEntry])(_: AccessToken, _: Traced))
        .expects(where {
          case (Seq(request), _, _) =>
            request.customFieldsValues.exists { cfv =>
              cfv.fieldId.contains(ModerationLinkFieldId) &&
              cfv.values.exists(_.toString.contains(URLEncoder.encode(payload.getUnifiedPhone, StandardCharsets.UTF_8)))
            }
          case _ => false
        })
        .returning(Future.successful(createLeadResponse))

      (mockCrmClient
        .linkLead(_: Long, _: Seq[LinkRequest])(_: AccessToken, _: Traced))
        .expects(newLeadId, Seq(linkRequest), accessToken, Traced.empty)
        .returning(Future.unit)

      val result = processor.process(action).futureValue
      result.isInstanceOf[ProcessedWithUpdate] shouldEqual true
      result.asInstanceOf[ProcessedWithUpdate].updatedPayload.getCreateOfferLead.getCreatedLeadId shouldEqual newLeadId
    }

    "REALTYBACK-6136 create comment to lead if old lead younger than 60 days" in new Wiring with Data with MockHelper {
      mockCreateRentRequestByOffer(findByUnifiedPhoneTimes = 1, leadAgeDays = 59)

      (mockCrmClient
        .getLead(_: Long, _: Iterable[WithParam])(_: AccessToken, _: Traced))
        .expects(leadId, List.empty[WithParam], accessToken, Traced.empty)
        .returning(Future.successful(Some(toAmoLead(existingLead))))

      (mockCrmClient
        .createLeadNote(_: Long, _: Seq[CreateNoteRequest])(_: AccessToken, _: Traced))
        .expects(where {
          case (noteLeadId, _, _, _) => noteLeadId == leadId
          case _ => false
        })
        .returning(Future.unit)

      val result = processor.process(action).futureValue
      result shouldEqual Processed
    }

    "REALTYBACK-6136 create new lead if there is no lead yet" in new Wiring with Data with MockHelper {
      mockCreateRentRequestByOfferWithoutLeads(findByUnifiedPhoneTimes = 2)

      (mockCrmClient
        .createContact(_: Seq[CreateContactEntry])(_: AccessToken, _: Traced))
        .expects(where {
          case (Seq(createContactEntry), _, _) =>
            createContactEntry.customFieldsValues.exists { cfv =>
              cfv.fieldCode.contains("PHONE") &&
              cfv.values.exists(_.toString.contains(payload.getUnifiedPhone))
            }
          case _ => false
        })
        .returning(Future.successful(createContactResponse))

      (mockCrmClient
        .getContact(_: Long, _: Iterable[WithParam])(_: AccessToken, _: Traced))
        .expects(*, *, *, *)
        .returning(Future.successful(Some(amoContact)))

      (mockCrmClient
        .createLead(_: Seq[CreateLeadEntry])(_: AccessToken, _: Traced))
        .expects(where {
          case (Seq(request), _, _) =>
            request.customFieldsValues.exists { cfv =>
              cfv.fieldId.contains(ModerationLinkFieldId) &&
              cfv.values.exists(_.toString.contains(URLEncoder.encode(payload.getUnifiedPhone, StandardCharsets.UTF_8)))
            }
          case _ => false
        })
        .returning(Future.successful(createLeadResponse))

      (mockCrmClient
        .linkLead(_: Long, _: Seq[LinkRequest])(_: AccessToken, _: Traced))
        .expects(newLeadId, Seq(linkRequest), accessToken, Traced.empty)
        .returning(Future.unit)

      val result = processor.process(action).futureValue
      result.isInstanceOf[ProcessedWithUpdate] shouldEqual true
      result.asInstanceOf[ProcessedWithUpdate].updatedPayload.getCreateOfferLead.getCreatedLeadId shouldEqual newLeadId
    }
  }

  trait Wiring {

    implicit val accessToken: AccessToken =
      AmocrmClient.AccessToken("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08")
    val mockContactDao: ContactDao = mock[ContactDao]
    val mockLeadDao: LeadDao = mock[LeadDao]
    val mockContactLeadDao: ContactLeadDao = mock[ContactLeadDao]
    val mockCrmClient: AmocrmClient = mock[AmocrmClient]

    val pipelineConfig: PipelineConfig = PipelineConfig(
      pipelineId = 42,
      responsibleUserId = None
    )

    val responsibleUserId = 150L

    val pipelinesConfig: AmocrmPipelineConfig = AmocrmPipelineConfig(
      requests = pipelineConfig,
      offers = pipelineConfig,
      showings = pipelineConfig,
      moveOuts = pipelineConfig
    )

    val crmManager: AmocrmManager = new AmocrmManager(
      mockContactDao,
      mockLeadDao,
      mockContactLeadDao,
      mockCrmClient,
      pipelinesConfig
    )

    val processor: CreateOfferLeadProcessor = new CreateOfferLeadProcessor(
      mockLeadDao,
      mockCrmClient,
      crmManager,
      pipelinesConfig
    )
  }

  trait Data extends AmohubModelsGen {
    this: Wiring =>

    val sampleLead: Lead = leadGen.next

    val allowedStatuses: Seq[Long] = {
      val env = LeadStatus.getEnvShowings
      Seq(
        env.ApplicationProcessed,
        env.CallMissedOnce,
        env.CallMissedTwice,
        env.ConfirmedByTenant,
        env.NewShowing,
        env.ShowingAppointed
      )
    }

    val leadsWithAllowedChanges: Seq[Lead] = allowedStatuses.map(statusId => sampleLead.copy(statusId = statusId))

    def toAmoLead(lead: Lead): AmoLead = AmoLead(
      id = lead.leadId,
      pipelineId = 0,
      statusId = lead.statusId,
      lossReasonId = None,
      responsibleUserId = None,
      createdAt = 0,
      updatedAt = 0,
      customFieldsValues = None,
      _embedded = LeadEmbedded(Nil, None)
    )

    val payload = CreateOfferLeadPayload
      .newBuilder()
      .setUnifiedPhone("+71112223344")
      .setPolygonId(RentPolygon.MSK.value)
      .setSource(CreateRentRequest.Source.SRC_REALTY_OFFER)
      .setOfferUrl("https://realty.yandex.ru/offer/4352837657489679")
      .build()

    val action = CrmAction(
      actionType = CrmActionType.CreateOfferLead,
      idempotencyKey = "1234",
      groupId = "4321",
      status = CrmActionStatus.New,
      retriesCount = 0,
      createTime = DateTimeUtil.now(),
      lastAttemptTime = None,
      payload = ActionPayload.newBuilder().setCreateOfferLead(payload).build(),
      visitTime = Some(DateTimeUtil.now()),
      shardKey = 1
    )

    val contactId = 1234567L

    val oldContact = Contact(
      contactId = contactId,
      phones = Seq(
        ContactPhone(contactId, payload.getUnifiedPhone, Some(payload.getUnifiedPhone), ContactPhoneStatus.Unified)
      ),
      deleted = false,
      responsibleUserId = Some(responsibleUserId),
      name = None,
      createTime = DateTimeUtil.now(),
      updateTime = DateTimeUtil.now(),
      visitTime = None,
      shardKey = 0,
      amoResponse = None
    )

    val leadId = 983458345L

    val contactLead: ContactLead = ContactLead(
      contactId = contactId,
      leadId = leadId,
      isMainContact = true
    )

    val existingLead: Lead = Lead(
      leadId = leadId,
      flatId = None,
      statusId = 1L,
      source = None,
      closeShowingCauseId = None,
      pipelineId = pipelinesConfig.offers.pipelineId,
      lossReasonId = None,
      isConfirmed = true,
      showingId = None,
      showingType = None,
      tenantStructure = None,
      deleted = false,
      createTime = DateTimeUtil.now(),
      updateTime = DateTimeUtil.now(),
      ytExportHash = None,
      kafkaEventHash = None,
      managerId = None,
      ownerRequestId = None,
      createdByBack = true,
      lastEventTimestamp = None,
      visitTime = None,
      shardKey = 0,
      amoResponse = None
    )

    val amoContact: AmoContact = AmoContact(
      id = contactId,
      name = "name",
      firstName = None,
      lastName = None,
      responsibleUserId = Some(responsibleUserId),
      createdAt = DateTimeUtil.now().getMillis,
      updatedAt = DateTimeUtil.now().getMillis,
      customFieldsValues = None,
      _embedded = ContactEmbedded(Seq.empty, None)
    )
    val ModerationLinkFieldId = 661849

    val newLeadId: Long = 672987987L

    val createLeadResponse = CreateEntityResponse(
      EmbeddedSummary(
        contacts = None,
        leads = Some(Seq(LeadSummary(newLeadId)))
      )
    )
    val linkRequest = LinkRequest(contactId, "contacts", LinkMetadata(true))

    val createContactResponse = CreateEntityResponse(
      EmbeddedSummary(
        contacts = Some(Seq(ContactSummary(amoContact.id))),
        leads = None
      )
    )
  }

  trait MockHelper {
    self: Wiring with Data =>

    def mockCreateRentRequestByOffer(findByUnifiedPhoneTimes: Int, leadAgeDays: Int): Unit = {
      require(findByUnifiedPhoneTimes > 0, "findByUnifiedPhoneTimes must be positive")
      require(leadAgeDays > 0, "leadAgeDays must be positive")

      (mockContactDao
        .findByUnifiedPhone(_: String, _: Boolean)(_: Traced))
        .expects(payload.getUnifiedPhone, true, *)
        .returning(Future.successful(Iterable(oldContact)))
        .repeat(findByUnifiedPhoneTimes)

      (mockContactLeadDao
        .getByContactIds(_: Set[Long])(_: Traced))
        .expects(Set(contactId), *)
        .returning(Future.successful(Iterable(contactLead)))

      (mockLeadDao
        .findByIds(_: Set[Long], _: Boolean)(_: Traced))
        .expects(Set(leadId), true, *)
        .returning(
          Future.successful(Iterable(existingLead.copy(updateTime = DateTimeUtil.now().minusDays(leadAgeDays))))
        )
    }

    def mockCreateRentRequestByOfferWithoutLeads(findByUnifiedPhoneTimes: Int): Unit = {
      (mockContactDao
        .findByUnifiedPhone(_: String, _: Boolean)(_: Traced))
        .expects(payload.getUnifiedPhone, true, *)
        .returning(Future.successful(Iterable.empty[Contact]))
        .repeat(findByUnifiedPhoneTimes)

      (mockContactDao
        .create(_: Iterable[Contact])(_: Traced))
        .expects(*, *)
        .returning(Future.unit)

      (mockContactLeadDao
        .getByContactIds(_: Set[Long])(_: Traced))
        .expects(Set.empty[Long], *)
        .returning(Future.successful(Iterable.empty[ContactLead]))

      (mockLeadDao
        .findByIds(_: Set[Long], _: Boolean)(_: Traced))
        .expects(Set.empty[Long], true, *)
        .returning(
          Future.successful(Iterable.empty[Lead])
        )
    }
  }
}
