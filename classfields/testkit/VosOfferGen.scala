package ru.auto.comeback.model.testkit

import java.time.Instant

import common.zio.testkit.CommonGen.listOfShuffled
import ru.auto.api.api_offer_model.{Category, Offer, OfferStatus, Phone, SellerType, TeleponyInfo}
import ru.auto.api.common_model.MileageInfo
import ru.auto.comeback.model.testkit.CommonGen._
import ru.auto.comeback.model.testkit.OfferGen._
import ru.auto.comeback.model.testkit.UserRefGen._
import zio.random.Random
import zio.test.Gen.{anyFloat, anyInt, anyLong}
import zio.test.{Gen, Sized}

object VosOfferGen {

  def offer[R <: Random with Sized](
      userRef: Gen[R, String] = anyUserRef,
      status: Gen[R, OfferStatus] = anyStatus,
      category: Gen[R, Category] = Gen.const(Category.CARS),
      wasActive: Gen[R, Boolean] = Gen.boolean,
      creationDate: Gen[R, Instant] = anyInstant,
      expireDate: Option[Gen[R, Instant]] = None,
      vin: Gen[R, String] = anyVinCode,
      year: Gen[R, Int] = anyInt,
      mark: Gen[R, String] = anyMark,
      model: Gen[R, String] = anyModel,
      superGenId: Gen[R, Int] = anyInt,
      sellerType: Gen[R, SellerType] = anySellerType,
      phone: Option[Gen[R, String]] = Some(anyPhoneNumber),
      redirectPhone: Option[Gen[R, String]] = Some(anyPhoneNumber),
      price: Gen[R, Float] = anyFloat,
      rid: Gen[R, Int] = anyInt,
      chatOnly: Gen[R, Boolean] = Gen.boolean): Gen[R, Offer] =
    for {
      id <- anyOfferId
      userRef <- userRef
      status <- status
      category <- category
      wasActive <- wasActive
      creationDate <- creationDate
      expireDate <- expireDate.map(gen => gen.map(Some.apply)).getOrElse(Gen.const(None))
      vin <- vin
      year <- year
      mark <- mark
      model <- model
      superGenId <- superGenId
      sellerType <- sellerType
      phone <- phone.map(p => p.map(Some.apply)).getOrElse(Gen.const(None))
      redirectPhone <- redirectPhone.map(p => p.map(Some.apply)).getOrElse(Gen.const(None))
      mileageHistoryItem1 = anyMileageHistoryItem
      mileageHistoryItem2 = anyMileageHistoryItem
      mileageHistory <- listOfShuffled(mileageHistoryItem1, mileageHistoryItem2)
      price <- price
      rid <- rid
      chatOnly <- chatOnly
    } yield {
      val offer = Offer()
        .withId(id)
        .withUserRef(userRef)
        .withStatus(status)
        .withCategory(category)
        .withSellerType(sellerType)
        .update(_.documents.vin := vin)
        .update(_.documents.year := year)
        .update(_.carInfo.mark := mark)
        .update(_.carInfo.model := model)
        .update(_.carInfo.superGenId := superGenId)
        .update(_.additionalInfo.wasActive := wasActive)
        .update(_.additionalInfo.creationDate := creationDate.toEpochMilli)
        .update(_.additionalInfo.expireDate := expireDate.fold(0L)(_.toEpochMilli))
        .update(_.additionalInfo.chatOnly := chatOnly)
        .update(_.mileageHistory := mileageHistory)
        .update(_.priceInfo.price := price)
        .update(_.seller.location.geobaseId := rid)

      val withPhone = phone.fold(offer)(p => offer.update(_.seller.phones.modify(Phone().withPhone(p) +: _)))
      val withRedirectPhone = redirectPhone.fold(withPhone)(p =>
        offer
          .update(_.seller.redirectPhones := true)
          .update(_.seller.teleponyInfo := TeleponyInfo())
          .update(_.seller.phones.modify(Phone().withRedirect(p) +: _))
      )

      withRedirectPhone
    }

  val anyMileageHistoryItem: Gen[Random, MileageInfo] =
    for {
      mileage <- anyInt
      seconds <- anyLong
      nanos <- anyInt
    } yield MileageInfo()
      .withMileage(mileage)
      .update(_.updateTimestamp.modify(_.withSeconds(seconds / 2).withNanos(nanos)))
}
