package ru.yandex.auto.vin.decoder.report.converters.formats.proto

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.ApiOfferModel.{OfferStatus, SellerType}
import ru.auto.api.CommonModel.{MileageInfo, PriceInfo}
import ru.auto.api.vin.VinReportModel
import ru.auto.api.vin.VinReportModel.HistoryBlock.{HistoryRecord, OwnerHistory}
import ru.auto.api.vin.VinReportModel._
import ru.auto.api.vin.VinResolutionEnums.Status
import ru.yandex.auto.vin.decoder.extdata.region.{GeoRegion, GeoRegionCodes, Tree}
import ru.yandex.auto.vin.decoder.proto.VinHistory
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import ru.yandex.auto.vin.decoder.report.converters.raw.blocks.PreparedBlock
import ru.yandex.auto.vin.decoder.report.converters.raw.blocks.audatex.CostRange
import ru.yandex.auto.vin.decoder.report.converters.raw.blocks.legal.PreparedPledgeData
import ru.yandex.auto.vin.decoder.report.converters.raw.blocks.offers.PreparedVosData
import ru.yandex.auto.vin.decoder.report.converters.raw.common.PreparedCommonSummaryData
import ru.yandex.auto.vin.decoder.report.converters.raw.{AdditionalReportData, EssentialReportData}
import ru.yandex.auto.vin.decoder.utils.features.CarfaxFeatures
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters.{IterableHasAsJava, ListHasAsScala}

class RawReportDecayManagerTest extends AnyWordSpecLike with Matchers with MockitoSupport {

  private val features = mock[CarfaxFeatures]
  private val featureTrue = mock[Feature[Boolean]]
  when(featureTrue.value).thenReturn(true)
  private val featureFalse = mock[Feature[Boolean]]
  when(featureFalse.value).thenReturn(false)
  when(features.AutocodeFines).thenReturn(featureFalse)
  when(features.HideEstimatePrices).thenReturn(featureTrue)
  when(features.ShowInsurancePayments).thenReturn(featureTrue)
  when(features.ShowLeasingsInReports).thenReturn(featureFalse)
  when(features.ShowReviewPhotos).thenReturn(featureFalse)

  private val tree = mock[Tree]

  private val regionMsc = GeoRegion(GeoRegionCodes.MOSCOW, "Москва", 0, "", "", "", 0, "", "", 14400) // UTC+04:00

  private val regionStPt = GeoRegion(
    GeoRegionCodes.ST_PETERSBURG_AND_LENINGRAD_REGION_CODE,
    "Санкт-Петербург",
    0,
    "",
    "",
    "",
    0,
    "",
    "",
    14400
  ) // UTC+04:00

  private val regionSverdlovsk =
    GeoRegion(GeoRegionCodes.SVERDLOVSK_REGION_CODE, "Свердловск", 0, "", "", "", 0, "", "", 18000) // UTC+05:00

  when(tree.findRegion(eq(213))).thenReturn(Some(regionMsc))
  when(tree.findRegion(eq(GeoRegionCodes.ST_PETERSBURG_AND_LENINGRAD_REGION_CODE))).thenReturn(Some(regionStPt))
  when(tree.findRegion(eq(GeoRegionCodes.SVERDLOVSK_REGION_CODE))).thenReturn(Some(regionSverdlovsk))

  when(
    tree.isInside(
      eq(213),
      eq(Set(GeoRegionCodes.ST_PETERSBURG_AND_LENINGRAD_REGION_CODE, GeoRegionCodes.SVERDLOVSK_REGION_CODE))
    )
  ).thenReturn(false)

  when(
    tree.isInside(
      eq(GeoRegionCodes.ST_PETERSBURG_AND_LENINGRAD_REGION_CODE),
      eq(Set(GeoRegionCodes.ST_PETERSBURG_AND_LENINGRAD_REGION_CODE, GeoRegionCodes.SVERDLOVSK_REGION_CODE))
    )
  ).thenReturn(true)

