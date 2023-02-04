package ru.yandex.realty.unifiedoffer.offertype

import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.offer.{Transaction, TransactionCondition}
import ru.yandex.realty.proto.offer.TransactionConditionType

import scala.collection.JavaConverters._

/**
  * @author nstaroverova
  */
@RunWith(classOf[JUnitRunner])
class TransactionConditionsProtoConverterSpec extends SpecBase with Matchers {

  "TransactionConditionsProtoConverter.toMessage " should {
    "return correct transformation " in {
      val transaction = new Transaction
      transaction.setTransactionCondition(TransactionCondition.CLEANING_INCLUDED, true)
      transaction.setTransactionCondition(TransactionCondition.HAGGLE, false)
      transaction.setTransactionCondition(TransactionCondition.MORTGAGE, null)

      val result = TransactionConditionsProtoConverter.toMessage(transaction)

      val cleaning = result.getConditionsList.asScala
        .find(_.getType == TransactionConditionType.TRANSACTION_CONDITION_CLEANING_INCLUDED)
      val haggle =
        result.getConditionsList.asScala.find(_.getType == TransactionConditionType.TRANSACTION_CONDITION_HAGGLE)
      val mortgage =
        result.getConditionsList.asScala.find(_.getType == TransactionConditionType.TRANSACTION_CONDITION_MORTGAGE)

      cleaning.exists(_.getAllowed == true) should be(true)
      haggle.exists(_.getAllowed == false) should be(true)
      mortgage.isEmpty should be(true)
      result.getConditionsCount should be(2)

    }

    "return empty conditions list if conditions is empty " in {
      val result = TransactionConditionsProtoConverter.toMessage(new Transaction)

      result.getConditionsCount should be(0)
    }

    "return empty conditions list transaction is empty " in {
      val result = TransactionConditionsProtoConverter.toMessage(null)

      result.getConditionsCount should be(0)
    }
  }

}
