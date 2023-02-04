package ru.auto.salesman.service.impl.user

import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.auto.salesman.model.user.PriceModifier.ProlongInterval
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Placement
import ru.auto.salesman.model.user.periodical_discount_exclusion.Product.{
  InDiscount,
  NoActiveDiscount,
  UserExcludedFromDiscount
}
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains, Funds}
import ru.auto.salesman.model.user.{
  Analytics,
  PeriodicalDiscount,
  PriceModifier,
  UserQuotaRemoved,
  UserQuotaRestored
}
import ru.auto.salesman.service.user.ModifyPriceService.{MatrixPrice, PatchedPrice}
import ru.auto.salesman.service.user.{PriceService, UserFeatureService}
import ru.auto.salesman.service.user.PriceService.{
  priceToFunds,
  MoneyFeatureInstance,
  ZeroPrice
}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.ServiceModelGenerators

class ModifyPriceServiceImplSpec extends BaseSpec with ServiceModelGenerators {

  import ModifyPriceServiceImplSpec._

  val featureService: UserFeatureService = mock[UserFeatureService]

  val modifyPriceService =
    new ModifyPriceServiceImpl(featureService)

  (featureService.bestServicePriceEnabled _)
    .expects()
    .returning(true)
    .anyNumberOfTimes()

  (featureService.cashbackFullPaymentRestrictionEnabled _)
    .expects()
    .returning(true)
    .anyNumberOfTimes()

  val constOriginalPrice = PriceService.priceToFunds(200)

