package ru.yandex.vos2.model

import com.google.protobuf.Duration
import org.scalacheck.Gen
import ru.auto.salesman.model.user.ApiModel.PriceModifier.Feature
import ru.auto.salesman.model.user.ApiModel.{Price, PriceModifier, ProductPrice}
import ru.yandex.vos2.model.CommonGen._

object SalesmanModelGenerator {

  val featureGen: Gen[Feature] = for {
    id <- Gen.alphaStr.suchThat(_.nonEmpty)
    count <- Gen.posNum[Int]
    timestamp <- protobufTimestampGen
  } yield {
    Feature.newBuilder
      .setId(id)
      .setCount(count)
      .setDeadline(timestamp)
      .build
  }

  val modifierGen: Gen[PriceModifier] = for {
    feature <- featureGen
  } yield {
    PriceModifier.newBuilder.setPromocoderFeature(feature).build
  }

  val priceWithFeature: Gen[Price] = for {
    basePrice <- Gen.posNum[Long]
    effectivePrice <- Gen.posNum[Long]
    modifier <- modifierGen
  } yield Price.newBuilder
    .setBasePrice(basePrice)
    .setEffectivePrice(effectivePrice)
    .setModifier(modifier)
    .build

  val priceWithoutFeature: Gen[Price] = for {
    basePrice <- Gen.posNum[Long]
    effectivePrice <- Gen.posNum[Long]
  } yield Price.newBuilder
    .setBasePrice(basePrice)
    .setEffectivePrice(effectivePrice)
    .build

  def productPriceGen(
      priceGen: Gen[Price],
      durationGen: Gen[Duration] = protobufDurationGen
  ): Gen[ProductPrice] =
    for {
      price <- priceGen
      duration <- durationGen
    } yield ProductPrice.newBuilder
      .setPrice(price)
      .setDuration(duration)
      .build
}
