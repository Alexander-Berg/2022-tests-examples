package ru.auto.comeback.consumer.test

import ru.auto.api.api_offer_model.{Category, OfferStatus, SellerType}
import ru.auto.api.common_model.BanReason
import ru.auto.comeback.ComebackUpdaterFromOffers
import ru.auto.comeback.model.testkit.OfferGen
import ru.auto.comeback.model.testkit.OfferGen._
import ru.auto.comeback.model.vos.VosOfferChangeEvent
import zio.test.Assertion.{isFalse, isTrue}
import zio.test.Gen.{anyASCIIString, anyInstant}
import zio.test.{assert, check, DefaultRunnableSpec, Gen}

object ComebackOffersUpdaterSpec extends DefaultRunnableSpec {

  def spec =
    suite("ComebackUpdater")(
      testM("process offer if status was changed") {
        check(
          OfferGen.offer(status = anyStatus.filter(_ != OfferStatus.ACTIVE)),
          anyInstant
        ) { (oldOffer, updated) =>
          val newOffer = oldOffer.copy(status = OfferStatus.ACTIVE)
          val event = VosOfferChangeEvent(oldOffer, newOffer, updated)
          val res = ComebackUpdaterFromOffers.shouldProcess(event)
          assert(res)(isTrue)
        }
      },
      testM("process offer if vin was changed")(
        check(
          OfferGen.offer(),
          anyInstant
        ) { (oldOffer, updated) =>
          val newOffer = oldOffer.copy(documents = oldOffer.documents.copy(vin = "Any another vin"))
          val event = VosOfferChangeEvent(oldOffer, newOffer, updated)
          val res = ComebackUpdaterFromOffers.shouldProcess(event)
          assert(res)(isTrue)
        }
      ),
      testM("process offer if category was changed")(
        check(
          OfferGen.offer(category = anyCategory.filter(_ != Category.CARS)),
          anyInstant
        ) { (oldOffer, updated) =>
          val newOffer = oldOffer.copy(category = Category.CARS)
          val event = VosOfferChangeEvent(oldOffer, newOffer, updated)
          val res = ComebackUpdaterFromOffers.shouldProcess(event)
          assert(res)(isTrue)
        }
      ),
      testM("process offer if seller type was changed")(
        check(
          OfferGen.offer(sellerType = anySellerType.filter(_ != SellerType.COMMERCIAL)),
          anyInstant
        ) { (oldOffer, updated) =>
          val newOffer = oldOffer.copy(sellerType = SellerType.COMMERCIAL)
          val event = VosOfferChangeEvent(oldOffer, newOffer, updated)
          val res = ComebackUpdaterFromOffers.shouldProcess(event)
          assert(res)(isTrue)
        }
      ),
      testM("process offer if activated ts was changed")(
        check(
          OfferGen.offer(),
          anyInstant
        ) { (oldOffer, updated) =>
          val newOffer = oldOffer.copy(activated = oldOffer.activated.plusSeconds(1000))
          val event = VosOfferChangeEvent(oldOffer, newOffer, updated)
          val res = ComebackUpdaterFromOffers.shouldProcess(event)
          assert(res)(isTrue)
        }
      ),
      testM("process offer if deactivated ts was changed")(
        check(
          OfferGen.offer(deactivated = None),
          anyInstant,
          anyInstant
        ) { (oldOffer, deactivated, updated) =>
          val newOffer = oldOffer.copy(deactivated = Some(deactivated))
          val event = VosOfferChangeEvent(oldOffer, newOffer, updated)
          val res = ComebackUpdaterFromOffers.shouldProcess(event)
          assert(res)(isTrue)
        }
      ),
      testM("process offer if mark was changed") {
        check(
          OfferGen.offer(mark = Gen.const("")),
          anyMark,
          anyInstant
        ) { (oldOffer, newMark, updated) =>
          val newOffer = oldOffer.copy(carInfo = oldOffer.carInfo.copy(mark = newMark))
          val event = VosOfferChangeEvent(oldOffer, newOffer, updated)
          val res = ComebackUpdaterFromOffers.shouldProcess(event)
          assert(res)(isTrue)
        }
      },
      testM("process offer if model was changed") {
        check(
          OfferGen.offer(model = Gen.const("")),
          anyModel,
          anyInstant
        ) { (oldOffer, newModel, updated) =>
          val newOffer = oldOffer.copy(carInfo = oldOffer.carInfo.copy(model = newModel))
          val event = VosOfferChangeEvent(oldOffer, newOffer, updated)
          val res = ComebackUpdaterFromOffers.shouldProcess(event)
          assert(res)(isTrue)
        }
      },
      testM("process offer if superGenId was changed") {
        check(
          OfferGen.offer(),
          anyInstant
        ) { (oldOffer, updated) =>
          val newOffer = oldOffer.copy(carInfo = oldOffer.carInfo.copy(superGenId = oldOffer.carInfo.superGenId + 3))
          val event = VosOfferChangeEvent(oldOffer, newOffer, updated)
          val res = ComebackUpdaterFromOffers.shouldProcess(event)
          assert(res)(isTrue)
        }
      },
      testM("process offer if manufactured year was changed")(
        check(
          OfferGen.offer(),
          anyInstant
        ) { (oldOffer, updated) =>
          val newOffer = oldOffer.copy(documents = oldOffer.documents.copy(year = oldOffer.documents.year + 1))
          val event = VosOfferChangeEvent(oldOffer, newOffer, updated)
          val res = ComebackUpdaterFromOffers.shouldProcess(event)
          assert(res)(isTrue)
        }
      ),
      testM("process offer if last mileage was changed")(
        check(
          OfferGen.offer(),
          anyInstant
        ) { (oldOffer, updated) =>
          val newOffer = oldOffer.copy(mileage = oldOffer.mileage + 10000)
          val event = VosOfferChangeEvent(oldOffer, newOffer, updated)
          val res = ComebackUpdaterFromOffers.shouldProcess(event)
          assert(res)(isTrue)
        }
      ),
      testM("process offer if price was changed")(
        check(
          OfferGen.offer(),
          anyInstant
        ) { (oldOffer, updated) =>
          val newOffer = oldOffer.copy(price = oldOffer.price + 10000)
          val event = VosOfferChangeEvent(oldOffer, newOffer, updated)
          val res = ComebackUpdaterFromOffers.shouldProcess(event)
          assert(res)(isTrue)
        }
      ),
      testM("process offer if location was changed")(
        check(
          OfferGen.offer(),
          anyInstant
        ) { (oldOffer, updated) =>
          val newOffer = oldOffer.copy(geobaseId = oldOffer.geobaseId + 1)
          val event = VosOfferChangeEvent(oldOffer, newOffer, updated)
          val res = ComebackUpdaterFromOffers.shouldProcess(event)
          assert(res)(isTrue)
        }
      ),
      testM("process offer if wasActive was changed")(
        check(
          OfferGen.offer(wasActive = Gen.const(false)),
          anyInstant
        ) { (oldOffer, updated) =>
          val newOffer = oldOffer.copy(wasActive = true)
          val event = VosOfferChangeEvent(oldOffer, newOffer, updated)
          val res = ComebackUpdaterFromOffers.shouldProcess(event)
          assert(res)(isTrue)
        }
      ),
      testM("process offer if reasonsBan was changed")(
        check(
          OfferGen.offer(),
          anyASCIIString,
          anyInstant
        ) { (oldOffer, banReason, updated) =>
          val newOffer = oldOffer.copy(reasonsBan = Set(banReason))
          val event = VosOfferChangeEvent(oldOffer, newOffer, updated)
          val res = ComebackUpdaterFromOffers.shouldProcess(event)
          assert(res)(isTrue)
        }
      ),
      testM("process offer if humanReasonsBan was changed")(
        check(
          OfferGen.offer(),
          anyInstant
        ) { (oldOffer, updated) =>
          val newOffer = oldOffer.copy(humanReasonsBan = Set(BanReason()))
          val event = VosOfferChangeEvent(oldOffer, newOffer, updated)
          val res = ComebackUpdaterFromOffers.shouldProcess(event)
          assert(res)(isTrue)
        }
      ),
      testM("do not process offer if vin is empty")(
        check(
          OfferGen.offer(),
          OfferGen.offer(vin = Gen.const("")),
          anyInstant
        ) { (oldOffer, newOffer, updated) =>
          val event = VosOfferChangeEvent(oldOffer, newOffer, updated)
          val res = ComebackUpdaterFromOffers.shouldProcess(event)
          assert(res)(isFalse)
        }
      ),
      testM("do not process offer if category is not cars")(
        check(
          OfferGen.offer(),
          OfferGen.offer(category = anyCategory.filter(_ != Category.CARS)),
          anyInstant
        ) { (oldOffer, newOffer, updated) =>
          val event = VosOfferChangeEvent(oldOffer, newOffer, updated)
          val res = ComebackUpdaterFromOffers.shouldProcess(event)
          assert(res)(isFalse)
        }
      )
    )
}
