package ru.auto.api.managers.price

import org.joda.time.DateTime
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.PaidServicePrice
import ru.auto.api.{ApiOfferModel, BaseSpec}
import ru.auto.api.CommonModel.PaidService
import ru.auto.api.model.AutoruProduct.Placement
import ru.yandex.vertis.mockito.MockitoSupport
import ru.auto.api.model.ModelGenerators._

import scala.jdk.CollectionConverters._

class UserPriceManagerHelperSpec extends BaseSpec with ScalaCheckPropertyChecks with MockitoSupport {
  "UserPriceManagerHelper" should {
    "check if placement deadline less than in 7 days and placement is active" in {
      forAll(CarsOfferGen.map(withDeadlineLessThanIn7Days)) { offer =>
        UserPriceManagerHelper.expiresIn7Days(offer) shouldBe true
      }
    }

    "check if placement deadline is more than in 7 days" in {
      forAll(CarsOfferGen.map(withDeadlineMoreThanIn7Days)) { offer =>
        UserPriceManagerHelper.expiresIn7Days(offer) shouldBe false
      }
    }

    "check if placement isn't active" in {
      forAll(CarsOfferGen.map(withNotActivePlacement)) { offer =>
        UserPriceManagerHelper.expiresIn7Days(offer) shouldBe false
      }
    }

    "not disable prolongation for vas" in {
      forAll(
        offerWithServiceGen(activePaidAutoruServiceGen(autoruProductGen(userVasGen))),
        paidServicePriceForProductGen(userVasGen).map(setProlongationTrue)
      ) { (offer, price) =>
        val patchedPrice = UserPriceManagerHelper.withPatchedProlongationFlags(price, offer, false)

        patchedPrice.getProlongationAllowed shouldBe true
        patchedPrice.getProlongationForced shouldBe true
        patchedPrice.getProlongationForcedNotTogglable shouldBe true
      }
    }
  }

  private def withDeadlineLessThanIn7Days(offer: ApiOfferModel.Offer): ApiOfferModel.Offer = {
    val now = new DateTime()
    val placement = PaidService
      .newBuilder()
      .setService(Placement.salesName)
      .setCreateDate(now.getMillis)
      .setExpireDate(now.plusDays(6).getMillis)
      .setIsActive(true)
      .build
    offer.toBuilder.clearServices().addAllServices(Seq(placement).asJava).build
  }

  private def withDeadlineMoreThanIn7Days(offer: ApiOfferModel.Offer): ApiOfferModel.Offer = {
    val now = new DateTime()
    val placement = PaidService
      .newBuilder()
      .setService(Placement.salesName)
      .setCreateDate(now.getMillis)
      .setExpireDate(now.plusDays(10).getMillis)
      .setIsActive(true)
      .build
    offer.toBuilder.clearServices().addAllServices(Seq(placement).asJava).build
  }

  private def withNotActivePlacement(offer: ApiOfferModel.Offer): ApiOfferModel.Offer = {
    val now = new DateTime()
    val placement = PaidService
      .newBuilder()
      .setService(Placement.salesName)
      .setCreateDate(now.getMillis)
      .setExpireDate(now.plusDays(6).getMillis)
      .setIsActive(false)
      .build
    offer.toBuilder.clearServices().addAllServices(Seq(placement).asJava).build
  }

  private def setProlongationTrue(price: PaidServicePrice): PaidServicePrice =
    price.toBuilder
      .setProlongationAllowed(true)
      .setProlongationForced(true)
      .setProlongationForcedNotTogglable(true)
      .build
}
