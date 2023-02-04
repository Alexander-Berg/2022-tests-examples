package ru.auto.salesman.service.impl.user

import org.joda.time.DateTime
import ru.auto.salesman._
import ru.auto.salesman.dao.user.{BundleDao, GoodsDao}
import ru.auto.salesman.model.ProductStatuses.Active
import ru.auto.salesman.model.user.product.ProductProvider.AutoruBundles.{Turbo, Vip}
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.{
  Color,
  Special,
  Top
}
import ru.auto.salesman.model.{
  AutoruUser,
  DeprecatedDomain,
  DeprecatedDomains,
  ProductStatuses,
  PromocoderUser
}
import ru.auto.salesman.service.impl.user.BundleServiceImpl.goodsContext
import ru.auto.salesman.service.user.UserFeatureService
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.{ServiceModelGenerators, UserModelGenerators}
import zio.{Ref, UIO}

class BundleServiceImplSpec
    extends BaseSpec
    with UserModelGenerators
    with ServiceModelGenerators {

  private val bundleDao = new DummyBundleDao
  private val goodsService = new DummyGoodsService
  private val promocoder = new DummyPromocoderClient

  private val vipBoostUsingCustomPriceEnabledValue = Ref.make(false).unsafeRun()

  private val userFeatureService = new UserFeatureService {
    def cashbackFullPaymentRestrictionEnabled: Boolean = ???
    def vinDecoderSttpClientEnabled: Boolean = ???
    def bestServicePriceEnabled: Boolean = ???
    def increasePlacementByVipReleaseDateFeature: DateTime = ???
    def prolongationFailedPushNotificationEnabled: Boolean = ???

    def prolongationFailedSmsNotificationEnabled: Boolean = ???

    def prolongationFailedEmailNotificationEnabled: Boolean = ???

    def enableAsyncSendingProlongationFailedNotification: Boolean = ???

    def requestPriceFromVosMasterEnabled: Boolean = ???

    def userQuotaModerationEventsEnabled: Boolean = ???

    def statisticLogBrokerEnabled: Boolean = ???

    def statisticLogBrokerSyncLogEnabled: Boolean = ???

    def useTrustGate: Boolean = ???

    def callCarfaxForReportPrice: Boolean = ???

    def usePriceFromCarfaxForReportPrice: Boolean = ???

    def useNewRecurrentPaymentWay: Boolean = ???

    def useTrustForScheduledPayments: Boolean = ???

    override val vipBoostUsingCustomPriceEnabled: UIO[Boolean] =
      vipBoostUsingCustomPriceEnabledValue.get
  }

  private val bundleService =
    new BundleServiceImpl(
      bundleDao,
      goodsService,
      promocoder,
      userFeatureService
    )

  "BundleService.add()" should {

    "save bundle into storage" in {
      forAll(paidTransactionGen(), productRequestGen(Turbo)) {
        (transaction, productRequest) =>
          bundleService.add(transaction, productRequest).success
          val result = getSavedBundles().head

          import transaction.{transactionId, user}
          result.offer shouldBe productRequest.offer.value
          result.user shouldBe user
          result.product shouldBe Turbo
          result.amount shouldBe productRequest.amount
          result.status shouldBe Active
          result.transactionId shouldBe transactionId
          result.context shouldBe productRequest.context

          clean()
      }
    }

    "save goods which bundle contains, into storage" in {
      forAll(paidTransactionGen(), productRequestGen(Turbo)) {
        (transaction, productRequest) =>
          bundleService.add(transaction, productRequest).success
          val result = getSavedGoods()

          result.map(_.product) should contain theSameElementsAs List(
            Color,
            Special,
            Top
          )
          val offer = productRequest.offer.value
          import transaction.{transactionId, user}
          result.map(_.offer) shouldBe List(offer, offer, offer)
          result.map(_.user) shouldBe List(user, user, user)
          result.map(_.amount) shouldBe List(0, 0, 0)
          result.map(_.status) shouldBe List(Active, Active, Active)
          result.map(_.transactionId) shouldBe List(
            transactionId,
            transactionId,
            transactionId
          )

          clean()
      }
    }

    "create promocoder feature for vip boost when vip-boost-using-custom-price-enabled feature is turned off" in {
      forAll(paidTransactionGen(), productRequestGen(Vip)) {
        (transaction, productRequest) =>
          {
            for {
              _ <- promocoder.clean
              _ <- vipBoostUsingCustomPriceEnabledValue.set(false)
              _ <- bundleService.add(transaction, productRequest)
              features <- promocoder.getFeatures(
                PromocoderUser(AutoruUser(transaction.user))
              )
            } yield features.loneElement.tag shouldBe "boost"
          }.success
      }
    }

    "not create promocoder feature for vip boost when vip-boost-using-custom-price-enabled feature is turned on" in {
      forAll(paidTransactionGen(), productRequestGen(Vip)) {
        (transaction, productRequest) =>
          {
            for {
              _ <- vipBoostUsingCustomPriceEnabledValue.set(true)
              _ <- promocoder.clean
              _ <- bundleService.add(transaction, productRequest)
              features <- promocoder.getFeatures(
                PromocoderUser(AutoruUser(transaction.user))
              )
            } yield features shouldBe empty
          }.success
      }
    }
  }

  "BundleService.deactivate" should {

    "delete promocoder features for vip boost on refund request when vip-boost-using-custom-price-enabled feature is turned off" in {
      forAll(paidTransactionGen(), productRequestGen(Vip)) {
        (transaction, productRequest) =>
          {
            for {
              _ <- promocoder.clean
              _ <- vipBoostUsingCustomPriceEnabledValue.set(false)
              bundle <- bundleService.add(transaction, productRequest)
              _ <- bundleService.deactivate(
                transaction,
                List(bundle),
                ProductStatuses.Canceled
              )
              features <- promocoder.getFeatures(
                PromocoderUser(AutoruUser(transaction.user))
              )
            } yield features shouldBe empty
          }.success
      }
    }

    "not touch promocoder features for vip boost on refund request when vip-boost-using-custom-price-enabled feature is turned on" in {
      forAll(paidTransactionGen(), productRequestGen(Vip)) {
        (transaction, productRequest) =>
          {
            for {
              _ <- promocoder.clean
              _ <- vipBoostUsingCustomPriceEnabledValue.set(false)
              bundle <- bundleService.add(transaction, productRequest)
              _ <- vipBoostUsingCustomPriceEnabledValue.set(true)
              _ <- bundleService.deactivate(
                transaction,
                List(bundle),
                ProductStatuses.Canceled
              )
              features <- promocoder.getFeatures(
                PromocoderUser(AutoruUser(transaction.user))
              )
            } yield features should not be empty
          }.success
      }
    }
  }

  "goodsContext creator" should {

    // Раньше при basePrice = 0 фоллбечились на получение цены из мойши.
    // При этом пользователи могут включать автопродление услуги из пакета (это
    // баг: https://st.yandex-team.ru/VSMONEY-1061). Это приводило к двойной
    // оплате: списывали деньги за автопродление всего пакета, а также за
    // автопродление услуги из этого пакета.
    // Чтобы избежать этого, при автопродлении запрещаем оплату продления услуг
    // с basePrice = 0.
    // В тесте проверяем необходимый для этого запрета констрейнт: в контексте
    // услуги из пакета basePrice = 0.
    "create context with basePrice = 0" in {
      forAll(BundleUnitGen, readableString, productDurationGen) {
        (bundleUnit, bundleId, bundleDuration) =>
          val result = goodsContext(bundleUnit, bundleId, bundleDuration)
          result.productPrice.price.basePrice shouldBe 0
      }
    }
  }

  private val epoch = DateTime.parse("1970-01-01T00:00+00:00")

  private def getSavedBundles() =
    bundleDao.get(BundleDao.Filter.ChangedSince(epoch)).success.value

  private def getSavedGoods() =
    goodsService.get(GoodsDao.Filter.ChangedSince(epoch)).success.value

  private def clean(): Unit = {
    bundleDao.clean()
    goodsService.clean()
  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
