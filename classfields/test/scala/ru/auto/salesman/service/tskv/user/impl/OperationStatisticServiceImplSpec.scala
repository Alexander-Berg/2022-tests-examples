package ru.auto.salesman.service.tskv.user.impl

import billing.finstat.autoru_physical.AutoruPhysicalFinstat
import billing.log_model.ProductEvent
import org.joda.time.DateTime
import ru.auto.salesman.client.PassportClient
import ru.auto.salesman.model.broker.MessageId
import ru.auto.salesman.model.offer.OfferIdentity
import ru.auto.salesman.model.user.product.ProductProvider
import ru.auto.salesman.model.user.{
  PaidProduct,
  PaidTransaction,
  ProductRequest,
  TransactionRequest
}
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains, ProductStatuses}
import ru.auto.salesman.service.broker.LogsBrokerService
import ru.auto.salesman.service.tskv.user.OperationMetaInfoService
import ru.auto.salesman.service.tskv.user.domain.MetaInfo.OfferMetaInfo
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.OfferModelGenerators
import ru.auto.salesman.test.model.gens.user.UserModelGenerators

class OperationStatisticServiceImplSpec
    extends BaseSpec
    with UserModelGenerators
    with OfferModelGenerators {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  val operationMetaInfoService = mock[OperationMetaInfoService]
  val brokerService = mock[LogsBrokerService]
  val passport = mock[PassportClient]

  "OperationStatisticServiceImpl" should {
    "send message to broker with transactionId as MessageId if product not canceled" in {
      val operationStatisticService = new OperationStatisticServiceImpl(
        operationMetaInfoService,
        passport,
        brokerService,
        brokerService
      )

      val paidTransaction = paidTransactionGen().next
      val product = GoodsGen.next
      val userModeration = userModerationStatusGen.next
      val meta = OfferMetaInfo(OfferIdentityGen.next, offerGen().next)
      val testProduct = product.copy(status = ProductStatuses.Active)

      (operationMetaInfoService
        .getMetaInfo(
          _: PaidTransaction,
          _: PaidProduct
        ))
        .expects(paidTransaction, testProduct)
        .returningZ(meta)

      (passport.userModeration _)
        .expects(PassportClient.asPassportUser(product.user))
        .returningZ(userModeration)

      (brokerService
        .sendStatisticsTskvLogEntry(
          _: MessageId,
          _: Map[String, String],
          _: DateTime
        ))
        .expects(
          MessageId(paidTransaction.transactionId),
          *,
          testProduct.activated
        )
        .returningZ(())

      (brokerService
        .sendProductEvent(
          _: MessageId,
          _: ProductEvent
        ))
        .expects(
          MessageId(paidTransaction.transactionId),
          *
        )
        .returningZ(())

      (brokerService
        .sendFinStatEvent(
          _: MessageId,
          _: AutoruPhysicalFinstat
        ))
        .expects(
          MessageId(paidTransaction.transactionId),
          *
        )
        .returningZ(())

      operationStatisticService.log(paidTransaction, testProduct).success.value
    }

    "send message to broker with canceled-<transactionId> as MessageId if product canceled" in {
      val operationStatisticService = new OperationStatisticServiceImpl(
        operationMetaInfoService,
        passport,
        brokerService,
        brokerService
      )

      val paidTransaction = paidTransactionGen().next
      val product = GoodsGen.next
      val userModeration = userModerationStatusGen.next
      val meta = OfferMetaInfo(OfferIdentityGen.next, offerGen().next)
      val testProduct = product.copy(status = ProductStatuses.Canceled)
      val expectedMessageId =
        MessageId(s"canceled-${paidTransaction.transactionId}")

      (operationMetaInfoService
        .getMetaInfo(
          _: PaidTransaction,
          _: PaidProduct
        ))
        .expects(paidTransaction, testProduct)
        .returningZ(meta)

      (passport.userModeration _)
        .expects(PassportClient.asPassportUser(product.user))
        .returningZ(userModeration)

      (brokerService
        .sendStatisticsTskvLogEntry(
          _: MessageId,
          _: Map[String, String],
          _: DateTime
        ))
        .expects(expectedMessageId, *, testProduct.activated)
        .returningZ(())

      (brokerService
        .sendProductEvent(
          _: MessageId,
          _: ProductEvent
        ))
        .expects(expectedMessageId, *)
        .returningZ(())

      (brokerService
        .sendFinStatEvent(
          _: MessageId,
          _: AutoruPhysicalFinstat
        ))
        .expects(expectedMessageId, *)
        .returningZ(())

      operationStatisticService.log(paidTransaction, testProduct).success.value
    }

    "send transaction creation event to broker with buffered client" in {
      val buffered = mock[LogsBrokerService]

      val operationStatisticService = new OperationStatisticServiceImpl(
        operationMetaInfoService,
        passport,
        brokerService,
        buffered
      )

      val transactionRequest = TransactionRequestGen.next
      val transactionResult = createTransactionResultGen().next
      val offer = offerGen().next
      val offerId = OfferIdentity(offer.getId)
      val request = productRequestGen(
        ProductProvider.AutoruGoods.Placement
      ).next.copy(offer = Some(offerId))
      val meta = OfferMetaInfo(offerId, offer)

      (operationMetaInfoService
        .getMetaInfo(
          _: TransactionRequest,
          _: ProductRequest
        ))
        .expects(transactionRequest, request)
        .returningZ(meta)

      (brokerService
        .sendStatisticsTskvLogEntry(
          _: MessageId,
          _: Map[String, String],
          _: DateTime
        ))
        .expects(*, *, *)
        .never()

      (brokerService
        .sendProductEvent(
          _: MessageId,
          _: ProductEvent
        ))
        .expects(*, *)
        .never()

      (buffered
        .sendProductEvent(
          _: MessageId,
          _: ProductEvent
        ))
        .expects(*, *)
        .returningZ(())

      operationStatisticService
        .log(transactionRequest, request, transactionResult)
        .success
        .value
    }
  }

}
