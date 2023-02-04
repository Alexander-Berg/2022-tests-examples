package ru.auto.salesman.service.impl.push

import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel
import ru.auto.salesman.client.VosClient
import ru.auto.salesman.client.pushnoy.PushnoyClient
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Placement
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains, Slave}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.OfferModelGenerators
import ru.auto.salesman.test.model.gens.user.UserModelGenerators
import ru.auto.salesman.util.AutomatedContext

class ProlongationFailedPushServiceImplSpec
    extends BaseSpec
    with UserModelGenerators
    with OfferModelGenerators {

  val pushnoyClient = mock[PushnoyClient]
  val pushTextCreator = mock[PushTextCreator]
  val vosClient = mock[VosClient]

  val prolongationFailedPushService = new ProlongationFailedPushServiceImpl(
    pushnoyClient,
    pushTextCreator,
    vosClient
  )

  implicit val rc = AutomatedContext("test")

  "ProlongationFailedPushService" should {
    "send prolongation failed push" in {
      forAll(
        PaidOfferProductNotPlacementGen,
        Gen.alphaNumStr,
        OfferCategoryGen
      ) { (paidOfferProduct, pushText, category) =>
        val product = paidOfferProduct.bindedProduct.product
        val offerIdOpt = paidOfferProduct.bindedProduct.optOfferId

        if (offerIdOpt.nonEmpty) {
          val offer = ApiOfferModel.Offer.newBuilder
            .setId(offerIdOpt.get.toString)
            .setCategory(category)
            .build

          (vosClient.getOffer _)
            .expects(offerIdOpt.get, Slave)
            .returningZ(offer)
        }

        (pushTextCreator.prolongationFailedText _)
          .expects(product, Some(category))
          .returningZ(pushText)

        (pushnoyClient.pushToUser _).expects(*, *).returningZ(1)

        prolongationFailedPushService
          .prolongationFailedPush(paidOfferProduct)
          .success
      }
    }

    "not send prolongation failed push if text creation failed" in {
      forAll(PaidOfferProductNotPlacementGen, OfferCategoryGen) {
        (paidOfferProduct, category) =>
          val product = paidOfferProduct.bindedProduct.product
          val offerIdOpt = paidOfferProduct.bindedProduct.optOfferId

          if (offerIdOpt.nonEmpty) {
            val offer = ApiOfferModel.Offer.newBuilder
              .setId(offerIdOpt.get.toString)
              .setCategory(category)
              .build

            (vosClient.getOffer _)
              .expects(offerIdOpt.get, Slave)
              .returningZ(offer)
          }

          val expectedException = new Exception("test")
          (pushTextCreator.prolongationFailedText _)
            .expects(product, Some(category))
            .throwingZ(expectedException)

          (pushnoyClient.pushToUser _).expects(*, *).never

          prolongationFailedPushService
            .prolongationFailedPush(paidOfferProduct)
            .failure
            .exception shouldBe expectedException
      }
    }

    "send prolongation placement failed push" in {
      forAll(goodsGen(Gen.const(Placement)), ActiveOfferGen, Gen.alphaNumStr) {
        (paidOfferProduct, offer, pushText) =>
          val offerId = paidOfferProduct.bindedProduct.optOfferId.get
          (vosClient.getOffer _)
            .expects(offerId, Slave)
            .returningZ(offer)

          (pushTextCreator
            .prolongationPlacementFailedText(_: ApiOfferModel.Offer))
            .expects(offer)
            .returningZ(pushText)

          (pushnoyClient.pushToUser _).expects(*, *).returningZ(1)

          prolongationFailedPushService
            .prolongationFailedPush(paidOfferProduct)
            .success
      }
    }
  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
