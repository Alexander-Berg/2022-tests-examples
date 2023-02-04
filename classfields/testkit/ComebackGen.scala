package ru.auto.comeback.model.testkit

import ru.auto.comeback.model.Comeback._
import ru.auto.comeback.model.Status._
import ru.auto.comeback.model.testkit.CommonGen.anyInstant
import ru.auto.comeback.model.testkit.OfferGen.anyPhoneNumber
import ru.auto.comeback.model.{Comeback, PastEvents, Status, Vin}
import zio.random.Random
import zio.test.{Gen, Sample, Sized}

import java.time.Instant

/** Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 21/01/2020
  */
object ComebackGen {

  val anyMeta: Gen[Random, Meta] =
    for {
      comebackAfter <- CommonGen.anyFiniteDuration
      sellersCountPast <- Gen.option(Gen.int(0, 1000000))
      lastEventType <- CommonGen.anyEventType
      lastEventDate <- CommonGen.anyInstant
    } yield Meta(comebackAfter, sellersCountPast, lastEventType, lastEventDate)

  val anyStatus: Gen[Random, Status] =
    Gen.elements(Active, Inactive)

  val anyCar: Gen[Random with Sized, CarInfo] =
    for {
      vin <- CommonGen.anyVinCode
      mark <- Gen.stringN(32)(Gen.alphaNumericChar)
      model <- Gen.stringN(32)(Gen.alphaNumericChar)
      year <- Gen.int(1900, 3000)
      superGen <- Gen.long(0, Int.MaxValue - 1)
    } yield CarInfo(vin, mark, model, year, superGen)

  val anyOffer: Gen[Random with Sized, OfferInfo] =
    for {
      ref <- OfferGen.anyOfferRef
      section <- CommonGen.anySection
      sellerType <- OfferGen.anySellerType
      created <- CommonGen.anyInstant
      deactivated <- Gen.option(CommonGen.anyInstant)
      car <- anyCar
      phone <- anyPhoneNumber
      status <- OfferGen.anyStatus
      mileage <- Gen.int(0, 1000000)
      priceRub <- Gen.int(0, Int.MaxValue - 1) // -1 is a workaround for
      rid <- Gen.int(1, Int.MaxValue - 1) // https://github.com/zio/zio/pull/2489
    } yield OfferInfo(
      ref,
      section,
      sellerType,
      created,
      deactivated,
      car,
      Some(phone),
      status,
      mileage,
      priceRub,
      rid
    )

  def comeback[R <: Random with Sized](
      id: Gen[R, Long] = Gen.anyLong,
      clientId: Gen[R, Int] = Gen.anyInt,
      status: Gen[R, Status] = anyStatus,
      meta: Gen[R, Meta] = anyMeta,
      offer: Gen[R, OfferInfo] = anyOffer,
      pastEventsGen: Gen[R, PastEvents] = PastEventsGen.anyNonEmptyPastEvents,
      updated: Gen[R, Instant] = anyInstant): Gen[R, Comeback] = {
    for {
      id <- id
      clientId <- clientId
      status <- status
      meta <- meta
      offer <- offer
      pastEvents <- pastEventsGen
      updated <- updated
    } yield Comeback(id, clientId, status, meta, offer, pastEvents, updated)
  }

  val anyComeback: Gen[Random with Sized, Comeback] = comeback()

  def newComeback[R <: Random with Sized](clientId: Gen[R, Int] = Gen.anyInt): Gen[R, NewComeback] =
    comeback(clientId = clientId).map(_.withoutId)

  val anyNewComeback: Gen[Random with Sized, NewComeback] = newComeback()

  val newComebacksFromOneClient: Gen[Random with Sized, (Int, List[NewComeback])] =
    (for {
      clientId <- Gen.anyInt
      listing <- Gen.medium(Gen.listOfN(_)(newComeback(clientId = Gen.const(clientId))), 5)
    } yield clientId -> listing)
      .reshrink(Sample.noShrink) // todo shrink failing list

  val existingComebacksFromOneClient: Gen[Random with Sized, (Int, List[Comeback])] =
    (for {
      clientId <- Gen.int(0, Int.MaxValue)
      listing <- Gen.medium(Gen.listOfN(_)(comeback(clientId = Gen.const(clientId))), 5)
    } yield clientId -> listing)
      .reshrink(Sample.noShrink) // todo shrink failing list
}
