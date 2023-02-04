package ru.auto.comeback.model.test

import ru.auto.comeback.model.testkit.OfferRecordGen
import ru.auto.api.api_offer_model.Category
import ru.auto.comeback.model.carfax.CarfaxHistoryOffer
import zio.test.Assertion._
import zio.test._

object CarfaxHistoryOfferTest extends DefaultRunnableSpec {

  def spec =
    suite("CarfaxHistoryOffer")(
      testM("set category = CARS if carfax offers category is unknown")(
        check(
          OfferRecordGen.offer(category = Gen.const(Category.CATEGORY_UNKNOWN))
        ) { carfaxOffer =>
          val offer = CarfaxHistoryOffer(carfaxOffer)
          assert(offer.category)(equalTo(Category.CARS))
        }
      )
    )
}
