package ru.auto.salesman.test.model.gens.user

import org.scalacheck.Gen
import ru.auto.salesman.model.user.PaymentPayload
import ru.auto.salesman.service.banker.domain._
import ru.auto.salesman.test.model.gens.{BankerModelGenerators, BasicSalesmanGenerators}
import ru.yandex.vertis.banker.model.ApiModel.Target

trait BankerApiModelGenerators extends BankerModelGenerators with UserDaoGenerators {

  val PayByAccountRequestGen: Gen[PayByAccountRequest] = for {
    account <- AccountGen
    amount <- Gen.posNum[Long]
    transactionId <- readableString
    domain <- domainGen
    receiptRows <- Gen.listOf(ReceiptRowGen)
  } yield
    PayByAccountRequest(
      account,
      amount,
      PaymentPayload(transactionId),
      receiptRows
    )

  val ReceiptRowGen: Gen[ReceiptRow] = for {
    productId <- OfferProductGen.map(_.name)
    name <- readableString
    qty <- Gen.posNum[Int]
    price <- Gen.posNum[Long]
  } yield ReceiptRow(productId, name, qty, price)

  val PayWithTiedCardRequestGen: Gen[PayWithLinkedCardRequest] = for {
    account <- AccountGen
    amount <- Gen.posNum[Long]
    target <- BasicSalesmanGenerators.protoEnumWithZeroGen(
      Target.values().toSeq
    )
    payload <- paymentRequestPayloadGen(Some(domain))
    receiptRows <- Gen.listOf(ReceiptRowGen)
    parentTransactionId <- readableString
  } yield
    PayWithLinkedCardRequest(
      account,
      amount,
      target,
      payload,
      receiptRows,
      Some(parentTransactionId)
    )

  val PayRecurrentRequestGen: Gen[PayRecurrentRequest] = for {
    account <- AccountGen
    amount <- Gen.posNum[Long]
    target <- BasicSalesmanGenerators.protoEnumWithZeroGen(
      Target.values().toSeq
    )
    payload <- paymentRequestPayloadGen(Some(domain))
    receiptRows <- Gen.listOf(ReceiptRowGen)
    parentTransactionId <- readableString
  } yield
    PayRecurrentRequest(
      account,
      amount,
      target,
      payload,
      receiptRows,
      Some(parentTransactionId)
    ).unsafeRun()

}
