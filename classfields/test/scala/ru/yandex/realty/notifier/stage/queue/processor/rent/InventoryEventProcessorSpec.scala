package ru.yandex.realty.notifier.stage.queue.processor.rent

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.time.{Millis, Minutes, Span}
import ru.yandex.realty.clients.rent.{
  RentContractServiceClient,
  RentInventoryServiceClient,
  RentOwnerRequestServiceClient
}
import ru.yandex.realty.features.FeaturesStubComponent
import ru.yandex.realty.notifier.manager.{PushnoyManager, SmsSenderManager}
import ru.yandex.realty.notifier.model.{NotificationEvent, NotificationTarget, RentUserTarget}
import ru.yandex.realty.notifier.model.enums.EventType
import ru.yandex.realty.notifier.model.enums.EventType.EventType
import ru.yandex.realty.notifier.proto.model.payload.{
  EventPayload,
  OwnerNeedToConfirmInventoryEventPayload,
  OwnerNeedToFillOutInventoryEventPayload,
  TenantNeedToConfirmInventoryEventPayload
}

import scala.jdk.CollectionConverters._
import ru.yandex.realty.notifier.stage.queue.processor.JustSendNotificationProcessor2.{ComeBackAt, Sent}
import ru.yandex.realty.pushnoy.model.{PalmaPushInfo, Targets}
import ru.yandex.realty.rent.proto.api.contract.{GetModerationRentContractsResponse, RentContract}
import ru.yandex.realty.rent.proto.api.flats.{OwnerRequest, OwnerRequestDetailedInfo}
import ru.yandex.realty.rent.proto.api.flats.OwnerRequestNamespace.Status
import ru.yandex.realty.rent.proto.api.internal.inventory.{InternalGetInventoryRequest, InternalInventoryResponse}
import ru.yandex.realty.rent.proto.api.internal.{
  InternalGetContracts,
  InternalGetCurrentRentedOwnerRequest,
  InternalGetOwnerRequest
}
import ru.yandex.realty.rent.proto.api.inventory.Inventory
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.{AsyncSpecBase, SpecBase}
import ru.yandex.realty.util.protobuf.ProtobufFormats
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class InventoryEventProcessorSpec extends SpecBase with AsyncSpecBase with ProtobufFormats with FeaturesStubComponent {
  implicit private val traced: Traced = Traced.empty
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(1, Minutes), interval = Span(50, Millis))

  features.InventoryPush.setNewState(true)

  private val pushnoyManager: PushnoyManager = mock[PushnoyManager]
  private val smsSenderManager: SmsSenderManager = mock[SmsSenderManager]
  private val rentOwnerRequestClient = mock[RentOwnerRequestServiceClient]
  private val rentContractClient = mock[RentContractServiceClient]
  private val rentInventoryClient = mock[RentInventoryServiceClient]
  private val flatId = "flatId"
  private val ownerRequestId = "ownerRequestId"
  private val uid = 12345L
  private val target = RentUserTarget(uid)
  private val OwnerNeedToConfirmInventoryORStatuses = Set(Status.LOOKING_FOR_TENANT, Status.COMPLETED)

  "NeedToConfirmInventoryProcessor" should {
    "reschedule tenant need to confirm inventory push when tenant check-in date in the future" in {
      val tenantCheckInDateAfterNow = DateTime.now().plusDays(1).withTimeAtStartOfDay()

      (rentContractClient
        .getContracts(_: String)(_: Traced))
        .expects(*, *)
        .returning(
          Future.successful(
            buildRentContractsResponse(tenantCheckInDateAfterNow)
          )
        )
        .once

      (pushnoyManager
        .sendPush(_: NotificationTarget, _: PalmaPushInfo, _: Targets.Value, _: Option[String])(_: Traced))
        .expects(*, *, *, *, *)
        .returning(Future.successful(1))
        .never()

      val payload = EventPayload
        .newBuilder()
        .setTenantNeedToConfirmInventory(
          TenantNeedToConfirmInventoryEventPayload.newBuilder().setFlatId(flatId).setOwnerRequestId(ownerRequestId)
        )
        .build()

      val event =
        buildEvent(EventType.TenantNeedToConfirmInventory, target, payload, "tenantNeedToConfirmInventoryEvent")

      getNotificationProcessor(EventType.TenantNeedToConfirmInventory)
        .processSingleEvent2(event, target)
        .futureValue shouldBe ComeBackAt(tenantCheckInDateAfterNow)
    }

    "send tenant need to confirm inventory push when tenant check-in date" in {
      val rentStartDateBeforeNow = DateTime.now().minusHours(1)

      (rentContractClient
        .getContracts(_: String)(_: Traced))
        .expects(*, *)
        .returning(
          Future.successful(
            buildRentContractsResponse(rentStartDateBeforeNow)
          )
        )
        .once()

      (pushnoyManager
        .sendPush(_: NotificationTarget, _: PalmaPushInfo, _: Targets.Value, _: Option[String])(_: Traced))
        .expects(*, *, *, *, *)
        .returning(Future.successful(1))
        .once()

      val payload = EventPayload
        .newBuilder()
        .setTenantNeedToConfirmInventory(
          TenantNeedToConfirmInventoryEventPayload.newBuilder().setFlatId(flatId).setOwnerRequestId(ownerRequestId)
        )
        .build()

      val event =
        buildEvent(EventType.TenantNeedToConfirmInventory, target, payload, "tenantNeedToConfirmInventoryEvent")

      getNotificationProcessor(EventType.TenantNeedToConfirmInventory)
        .processSingleEvent2(event, target)
        .futureValue shouldBe Sent
    }

    "send push when manager has changed inventory" in {
      OwnerNeedToConfirmInventoryORStatuses.foreach { status =>
        (rentOwnerRequestClient
          .getOwnerRequest(_: String)(_: Traced))
          .expects(*, *)
          .returning(Future.successful(Some(buildOwnerRequestResponse(status))))
          .once()
        (pushnoyManager
          .sendPush(_: NotificationTarget, _: PalmaPushInfo, _: Targets.Value, _: Option[String])(_: Traced))
          .expects(*, *, *, *, *)
          .returning(Future.successful(1))
          .once()

        val payload = EventPayload
          .newBuilder()
          .setOwnerNeedToConfirmInventory(
            OwnerNeedToConfirmInventoryEventPayload.newBuilder().setFlatId(flatId).setOwnerRequestId(ownerRequestId)
          )
          .build()

        val event =
          buildEvent(EventType.OwnerNeedToConfirmInventory, target, payload, "ownerNeedToConfirmInventoryEvent")

        getNotificationProcessor(EventType.OwnerNeedToConfirmInventory)
          .processSingleEvent2(event, target)
          .futureValue shouldBe Sent
      }
    }

    "send owner need to fill out inventory" in {
      (rentInventoryClient
        .getLastInventory(_: InternalGetInventoryRequest)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(buildInventoryResponse(confirmedByOwner = false, confirmedByTenant = false)))
        .once()
      (pushnoyManager
        .sendPush(_: NotificationTarget, _: PalmaPushInfo, _: Targets.Value, _: Option[String])(_: Traced))
        .expects(*, *, *, *, *)
        .returning(Future.successful(1))
        .once()

      val payload = EventPayload
        .newBuilder()
        .setOwnerNeedToFillOutInventory(
          OwnerNeedToFillOutInventoryEventPayload.newBuilder().setFlatId(flatId).setOwnerRequestId(ownerRequestId)
        )
        .build()

      val event =
        buildEvent(EventType.OwnerNeedToFillOutInventory, target, payload, "ownerNeedToFillOutInventoryEvent")

      getNotificationProcessor(EventType.OwnerNeedToFillOutInventory)
        .processSingleEvent2(event, target)
        .futureValue shouldBe Sent
    }
  }

  def getNotificationProcessor(eventType: EventType): InventoryEventProcessor =
    new InventoryEventProcessor(
      eventType,
      rentOwnerRequestClient,
      rentContractClient,
      rentInventoryClient,
      pushnoyManager,
      smsSenderManager,
      features
    )

  def buildEvent(
    eventType: EventType,
    target: NotificationTarget,
    payload: EventPayload,
    eventId: String
  ): NotificationEvent =
    NotificationEvent(
      queueId = 0,
      target = target,
      eventType = eventType,
      eventId = eventId,
      processTime = DateTimeUtil.now(),
      createTime = DateTimeUtil.now(),
      payload = payload
    )

  def buildRentedOwnerRequestResponse(ownerRequestId: String): InternalGetCurrentRentedOwnerRequest.Response =
    InternalGetCurrentRentedOwnerRequest.Response
      .newBuilder()
      .setOwnerRequestId(ownerRequestId)
      .setTenantUid(uid)
      .build()

  private def buildRentContractsResponse(rentStartDate: DateTime): InternalGetContracts.Response = {
    InternalGetContracts.Response
      .newBuilder()
      .setResponse(
        GetModerationRentContractsResponse
          .newBuilder()
          .addAllContracts(
            Seq(RentContract.newBuilder().setTenantCheckInDate(DateTimeFormat.write(rentStartDate)).build()).asJava
          )
      )
      .build()
  }

  private def buildInventoryResponse(confirmedByOwner: Boolean, confirmedByTenant: Boolean) = {
    InternalInventoryResponse
      .newBuilder()
      .setInventory(
        Inventory.newBuilder().setConfirmedByOwner(confirmedByOwner).setConfirmedByTenant(confirmedByTenant)
      )
      .build()
  }

  def buildOwnerRequestResponse(status: Status): InternalGetOwnerRequest.Response =
    InternalGetOwnerRequest.Response
      .newBuilder()
      .setOwnerRequest(
        OwnerRequestDetailedInfo.newBuilder().setOwnerRequest(OwnerRequest.newBuilder().setStatus(status))
      )
      .build()
}
