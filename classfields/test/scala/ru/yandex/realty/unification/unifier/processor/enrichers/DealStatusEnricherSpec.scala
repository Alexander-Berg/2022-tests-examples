package ru.yandex.realty.unification.unifier.processor.enrichers

import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.model.offer._
import ru.yandex.realty.tracing.Traced

@RunWith(classOf[JUnitRunner])
class DealStatusEnricherSpec extends AsyncSpecBase with Matchers {

  implicit val trace: Traced = Traced.empty

  private val enricher = new DealStatusEnricher

  private def testEnricher(oldDealType: DealType, newDealStatus: DealStatus) {
    val transaction = new Transaction
    transaction.setDealType(oldDealType)

    val offer = new Offer
    offer.setTransaction(transaction)

    enricher.enrich(offer).futureValue
    transaction.getDealStatus shouldEqual newDealStatus
  }

  "DealStatusEnricher" should {
    "convert Direct deal type to Sale deal status" in {
      testEnricher(DealType.DIRECT, DealStatus.SALE)
    }

    "convert Alternative deal type to Countersale deal status" in {
      testEnricher(DealType.ALTERNATIVE, DealStatus.COUNTERSALE)
    }
  }
}
