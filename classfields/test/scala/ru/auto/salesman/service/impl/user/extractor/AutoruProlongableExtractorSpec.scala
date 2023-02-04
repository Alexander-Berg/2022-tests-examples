package ru.auto.salesman.service.impl.user.extractor

import ru.auto.api.ApiOfferModel.{Category, Offer}
import ru.auto.salesman.client.PromocoderClient
import ru.auto.salesman.dao.impl.jdbc.user.{
  JdbcBundleDao,
  JdbcGoodsDao,
  JdbcProductScheduleDao
}
import ru.auto.salesman.model._
import ru.auto.salesman.model.offer.{AutoruOfferId, OfferIdentity}
import ru.auto.salesman.model.user.Prolongable
import ru.auto.salesman.model.user.UserTariff.ShortPlacement
import ru.auto.salesman.model.user.product.ProductProvider.AutoruBundles.Turbo
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods._
import ru.auto.salesman.service.impl.user.goods.BoostScheduler
import ru.auto.salesman.service.impl.user.{BundleServiceImpl, GoodsServiceImpl}
import ru.auto.salesman.service.user.{UserFeatureService, UserPromocodesService}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.OfferModelGenerators
import ru.auto.salesman.test.model.gens.user.{UserDaoGenerators, UserModelGenerators}
import ru.auto.salesman.test.template.SalesmanUserJdbcSpecTemplate

