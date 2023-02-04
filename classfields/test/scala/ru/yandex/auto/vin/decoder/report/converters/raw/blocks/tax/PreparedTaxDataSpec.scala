package ru.yandex.auto.vin.decoder.report.converters.raw.blocks.tax

import org.mockito.Mockito._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import ru.auto.api.OwnerExpensesModel.TransportTax
import ru.yandex.auto.vin.decoder.extdata.region.{GeoRegion, Tree}

class PreparedTaxDataSpec extends AnyWordSpecLike with Matchers with MockitoSugar {

  private val year = 2018
  private val horsePower = 112
  private val taxByYear = 4567
  private val regionId = 213
  private val tree = mock[Tree]

  private val region =
    GeoRegion(regionId, "Москва", -1, "Москвы", "Москве", "Москву", 6, "Москве", "в", 10800)

  "PreparedTaxDataSpec" should {
    "build tax data with max value only" in {
      val tax = TransportTax
        .newBuilder()
        .setBoost(1)
        .setHoldingPeriodMonth(24)
        .setHorsePower(horsePower)
        .setRid(regionId)
        .setYear(year)
        .setTaxByYear(taxByYear)
        .build()

      when(tree.findRegion(regionId)).thenReturn(Some(region))

      val actual = PreparedTaxData.build(regionId, Seq(tax), tree)
      actual shouldBe Some(
        PreparedTaxData(None, 2283, year, Some(region), regionId)
      )
    }

    "build empty tax data when empty tax sequence provided" in {
      val actual = PreparedTaxData.build(regionId, Seq.empty, tree)
      actual shouldBe None
    }

    "build tax data with max value only if same tax values provided" in {
      val tax = TransportTax
        .newBuilder()
        .setBoost(1)
        .setHoldingPeriodMonth(24)
        .setHorsePower(horsePower)
        .setRid(regionId)
        .setYear(year)
        .setTaxByYear(taxByYear)
        .build()

      when(tree.findRegion(regionId)).thenReturn(Some(region))

      val actual =
        PreparedTaxData.build(regionId, Seq(tax, tax, tax), tree)
      actual shouldBe Some(
        PreparedTaxData(None, 2283, year, Some(region), regionId)
      )
    }

    "build tax data with both max and min value if different tax values provided" in {
      val taxForMax = TransportTax
        .newBuilder()
        .setBoost(1)
        .setHoldingPeriodMonth(24)
        .setHorsePower(horsePower)
        .setRid(regionId)
        .setYear(year)
        .setTaxByYear(taxByYear)
        .build()

      val taxForMin =
        taxForMax.toBuilder.setTaxByYear(taxByYear + taxByYear).build()

      when(tree.findRegion(regionId)).thenReturn(Some(region))

      val actual =
        PreparedTaxData.build(regionId, Seq(taxForMax, taxForMin), tree)
      actual shouldBe Some(
        PreparedTaxData(Some(2283), 4567, year, Some(region), regionId)
      )
    }
  }
}