  when(
    tree.isInside(
      eq(GeoRegionCodes.SVERDLOVSK_REGION_CODE),
      eq(Set(GeoRegionCodes.ST_PETERSBURG_AND_LENINGRAD_REGION_CODE, GeoRegionCodes.SVERDLOVSK_REGION_CODE))
    )
  ).thenReturn(true)

  private val summary: PreparedCommonSummaryData = PreparedCommonSummaryData.fromPrepared(
    preparedVin = "",
    updateTimestamp = 0,
    optOffer = None,
    tech = None,
    wanted = None,
    pledge = None,
    legal = None,
    constraint = None,
    dtp = None,
    recalls = Seq(),
    owners = None,
    historyEntities = Seq(),
    forCard = true,
    isGibddUpdating = true,
    priceExperiment = None,
    isPaid = false
  )

  private val manager = new RawReportDecayManager(features)

  "decayBasic" should {
    "do not the clear price for ALL offers if feature turn on and seller not equal COMMERCIAL" in {

      val autoruOfferBuilder = AutoruOffersBlock
        .newBuilder()
        .addOffers(
          getOffer(
            GeoRegionCodes.ST_PETERSBURG_AND_LENINGRAD_REGION_CODE,
            OfferStatus.ACTIVE,
            SellerType.PRIVATE,
            20000
          )
        )
        .addOffers(getOffer(GeoRegionCodes.MOSCOW, OfferStatus.REMOVED, SellerType.COMMERCIAL, 40000))
        .addOffers(getOffer(GeoRegionCodes.SVERDLOVSK_REGION_CODE, OfferStatus.BANNED, SellerType.COMMERCIAL, 60000))

      val builder = RawVinReport
        .newBuilder()
        .setAutoruOffers(autoruOfferBuilder)

      manager.decayBasic(builder, EssentialReportData.Empty, AdditionalReportData.Empty, None)

      builder.getAutoruOffers.getOffersList.asScala.head.getPrice shouldBe 20000
      builder.getAutoruOffers.getOffersList.asScala.head.getOfferStatus shouldBe OfferStatus.ACTIVE
      builder.getAutoruOffers.getOffersList.asScala.head.getGeobaseId shouldBe GeoRegionCodes.ST_PETERSBURG_AND_LENINGRAD_REGION_CODE
      builder.getAutoruOffers.getOffersList.asScala.head.getOfferChangesHistoryRecordsList.asScala.head.getPrice.getPrice.getPrice shouldBe 3000f
      builder.getAutoruOffers.getOffersList.asScala.head.getOfferChangesHistoryRecordsList.asScala.head.getMileage.getMileage.getMileage shouldBe 100

      builder.getAutoruOffers.getOffersList.asScala(1).getPrice shouldBe 40000
      builder.getAutoruOffers.getOffersList.asScala(1).getOfferStatus shouldBe OfferStatus.REMOVED
      builder.getAutoruOffers.getOffersList.asScala(1).getGeobaseId shouldBe GeoRegionCodes.MOSCOW
      builder.getAutoruOffers.getOffersList
        .asScala(1)
        .getOfferChangesHistoryRecordsList
        .asScala
        .head
        .getPrice
        .getPrice
        .getPrice shouldBe 3000f
      builder.getAutoruOffers.getOffersList
        .asScala(1)
        .getOfferChangesHistoryRecordsList
        .asScala
        .head
        .getMileage
        .getMileage
        .getMileage shouldBe 100

      builder.getAutoruOffers.getOffersList.asScala.last.getPrice shouldBe 60000
      builder.getAutoruOffers.getOffersList.asScala.last.getOfferStatus shouldBe OfferStatus.BANNED
      builder.getAutoruOffers.getOffersList.asScala.last.getGeobaseId shouldBe GeoRegionCodes.SVERDLOVSK_REGION_CODE
      builder.getAutoruOffers.getOffersList.asScala.last.getOfferChangesHistoryRecordsList.asScala.head.getPrice.getPrice.getPrice shouldBe 3000f
      builder.getAutoruOffers.getOffersList.asScala.last.getOfferChangesHistoryRecordsList.asScala.head.getMileage.getMileage.getMileage shouldBe 100

    }

    "clear the price for all not ACTIVE offers if feature turn on and ACTIVE offer with seller COMMERCIAL" in {

      val autoruOfferBuilder = AutoruOffersBlock
        .newBuilder()
        .addOffers(
          getOffer(
            GeoRegionCodes.ST_PETERSBURG_AND_LENINGRAD_REGION_CODE,
            OfferStatus.ACTIVE,
            SellerType.COMMERCIAL,
            20000
          )
        )
        .addOffers(getOffer(GeoRegionCodes.MOSCOW, OfferStatus.REMOVED, SellerType.COMMERCIAL, 40000))
        .addOffers(getOffer(GeoRegionCodes.SVERDLOVSK_REGION_CODE, OfferStatus.BANNED, SellerType.COMMERCIAL, 60000))

      val builder = RawVinReport
        .newBuilder()
        .setAutoruOffers(autoruOfferBuilder)

      manager.decayBasic(builder, EssentialReportData.Empty, AdditionalReportData.Empty, None)

      builder.getAutoruOffers.getOffersList.asScala.head.getPrice shouldBe 20000
      builder.getAutoruOffers.getOffersList.asScala.head.getOfferStatus shouldBe OfferStatus.ACTIVE
      builder.getAutoruOffers.getOffersList.asScala.head.getGeobaseId shouldBe GeoRegionCodes.ST_PETERSBURG_AND_LENINGRAD_REGION_CODE
      builder.getAutoruOffers.getOffersList.asScala.head.getOfferChangesHistoryRecordsList.asScala.head.getPrice.getPrice.getPrice shouldBe 3000f
      builder.getAutoruOffers.getOffersList.asScala.head.getOfferChangesHistoryRecordsList.asScala.head.getMileage.getMileage.getMileage shouldBe 100

      builder.getAutoruOffers.getOffersList.asScala(1).getPrice shouldBe 0
      builder.getAutoruOffers.getOffersList.asScala(1).getOfferStatus shouldBe OfferStatus.REMOVED
      builder.getAutoruOffers.getOffersList.asScala(1).getGeobaseId shouldBe GeoRegionCodes.MOSCOW
      builder.getAutoruOffers.getOffersList
        .asScala(1)
        .getOfferChangesHistoryRecordsList
        .asScala
        .head
        .getPrice
        .getPrice
        .getPrice shouldBe 0
      builder.getAutoruOffers.getOffersList
        .asScala(1)
        .getOfferChangesHistoryRecordsList
        .asScala
        .head
        .getMileage
        .getMileage
        .getMileage shouldBe 100

      builder.getAutoruOffers.getOffersList.asScala.last.getPrice shouldBe 0
      builder.getAutoruOffers.getOffersList.asScala.last.getOfferStatus shouldBe OfferStatus.BANNED
      builder.getAutoruOffers.getOffersList.asScala.last.getGeobaseId shouldBe GeoRegionCodes.SVERDLOVSK_REGION_CODE
      builder.getAutoruOffers.getOffersList.asScala.last.getOfferChangesHistoryRecordsList.asScala.head.getPrice.getPrice.getPrice shouldBe 0
      builder.getAutoruOffers.getOffersList.asScala.last.getOfferChangesHistoryRecordsList.asScala.head.getMileage.getMileage.getMileage shouldBe 100

    }

    import auto.carfax.common.utils.protobuf.ProtobufConverterOps.BooleanOps

    "clear the price for all not ACTIVE offers if feature turn on and ACTIVE offer from RESELLER" in {

      val autoruOfferBuilder = AutoruOffersBlock
        .newBuilder()
        .addOffers(
          getOffer(
            GeoRegionCodes.ST_PETERSBURG_AND_LENINGRAD_REGION_CODE,
            OfferStatus.ACTIVE,
            SellerType.PRIVATE,
            20000
          )
        )
        .addOffers(getOffer(GeoRegionCodes.MOSCOW, OfferStatus.REMOVED, SellerType.COMMERCIAL, 40000))
        .addOffers(getOffer(GeoRegionCodes.SVERDLOVSK_REGION_CODE, OfferStatus.BANNED, SellerType.COMMERCIAL, 60000))

      val first = VinHistory.VinInfo
        .newBuilder()
        .setReseller(true.toBoolValue)
        .setRegionId(GeoRegionCodes.ST_PETERSBURG_AND_LENINGRAD_REGION_CODE)
        .setOfferStatus(OfferStatus.ACTIVE)
        .setPrice(20000)
        .build()

      val second = VinHistory.VinInfo
        .newBuilder()
        .setReseller(false.toBoolValue)
        .setRegionId(GeoRegionCodes.MOSCOW)
        .setOfferStatus(OfferStatus.REMOVED)
        .setPrice(40000)
        .build()

      val third = VinHistory.VinInfo
        .newBuilder()
        .setReseller(false.toBoolValue)
        .setRegionId(GeoRegionCodes.SVERDLOVSK_REGION_CODE)
        .setOfferStatus(OfferStatus.ACTIVE)
        .setPrice(60000)
        .build()

      val builder = RawVinReport
        .newBuilder()
        .setAutoruOffers(autoruOfferBuilder)

      val preparedVosData = PreparedVosData(Seq(first, second, third), Some(first))
      manager.decayBasic(
        builder,
        EssentialReportData.Empty.copy(vosData = preparedVosData),
        AdditionalReportData.Empty,
        None
      )

      builder.getAutoruOffers.getOffersList.asScala.head.getPrice shouldBe 20000
      builder.getAutoruOffers.getOffersList.asScala.head.getOfferStatus shouldBe OfferStatus.ACTIVE
      builder.getAutoruOffers.getOffersList.asScala.head.getGeobaseId shouldBe GeoRegionCodes.ST_PETERSBURG_AND_LENINGRAD_REGION_CODE
      builder.getAutoruOffers.getOffersList.asScala.head.getOfferChangesHistoryRecordsList.asScala.head.getMileage.getMileage.getMileage shouldBe 100
      builder.getAutoruOffers.getOffersList.asScala.head.getOfferChangesHistoryRecordsList.asScala.head.getPrice.getPrice.getPrice shouldBe 3000f

      builder.getAutoruOffers.getOffersList.asScala(1).getPrice shouldBe 0
      builder.getAutoruOffers.getOffersList.asScala(1).getOfferStatus shouldBe OfferStatus.REMOVED
      builder.getAutoruOffers.getOffersList.asScala(1).getGeobaseId shouldBe GeoRegionCodes.MOSCOW
      builder.getAutoruOffers.getOffersList
        .asScala(1)
        .getOfferChangesHistoryRecordsList
        .asScala
        .head
        .getPrice
        .getPrice
        .getPrice shouldBe 0
      builder.getAutoruOffers.getOffersList
        .asScala(1)
        .getOfferChangesHistoryRecordsList
        .asScala
        .head
        .getMileage
        .getMileage
        .getMileage shouldBe 100

      builder.getAutoruOffers.getOffersList.asScala.last.getPrice shouldBe 0
      builder.getAutoruOffers.getOffersList.asScala.last.getOfferStatus shouldBe OfferStatus.BANNED
      builder.getAutoruOffers.getOffersList.asScala.last.getGeobaseId shouldBe GeoRegionCodes.SVERDLOVSK_REGION_CODE
      builder.getAutoruOffers.getOffersList.asScala.last.getOfferChangesHistoryRecordsList.asScala.head.getPrice.getPrice.getPrice shouldBe 0
      builder.getAutoruOffers.getOffersList.asScala.last.getOfferChangesHistoryRecordsList.asScala.head.getMileage.getMileage.getMileage shouldBe 100

    }

    "do not to clear price for ALL not ACTIVE offer if feature turn on and ACTIVE offer are not from RESELLER and COMMERCIAL" in {

      val autoruOfferBuilder = AutoruOffersBlock
        .newBuilder()
        .addOffers(
          getOffer(
            GeoRegionCodes.ST_PETERSBURG_AND_LENINGRAD_REGION_CODE,
            OfferStatus.ACTIVE,
            SellerType.PRIVATE,
            20000
          )
        )
        .addOffers(getOffer(GeoRegionCodes.MOSCOW, OfferStatus.REMOVED, SellerType.COMMERCIAL, 40000))
        .addOffers(getOffer(GeoRegionCodes.SVERDLOVSK_REGION_CODE, OfferStatus.BANNED, SellerType.COMMERCIAL, 60000))

      val first = VinHistory.VinInfo
        .newBuilder()
        .setReseller(false.toBoolValue)
        .setRegionId(GeoRegionCodes.ST_PETERSBURG_AND_LENINGRAD_REGION_CODE)
        .setOfferStatus(OfferStatus.ACTIVE)
        .setPrice(20000)
        .build()

      val second = VinHistory.VinInfo
        .newBuilder()
        .setReseller(true.toBoolValue)
        .setRegionId(GeoRegionCodes.MOSCOW)
        .setOfferStatus(OfferStatus.REMOVED)
        .setPrice(40000)
        .build()

      val third = VinHistory.VinInfo
        .newBuilder()
        .setReseller(false.toBoolValue)
        .setRegionId(GeoRegionCodes.SVERDLOVSK_REGION_CODE)
        .setOfferStatus(OfferStatus.ACTIVE)
        .setPrice(60000)
        .build()

      val builder = RawVinReport
        .newBuilder()
        .setAutoruOffers(autoruOfferBuilder)

      val preparedVosData = PreparedVosData(Seq(first, second, third), Some(first))
      manager.decayBasic(
        builder,
        EssentialReportData.Empty.copy(vosData = preparedVosData),
        AdditionalReportData.Empty,
        None
      )

      builder.getAutoruOffers.getOffersList.asScala.head.getPrice shouldBe 20000
      builder.getAutoruOffers.getOffersList.asScala.head.getOfferStatus shouldBe OfferStatus.ACTIVE
      builder.getAutoruOffers.getOffersList.asScala.head.getGeobaseId shouldBe GeoRegionCodes.ST_PETERSBURG_AND_LENINGRAD_REGION_CODE
      builder.getAutoruOffers.getOffersList.asScala.head.getOfferChangesHistoryRecordsList.asScala.head.getMileage.getMileage.getMileage shouldBe 100
      builder.getAutoruOffers.getOffersList.asScala.head.getOfferChangesHistoryRecordsList.asScala.head.getPrice.getPrice.getPrice shouldBe 3000f

      builder.getAutoruOffers.getOffersList.asScala(1).getPrice shouldBe 40000
      builder.getAutoruOffers.getOffersList.asScala(1).getOfferStatus shouldBe OfferStatus.REMOVED
      builder.getAutoruOffers.getOffersList.asScala(1).getGeobaseId shouldBe GeoRegionCodes.MOSCOW
      builder.getAutoruOffers.getOffersList
        .asScala(1)
        .getOfferChangesHistoryRecordsList
        .asScala
        .head
        .getMileage
        .getMileage
        .getMileage shouldBe 100
      builder.getAutoruOffers.getOffersList
        .asScala(1)
        .getOfferChangesHistoryRecordsList
        .asScala
        .head
        .getPrice
        .getPrice
        .getPrice shouldBe 3000f

      builder.getAutoruOffers.getOffersList.asScala.last.getPrice shouldBe 60000
      builder.getAutoruOffers.getOffersList.asScala.last.getOfferStatus shouldBe OfferStatus.BANNED
      builder.getAutoruOffers.getOffersList.asScala.last.getGeobaseId shouldBe GeoRegionCodes.SVERDLOVSK_REGION_CODE
      builder.getAutoruOffers.getOffersList.asScala.last.getOfferChangesHistoryRecordsList.asScala.head.getMileage.getMileage.getMileage shouldBe 100
      builder.getAutoruOffers.getOffersList.asScala.last.getOfferChangesHistoryRecordsList.asScala.head.getPrice.getPrice.getPrice shouldBe 3000f

    }

    "clear repair calculation costs only for dealers" in {
      val repairItems = (0 until 10).map(_ => randomRepairCalcItem)
      val report = RawVinReport.newBuilder
        .setRepairCalculations(
          RepairCalculationBlock.newBuilder
            .addAllCalculationRecords(repairItems.asJava)
        )
        .setHistory(
          HistoryBlock.newBuilder
            .addAllOwners(
              List(
                OwnerHistory.newBuilder
                  .addAllHistoryRecords(
                    repairItems.map(item => HistoryRecord.newBuilder.setRepairCalculationRecord(item).build).asJava
                  )
                  .build
              ).asJava
            )
        )
        .build

      var reportB = report.toBuilder
      manager.decayBasic(reportB, EssentialReportData.Empty, AdditionalReportData.Empty, clientId = None) // частник
      reportB.getRepairCalculations.getCalculationRecordsList.asScala.foreach(costsShouldBeClear)
      reportB.getHistory.getOwnersList.asScala
        .flatMap(_.getHistoryRecordsList.asScala)
        .map(_.getRepairCalculationRecord)
        .foreach(costsShouldBeClear)

      reportB = report.toBuilder
      manager.decayBasic(reportB, EssentialReportData.Empty, AdditionalReportData.Empty, clientId = Some(123)) // дилер
      reportB.getRepairCalculations.getCalculationRecordsList.asScala.foreach(rangesShouldBeClear)
      reportB.getHistory.getOwnersList.asScala
        .flatMap(_.getHistoryRecordsList.asScala)
        .map(_.getRepairCalculationRecord)
        .foreach(rangesShouldBeClear)
    }

    "clear estimate cost when has active offer owned by dealer with common head company" in {
      val item = buildEstimateItem(List(1234), 39)

      val builder = RawVinReport
        .newBuilder()
        .setEstimates(
          EstimateBlock
            .newBuilder()
            .addEstimateRecords(
              buildEstimateItem(List(1234), 39)
            )
        )
        .setHistory(
          HistoryBlock
            .newBuilder()
            .addOwners(
              OwnerHistory
                .newBuilder()
                .addHistoryRecords(
                  HistoryRecord
                    .newBuilder()
                    .setEstimateRecord(item)
                )
            )
        )

      manager.decayBasic(
        builder,
        EssentialReportData.Empty,
        AdditionalReportData.Empty,
        None
      )

      val actual = builder.getEstimates.getEstimateRecordsList.asScala.head.getResults
      actual.getPriceFrom shouldBe 0
      actual.getPriceTo shouldBe 0

      val timelineRecordActual = builder.getHistory.getOwners(0).getHistoryRecords(0).getEstimateRecord.getResults
      timelineRecordActual.getPriceFrom shouldBe 0
      timelineRecordActual.getPriceTo shouldBe 0
    }

    "clear estimate cost when has active offer owned by dealer without head company with same client_id" in {
      val item = buildEstimateItem(List(1234), 39)

      val builder = RawVinReport
        .newBuilder()
        .setEstimates(
          EstimateBlock
            .newBuilder()
            .addEstimateRecords(item)
        )
        .setHistory(
          HistoryBlock
            .newBuilder()
            .addOwners(
              OwnerHistory
                .newBuilder()
                .addHistoryRecords(
                  HistoryRecord
                    .newBuilder()
                    .setEstimateRecord(item)
                )
            )
        )

      manager.decayBasic(
        builder,
        EssentialReportData.Empty,
        AdditionalReportData.Empty,
        None
      )

      val actual = builder.getEstimates.getEstimateRecordsList.asScala.head.getResults
      actual.getPriceFrom shouldBe 0
      actual.getPriceTo shouldBe 0

      val timelineRecordActual = builder.getHistory.getOwners(0).getHistoryRecords(0).getEstimateRecord.getResults
      timelineRecordActual.getPriceFrom shouldBe 0
      timelineRecordActual.getPriceTo shouldBe 0
    }

    "show only fnp pledges on owner request" in {
      val builder = RawVinReport
        .newBuilder()
        .setPledge(
          PledgeBlock
            .newBuilder()
            .setNbkiStatus(Status.ERROR)
            .setStatus(Status.ERROR)
            .setFnpStatus(Status.OK)
            .build()
        )
        .setEstimates(
          EstimateBlock
            .newBuilder()
            .addEstimateRecords(
              buildEstimateItem(List(1234), 39)
            )
        )

      manager.decayFree(
        builder,
        AdditionalReportData.Empty.copy(requestFromOwner = true),
        EssentialReportData.Empty.copy(
          pledge = PreparedBlock[PreparedPledgeData](
            isUpdating = false,
            None,
            Some(
              PreparedPledgeData(
                status = VinInfoHistory.Status.ERROR,
                nbkiStatus = VinInfoHistory.Status.ERROR,
                fnpStatus = VinInfoHistory.Status.OK,
                Seq(),
                None,
                Seq()
              )
            )
          ),
          summary = summary
        )
      )

      builder.getPledge.getStatus shouldBe (Status.OK)
      builder.getPledge.getNbkiStatus shouldBe (Status.UNDEFINED)
      builder.getPledge.getFnpStatus shouldBe (Status.OK)

    }

    def costsShouldBeClear(item: RepairCalculationItem): Unit = {
      item.hasTotalCostRange shouldBe true
      item.hasColoringCostRange shouldBe true
      item.hasWorksCostRange shouldBe true
      item.hasPartsCostRange shouldBe true
      item.getTotalCost shouldBe 0
      item.getColoringCost shouldBe 0
      item.getWorksCost shouldBe 0
      item.getPartsCost shouldBe 0
      ()
    }

    def rangesShouldBeClear(item: RepairCalculationItem): Unit = {
      item.hasTotalCostRange shouldBe false
      item.hasColoringCostRange shouldBe false
      item.hasWorksCostRange shouldBe false
      item.hasPartsCostRange shouldBe false
      item.getTotalCost shouldNot be(0)
      item.getColoringCost shouldNot be(0)
      item.getWorksCost shouldNot be(0)
      item.getPartsCost shouldNot be(0)
      ()
    }
  }

