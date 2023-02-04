package ru.yandex.vos2.watching.stages

import java.util.UUID

import ru.yandex.realty.proto.offer.{CampaignType, Product}
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.realty.model.TestUtils

import scala.collection.JavaConverters._

/**
  * @author Vsevolod Levin
  */
package object products {

  def createProduct(
    productType: CampaignType,
    id: String = UUID.randomUUID().toString,
    duration: Long = 1000,
    start: Long = System.currentTimeMillis(),
    active: Boolean = true
  ): Product = {
    Product
      .newBuilder()
      .setId(id)
      .setType(productType)
      .setStartTime(start)
      .setEndTime(start + duration)
      .setActive(active)
      .build()
  }

  def newOfferWithProducts(products: Seq[Product]): Offer.Builder = {
    val builder = TestUtils.createOffer()
    builder.getOfferRealtyBuilder.addAllProducts(products.asJava)
    builder
  }

}
