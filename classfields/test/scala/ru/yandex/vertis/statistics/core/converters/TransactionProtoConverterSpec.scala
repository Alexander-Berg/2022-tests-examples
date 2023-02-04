package ru.yandex.vertis.statistics.core.converters

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.realty.clients.abram.Tariffs
import ru.yandex.realty.statistics.model.DruidRealtyTransactionRow.Product
import ru.yandex.vertis.billing.BillingEvent.{BillingOperation, CommonBillingInfo, SimpleProduct}
import ru.yandex.vertis.billing.BillingEvent.BillingOperation.WithdrawPayload
import ru.yandex.vertis.billing.BillingEvent.CommonBillingInfo.TransactionInfo.TransactionType
import ru.yandex.vertis.billing.BillingEvent.CommonBillingInfo.TransactionInfo
import ru.yandex.vertis.billing.Model

@RunWith(classOf[JUnitRunner])
class TransactionProtoConverterSpec extends SpecBase with ScalaCheckPropertyChecks {

  val Id = "123r4asd"

  def ValidWithdrawalBuilder: WithdrawPayload.Builder =
    WithdrawPayload
      .newBuilder()
      .setCallFact(
        CommonBillingInfo.CallFact
          .newBuilder()
          .setCallFact(Model.CallFact.newBuilder().setId(Id))
      )
      .setActual(200L)
      .setExpected(200L)
      .setProduct(
        SimpleProduct
          .newBuilder()
          .setName(Tariffs.CallsMaximum.nameCampaign)
          .build()
      )

  val InvalidSumsWithdrawal: WithdrawPayload =
    ValidWithdrawalBuilder
      .setActual(-1L)
      .setExpected(-12000L)
      .build()

  val InvalidProductWithdrawal: WithdrawPayload =
    ValidWithdrawalBuilder
      .setProduct(
        SimpleProduct
          .newBuilder()
          .setName("NOT-EXISTING-PRODUCT-NAME")
          .build()
      )
      .build()

  def BillingOperationBuilder: BillingOperation.Builder = {
    val transactionInfo = TransactionInfo
      .newBuilder()
      .setId(Id)
      .setAmount(200L)
      .setType(TransactionType.INCOMING)
      .build()
    BillingOperation
      .newBuilder()
      .setId(Id)
      .setTransactionInfo(transactionInfo)
  }

  val NotExistingTransactionInfoOperation: BillingOperation =
    BillingOperationBuilder
      .clearTransactionInfo()
      .build()

  val converter: TransactionProtoConverter.type = TransactionProtoConverter
  "TransactionProtoConverter" should {
    "convert incoming message to transaction correctly for Maximum product" in {
      val transactionOpt =
        converter.fromMessage(BillingOperationBuilder.setWithdrawPayload(ValidWithdrawalBuilder).build())
      transactionOpt shouldNot be(None)
      transactionOpt.get.product shouldBe Product.PAYED_MAXIMUM_CALL
    }
    "convert incoming message to transaction correctly for CallsMinimum product" in {
      val payloadWithMinimum = ValidWithdrawalBuilder.build().toBuilder
      payloadWithMinimum.getProductBuilder.setName(Tariffs.CallsMinimum.nameCampaign)
      val transactionOpt = converter.fromMessage(BillingOperationBuilder.setWithdrawPayload(payloadWithMinimum).build())
      transactionOpt shouldNot be(None)
      transactionOpt.get.product shouldBe Product.PAYED_TUZ_CALL
    }
    "convert incoming message to transaction correctly for TUZ product" in {
      val payloadWithMinimum = ValidWithdrawalBuilder.build().toBuilder
      payloadWithMinimum.getProductBuilder.setName(Tariffs.CallsExtended.nameCampaign)
      val transactionOpt = converter.fromMessage(BillingOperationBuilder.setWithdrawPayload(payloadWithMinimum).build())
      transactionOpt shouldNot be(None)
      transactionOpt.get.product shouldBe Product.PAYED_TUZ_CALL
    }
    "return None if a product wasn't found" in {
      val message = converter.fromMessage(BillingOperationBuilder.setWithdrawPayload(InvalidProductWithdrawal).build())
      message should be(None)
    }
    "return None if transaction sums are incorrect" in {
      val message = converter.fromMessage(BillingOperationBuilder.setWithdrawPayload(InvalidSumsWithdrawal).build())
      message should be(None)
    }
    "return None if withdrawal was not defined" in {
      val message = converter.fromMessage(BillingOperationBuilder.build())
      message should be(None)
    }
    "return None if transaction was not defined" in {
      val message = converter.fromMessage(NotExistingTransactionInfoOperation)
      message should be(None)
    }
  }
}
