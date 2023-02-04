package ru.yandex.vertis.moisha.impl.autoru_auction.v13

import org.joda.time.DateTime
import ru.yandex.vertis.moisha.impl.autoru_auction.AutoRuAuctionPolicy.AutoRuAuctionRequest
import ru.yandex.vertis.moisha.impl.autoru_auction.model.Categories.Cars
import ru.yandex.vertis.moisha.impl.autoru_auction.model.Sections.New
import ru.yandex.vertis.moisha.impl.autoru_auction.model._
import ru.yandex.vertis.moisha.model.DateTimeInterval
import ru.yandex.vertis.moisha.model.FundsConversions._
import ru.yandex.vertis.moisha.test.BaseSpec
import ru.yandex.vertis.ops.test.TestOperationalSupport

class CallDailyPolicyCalculatorSpec extends BaseSpec {

  private def calculateCapitalCallPrice(mark: Option[MarkId], model: Option[ModelId], dealerMarks: List[MarkId]) = {
    val rq = AutoRuAuctionRequest(
      Products.Call,
      AutoRuAuctionOffer(Cars, New, mark, model),
      AutoRuAuctionContext(clientRegionId = 1, clientCityId = None, dealerMarks),
      DateTimeInterval.dayIntervalFrom(DateTime.parse("2020-12-26T00:00+03:00"))
    )
    new CallDailyPolicyCalculator(TestOperationalSupport)(rq).success.value.goods.loneElement.price
  }

  "CallDailyPolicy" should {

    "return volkswagen tiguan call price for volkswagen tiguan offer" in {
      calculateCapitalCallPrice(
        mark = Some("VOLKSWAGEN"),
        model = Some("TIGUAN"),
        dealerMarks = List("VOLKSWAGEN", "AUDI")
      ) shouldBe 2800.rubles
    }

    "return volkswagen default call price for volkswagen offer with model which is not in matrix" in {
      calculateCapitalCallPrice(
        mark = Some("VOLKSWAGEN"),
        model = Some("GOLF"),
        dealerMarks = List("VOLKSWAGEN", "AUDI")
      ) shouldBe 3000.rubles
    }

    "return volkswagen call price when call from dealer's page (hence no offer) and dealer sells only volkswagen" in {
      calculateCapitalCallPrice(
        mark = None,
        model = None,
        dealerMarks = List("VOLKSWAGEN")
      ) shouldBe 3000.rubles
    }

    "return audi call price when call from dealer's page (hence no offer) and dealer sells volkswagen and audi" in {
      calculateCapitalCallPrice(
        mark = None,
        model = None,
        dealerMarks = List("VOLKSWAGEN", "AUDI")
      ) shouldBe 4000.rubles
    }

    "return default price when offer mark is not in matrix" in {
      calculateCapitalCallPrice(
        mark = Some("DAEWOO"),
        model = Some("MATIZ"),
        dealerMarks = List("DAEWOO")
      ) shouldBe 2000.rubles
    }

    "return default price when call from dealer's page (hence no offer) and dealer mark is not in matrix" in {
      calculateCapitalCallPrice(
        mark = None,
        model = None,
        dealerMarks = List("DAEWOO")
      ) shouldBe 2000.rubles
    }
  }
}
