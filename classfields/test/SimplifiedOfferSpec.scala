package auto.dealers.trade_in_notifier.model.test

import ru.auto.api.api_offer_model._
import ru.auto.api.cars_model._
import ru.auto.api.common_model._
import auto.dealers.trade_in_notifier.model.SimplifiedOffer
import auto.dealers.trade_in_notifier.model.SimplifiedOffer._
import common.geobase.model.RegionIds.RegionId
import org.apache.commons.codec.digest.DigestUtils.md5Hex
import io.circe.syntax._
import io.circe.parser._
import zio.test._
import zio.test.Assertion._

object SimplifiedOfferSpec extends DefaultRunnableSpec {

  val seller = Seller(
    name = "Bob",
    phones = List(Phone("79111234567")),
    location = Some(Location(geobaseId = 0))
  )

  val carInfo = CarInfo(
    mark = Some("BMW"),
    model = Some("X5"),
    bodyType = Some("ALLROAD_5_DOORS"),
    engineType = Some("DIESEL"),
    transmission = Some("AUTOMATIC"),
    drive = Some("ALL"),
    superGenId = Some(42L),
    configurationId = Some(43L),
    techParamId = Some(44L),
    complectationId = Some(45L)
  )

  val offer = Offer(
    id = "0123-4567",
    seller = Some(seller),
    category = Category.CARS,
    categoryInfo = Offer.CategoryInfo.CarInfo(carInfo),
    colorHex = "0x5a5a5a",
    documents = Some(Documents(year = 1998, ownersNumber = 1)),
    state = Some(State(mileage = 50000)),
    tradeInInfo = Some(
      TradeInInfo(
        tradeInType = TradeInType.FOR_NEW,
        tradeInPriceRange = Some(TradeInInfo.PriceRange(from = 300000, to = 330000, currency = "RUB"))
      )
    )
  )

  val simpleCarInfo = SimplifiedCarInfo(
    mark = "BMW",
    model = "X5",
    body = "ALLROAD_5_DOORS",
    engine = "DIESEL",
    transmission = "AUTOMATIC",
    drive = "ALL",
    superGenId = Some(42L),
    configurationId = Some(43L),
    techParamId = Some(44L),
    complectationId = Some(45L)
  )

  val simpleOffer = SimplifiedOffer(
    name = "Bob",
    phones = List("79111234567"),
    location = RegionId(0),
    year = 1998,
    mileage = 50000,
    info = simpleCarInfo,
    color = "0x5a5a5a",
    ownersNumber = 1,
    tradeInType = "FOR_NEW",
    priceRange = PriceRange(BigDecimal(300000), BigDecimal(330000)),
    currency = "RUB",
    offerId = Some("0123-4567")
  )

  val simpleOfferJson = """
    {
    "name": "Bob",
    "phones": [
      "79111234567"
    ],
    "location": 0,
    "year": 1998,
    "mileage": 50000,
    "info": {
      "SimplifiedCarInfo": {
        "mark": "BMW",
        "model": "X5",
        "body": "ALLROAD_5_DOORS",
        "engine": "DIESEL",
        "transmission": "AUTOMATIC",
        "drive": "ALL",
        "superGenId": 42,
        "configurationId": 43,
        "techParamId": 44,
        "complectationId": 45
      }
    },
    "color": "0x5a5a5a",
    "ownersNumber": 1,
    "tradeInType": "FOR_NEW",
    "priceRange": { "from": 300000, "to": 330000 },
    "currency": "RUB",
    "offerId": "0123-4567"
  }"""

  val converterTest =
    test("Offer->SimplifiedOffer conversion") {
      assert(SimplifiedOffer.fromOffer(offer))(isRight(equalTo(simpleOffer)))
    }

  val converterFailureTest =
    test("Offer->SimplifiedOffer conversion: missing fields") {
      val badOffer = offer.copy(state = None)
      assert(SimplifiedOffer.fromOffer(badOffer))(isLeft(equalTo(MissingField("state"))))
    }

  val simpleOfferToJsonTest =
    test("SimplifiedOffer->Json encoding") {
      assert(simpleOffer.asJson)(equalTo(parse(simpleOfferJson).toOption.get))
    }

  val simpleOfferFromJsonTest =
    test("Json->SimplifiedOffer encoding") {
      assert(parse(simpleOfferJson).flatMap(_.as[SimplifiedOffer]))(isRight(equalTo(simpleOffer)))
    }

  val hashTest =
    test("SimplifiedOffer hash") {
      assert(simpleOffer.simplifiedOfferHash)(equalTo(md5Hex(simpleOffer.offerId.get)))
    }

  val hashMissingOfferIdTest =
    test("SimplifiedOffer hash when offerId is not provided") {
      val so = simpleOffer.copy(offerId = None)
      val expected = md5Hex(
        List(
          simpleOffer.name,
          simpleOffer.phones.mkString(","),
          simpleCarInfo.mark,
          simpleCarInfo.model,
          simpleOffer.tradeInType
        ).mkString(",")
      )
      assert(so.simplifiedOfferHash)(equalTo(expected))
    }

  override val spec =
    suite("SimplifiedOfferSpec")(
      converterTest,
      converterFailureTest,
      simpleOfferToJsonTest,
      simpleOfferFromJsonTest,
      hashTest,
      hashMissingOfferIdTest
    )

}
