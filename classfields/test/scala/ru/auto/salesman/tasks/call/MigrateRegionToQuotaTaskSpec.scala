package ru.auto.salesman.tasks.call

import com.typesafe.scalalogging.StrictLogging
import ru.auto.salesman.dao.{ClientDao, QuotaRequestDao}
import ru.auto.salesman.dao.ClientDao.{Filter, Patch}
import ru.auto.salesman.model.{Client, ClientId, PoiId}
import ru.auto.salesman.service.billingcampaign.{
  BillingCampaignService,
  CallCampaignNotFoundException
}
import ru.auto.salesman.tasks.kafka.services.ondelete.BillingCampaignDisablingService
import ru.auto.salesman.tasks.kafka.services.BillingTestData
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.Task
import ru.yandex.vertis.generators.ProducerProvider._
import zio.ZIO

import scala.util.control.NoStackTrace

class MigrateRegionToQuotaTaskSpec extends BaseSpec with StrictLogging {

  private val billingCampaignService = mock[BillingCampaignService]
  private val quotaRequestDao = mock[QuotaRequestDao]

  private val billingCampaignDisablingService =
    mock[BillingCampaignDisablingService]
  private val campaignHeader = BillingTestData.campaignHeader(true)

  private val clientDao = mock[ClientDao]

  "MigrateRegionToQuotaTask" should {

    "disable calls auction for client without call campaign" in {
      // client without a campaign
      // get call campaign and fail on it with CallCampaignNotFoundException
      // disable call auction
      // do nothing more
      val clientWithoutCampaign = {
        val client = ClientRecordGen.next
          .copy(clientId = 0, paidCallsAvailable = true)

        (billingCampaignService.getCallCampaign _)
          .expects(client.clientId)
          .throwingZ(CallCampaignNotFoundException(client.clientId))

        (clientDao
          .setPaidCallsAvailable(_: ClientId, _: Boolean))
          .expects(0L, false)
          .returningZ(unit)

        client
      }
      val clients = List(clientWithoutCampaign)
      (clientDao.get _).expects(*).returningZ(clients)

      val task = new MigrateRegionToQuotaTask(
        clientDao,
        quotaRequestDao,
        billingCampaignDisablingService,
        billingCampaignService
      )(None, true, None)

      task.execute()
    }

    "migration for standard client" in {

      val clientWithCampaign = {
        val client = ClientRecordGen.next
          .copy(clientId = 1, paidCallsAvailable = true)

        (billingCampaignService.getCallCampaign _)
          .expects(client.clientId)
          .returningZ(campaignHeader)

        (clientDao
          .setPaidCallsAvailable(_: ClientId, _: Boolean))
          .expects(client.clientId, *)
          .returningZ(unit)

        (billingCampaignDisablingService.disableClientCampaign _)
          .expects(client.clientId, *)
          .returningZ(Seq(campaignHeader))

        (clientDao.setCallTrackingEnabled _)
          .expects(client.clientId, *)
          .returningZ(unit)

        (quotaRequestDao.add _)
          .expects(*)
          .returningT(unit)

        client
      }

      val clients = List(clientWithCampaign)
      (clientDao.get _).expects(*).returningZ(clients)

      val task = new MigrateRegionToQuotaTask(
        clientDao,
        quotaRequestDao,
        billingCampaignDisablingService,
        billingCampaignService
      )(None, true, None)

      task.execute()

    }

    "execute the calls auction rollback in case of exception" in {
      // client with a campaign, but with an error on disabling it
      // get call campaign
      // filter it with campaign.isEnabled
      // enabling call tracking
      // fail on disabling campaign
      // rolling back call auction to retry later

      val clientWithException = {
        val client = ClientRecordGen.next
          .copy(clientId = 2, paidCallsAvailable = true)

        (billingCampaignService.getCallCampaign _)
          .expects(client.clientId)
          .returningZ(campaignHeader)

        (billingCampaignDisablingService.disableClientCampaign _)
          .expects(client.clientId, *)
          .throwingZ(
            new Exception("disable client campaign exception") with NoStackTrace
          )

        client
      }

      val clients = List(clientWithException)

      var paidCallsAvailable = true
      var callTrackingEnabled = false
      val inMemoryClientDao = new ClientDao {
        def getPoiId(clientId: ClientId): Task[Option[PoiId]] = ???
        def update(clientId: ClientId, patch: Patch): Task[Unit] = ???
        def loadMarks(clientId: ClientId): Task[List[String]] = ???
        def getClientByPoiId(poiId: PoiId): Task[Option[Client]] = ???

        def get(filter: Filter): Task[List[Client]] = ZIO(clients)

        def setPaidCallsAvailable(
            clientId: ClientId,
            available: Boolean
        ): Task[Unit] =
          ZIO { paidCallsAvailable = available }

        def setCallTrackingEnabled(
            clientId: ClientId,
            enabled: Boolean
        ): Task[Unit] =
          ZIO { callTrackingEnabled = enabled }

      }

      val task = new MigrateRegionToQuotaTask(
        inMemoryClientDao,
        quotaRequestDao,
        billingCampaignDisablingService,
        billingCampaignService
      )(None, true, None)

      task.execute()

      paidCallsAvailable shouldBe true
      callTrackingEnabled shouldBe true

      //preparing retry
      (billingCampaignService.getCallCampaign _)
        .expects(clientWithException.clientId)
        .returningZ(campaignHeader)

      (billingCampaignDisablingService.disableClientCampaign _)
        .expects(clientWithException.clientId, *)
        .returningZ(Seq(campaignHeader))

      (quotaRequestDao.add _)
        .expects(*)
        .returningT(unit)

      task.execute()

      paidCallsAvailable shouldBe false
      callTrackingEnabled shouldBe true
    }
  }
}