  private def randomRepairCalcItem: RepairCalculationItem = {
    val totalCost = (math.random() * 10000000).toInt + 1
    val coloringCost = (math.random() * 10000000).toInt + 1
    val worksCost = (math.random() * 10000000).toInt + 1
    val partsCost = (math.random() * 10000000).toInt + 1
    RepairCalculationItem.newBuilder
      .setTotalCost(totalCost)
      .setColoringCost(coloringCost)
      .setWorksCost(worksCost)
      .setPartsCost(partsCost)
      .setTotalCostRange(CostRange(totalCost).toMessage)
      .setColoringCostRange(CostRange(coloringCost).toMessage)
      .setWorksCostRange(CostRange(worksCost).toMessage)
      .setPartsCostRange(CostRange(partsCost).toMessage)
      .build
  }

  private def buildEstimateItem(clientIds: List[Long], headCompanyId: Long): EstimateItem.Builder = {
    EstimateItem
      .newBuilder()
      .setResults(
        EstimateItem.Results
          .newBuilder()
          .setPriceFrom(100)
          .setPriceTo(200)
      )
      .setDate(System.currentTimeMillis())
      .setMeta(
        VinReportModel.RecordMeta
          .newBuilder()
          .setSource(
            VinReportModel.RecordMeta.SourceMeta
              .newBuilder()
              .setAutoruCompanyId(headCompanyId)
              .addAllAutoruClientIds(clientIds.map(Long.box).asJava)
          )
      )
  }

  def getOffer(geobaseId: Long, offerStatus: OfferStatus, sellerType: SellerType, price: Int): OfferRecord.Builder = {
    OfferRecord
      .newBuilder()
      .setGeobaseId(geobaseId)
      .setOfferStatus(offerStatus)
      .setSellerType(sellerType)
      .setPrice(price)
      .addAllOfferChangesHistoryRecords(
        Seq(
          OfferChangesHistoryRecord
            .newBuilder()
            .setPrice(
              OfferChangesHistoryRecord.PriceHistory
                .newBuilder()
                .setPrice(PriceInfo.newBuilder().setCurrency("rub").setPrice(3000f).setCreateTimestamp(10).build())
            )
            .setMileage(
              OfferChangesHistoryRecord.MileageHistory.newBuilder().setMileage(MileageInfo.newBuilder().setMileage(100))
            )
            .build()
        ).asJava
      )
  }
}
