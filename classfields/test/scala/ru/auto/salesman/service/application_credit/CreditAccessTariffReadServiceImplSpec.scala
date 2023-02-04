package ru.auto.salesman.service.application_credit

import com.google.protobuf.util.Timestamps
import org.joda.time.DateTime
import ru.auto.salesman.Task
import ru.auto.salesman.model.{
  ActiveProductNaturalKey,
  Product,
  ProductDuration,
  ProductId
}
import ru.auto.salesman.products.ProductsOuterClass
import ru.auto.salesman.service.application_credit.CreditAccessTariffReadServiceImpl.{
  toAccessTariff,
  toRawKey
}
import ru.auto.salesman.service.application_credit.TariffScope.CarsNew
import ru.auto.salesman.service.PriceExtractor
import ru.yandex.vertis.scalatest._
import ru.auto.salesman.dao.impl.jdbc.JdbcProductDao
import ru.auto.salesman.model.AutoruDealer
import ru.auto.salesman.service.PriceExtractor.ProductInfo
import ru.auto.salesman.service.ProductsService
import ru.auto.salesman.service.application_credit.TariffScope._
import ru.auto.salesman.tariffs.CreditTariffs.AccessTariff.Status._
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.ProductGenerators
import ru.auto.salesman.test.template.SalesmanJdbcSpecTemplate
import ru.auto.salesman.util.money.Money.Kopecks
import zio.ZIO.foreach_

class CreditAccessTariffReadServiceImplSpec
    extends BaseSpec
    with ProductGenerators
    with BetterEitherValues
    with SalesmanJdbcSpecTemplate {
  private val productDao = new JdbcProductDao(database, database)
  private val productsService = new ProductsService(productDao)
  private val productInfoService = mock[ProductInfoService]

  private val service =
    new CreditAccessTariffReadServiceImpl(
      productsService,
      productDao,
      productInfoService
    )

  private val opsService =
    new CreditAccessTariffWriteServiceImpl(productsService)

  private val dealer1 = AutoruDealer("dealer:123")
  private val dealer2 = AutoruDealer("dealer:1234")

  private val productInfo = PriceExtractor.ProductInfo(
    ProductId.CreditApplication,
    Kopecks(15000L),
    None,
    Some(ProductDuration.days(15)),
    None,
    appliedExperiment = None,
    policyId = None
  )

  private def simulatePriceService(
      dealer: AutoruDealer,
      tariff: TariffScope,
      productInfo: ProductInfo = productInfo
  ): Task[Unit] =
    productsService
      .getLastCreatedProduct(toRawKey(dealer, tariff))
      .map { product =>
        (productInfoService
          .getAccessProductInfo(
            _: ActiveProductNaturalKey,
            _: Option[DateTime]
          ))
          .expects(product.get.key, Some(product.get.createDate))
          .returningZ(productInfo)
      }

  private val simulateBilling =
    productDao.getWaitingForPayment.flatMap { products =>
      foreach_(products) { product =>
        productDao.activate(product.id, product.createDate.plusDays(7))
      }
    }

  private val activeProductCarsNewNaturalKey =
    ProductsOuterClass.ActiveProductNaturalKey.newBuilder
      .setDomain("application-credit")
      .setPayer(dealer1.toString)
      .setProductType("access")
      .setTarget(TariffScope.CarsNew.toProductTarget)
      .build

  private val activeProductCarsNewGen =
    ActiveProductNaturalKey(activeProductCarsNewNaturalKey)
      .map(key => activeProductGen.map(_.copy(key = key)))
      .right
      .value

  "CreditAccessTariffServiceImpl" should {

    "on activate request, create tariff in TURNING_ON state" in {
      for {
        _ <- opsService.turnOnTariff(dealer1, CarsNew)
        _ <- simulatePriceService(dealer1, CarsNew)
        tariff <- service.getTariff(dealer1, CarsNew)
      } yield tariff.getStatus shouldBe TURNING_ON
    }.success

    "on deactivate request, turn tariff off but leave it active" in {
      for {
        _ <- opsService.turnOnTariff(dealer1, CarsNew)
        // Пока тариф не оплачен, не получится деактивировать его (для простоты
        // реализации - избегаем гонок с процессом обилливания тарифов).
        _ <- simulateBilling
        _ <- opsService.turnOffTariff(dealer1, CarsNew)
        _ <- simulatePriceService(dealer1, CarsNew)
        tariff <- service.getTariff(dealer1, CarsNew)
      } yield tariff.getStatus shouldBe TURNED_OFF_ACTIVE
    }.success

    "on activate request of tariff which was deactivated earlier, but not became inactive yet, activate it" in {
      for {
        _ <- opsService.turnOnTariff(dealer1, CarsNew)
        _ <- simulateBilling
        _ <- opsService.turnOffTariff(dealer1, CarsNew)
        _ <- opsService.turnOnTariff(dealer1, CarsNew)
        _ <- simulatePriceService(dealer1, CarsNew)
        tariff <- service.getTariff(dealer1, CarsNew)
      } yield tariff.getStatus shouldBe TURNED_ON
    }.success

    "not return dealers if tariff isn't paid" in {
      for {
        _ <- opsService.turnOnTariff(dealer1, CarsNew)
        result <- service.dealers
      } yield
        withClue(result) {
          result.dealersWithCarsNew shouldBe Set()
          result.dealersWithCarsUsed shouldBe Set()
        }
    }.success

    "return dealers with active access tariffs partitioned by scopes" in {
      for {
        _ <- opsService.turnOnTariff(dealer1, CarsNew)
        _ <- opsService.turnOnTariff(dealer2, CarsUsed)
        _ <- simulateBilling
        result <- service.dealers
      } yield
        withClue(result) {
          result.dealersWithCarsNew shouldBe Set(dealer1)
          result.dealersWithCarsUsed shouldBe Set(dealer2)
        }
    }.success

    "get tariff with price and duration" in {
      val price = Kopecks(1000000L)
      val duration = ProductDuration.days(10)

      for {
        _ <- opsService.turnOnTariff(dealer1, CarsNew)
        _ <- simulatePriceService(
          dealer1,
          CarsNew,
          productInfo.copy(price = price, duration = Some(duration))
        )
        tariff <- service.getTariff(dealer1, CarsNew)
      } yield
        withClue(tariff) {
          tariff.getBasePriceRubles shouldBe 10000
          tariff.getDuration shouldBe duration.days.getDays
        }
    }.success

    "set deadline = product's expire date if product has expire date (usually if it's active)" in {
      val expireDate = DateTime.now().minusDays(1)
      val deadline = Timestamps.fromMillis(expireDate.getMillis)
      val productGen = activeProductCarsNewGen.map(
        _.copy(
          status = Product.ProductPaymentStatus.Active,
          expireDate = Some(expireDate)
        )
      )
      forAll(productGen) { product =>
        toAccessTariff(
          product,
          Kopecks(0),
          Some(ProductDuration.days(15))
        ).getDeadline shouldBe deadline
      }
    }

    "not set deadline if product doesn't have expire date (usually if it isn't active yet)" in {
      val productGen = activeProductCarsNewGen.map(
        _.copy(
          status = Product.ProductPaymentStatus.NeedPayment,
          expireDate = None
        )
      )
      forAll(productGen) { product =>
        toAccessTariff(
          product,
          Kopecks(0),
          Some(ProductDuration.days(15))
        ).hasDeadline shouldBe false
      }
    }
  }
}
