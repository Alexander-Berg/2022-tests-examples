package ru.yandex.realty.seller.processing.products

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.abram.Tariff
import ru.yandex.realty.clients.vos.ng.VosClientNG
import ru.yandex.realty.model.offer.PaymentType
import ru.yandex.realty.model.user.UserRef
import ru.yandex.realty.seller.dao.{PurchaseDao, PurchasedProductDao, RenewalStateDao}
import ru.yandex.realty.seller.model.ProductType
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.product._
import ru.yandex.realty.seller.model.purchase.{Purchase, PurchaseFilter}
import ru.yandex.realty.seller.processing.purchases.renewal.{
  RenewalActualityLogic,
  RenewalActualityVerdict,
  ShouldBeRenewed
}
import ru.yandex.realty.seller.service.PriceService.GetContextRequest
import ru.yandex.realty.seller.service.{PriceService, RenewalEventProducer}
import ru.yandex.realty.seller.service.builders.impl.PurchaseWithProductsBuilderImpl
import ru.yandex.realty.seller.service.util.RenewalUtils
import ru.yandex.realty.vos.model.user.{ExtendedUserType, User}
import ru.yandex.realty.watching.ProcessingState
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class CreateRenewedProductStageSpec
  extends AsyncSpecBase
  with SellerModelGenerators
  with PropertyChecks
  with RenewalUtils {

  private val renewalStateDao = mock[RenewalStateDao]
  private val productDao = mock[PurchasedProductDao]
  private val purchaseDao: PurchaseDao = mock[PurchaseDao]
  private val priceService: PriceService = mock[PriceService]
  private val renewalActualityCheckLogic: RenewalActualityLogic = mock[RenewalActualityLogic]
  private val renewalEventProducer: RenewalEventProducer = mock[RenewalEventProducer]
  private val vosClient: VosClientNG = mock[VosClientNG]
  implicit val traced: Traced = Traced.empty
  private val VasPurchasedProductGen =
    for {
      p <- purchasedProductGen
      purchaseId <- Gen.some(sellerStrIdGen)
      priceContext <- Gen.some(priceContextGen)
      endTime <- dateTimeInPast
      context = p.context.copy(paymentType = Some(PaymentType.NATURAL_PERSON))
    } yield p.copy(
      status = PurchasedProductStatuses.Active,
      endTime = Some(endTime),
      context = context,
      source = ManualSource,
      priceContext = priceContext,
      purchaseId = purchaseId
    )

  private val PlacementPurchasedProductGen =
    VasPurchasedProductGen
      .map(p => p.copy(product = ProductTypes.Placement))

  val stage = new CreateRenewedProductStage(
    renewalStateDao = renewalStateDao,
    productDao = productDao,
    purchaseDao = purchaseDao,
    purchaseBuilder = new PurchaseWithProductsBuilderImpl(priceService),
    priceService = priceService,
    renewalLogic = renewalActualityCheckLogic,
    renewalEventProducer = renewalEventProducer,
    vosClient = vosClient
  )

  "CreateRenewedProductStage" should {

    "create new product using purchase price for VAS products(except placement)" in {
      forAll(VasPurchasedProductGen, purchaseGen) { (purchasedProduct, purchase) =>
        mockRenewalLogic(purchasedProduct, verdict = ShouldBeRenewed)
        mockPurchaseDaoForCurrentProduct(purchasedProduct, returningPurchase = purchase)
        mockPurchaseDaoForPreviousPurchases(returningPurchases = Seq.empty)
        val previousPurchasePrice = purchasedProduct.priceContext.map(_.basePrice).get

        checkCreatedPurchasedProduct(purchasedProduct.product, price = previousPurchasePrice)
        val state = ProcessingState(purchasedProduct)
        stage.process(state).futureValue // run stage
      }
    }

    "create new product using actual price for a placement product" in {
      forAll(PlacementPurchasedProductGen, purchaseGen) { (purchasedProduct, purchase) =>
        (vosClient
          .getUser(_: String, _: Boolean, _: Iterable[User.Feature])(_: Traced))
          .expects(*, *, *, *)
          .returning(Future.successful(Some(User.newBuilder().build())))

        mockRenewalLogic(purchasedProduct, verdict = ShouldBeRenewed)
        mockPurchaseDaoForCurrentProduct(purchasedProduct, returningPurchase = purchase)
        mockPurchaseDaoForPreviousPurchases(returningPurchases = Seq.empty)

        val greaterPrice = purchasedProduct.priceContext.map(_.basePrice + 150).get
        mockPriceService(purchasedProduct, returningPrice = greaterPrice)

        checkCreatedPurchasedProduct(ProductTypes.Placement, greaterPrice)
        val state = ProcessingState(purchasedProduct)
        stage.process(state).futureValue // run stage
      }
    }
  }

  private def mockPriceService(purchasedProduct: PurchasedProduct, returningPrice: Long): Unit = {
    val contexts: Contexts = Contexts(
      target = purchasedProduct.target,
      productType = purchasedProduct.product,
      priceContext = PriceContext(returningPrice, returningPrice, modifiers = Seq.empty, experimentFlags = Seq.empty),
      productContext = ProductContext(7.days, Some(PaymentType.NATURAL_PERSON), shouldTurnOnRenewal = true),
      billingContext = None
    )
    (priceService
      .getContexts(
        _: UserRef,
        _: Iterable[GetContextRequest],
        _: Boolean,
        _: Option[ExtendedUserType],
        _: Boolean
      )(_: Traced))
      .expects(*, *, true, *, false, *)
      .returning(Future.successful(Seq(contexts)))
  }

  private def mockRenewalLogic(purchasedProduct: PurchasedProduct, verdict: RenewalActualityVerdict): Unit =
    (renewalActualityCheckLogic.shouldProductRenewed _)
      .expects(purchasedProduct.withoutVisitTime)
      .returning(Future.successful(verdict))

  private def mockPurchaseDaoForPreviousPurchases(returningPurchases: Seq[Purchase]): Unit =
    (purchaseDao
      .getPurchases(_: PurchaseFilter))
      .expects(*)
      .returning(Future(returningPurchases))

  private def mockPurchaseDaoForCurrentProduct(p: PurchasedProduct, returningPurchase: Purchase): Unit =
    (purchaseDao.getPurchase _)
      .expects(p.purchaseId.getOrElse(""))
      .returning(Future.successful(returningPurchase.copy(id = p.purchaseId.getOrElse(""))))

  private def checkCreatedPurchasedProduct(productType: ProductType, price: Long): Unit =
    (purchaseDao
      .create(_: Purchase, _: Iterable[PurchasedProduct]))
      .expects(where { (_, products) =>
        hasProductWithPrice(products, productType, price)
      })
      .returning(Future.successful(Unit))

  def hasProductWithPrice(pps: Iterable[PurchasedProduct], product: ProductType, price: Long): Boolean =
    pps.exists(pp => pp.product == product && pp.priceContext.exists(_.basePrice == price))
}
