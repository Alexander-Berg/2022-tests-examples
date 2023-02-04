package ru.auto.salesman.service.application_credit

import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.auto.salesman.billing.CampaignsClient
import ru.auto.salesman.client.billing.model.BillingProductType.{
  ApplicationCreditSingleTariffCarsNew,
  ApplicationCreditSingleTariffCarsUsed
}
import ru.auto.salesman.model
import ru.auto.salesman.model.{
  AccountId,
  ActiveProductNaturalKey,
  AdsRequestTypes,
  AgencyId,
  AutoruDealer,
  BalanceClientId,
  CityId,
  ClientId,
  DetailedClient,
  ProductDuration,
  ProductId,
  ProductTariff,
  RegionId
}
import ru.auto.salesman.model.Product.ProductPaymentStatus
import ru.auto.salesman.dao.{BalanceClientDao, ProductDao}
import ru.auto.salesman.model.Product.RawKey
import ru.auto.salesman.service.{
  BillingService,
  DetailedClientSource,
  PriceExtractor,
  ProductsService
}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.products.{ProductsOuterClass => proto}
import ru.auto.salesman.service.application_credit.CreditTariffReadServiceImplSpec.{
  buildActiveProductGen,
  buildProductNaturalKey
}
import ru.auto.salesman.tariffs.CreditTariffs
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.util.money.Money.Kopecks
import ru.yandex.vertis.billing.Model.InactiveReason
import ru.yandex.vertis.scalatest._

import scala.collection.JavaConverters._

class CreditTariffReadServiceImplSpec extends BaseSpec with ProductGenerators {
  private val detailedClientSource = mock[DetailedClientSource]
  private val billingService = mock[BillingService]
  private val campaignsClient = mock[CampaignsClient]
  private val productInfoService = mock[ProductInfoService]
  private val balanceClientDao = mock[BalanceClientDao]

  private val singleTariffService = new CreditSingleTariffReadServiceImpl(
    billingService,
    campaignsClient: CampaignsClient,
    balanceClientDao,
    productInfoService
  )

  private val productsService = mock[ProductsService]
  private val productDao = mock[ProductDao]

  private val accessTariffService =
    new CreditAccessTariffReadServiceImpl(
      productsService,
      productDao,
      productInfoService
    )

  private val creditTariffService = new CreditTariffReadServiceImpl(
    detailedClientSource,
    accessTariffService,
    singleTariffService
  )

  val TestClientId: ClientId = 20101
  val TestAgencyId: Option[AgencyId] = None
  val TestBalanceClientId: BalanceClientId = 2010100
  val TestBalanceAgencyId: Option[BalanceClientId] = None
  val TestAccountId: AccountId = 1

  private val detailedClient = DetailedClient(
    TestClientId,
    TestAgencyId,
    TestBalanceClientId,
    TestBalanceAgencyId,
    None,
    None,
    RegionId(1L),
    CityId(1123L),
    TestAccountId,
    isActive = true,
    firstModerated = true,
    singlePayment = Set(AdsRequestTypes.CarsUsed)
  )

  private val dealer = AutoruDealer("dealer:20101")

  private val accessProductKeyCarsNew =
    RawKey("application-credit", dealer.toString, "cars:new", "access")

  private val accessProductKeyCarsUsed =
    RawKey("application-credit", dealer.toString, "cars:used", "access")

  private val carsNewKey =
    buildProductNaturalKey(dealer.toString, "access", TariffScope.CarsNew)

  private val accessProductInfo = PriceExtractor.ProductInfo(
    ProductId.CreditApplication,
    price = Kopecks(15000L),
    None,
    duration = Some(ProductDuration.days(15)),
    None,
    appliedExperiment = None,
    policyId = None
  )

  private val singleCreditProductInfo = PriceExtractor.ProductInfo(
    ProductId.SingleCreditApplication,
    price = Kopecks(25000),
    None,
    duration = None,
    None,
    appliedExperiment = None,
    policyId = None
  )

