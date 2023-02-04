package ru.auto.comeback.consumer.test

import java.time.Instant

import cats.syntax.option._
import common.zio.testkit.CommonGen._
import ru.auto.api.api_offer_model.OfferStatus
import ru.auto.comeback.ComebackBuilder._
import ru.auto.comeback.model.Offer
import ru.auto.comeback.model.PastEvents.{PastExternalOffer, PastExternalSale, PastOffer}
import ru.auto.comeback.model.carfax.CarfaxHistoryOffer
import ru.auto.comeback.model.testkit.{CarfaxHistoryOfferGen, OfferGen}
import ru.auto.comeback.model.testkit.OfferGen._
import ru.auto.comeback.model.testkit.PastEventsGen._
import zio.random.Random
import zio.test.Assertion._
import zio.test.Gen.{anyInt, anyLong}
import zio.test._

object ComebackBuilderSpec extends DefaultRunnableSpec {
  def instant(ts: String): Gen[Any, Instant] = Gen.const(Instant.parse(ts))

  val now = instant("2020-01-29T12:00:00Z")
  val past1 = instant("2019-08-23T13:00:00Z")
  val past2 = instant("2019-05-29T12:00:00Z")
  val past3 = instant("2018-05-01T00:00:00Z")
  val past4 = instant("2015-04-19T02:03:00Z")
  val past5 = instant("2014-12-11T10:00:00Z")

  val past = Gen.oneOf(past1, past2, past3, past4, past5)

  val notBannedStatus = anyStatus.filter(_ != OfferStatus.BANNED)

  def genCarfaxHistoryOffer(
      userRef: String,
      maybeClientId: Option[Long],
      activated: Gen[Random with Sized, Instant]): Gen[Random with Sized, CarfaxHistoryOffer] =
    CarfaxHistoryOfferGen.offer(
      userRef = Gen.const(userRef),
      clientId = maybeClientId.map(clientId => Gen.const(clientId)),
      activated = activated
    )

