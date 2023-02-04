package ru.yandex.realty.seller.model.event

import org.junit.runner.RunWith
import org.scalatest.TryValues
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.user.PassportUser
import ru.yandex.realty.proto.offer
import ru.yandex.realty.proto.offer.event.SellerEvent
import ru.yandex.realty.proto.offer.{CampaignType, ProductSource}
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.product._

import scala.collection.JavaConverters._

/**
  * @author Vsevolod Levin
  */
@RunWith(classOf[JUnitRunner])
class DefaultSellerEventConverterSpec extends SpecBase with SellerModelGenerators with PropertyChecks with TryValues {

  val converter: SellerEventConverter = DefaultSellerEventConverter

  "SellerEventConverter" should {
    "work for simple products" in {
      forAll(purchasedProductGen) { product =>
        val result = converter.toMessage(product, Seq.empty)
        val event = result.success.value
        check(event, product)
      }
    }

    "work for package products" in {
      forAll(purchasedPackageGen, listUnique(1, 5, purchasedProductGen)(_.id)) { (product, products) =>
        val result = converter.toMessage(product, products)
        val event = result.success.value
        check(event, product)
        products
          .zip(event.getOfferProducts.getProducts(0).getPackageProductsList.asScala)
          .foreach {
            case (origin, res) => check(origin, res)
          }
      }
    }
  }

  private def check(event: SellerEvent, product: PurchasedProduct): Unit = {
    event.getPayloadCase shouldBe SellerEvent.PayloadCase.OFFER_PRODUCTS
    val payload = event.getOfferProducts

    product.owner match {
      case PassportUser(u) => payload.getIdentity.getUid shouldBe u.toString
    }

    product.target match {
      case OfferTarget(offerId) => payload.getIdentity.getOfferId shouldBe offerId
    }

    payload.getProductsCount shouldBe 1
    val p = payload.getProducts(0)
    check(product, p)
  }

  //scalastyle:off cyclomatic.complexity
  private def check(product: PurchasedProduct, p: offer.Product): Unit = {
    p.getId shouldBe product.id
    product.startTime match {
      case Some(dt) => p.getStartTime shouldBe dt.getMillis
      case None => p.getStartTime shouldBe 0
    }
    product.endTime match {
      case Some(dt) => p.getEndTime shouldBe dt.getMillis
      case None => p.getEndTime shouldBe 0
    }
    p.getActive shouldBe product.status == PurchasedProductStatuses.Active

    val campaignType = product.product match {
      case ProductTypes.Raising => CampaignType.CAMPAIGN_TYPE_RAISING
      case ProductTypes.Premium => CampaignType.CAMPAIGN_TYPE_PREMIUM
      case ProductTypes.Promotion => CampaignType.CAMPAIGN_TYPE_PROMOTION
      case ProductTypes.PackageTurbo => CampaignType.CAMPAIGN_TYPE_PACKAGE_TURBO
      case ProductTypes.PackageRaising => CampaignType.CAMPAIGN_TYPE_PACKAGE_RAISING
    }
    p.getType shouldBe campaignType

    campaignType match {
      case CampaignType.CAMPAIGN_TYPE_PACKAGE_TURBO | CampaignType.CAMPAIGN_TYPE_PACKAGE_RAISING =>
        p.getPackageProductsCount should be > 0
      case _ =>
        p.getPackageProductsCount shouldBe 0
    }

    product.source match {
      case ManualSource =>
        p.getSource shouldBe ProductSource.PRODUCT_SOURCE_MANUAL
        p.getSourceId shouldBe empty
      case FeedSource =>
        p.getSource shouldBe ProductSource.PRODUCT_SOURCE_FEED
        p.getSourceId shouldBe empty
      case PackageSource(packageProductId) =>
        p.getSource shouldBe ProductSource.PRODUCT_SOURCE_PACKAGE
        p.getSourceId shouldBe packageProductId
      case RuleSource(ruleId) =>
        p.getSource shouldBe ProductSource.PRODUCT_SOURCE_RULE
        p.getSourceId shouldBe ruleId
      case ExperimentSource(experimentKey) =>
        p.getSource shouldBe ProductSource.PRODUCT_SOURCE_EXPERIMENT
        p.getSourceId shouldBe experimentKey
      case TuzSource =>
        p.getSource shouldBe ProductSource.PRODUCT_SOURCE_TUZ
        p.getSourceId shouldBe empty
    }
  }

  //scalastyle:on
}
