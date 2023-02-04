package ru.auto.salesman.service.tskv.user.impl

import ru.auto.salesman.client.VosClient
import ru.auto.salesman.model.offer.OfferIdentity
import ru.auto.salesman.model.user.product.ProductProvider
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains, Slave}
import ru.auto.salesman.service.tskv.user.domain.MetaInfo.OfferMetaInfo
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.OfferModelGenerators
import ru.auto.salesman.test.model.gens.user.UserModelGenerators

class OperationMetaInfoServiceImplSpec
    extends BaseSpec
    with UserModelGenerators
    with OfferModelGenerators {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  val vosClient = mock[VosClient]

  val statisticLoggerLogRecordProviderImpl =
    new OperationMetaInfoServiceImpl(
      vosClient
    )

  "OperationMetaInfoServiceImpl" should {
    "prepare all the necessary meta data for paid event" in {
      val paidTransaction = paidTransactionGen().next
      val product = PaidOfferProductGen.next
      val offer = offerGen().next

      (vosClient.getOffer _)
        .expects(product.offer, Slave)
        .returningZ(offer)

      val offerMetaInfo = OfferMetaInfo(
        product.offer,
        offer
      )

      statisticLoggerLogRecordProviderImpl
        .getMetaInfo(paidTransaction, product)
        .success
        .value
        .shouldBe(offerMetaInfo)
    }

    "prepare all the necessary meta data for init event" in {
      val transactionRequest = TransactionRequestGen.next
      val offer = offerGen().next
      val offerId = OfferIdentity(offer.getId)
      val request = productRequestGen(
        ProductProvider.AutoruGoods.Placement
      ).next.copy(offer = Some(offerId))

      (vosClient.getOffer _)
        .expects(offerId, Slave)
        .returningZ(offer)

      val offerMetaInfo = OfferMetaInfo(
        offerId,
        offer
      )

      statisticLoggerLogRecordProviderImpl
        .getMetaInfo(transactionRequest, request)
        .success
        .value
        .shouldBe(offerMetaInfo)
    }
  }

}
