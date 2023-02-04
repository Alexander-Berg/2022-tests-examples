package ru.yandex.realty.rent.service.amohub

import com.google.protobuf.Timestamp
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.amohub.proto.api.internal.AmohubEvent.LeadUpdated
import ru.yandex.realty.amohub.proto.api.internal.Lead.LeadContact
import ru.yandex.realty.amohub.proto.api.internal.{Contact, ContactPhone, InternalGetLeads, Lead, LeadContent}
import ru.yandex.realty.amohub.proto.api.internal.LeadContent.ShowingsLead
import ru.yandex.realty.clients.amohub.AmohubLeadServiceClient
import ru.yandex.realty.rent.backend.ShowingCreator
import ru.yandex.realty.rent.dao.{FlatDao, FlatQuestionnaireDao, FlatShowingDao, OwnerRequestDao}
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.enums.CloseShowingCause.CloseShowingCause
import ru.yandex.realty.rent.model.enums.FlatShowingStatus.FlatShowingStatus
import ru.yandex.realty.rent.model.enums.FlatShowingType.FlatShowingType
import ru.yandex.realty.rent.model.{Flat, FlatQuestionnaire, FlatShowing, OwnerRequest}
import ru.yandex.realty.rent.proto.api.showing.FlatShowingStatusNamespace.{FlatShowingStatus => ProtoShowingStatus}
import ru.yandex.realty.rent.proto.api.moderation.{FlatQuestionnaire => FlatQuestionnaireData}
import ru.yandex.realty.rent.proto.model.flat.showing.FlatShowingData
import ru.yandex.realty.rent.service.impl.amohub.ShowingsLeadProcessor
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.Mappings._
import ru.yandex.vertis.protobuf.BasicProtoFormats.DateTimeFormat

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ShowingsLeadProcessorSpec extends AsyncSpecBase with RentModelsGen {

  implicit private val trace: Traced = Traced.empty

  "ShowingsLeadProcessor" should {
    "reschedule flat for existing showing if it is closed" in new Wiring with Data {
      mockExistingShowing()
      mockFlatIdSearch()
      mockFlatSearch()
      mockFlatRescheduling()
      mockShowingUpdate()
      val event: LeadUpdated = leadUpdatedEvent(withShowingId = true, status = ProtoShowingStatus.CLOSE_WITHOUT_RELEASE)

      processor.process(event, Timestamp.getDefaultInstance).futureValue
    }

    "update showing status for existing showing if it isn't closed" in new Wiring with Data {
      mockExistingShowing()
      mockShowingUpdate()
      val event: LeadUpdated = leadUpdatedEvent(withShowingId = true)

      processor.process(event, Timestamp.getDefaultInstance).futureValue
    }

    "ignore event when owner request not found" in new Wiring with Data {
      mockLastOwnerRequest(exists = false)
      val event: LeadUpdated = leadUpdatedEvent(withFlatId = true)

      processor.process(event, Timestamp.getDefaultInstance).futureValue
    }

    "create new showing if it wasn't created earlier" in new Wiring with Data {
      mockLastOwnerRequest(exists = true)
      mockExistingShowings(exist = false)
      mockGetLeads(withPhone = true)
      mockFlatQuestionnaireSearch(None)
      mockCreateShowing()
      val event: LeadUpdated = leadUpdatedEvent(withFlatId = true)

      processor.process(event, Timestamp.getDefaultInstance).futureValue
    }

    "create new showing with move out date if it wasn't created earlier" in new Wiring with Data {
      mockLastOwnerRequest(exists = true)
      mockExistingShowings(exist = false)
      mockGetLeads(withPhone = true)
      mockFlatQuestionnaireSearch(Some(flatQuestionnaire))
      mockCreateShowing()
      val event: LeadUpdated = leadUpdatedEvent(withFlatId = true)

      processor.process(event, Timestamp.getDefaultInstance).futureValue
    }

    "ignore event when showing is already created and no phones found in contacts" in new Wiring with Data {
      mockLastOwnerRequest(exists = true)
      mockExistingShowings(exist = true)
      mockGetLeads(withPhone = false)
      val event: LeadUpdated = leadUpdatedEvent(withFlatId = true)

      processor.process(event, Timestamp.getDefaultInstance).futureValue
    }

    "try to link user to showing when it is already created and there is phone in contacts" in new Wiring with Data {
      mockLastOwnerRequest(exists = true)
      mockExistingShowings(exist = true)
      mockGetLeads(withPhone = true)
      mockLinkUserToShowing()
      val event: LeadUpdated = leadUpdatedEvent(withFlatId = true)

      processor.process(event, Timestamp.getDefaultInstance).futureValue
    }
  }

  trait Wiring {
    self: Data =>

    val flatDao: FlatDao = mock[FlatDao]
    val flatQuestionnaireDao: FlatQuestionnaireDao = mock[FlatQuestionnaireDao]
    val ownerRequestDao: OwnerRequestDao = mock[OwnerRequestDao]
    val flatShowingDao: FlatShowingDao = mock[FlatShowingDao]
    val amohubLeadClient: AmohubLeadServiceClient = mock[AmohubLeadServiceClient]
    val showingCreator: ShowingCreator = mock[ShowingCreator]

    val processor =
      new ShowingsLeadProcessor(
        flatDao,
        flatQuestionnaireDao,
        ownerRequestDao,
        flatShowingDao,
        amohubLeadClient,
        showingCreator
      )

    def mockExistingShowing(): Unit =
      (flatShowingDao
        .find(_: String, _: Boolean)(_: Traced))
        .expects(showingId, *, *)
        .returning(Future.successful(Some(showing)))

    def mockFlatIdSearch(): Unit =
      (ownerRequestDao
        .findFlatIdById(_: String)(_: Traced))
        .expects(ownerRequestId, *)
        .returning(Future.successful(Some(flatId)))

    def mockFlatSearch(): Unit =
      (flatDao
        .findByIdOpt(_: String)(_: Traced))
        .expects(flatId, *)
        .returning(Future.successful(Some(flat)))

    def mockShowingUpdate(): Unit =
      (flatShowingDao
        .update(_: String)(_: FlatShowing => FlatShowing)(_: Traced))
        .expects(showingId, *, *)
        .returning(Future.successful(showing))

    def mockFlatRescheduling(): Unit =
      (flatDao
        .tryDecreaseVisitTime(_: String, _: DateTime)(_: Traced))
        .expects(flatId, *, *)
        .returning(Future.unit)

    def mockLastOwnerRequest(exists: Boolean): Unit =
      (ownerRequestDao
        .findLastByFlatId(_: String)(_: Traced))
        .expects(flatId, *)
        .returning(Future.successful(Some(ownerRequest).filter(_ => exists)))

    def mockExistingShowings(exist: Boolean): Unit =
      (flatShowingDao
        .findActiveByOwnerRequest(_: String, _: Boolean)(_: Traced))
        .expects(ownerRequestId, *, *)
        .returning(Future.successful(Seq(showing).filter(_ => exist)))
        .applyTransformIf(exist, _.once())
        .applyTransformIf(!exist, _.twice())

    def mockGetLeads(withPhone: Boolean): Unit =
      (amohubLeadClient
        .getLeads(_: InternalGetLeads.Request)(_: Traced))
        .expects(getLeadsRequest, *)
        .returning(Future.successful(getLeadsResponse(withPhone)))

    def mockFlatQuestionnaireSearch(q: Option[FlatQuestionnaire]) =
      (flatQuestionnaireDao
        .findByFlatId(_: String)(_: Traced))
        .expects(flatId, *)
        .returning(Future.successful(q))

    def mockCreateShowing(): Unit =
      (showingCreator
        .createShowingWithTriggers(
          _: OwnerRequest,
          _: FlatShowingData,
          _: FlatShowingStatus,
          _: FlatShowingType,
          _: Option[CloseShowingCause]
        )(_: Traced))
        .expects(ownerRequest, *, *, *, *, *)
        .returning(Future.successful(showing))

    def mockLinkUserToShowing(): Unit = {
      (showingCreator
        .linkUserWithShowing(_: FlatShowing, _: String)(_: Traced))
        .expects(showing, phone, *)
        .returning(Future.unit)
    }
  }

  trait Data {

    val flatId = "flat-id"
    val ownerRequestId = "owner-request-id"
    val showingId = "showing-id"
    val leadId = 1234L
    val phone = "+79001234567"
    val moveOutDate: Timestamp = DateTimeFormat.write(DateTime.parse("2022-06-02"))

    def leadUpdatedEvent(
      withFlatId: Boolean = false,
      withShowingId: Boolean = false,
      status: ProtoShowingStatus = ProtoShowingStatus.NEW_SHOWING
    ): LeadUpdated =
      LeadUpdated
        .newBuilder()
        .setLeadId(leadId)
        .setContent {
          LeadContent
            .newBuilder()
            .setShowings {
              ShowingsLead
                .newBuilder()
                .applyTransformIf(withFlatId, _.setFlatId(flatId))
                .applyTransformIf(withShowingId, _.setShowingId(showingId))
                .setStatus(status)
                .setCreatedByBack(false)
            }
        }
        .build()

    val showing: FlatShowing = flatShowingGen.next.copy(
      showingId = showingId,
      ownerRequestId = ownerRequestId,
      data = FlatShowingData.newBuilder().setLeadId(leadId).build()
    )

    val ownerRequest: OwnerRequest = ownerRequestGen.next.copy(ownerRequestId = ownerRequestId, flatId = flatId)

    val flat: Flat = flatGen(recursive = false).next.copy(flatId = flatId)

    val flatQuestionnaire: FlatQuestionnaire = FlatQuestionnaire(
      flatId,
      FlatQuestionnaireData.newBuilder().setMoveOutDate(moveOutDate).build(),
      Set.empty
    )

    val getLeadsRequest: InternalGetLeads.Request =
      InternalGetLeads.Request
        .newBuilder()
        .addLeadId(leadId)
        .build()

    def getLeadsResponse(withPhone: Boolean): InternalGetLeads.Response =
      InternalGetLeads.Response
        .newBuilder()
        .addLeads {
          Lead
            .newBuilder()
            .setLeadId(leadId)
            .applySideEffectIf(withPhone, _.addContacts(leadContact))
        }
        .build()

    val leadContact: LeadContact =
      LeadContact
        .newBuilder()
        .setIsMain(true)
        .setContact {
          Contact
            .newBuilder()
            .addPhones(ContactPhone.newBuilder().setPhone(phone).setUnifiedPhone(phone))
        }
        .build()
  }
}
