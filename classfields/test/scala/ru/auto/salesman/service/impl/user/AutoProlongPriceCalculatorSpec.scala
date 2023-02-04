package ru.auto.salesman.service.impl.user

import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.salesman.client.VosClient
import ru.auto.salesman.model.ProductDuration.{days, SevenDays}
import ru.auto.salesman.model.user.{
  Analytics,
  ExperimentInfo,
  Goods,
  PriceModifier,
  UserQuotaRemoved,
  UserQuotaRestored
}
import ru.auto.salesman.model.{
  AutoruUser,
  DeprecatedDomain,
  DeprecatedDomains,
  ExperimentId,
  Funds,
  Slave,
  UserSellerType
}
import ru.auto.salesman.service.user.PriceReducer
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.ServiceModelGenerators
import ru.auto.salesman.model.user.product.AutoruProduct
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Placement
import ru.auto.salesman.service.PassportService
import ru.auto.salesman.service.impl.user.AutoProlongPriceCalculator.NoBaseProlongPrice
import ru.yandex.vertis.moderation.proto.Model.Domain.{UsersAutoru => ModerationCategory}

class AutoProlongPriceCalculatorSpec extends BaseSpec with ServiceModelGenerators {
  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  private val priceReducer = mock[PriceReducer]
  private val vosClient = mock[VosClient]

  private val autoProlongPriceCalculator = {
    val passportService = stub[PassportService]
    (passportService
      .userType(_: AutoruUser, _: Option[ModerationCategory]))
      .when(*, *)
      .returningZ(UserSellerType.Reseller)

    new AutoProlongPriceCalculator(
      priceReducer,
      vosClient,
      passportService
    )
  }

  private def mockReducePrice = toMockFunction6 {
    priceReducer.reducePrice(
      _: Funds,
      _: AutoruProduct,
      _: AutoruUser,
      _: Option[Offer],
      _: Option[ExperimentInfo],
      _: Option[ExperimentId]
    )
  }

  private def calculatePrice(good: Goods) =
    autoProlongPriceCalculator.calculate(good).success.value

