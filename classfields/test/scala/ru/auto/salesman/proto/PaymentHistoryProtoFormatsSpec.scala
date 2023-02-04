package ru.auto.salesman.proto

import ru.auto.api.user.PaymentOuterClass.Payment
import ru.auto.api.user.PaymentOuterClass.Payment.ProductTransaction.Product
import ru.auto.api.user.payment.Payment.ProductTransaction.ProductTransactionStatus
import ru.auto.api.user.payment.Payment.Status
import ru.auto.api.user.payment.Payment.Status.toJavaValue
import ru.auto.salesman.model.{DeprecatedDomain, TransactionStatuses}
import ru.auto.salesman.service.impl.user.TransactionServiceImpl
import ru.auto.salesman.test.{BaseSpec, IntegrationPropertyCheckConfig}
import ru.auto.salesman.test.model.gens.user.UserDaoGenerators
import ru.auto.salesman.environment.RichDateTime
import ru.auto.salesman.model.DeprecatedDomains.AutoRu
import ru.auto.salesman.model.offer.OfferIdentity
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Placement
import ru.auto.salesman.model.user.product.ProductProvider.AutoruSubscriptions.OffersHistoryReports

import scala.collection.JavaConverters._

class PaymentHistoryProtoFormatsSpec
    extends BaseSpec
    with UserDaoGenerators
    with IntegrationPropertyCheckConfig {
  import PaymentHistoryProtoFormats._

  implicit override def domain: DeprecatedDomain = AutoRu

  "transactionStatusToPaymentStatus" should {
    "convert unpaid statuses to new" in {
      transactionStatusToPaymentStatus(
        TransactionStatuses.New
      ) shouldBe Status.NEW
      transactionStatusToPaymentStatus(
        TransactionStatuses.Process
      ) shouldBe Status.NEW
    }

    "convert paid statuses to paid" in {
      transactionStatusToPaymentStatus(
        TransactionStatuses.Paid
      ) shouldBe Status.PAID
      transactionStatusToPaymentStatus(
        TransactionStatuses.Closed
      ) shouldBe Status.PAID
    }

    "convert canceled status to canceled" in {
      transactionStatusToPaymentStatus(
        TransactionStatuses.Canceled
      ) shouldBe Status.CANCELED
    }
  }

  "transactionStatusToProtoStatus" should {
    "properly convert status" in {
      transactionStatusToProtoStatus(
        TransactionStatuses.New
      ) shouldBe ProductTransactionStatus.NEW
      transactionStatusToProtoStatus(
        TransactionStatuses.Process
      ) shouldBe ProductTransactionStatus.PROCESS
      transactionStatusToProtoStatus(
        TransactionStatuses.Paid
      ) shouldBe ProductTransactionStatus.PAID
      transactionStatusToProtoStatus(
        TransactionStatuses.Closed
      ) shouldBe ProductTransactionStatus.CLOSED
      transactionStatusToProtoStatus(
        TransactionStatuses.Canceled
      ) shouldBe ProductTransactionStatus.CANCELED
    }
  }

  "extractOfferId" should {
    "extract offer from first context" in {
      forAll(productRequestGen(Placement), transactionRecordGen()) {
        (productRequest, genTransaction) =>
          val transaction = TransactionServiceImpl.asTransaction(genTransaction)
          val productRequest1 =
            productRequest.copy(offer = Some(OfferIdentity("1")))
          val productRequest2 =
            productRequest.copy(offer = Some(OfferIdentity("2")))
          val testTransaction =
            transaction.copy(payload = List(productRequest1, productRequest2))
          extractOfferId(testTransaction) shouldBe Some("1")
      }
    }

    "extract offer from second context if first empty" in {
      forAll(
        productRequestGen(OffersHistoryReports(1)),
        productRequestGen(Placement),
        transactionRecordGen()
      ) { (product1, product2, genTransaction) =>
        val transaction = TransactionServiceImpl.asTransaction(genTransaction)
        val productRequest1 = product1.copy(offer = None)
        val productRequest2 = product2.copy(offer = Some(OfferIdentity("2")))
        val testTransaction =
          transaction.copy(payload = List(productRequest1, productRequest2))
        extractOfferId(testTransaction) shouldBe Some("2")
      }
    }
  }

  "toProto" should {
    "convert transaction to payment" in {
      forAll(transactionRecordGen()) { testTransaction =>
        val transaction = TransactionServiceImpl.asTransaction(testTransaction)
        val productDescriptions = testTransaction.payload
          .map(productRequest =>
            (productRequest.product, s"desc:${productRequest.product.name}")
          )
          .toMap

        val expectedProducts = testTransaction.payload.map { productRequest =>
          Product
            .newBuilder()
            .setHumanName(s"desc:${productRequest.product.name}")
            .setName(productRequest.product.name)
            .setAlias(productRequest.product.alias)
            .build()
        }

        val productTransaction = Payment.ProductTransaction
          .newBuilder()
          .addAllProducts(expectedProducts.asJava)
          .setAmount(testTransaction.amount)
          .setCreatedAt(testTransaction.createdAt.asTimestamp)
          .setId(testTransaction.transactionId)
          .setStatus(
            ProductTransactionStatus.toJavaValue(
              transactionStatusToProtoStatus(testTransaction.status)
            )
          )

        testTransaction.paidAt.foreach(dt => productTransaction.setPaidAt(dt.asTimestamp))
        extractOfferId(transaction).foreach(productTransaction.setOffer)

        val expectedReason =
          Payment.PaymentReason
            .newBuilder()
            .setProductTransaction(productTransaction)

        val expectedPayment = Payment
          .newBuilder()
          .setAmount(testTransaction.amount)
          .setStatus(
            toJavaValue(
              transactionStatusToPaymentStatus(testTransaction.status)
            )
          )
          .setTimestamp(testTransaction.createdAt.asTimestamp)
          .setReason(expectedReason)
          .build()

        transaction.toProto(productDescriptions) shouldBe expectedPayment

      }
    }
  }

}
