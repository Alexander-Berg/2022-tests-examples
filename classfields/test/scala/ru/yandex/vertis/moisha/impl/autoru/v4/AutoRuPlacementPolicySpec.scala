package ru.yandex.vertis.moisha.impl.autoru.v4

import ru.yandex.vertis.moisha.impl.autoru.AutoRuPolicy.AutoRuRequest
import ru.yandex.vertis.moisha.impl.autoru.SingleProductDailyPolicySpec
import ru.yandex.vertis.moisha.impl.autoru.gens._
import ru.yandex.vertis.moisha.impl.autoru.model._
import ru.yandex.vertis.moisha.impl.autoru.utils._
import ru.yandex.vertis.moisha.model.FundsConversions._
import ru.yandex.vertis.moisha.model._
import ru.yandex.vertis.moisha.model.gens.Producer

/**
  * Specs on AutoRu policy for [[Products.Placement]]
  */
trait AutoRuPlacementPolicySpec extends SingleProductDailyPolicySpec {

  import AutoRuPlacementPolicySpec._

  def product: Products.Value = Products.Placement

  "Placement policy" should {
    "support Products.Placement" in {
      policy.products.contains(Products.Placement.toString) should be(true)
    }

    "return correct price for Moscow in first day" in {
      checkPolicy(
        correctInterval,
        placement(250.rubles),
        priceIn(0L, Long.MaxValue),
        todaysOffer,
        inMoscow,
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )
    }

    "return correct price for SPb in first day" in {
      checkPolicy(
        correctInterval,
        placement(200.rubles),
        priceIn(0L, Long.MaxValue),
        todaysOffer,
        inSPb,
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )
    }

    "return correct price for Krasnodar in first day" in {
      checkPolicy(
        correctInterval,
        placement(50.rubles),
        priceIn(0L, Long.MaxValue),
        todaysOffer,
        inRegion(RegKrasnodar),
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )
    }

    "return correct price for Voronezh in first day" in {
      checkPolicy(
        correctInterval,
        placement(50.rubles),
        priceIn(0L, Long.MaxValue),
        todaysOffer,
        inRegion(RegVoronezh),
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )
    }

    "return correct price for NNov in first day" in {
      checkPolicy(
        correctInterval,
        placement(50.rubles),
        priceIn(0L, Long.MaxValue),
        todaysOffer,
        inRegion(RegNNovgorod),
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )
    }

    "return correct price for Ryazan in first day" in {
      checkPolicy(
        correctInterval,
        placement(50.rubles),
        priceIn(0L, Long.MaxValue),
        todaysOffer,
        inRegion(RegRyazan),
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )
    }

    "return correct prices for Moscow in other days" in {
      checkPolicy(
        correctInterval,
        placement(10.rubles),
        priceIn(0L, 300.thousands),
        oldOffer,
        inMoscow,
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )

      checkPolicy(
        correctInterval,
        placement(20.rubles),
        priceIn(300.thousands, 500.thousands),
        oldOffer,
        inMoscow,
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )

      checkPolicy(
        correctInterval,
        placement(40.rubles),
        priceIn(500.thousands, 1500.thousands),
        oldOffer,
        inMoscow,
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )

      checkPolicy(
        correctInterval,
        placement(60.rubles),
        priceIn(1500.thousands, Long.MaxValue),
        oldOffer,
        inMoscow,
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )
    }

    "return correct prices for SPb in other days" in {
      checkPolicy(
        correctInterval,
        placement(10.rubles),
        priceIn(0L, 300.thousands),
        oldOffer,
        inSPb,
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )

      checkPolicy(
        correctInterval,
        placement(15.rubles),
        priceIn(300.thousands, 500.thousands),
        oldOffer,
        inSPb,
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )

      checkPolicy(
        correctInterval,
        placement(20.rubles),
        priceIn(500.thousands, 1500.thousands),
        oldOffer,
        inSPb,
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )

      checkPolicy(
        correctInterval,
        placement(30.rubles),
        priceIn(1500.thousands, Long.MaxValue),
        oldOffer,
        inSPb,
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )
    }

    "return correct prices for Krasnodar in other days" in {
      checkPolicy(
        correctInterval,
        placement(1.rubles),
        priceIn(0L, 300.thousands),
        oldOffer,
        inRegion(RegKrasnodar),
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )

      checkPolicy(
        correctInterval,
        placement(2.rubles),
        priceIn(300.thousands, 500.thousands),
        oldOffer,
        inRegion(RegKrasnodar),
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )

      checkPolicy(
        correctInterval,
        placement(3.rubles),
        priceIn(500.thousands, 1500.thousands),
        oldOffer,
        inRegion(RegKrasnodar),
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )

      checkPolicy(
        correctInterval,
        placement(4.rubles),
        priceIn(1500.thousands, Long.MaxValue),
        oldOffer,
        inRegion(RegKrasnodar),
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )
    }

    "return correct prices for Voronezh in other days" in {
      checkPolicy(
        correctInterval,
        placement(1.rubles),
        priceIn(0L, 300.thousands),
        oldOffer,
        inRegion(RegVoronezh),
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )

      checkPolicy(
        correctInterval,
        placement(2.rubles),
        priceIn(300.thousands, 500.thousands),
        oldOffer,
        inRegion(RegVoronezh),
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )

      checkPolicy(
        correctInterval,
        placement(3.rubles),
        priceIn(500.thousands, 1500.thousands),
        oldOffer,
        inRegion(RegVoronezh),
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )

      checkPolicy(
        correctInterval,
        placement(4.rubles),
        priceIn(1500.thousands, Long.MaxValue),
        oldOffer,
        inRegion(RegVoronezh),
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )
    }

    "return correct prices for NNov in other days" in {
      checkPolicy(
        correctInterval,
        placement(1.rubles),
        priceIn(0L, 300.thousands),
        oldOffer,
        inRegion(RegNNovgorod),
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )

      checkPolicy(
        correctInterval,
        placement(2.rubles),
        priceIn(300.thousands, 500.thousands),
        oldOffer,
        inRegion(RegNNovgorod),
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )

      checkPolicy(
        correctInterval,
        placement(3.rubles),
        priceIn(500.thousands, 1500.thousands),
        oldOffer,
        inRegion(RegNNovgorod),
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )

      checkPolicy(
        correctInterval,
        placement(4.rubles),
        priceIn(1500.thousands, Long.MaxValue),
        oldOffer,
        inRegion(RegNNovgorod),
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )
    }

    "return correct prices for Ryazan in other days" in {
      checkPolicy(
        correctInterval,
        placement(1.rubles),
        priceIn(0L, 300.thousands),
        oldOffer,
        inRegion(RegRyazan),
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )

      checkPolicy(
        correctInterval,
        placement(2.rubles),
        priceIn(300.thousands, 500.thousands),
        oldOffer,
        inRegion(RegRyazan),
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )

      checkPolicy(
        correctInterval,
        placement(3.rubles),
        priceIn(500.thousands, 1500.thousands),
        oldOffer,
        inRegion(RegRyazan),
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )

      checkPolicy(
        correctInterval,
        placement(4.rubles),
        priceIn(1500.thousands, Long.MaxValue),
        oldOffer,
        inRegion(RegRyazan),
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )
    }

    "fail if other transport" in {
      checkPolicyFailure(
        correctInterval,
        Products.Placement,
        priceIn(0L, Long.MaxValue),
        oldOffer,
        inMoscow,
        withTransport(Transports.Moto),
        withCategory(Categories.Used)
      )
    }

    "fail if cars new" in {
      checkPolicyFailure(
        correctInterval,
        Products.Placement,
        priceIn(0L, Long.MaxValue),
        oldOffer,
        inMoscow,
        withTransport(Transports.Cars),
        withCategory(Categories.New)
      )
    }

    "fail if region is undefined" in {
      checkPolicyFailure(
        correctInterval,
        Products.Placement,
        priceIn(0L, Long.MaxValue),
        inRegion(0),
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )
    }

    "return empty points if interval is incorrect" in {
      checkPolicyEmpty(
        incorrectInterval,
        Products.Placement,
        priceIn(0L, Long.MaxValue),
        inMoscow,
        withTransport(Transports.Cars),
        withCategory(Categories.Used)
      )
    }
  }
}

object AutoRuPlacementPolicySpec {

  val TestIterations = 50

  def placement(price: Funds): AutoRuProduct =
    AutoRuProduct(
      Products.Placement,
      Set(
        AutoRuGood(Goods.Custom, Costs.PerIndexing, price)
      ),
      duration = DefaultDuration
    )

  import ru.yandex.vertis.moisha.environment._

  def oneDayPlacement: AutoRuRequest =
    RequestGen.next.copy(product = Products.Placement, interval = wholeDay(now()))

}
