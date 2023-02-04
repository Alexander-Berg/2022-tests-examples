package ru.yandex.auto.vin.decoder.report.converters.formats.proto.builder

import auto.carfax.common.utils.avatars.AvatarsExternalUrlsBuilder
import auto.carfax.common.utils.tracing.Traced
import com.google.protobuf.util.Timestamps
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.VinReportModel.RawVinEssentialsReport
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.extdata.ApiExtDataClient
import ru.yandex.auto.vin.decoder.extdata.region.Tree
import ru.yandex.auto.vin.decoder.model.vin.{Offer, VinCatalog}
import ru.yandex.auto.vin.decoder.model.{offer, MockedFeatures, VinCode}
import ru.yandex.auto.vin.decoder.proto.VinHistory.{KmageInfo, RegistrationEvent, VinInfo}
import ru.yandex.auto.vin.decoder.providers.certification.BrandCertifications
import ru.yandex.auto.vin.decoder.providers.vin.ClientVinProvider
import ru.yandex.auto.vin.decoder.report.ReportDefinition.AllRecords
import ru.yandex.auto.vin.decoder.report.converters.formats.proto.builder.BlockBuilder.BuilderParams
import ru.yandex.auto.vin.decoder.report.converters.formats.proto.builder.additional.UrlBuilder
import ru.yandex.auto.vin.decoder.report.converters.raw.EssentialReportData
import ru.yandex.auto.vin.decoder.report.converters.raw.blocks.PreparedBlock
import ru.yandex.auto.vin.decoder.report.converters.raw.blocks.history.PreparedHistoryData
import ru.yandex.auto.vin.decoder.report.converters.raw.blocks.history.entities.OfferPreparedHistoryEntity
import ru.yandex.auto.vin.decoder.report.converters.raw.blocks.mileages.PreparedMileagesData
import ru.yandex.auto.vin.decoder.report.converters.raw.common.PreparedRegistrationPeriodsData
import ru.yandex.auto.vin.decoder.report.processors.entities.OfferEntity
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters.CollectionHasAsScala

class EssentialsBuilderTest extends AnyWordSpecLike with MockitoSupport with MockedFeatures {

  implicit val t = Traced.empty

  when(features.ShowRegActionsByOwners).thenReturn(Feature("", _ => true))

  val urlBuilder = mock[UrlBuilder]
  val AvatarsBuilderMock = mock[AvatarsExternalUrlsBuilder]
  val brandCertifications = BrandCertifications(Map.empty)
  val builders = new ReportBlockBuilders(AvatarsBuilderMock, urlBuilder, brandCertifications, features)
  val UnregisteredPeriodsColor = "#919BA7"
  val vin = VinCode("WBAUE11040E238571")

  "EssentialsBuilder".can {
    "build owner colored timeline related blocks" should {

      def getOwner(from: Long, to: Long): RegistrationEvent = {
        RegistrationEvent
          .newBuilder()
          .setOwner("OWNER")
          .setFrom(from)
          .setTo(to)
          .build()
      }

      val d = OfferPreparedHistoryEntity(
        id = "",
        currentOfferId = None,
        isCurrent = false,
        mileage = Some(1232),
        ts = 1231234124,
        isRed = false,
        offerGroup = buildOfferEntity(kmage = 1234567, date = 40).offerGroup,
        List.empty
      )

      "keep same colors for both history and mileage graph" in {
        val firstOwner = getOwner(2, 3)
        val secondOwner = getOwner(4, 6)
        val owners = List(firstOwner, secondOwner)
        val regPeriods = PreparedRegistrationPeriodsData(Some(owners), owners, List(d), 1970)
        val mileagesData = PreparedMileagesData("false", List(d), regPeriods)
        val essentialsData = EssentialReportData.Empty.copy(
          mileages = PreparedBlock(isUpdating = false, None, mileagesData),
          history = PreparedBlock(
            isUpdating = false,
            None,
            Some(PreparedHistoryData(vin, regPeriods, List(d), "", Tree.Empty, VinCatalog.Empty))
          )
        )
        val report = BlockBuilder.compose(
          RawVinEssentialsReport.newBuilder(),
          Set(builders.mileagesGraphDataBuilder, builders.historyBlockBuilder),
          essentialsData,
          BuilderParams(isFree = false, isRequestFromOwner = false, timelineMode = AllRecords)
        )
        val ownersBlockColors = report.getHistory.getOwnersList.asScala.map(_.getOwner.getColorHex)
        val mileagesBlockColors = report.getMileagesGraph.getMileagesGraphData.getOwnersList.asScala.map(_.getColorHex)
        assert(ownersBlockColors == mileagesBlockColors)
      }

      "keep no owner colors for no gibdd events" in {
        val regPeriods = PreparedRegistrationPeriodsData(Some(List.empty), List.empty, List(d, d, d), 1970)
        val mileagesData = PreparedMileagesData("false", List(d), regPeriods)
        val essentialsData = EssentialReportData.Empty.copy(
          history = PreparedBlock(
            isUpdating = false,
            None,
            Some(PreparedHistoryData(vin, regPeriods, List.empty, "", Tree.Empty, VinCatalog.Empty))
          ),
          mileages = PreparedBlock(isUpdating = false, None, mileagesData)
        )
        val report = BlockBuilder.compose(
          RawVinEssentialsReport.newBuilder(),
          Set(builders.historyBlockBuilder, builders.mileagesGraphDataBuilder),
          essentialsData,
          BuilderParams(isFree = false, isRequestFromOwner = false, timelineMode = AllRecords)
        )
        val ownersBlockColors = report.getHistory.getOwnersList.asScala.map(_.getOwner.getColorHex)
        val mileagesBlockColors = report.getMileagesGraph.getMileagesGraphData.getOwnersList.asScala.map(_.getColorHex)
        assert(mileagesBlockColors.size == 1)
        assert(mileagesBlockColors.head == UnregisteredPeriodsColor)
        assert(ownersBlockColors.isEmpty)
      }

    }
  }

  def buildOfferEntity(kmage: Int, date: Long): OfferEntity[Offer] = {
    OfferEntity(
      offer.OfferGroup(
        List(
          VinInfo.newBuilder
            .addKmageHistory(
              KmageInfo.newBuilder.setKmage(kmage).setUpdateTimestamp(Timestamps.fromMillis(date))
            )
            .build
        ),
        EventType.AUTORU_OFFER
      ),
      Offer.Empty,
      Map.empty,
      "Z0NZWE00054341429"
    )(OfferEntity.OfferToOpt)
  }

}
