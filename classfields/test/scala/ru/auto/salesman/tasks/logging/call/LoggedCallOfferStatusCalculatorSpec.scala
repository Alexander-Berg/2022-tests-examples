package ru.auto.salesman.tasks.logging.call

import org.joda.time.DateTime
import ru.auto.api.ApiOfferModel.Section
import ru.auto.salesman.dao.BalanceClientDao
import ru.auto.salesman.model.OfferStatuses.OfferStatus
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.{
  BalanceAgencyId,
  BalanceClientId,
  CityId,
  Client,
  ClientId,
  ClientStatus,
  ClientStatuses,
  Offer,
  OfferCategories,
  OfferCurrencies,
  OfferStatuses,
  RegionId
}
import ru.auto.salesman.service.logging.ServiceAccessLogger
import ru.auto.salesman.tasks.kafka.services.BillingTestData
import ru.auto.salesman.tasks.kafka.services.BillingTestData.ExistingCampaignId
import ru.auto.salesman.test.BaseSpec
import ru.yandex.vertis.billing.Model.InactiveReason

//noinspection NameBooleanParameters
class LoggedCallOfferStatusCalculatorSpec
    extends BaseSpec
    with LoggedCallOfferStatusCalculator
    with ServiceAccessLogger {

  "LoggedCallOfferStatusCalculator" should {

    "calculate call offer status args" in {
      val clientId = 20101
      val clientStatus = ClientStatuses.Active
      val offerId = "1077379029-68559178"
      val offerStatus = OfferStatuses.Show
      val balanceClientId = 4327999
      val balanceAgencyId = 4809218
      val inactiveReason = InactiveReason.NO_ENOUGH_FUNDS

      val client = makeClient(clientId, clientStatus)
      val offer = makeOffer(offerId, offerStatus)
      val balanceClient = makeBalanceClient(balanceClientId, balanceAgencyId)
      val campaign = makeCampaign(inactiveReason)

      val result = calculateCallOfferStatusArgs(
        client,
        offer,
        balanceClient,
        Some(campaign)
      )
      // compare ignoring case to allow more refactoring without test failures
      val paramValues = result.values.map(_.toString.toLowerCase)

      val expected = Seq(
        clientId,
        clientStatus,
        offerId,
        offerStatus,
        balanceClientId,
        balanceAgencyId,
        ExistingCampaignId,
        inactiveReason
      ).map(_.toString.toLowerCase)

      paramValues should contain allElementsOf expected
    }
  }

  // don't care about most of params in these methods, so put some defaults
  // and don't even name them
  private def makeClient(id: ClientId, status: ClientStatus) =
    Client(
      id,
      None,
      None,
      None,
      RegionId(-1),
      CityId(-1),
      status,
      Set(),
      false,
      None,
      false,
      true
    )

  private def makeOffer(id: String, status: OfferStatus) = {
    val parsedId = AutoruOfferId(id)
    Offer(
      parsedId.id,
      parsedId.hash.getOrElse(""),
      OfferCategories.Cars,
      Section.NEW,
      0,
      OfferCurrencies.RUR,
      status,
      -1,
      DateTime.now(),
      DateTime.now(),
      DateTime.now(),
      None
    )
  }

  private def makeBalanceClient(
      balanceClientId: BalanceClientId,
      balanceAgencyId: BalanceAgencyId
  ) =
    BalanceClientDao.BaseRecord(
      -1,
      balanceClientId,
      Some(balanceAgencyId),
      false
    )

  private def makeCampaign(inactiveReason: InactiveReason) =
    BillingTestData
      .campaignHeader(isEnabled = false)
      .toBuilder
      .setInactiveReason(inactiveReason)
      .build()
}
