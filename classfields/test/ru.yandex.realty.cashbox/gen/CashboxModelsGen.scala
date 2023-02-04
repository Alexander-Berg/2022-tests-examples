package ru.yandex.realty.cashbox.gen

import org.scalacheck.Gen
import ru.yandex.realty.cashbox.model.enums.{ReceiptRentType, ReceiptStatus}
import ru.yandex.realty.cashbox.model.{Receipt, Spirit}
import ru.yandex.realty.cashbox.proto.receipt.{Receipt => ApiReceipt}
import ru.yandex.vertis.generators.{BasicGenerators, DateTimeGenerators}

trait CashboxModelsGen extends BasicGenerators with DateTimeGenerators {

  def receiptGen: Gen[Receipt] =
    for {
      receiptId <- readableString
      paymentId <- readableString
      data = ApiReceipt.getDefaultInstance
      receiptStatus <- Gen.oneOf(Seq(ReceiptStatus.Active, ReceiptStatus.Approved, ReceiptStatus.Sent))
      rentType <- Gen.oneOf(
        Seq(ReceiptRentType.Agent, ReceiptRentType.Commission, ReceiptRentType.CommissionPrepayment)
      )
      spiritId <- Gen.option(Gen.choose(0L, 10000L))
      spiritCheckUrl <- Gen.option(readableString)
      spiritData <- Gen.option(readableString)
      spiritErrorMessage <- Gen.option(readableString)
      created <- dateTimeInFuture()
      updated <- dateTimeInFuture()
      visitTime <- Gen.option(dateTimeInFuture())
      shardKey <- posNum[Int]
    } yield Receipt(
      receiptId,
      paymentId,
      data,
      receiptStatus,
      rentType,
      Spirit(spiritId, spiritCheckUrl, spiritData, spiritErrorMessage),
      created,
      updated,
      visitTime,
      shardKey
    )
}