  "AutoProlongPriceCalculator" should {

    "fail if can't parse user" in {
      forAll(GoodsGen, Gen.const("badUserId")) { (goodGenerated, badUserId) =>
        val goods = goodGenerated.copy(user = badUserId)

        autoProlongPriceCalculator
          .calculate(goods)
          .failure
          .exception shouldBe an[IllegalArgumentException]
      }
    }

    "calculate price for proper product and return same product" in {
      forAll(goodsGen(), patchedPriceGen(), offerGen()) { (good, price, offer) =>
        (vosClient.getOffer _).expects(good.offer, Slave).returningZ(offer)
        mockReducePrice
          .expects(*, good.product, *, *, *, *)
          .returningZ(price)
        val result = calculatePrice(good)
        result.product shouldBe good.product
      }
    }

    "calculate price for proper user and offer id" in {
      forAll(goodsGen(), "user:33108624", patchedPriceGen(), offerGen()) {
        (base, user, price, offer) =>
          val good = base.copy(user = user)
          (vosClient.getOffer _).expects(good.offer, Slave).returningZ(offer)
          mockReducePrice
            // real assertion is here: check user and offer id
            .expects(*, *, AutoruUser(33108624), Some(offer), *, *)
            .returningZ(price)
          calculatePrice(good) // should succeed
      }
    }

    "return duration from previous product activation" in {
      forAll(goodsGen(), patchedPriceGen(), offerGen()) { (good, price, offer) =>
        (vosClient.getOffer _).expects(good.offer, Slave).returningZ(offer)
        mockReducePrice.expects(*, *, *, *, *, *).returningZ(price)
        val result = calculatePrice(good)
        result.duration shouldBe good.context.productPrice.duration
      }
    }

    "return paymentReason from previous product activation" in {
      forAll(goodsGen(), patchedPriceGen(), offerGen()) { (good, price, offer) =>
        (vosClient.getOffer _).expects(good.offer, Slave).returningZ(offer)
        mockReducePrice.expects(*, *, *, *, *, *).returningZ(price)
        val result = calculatePrice(good)
        result.paymentReason shouldBe good.context.productPrice.paymentReason
      }
    }

    "return prolongation flags from previous product activation" in {
      forAll(goodsGen(), patchedPriceGen(), offerGen()) { (good, price, offer) =>
        (vosClient.getOffer _).expects(good.offer, Slave).returningZ(offer)
        mockReducePrice.expects(*, *, *, *, *, *).returningZ(price)
        val result = calculatePrice(good)
        result.prolongationAllowed shouldBe good.context.productPrice.prolongationAllowed
        result.prolongationForced shouldBe good.context.productPrice.prolongationForced
        result.prolongationForcedNotTogglable shouldBe good.context.productPrice.prolongationForcedNotTogglable
      }
    }

    "return modifier given by reducePrice()" in {
      forAll(goodsGen(), patchedPriceGen(), offerGen()) { (good, price, offer) =>
        (vosClient.getOffer _).expects(good.offer, Slave).returningZ(offer)
        mockReducePrice.expects(*, *, *, *, *, *).returningZ(price)
        val result = calculatePrice(good)
        result.price.modifier shouldBe price.modifier
      }
    }

    "keep userQuotaRemoved price modifier" in {
      forAll(goodsGen(), patchedPriceGen(), offerGen()) { (genGoods, price, offer) =>
        val goods = genGoods.copy(
          context = genGoods.context.copy(
            productPrice = genGoods.context.productPrice.copy(
              price = genGoods.context.productPrice.price.copy(
                modifier = Some {
                  PriceModifier(
                    feature = None,
                    userQuotaChanged = Some {
                      UserQuotaRemoved(originalProlongPrice = 500L)
                    },
                    appliedExperimentId = None
                  )
                }
              )
            )
          )
        )

        (vosClient.getOffer _).expects(goods.offer, Slave).returningZ(offer)
        mockReducePrice.expects(*, *, *, *, *, *).returningZ(price)

        val result = calculatePrice(goods)

        val expectedModifier = price.modifier
          .orElse(Some(PriceModifier.empty))
          .map { m =>
            m.copy(userQuotaChanged = Some {
              UserQuotaRemoved(originalProlongPrice = 500L)
            })
          }

        price.modifier.userQuotaRemoved shouldBe None
        result.price.modifier shouldBe expectedModifier
      }
    }

    "dont keep userQuotaRestored price modifier" in {
      forAll(goodsGen(), patchedPriceGen(), offerGen()) { (genGoods, price, offer) =>
        val goods = genGoods.copy(
          context = genGoods.context.copy(
            productPrice = genGoods.context.productPrice.copy(
              price = genGoods.context.productPrice.price.copy(
                modifier = Some {
                  PriceModifier(
                    feature = None,
                    userQuotaChanged = Some {
                      UserQuotaRestored(originalProlongPrice = 500L)
                    },
                    appliedExperimentId = None
                  )
                }
              )
            )
          )
        )

        (vosClient.getOffer _).expects(goods.offer, Slave).returningZ(offer)
        mockReducePrice.expects(*, *, *, *, *, *).returningZ(price)

        val result = calculatePrice(goods)
        result.price.modifier shouldBe price.modifier
      }
    }

    "return effective price given by reducePrice()" in {
      forAll(goodsGen(), patchedPriceGen(), offerGen()) { (good, price, offer) =>
        (vosClient.getOffer _).expects(good.offer, Slave).returningZ(offer)
        mockReducePrice.expects(*, *, *, *, *, *).returningZ(price)
        val result = calculatePrice(good)
        result.price.effectivePrice shouldBe price.effectivePrice
      }
    }

    "use prolongPrice as base prolong price" in {
      val prolongPrice = 100: Funds
      forAll(
        goodsGen(
          context = goodsContextGen(
            productPriceGen(price =
              constPriceGen(
                basePrice = 200: Funds,
                prolongPrice = Some(prolongPrice)
              )
            )
          )
        ),
        patchedPriceGen(),
        offerGen()
      ) { (good, price, offer) =>
        (vosClient.getOffer _).expects(good.offer, Slave).returningZ(offer)
        mockReducePrice
          .expects(100: Funds, good.product, *, *, *, *)
          .returningZ(price)

        val result = calculatePrice(good)

        result.price.basePrice shouldBe 200
        result.price.prolongPrice.value shouldBe 100
      }
    }

    "use basePrice if prolongPrice = 0" in {
      forAll(
        goodsGen(
          context = goodsContextGen(
            productPriceGen(price =
              constPriceGen(
                basePrice = 200: Funds,
                prolongPrice = Gen.const(Some(0))
              )
            )
          )
        ),
        patchedPriceGen(),
        offerGen()
      ) { (good, price, offer) =>
        (vosClient.getOffer _).expects(good.offer, Slave).returningZ(offer)
        mockReducePrice
          .expects(200L, good.product, *, *, *, *)
          .returningZ(price)

        val result = calculatePrice(good)

        result.price.basePrice shouldBe 200
        result.price.prolongPrice.value shouldBe 200
      }
    }

    "use basePrice if prolongPrice is empty" in {
      forAll(
        goodsGen(context =
          goodsContextGen(
            productPriceGen(
              price = constPriceGen(basePrice = 200: Funds, prolongPrice = None)
            )
          )
        ),
        patchedPriceGen(),
        offerGen()
      ) { (good, price, offer) =>
        (vosClient.getOffer _).expects(good.offer, Slave).returningZ(offer)
        val basePrice = good.context.productPrice.price.basePrice
        mockReducePrice
          .expects(basePrice, good.product, *, *, *, *)
          .returningZ(price)

        val result = calculatePrice(good)

        result.price.basePrice shouldBe basePrice
        result.price.prolongPrice.value shouldBe basePrice
      }
    }

    "use periodical discount exclusion" in {
      forAll(
        patchedPriceGen(
          Some(Analytics.UserExcludedFromDiscount("exclusion-id"))
        ),
        goodsGen(context =
          goodsContextGen(
            productPriceGen(
              price = constPriceGen(basePrice = 200: Funds, prolongPrice = None)
            )
          )
        ),
        offerGen()
      ) { (price, good, offer) =>
        (vosClient.getOffer _).expects(good.offer, Slave).returningZ(offer)
        val basePrice = good.context.productPrice.price.basePrice
        mockReducePrice
          .expects(basePrice, good.product, *, *, *, *)
          .returningZ(price)

        val result = calculatePrice(good)

        result.analytics shouldBe Some(
          Analytics(Some(Analytics.UserExcludedFromDiscount("exclusion-id")))
        )
      }
    }

    "set empty periodical discount exclusion" in {
      forAll(
        patchedPriceGen(periodicalDiscountExclusion = None),
        goodsGen(context =
          goodsContextGen(
            productPriceGen(
              price = constPriceGen(basePrice = 200: Funds, prolongPrice = None)
            )
          )
        ),
        offerGen()
      ) { (price, good, offer) =>
        (vosClient.getOffer _).expects(good.offer, Slave).returningZ(offer)
        val basePrice = good.context.productPrice.price.basePrice
        mockReducePrice
          .expects(basePrice, good.product, *, *, *, *)
          .returningZ(price)

        val result = calculatePrice(good)

        result.analytics shouldBe None
      }
    }

    // Раньше при basePrice = 0 фоллбечились на получение цены из мойши.
    // При этом пользователи могут включать автопродление услуги из пакета (это
    // баг: https://st.yandex-team.ru/VSMONEY-1061).
    // У услуг из пакета basePrice = 0. Это приводило к двойной оплате:
    // списывали деньги за автопродление всего пакета, а также за автопродление
    // услуги из этого пакета.
    // Чтобы избежать этого, при автопродлении запрещаем оплату продления услуг
    // с basePrice = 0.
    "fail if basePrice = 0 and prolongPrice = 0" in {
      forAll(GoodsGen) { goodGenerated =>
        val price =
          goodGenerated.context.productPrice.price
            .copy(basePrice = 0, prolongPrice = Some(0))
        val productPrice =
          goodGenerated.context.productPrice.copy(price = price)
        val good = goodGenerated.copy(
          context = goodGenerated.context.copy(productPrice = productPrice)
        )

        autoProlongPriceCalculator
          .calculate(good)
          .failure
          .exception shouldBe a[NoBaseProlongPrice]
      }
    }

    "fail if basePrice = 0 and prolongPrice is empty" in {
      forAll(GoodsGen) { goodGenerated =>
        val price =
          goodGenerated.context.productPrice.price
            .copy(basePrice = 0, prolongPrice = None)
        val productPriceC =
          goodGenerated.context.productPrice.copy(price = price)
        val good = goodGenerated.copy(
          context = goodGenerated.context.copy(productPrice = productPriceC)
        )

        autoProlongPriceCalculator
          .calculate(good)
          .failure
          .exception shouldBe a[NoBaseProlongPrice]
      }
    }

    // Считаем всех юзеров как перекупов в этом тесте при инициализации passportService.
    // Здесь тестируем только верхнеуровнево. Подробные тесты логики
    // проставления scheduleFreeBoost в ScheduleFreeBoostCalculatorSpec.
    "return scheduleFreeBoost = true for 7-day reseller placement" in {
      forAll(
        goodsGen(
          goodsProduct = Placement,
          context = goodsContextGen(productPriceGen(duration = SevenDays))
        ),
        offerGen(offerCategoryGen = OfferCategoryKnownGen),
        patchedPriceGen()
      ) { (goods, offer, price) =>
        (vosClient.getOffer _).expects(*, *).returningZ(offer)
        mockReducePrice
          .expects(*, *, *, *, *, *)
          .returningZ(price)
        val result = calculatePrice(goods).scheduleFreeBoost
        result shouldBe true
      }
    }

    "return scheduleFreeBoost = false for 60-day reseller placement" in {
      forAll(
        goodsGen(
          goodsProduct = Placement,
          context = goodsContextGen(productPriceGen(duration = days(60)))
        ),
        offerGen(offerCategoryGen = OfferCategoryKnownGen),
        patchedPriceGen()
      ) { (goods, offer, price) =>
        (vosClient.getOffer _).expects(*, *).returningZ(offer)
        mockReducePrice
          .expects(*, *, *, *, *, *)
          .returningZ(price)
        val result = calculatePrice(goods).scheduleFreeBoost
        result shouldBe false
      }
    }

    "return experiment from previous product activation" in {
      forAll(goodsGen(), patchedPriceGen(), offerGen()) { (good, price, offer) =>
        val activationExperiment =
          good.context.productPrice.price.modifier.flatMap(_.experimentInfo)

        val testPrice = price.copy(modifier =
          good.context.productPrice.price.modifier
            .map(_.copy(experimentInfo = activationExperiment))
        )
        (vosClient.getOffer _).expects(good.offer, Slave).returningZ(offer)
        mockReducePrice
          .expects(*, *, *, *, activationExperiment, *)
          .returningZ(testPrice)
        val result = calculatePrice(good)
        result.price.modifier.flatMap(
          _.experimentInfo
        ) shouldBe activationExperiment
      }
    }
  }
}