  "ModifyPriceService" should {

    "patch price with experiment and global discount if it's free by quota" in {
      forAll(
        featureInstanceGen,
        PeriodicalDiscountGen,
        ExperimentInfoGen,
        Gen.const(constOriginalPrice)
      ) { (featureInstance, periodicalDiscount, experimentInfo, originalPrice) =>
        val inDiscount = InDiscount(periodicalDiscount)
        modifyPriceService
          .buildPatchedPrice(
            isFreeByQuota = true,
            originalPrice,
            zeroPriceFeature = None,
            List(featureInstance),
            inDiscount,
            Some(experimentInfo),
            experimentInfo.activeExperimentId,
            applyMoneyFeature = false,
            bonusMoneyFeatures = List.empty,
            prolongIntervalInfo = None
          )
          .success
          .value shouldBe PatchedPrice(
          basePrice = ZeroPrice,
          effectivePrice = ZeroPrice,
          Some(
            PriceModifier(
              feature = None,
              bundleId = None,
              Some(experimentInfo),
              experimentInfo.activeExperimentId,
              periodicalDiscount = Some(
                PriceModifier.PeriodicalDiscount(
                  periodicalDiscount.discountId,
                  periodicalDiscount.discount,
                  periodicalDiscount.deadline
                )
              )
            )
          ),
          periodicalDiscountExclusion = None
        )
      }
    }

    "patch price with periodical discount exclusion" in {
      forAll(PeriodicalDiscountGen, Gen.const(constOriginalPrice)) {

        (periodicalDiscount, originalPrice) =>
          val userExcludedFromDiscount =
            UserExcludedFromDiscount(periodicalDiscount)
          modifyPriceService
            .buildPatchedPrice(
              isFreeByQuota = false,
              originalPrice,
              zeroPriceFeature = None,
              List(),
              userExcludedFromDiscount,
              experiment = None,
              appliedExperiment = None,
              applyMoneyFeature = false,
              bonusMoneyFeatures = List.empty,
              prolongIntervalInfo = None
            )
            .success
            .value shouldBe PatchedPrice(
            basePrice = originalPrice,
            effectivePrice = originalPrice,
            modifier = None,
            periodicalDiscountExclusion = Some(
              Analytics.UserExcludedFromDiscount(periodicalDiscount.discountId)
            )
          )
      }
    }

    "do not patch zero price with feature" in {
      forAll(featureInstanceGen) { feature =>
        modifyPriceService
          .patchPriceWithPromoFeature(None, List(feature))(
            PatchedPrice(
              testBasePriceFor(ZeroPrice),
              ZeroPrice,
              modifier = None,
              periodicalDiscountExclusion = None
            )
          )
          .effectivePrice shouldBe 0L
      }
    }

    "do not patch zero price with full feature" in {
      forAll(featureInstanceGen) { feature =>
        modifyPriceService
          .patchPriceWithPromoFeature(Some(feature), Nil)(
            PatchedPrice(
              testBasePriceFor(ZeroPrice),
              ZeroPrice,
              modifier = None,
              periodicalDiscountExclusion = None
            )
          )
          .effectivePrice shouldBe 0L
      }
    }

    "do not patch price with feature if it's became zero after periodical discount" in {
      forAll(featureInstanceGen) { feature =>
        val inDiscount = InDiscount(
          PeriodicalDiscount(
            "discountId",
            DateTime.now,
            DateTime.now,
            100,
            None
          )
        )
        val originalPrice = priceToFunds(1000)
        val price = modifyPriceService
          .buildPatchedPrice(
            isFreeByQuota = false,
            originalPrice,
            zeroPriceFeature = None,
            List(feature),
            inDiscount,
            experiment = None,
            appliedExperiment = None,
            applyMoneyFeature = false,
            bonusMoneyFeatures = List.empty,
            prolongIntervalInfo = None
          )
          .success
          .value
        println(price)
        price.modifier.get.feature shouldBe None
        price.effectivePrice shouldBe 0
      }
    }

    "do not patch price with full feature" in {
      forAll(featureInstanceGen) { feature =>
        val price = PriceService.priceToFunds(499)
        modifyPriceService
          .patchPriceWithPromoFeature(Some(feature), Nil)(
            PatchedPrice(
              testBasePriceFor(price),
              price,
              modifier = None,
              periodicalDiscountExclusion = None
            )
          )
          .effectivePrice shouldBe 0L
      }
    }

    "patch price with fixed price feature, where feature price < product price" in {
      forAll(ProductGen) { product =>
        val intPrice = 200
        val price = PriceService.priceToFunds(intPrice)
        forAll(featureInstanceFixedPriceInRangeGen(product, 1, intPrice - 1)) { feature =>
          modifyPriceService
            .patchPriceWithPromoFeature(None, List(feature))
            .patch(
              PatchedPrice(
                testBasePriceFor(price),
                price,
                modifier = None,
                periodicalDiscountExclusion = None
              )
            )
            .effectivePrice shouldBe priceToFunds(
            feature.payload.discount.get.value
          )
        }
      }
    }

    "don't patch price with fixed price feature, where feature price > product price" in {
      forAll(ProductGen) { product =>
        val price = PriceService.priceToFunds(200)
        forAll(
          featureInstanceFixedPriceInRangeGen(
            product,
            price.toInt + 1,
            price.toInt + 500
          )
        ) { feature =>
          modifyPriceService
            .patchPriceWithPromoFeature(None, List(feature))(
              PatchedPrice(
                testBasePriceFor(price),
                price,
                modifier = None,
                periodicalDiscountExclusion = None
              )
            )
            .effectivePrice shouldBe price
        }
      }
    }

    "patch price with percent feature" in {
      forAll(ProductGen) { product =>
        forAll(featureInstancePercentGen(product)) { feature =>
          val price = PriceService.priceToFunds(200)
          val priceWithDiscount = PriceService.priceWithDiscount(
            price,
            feature.payload.discount.get.value
          )
          modifyPriceService
            .patchPriceWithPromoFeature(None, List(feature))(
              PatchedPrice(
                testBasePriceFor(price),
                price,
                modifier = None,
                periodicalDiscountExclusion = None
              )
            )
            .effectivePrice shouldBe priceWithDiscount
        }
      }
    }

    "choose best price" in {
      forAll(ProductGen) { product =>
        forAll(
          featureInstancePercentGen(product),
          featureInstanceFixedPriceGen(product)
        ) { (percentFeature, fixedPriceFeature) =>
          val featureService: UserFeatureService = mock[UserFeatureService]
          val modifyPriceService =
            new ModifyPriceServiceImpl(featureService)
          (featureService.bestServicePriceEnabled _)
            .expects()
            .returning(true)
            .anyNumberOfTimes()

          val price = PriceService.priceToFunds(200)
          val priceAfterDiscount = PriceService.priceWithDiscount(
            price,
            percentFeature.payload.discount.get.value
          )
          val fixedPrice =
            priceToFunds(fixedPriceFeature.payload.discount.get.value.toLong)

          modifyPriceService
            .patchPriceWithPromoFeature(
              None,
              List(fixedPriceFeature, percentFeature)
            )(
              PatchedPrice(
                testBasePriceFor(price),
                price,
                modifier = None,
                periodicalDiscountExclusion = None
              )
            )
            .effectivePrice shouldBe List(
            price,
            priceAfterDiscount,
            fixedPrice
          ).min
        }
      }
    }

    "return price if no feature" in {
      val price = PriceService.priceToFunds(200)
      modifyPriceService
        .patchPriceWithPromoFeature(None, Nil)(
          PatchedPrice(
            testBasePriceFor(price),
            price,
            modifier = None,
            periodicalDiscountExclusion = None
          )
        )
        .effectivePrice shouldBe price
    }

    "return zero if original price is zero" in {
      val result = modifyPriceService
        .buildPatchedPrice(
          isFreeByQuota = false,
          matrixPrice = 0,
          zeroPriceFeature = None,
          promoFeatures = Nil,
          periodicalDiscount = NoActiveDiscount,
          experiment = None,
          appliedExperiment = None,
          applyMoneyFeature = false,
          bonusMoneyFeatures = List.empty,
          prolongIntervalInfo = None
        )
        .success
        .value
      result.effectivePrice shouldBe 0
      result.modifier shouldBe None
    }

    "apply bonus money feature if it exists and zero price feature is empty" in {
      val originalPrice = 300
      val discount = 100
      forAll(moneyFeatureInstanceGen(countGen = discount)) { bonusMoneyFeature =>
        val result = modifyPriceService
          .buildPatchedPrice(
            isFreeByQuota = false,
            originalPrice,
            zeroPriceFeature = None,
            promoFeatures = Nil,
            periodicalDiscount = NoActiveDiscount,
            experiment = None,
            appliedExperiment = None,
            bonusMoneyFeatures = List(bonusMoneyFeature),
            prolongIntervalInfo = None
          )
          .success
          .value
        result.effectivePrice shouldBe 200
      }
    }

    "set price to 0 when applying cashback if feature is not toggled" in {
      val features = mock[UserFeatureService]
      val service = new ModifyPriceServiceImpl(features)

      (features.cashbackFullPaymentRestrictionEnabled _)
        .expects()
        .returning(false)
        .anyNumberOfTimes()

      (features.bestServicePriceEnabled _)
        .expects()
        .returning(true)
        .anyNumberOfTimes()

      val originalPrice = 300
      val discount = 300
      forAll(cashbackGen(countGen = discount)) { bonusMoneyFeature =>
        val result = service
          .buildPatchedPrice(
            isFreeByQuota = false,
            originalPrice,
            zeroPriceFeature = None,
            promoFeatures = Nil,
            periodicalDiscount = NoActiveDiscount,
            experiment = None,
            appliedExperiment = None,
            bonusMoneyFeatures = List(bonusMoneyFeature),
            prolongIntervalInfo = None
          )
          .success
          .value
        result.effectivePrice shouldBe 0
      }
    }

    "not set price to 0 when applying cashback" in {
      val originalPrice = 300
      val discount = 300
      forAll(cashbackGen(countGen = discount)) { bonusMoneyFeature =>
        val result = modifyPriceService
          .buildPatchedPrice(
            isFreeByQuota = false,
            originalPrice,
            zeroPriceFeature = None,
            promoFeatures = Nil,
            periodicalDiscount = NoActiveDiscount,
            experiment = None,
            appliedExperiment = None,
            bonusMoneyFeatures = List(bonusMoneyFeature),
            prolongIntervalInfo = None
          )
          .success
          .value
        result.effectivePrice shouldBe 100
      }
    }

    "set price to 0 when applying a money promocode" in {
      val originalPrice = 300
      val discount = 300
      forAll(
        featureInstanceGen(readableString, promocodeFeaturePayloadGen, discount)
      ) { feature =>
        val result = modifyPriceService
          .buildPatchedPrice(
            isFreeByQuota = false,
            originalPrice,
            zeroPriceFeature = None,
            promoFeatures = Nil,
            periodicalDiscount = NoActiveDiscount,
            experiment = None,
            appliedExperiment = None,
            bonusMoneyFeatures = List(MoneyFeatureInstance(feature)),
            prolongIntervalInfo = None
          )
          .success
          .value
        result.effectivePrice shouldBe 0
      }
    }

    "set price to 1 ruble when there are cashback and 1 ruble promocode" in {
      val originalPrice = 300
      val discount = 300

      val moneyFeatures =
        for {
          cashback <- cashbackGen(countGen = discount)
          promocode <- featureInstanceGen(
            readableString,
            promocodeFeaturePayloadGen,
            100
          )
        } yield List(cashback, MoneyFeatureInstance(promocode))

      forAll(moneyFeatures) { bonusMoneyFeatures =>
        val result = modifyPriceService
          .buildPatchedPrice(
            isFreeByQuota = false,
            originalPrice,
            zeroPriceFeature = None,
            promoFeatures = Nil,
            periodicalDiscount = NoActiveDiscount,
            experiment = None,
            appliedExperiment = None,
            bonusMoneyFeatures = bonusMoneyFeatures,
            prolongIntervalInfo = None
          )
          .success
          .value
        result.effectivePrice shouldBe 100
      }
    }

    "set price to 0 with promocode when there are cashback, promocode and service price is 1 ruble" in {
      val originalPrice = 100

      val moneyFeatures =
        for {
          cashback <- cashbackGen(countGen = 300)
          promocode <- featureInstanceGen(
            readableString,
            promocodeFeaturePayloadGen,
            100
          )
        } yield (promocode.id, List(cashback, MoneyFeatureInstance(promocode)))

      forAll(moneyFeatures) { case (featureId, bonusMoneyFeatures) =>
        val result = modifyPriceService
          .buildPatchedPrice(
            isFreeByQuota = false,
            originalPrice,
            zeroPriceFeature = None,
            promoFeatures = Nil,
            periodicalDiscount = NoActiveDiscount,
            experiment = None,
            appliedExperiment = None,
            bonusMoneyFeatures = bonusMoneyFeatures,
            prolongIntervalInfo = None
          )
          .success
          .value
        result.modifier
          .flatMap(_.feature)
          .exists(_.featureInstanceId == featureId) shouldBe true
        result.effectivePrice shouldBe 0
      }
    }

    "apply promo feature if it exists and zero price feature is empty; not apply bonus money feature" in {
      val originalPrice = 30000
      val promoFeatureGen = featureInstancePercentGen(Placement, 50)
      val bonusMoneyFeatureGen = moneyFeatureInstanceGen(countGen = 10000)
      forAll(promoFeatureGen, bonusMoneyFeatureGen) { (promoFeature, bonusMoneyFeature) =>
        val result = modifyPriceService
          .buildPatchedPrice(
            isFreeByQuota = false,
            originalPrice,
            zeroPriceFeature = None,
            promoFeatures = List(promoFeature),
            periodicalDiscount = NoActiveDiscount,
            experiment = None,
            appliedExperiment = None,
            bonusMoneyFeatures = List(bonusMoneyFeature),
            prolongIntervalInfo = None
          )
          .success
          .value
        // Не должен применять bonusMoneyFeature после promoFeature!
        // Поэтому 30000-50%, а не 30000-50%-10000
        result.effectivePrice shouldBe 15000
        result.modifier.value.feature.value.featureInstanceId shouldBe promoFeature.id
      }
    }

    "return original price if applyMoneyFeature = false" in {
      forAll(Gen.posNum[Long], moneyFeatureInstanceGen()) {
        (originalPrice, bonusMoneyFeature) =>
          val result = modifyPriceService
            .buildPatchedPrice(
              isFreeByQuota = false,
              originalPrice,
              zeroPriceFeature = None,
              promoFeatures = Nil,
              periodicalDiscount = NoActiveDiscount,
              experiment = None,
              appliedExperiment = None,
              applyMoneyFeature = false,
              bonusMoneyFeatures = List(bonusMoneyFeature),
              prolongIntervalInfo = None
            )
            .success
            .value
          result.effectivePrice shouldBe originalPrice
          result.modifier shouldBe None
      }
    }

    "apply only zero price feature; not apply money feature" in {
      forAll(
        Gen.posNum[Long],
        featureInstanceGen,
        Gen.listOf(moneyFeatureInstanceGen())
      ) { (originalPrice, zeroPriceFeature, bonusMoneyFeature) =>
        val result = modifyPriceService
          .buildPatchedPrice(
            isFreeByQuota = false,
            originalPrice,
            zeroPriceFeature = Some(zeroPriceFeature),
            promoFeatures = Nil,
            periodicalDiscount = NoActiveDiscount,
            experiment = None,
            appliedExperiment = None,
            applyMoneyFeature = true,
            bonusMoneyFeatures = bonusMoneyFeature,
            prolongIntervalInfo = None
          )
          .success
          .value
        result.effectivePrice shouldBe 0
        result.modifier.value.feature.value.featureInstanceId shouldBe zeroPriceFeature.id
      }
    }

    def prolongIntervalInfoFixedPriceGen(value: Funds) =
      prolongIntervalInfoGen(
        productPrice =
          productPriceGen(price = constPriceGen(prolongPrice = Gen.some(value)))
      )

    def prolongIntervalPriceInIntervalGen(from: Int, to: Int) =
      prolongIntervalInfoGen(
        productPrice = productPriceGen(price =
          constPriceGen(
            prolongPrice = Gen.some(Gen.choose(min = from, max = to))
          )
        )
      )

    "apply prolongIntervalPrice" in {
      val originalPrice = 300
      val prolongPrice = 200
      val discount = 100
      forAll(
        moneyFeatureInstanceGen(countGen = discount),
        prolongIntervalInfoFixedPriceGen(prolongPrice)
      ) { (moneyFeature, prolongIntervalInfo) =>
        val result = modifyPriceService
          .buildPatchedPrice(
            isFreeByQuota = false,
            originalPrice,
            zeroPriceFeature = None,
            promoFeatures = Nil,
            periodicalDiscount = NoActiveDiscount,
            experiment = None,
            appliedExperiment = None,
            applyMoneyFeature = true,
            bonusMoneyFeatures = List(moneyFeature),
            prolongIntervalInfo = Some(prolongIntervalInfo)
          )
          .success
          .value

        result.modifier.value.prolongInterval.value.prolongPrice shouldBe prolongIntervalInfo.prolongPrice
        result.modifier.value.feature.value.featureInstanceId shouldBe moneyFeature.featureInstance.id
        result.modifier.value.prolongInterval.value.willExpire shouldBe prolongIntervalInfo.willExpire
        result.effectivePrice shouldBe 100
      }
    }

    "apply prolongIntervalPrice & promoFeature" in {
      forAll(
        Gen.choose(min = 51, max = 100),
        prolongIntervalPriceInIntervalGen(1, 50),
        featureInstanceGen
      ) { (originalPrice, prolongIntervalInfo, zeroPriceFeature) =>
        val result = modifyPriceService
          .buildPatchedPrice(
            isFreeByQuota = false,
            originalPrice,
            zeroPriceFeature = Some(zeroPriceFeature),
            promoFeatures = Nil,
            periodicalDiscount = NoActiveDiscount,
            experiment = None,
            appliedExperiment = None,
            applyMoneyFeature = true,
            bonusMoneyFeatures = List.empty,
            prolongIntervalInfo = Some(prolongIntervalInfo)
          )
          .success
          .value

        result.effectivePrice shouldBe 0
        result.modifier.value.feature.value.featureInstanceId shouldBe zeroPriceFeature.id
        result.modifier.value.prolongInterval.value.prolongPrice shouldBe prolongIntervalInfo.prolongPrice
        result.modifier.value.prolongInterval.value.willExpire shouldBe prolongIntervalInfo.willExpire
      }
    }

    "apply prolongIntervalPrice & moneyFeature" in {
      forAll(
        Gen.choose(min = 51, max = 100),
        prolongIntervalPriceInIntervalGen(1, 50),
        featureInstanceGen
      ) { (originalPrice, prolongIntervalInfo, zeroPriceFeature) =>
        val result = modifyPriceService
          .buildPatchedPrice(
            isFreeByQuota = false,
            originalPrice,
            zeroPriceFeature = Some(zeroPriceFeature),
            promoFeatures = Nil,
            periodicalDiscount = NoActiveDiscount,
            experiment = None,
            appliedExperiment = None,
            applyMoneyFeature = true,
            bonusMoneyFeatures = List.empty,
            prolongIntervalInfo = Some(prolongIntervalInfo)
          )
          .success
          .value

        result.effectivePrice shouldBe 0
        result.modifier.value.feature.value.featureInstanceId shouldBe zeroPriceFeature.id
        result.modifier.value.prolongInterval.value.prolongPrice shouldBe prolongIntervalInfo.prolongPrice
        result.modifier.value.prolongInterval.value.willExpire shouldBe prolongIntervalInfo.willExpire
      }
    }

    "not apply prolongIntervalPrice if product is free by quota" in {
      forAll(Gen.posNum[Long], prolongIntervalInfoGen()) {
        (originalPrice, prolongIntervalInfo) =>
          val result = modifyPriceService
            .buildPatchedPrice(
              isFreeByQuota = true,
              originalPrice,
              zeroPriceFeature = None,
              promoFeatures = Nil,
              periodicalDiscount = NoActiveDiscount,
              experiment = None,
              appliedExperiment = None,
              applyMoneyFeature = true,
              bonusMoneyFeatures = List.empty,
              prolongIntervalInfo = Some(prolongIntervalInfo)
            )
            .success
            .value

          result.effectivePrice shouldBe 0
          result.modifier shouldBe None
      }
    }

    "not apply prolongIntervalPrice if it's > price" in {
      forAll(
        Gen.choose(min = 0, max = 50),
        prolongIntervalPriceInIntervalGen(51, 100)
      ) { (originalPrice, prolongIntervalInfo) =>
        val result = modifyPriceService
          .buildPatchedPrice(
            isFreeByQuota = false,
            originalPrice,
            zeroPriceFeature = None,
            promoFeatures = Nil,
            periodicalDiscount = NoActiveDiscount,
            experiment = None,
            appliedExperiment = None,
            applyMoneyFeature = true,
            bonusMoneyFeatures = List.empty,
            prolongIntervalInfo = Some(prolongIntervalInfo)
          )
          .success
          .value

        result.effectivePrice shouldBe originalPrice
        result.modifier shouldBe None
      }
    }

    "apply prolongIntervalPrice if it's equal to price" in {
      forAll(Gen.posNum[Long], minSuccessful(10)) { originalPrice =>
        forAll(
          prolongIntervalInfoFixedPriceGen(originalPrice),
          minSuccessful(10)
        ) { prolongIntervalInfo =>
          val result = modifyPriceService
            .buildPatchedPrice(
              isFreeByQuota = false,
              originalPrice,
              zeroPriceFeature = None,
              promoFeatures = Nil,
              periodicalDiscount = NoActiveDiscount,
              experiment = None,
              appliedExperiment = None,
              applyMoneyFeature = true,
              bonusMoneyFeatures = List.empty,
              prolongIntervalInfo = Some(prolongIntervalInfo)
            )
            .success
            .value

          result.modifier.value.prolongInterval.value.prolongPrice shouldBe prolongIntervalInfo.prolongPrice
          result.modifier.value.prolongInterval.value.willExpire shouldBe prolongIntervalInfo.willExpire
          result.effectivePrice shouldBe prolongIntervalInfo.prolongPrice
        }
      }
    }

    "keep userQuotaRemove modifier in patchPriceWithProlongIntervalPrice" in {
      forAll(prolongIntervalInfoFixedPriceGen(value = 20000L)) { info =>
        val prolongIntervalInfo = info.copy(
          productPrice = info.productPrice.copy(
            price = info.productPrice.price.copy(
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

        val result = MatrixPrice(matrixPrice = 50000L).apply(
          modifyPriceService.patchPriceWithProlongIntervalPrice {
            Some(prolongIntervalInfo)
          }
        )

        result.modifier shouldBe Some {
          PriceModifier(
            feature = None,
            prolongInterval = Some {
              ProlongInterval(info.prolongPrice, info.willExpire)
            },
            userQuotaChanged = Some {
              UserQuotaRemoved(originalProlongPrice = 500L)
            },
            appliedExperimentId = None
          )
        }
      }
    }

    "dont keep userQuotaRestore modifier in patchPriceWithProlongIntervalPrice" in {
      forAll(prolongIntervalInfoFixedPriceGen(value = 20000L)) { info =>
        val prolongIntervalInfo = info.copy(
          productPrice = info.productPrice.copy(
            price = info.productPrice.price.copy(
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

        val result = MatrixPrice(matrixPrice = 50000L).apply(
          modifyPriceService.patchPriceWithProlongIntervalPrice {
            Some(prolongIntervalInfo)
          }
        )

        result.modifier shouldBe Some {
          PriceModifier(
            feature = None,
            prolongInterval = Some {
              ProlongInterval(info.prolongPrice, info.willExpire)
            },
            appliedExperimentId = None
          )
        }
      }
    }

  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}

object ModifyPriceServiceImplSpec {

  def testBasePriceFor(price: Funds): Funds = price + 100
}