  def spec =
    suite("ComebackBuilder")(
      testM("return None if offer wasn't active yet")(
        check(
          OfferGen.offer(wasActive = Gen.const(false)),
          anyLong,
          anyNonEmptyPastEvents,
          anyInstant
        ) { (offer, clientId, pastEvents, updated) =>
          val res = maybeComeback(offer, clientId, pastEvents, None, updated)
          assert(res)(isNone)
        }
      ),
      testM("return None if offer is banned")(
        check(
          OfferGen.offer(status = Gen.const(OfferStatus.BANNED)),
          anyLong,
          anyNonEmptyPastEvents,
          anyInstant
        ) { (offer, clientId, pastEvents, updated) =>
          val res = maybeComeback(offer, clientId, pastEvents, None, updated)
          assert(res)(isNone)
        }
      ),
      testM("return None if past events is empty and offer was active")(
        check(
          OfferGen.offer(activated = now, wasActive = Gen.const(true), status = notBannedStatus),
          anyLong,
          emptyPastEvents,
          anyInstant
        ) { (offer, clientId, pastEvents, updated) =>
          val res = maybeComeback(offer, clientId, pastEvents, None, updated)
          assert(res)(isNone)
        }
      ),
      testM("return comeback if past events non empty and offer was active")(
        check(
          OfferGen.offer(activated = now, wasActive = Gen.const(true), status = notBannedStatus),
          anyLong,
          anyNonEmptyPastEvents,
          anyInstant
        ) { (offer, clientId, pastEvents, updated) =>
          val res = maybeComeback(offer, clientId, pastEvents, None, updated)
          assert(res)(isSome(anything))
        }
      ),
      testM("return comeback with correct client id if past events non empty and offer was active")(
        check(
          OfferGen.offer(activated = now, wasActive = Gen.const(true), status = notBannedStatus),
          anyLong,
          anyNonEmptyPastEvents,
          anyInstant
        ) { (offer, clientId, pastEvents, updated) =>
          val res = maybeComeback(offer, clientId, pastEvents, None, updated)
          assert(res.map(_.clientId))(isSome(equalTo(clientId)))
        }
      ),
      testM("return passed sellersCountAfterPast if past events non empty and offer was active")(
        check(
          OfferGen.offer(activated = now, wasActive = Gen.const(true), status = notBannedStatus),
          anyLong,
          anyNonEmptyPastEvents,
          anyInt,
          anyInstant
        ) { (offer, clientId, pastEvents, sellersCountAfterPast, updated) =>
          val res = maybeComeback(offer, clientId, pastEvents, Some(sellersCountAfterPast), updated)
          assert(res.flatMap(_.meta.sellersCountAfterPast))(isSome(equalTo(sellersCountAfterPast)))
        }
      ),
      testM("return None if there are no past offer or past external offer or past external sale")(
        check(
          listOfShuffled(
            genCarfaxHistoryOffer(userRef = "dealer:3", maybeClientId = 3L.some, activated = past2),
            genCarfaxHistoryOffer(userRef = "user:2", maybeClientId = none, activated = past1),
            genCarfaxHistoryOffer(userRef = "user:3", maybeClientId = none, activated = now)
          )
        ) { history =>
          val res = sellersCountAfter(1, None, None, None, history)
          assert(res)(isNone)
        }
      ),
      testM("return sellersCountAfter = 1 if there is 1 seller after past offer")(
        check(
          genCarfaxHistoryOffer(userRef = "dealer:1", maybeClientId = 1L.some, activated = past),
          listOfShuffled(
            genCarfaxHistoryOffer(userRef = "user:2", maybeClientId = none, activated = now)
          )
        ) { (pastOffer, history) =>
          val res = sellersCountAfter(1, None, PastOffer(pastOffer).some, None, history :+ pastOffer)
          assert(res)(isSome(equalTo(1)))
        }
      ),
      testM("return sellersCountAfter = 2 if there are 2 sellers after past offer")(
        check(
          genCarfaxHistoryOffer(userRef = "dealer:1", maybeClientId = 1L.some, activated = past2),
          listOfShuffled(
            genCarfaxHistoryOffer(userRef = "user:2", maybeClientId = none, activated = past1),
            genCarfaxHistoryOffer(userRef = "user:3", maybeClientId = none, activated = now)
          )
        ) { (pastOffer, history) =>
          val res = sellersCountAfter(1, None, Some(PastOffer(pastOffer)), None, history :+ pastOffer)
          assert(res)(isSome(equalTo(2)))
        }
      ),
      testM("return sellersCountAfter = 2 if there are 2 sellers after past offer and 1 seller before past offer")(
        check(
          genCarfaxHistoryOffer(userRef = "dealer:1", maybeClientId = 1L.some, activated = past2),
          listOfShuffled(
            genCarfaxHistoryOffer(userRef = "dealer:3", maybeClientId = 3L.some, activated = past3),
            genCarfaxHistoryOffer(userRef = "user:2", maybeClientId = none, activated = past1),
            genCarfaxHistoryOffer(userRef = "user:3", maybeClientId = none, activated = now)
          )
        ) { (pastOffer, history) =>
          val res = sellersCountAfter(1, None, Some(PastOffer(pastOffer)), None, history :+ pastOffer)
          assert(res)(isSome(equalTo(2)))
        }
      ),
      testM("return sellersCountAfter = 1 if owner sold car twice and there is 1 seller after him")(
        check(
          genCarfaxHistoryOffer(userRef = "dealer:3", maybeClientId = 3L.some, activated = past2),
          listOfShuffled(
            genCarfaxHistoryOffer(userRef = "dealer:3", maybeClientId = 3L.some, activated = past3),
            genCarfaxHistoryOffer(userRef = "user:2", maybeClientId = none, activated = now)
          )
        ) { (pastOffer, history) =>
          val res = sellersCountAfter(3, None, Some(PastOffer(pastOffer)), None, history :+ pastOffer)
          assert(res)(isSome(equalTo(1)))
        }
      ),
      testM(
        "return sellersCountAfter = 2 if owner sold car twice and there is 1 seller between his offers and two sellers after him"
      )(
        check(
          genCarfaxHistoryOffer(userRef = "dealer:4", maybeClientId = 4L.some, activated = past2),
          listOfShuffled(
            genCarfaxHistoryOffer(userRef = "dealer:4", maybeClientId = 4L.some, activated = past4),
            genCarfaxHistoryOffer(userRef = "user:555", maybeClientId = none, activated = past3),
            genCarfaxHistoryOffer(userRef = "user:666", maybeClientId = none, activated = past1),
            genCarfaxHistoryOffer(userRef = "user:5", maybeClientId = none, activated = now)
          )
        ) { (pastOffer, history) =>
          val res = sellersCountAfter(4, None, Some(PastOffer(pastOffer)), None, history :+ pastOffer)
          assert(res)(isSome(equalTo(2)))
        }
      ),
      testM("return sellers count = 1 if there is 1 seller before last external sale and is 1 seller after")(
        check(
          externalSale(past1),
          listOfShuffled(
            genCarfaxHistoryOffer(userRef = "user:2", maybeClientId = none, activated = past2),
            genCarfaxHistoryOffer(userRef = "user:5", maybeClientId = none, activated = now)
          )
        ) { (lastExternalSale, history) =>
          val res = sellersCountAfter(1, Some(PastExternalSale(lastExternalSale)), None, None, history)
          assert(res)(isSome(equalTo(1)))
        }
      ),
      testM(
        "return sellers count = 2 if there are 2 sellers after last external sale and exteral sale is after last offer"
      )(
        check(
          externalSale(past2),
          genCarfaxHistoryOffer(userRef = "dealer:1", maybeClientId = 1L.some, activated = past3),
          listOfShuffled(
            genCarfaxHistoryOffer(userRef = "user:3", maybeClientId = none, activated = past1),
            genCarfaxHistoryOffer(userRef = "user:5", maybeClientId = none, activated = past4),
            genCarfaxHistoryOffer(userRef = "user:2", maybeClientId = none, activated = now)
          )
        ) { (lastSale, lastOffer, history) =>
          val res =
            sellersCountAfter(
              1,
              Some(PastExternalSale(lastSale)),
              Some(PastOffer(lastOffer)),
              None,
              history :+ lastOffer
            )
          assert(res)(isSome(equalTo(2)))
        }
      ),
      testM("return sellers count = 1 if there is 1 seller before last external offer and is 1 seller after")(
        check(
          externalOffer(past1),
          listOfShuffled(
            genCarfaxHistoryOffer(userRef = "user:2", maybeClientId = none, activated = past2),
            genCarfaxHistoryOffer(userRef = "user:5", maybeClientId = none, activated = now)
          )
        ) { (lastExternalOffer, history) =>
          val res = sellersCountAfter(1, None, None, Some(PastExternalOffer(lastExternalOffer)), history)
          assert(res)(isSome(equalTo(1)))
        }
      ),
      testM(
        "return sellers count = 2 if there are 2 sellers after last external offer and exteral offer is after last offer"
      )(
        check(
          externalOffer(past2),
          genCarfaxHistoryOffer(userRef = "dealer:1", maybeClientId = 1L.some, activated = past3),
          listOfShuffled(
            genCarfaxHistoryOffer(userRef = "user:3", maybeClientId = none, activated = past1),
            genCarfaxHistoryOffer(userRef = "user:5", maybeClientId = none, activated = past4),
            genCarfaxHistoryOffer(userRef = "user:2", maybeClientId = none, activated = now)
          )
        ) { (lastExternalOffer, lastOffer, history) =>
          val res =
            sellersCountAfter(
              1,
              None,
              Some(PastOffer(lastOffer)),
              Some(PastExternalOffer(lastExternalOffer)),
              history :+ lastOffer
            )
          assert(res)(isSome(equalTo(2)))
        }
      )
    )
}
