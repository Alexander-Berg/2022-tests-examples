package ru.auto.salesman.service.impl

import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel
import ru.auto.api.TrucksModel.TruckCategory
import ru.auto.salesman.client.VosClient
import ru.auto.salesman.dao.user.GoodsDao
import ru.auto.salesman.dao.user.GoodsDao.Filter.UserProductDeadlineSince
import ru.auto.salesman.dao.user.GoodsDao.Request
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.user._
import ru.auto.salesman.model.user.product.AutoruProduct
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Placement
import ru.auto.salesman.model.{
  AutoruUser,
  DeprecatedDomain,
  DeprecatedDomains,
  Funds,
  Slave
}
import ru.auto.salesman.service.impl.UserQuotaModerationServiceImpl.OfferGoods
import ru.auto.salesman.service.impl.user.notify.NotificationSender
import ru.auto.salesman.service.user.PriceService
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.OfferModelGenerators
import ru.auto.salesman.test.model.gens.user.UserModelGenerators
import ru.auto.salesman.util.PriceRequestContextOffers
import ru.yandex.vertis.moderation.proto.Model.Domain.{UsersAutoru => ModerationCategory}
import ru.yandex.vertis.util.time.DateTimeUtil

class UserQuotaModerationServiceSpec
    extends BaseSpec
    with UserModelGenerators
    with OfferModelGenerators {
  private val goodsDao = mock[GoodsDao]
  private val vosClient = mock[VosClient]
  private val priceService = mock[PriceService]
  private val notificationSender = mock[NotificationSender]

  private val service =
    new UserQuotaModerationServiceImpl(
      goodsDao,
      vosClient,
      priceService,
      notificationSender
    )

  val now = DateTime
    .parse("1970-01-04T00:00:00.000")
    .withZone(DateTimeUtil.DefaultTimeZone)

  val since = DateTime
    .parse("1970-01-01T00:00:00.000")
    .withZone(DateTimeUtil.DefaultTimeZone)

  "restoreQuota" should {
    def gen(
        prolongPrice: Option[Funds],
        userQuotaRemoved: Option[UserQuotaRemoved]
    ) =
      for {
        modifier <- priceModifierGen()
        //workaround for https://github.com/scalameta/scalafmt/issues/2085
        m = Some(modifier.copy(userQuotaChanged = userQuotaRemoved))
        g <- goodsGen().map { g =>
          val p = g.context.productPrice.price
            .copy(prolongPrice = prolongPrice, modifier = m)
          g.copy(context =
            g.context.copy(
              productPrice = g.context.productPrice.copy(price = p)
            )
          )
        }
        o <- offerGen()
      } yield OfferGoods(o, g)

    "restore quota" in {
      forAll(gen(prolongPrice = Some(10L), Some(UserQuotaRemoved(20L)))) { og =>
        (goodsDao.replaceBatch _).expects(*).returningZ(List(og.goods))
        service.restoreQuota(List(og)).success
      }

    }

    "not restore quota if there isn't removedQuota modifier" in {
      forAll(gen(prolongPrice = Some(10L), None)) { og =>
        (goodsDao.replaceBatch _)
          .expects(Map.empty[Goods, Request])
          .returningZ(List.empty)
        service.restoreQuota(List(og)).success
      }
    }

    "not restore quota if there isn't old prolong price" in {
      forAll(gen(None, Some(UserQuotaRemoved(20L)))) { og =>
        (goodsDao.replaceBatch _)
          .expects(Map.empty[Goods, Request])
          .returningZ(List.empty)
        service.restoreQuota(List(og)).success
      }
    }

    "withUserQuotaRestored" in {
      forAll(gen(Some(10L), Some(UserQuotaRemoved(20L)))) { og =>
        val context = og.goods.context
        import UserQuotaModerationServiceImpl.RichContext
        val res = context.withUserQuotaRestored(
          oldProlongPrice = 10L,
          newProlongPrice = 20L
        )
        res.productPrice.price.prolongPrice shouldBe Some(20L)
        res.productPrice.price.modifier.value.userQuotaChanged shouldBe Some(
          UserQuotaRestored(10L)
        )
      }
    }
  }

  "getOfferGoods" should {
    "fetch offer goods by prolong interval" in {
      val gen = for {
        g <- goodsGen().map { g =>
          g.copy(context =
            g.context.copy(productPrice =
              g.context.productPrice.copy(
                prolongationAllowed = true,
                paymentReason = Some(PaymentReasons.PremiumOffer)
              )
            )
          )
        }
        o <- offerGen(
          offerIdGen = Gen.const(g.offer.asInstanceOf[AutoruOfferId]),
          offerCategoryGen = Gen
            .oneOf(ApiOfferModel.Category.CARS, ApiOfferModel.Category.TRUCKS),
          truckCategoryGen = Gen.const(TruckCategory.TRUCK)
        )
      } yield OfferGoods(o, g)

      forAll(Gen.listOf(gen)) { offerGoods =>
        val user = AutoruUser("user:123")
        (goodsDao.get _)
          .expects(UserProductDeadlineSince(since, Placement, "user:123"))
          .returningZ(offerGoods.map(_.goods))

        offerGoods.foreach { og =>
          (vosClient.getOptOffer _)
            .expects(og.goods.offer, Slave)
            .returningZ(Some(og.offer))
        }

        val result = service
          .getOfferGoods(
            user,
            Seq(ModerationCategory.CARS, ModerationCategory.TRUCK)
          )
          .provideConstantClock(now)
          .success
          .value

        result should contain theSameElementsAs offerGoods
      }
    }

    "drop goods with not allowed prolongation" in {
      val gen = for {
        g <- goodsGen().map { g =>
          g.copy(context =
            g.context.copy(productPrice =
              g.context.productPrice.copy(
                prolongationAllowed = false,
                paymentReason = Some(PaymentReasons.PremiumOffer)
              )
            )
          )
        }
        o <- offerGen(
          offerIdGen = Gen.const(g.offer.asInstanceOf[AutoruOfferId]),
          offerCategoryGen = Gen
            .oneOf(ApiOfferModel.Category.CARS, ApiOfferModel.Category.TRUCKS),
          truckCategoryGen = Gen.const(TruckCategory.TRUCK)
        )
      } yield OfferGoods(o, g)

      forAll(Gen.listOf(gen)) { offerGoods =>
        val user = AutoruUser("user:123")
        (goodsDao.get _)
          .expects(UserProductDeadlineSince(since, Placement, "user:123"))
          .returningZ(offerGoods.map(_.goods))

        service
          .getOfferGoods(
            user,
            Seq(ModerationCategory.CARS, ModerationCategory.TRUCK)
          )
          .provideConstantClock(now)
          .success
          .value shouldBe empty
      }
    }

    "return only last goods record by offer" in {
      val offerIdGen = Gen.const(AutoruOfferId("1111111-fff"))

      val offer = offerGen(
        offerIdGen = offerIdGen,
        offerCategoryGen = Gen.const(ApiOfferModel.Category.CARS)
      ).next

      val gen = for {
        g <- goodsGen(offerIdentity = offerIdGen).map { g =>
          g.copy(context =
            g.context.copy(productPrice =
              g.context.productPrice.copy(
                prolongationAllowed = true,
                paymentReason = Some(PaymentReasons.PremiumOffer)
              )
            )
          )
        }
      } yield OfferGoods(offer, g)

      forAll(Gen.listOfN(2, gen)) { offerGoods =>
        val user = AutoruUser("user:123")
        (goodsDao.get _)
          .expects(UserProductDeadlineSince(since, Placement, "user:123"))
          .returningZ(offerGoods.map(_.goods))

        offerGoods.foreach { og =>
          (vosClient.getOptOffer _)
            .expects(og.goods.offer, Slave)
            .returningZ(Some(og.offer))
            .anyNumberOfTimes()
        }

        val result = service
          .getOfferGoods(user, Seq(ModerationCategory.CARS))
          .provideConstantClock(now)
          .success
          .value

        result shouldBe List(offerGoods.head)
      }
    }

    "drop goods without offer" in {
      val gen = for {
        g <- goodsGen().map { g =>
          g.copy(context =
            g.context.copy(productPrice =
              g.context.productPrice.copy(
                prolongationAllowed = true,
                paymentReason = Some(PaymentReasons.PaidOffer)
              )
            )
          )
        }
        o <- offerGen(
          offerIdGen = Gen.const(g.offer.asInstanceOf[AutoruOfferId]),
          offerCategoryGen = Gen
            .oneOf(ApiOfferModel.Category.CARS, ApiOfferModel.Category.TRUCKS),
          truckCategoryGen = Gen.const(TruckCategory.TRUCK)
        )
      } yield OfferGoods(o, g)

      val goodsWithoutOffer = goodsGen().map { g =>
        g.copy(context =
          g.context.copy(productPrice =
            g.context.productPrice.copy(
              prolongationAllowed = true,
              paymentReason = Some(PaymentReasons.PaidOffer)
            )
          )
        )
      }

      forAll(Gen.listOf(gen), Gen.listOf(goodsWithoutOffer)) {
        case (offerGoods, goodsWithoutOffer) =>
          val user = AutoruUser("user:123")
          (goodsDao.get _)
            .expects(UserProductDeadlineSince(since, Placement, "user:123"))
            .returningZ(offerGoods.map(_.goods) ::: goodsWithoutOffer)

          offerGoods.foreach { og =>
            (vosClient.getOptOffer _)
              .expects(og.goods.offer, Slave)
              .returningZ(Some(og.offer))
          }

          goodsWithoutOffer.foreach { g =>
            (vosClient.getOptOffer _)
              .expects(g.offer, Slave)
              .returningZ(None)
          }

          val result = service
            .getOfferGoods(
              user,
              Seq(ModerationCategory.CARS, ModerationCategory.TRUCK)
            )
            .provideConstantClock(now)
            .success
            .value

          result should contain theSameElementsAs offerGoods
      }
    }

    "drop offers with category not from domain" in {
      val gen = for {
        g <- goodsGen().map { g =>
          g.copy(context =
            g.context.copy(productPrice =
              g.context.productPrice.copy(
                prolongationAllowed = true,
                paymentReason = Some(PaymentReasons.PaidOffer)
              )
            )
          )
        }
        o <- offerGen(
          offerIdGen = Gen.const(g.offer.asInstanceOf[AutoruOfferId]),
          offerCategoryGen = Gen
            .oneOf(
              ApiOfferModel.Category.CARS,
              ApiOfferModel.Category.TRUCKS,
              ApiOfferModel.Category.MOTO
            ),
          truckCategoryGen = Gen.const(TruckCategory.TRUCK)
        )
      } yield OfferGoods(o, g)

      forAll(Gen.listOf(gen)) { offerGoods =>
        val user = AutoruUser("user:123")
        (goodsDao.get _)
          .expects(UserProductDeadlineSince(since, Placement, "user:123"))
          .returningZ(offerGoods.map(_.goods))

        offerGoods.foreach { og =>
          (vosClient.getOptOffer _)
            .expects(og.goods.offer, Slave)
            .returningZ(Some(og.offer))
        }

        val expected = offerGoods.filterNot {
          _.offer.getCategory == ApiOfferModel.Category.MOTO
        }

        val result = service
          .getOfferGoods(
            user,
            Seq(ModerationCategory.CARS, ModerationCategory.TRUCK)
          )
          .provideConstantClock(now)
          .success
          .value

        result should contain theSameElementsAs expected
      }
    }
  }

  "getGoodsToProcess" should {
    "return correct goods" in {
      def goods(offerId: String, prolongationAllowed: Boolean) =
        goodsGen().map { g =>
          g.copy(
            offer = AutoruOfferId(offerId),
            context = g.context.copy(
              productPrice = g.context.productPrice.copy(
                prolongationAllowed = prolongationAllowed
              )
            )
          )
        }.next

      val goodsA = goods("111-fff", prolongationAllowed = true)
      val goodsB = goods("222-fff", prolongationAllowed = true)
      val goodsC = goods("111-fff", prolongationAllowed = true)
      val goodsD = goods("333-fff", prolongationAllowed = false)
      val goodsE = goods("333-fff", prolongationAllowed = true)
      val goodsF = goods("444-fff", prolongationAllowed = true)
      val goodsG = goods("222-fff", prolongationAllowed = true)

      (goodsDao.get _)
        .expects(UserProductDeadlineSince(since, Placement, "user:123"))
        .returningZ {
          Iterable(goodsA, goodsB, goodsC, goodsD, goodsE, goodsF, goodsG)
        }

      val expected = Iterable(goodsA, goodsB, goodsF)

      val result = service
        .getGoodsToProcess(AutoruUser("user:123"))
        .provideConstantClock(now)
        .success
        .value

      result should contain theSameElementsAs expected
    }
  }

  "removeQuota" should {
    def gen(
        prolongPrice: Option[Funds],
        user: AutoruUser,
        offerId: String
    ): Gen[OfferGoods] =
      for {
        g <- goodsGen(goodsProduct = Gen.const(Placement)).map { g =>
          val p = g.context.productPrice.price.copy(
            prolongPrice = prolongPrice
          )
          g.copy(
            context = g.context.copy(
              productPrice = g.context.productPrice.copy(price = p)
            ),
            user = user.toString
          )
        }
        o <- offerGen(
          offerIdGen = Gen.const(AutoruOfferId(offerId)),
          offerCategoryGen = Gen
            .oneOf(ApiOfferModel.Category.CARS, ApiOfferModel.Category.TRUCKS),
          truckCategoryGen = Gen.const(TruckCategory.TRUCK)
        )
      } yield OfferGoods(o, g)

    "remove quota" in {
      val price = 100L
      val user = AutoruUser("user:123")
      val offerId = "123-abc"
      forAll(
        gen(Some(price), user, offerId),
        ProductPricesGen.map(_.copy(offerId = offerId))
      ) { (offerGoods, rawPrices) =>
        val necessaryPrice =
          productPriceGen(
            product = Gen.const(offerGoods.goods.product),
            price = PriceGen.map(_.copy(prolongPrice = Some(120)))
          ).next
        val prices = rawPrices.copy(prices =
          necessaryPrice :: rawPrices.prices.filterNot(
            _.product == offerGoods.goods.product
          )
        )
        (priceService
          .calculatePricesForMultipleOffers(
            _: List[AutoruProduct],
            _: PriceRequestContextOffers
          ))
          .expects(
            List(offerGoods.goods.product),
            PriceRequestContextOffers(
              offers = List(offerGoods.offer),
              applyMoneyFeature = false,
              applyProlongInterval = false,
              user = Some(user)
            )
          )
          .returningZ(List(prices))

        val context = offerGoods.goods.context.copy(productPrice =
          offerGoods.goods.context.productPrice.copy(
            price = offerGoods.goods.context.productPrice.price
              .copy(
                prolongPrice = Some(120),
                modifier = offerGoods.goods.context.productPrice.price.modifier
                  .orElse(Some(PriceModifier.empty))
                  .map {
                    _.copy(userQuotaChanged = Some(UserQuotaRemoved(price)))
                  }
              )
          )
        )

        val request = Map(
          offerGoods.goods -> Request(
            offer = offerGoods.goods.offer,
            user = "user:123",
            product = offerGoods.goods.product,
            amount = 0,
            status = offerGoods.goods.status,
            transactionId = offerGoods.goods.transactionId,
            baseGoodsId = Some(offerGoods.goods.id),
            activated = offerGoods.goods.activated,
            deadline = offerGoods.goods.deadline,
            context = context,
            prolongable = Prolongable(false)
          )
        )

        (goodsDao.replaceBatch _)
          .expects(request)
          .returningZ(List(offerGoods.goods))

        (notificationSender.notifyProlongDisableAfterPriceChange _)
          .expects(AutoruUser("user:123"))
          .returningZ(())

        service.removeQuota(user, List(offerGoods)).success.value
      }
    }

    "don't touch prolongation on empty prolong price" in {
      val price = 100L
      val user = AutoruUser("user:123")
      val offerId = "123-abc"
      forAll(
        gen(Some(price), user, offerId),
        ProductPricesGen.map(_.copy(offerId = offerId))
      ) { (offerGoods, rawPrices) =>
        val prices = rawPrices.copy(prices =
          rawPrices.prices.filterNot(_.product == offerGoods.goods.product)
        )
        (priceService
          .calculatePricesForMultipleOffers(
            _: List[AutoruProduct],
            _: PriceRequestContextOffers
          ))
          .expects(
            List(offerGoods.goods.product),
            PriceRequestContextOffers(
              offers = List(offerGoods.offer),
              applyMoneyFeature = false,
              applyProlongInterval = false,
              user = Some(user)
            )
          )
          .returningZ(List(prices))

        (goodsDao.replaceBatch _)
          .expects(*)
          .never()

        service.removeQuota(user, List(offerGoods)).success.value
      }
    }

    "don't replace goods and send notifications on same price" in {
      val price = 100L
      val user = AutoruUser("user:123")
      val offerId = "123-abc"
      forAll(
        gen(Some(price), user, offerId),
        ProductPricesGen.map(_.copy(offerId = offerId))
      ) { (offerGoods, rawPrices) =>
        val necessaryPrice =
          productPriceGen(
            product = Gen.const(offerGoods.goods.product),
            price = PriceGen.map(_.copy(prolongPrice = Some(100)))
          ).next
        val prices = rawPrices.copy(prices =
          necessaryPrice :: rawPrices.prices.filterNot(
            _.product == offerGoods.goods.product
          )
        )
        (priceService
          .calculatePricesForMultipleOffers(
            _: List[AutoruProduct],
            _: PriceRequestContextOffers
          ))
          .expects(
            List(offerGoods.goods.product),
            PriceRequestContextOffers(
              offers = List(offerGoods.offer),
              applyMoneyFeature = false,
              applyProlongInterval = false,
              user = Some(user)
            )
          )
          .returningZ(List(prices))

        (goodsDao.replaceBatch _)
          .expects(*)
          .never()

        service.removeQuota(user, List(offerGoods)).success.value
      }
    }

    "remove user quota" in {
      val price = 100L
      val user = AutoruUser("user:123")
      val offerId = "123-abc"
      forAll(
        gen(Some(price), user, offerId).map { og =>
          val context = og.goods.context.copy(productPrice =
            og.goods.context.productPrice.copy(
              prolongationAllowed = true,
              paymentReason = Some(PaymentReasons.PremiumOffer)
            )
          )
          og.copy(goods = og.goods.copy(context = context))
        },
        ProductPricesGen.map(_.copy(offerId = offerId))
      ) { (offerGoods, rawPrices) =>
        (goodsDao.get _)
          .expects(UserProductDeadlineSince(since, Placement, "user:123"))
          .returningZ(Seq(offerGoods.goods))

        (vosClient.getOptOffer _)
          .expects(offerGoods.goods.offer, Slave)
          .returningZ(Some(offerGoods.offer))

        val necessaryPrice =
          productPriceGen(
            product = Gen.const(offerGoods.goods.product),
            price = PriceGen.map(_.copy(prolongPrice = Some(120)))
          ).next
        val prices = rawPrices.copy(prices =
          necessaryPrice :: rawPrices.prices.filterNot(
            _.product == offerGoods.goods.product
          )
        )
        (priceService
          .calculatePricesForMultipleOffers(
            _: List[AutoruProduct],
            _: PriceRequestContextOffers
          ))
          .expects(
            List(offerGoods.goods.product),
            PriceRequestContextOffers(
              offers = List(offerGoods.offer),
              applyMoneyFeature = false,
              applyProlongInterval = false,
              user = Some(user)
            )
          )
          .returningZ(List(prices))

        (goodsDao.replaceBatch _)
          .expects(*)
          .returningZ(List(offerGoods.goods))

        (notificationSender.notifyProlongDisableAfterPriceChange _)
          .expects(AutoruUser("user:123"))
          .returningZ(())

        service
          .removeUserQuota(
            user,
            Seq(ModerationCategory.CARS, ModerationCategory.TRUCK)
          )
          .provideConstantClock(now)
          .success
          .value
      }
    }

    "remove user quota exclude goods with UserQuotaRemoved modifier" in {
      val price = 100L
      val user = AutoruUser("user:123")
      val offerId = "123-abc"
      forAll(
        gen(Some(price), user, offerId).map { og =>
          val context = og.goods.context.copy(productPrice =
            og.goods.context.productPrice.copy(
              prolongationAllowed = true,
              price = og.goods.context.productPrice.price.copy(
                modifier = Some {
                  og.goods.context.productPrice.price.modifier
                    .getOrElse(PriceModifier.empty)
                    .copy(userQuotaChanged = Some(UserQuotaRemoved(100L)))
                }
              )
            )
          )
          og.copy(goods = og.goods.copy(context = context))
        }
      ) { offerGoods =>
        (goodsDao.get _)
          .expects(UserProductDeadlineSince(since, Placement, "user:123"))
          .returningZ(Seq(offerGoods.goods))

        (vosClient.getOptOffer _)
          .expects(offerGoods.goods.offer, Slave)
          .returningZ(Some(offerGoods.offer))

        (priceService
          .calculatePricesForMultipleOffers(
            _: List[AutoruProduct],
            _: PriceRequestContextOffers
          ))
          .expects(*, *)
          .never()

        (goodsDao.replaceBatch _)
          .expects(*)
          .never()

        service
          .removeUserQuota(
            user,
            Seq(ModerationCategory.CARS, ModerationCategory.TRUCK)
          )
          .provideConstantClock(now)
          .success
          .value
      }
    }
  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
