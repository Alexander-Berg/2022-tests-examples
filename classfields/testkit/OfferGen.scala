package ru.auto.comeback.model.testkit

import java.time.Instant

import ru.auto.api.api_offer_model.{Category, OfferStatus, SellerType}
import ru.auto.comeback.model.Comeback.OfferRef
import ru.auto.comeback.model.testkit.CommonGen.{anyInstant, anySection}
import ru.auto.comeback.model.testkit.UserRefGen.anyUserRef
import ru.auto.comeback.model.{CarInfo, Documents, Offer}
import zio.random.Random
import zio.test.Gen.{anyASCIIString, anyInt, anyLong}
import zio.test.{Gen, Sized}

object OfferGen {

  private val hexChar = Gen.weighted(Gen.char('0', '9') -> 10, Gen.char('a', 'f') -> 6)

  val anyOfferId: Gen[Random with Sized, String] = for {
    id <- Gen.long(1, Long.MaxValue)
    hash <- Gen.weighted(Gen.const("") -> 1, Gen.stringN(8)(hexChar) -> 100)
  } yield hash match {
    case "" => id.toString
    case _ => s"$id-$hash"
  }

  val anyOfferRef: Gen[Random with Sized, OfferRef] =
    CommonGen.anyCategory.zip(anyOfferId).map(OfferRef.tupled)

  val anySellerType: Gen[Random, SellerType] = Gen.fromIterable(SellerType.values)

  val anyCategory: Gen[Random, Category] = Gen.fromIterable(Category.values)

  val anyStatus: Gen[Random, OfferStatus] =
    Gen.fromIterable(OfferStatus.values)

  val anyMark: Gen[Random with Sized, String] = Gen.alphaNumericString.map(_.take(6)).filter(_.nonEmpty)

  val anyModel: Gen[Random with Sized, String] = Gen.alphaNumericString.map(_.take(8)).filter(_.nonEmpty)

  val anyPhoneNumber: Gen[Random, String] =
    Gen.long(70000000000L, 79999999999L).map(number => s"+$number")

  def anyCarInfo[R <: Random with Sized]: Gen[R, CarInfo] =
    for {
      mark <- anyASCIIString
      model <- anyASCIIString
      superGenId <- anyInt
    } yield CarInfo(mark, model, superGenId)

  def anyDocuments[R <: Random with Sized]: Gen[R, Documents] =
    for {
      vin <- anyASCIIString
      year <- anyInt
    } yield Documents(vin, year)

  def offer[R <: Random with Sized](
      userRef: Gen[R, String] = anyUserRef,
      clientId: Option[Gen[R, Long]] = Some(anyLong),
      category: Gen[R, Category] = Gen.const(Category.CARS),
      status: Gen[R, OfferStatus] = anyStatus,
      sellerType: Gen[R, SellerType] = anySellerType,
      activated: Gen[R, Instant] = anyInstant,
      deactivated: Option[Gen[R, Instant]] = Some(anyInstant),
      wasActive: Gen[R, Boolean] = Gen.boolean,
      mark: Gen[R, String] = anyMark,
      model: Gen[R, String] = anyModel,
      superGenId: Gen[R, Long] = anyLong,
      vin: Gen[R, String] = CommonGen.anyVinCode,
      year: Gen[R, Int] = anyInt,
      phone: Option[Gen[R, String]] = Some(anyPhoneNumber),
      price: Gen[R, Int] = anyInt,
      mileage: Gen[R, Int] = anyInt): Gen[R, Offer] =
    for {
      id <- anyOfferId
      userRef <- userRef
      clientId <- clientId.map(gen => gen.map(Some.apply)).getOrElse(Gen.const(None))
      category <- category
      section <- anySection
      status <- status
      sellerType <- sellerType
      activated <- activated
      deactivated <- deactivated.map(gen => gen.map(Some.apply)).getOrElse(Gen.const(None))
      wasActive <- wasActive
      mark <- mark
      model <- model
      superGenId <- superGenId
      vin <- vin
      year <- year
      phone <- phone.map(p => p.map(Some.apply)).getOrElse(Gen.const(None))
      mileage <- mileage
      price <- price
      geobaseId <- anyLong
    } yield Offer(
      id,
      userRef,
      clientId,
      category,
      section,
      status,
      sellerType,
      activated,
      deactivated,
      wasActive,
      CarInfo(mark, model, superGenId),
      Documents(vin, year),
      mileage,
      price,
      phone,
      geobaseId,
      Set.empty,
      Set.empty
    )
}
