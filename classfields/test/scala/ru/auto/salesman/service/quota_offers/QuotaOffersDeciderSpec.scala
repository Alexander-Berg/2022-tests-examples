package ru.auto.salesman.service.quota_offers

import ru.auto.salesman.dao.ClientDao.ForIdWithPoi7
import ru.auto.salesman.dao.OfferDao.ForClientPaymentGroupStatus
import ru.auto.salesman.dao.{ClientDao, OfferDao, QuotaDao}
import ru.auto.salesman.model.{
  Client,
  ClientId,
  Offer,
  PaymentGroup,
  Poi7Value,
  StoredQuota,
  _
}
import ru.auto.salesman.test.service.payment_model.TestPaymentModelFactory
import ru.auto.salesman.service.quota_offers.QuotaOffersTestData._
import ru.auto.salesman.service.QuotaService
import ru.auto.salesman.service.client.ClientService
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens
import ru.auto.salesman.util.RequestContext
import ru.yandex.vertis.generators.ProducerProvider.asProducer

import scala.util.{Success, Try}

class QuotaOffersDeciderSpec extends BaseSpec {

  private val offerDao = mock[OfferDao]
  private val categorizedOfferDao = mock[OfferDao]
  private val clientDao = mock[ClientDao]
  private val clientService = mock[ClientService]
  private val quotaService = mock[QuotaService]

  private val paymentModelFactory =
    TestPaymentModelFactory.withoutSingleWithCalls()

  val decider =
    new QuotaOffersDecider(
      new QuotaOffersManager(
        offerDao,
        categorizedOfferDao,
        clientDao,
        clientService,
        quotaService,
        paymentModelFactory
      )
    )

  "QuotaOffersDecider" should {
    "decide to activate offer" in {
      val offer = gens.oldOfferGen().next
      val offers = genOffers(testQuotaSize.toInt - 1) :+ offer

      whenClientDaoGet(testClientId, Some(testClientRecord))
      whenClientSource(testClient)
      whenOfferDaoGet(testPaymentGroup, testClientId)(Success(offers))
      whenQuotaService(Success(Iterable(testQuota)))

      decider
        .canActivate(offer.id, testQuota.quotaType, testQuota.clientId)
        .success
        .value shouldBe true
    }

    "decide to activate hidden offer if quota permits" in {
      val offer = gens.oldOfferGen().next
      val offers = genOffers(testQuotaSize.toInt - 1)

      whenClientDaoGet(testClientId, Some(testClientRecord))
      whenClientSource(testClient)
      whenOfferDaoGet(testPaymentGroup, testClientId)(Success(offers))
      whenQuotaService(Success(Iterable(testQuota)))

      decider
        .canActivate(offer.id, testQuota.quotaType, testQuota.clientId)
        .success
        .value shouldBe true
    }

    "decide to not activate offer if no quota left" in {
      val offer = gens.oldOfferGen().next
      val offers = genOffers(testQuotaSize + 1)
        .map(_.copy(status = OfferStatuses.Show)) :+ offer

      whenClientDaoGet(testClientId, Some(testClientRecord))
      whenClientSource(testClient)
      whenOfferDaoGet(testPaymentGroup, testClientId)(Success(offers))
      whenQuotaService(Success(Iterable(testQuota)))

      decider
        .canActivate(offer.id, testQuota.quotaType, testQuota.clientId)
        .success
        .value shouldBe false
    }

    "decide to not activate hidden offer if no quota left" in {
      val offer = gens.oldOfferGen().next
      val offers = genOffers(testQuotaSize + 1)
        .map(_.copy(status = OfferStatuses.Show))

      whenClientDaoGet(testClientId, Some(testClientRecord))
      whenClientSource(testClient)
      whenOfferDaoGet(testPaymentGroup, testClientId)(Success(offers))
      whenQuotaService(Success(Iterable(testQuota)))

      decider
        .canActivate(offer.id, testQuota.quotaType, testQuota.clientId)
        .success
        .value shouldBe false
    }
  }

  protected def whenClientSource(response: Client): Unit =
    (clientService.getByIdOrFail _)
      .expects(testClientId, false)
      .returningZ(response)

  protected def whenClientDaoGet(id: ClientId, result: Option[Client]): Unit =
    (clientDao.get _)
      .expects(argThat { arg: ForIdWithPoi7 =>
        arg match {
          case ForIdWithPoi7(`id`, (_, Poi7Value("1"))) => true
          case other => false
        }
      })
      .returningZ(result.toList)

  protected def whenQuotaService(response: Try[Iterable[StoredQuota]]): Unit =
    (quotaService
      .get(_: QuotaDao.Filter)(_: RequestContext))
      .expects(*, *)
      .returning(response)

  protected def whenOfferDaoGet(paymentGroup: PaymentGroup, clientId: ClientId)(
      offers: Try[List[Offer]]
  ): Unit =
    (offerDao.get _)
      .expects {
        argThat { arg: ForClientPaymentGroupStatus =>
          arg match {
            case ForClientPaymentGroupStatus(cId, pg, _)
                if clientId == cId
                  && pg.category == paymentGroup.category
                  && pg.section == paymentGroup.section =>
              true
            case other => false
          }
        }
      }
      .returning(offers)
}
