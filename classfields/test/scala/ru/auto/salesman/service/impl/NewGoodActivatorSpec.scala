package ru.auto.salesman.service.impl

import org.joda.time.DateTime
import ru.auto.api.ApiOfferModel.Category.MOTO
import ru.auto.salesman.dao.GoodsDao
import ru.auto.salesman.dao.GoodsDao.AppliedSource
import ru.auto.salesman.model.GoodStatuses.Active
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.service.GoodsDecider.Action.Activate
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.BillingModelGenerators._
import ru.auto.salesman.test.model.gens.OfferModelGenerators.offerGen
import ru.auto.salesman.test.model.gens.{activateDateGen, ProductIdGen}
import ru.yandex.vertis.generators.BasicGenerators.readableString

class NewGoodActivatorSpec extends BaseSpec {

  private val goodsDao = mock[GoodsDao]

  "New good activator" should {

    "make good active" in {
      val nonHashedId = 1082490862
      val hash = "7d1492"
      val offerId = AutoruOfferId(nonHashedId, Some(hash))
      val clientId = 88
      val deadlineDt = DateTime.parse("2018-12-20T15:05:00+03:00")
      val deadlineMillis = 1545307500000L
      forAll(
        offerGen(offerId, MOTO, userRefGen = s"dealer:88"),
        ProductIdGen,
        activateDateGen,
        readableString,
        offerBillingGen
      ) { (offer, product, now, holdId, baseOfferBilling) =>
        val offerBillingBuilder = baseOfferBilling.toBuilder
        offerBillingBuilder.getKnownCampaignBuilder
          .setActiveDeadline(deadlineMillis)
          .setHold(holdId)
        val offerBilling = offerBillingBuilder.build()
        val action = Activate(
          activateDate = now,
          offerBilling,
          features = List.empty
        )
        (goodsDao.insertApplied _)
          .expects {
            argAssert { source: AppliedSource =>
              source.offerId shouldBe nonHashedId
              source.product shouldBe product
              source.status shouldBe Active
              source.activateDate shouldBe now
              source.expireDate.getMillis shouldBe deadlineDt.getMillis
              source.offerBilling shouldBe offerBilling.toByteArray
              source.offerBillingDeadline.getMillis shouldBe deadlineDt.getMillis
              source.holdId shouldBe holdId
              source.details.offerHash.value shouldBe hash
              source.details.category.protoParent shouldBe MOTO
              source.details.section shouldBe offer.getSection
              source.details.clientId shouldBe clientId
              ()
            }
          }
          .returningT(())
        val activator = new NewGoodActivator(goodsDao, offer, product)
        activator.makeGoodActive(action, now).success.value shouldBe (())
      }
    }
  }
}
