package ru.yandex.vertis.moisha.impl.autoru_auction.v7

import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.yandex.vertis.moisha.impl.autoru_auction.AuctionSpec
import ru.yandex.vertis.moisha.impl.autoru_auction.utils.Mark
import ru.yandex.vertis.moisha.impl.autoru_auction.utils.Mark.{
  AstonMartin,
  Audi,
  BMW,
  Bentley,
  Bugatti,
  Cadillac,
  Ferrari,
  Genesis,
  Infinity,
  Jaguar,
  Jeep,
  Lamborgini,
  LandRover,
  Lexus,
  Maserati,
  Mercedes,
  Mini,
  Porsche,
  RollsRoyce,
  VAZ,
  Volvo
}
import ru.yandex.vertis.moisha.impl.autoru_auction.gens._
import ru.yandex.vertis.moisha.model.gens._
import ru.yandex.vertis.moisha.impl.autoru_auction.model.Products.Call
import ru.yandex.vertis.moisha.model.RegionId
import ru.yandex.vertis.moisha.util.GeoIds._
import ru.yandex.vertis.moisha.model.FundsConversions._

class CallDailyPolicySpec extends AuctionSpec {
  val start = DateTime.parse("2020-05-04T00:00:00.000+03:00")
  val policy = new AutoRuAuctionPolicyV7

  val firstGroup =
    Set(AstonMartin, Bentley, Bugatti, Ferrari, Jaguar, Lamborgini, LandRover, Maserati, Porsche, RollsRoyce)
  def oneOfFirstGroup: Gen[Mark] = Gen.oneOf(firstGroup)

  val secondGroup = Set(Audi, BMW, Cadillac, Infinity, Genesis, Jeep, Lexus, Mini, Mercedes, Volvo)
  def oneOfSecondGroup: Gen[Mark] = Gen.oneOf(secondGroup)

  def oldRegionGen: Gen[RegionId] =
    Gen.oneOf(Set(RegVoronezh, RegTula, RegYaroslavl, RegSverdlovsk, RegChelyabinsk))

  "CallDailyPolicy.v7 calls" should {
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

    "return 0 for not Moscow and SPb regionIds" in {
      checkPrice(req(oldRegionGen, oneOfFirstGroup, hasPriorityPlacement = false), 0.rubles)
    }

    "return 0 for not Moscow and SPb regionIds, doesn't matter if hasPriorityPlacement = true" in {
      checkPrice(req(oldRegionGen, oneOfFirstGroup, hasPriorityPlacement = true), 0.rubles)
    }

    "get price in first group, not priorityPlacement in Moscow" in {
      checkPrice(req(RegMoscow, oneOfFirstGroup, hasPriorityPlacement = false), 1200.rubles)
    }

    "get price in first group, not priorityPlacement in Moscow, doesn't matter if hasPriorityPlacement = true" in {
      checkPrice(req(RegMoscow, oneOfFirstGroup, hasPriorityPlacement = true), 1200.rubles)
    }

    "get price in first group, not priorityPlacement in SPb" in {
      checkPrice(req(RegSPb, oneOfFirstGroup, hasPriorityPlacement = false), 1200.rubles)
    }

    "get price in first group, not priorityPlacement in Spb, doesn't matter if hasPriorityPlacement = true" in {
      checkPrice(req(RegSPb, oneOfFirstGroup, hasPriorityPlacement = true), 1200.rubles)
    }

    "get price in second group, not priorityPlacement in Moscow" in {
      checkPrice(req(RegMoscow, oneOfSecondGroup, hasPriorityPlacement = false), 1200.rubles)
    }

    "get price in second group, not priorityPlacement in Moscow, doesn't matter if hasPriorityPlacement = true" in {
      checkPrice(req(RegMoscow, oneOfSecondGroup, hasPriorityPlacement = true), 1200.rubles)
    }

    "get price in second group, not priorityPlacement in SPb" in {
      checkPrice(req(RegSPb, oneOfSecondGroup, hasPriorityPlacement = false), 1200.rubles)
    }

    "get price in second group, not priorityPlacement in Spb, doesn't matter if hasPriorityPlacement = true" in {
      checkPrice(req(RegSPb, oneOfSecondGroup, hasPriorityPlacement = true), 1200.rubles)
    }

    "get price for VAZ in Moscow" in {
      checkPrice(req(RegMoscow, VAZ, hasPriorityPlacement = false), 700.rubles)
    }

    "get price for VAZ in Moscow, doesn't matter if hasPriorityPlacement = true" in {
      checkPrice(req(RegMoscow, VAZ, hasPriorityPlacement = true), 700.rubles)
    }

    "get price for VAZ in SPb" in {
      checkPrice(req(RegSPb, VAZ, hasPriorityPlacement = false), 700.rubles)
    }

    "get price for VAZ in SPb, doesn't matter if hasPriorityPlacement = true" in {
      checkPrice(req(RegSPb, VAZ, hasPriorityPlacement = true), 700.rubles)
    }

  }
}
