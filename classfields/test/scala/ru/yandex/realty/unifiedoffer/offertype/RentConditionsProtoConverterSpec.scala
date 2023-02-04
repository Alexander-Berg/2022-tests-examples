package ru.yandex.realty.unifiedoffer.offertype

import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.offer.{RentCondition, Transaction, TransactionCondition}
import ru.yandex.realty.proto.offer.RentConditionType

import scala.collection.JavaConverters._

/**
  * @author nstaroverova
  */
@RunWith(classOf[JUnitRunner])
class RentConditionsProtoConverterSpec extends SpecBase with Matchers {

  "RentConditionsProtoConverter.toMessage " should {
    "return correct transformation " in {
      val transaction = new Transaction
      transaction.setRendCondition(RentCondition.CHILDREN, false)
      transaction.setRendCondition(RentCondition.PETS, null)

      val result = RentConditionsProtoConverter.toMessage(transaction)

      val child = result.getConditionsList.asScala.find(_.getType == RentConditionType.RENT_CONDITION_CHILDREN)
      val pets = result.getConditionsList.asScala.find(_.getType == RentConditionType.RENT_CONDITION_PETS)

      child.exists(_.getAllowed == false) should be(true)
      pets.isEmpty should be(true)
      result.getConditionsCount should be(1)

    }

    "return empty conditions list if conditions is empty " in {
      val result = RentConditionsProtoConverter.toMessage(new Transaction)

      result.getConditionsCount should be(0)
    }

    "return empty conditions list transaction is empty " in {
      val result = RentConditionsProtoConverter.toMessage(null)

      result.getConditionsCount should be(0)
    }
  }

}