  "CreditTariffServiceImpl" should {

    "use right number of tariff scopes" in {
      val productGen =
        buildActiveProductGen(
          carsNewKey,
          prolongable = true,
          ProductPaymentStatus.Active
        )

      forAll(productGen, campaignHeaderGen()) { (accessProduct, campaign) =>
        (productsService.getLastCreatedProduct _)
          .expects(accessProductKeyCarsNew)
          .returningZ(Some(accessProduct))

        (productsService.getLastCreatedProduct _)
          .expects(accessProductKeyCarsUsed)
          .returningZ(None)

        (detailedClientSource.unsafeResolve _)
          .expects(20101: ClientId, false)
          .returningZ(detailedClient)

        (billingService.getCampaign _)
          .expects(detailedClient, ApplicationCreditSingleTariffCarsNew)
          .returningZ(Some(campaign))

        (billingService.getCampaign _)
          .expects(detailedClient, ApplicationCreditSingleTariffCarsUsed)
          .returningZ(None)

        (productInfoService
          .getAccessProductInfo(
            _: ActiveProductNaturalKey,
            _: Option[DateTime]
          ))
          .expects(accessProduct.key, Some(accessProduct.createDate))
          .returningZ(accessProductInfo)

        (productInfoService
          .getAccessProductInfo(
            _: ActiveProductNaturalKey,
            _: Option[DateTime]
          ))
          .expects(
            ActiveProductNaturalKey(accessProductKeyCarsUsed).right.value,
            None
          )
          .returningZ(accessProductInfo)

        (productInfoService
          .getSingleProductInfo(_: DetailedClient, _: ProductTariff))
          .expects(
            detailedClient,
            ProductTariff.ApplicationCreditSingleTariffCarsNew
          )
          .returningZ(singleCreditProductInfo)

        (productInfoService
          .getSingleProductInfo(_: DetailedClient, _: ProductTariff))
          .expects(
            detailedClient,
            ProductTariff.ApplicationCreditSingleTariffCarsUsed
          )
          .returningZ(singleCreditProductInfo)

        creditTariffService
          .getTariffs(dealer)
          .map { tariffs =>
            tariffs.getTariffsList.asScala.size shouldBe 2
          }
          .success
          .value
      }
    }

    "prefer access tariff" in {
      //prolongable = true and status active means that an access tariff is active
      val productGen =
        buildActiveProductGen(
          carsNewKey,
          prolongable = true,
          ProductPaymentStatus.Active
        )

      val campaignGen =
        campaignHeaderGen(inactiveReasonGen = None, isEnabled = true)

      forAll(productGen, campaignGen) { (accessProduct, activeSingleCampaign) =>
        (productsService.getLastCreatedProduct _)
          .expects(accessProductKeyCarsNew)
          .returningZ(Some(accessProduct))

        (productsService.getLastCreatedProduct _)
          .expects(accessProductKeyCarsUsed)
          .returningZ(None)

        (detailedClientSource.unsafeResolve _)
          .expects(20101: ClientId, false)
          .returningZ(detailedClient)

        (billingService.getCampaign _)
          .expects(detailedClient, ApplicationCreditSingleTariffCarsNew)
          .returningZ(Some(activeSingleCampaign))

        (billingService.getCampaign _)
          .expects(detailedClient, ApplicationCreditSingleTariffCarsUsed)
          .returningZ(None)

        (productInfoService
          .getAccessProductInfo(
            _: ActiveProductNaturalKey,
            _: Option[DateTime]
          ))
          .expects(accessProduct.key, Some(accessProduct.createDate))
          .returningZ(accessProductInfo)

        (productInfoService
          .getAccessProductInfo(
            _: ActiveProductNaturalKey,
            _: Option[DateTime]
          ))
          .expects(
            ActiveProductNaturalKey(accessProductKeyCarsUsed).right.value,
            None
          )
          .returningZ(accessProductInfo)

        (productInfoService
          .getSingleProductInfo(_: DetailedClient, _: ProductTariff))
          .expects(
            detailedClient,
            ProductTariff.ApplicationCreditSingleTariffCarsNew
          )
          .returningZ(singleCreditProductInfo)

        (productInfoService
          .getSingleProductInfo(_: DetailedClient, _: ProductTariff))
          .expects(
            detailedClient,
            ProductTariff.ApplicationCreditSingleTariffCarsUsed
          )
          .returningZ(singleCreditProductInfo)

        creditTariffService
          .getTariffs(dealer)
          .map { tariffs =>
            val carsNewTariffs = tariffs.getTariffsList.asScala
              .filter(_.getTariffScope == CreditTariffs.TariffScope.CARS_NEW)
              .loneElement
            val accessTariff = carsNewTariffs.getAccessTariff
            val singleTariff = carsNewTariffs.getSingleTariff

            accessTariff.getStatus shouldBe CreditTariffs.AccessTariff.Status.TURNED_ON
            singleTariff.getStatus shouldBe CreditTariffs.SingleTariff.Status.TURNED_ON_ACTIVE
          }
          .success
          .value
      }
    }

    "access tariff works till end, single tariff next active" in {
      val productGen =
        buildActiveProductGen(
          carsNewKey,
          prolongable = false,
          ProductPaymentStatus.Active
        )

      val campaignGen =
        campaignHeaderGen(inactiveReasonGen = None, isEnabled = true)

      forAll(productGen, campaignGen) { (accessProduct, activeSingleCampaign) =>
        (productsService.getLastCreatedProduct _)
          .expects(accessProductKeyCarsNew)
          .returningZ(Some(accessProduct))

        (productsService.getLastCreatedProduct _)
          .expects(accessProductKeyCarsUsed)
          .returningZ(None)

        (detailedClientSource.unsafeResolve _)
          .expects(20101: ClientId, false)
          .returningZ(detailedClient)

        (billingService.getCampaign _)
          .expects(detailedClient, ApplicationCreditSingleTariffCarsNew)
          .returningZ(Some(activeSingleCampaign))
        (billingService.getCampaign _)
          .expects(detailedClient, ApplicationCreditSingleTariffCarsUsed)
          .returningZ(None)

        (productInfoService
          .getAccessProductInfo(
            _: ActiveProductNaturalKey,
            _: Option[DateTime]
          ))
          .expects(accessProduct.key, Some(accessProduct.createDate))
          .returningZ(accessProductInfo)

        (productInfoService
          .getAccessProductInfo(
            _: ActiveProductNaturalKey,
            _: Option[DateTime]
          ))
          .expects(
            ActiveProductNaturalKey(accessProductKeyCarsUsed).right.value,
            None
          )
          .returningZ(accessProductInfo)

        (productInfoService
          .getSingleProductInfo(_: DetailedClient, _: ProductTariff))
          .expects(
            detailedClient,
            ProductTariff.ApplicationCreditSingleTariffCarsNew
          )
          .returningZ(singleCreditProductInfo)

        (productInfoService
          .getSingleProductInfo(_: DetailedClient, _: ProductTariff))
          .expects(
            detailedClient,
            ProductTariff.ApplicationCreditSingleTariffCarsUsed
          )
          .returningZ(singleCreditProductInfo)

        creditTariffService
          .getTariffs(dealer)
          .map { tariffs =>
            val carsNewTariffs = tariffs.getTariffsList.asScala
              .filter(_.getTariffScope == CreditTariffs.TariffScope.CARS_NEW)
              .loneElement
            val accessTariff = carsNewTariffs.getAccessTariff
            val singleTariff = carsNewTariffs.getSingleTariff

            accessTariff.getStatus shouldBe CreditTariffs.AccessTariff.Status.TURNED_OFF_ACTIVE
            singleTariff.getStatus shouldBe CreditTariffs.SingleTariff.Status.NEXT_ACTIVE
          }
          .success
          .value
      }
    }

    "only access tariff is active" in {
      val productGen =
        buildActiveProductGen(
          carsNewKey,
          prolongable = true,
          ProductPaymentStatus.Active
        )

      val campaignGen = campaignHeaderGen(
        inactiveReasonGen = Some(InactiveReason.MANUALLY_DISABLED),
        isEnabled = false
      )

      forAll(productGen, campaignGen) { (accessProduct, inactiveSingleCampaign) =>
        (productsService.getLastCreatedProduct _)
          .expects(accessProductKeyCarsNew)
          .returningZ(Some(accessProduct))

        (productsService.getLastCreatedProduct _)
          .expects(accessProductKeyCarsUsed)
          .returningZ(None)

        (detailedClientSource.unsafeResolve _)
          .expects(20101: ClientId, false)
          .returningZ(detailedClient)

        (billingService.getCampaign _)
          .expects(detailedClient, ApplicationCreditSingleTariffCarsNew)
          .returningZ(Some(inactiveSingleCampaign))

        (billingService.getCampaign _)
          .expects(detailedClient, ApplicationCreditSingleTariffCarsUsed)
          .returningZ(None)

        (productInfoService
          .getAccessProductInfo(
            _: ActiveProductNaturalKey,
            _: Option[DateTime]
          ))
          .expects(accessProduct.key, Some(accessProduct.createDate))
          .returningZ(accessProductInfo)

        (productInfoService
          .getAccessProductInfo(
            _: ActiveProductNaturalKey,
            _: Option[DateTime]
          ))
          .expects(
            ActiveProductNaturalKey(accessProductKeyCarsUsed).right.value,
            None
          )
          .returningZ(accessProductInfo)

        (productInfoService
          .getSingleProductInfo(_: DetailedClient, _: ProductTariff))
          .expects(
            detailedClient,
            ProductTariff.ApplicationCreditSingleTariffCarsNew
          )
          .returningZ(singleCreditProductInfo)

        (productInfoService
          .getSingleProductInfo(_: DetailedClient, _: ProductTariff))
          .expects(
            detailedClient,
            ProductTariff.ApplicationCreditSingleTariffCarsUsed
          )
          .returningZ(singleCreditProductInfo)

        creditTariffService
          .getTariffs(dealer)
          .map { tariffs =>
            val carsNewTariffs = tariffs.getTariffsList.asScala
              .filter(_.getTariffScope == CreditTariffs.TariffScope.CARS_NEW)
              .loneElement
            val accessTariff = carsNewTariffs.getAccessTariff
            val singleTariff = carsNewTariffs.getSingleTariff

            accessTariff.getStatus shouldBe CreditTariffs.AccessTariff.Status.TURNED_ON
            singleTariff.getStatus shouldBe CreditTariffs.SingleTariff.Status.TURNED_OFF
          }
          .success
          .value
      }
    }

    "only single tariff is active" in {
      val productGen =
        buildActiveProductGen(
          carsNewKey,
          prolongable = true,
          ProductPaymentStatus.Inactive
        )

      val campaignGen =
        campaignHeaderGen(inactiveReasonGen = None, isEnabled = true)

      forAll(productGen, campaignGen) { (accessProduct, activeSingleCampaign) =>
        (productsService.getLastCreatedProduct _)
          .expects(accessProductKeyCarsNew)
          .returningZ(Some(accessProduct))

        (productsService.getLastCreatedProduct _)
          .expects(accessProductKeyCarsUsed)
          .returningZ(None)

        (detailedClientSource.unsafeResolve _)
          .expects(20101: ClientId, false)
          .returningZ(detailedClient)

        (billingService.getCampaign _)
          .expects(detailedClient, ApplicationCreditSingleTariffCarsNew)
          .returningZ(Some(activeSingleCampaign))

        (billingService.getCampaign _)
          .expects(detailedClient, ApplicationCreditSingleTariffCarsUsed)
          .returningZ(None)

        (productInfoService
          .getAccessProductInfo(
            _: ActiveProductNaturalKey,
            _: Option[DateTime]
          ))
          .expects(accessProduct.key, Some(accessProduct.createDate))
          .returningZ(accessProductInfo)

        (productInfoService
          .getAccessProductInfo(
            _: ActiveProductNaturalKey,
            _: Option[DateTime]
          ))
          .expects(
            ActiveProductNaturalKey(accessProductKeyCarsUsed).right.value,
            None
          )
          .returningZ(accessProductInfo)

        (productInfoService
          .getSingleProductInfo(_: DetailedClient, _: ProductTariff))
          .expects(
            detailedClient,
            ProductTariff.ApplicationCreditSingleTariffCarsNew
          )
          .returningZ(singleCreditProductInfo)

        (productInfoService
          .getSingleProductInfo(_: DetailedClient, _: ProductTariff))
          .expects(
            detailedClient,
            ProductTariff.ApplicationCreditSingleTariffCarsUsed
          )
          .returningZ(singleCreditProductInfo)

        creditTariffService
          .getTariffs(dealer)
          .map { tariffs =>
            val carsNewTariffs = tariffs.getTariffsList.asScala
              .filter(_.getTariffScope == CreditTariffs.TariffScope.CARS_NEW)
              .loneElement
            val accessTariff = carsNewTariffs.getAccessTariff
            val singleTariff = carsNewTariffs.getSingleTariff

            accessTariff.getStatus shouldBe CreditTariffs.AccessTariff.Status.TURNED_OFF_INACTIVE
            singleTariff.getStatus shouldBe CreditTariffs.SingleTariff.Status.TURNED_ON_ACTIVE
          }
          .success
          .value
      }
    }

    "both inactive" in {
      val productGen =
        buildActiveProductGen(
          carsNewKey,
          prolongable = true,
          ProductPaymentStatus.Inactive
        )

      val campaignGen = campaignHeaderGen(
        inactiveReasonGen = Some(InactiveReason.MANUALLY_DISABLED),
        isEnabled = false
      )

      forAll(productGen, campaignGen) { (accessProduct, inactiveSingleCampaign) =>
        (productsService.getLastCreatedProduct _)
          .expects(accessProductKeyCarsNew)
          .returningZ(Some(accessProduct))

        (productsService.getLastCreatedProduct _)
          .expects(accessProductKeyCarsUsed)
          .returningZ(None)

        (detailedClientSource.unsafeResolve _)
          .expects(20101: ClientId, false)
          .returningZ(detailedClient)

        (billingService.getCampaign _)
          .expects(detailedClient, ApplicationCreditSingleTariffCarsNew)
          .returningZ(Some(inactiveSingleCampaign))

        (billingService.getCampaign _)
          .expects(detailedClient, ApplicationCreditSingleTariffCarsUsed)
          .returningZ(None)

        (productInfoService
          .getAccessProductInfo(
            _: ActiveProductNaturalKey,
            _: Option[DateTime]
          ))
          .expects(accessProduct.key, Some(accessProduct.createDate))
          .returningZ(accessProductInfo)

        (productInfoService
          .getAccessProductInfo(
            _: ActiveProductNaturalKey,
            _: Option[DateTime]
          ))
          .expects(
            ActiveProductNaturalKey(accessProductKeyCarsUsed).right.value,
            None
          )
          .returningZ(accessProductInfo)

        (productInfoService
          .getSingleProductInfo(_: DetailedClient, _: ProductTariff))
          .expects(
            detailedClient,
            ProductTariff.ApplicationCreditSingleTariffCarsNew
          )
          .returningZ(singleCreditProductInfo)

        (productInfoService
          .getSingleProductInfo(_: DetailedClient, _: ProductTariff))
          .expects(
            detailedClient,
            ProductTariff.ApplicationCreditSingleTariffCarsUsed
          )
          .returningZ(singleCreditProductInfo)

        creditTariffService
          .getTariffs(dealer)
          .map { tariffs =>
            val carsNewTariffs = tariffs.getTariffsList.asScala
              .filter(_.getTariffScope == CreditTariffs.TariffScope.CARS_NEW)
              .loneElement
            val accessTariff = carsNewTariffs.getAccessTariff
            val singleTariff = carsNewTariffs.getSingleTariff

            accessTariff.getStatus shouldBe CreditTariffs.AccessTariff.Status.TURNED_OFF_INACTIVE
            singleTariff.getStatus shouldBe CreditTariffs.SingleTariff.Status.TURNED_OFF
          }
          .success
          .value
      }
    }

    "single tariff TURNED_ON_INACTIVE" in {
      val productGen =
        buildActiveProductGen(
          carsNewKey,
          prolongable = true,
          ProductPaymentStatus.Inactive
        )

      val campaignGen = campaignHeaderGen(
        inactiveReasonGen = Some(InactiveReason.NO_ENOUGH_FUNDS),
        isEnabled = true
      )

      forAll(productGen, campaignGen) { (accessProduct, activeSingleCampaign) =>
        (productsService.getLastCreatedProduct _)
          .expects(accessProductKeyCarsNew)
          .returningZ(Some(accessProduct))

        (productsService.getLastCreatedProduct _)
          .expects(accessProductKeyCarsUsed)
          .returningZ(None)

        (detailedClientSource.unsafeResolve _)
          .expects(20101: ClientId, false)
          .returningZ(detailedClient)

        (billingService.getCampaign _)
          .expects(detailedClient, ApplicationCreditSingleTariffCarsNew)
          .returningZ(Some(activeSingleCampaign))

        (billingService.getCampaign _)
          .expects(detailedClient, ApplicationCreditSingleTariffCarsUsed)
          .returningZ(None)

        (productInfoService
          .getAccessProductInfo(
            _: ActiveProductNaturalKey,
            _: Option[DateTime]
          ))
          .expects(accessProduct.key, Some(accessProduct.createDate))
          .returningZ(accessProductInfo)

        (productInfoService
          .getAccessProductInfo(
            _: ActiveProductNaturalKey,
            _: Option[DateTime]
          ))
          .expects(
            ActiveProductNaturalKey(accessProductKeyCarsUsed).right.value,
            None
          )
          .returningZ(accessProductInfo)

        (productInfoService
          .getSingleProductInfo(_: DetailedClient, _: ProductTariff))
          .expects(
            detailedClient,
            ProductTariff.ApplicationCreditSingleTariffCarsNew
          )
          .returningZ(singleCreditProductInfo)

        (productInfoService
          .getSingleProductInfo(_: DetailedClient, _: ProductTariff))
          .expects(
            detailedClient,
            ProductTariff.ApplicationCreditSingleTariffCarsUsed
          )
          .returningZ(singleCreditProductInfo)

        creditTariffService
          .getTariffs(dealer)
          .map { tariffs =>
            val carsNewTariffs = tariffs.getTariffsList.asScala
              .filter(_.getTariffScope == CreditTariffs.TariffScope.CARS_NEW)
              .loneElement
            val accessTariff = carsNewTariffs.getAccessTariff
            val singleTariff = carsNewTariffs.getSingleTariff

            accessTariff.getStatus shouldBe CreditTariffs.AccessTariff.Status.TURNED_OFF_INACTIVE
            singleTariff.getStatus shouldBe CreditTariffs.SingleTariff.Status.TURNED_ON_INACTIVE
          }
          .success
          .value
      }
    }
  }