class AutoruProlongableExtractorSpec
    extends BaseSpec
    with UserModelGenerators
    with OfferModelGenerators
    with UserDaoGenerators
    with SalesmanUserJdbcSpecTemplate {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  private val boost = AutoruGoods.Boost
  private val placement = AutoruGoods.Placement

  private val userId = "user:47384322"
  private val autoruUser = AutoruUser(userId)
  private val offerId = AutoruOfferId("1090438490-abbdff")
  private val transactionId = "transaction-id"

  private val transaction =
    paidTransactionGen().next.copy(transactionId = transactionId, user = userId)

  private val promocoderClient = mock[PromocoderClient]
  (promocoderClient.changeFeatureCount _)
    .expects(*, *)
    .returningZ(featureInstanceGen.next)
    .anyNumberOfTimes()
  private val userPromocodesService = mock[UserPromocodesService]

  private val goodsDao = new JdbcGoodsDao(database)
  private val bundleDao = new JdbcBundleDao(database)
  private val productScheduleDao = new JdbcProductScheduleDao(database)
  private val boostScheduler = new BoostScheduler(productScheduleDao)

  private val userFeatureService = stub[UserFeatureService]
  (userFeatureService.cashbackFullPaymentRestrictionEnabled _)
    .when()
    .returning(true)
    .anyNumberOfTimes()

  private val goodsService =
    new GoodsServiceImpl(
      goodsDao,
      bundleDao,
      promocoderClient,
      userPromocodesService,
      boostScheduler,
      userFeatureService
    )

  private val bundleService = new BundleServiceImpl(
    new JdbcBundleDao(database),
    goodsService,
    promocoderClient,
    userFeatureService
  )

  private val extractor = new AutoruProlongableExtractor(goodsDao)

  "AutoruProlongableExtractor" should {

    "extract prolongationForced field from Placement and prolongationAllowed=true" in {
      extractor
        .prolongationForced(Prolongable(true), placement)
        .success
        .value shouldBe Prolongable(true)
    }

    "extract prolongationForced field from Placement and prolongationAllowed=false" in {
      extractor
        .prolongationForced(Prolongable(false), placement)
        .success
        .value shouldBe Prolongable(false)
    }

    "extract prolongationForced field from Boost and prolongationAllowed=true" in {
      extractor
        .prolongationForced(Prolongable(true), boost)
        .success
        .value shouldBe Prolongable(false)
    }

    "extract prolongationForced field from Boost and prolongationAllowed=false" in {
      extractor
        .prolongationForced(Prolongable(false), boost)
        .success
        .value shouldBe Prolongable(false)
    }

    "return prolongationAllowed = true if product is autoprolongable package" in {
      extractor
        .prolongationAllowed(
          Turbo,
          Some(autoruUser),
          Some(offerId),
          tariff = None
        )
        .success
        .value shouldBe Prolongable(true)
    }

    "return prolongationAllowed = true if product is autoprolongable and isn't activated" in {
      extractor
        .prolongationAllowed(
          Turbo,
          Some(autoruUser),
          Some(offerId),
          tariff = None
        )
        .success
        .value shouldBe Prolongable(true)
    }

    "return prolongationAllowed = true if product is autoprolongable and user is anonymous" in {
      extractor
        .prolongationAllowed(Top, user = None, offerId = None, tariff = None)
        .success
        .value shouldBe Prolongable(true)
    }

    "return prolongationAllowed = true if product is autoprolongable and is activated as single good, without a package" in {
      val goodsCreateRequest =
        productRequestGen(
          product = Top,
          Some(offerId),
          selfActivatedProductPriceGen
        ).next
      goodsService.add(transaction, goodsCreateRequest).success
      extractor
        .prolongationAllowed(
          Turbo,
          Some(autoruUser),
          Some(offerId),
          tariff = None
        )
        .success
        .value shouldBe Prolongable(true)
    }

    "return prolongationAllowed = false if product is activated as a part of package" in {
      val bundleCreateRequest =
        productRequestGen(product = Turbo, Some(offerId)).next
      bundleService.add(transaction, bundleCreateRequest).success
      extractor
        .prolongationAllowed(
          Top,
          Some(autoruUser),
          Some(offerId),
          tariff = None
        )
        .success
        .value shouldBe Prolongable(false)
    }

    "return prolongationAllowed = false if product isn't autoprolongable" in {
      extractor
        .prolongationAllowed(
          Color,
          Some(autoruUser),
          Some(offerId),
          tariff = None
        )
        .success
        .value shouldBe Prolongable(false)
    }

    "return prolongationAllowed = true if product = Placement && tariff = ShortPlacement" in {
      extractor
        .prolongationAllowed(
          Placement,
          Some(autoruUser),
          Some(offerId),
          Some(ShortPlacement)
        )
        .success
        .value shouldBe Prolongable(true)
    }

    "return prolongationAllowed = true if product = Placement && tariff != ShortPlacement" in {
      extractor
        .prolongationAllowed(
          Placement,
          Some(autoruUser),
          Some(offerId),
          tariff = None
        )
        .success
        .value shouldBe Prolongable(false)
    }

    "return prolongationForcedNotTogglable = true if product = Placement && tariff = ShortPlacement && vip isn't active" in {
      extractor
        .prolongationForcedNotTogglable(
          Placement,
          Some(ShortPlacement),
          Some(Offer.getDefaultInstance)
        )
        .success
        .value shouldBe Prolongable(true)
    }

    "return prolongationForcedNotTogglable = false if product = Placement && tariff = ShortPlacement && vip is active" in {
      val b = Offer.newBuilder()
      b.addServicesBuilder().setService("package_vip").setIsActive(true)
      val offer = b.build()
      extractor
        .prolongationForcedNotTogglable(
          Placement,
          Some(ShortPlacement),
          Some(offer)
        )
        .success
        .value shouldBe Prolongable(false)
    }

    "return prolongationForcedNotTogglable = false if product = Placement && tariff != ShortPlacement" in {
      extractor
        .prolongationForcedNotTogglable(
          Placement,
          tariff = None,
          Some(Offer.getDefaultInstance)
        )
        .success
        .value shouldBe Prolongable(false)
    }

    "return prolongationForcedNotTogglable = false if product != Placement" in {
      extractor
        .prolongationForcedNotTogglable(
          Turbo,
          tariff = None,
          Some(Offer.getDefaultInstance)
        )
        .success
        .value shouldBe Prolongable(false)
    }
  }

  def buildOffer(
      offer: Offer,
      offerId: OfferIdentity,
      user: UserId,
      geoId: RegionId
  ): Offer = {
    val offerBuilder = offer.toBuilder
    val sellerBuilder = offerBuilder.getSellerBuilder
    val locationBuilder = sellerBuilder.getLocationBuilder

    offerBuilder
      .setId(offerId.toString)
      .setUserRef(user)
      .setSeller {
        sellerBuilder
          .setLocation {
            locationBuilder
              .setGeobaseId(geoId)
              .build()
          }
          .build()
      }
      .setCategory(Category.CARS)
      .build()
  }

}
