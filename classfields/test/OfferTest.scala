package ru.auto.comeback.model.test

import ru.auto.comeback.model.Offer
import ru.auto.api.api_offer_model.OfferStatus
import ru.auto.comeback.model.testkit.OfferGen.anyStatus
import ru.auto.comeback.model.testkit.UserRefGen._
import ru.auto.comeback.model.testkit.VosOfferGen
import zio.test.Assertion._
import zio.test._

object OfferTest extends DefaultRunnableSpec {

  def spec =
    suite("Offer")(
      testM("return dealer client id for dealers offer") {
        check(
          VosOfferGen.offer(userRef = Gen.const("dealer:29430"))
        ) { vosOffer =>
          val fromVos = Offer(vosOffer)
          assert(fromVos.clientId)(isSome(equalTo(29430L)))
        }
      },
      testM("return None clientId for private users offer")(
        check(
          VosOfferGen.offer(userRef = privateUserRef)
        ) { vosOffer =>
          val fromVos = Offer(vosOffer)
          assert(fromVos.clientId)(isNone)
        }
      ),
      testM("return None phone for active offer with no phones")(
        check(
          VosOfferGen.offer(phone = None, redirectPhone = None, status = Gen.const(OfferStatus.ACTIVE))
        ) { vosOffer =>
          val fromVos = Offer(vosOffer)
          assert(fromVos.phone)(isNone)
        }
      ),
      testM("return phone for active offer with phones") {
        val phone = "+79991111111"
        check(
          VosOfferGen.offer(
            phone = Some(Gen.const(phone)),
            redirectPhone = None,
            status = Gen.const(OfferStatus.ACTIVE),
            chatOnly = Gen.const(false)
          )
        ) { vosOffer =>
          val fromVos = Offer(vosOffer)
          assert(fromVos.phone)(isSome(equalTo(phone)))
        }
      },
      testM("return None phone for active offer with phones, but chat only") {
        val phone = "+79991111111"
        val redirected = "+79992222222"
        check(
          VosOfferGen.offer(
            phone = Some(Gen.const(phone)),
            redirectPhone = Some(Gen.const(redirected)),
            status = Gen.const(OfferStatus.ACTIVE),
            chatOnly = Gen.const(true)
          )
        ) { vosOffer =>
          val fromVos = Offer(vosOffer)
          assert(fromVos.phone)(isNone)
        }
      },
      testM("return None phone for not active offers with phones") {
        val phone = "+79991111111"
        val redirected = "+79992222222"
        check(
          VosOfferGen.offer(
            phone = Some(Gen.const(phone)),
            redirectPhone = Some(Gen.const(redirected)),
            status = anyStatus.filter(_ != OfferStatus.ACTIVE)
          )
        ) { vosOffer =>
          val fromVos = Offer(vosOffer)
          assert(fromVos.phone)(isNone)
        }
      },
      testM("return redirected phone for active offer with redirect phones") {
        val phone = "+79991111111"
        val redirected = "+79992222222"
        check(
          VosOfferGen.offer(
            phone = Some(Gen.const(phone)),
            redirectPhone = Some(Gen.const(redirected)),
            status = Gen.const(OfferStatus.ACTIVE),
            chatOnly = Gen.const(false)
          )
        ) { vosOffer =>
          val fromVos = Offer(vosOffer)
          assert(fromVos.phone)(isSome(equalTo(redirected)))
        }
      }
    )
}
