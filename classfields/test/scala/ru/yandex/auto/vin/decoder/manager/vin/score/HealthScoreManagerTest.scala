package ru.yandex.auto.vin.decoder.manager.vin.score

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.ApiOfferModel.SellerType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory.Status
import ru.yandex.auto.vin.decoder.report.converters.raw.blocks.offers.PreparedOfferData
import ru.yandex.vertis.mockito.MockitoSupport

class HealthScoreManagerTest extends AnyWordSpecLike with Matchers with MockitoSupport {

  implicit val t: Traced = Traced.empty
  private val TestVin = VinCode.apply("XTT315196C0516055")

  val calculator = new HealthScoreCalculator
  val manager = new HealthScoreManager(calculator)

  def suitableDependencies: ScoreDependencies = {
    val d = mock[ScoreDependencies]
    when(d.hasFreshMileage).thenReturn(true)
    when(d.tooOldVehicle).thenReturn(false)
    when(d.hasOrUpdateAllDependencies).thenReturn(true)
    d
  }

  def mockedOffer(sellerType: SellerType, isActive: Boolean): PreparedOfferData = {
    val o = mock[PreparedOfferData]
    when(o.isActive).thenReturn(isActive)
    when(o.sellerType).thenReturn(sellerType)
    o
  }

  "show score block" should {
    "return true" when {
      "all dependencies suitable" in {
        val res =
          manager.canShowBlock(
            vin = TestVin,
            beatenStatus = Status.OK,
            offers = List(mockedOffer(SellerType.PRIVATE, false)),
            forEssentials = false,
            dependencies = suitableDependencies
          )

        res shouldBe true
      }
      "there are not active dealer offer" in {
        val dependencies = suitableDependencies

        val res =
          manager.canShowBlock(
            vin = TestVin,
            beatenStatus = Status.OK,
            offers = List(mockedOffer(SellerType.COMMERCIAL, false)),
            forEssentials = false,
            dependencies = dependencies
          )

        res shouldBe true
      }
      "there are active dealer offer (for essentials)" in {
        val dependencies = suitableDependencies

        val res =
          manager.canShowBlock(
            vin = TestVin,
            beatenStatus = Status.OK,
            offers = List(mockedOffer(SellerType.COMMERCIAL, true)),
            forEssentials = true,
            dependencies = dependencies
          )

        res shouldBe true
      }
    }
    "return false" when {
      "car was beaten" in {
        val res =
          manager.canShowBlock(
            vin = TestVin,
            beatenStatus = Status.ERROR,
            offers = List(mockedOffer(SellerType.PRIVATE, false)),
            forEssentials = false,
            dependencies = suitableDependencies
          )

        res shouldBe false
      }

      "car too old" in {
        val dependencies = suitableDependencies
        when(dependencies.tooOldVehicle).thenReturn(true)

        val res =
          manager.canShowBlock(
            vin = TestVin,
            beatenStatus = Status.OK,
            offers = List(mockedOffer(SellerType.PRIVATE, false)),
            forEssentials = false,
            dependencies = dependencies
          )

        res shouldBe false
      }

      "dont have fresh mileage" in {
        val dependencies = suitableDependencies
        when(dependencies.hasFreshMileage).thenReturn(false)

        val res =
          manager.canShowBlock(
            vin = TestVin,
            beatenStatus = Status.OK,
            offers = List(mockedOffer(SellerType.PRIVATE, false)),
            forEssentials = false,
            dependencies = dependencies
          )

        res shouldBe false
      }

      "there are active dealer offer" in {
        val dependencies = suitableDependencies

        val res =
          manager.canShowBlock(
            vin = TestVin,
            beatenStatus = Status.OK,
            offers = List(mockedOffer(SellerType.COMMERCIAL, true)),
            forEssentials = false,
            dependencies = dependencies
          )

        assert(!res)
      }
    }
  }
}