  "CreditTariffServiceImpl getDealersActiveApplicationCreditInfo" should {
    val allAccessTariffService = mock[CreditAccessTariffReadService]
    val allSingleTariffService =
      mock[CreditSingleTariffReadService]

    val allCreditTariffService = new CreditTariffReadServiceImpl(
      detailedClientSource = detailedClientSource,
      accessTariffService = allAccessTariffService,
      singleTariffService = allSingleTariffService
    )

    "return all tariff info without single if access active" in {
      val accessDealers = GroupedDealers(
        dealersWithCarsNew = Set(AutoruDealer(1), AutoruDealer(2)),
        dealersWithCarsUsed = Set(AutoruDealer(3), AutoruDealer(4))
      )
      val singleDealers = GroupedDealers(
        dealersWithCarsNew = Set(AutoruDealer(1), AutoruDealer(5)),
        dealersWithCarsUsed = Set(AutoruDealer(6))
      )

      (allAccessTariffService.dealers _).expects().returningZ(accessDealers)

      (allSingleTariffService.dealers _).expects().returningZ(singleDealers)

      val expectedResult =
        ApplicationCreditSnapshot(
          singleCredit = singleDealers.copy(dealersWithCarsNew = Set(AutoruDealer(5))),
          accessCredit = accessDealers
        )

      val result =
        allCreditTariffService.getApplicationCreditSnapshot.success.value

      result shouldEqual expectedResult

    }
  }
}

object CreditTariffReadServiceImplSpec extends ProductGenerators with BetterEitherValues {

  def buildProductNaturalKey(
      payer: String,
      productType: String,
      scope: TariffScope
  ): proto.ActiveProductNaturalKey =
    proto.ActiveProductNaturalKey.newBuilder
      .setDomain("application-credit")
      .setPayer(payer)
      .setProductType(productType)
      .setTarget(scope.toProductTarget)
      .build

  def buildActiveProductGen(
      key: proto.ActiveProductNaturalKey,
      prolongable: Boolean,
      status: ProductPaymentStatus
  ): Gen[model.Product] =
    activeProductGen.map { product =>
      product.copy(
        key = ActiveProductNaturalKey(key).right.value,
        prolongable = prolongable,
        status = status
      )
    }

}
