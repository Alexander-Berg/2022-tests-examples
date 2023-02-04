package ru.auto.salesman.service.impl.user

import org.scalatest.BeforeAndAfter
import ru.auto.salesman.client.PromocoderClient
import ru.auto.salesman.dao.impl.jdbc.user.{JdbcBundleDao, JdbcGoodsDao}
import ru.auto.salesman.model.user.PaymentReasons
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods._
import ru.auto.salesman.service.user.{UserFeatureService, UserPromocodesService}
import ru.auto.salesman.test.{BaseSpec, IntegrationPropertyCheckConfig}
import ru.auto.salesman.test.model.gens.user.{UserDaoGenerators, UserModelGenerators}
import ru.auto.salesman.test.template.SalesmanUserJdbcSpecTemplate
import org.scalacheck.Gen
import ru.auto.salesman.dao.user.GoodsDao.Filter.ForGoodsId
import ru.auto.salesman.dao.user.ProductScheduleDao
import ru.auto.salesman.model.ProductStatuses.{Active, Inactive}
import ru.auto.salesman.model.user.product.ProductProvider.AutoruBundles.Vip
import ru.auto.salesman.model.user.product.Products
import ru.auto.salesman.service.impl.user.goods.BoostScheduler

class GoodsServiceImplSpec
    extends BaseSpec
    with UserModelGenerators
    with UserDaoGenerators
    with SalesmanUserJdbcSpecTemplate
    with BeforeAndAfter
    with IntegrationPropertyCheckConfig {

  private val goodsDao = new JdbcGoodsDao(database)
  private val bundleDao = new JdbcBundleDao(database)
  private val promocoder = mock[PromocoderClient]
  private val userPromocodesService = mock[UserPromocodesService]
  private val productScheduleDao = mock[ProductScheduleDao]
  private val boostScheduler = new BoostScheduler(productScheduleDao)
  private val userFeatureService = mock[UserFeatureService]

  (userFeatureService.cashbackFullPaymentRestrictionEnabled _)
    .expects()
    .returning(true)
    .anyNumberOfTimes()

  private val goodsService =
    new GoodsServiceImpl(
      goodsDao,
      bundleDao,
      promocoder,
      userPromocodesService,
      boostScheduler,
      userFeatureService
    )

  private val bundlesDao = new JdbcBundleDao(database)

  after {
    database.withSession { session =>
      session.conn.prepareStatement("DELETE FROM goods").execute()
      session.conn.prepareStatement("DELETE FROM bundle").execute()
    }
  }

  "GoodsServiceImpl.add" should {
    "immediately succeed for already existing good, compare goods by transactionId" in {
      val product = productNotPlacementGen[Products.Goods].next
      forAll(
        paidTransactionGen(),
        productRequestGen(product = product),
        minSuccessful(2)
      ) { (transaction, request) =>
        forAll(
          goodsCreateRequestGen(
            offerId = request.offer.get,
            product = product,
            userId = transaction.user,
            status = Active,
            transactionId = transaction.transactionId
          ),
          minSuccessful(2)
        ) { goodRequestCreation =>
          goodsDao.insertIfNotExists(goodRequestCreation).success

          (productScheduleDao.insertIfAbsent _)
            .expects(*)
            .returningT(unit)
            .anyNumberOfTimes()

          goodsService
            .add(transaction, request)
            .success
            .value
            .transactionId shouldBe goodRequestCreation.transactionId
        }
      }
    }

    "for already added good(not placement), deactivate old good and activate new" in {
      val transactionNewProduct = paidTransactionGen().next
      val product = productNotPlacementGen[Products.Goods].next
      val requestNewProduct = productRequestGen(product = product).next

      val oldCreation = goodsCreateRequestGen(
        offerId = requestNewProduct.offer.get,
        product = product,
        userId = transactionNewProduct.user,
        status = Active
      ).next
      val oldProduct = goodsDao.insertIfNotExists(oldCreation).success.value

      val featureInstance = featureInstanceFixedPriceGen(product).next
      (promocoder.changeFeatureCount _)
        .expects(*, *)
        .returningZ(featureInstance)
        .anyNumberOfTimes()
      (userPromocodesService.applyUserMoneyPromocode _)
        .expects(*, *, *)
        .returningZ(featureInstance)
        .anyNumberOfTimes()
      (productScheduleDao.insertIfAbsent _)
        .expects(*)
        .returningT(unit)
        .anyNumberOfTimes()

      val res =
        goodsService.add(transactionNewProduct, requestNewProduct).success.value
      res.transactionId shouldBe transactionNewProduct.transactionId

      goodsDao
        .get(ForGoodsId(oldProduct.id))
        .success
        .value
        .head
        .status shouldBe Inactive
    }

    "for already added placement, prolong it's deadline by Vip deadline" in {
      val featureInstance = featureInstanceFixedPriceGen(Placement).next
      val now = dateTimeInFuture().next
      val transactionPlacement = paidTransactionGen().next
      val requestPlacement = productRequestGen(product = Placement).next

      val placementDeadline = now.plusDays(
        requestPlacement.context.productPrice.duration.days.getDays
      )
      val vipDeadline = placementDeadline.plusDays(1)

      val vipCreation = bundleCreateRequestGen(
        offerId = requestPlacement.offer.get,
        product = Vip,
        userId = transactionPlacement.user,
        status = Active,
        deadline = vipDeadline
      ).next
      bundlesDao.insertIfNotExists(vipCreation).success

      (promocoder.changeFeatureCount _)
        .expects(*, *)
        .returningZ(featureInstance)
        .anyNumberOfTimes()
      (userPromocodesService.applyUserMoneyPromocode _)
        .expects(*, *, *)
        .returningZ(featureInstance)
        .anyNumberOfTimes()
      (productScheduleDao.insertIfAbsent _)
        .expects(*)
        .returningT(unit)
        .anyNumberOfTimes()

      val res = goodsService
        .add(transactionPlacement, requestPlacement)
        .provideConstantClock(now)
        .success
        .value
      res.transactionId shouldBe transactionPlacement.transactionId
      res.deadline shouldBe vipDeadline
    }

    "use placement deadline if Vip deadline is in the past" in {
      val featureInstance = featureInstanceFixedPriceGen(Placement).next
      val transactionPlacement = paidTransactionGen().next
      val requestPlacement = productRequestGen(product = Placement).next

      val now = dateTimeInFuture().next
      val placementDeadline = now.plusDays(
        requestPlacement.context.productPrice.duration.days.getDays
      )
      val vipDeadline = placementDeadline.minusDays(1)

      val vipCreation = bundleCreateRequestGen(
        offerId = requestPlacement.offer.get,
        product = Vip,
        userId = transactionPlacement.user,
        status = Active,
        deadline = vipDeadline
      ).next
      bundlesDao.insertIfNotExists(vipCreation).success

      (promocoder.changeFeatureCount _)
        .expects(*, *)
        .returningZ(featureInstance)
        .anyNumberOfTimes()
      (userPromocodesService.applyUserMoneyPromocode _)
        .expects(*, *, *)
        .returningZ(featureInstance)
        .anyNumberOfTimes()
      (productScheduleDao.insertIfAbsent _)
        .expects(*)
        .returningT(unit)
        .anyNumberOfTimes()

      val res = goodsService
        .add(transactionPlacement, requestPlacement)
        .provideConstantClock(now)
        .success
        .value
      res.transactionId shouldBe transactionPlacement.transactionId
      res.deadline shouldBe placementDeadline
    }

    "for already added placement, deactivate old, activate new with deadline counted from placement, and prolong deadline by Vip" in {
      val transactionNewPlacement = paidTransactionGen().next
      val requestNewPlacement = productRequestGen(product = Placement).next

      val now = dateTimeInFuture().next
      val oldPlacementDeadline = now.plusDays(3)
      val vipDeadline = oldPlacementDeadline.plusDays(
        requestNewPlacement.context.productPrice.duration.days.getDays
      )
      val vipCreation = bundleCreateRequestGen(
        offerId = requestNewPlacement.offer.get,
        product = Vip,
        userId = transactionNewPlacement.user,
        status = Active,
        deadline = vipDeadline
      ).next
      bundlesDao.insertIfNotExists(vipCreation).success

      val placementOldCreation = goodsCreateRequestGen(
        offerId = requestNewPlacement.offer.get,
        product = Placement,
        userId = transactionNewPlacement.user,
        status = Active,
        deadline = oldPlacementDeadline
      ).next
      val oldPlacement =
        goodsDao.insertIfNotExists(placementOldCreation).success.value

      val featureInstance = featureInstanceFixedPriceGen(Placement).next
      (promocoder.changeFeatureCount _)
        .expects(*, *)
        .returningZ(featureInstance)
        .anyNumberOfTimes()
      (userPromocodesService.applyUserMoneyPromocode _)
        .expects(*, *, *)
        .returningZ(featureInstance)
        .anyNumberOfTimes()
      (productScheduleDao.insertIfAbsent _)
        .expects(*)
        .returningT(unit)
        .anyNumberOfTimes()

      val res = goodsService
        .add(transactionNewPlacement, requestNewPlacement)
        .provideConstantClock(now)
        .success
        .value
      res.transactionId shouldBe transactionNewPlacement.transactionId
      res.deadline shouldBe vipDeadline
      goodsDao
        .get(ForGoodsId(oldPlacement.id))
        .success
        .value
        .head
        .status shouldBe Inactive
    }

    "give cashback for first placement" in {
      val transaction = paidTransactionGen().next
      val paymentReason = Gen
        .oneOf(
          Set(
            PaymentReasons.PaidOffer,
            PaymentReasons.PremiumOffer,
            PaymentReasons.DuplicateOffer
          )
        )
        .next
      val request = productRequestGen(
        product = Placement,
        productPrice = productPriceGen(paymentReason = Some(paymentReason))
      ).next

      val featureInstance = featureInstanceFixedPriceGen(Placement).next
      (userPromocodesService.applyUserMoneyPromocode _)
        .expects(*, *, *)
        .returningZ(featureInstance)
        .once()

      (promocoder.changeFeatureCount _)
        .expects(*, *)
        .returningZ(featureInstance)
        .anyNumberOfTimes()
      (productScheduleDao.insertIfAbsent _)
        .expects(*)
        .returningT(unit)
        .anyNumberOfTimes()

      goodsService.add(transaction, request).success.value
    }

    "pay by promocoder feature" in {
      val transaction = paidTransactionGen().next
      val feature = FeatureGen.next
      val product = productNotPlacementGen[Products.Goods].next

      val request = productRequestGen(
        product = product,
        productPrice = productPriceGen(price =
          priceGen(modifier = priceModifierGen(feature = Some(feature)).map(Some(_)))
        )
      ).next

      val featureInstance = featureInstanceFixedPriceGen(Placement).next
      (userPromocodesService.applyUserMoneyPromocode _)
        .expects(*, *, *)
        .returningZ(featureInstance)
        .anyNumberOfTimes()
      (productScheduleDao.insertIfAbsent _)
        .expects(*)
        .returningT(unit)
        .anyNumberOfTimes()

      (promocoder.changeFeatureCount _)
        .expects(*, *)
        .returningZ(featureInstance)
        .once()

      goodsService.add(transaction, request).success.value
    }

    "create boost schedule for new placement. Used for prolongInterval" in {
      val transaction = paidTransactionGen().next
      val request = productRequestGen(
        product = Placement,
        productPrice = productPriceGen(scheduleFreeBoost = true)
      ).next

      val featureInstance = featureInstanceFixedPriceGen(Placement).next
      (userPromocodesService.applyUserMoneyPromocode _)
        .expects(*, *, *)
        .returningZ(featureInstance)
        .anyNumberOfTimes()
      (promocoder.changeFeatureCount _)
        .expects(*, *)
        .returningZ(featureInstance)
        .anyNumberOfTimes()

      (productScheduleDao.insertIfAbsent _).expects(*).returningT(unit).once()

      goodsService.add(transaction, request).success.value
    }

    "create boost schedule for replaced placement. Used in auto prolongation" in {
      val transactionNewPlacement = paidTransactionGen().next
      val requestNewPlacement = productRequestGen(
        product = Placement,
        productPrice = productPriceGen(scheduleFreeBoost = true)
      ).next

      val placementOldCreation = goodsCreateRequestGen(
        offerId = requestNewPlacement.offer.get,
        product = Placement,
        userId = transactionNewPlacement.user,
        status = Active
      ).next
      goodsDao.insertIfNotExists(placementOldCreation).success.value

      (productScheduleDao.insertIfAbsent _).expects(*).returningT(unit).once()

      val featureInstance = featureInstanceFixedPriceGen(Placement).next
      (userPromocodesService.applyUserMoneyPromocode _)
        .expects(*, *, *)
        .returningZ(featureInstance)
        .anyNumberOfTimes()
      (promocoder.changeFeatureCount _)
        .expects(*, *)
        .returningZ(featureInstance)
        .anyNumberOfTimes()

      goodsService
        .add(transactionNewPlacement, requestNewPlacement)
        .success
        .value
    }

    "try to create boost schedule if good already exists" in {
      val transaction = paidTransactionGen().next
      val request = productRequestGen(
        product = Placement,
        productPrice = productPriceGen(scheduleFreeBoost = true)
      ).next
      val goodRequestCreation = goodsCreateRequestGen(
        offerId = request.offer.get,
        product = Placement,
        userId = transaction.user,
        status = Active,
        transactionId = transaction.transactionId
      ).next

      goodsDao.insertIfNotExists(goodRequestCreation).success

      (productScheduleDao.insertIfAbsent _).expects(*).returningT(unit).once()

      goodsService
        .add(transaction, request)
        .success
    }

  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
