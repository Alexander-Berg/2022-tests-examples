package ru.yandex.vertis.moisha.impl.autoru_auction.v6

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moisha.impl.autoru_auction.AuctionSpec
import ru.yandex.vertis.moisha.impl.autoru_auction.gens._
import ru.yandex.vertis.moisha.impl.autoru_auction.model.Products.Call
import ru.yandex.vertis.moisha.impl.autoru_auction.utils.Mark
import ru.yandex.vertis.moisha.impl.autoru_auction.utils.Mark._
import ru.yandex.vertis.moisha.model.RegionId
import ru.yandex.vertis.moisha.util.GeoIds._
import ru.yandex.vertis.moisha.model.gens._
import ru.yandex.vertis.moisha.model.FundsConversions._

@RunWith(classOf[JUnitRunner])
class CallDailyPolicySpec extends AuctionSpec {

  override val start = DateTime.parse("2020-04-01T00:00:00.000+03:00")

  override val policy = new AutoRuAuctionPolicyV6

  val firstGroup =
    Set(AstonMartin, Bentley, Bugatti, Ferrari, Jaguar, Lamborgini, LandRover, Maserati, Porsche, RollsRoyce)
  def oneOfFirstGroup: Gen[Mark] = Gen.oneOf(firstGroup)

  val secondGroup = Set(Audi, BMW, Cadillac, Infinity, Genesis, Jeep, Lexus, Mini, Mercedes, Volvo)
  def oneOfSecondGroup: Gen[Mark] = Gen.oneOf(secondGroup)

  val thirdGroup = Set(Chrysler, Dodge, Mazda, Mitsubishi, Skoda, Subaru, Suzuki, Toyota, Volkswagen)
  def oneOfThirdGroup: Gen[Mark] = Gen.oneOf(thirdGroup)

  "CallDailyPolicy.v6 calls" should {
    "don't get price if not in interval" in {
      forAll(
        autoRuAuctionRequestGen(
          product = Call,
          interval = dateTimeIntervalEndsBeforeGen(start)
        )
      ) { request =>
        val res = policy.estimate(request).success.value
        res.points.headOption shouldBe None
      }
    }

    def oldRegionGen: Gen[RegionId] =
      Gen.oneOf(Set(RegVoronezh, RegTula, RegYaroslavl, RegSverdlovsk, RegChelyabinsk))
    "return 0 for not Moscow and SPb regionIds" in {
      checkPrice(req(oldRegionGen, oneOfFirstGroup, hasPriorityPlacement = false), 0.rubles)
    }

    "get price in first group, not priorityPlacement in Moscow" in {
      checkPrice(req(RegMoscow, oneOfFirstGroup, hasPriorityPlacement = false), 700.rubles)
    }

    "get price in second group, not priorityPlacement in Moscow" in {
      checkPrice(req(RegMoscow, oneOfSecondGroup, hasPriorityPlacement = false), 700.rubles)
    }

    "get price in third group, not priorityPlacement in Moscow" in {
      checkPrice(req(RegMoscow, oneOfThirdGroup, hasPriorityPlacement = false), 700.rubles)
    }

    "get price for BMW in Moscow " in {
      checkPrice(req(RegMoscow, BMW, hasPriorityPlacement = false), 700.rubles)
    }

    "get price for priorityPlacement BMW in Moscow " in {
      checkPrice(req(RegMoscow, BMW, hasPriorityPlacement = true), 700.rubles)
    }

    "get price for priorityPlacement Infinity in Moscow " in {
      checkPrice(req(RegMoscow, Infinity, hasPriorityPlacement = true), 700.rubles)
    }

    "get price for Mercedes in Moscow " in {
      checkPrice(req(RegMoscow, Mercedes, hasPriorityPlacement = false), 700.rubles)
    }

    "get price for priorityPlacement Mercedes in Moscow " in {
      checkPrice(req(RegMoscow, Mercedes, hasPriorityPlacement = true), 700.rubles)
    }

    "get price for VAZ in Moscow" in {
      checkPrice(req(RegMoscow, VAZ, hasPriorityPlacement = false), 700.rubles)
    }

    "get price for priorityPlacement VAZ in Moscow " in {
      checkPrice(req(RegMoscow, VAZ, hasPriorityPlacement = true), 700.rubles)
    }

    "get price in first group, not priorityPlacement in SPb" in {
      checkPrice(req(RegSPb, oneOfFirstGroup, hasPriorityPlacement = false), 700.rubles)
    }

    "get price in second group, not priorityPlacement in SPb" in {
      checkPrice(req(RegSPb, oneOfSecondGroup, hasPriorityPlacement = false), 700.rubles)
    }

    "get price in third group, not priorityPlacement in SPb" in {
      checkPrice(req(RegSPb, oneOfThirdGroup, hasPriorityPlacement = false), 700.rubles)
    }

    "get price for BMW in SPb " in {
      checkPrice(req(RegSPb, BMW, hasPriorityPlacement = false), 700.rubles)
    }

    "get price for priorityPlacement BMW in SPb " in {
      checkPrice(req(RegSPb, BMW, hasPriorityPlacement = true), 700.rubles)
    }

    "get price for priorityPlacement Infinity in SPb " in {
      checkPrice(req(RegSPb, Infinity, hasPriorityPlacement = true), 700.rubles)
    }

    "get price for Mercedes in SPb " in {
      checkPrice(req(RegSPb, Mercedes, hasPriorityPlacement = false), 700.rubles)
    }

    "get price for priorityPlacement Mercedes in SPb " in {
      checkPrice(req(RegSPb, Mercedes, hasPriorityPlacement = true), 700.rubles)
    }

    "get price for VAZ in SPb" in {
      checkPrice(req(RegSPb, VAZ, hasPriorityPlacement = false), 700.rubles)
    }

    "get price for priorityPlacement VAZ in SPb " in {
      checkPrice(req(RegSPb, VAZ, hasPriorityPlacement = true), 700.rubles)
    }

  }
}
