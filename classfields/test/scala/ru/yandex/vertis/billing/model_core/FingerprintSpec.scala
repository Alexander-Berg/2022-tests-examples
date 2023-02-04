package ru.yandex.vertis.billing.model_core

import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.DynamicPrice.Constraints
import ru.yandex.vertis.billing.model_core.Fingerprint._
import ru.yandex.vertis.billing.model_core.FixPrice.unitsToFixPrice
import ru.yandex.vertis.billing.model_core.Good.OfferIdTarget
import ru.yandex.vertis.billing.model_core.gens.{
  randomPrintableString,
  teleponyCallFactHeaderGen,
  OfferIdGen,
  PhoneGen,
  Producer,
  TeleponyCallFactGenCallTypes,
  TeleponyCallFactGenParams
}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

/**
  * Specs on [[WithdrawUtils]]
  *
  * @author alesavin
  */
class FingerprintSpec extends AnyWordSpec with Matchers {

  "Fingerprint.ofProduct" should {
    val products1 = Seq(
      Product(Highlighting(CostPerClick(40L))),
      Product(Highlighting(CostPerClick(40L)))
    )

    val products2 = Seq(
      Product(Highlighting(CostPerClick(40L))),
      Product(Raising(CostPerClick(40L))),
      Product(`Raise+Highlighting`(CostPerClick(40L))),
      Product(`Raise+Highlighting`(CostPerMille(40L)))
    )

    "be consistent" in {
      products1.map(ofProduct).toSet.size should be(1)
      products2.map(ofProduct).toSet.size should be(4)
    }
  }

  "Fingerprint.ofCallFactHeader" should {
    "be correct on MetrikaCallFactHeader" in {
      val phone1 = Gen.some(PhoneGen).next
      val phone2 = PhoneGen.next

      val size = Gen.choose(2, 20).next
      val identities = (1 to size)
        .map(i =>
          for {
            shift <- Gen.choose[Int](1, 20)
            track <- Gen.choose(0, 1000000)
            incoming <- Gen.alphaStr
          } yield MetrikaCallFactHeader(
            DateTime.now().minusDays(shift),
            incoming,
            phone1,
            phone2,
            track
          )
        )
        .map(_.next)
        .map(_.identity)
        .toSet

      identities.size should be(size)
    }

    "be correct on TeleponyCallFactHeader" in {
      val size = Gen.choose(2, 20).next
      val identities = teleponyCallFactHeaderGen().next(size).map(_.identity).toSet
      identities.size should be(size)
    }

    "use call id as identity for redirect call" in {
      val callId = randomPrintableString(5)
      val oldCallHeader = teleponyCallFactHeaderGen(
        TeleponyCallFactGenParams(TeleponyCallFactGenCallTypes.Redirect)
      ).next.copy(
        callId = Some(callId),
        timestamp = Fingerprint.CallFactIdVSBILLING3544DateStart.minusDays(1)
      )

      Fingerprint.ofCallFactHeader(oldCallHeader) should not be callId

      val newCallHeader = teleponyCallFactHeaderGen(
        TeleponyCallFactGenParams(TeleponyCallFactGenCallTypes.Redirect)
      ).next.copy(
        callId = Some(callId),
        timestamp = Fingerprint.CallFactIdVSBILLING3544DateStart.plus(1)
      )

      Fingerprint.ofCallFactHeader(newCallHeader) shouldBe callId
    }

    "use call id as identity for callback call" in {
      val callId = randomPrintableString(5)
      val newCallHeader = teleponyCallFactHeaderGen(
        TeleponyCallFactGenParams(TeleponyCallFactGenCallTypes.Callback)
      ).next.copy(
        callId = Some(callId),
        timestamp = Fingerprint.CallFactIdVSBILLING3544DateStart.plus(1)
      )

      Fingerprint.ofCallFactHeader(newCallHeader) shouldBe callId
    }

  }

  "Fingerprint.ofUser" should {
    "be correct on user" in {
      ofUser(Uid(1000L))
      ofUser(YandexUid("33333ffff"))
      ofUser(Login("samehome"))
      ofUser(AutoRuUid("111"))
      ofUser(EmptyUser)
    }
  }

  "Fingerprint.ofCost" should {
    "be correct on cost" in {
      Set(
        ofCost(CostPerClick(1L)),
        ofCost(CostPerMille(1L)),
        ofCost(CostPerDay(1L)),
        ofCost(CostPerAction),
        ofCost(CostPerCall(1L)),
        ofCost(CostPerOffer(1L))
      ).size should be(6)
    }
    "be correct on cost with dynamic price" in {
      Set(
        ofCost(CostPerClick(FixPrice(1L))),
        ofCost(CostPerClick(DynamicPrice(Some(1L)))),
        ofCost(CostPerClick(DynamicPrice(None, DynamicPrice.Constraints(max = Some(1L)))))
      ).size should be(3)
    }
  }

  "Fingerprint.ofConstraints" should {
    "be corrent" in {
      Set(
        ofConstraints(Constraints()),
        ofConstraints(Constraints(max = Some(1L))),
        ofConstraints(Constraints(min = Some(1L))),
        ofConstraints(Constraints(divider = Some(1L))),
        ofConstraints(Constraints(max = Some(2L), min = Some(1L))),
        ofConstraints(Constraints(max = Some(2L), divider = Some(1L))),
        ofConstraints(Constraints(min = Some(1L), divider = Some(1L))),
        ofConstraints(Constraints(max = Some(2L), min = Some(1L), divider = Some(1L)))
      ).size should be(8)
    }
  }

  "Fingerprint.ofGood" should {
    "be correct" in {
      val good = Highlighting(CostPerClick(10L))
      Set(
        good,
        good.copy(duration = Some(FiniteDuration(1, TimeUnit.MINUTES))),
        good.copy(target = Some(OfferIdTarget(OfferIdGen.next)))
      ).map(ofGood).size should be(3)
    }
  }

}
