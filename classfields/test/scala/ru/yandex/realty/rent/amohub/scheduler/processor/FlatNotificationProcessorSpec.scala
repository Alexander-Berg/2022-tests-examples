package ru.yandex.realty.rent.amohub.scheduler.processor

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.amohub.clients.amocrm.AmocrmClient.{AccessToken, WithParam}
import ru.yandex.realty.amohub.clients.amocrm.{AmocrmClient, PipelineConfig}
import ru.yandex.realty.amohub.clients.amocrm.model.{
  CreateNoteRequest,
  CreateTaskRequest,
  LeadEmbedded,
  UpdateLeadRequest,
  Lead => AmoLead
}
import ru.yandex.realty.{AsyncSpecBase, SpecBase}
import ru.yandex.realty.amohub.proto.model.payload.{ActionPayload, FlatNotificationPayload, OwnerRequestSmsSentPayload}
import ru.yandex.realty.clients.rent.RentOwnerRequestServiceGeneratedClient
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.rent.amohub.application.AmocrmPipelineConfig
import ru.yandex.realty.rent.amohub.dao.{CrmActionDao, LeadDao}
import ru.yandex.realty.rent.amohub.gen.AmohubModelsGen
import ru.yandex.realty.rent.amohub.model.CrmActionStatus.CrmActionStatus
import ru.yandex.realty.rent.amohub.model.{CrmActionGroups, CrmActionStatus, CrmActionType, RentLead => Lead}
import ru.yandex.realty.rent.amohub.util.BuildCrmActionUtils
import ru.yandex.realty.rent.proto.api.internal.InternalGetCurrentOwner
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.Mappings._

import scala.concurrent.Future

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class FlatNotificationProcessorSpec
  extends SpecBase
  with AsyncSpecBase
  with RequestAware
  with BuildCrmActionUtils
  with AmohubModelsGen {
  "Flat notification processor" should {
    "add tag to amocrm lead for odd owner uid" in new Wiring with Data with MockHelper {
      createMocksForUid(oddOwnerUid)

      (mockCrmClient
        .updateLead(_: Seq[UpdateLeadRequest])(_: AccessToken, _: Traced))
        .expects(where({ case (r, _, _) => r.exists(req => req.id == leadId && req._embedded.get.tags.nonEmpty) }))
        .returning(Future.unit)

      processor.process(action).futureValue shouldBe ProcessedWithUpdate(expectedAction)
    }

    "not add tag to amocrm lead for even owner uid" in new Wiring with Data with MockHelper {
      createMocksForUid(evenOwnerUid)

      (mockCrmClient
        .updateLead(_: Seq[UpdateLeadRequest])(_: AccessToken, _: Traced))
        .expects(where({ case (r, _, _) => r.exists(req => req.id == leadId && req._embedded.get.tags.isEmpty) }))
        .returning(Future.unit)

      processor.process(action).futureValue shouldBe ProcessedWithUpdate(expectedAction)
    }
  }

  trait Wiring {
    val mockCrmClient: AmocrmClient = mock[AmocrmClient]
    val mockLeadDao: LeadDao = mock[LeadDao]
    val mockCrmActionDao: CrmActionDao = mock[CrmActionDao]

    val mockRentOwnerRequestClient: RentOwnerRequestServiceGeneratedClient =
      mock[RentOwnerRequestServiceGeneratedClient]

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
      AmocrmClient.AccessToken("test-token-for-amocrm")

    val processor = new FlatNotificationProcessor(
      mockCrmClient,
      mockLeadDao,
      mockCrmActionDao,
      mockRentOwnerRequestClient,
      pipelinesConfig,
      CrmActionType.OwnerRequestConfirmed
    )
  }

  trait Data {
    self: Wiring =>

    val flatId = "flat-1"
    val ownerRequestId = "owner-request-1"
    val oddOwnerUid = 100000021
    val evenOwnerUid = 100000022

    val action = buildAction(
      actionType = CrmActionType.OwnerRequestConfirmed,
      idempotencyKey = ownerRequestId,
      groupId = CrmActionGroups.flatGroupId(flatId),
      payload = ActionPayload
        .newBuilder()
        .setFlatNotification(
          FlatNotificationPayload
            .newBuilder()
            .setFlatId(flatId)
        )
        .build()
    )

    val leadId = 1233264387L

    val previousAction = buildAction(
      actionType = CrmActionType.OwnerRequestSmsSent,
      idempotencyKey = "some-idempotency-key",
      groupId = CrmActionGroups.flatGroupId(flatId),
      payload = ActionPayload
        .newBuilder()
        .setOwnerRequestSmsSent(
          OwnerRequestSmsSentPayload
            .newBuilder()
            .setFlatId(flatId)
            .setOwnerRequestId(ownerRequestId)
            .setCreatedOrAssignedLeadId(leadId)
        )
        .build()
    ).copy(status = CrmActionStatus.Processed)

    val existingLead: Lead = leadGen.next.copy(
      leadId = leadId,
      statusId = 1L,
      pipelineId = pipelinesConfig.requests.pipelineId,
      isConfirmed = false
    )

    val expectedAction = action.payload.toBuilder
      .applySideEffect(_.getFlatNotificationBuilder.setIsTaskCreated(true))
      .build()

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
  }

  trait MockHelper {
    self: Wiring with Data =>

    def createMocksForUid(uid: Long): Unit = {
      (mockCrmActionDao
        .getByGroupIds(_: Set[String], _: Set[CrmActionStatus])(_: Traced))
        .expects(Set(action.groupId), Set(CrmActionStatus.Processed), *)
        .returning(Future.successful(Seq(previousAction)))

      (mockLeadDao
        .findByFlatIdsAndPipeline(
          _: Set[String],
          _: Long,
          _: Set[Long],
          _: Boolean
        )(_: Traced))
        .expects(Set(flatId), pipelinesConfig.requests.pipelineId, Set.empty[Long], true, *)
        .returning(Future.successful(Iterable(existingLead)))

      (mockCrmClient
        .getLead(_: Long, _: Iterable[WithParam])(_: AccessToken, _: Traced))
        .expects(leadId, Iterable.empty[WithParam], accessToken, emptyTraced)
        .returning(
          Future.successful(
            Some(
              toAmoLead(existingLead)
            )
          )
        )

      (mockRentOwnerRequestClient
        .getCurrentOwner(_: InternalGetCurrentOwner.Request)(_: Traced))
        .expects(InternalGetCurrentOwner.Request.newBuilder().setFlatId(flatId).build(), emptyTraced)
        .returning(
          Future.successful(
            Some(
              InternalGetCurrentOwner.Response.newBuilder.setOwnerUid(uid).build()
            )
          )
        )

      (mockCrmClient
        .createLeadNote(_: Long, _: Seq[CreateNoteRequest])(_: AccessToken, _: Traced))
        .expects(*, *, *, *)
        .returning(Future.unit)

      (mockCrmClient
        .createTask(_: Seq[CreateTaskRequest])(_: AccessToken, _: Traced))
        .expects(*, *, *)
        .returning(Future.unit)
    }
  }
}
