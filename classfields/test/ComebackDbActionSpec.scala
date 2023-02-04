package ru.auto.comeback.consumer.test

import ru.auto.comeback.ComebackDbAction._
import ru.auto.comeback.consumer.test.ComebackDbActionSpecOps._
import ru.auto.comeback.model.Comeback._
import ru.auto.comeback.model.ComebackT
import ru.auto.comeback.model.testkit.ComebackGen
import ru.auto.comeback.model.testkit.CommonGen.anyInstant
import zio.test.Assertion._
import zio.test._

import scala.language.implicitConversions

object ComebackDbActionSpec extends DefaultRunnableSpec {

  def spec =
    suite("ComebackDbAction")(
      testM("do nothing if no comebacks given") {
        check(anyInstant) { updated =>
          val actions = comebackDbActions(existing = Nil, wanted = Nil, updated)
          assert(actions)(isEmpty)
        }
      },
      testM("insert any comeback if no comebacks existed") {
        check(ComebackGen.anyNewComeback) { comeback =>
          val actions = comebackDbActions(existing = Nil, wanted = List(comeback), comeback.updated)
          assert(actions)(hasSameElements(List(insert(comeback))))
        }
      },
      testM("delete any comeback if no comebacks should stay") {
        check(ComebackGen.anyComeback) { comeback =>
          val actions = comebackDbActions(existing = List(comeback), wanted = Nil, comeback.updated)
          assert(actions)(hasSameElements(List(delete(comeback))))
        }
      },
      testM("do nothing if comeback shouldn't changed") {
        check(ComebackGen.anyComeback) { comeback =>
          val actions = comebackDbActions(existing = List(comeback), wanted = List(comeback.asNew), comeback.updated)
          assert(actions)(isEmpty)
        }
      },
      testM("update comeback if offer has changed") {
        check(ComebackGen.anyComeback) { existing =>
          val wanted = existing.withDifferentOfferPrice
          val actions = comebackDbActions(List(existing), List(wanted.asNew), existing.updated)
          assert(actions)(hasSameElements(List(update(existing.withDifferentOfferPrice))))
        }
      },
      testM("insert exactly one comeback if there is more than one offer to create comeback from") {
        check(ComebackGen.anyComeback) { base =>
          val wanted1 =
            base.asNew.copy(pastEvents =
              base.pastEvents.copy(
                pastExternalOffer = None,
                pastExternalSale = None,
                pastMaintenance = None,
                pastEstimate = None,
                pastInsuranceSale = None,
                pastCarfaxReportPurchase = None
              )
            )
          val wanted2 = wanted1.withIncrementedPastOfferCreated
          val actions = comebackDbActions(existing = Nil, wanted = List(wanted1, wanted2), base.updated)
          assert(actions)(hasSameElements(List(insert(wanted2))))
        }
      },
      testM("delete existing comeback and insert another if they are created from different offers") {
        check(ComebackGen.anyComeback) { existing =>
          val base = existing.withIncrementedOfferRefCreatedFrom
          val wanted = base.asNew
          val actions = comebackDbActions(List(existing), List(wanted), existing.updated)
          assert(actions)(hasSameElements(List(delete(existing), insert(base.asNew))))
        }
      },
      testM("update proper comebacks if there are two of them") {
        checkM(ComebackGen.anyComeback) { existing1 =>
          val existing2 = existing1.withIncrementedOfferRefCreatedFrom
          val expected1 = existing1.withDifferentOfferPrice
          val expected2 = existing2.withDifferentOfferPrice
          val wanted1 = expected1.asNew
          val wanted2 = expected2.asNew
          for {
            existing <- zio.random.shuffle(List(existing1, existing2))
            wanted <- zio.random.shuffle(List(wanted1, wanted2))
          } yield {
            val actions = comebackDbActions(existing, wanted, existing1.updated)
            assert(actions)(hasSameElements(List(update(expected1), update(expected2))))
          }
        }
      },
      testM("do not update existing comeback if it was updated after new wanted comeback was updated") {
        check(ComebackGen.anyComeback.filter(_.updated.toEpochMilli > 1000000)) { existing =>
          val wanted = existing.copy(updated = existing.updated.minusMillis(1000))
          val actions = comebackDbActions(List(existing), List(wanted.asNew), wanted.updated)
          assert(actions)(isEmpty)
        }
      },
      testM("do not delete existing comeback if received events timestamp before existing comebacks date updated") {
        check(ComebackGen.anyComeback.filter(_.updated.toEpochMilli > 1000000)) { existing =>
          val updated = existing.updated.minusMillis(1000)
          val actions = comebackDbActions(List(existing), Nil, updated)
          assert(actions)(isEmpty)
        }
      }
    )
}

object ComebackDbActionSpecOps {

  implicit class TestOfferRefOps(private val offerRef: OfferRef) extends AnyVal {

    def withIncrementedId: OfferRef =
      offerRef.copy(id = offerRef.id + "1")
  }

  implicit class TestComebackOps[Id](private val comeback: ComebackT[Id]) extends AnyVal {

    private def withUpdatedOffer(f: OfferInfo => OfferInfo): ComebackT[Id] =
      comeback.copy(offer = f(comeback.offer))

    def withIncrementedOfferRefCreatedFrom: ComebackT[Id] =
      withUpdatedOffer(offer => offer.copy(ref = offer.ref.withIncrementedId))

    def withDifferentOfferPrice: ComebackT[Id] =
      withUpdatedOffer(_.copy(priceRub = comeback.offer.priceRub + 1000))

    def withIncrementedPastOfferCreated: ComebackT[Id] = {
      val newPastEvents = comeback.pastEvents.copy(
        pastOffer =
          comeback.pastEvents.pastOffer.map(pastOffer => pastOffer.copy(activated = pastOffer.activated.plusSeconds(1)))
      )

      comeback.copy(pastEvents = newPastEvents)
    }

    def asNew: NewComeback =
      comeback.withoutId
  }

}
