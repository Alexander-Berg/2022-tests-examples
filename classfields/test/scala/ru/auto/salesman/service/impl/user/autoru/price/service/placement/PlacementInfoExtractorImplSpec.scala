package ru.auto.salesman.service.impl.user.autoru.price.service.placement

import ru.auto.salesman.dao.user.GoodsDao
import ru.auto.salesman.dao.user.GoodsDao.Filter.ForActiveProductUserOffer
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Placement
import ru.auto.salesman.model.{AutoruUser, DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.ServiceModelGenerators

class PlacementInfoExtractorImplSpec extends BaseSpec with ServiceModelGenerators {
  val goodsDao: GoodsDao = mock[GoodsDao]
  val placementInfoExtractor = new PlacementInfoExtractorImpl(goodsDao)

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  "alreadyPaid" should {

    "fail if there is no user" in {
      forAll(EnrichedOfferGen) { enrichedOfferGenerated =>
        val enrichedOffer = enrichedOfferGenerated.copy(user = None)
        placementInfoExtractor.alreadyPaidPlacement(enrichedOffer).failure
      }
    }

    "return false if no goods in goodsDao response" in {
      forAll(EnrichedOfferGen) { enrichedOfferGenerated =>
        val user = AutoruUser(123)
        val enrichedOffer = enrichedOfferGenerated.copy(user = Some(user))

        (goodsDao.get _)
          .expects(
            ForActiveProductUserOffer(
              Placement,
              "user:123",
              enrichedOffer.offerId
            )
          )
          .returningZ(Nil)

        placementInfoExtractor
          .alreadyPaidPlacement(enrichedOffer)
          .success
          .value shouldBe false
      }
    }

    "return true if there is goods in goodsDao response" in {
      forAll(EnrichedOfferGen, GoodsGen) { (enrichedOfferGenerated, goods) =>
        val user = AutoruUser(123)
        val enrichedOffer = enrichedOfferGenerated.copy(user = Some(user))

        (goodsDao.get _)
          .expects(
            ForActiveProductUserOffer(
              Placement,
              "user:123",
              enrichedOffer.offerId
            )
          )
          .returningZ(Iterable(goods))

        placementInfoExtractor
          .alreadyPaidPlacement(enrichedOffer)
          .success
          .value shouldBe true
      }
    }
  }

}
