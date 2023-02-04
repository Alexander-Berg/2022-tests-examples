package ru.yandex.vertis.moisha.impl.autoru_auction.v10

import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.yandex.vertis.moisha.impl.autoru_auction.AuctionSpec
import ru.yandex.vertis.moisha.impl.autoru_auction.gens._
import ru.yandex.vertis.moisha.impl.autoru_auction.model.Products.Call
import ru.yandex.vertis.moisha.impl.autoru_auction.utils.Mark
import ru.yandex.vertis.moisha.impl.autoru_auction.utils.Mark._
import ru.yandex.vertis.moisha.model.FundsConversions._
import ru.yandex.vertis.moisha.model.RegionId
import ru.yandex.vertis.moisha.model.gens._
import ru.yandex.vertis.moisha.util.GeoIds._

class CallDailyPolicySpec extends AuctionSpec {
  val start = DateTime.parse("2020-06-11T00:00:00.000+03:00")
  val policy = new AutoRuAuctionPolicyV10

  val premiumMarks_Group1 = Set(
    AstonMartin,
    Bentley,
    Bugatti,
    Ferrari,
    Jaguar,
    Lamborgini,
    LandRover,
    Maserati,
    Porsche,
    RollsRoyce
  )

  def oneOfFirstGroup: Gen[Mark] = Gen.oneOf(premiumMarks_Group1)

  val premiumMarks_Group2 = Set(
    Audi,
    BMW,
    Cadillac,
    Infinity,
    Genesis,
    Jeep,
    Lexus,
    Mini,
    Mercedes,
    Volvo
  )

  def oneOfFirstGroup2: Gen[Mark] = Gen.oneOf(premiumMarks_Group2)

  val secondGroup = Set(Chrysler, Dodge, Mazda, Mitsubishi, Skoda, Subaru, Suzuki, Toyota, Volkswagen, VAZ)
  def oneOfSecondGroup: Gen[Mark] = Gen.oneOf(secondGroup)

  def oldRegionGen: Gen[RegionId] =
    Gen.oneOf(Set(RegVoronezh, RegTula, RegYaroslavl, RegSverdlovsk, RegChelyabinsk))

  "CallDailyPolicy.v10 calls" should {

    // Wrong interval

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

    // Other regions

    "return 0 for not Moscow and SPb regionIds" in {
      checkPrice(req(oldRegionGen, oneOfFirstGroup, hasPriorityPlacement = false), 0.rubles)
    }

    "return 0 for not Moscow and SPb regionIds, doesn't matter if hasPriorityPlacement = true" in {
      checkPrice(req(oldRegionGen, oneOfFirstGroup, hasPriorityPlacement = true), 0.rubles)
    }

    // First group

    "get price in first group, not priorityPlacement in Moscow" in {
      checkPrice(req(RegMoscow, oneOfFirstGroup, hasPriorityPlacement = false), 2000.rubles)
    }

    "get price in first group, not priorityPlacement in Moscow, doesn't matter if hasPriorityPlacement = true" in {
      checkPrice(req(RegMoscow, oneOfFirstGroup, hasPriorityPlacement = true), 2000.rubles)
    }

    "get price in first group, not priorityPlacement in SPb" in {
      checkPrice(req(RegSPb, oneOfFirstGroup, hasPriorityPlacement = false), 4000.rubles)
    }

    "get price in first group, not priorityPlacement in Spb, doesn't matter if hasPriorityPlacement = true" in {
      checkPrice(req(RegSPb, oneOfFirstGroup, hasPriorityPlacement = true), 4000.rubles)
    }

    "get price in first group2, not priorityPlacement in SPb" in {
      checkPrice(req(RegSPb, oneOfFirstGroup2, hasPriorityPlacement = false), 3000.rubles)
    }

    "get price in first group2, not priorityPlacement in Spb, doesn't matter if hasPriorityPlacement = true" in {
      checkPrice(req(RegSPb, oneOfFirstGroup2, hasPriorityPlacement = true), 3000.rubles)
    }

    // Second group

    "get price for secondGroup in Moscow" in {
      checkPrice(req(RegMoscow, oneOfSecondGroup, hasPriorityPlacement = false), 1500.rubles)
    }

    "get price for secondGroup in Moscow, doesn't matter if hasPriorityPlacement = true" in {
      checkPrice(req(RegMoscow, oneOfSecondGroup, hasPriorityPlacement = true), 1500.rubles)
    }

    "get price for secondGroup in SPb" in {
      checkPrice(req(RegSPb, oneOfSecondGroup, hasPriorityPlacement = false), 1500.rubles)
    }

    "get price for secondGroup in SPb, doesn't matter if hasPriorityPlacement = true" in {
      checkPrice(req(RegSPb, oneOfSecondGroup, hasPriorityPlacement = true), 1500.rubles)
    }

  }
}
