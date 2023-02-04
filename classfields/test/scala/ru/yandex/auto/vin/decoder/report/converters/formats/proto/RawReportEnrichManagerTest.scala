package ru.yandex.auto.vin.decoder.report.converters.formats.proto

import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.VinReportModel.RawVinReport
import ru.yandex.auto.vin.decoder.model.ColorsSelectorHolder
import ru.yandex.auto.vin.decoder.model.vin.Offer
import ru.yandex.auto.vin.decoder.report.converters.formats.proto.builder.additional.UrlBuilder
import ru.yandex.auto.vin.decoder.report.converters.raw.AdditionalReportData
import ru.yandex.auto.vin.decoder.utils.features.CarfaxFeatures
import ru.yandex.vertis.mockito.MockitoSupport

class RawReportEnrichManagerTest extends AnyWordSpecLike with MockitoSupport {

  "RawReportEnrichManager" should {
    "not enrich empty pts_owners block" in {
      val manager = buildEnrichManager

      val builder = RawVinReport.newBuilder()
      val additional = AdditionalReportData.Empty.copy(
        offer = Some(Offer.Empty.copy(ownersCount = Some(1))),
        requestFromOwner = false,
        isFree = false
      )
      manager.enrichWithOfferData(builder, additional)
      assert(!builder.hasPtsOwners)
    }
  }

  def buildEnrichManager = {
    val urlBuilder = mock[UrlBuilder]
    val colorsHolder = mock[ColorsSelectorHolder]
    val features = mock[CarfaxFeatures]
    new RawReportEnrichManager(colorsHolder, urlBuilder, features)
  }
}
