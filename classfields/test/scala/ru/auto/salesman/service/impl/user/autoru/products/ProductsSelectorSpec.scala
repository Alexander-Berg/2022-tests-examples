package ru.auto.salesman.service.impl.user.autoru.products

import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.model.user.product.ProductProvider.AutoruSubscriptions.OffersHistoryReports
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.UserModelGenerators
import ru.yandex.passport.model.common.CommonModel
import ru.yandex.passport.model.common.CommonModel.UserModerationStatus
import ru.yandex.vertis.moderation.proto.Model.Reason.USER_RESELLER
import scala.collection.JavaConverters._

class ProductsSelectorSpec extends BaseSpec with UserModelGenerators {

  "ProductsSelector" should {
    "choose available offersHistoryReports products" in {
      ProductsSelector
        .parseAndChooseAvailableProducts(
          List("offers-history-reports"),
          optUserModerationStatus = None
        )
        .success
        .value shouldBe List(
        OffersHistoryReports(1),
        OffersHistoryReports(5),
        OffersHistoryReports(10)
      )
    }

    "choose exact offers-history-reports-10 product" in {
      ProductsSelector
        .parseAndChooseAvailableProducts(
          List("offers-history-reports-10"),
          optUserModerationStatus = None
        )
        .success
        .value shouldBe List(OffersHistoryReports(10))
    }

    "choose other products" in {
      forAll(OfferProductGen) { offerProduct =>
        ProductsSelector
          .parseAndChooseAvailableProducts(
            List(offerProduct.name),
            optUserModerationStatus = None
          )
          .success
          .value shouldBe List(offerProduct)
      }
    }

    "fail on unknown product" in {
      ProductsSelector
        .parseAndChooseAvailableProducts(
          List("неизвестный продукт"),
          optUserModerationStatus = None
        )
        .failure
    }

    "if reseller, don't get offers-history-reports-5" in {
      val ban = Map(
        "CARS" -> CommonModel.DomainBan.newBuilder
          .addAllReasons(Iterable(USER_RESELLER.toString).asJava)
          .build
      ).asJava
      val userModerationStatus =
        UserModerationStatus.newBuilder().putAllBans(ban).build
      ProductsSelector
        .parseAndChooseAvailableProducts(
          List("offers-history-reports"),
          Some(userModerationStatus)
        )
        .success
        .value should not contain OffersHistoryReports(5)
    }

    "if reseller, get offers-history-reports-50 for new apps" in {
      val ban = Map(
        "CARS" -> CommonModel.DomainBan.newBuilder
          .addAllReasons(Iterable(USER_RESELLER.toString).asJava)
          .build
      ).asJava
      val userModerationStatus =
        UserModerationStatus.newBuilder().putAllBans(ban).build
      ProductsSelector
        .parseAndChooseAvailableProducts(
          List("offers-history-reports"),
          Some(userModerationStatus)
        )
        .success
        .value should contain(OffersHistoryReports(50))
    }

    "if reseller, get offers-history-reports-10 for old apps" in {
      val ban = Map(
        "CARS" -> CommonModel.DomainBan.newBuilder
          .addAllReasons(Iterable(USER_RESELLER.toString).asJava)
          .build
      ).asJava
      val userModerationStatus =
        UserModerationStatus.newBuilder().putAllBans(ban).build
      ProductsSelector
        .parseAndChooseAvailableProducts(
          List("offers-history-reports"),
          Some(userModerationStatus)
        )
        .success
        .value should contain(OffersHistoryReports(10))
    }
  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
