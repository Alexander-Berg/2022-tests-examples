package ru.yandex.realty.context.v2

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.context.{ProductDictionaryFilter, VasProductDictionaryStorage}
import ru.yandex.realty.model.message.ExtDataSchema.ProductDictionaryRecord
import ru.yandex.realty.model.message.ExtDataSchema.ProductDictionaryRecord._

/**
  * @author nstaroverova
  */
@RunWith(classOf[JUnitRunner])
class VasProductDictionaryStorageSpec extends AsyncSpecBase {

  private val dictionary = Set(
    buildProductRecord(TargetType.OFFER, PaymentType.JURIDICAL_PERSON),
    buildProductRecord(TargetType.OFFER, PaymentType.NATURAL_PERSON)
  )

  private def buildProductRecord(target: TargetType, payment: PaymentType): ProductDictionaryRecord = {
    ProductDictionaryRecord
      .newBuilder()
      .setPaymentType(payment)
      .setTarget(target)
      .build()
  }

  "VasProductDictionaryStorage" should {

    "get filter returns all Set if all fields are empty" in {
      val dict = new VasProductDictionaryStorage(dictionary)
      val filter = ProductDictionaryFilter(None, None)

      val result = dict.getByFilter(filter)

      result.size should be(2)
    }

    "get filter returns Set if one of fields is empty" in {
      val dict = new VasProductDictionaryStorage(dictionary)
      val filter = ProductDictionaryFilter(None, Some(PaymentType.JURIDICAL_PERSON))

      val result = dict.getByFilter(filter)

      result.size should be(1)
      result.head.getPaymentType should be(PaymentType.JURIDICAL_PERSON)
    }

    "get filter returns Set if all fields are not empty" in {
      val dict = new VasProductDictionaryStorage(dictionary)
      val filter = ProductDictionaryFilter(Some(TargetType.OFFER), Some(PaymentType.NATURAL_PERSON))

      val result = dict.getByFilter(filter)

      result.size should be(1)
      result.head.getPaymentType should be(PaymentType.NATURAL_PERSON)
      result.head.getTarget should be(TargetType.OFFER)
    }
  }

}
