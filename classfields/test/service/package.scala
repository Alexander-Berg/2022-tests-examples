package ru.vertistraf.cost_plus.builder.auto

import ru.vertistraf.cost_plus.model.{CarouselImage, Review, ServiceOffer}
import zio.URIO
import zio.prelude.NonEmptyList
import zio.random.Random
import zio.stream.ZSink
import zio.test.Gen

package object service {

  private[service] def generateN[R, A](n: Int)(gen: Gen[R, A]): URIO[R, Seq[A]] =
    gen.sample.forever.map(_.value).run(ZSink.take(n))

  private[service] val priceGen =
    Gen.long(100000, 10000000)

  private[service] def offerGen(
      markCode: String,
      modelCode: String,
      id: Option[Long] = None,
      superGenId: Option[Long] = None,
      techParamId: Option[Long] = None,
      configurationId: Option[Long] = None,
      transmissionName: Option[String] = None,
      complectationId: Option[Long] = None,
      complectationName: Option[String] = None,
      enginePower: Option[Int] = None,
      engineDisplacementLiters: Option[Double] = None,
      tableImagesCount: Int = 4,
      dealer: Option[String] = None,
      price: Option[Long] = None,
      relevance: Option[Int] = Some(1)): Gen[Random, ServiceOffer.Auto.Car] =
    for {
      price <- price.fold(priceGen)(Gen.const(_))
      id <- id.fold(Gen.long(1000000, 9 * 1000000).map(_.toString))(Gen.const(_).map(_.toString))
      superGenId <- superGenId.fold(Gen.long(22500000, 55000000))(Gen.const(_))
      techParamId <- techParamId.fold(Gen.long(22500000, 55000000))(Gen.const(_))
      configurationId <- configurationId.fold(Gen.long(22500000, 55000000))(Gen.const(_))
      transmissionName <- transmissionName.fold(Gen.elements("Автомат", "Механическая"))(Gen.const(_))
      complectationId <- complectationId.fold(Gen.long(22500000, 55000000))(Gen.const(_))
      complectationName <- complectationName.fold(Gen.elements("Classic", "Comfort", "Prestige"))(Gen.const(_))
      enginePower <- enginePower.fold(Gen.int(10, 500))(Gen.const(_))
      engineDisplacementLiters <- engineDisplacementLiters.fold(Gen.double(0.5, 5))(Gen.const(_))
      relevance <- relevance.fold(Gen.int(1, 100))(Gen.const(_))
      (mark, model) = (markCode.toLowerCase, modelCode.toLowerCase)
    } yield ServiceOffer.Auto.Car(
      relevance = relevance,
      offerId = id,
      price = price,
      vendor = mark + " vendor",
      offerImage = CarouselImage(s"img.host/offer/$id", s"offer $id title"),
      modelImage = CarouselImage(s"img.host/$mark/$model", model),
      markImage = CarouselImage(s"img.host/$mark", mark),
      dealerImage = dealer.map(d => CarouselImage(s"img.host/$d", s"$d title")),
      tableImages = NonEmptyList.fromIterableOption((1 to tableImagesCount).map(i => s"img.host/$mark/$model/$i")).get,
      modelReview = Review(100, s"review.host/$mark/$model"),
      markReview = Review(200, s"review.host/$mark"),
      markUrlCode = markCode,
      modelUrlCode = modelCode,
      dealerDirectUrl = dealer.map(d => s"dealer.url/$d"),
      superGenId = Some(superGenId),
      techParamId = techParamId,
      configurationId = configurationId,
      transmissionName = transmissionName,
      complectationId = Some(complectationId),
      complectationName = Some(complectationName),
      enginePower = enginePower,
      engineDisplacementLiters = engineDisplacementLiters,
      utmTerm = Some(s"utm-term-${dealer.getOrElse(mark)}")
    )
}
