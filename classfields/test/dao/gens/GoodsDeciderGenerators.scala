package ru.auto.salesman.test.dao.gens

import org.scalacheck.Gen
import ru.auto.salesman.model._
import ru.auto.salesman.model.ProductId.Badge
import ru.auto.salesman.service.GoodsDecider.Action.Activate
import ru.auto.salesman.service.GoodsDecider._
import ru.auto.salesman.test.model.gens.BillingModelGenerators._
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.test.model.gens.OfferModelGenerators.offerGen
import ru.auto.salesman.util.PromocoderUtils._
import ru.yandex.vertis.generators.DateTimeGenerators.dateTime

object GoodsDeciderGenerators {

  private def contextGen: Gen[Context] = GoodsNameGen.flatMap {
    case Badge => Gen.posNum[GoodsId].map(BadgeContext)
    case product => ProductContext(product)
  }

  def requestGen: Gen[Request] =
    for {
      clientId <- ClientIdGen
      offer <- offerGen()
      context <- contextGen
      firstActivateDate <- dateTime().map(FirstActivateDate.apply)
      offerBillingDeadline <- Gen.option(dateTime())
      customPrice <- Gen.option(Gen.posNum[Funds])
    } yield
      Request(
        clientId,
        offer,
        context,
        firstActivateDate,
        offerBillingDeadline,
        customPrice
      )

  def activateGen(
      featureInstanceGen: Gen[Option[FeatureInstance]] =
        Gen.option(PromocoderModelGenerators.featureInstanceGen)
  ): Gen[Activate] =
    for {
      activateDate <- activateDateGen
      offerBilling <- offerBillingGen
      featureInstance <- featureInstanceGen
      price <- Gen.posNum[Long]
    } yield
      Activate(
        activateDate,
        offerBilling,
        featureInstance
          .map(f =>
            List(
              PriceModifierFeature(
                f,
                f.createCountForPaying(1L, price),
                discountAmount = price
              )
            )
          )
          .getOrElse(List.empty)
      )
}
