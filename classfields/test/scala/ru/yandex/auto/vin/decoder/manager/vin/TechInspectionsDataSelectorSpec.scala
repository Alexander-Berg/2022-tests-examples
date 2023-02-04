package ru.yandex.auto.vin.decoder.manager.vin

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.manager.offer.VosOffers
import ru.yandex.auto.vin.decoder.manager.vin.adaperio.AdaperioDataSelector
import ru.yandex.auto.vin.decoder.manager.vin.autocode.AutocodeDataSelector
import ru.yandex.auto.vin.decoder.manager.vin.checkburo.CheckburoDataSelector
import ru.yandex.auto.vin.decoder.model.data_provider.TechInspectionDataProvider
import ru.yandex.auto.vin.decoder.model.{MockedFeatures, VinCode}
import ru.yandex.auto.vin.decoder.proto.VinHistory.{DiagnosticCard, Mileage, VinInfoHistory}
import ru.yandex.auto.vin.decoder.utils.features.TechInspectionProvidersTrafficDistributionFeature
import ru.yandex.auto.vin.decoder.utils.features.TrafficDistributionFeature.ProviderWeights
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager.Prepared
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters.{IterableHasAsJava, ListHasAsScala}

class TechInspectionsDataSelectorSpec extends AnyWordSpecLike with Matchers with MockitoSupport with MockedFeatures {

  private val selector = new TechInspectionsDataSelector(
    new AutocodeDataSelector,
    new AdaperioDataSelector,
    new CheckburoDataSelector,
    features
  )

  private val TestVin: VinCode = VinCode.apply("WP1ZZZ92ZGLA80455")

  val autocodeMileages = List(
    Mileage.newBuilder.setDate(100).build(),
    Mileage.newBuilder.setDate(200).build()
  )

  val adaperioMileages = List(
    Mileage.newBuilder.setDate(300).build(),
    Mileage.newBuilder.setDate(400).build(),
    Mileage.newBuilder.setDate(500).build()
  )

  val checkburoMileages = List(
    Mileage.newBuilder.setDate(200).build(),
    Mileage.newBuilder.setDate(300).build(),
    Mileage.newBuilder.setDate(400).build()
  )

  val autocodeDiagnosticCards = List(
    DiagnosticCard.newBuilder.setFrom(100).build(),
    DiagnosticCard.newBuilder.setFrom(200).build(),
    DiagnosticCard.newBuilder.setFrom(300).build()
  )

  val adaperioDiagnosticCards = List(
    DiagnosticCard.newBuilder.setFrom(400).build(),
    DiagnosticCard.newBuilder.setFrom(500).build()
  )

  val checkburoDiagnosticCards = List(
    DiagnosticCard.newBuilder.setFrom(100).build(),
    DiagnosticCard.newBuilder.setFrom(200).build(),
    DiagnosticCard.newBuilder.setFrom(300).build()
  )

  "TechInspectionsDataSelector" should {

    "get latest tech inspections for users" in {

      val res = selector.getTechInspections(techInspectionsData, isForDealer = false)

      res.get.data.getMileageList.size() shouldBe 2
      res.get.data.getMileageList.asScala shouldBe autocodeMileages

      res.get.data.getDiagnosticCardsList.size() shouldBe 3
      res.get.data.getDiagnosticCardsList.asScala shouldBe autocodeDiagnosticCards
    }

    "get latest tech inspections for dealers from adaperio when other sources prohibited even if there is fresher autocode data" in {

      when(features.TechInspectionProvidersTrafficDistributionForDealers).thenReturn(
        Feature[ProviderWeights[TechInspectionDataProvider]](
          TechInspectionProvidersTrafficDistributionFeature.nameForDealers,
          _ =>
            Map(
              TechInspectionDataProvider.AUTOCODE -> 0,
              TechInspectionDataProvider.ADAPERIO -> 100,
              TechInspectionDataProvider.CHECKBURO -> 0
            )
        )
      )

      val res = selector.getTechInspections(techInspectionsData, isForDealer = true)

      res.get.data.getMileageList.size() shouldBe 3
      res.get.data.getMileageList.asScala shouldBe adaperioMileages

      res.get.data.getDiagnosticCardsList.size() shouldBe 2
      res.get.data.getDiagnosticCardsList.asScala shouldBe adaperioDiagnosticCards
    }

    "get latest tech inspections for dealers when autocode is allowed" in {

      when(features.TechInspectionProvidersTrafficDistributionForDealers).thenReturn(
        Feature[ProviderWeights[TechInspectionDataProvider]](
          TechInspectionProvidersTrafficDistributionFeature.nameForDealers,
          _ =>
            Map(
              TechInspectionDataProvider.AUTOCODE -> 50,
              TechInspectionDataProvider.ADAPERIO -> 50,
              TechInspectionDataProvider.CHECKBURO -> 50
            )
        )
      )

      val res = selector.getTechInspections(techInspectionsData, isForDealer = true)

      res.get.data.getMileageList.size() shouldBe 2
      res.get.data.getMileageList.asScala shouldBe autocodeMileages

      res.get.data.getDiagnosticCardsList.size() shouldBe 3
      res.get.data.getDiagnosticCardsList.asScala shouldBe autocodeDiagnosticCards
    }
  }

  private def techInspectionsData = {

    val autocodeTechInspections = {
      val builder = VinInfoHistory.newBuilder()
      builder.getStatusesBuilder
        .setMileagesStatus(VinInfoHistory.Status.OK)
        .setDiagnosticCardsStatus(VinInfoHistory.Status.OK)
      builder.addAllMileage(autocodeMileages.asJava).addAllDiagnosticCards(autocodeDiagnosticCards.asJava)

      Prepared(200, 200, 200, builder.build(), "")
    }

    val adaperioTechInspections = {
      val builder = VinInfoHistory.newBuilder()
      builder.getStatusesBuilder
        .setMileagesStatus(VinInfoHistory.Status.OK)
        .setDiagnosticCardsStatus(VinInfoHistory.Status.OK)
      builder.addAllMileage(adaperioMileages.asJava).addAllDiagnosticCards(adaperioDiagnosticCards.asJava)

      Prepared(100, 100, 100, builder.build(), "")
    }

    val checkburoTechInspections = {
      val builder = VinInfoHistory.newBuilder()
      builder.getStatusesBuilder
        .setMileagesStatus(VinInfoHistory.Status.OK)
        .setDiagnosticCardsStatus(VinInfoHistory.Status.OK)
      builder.addAllMileage(checkburoMileages.asJava).addAllDiagnosticCards(checkburoDiagnosticCards.asJava)

      Prepared(50, 50, 50, builder.build(), "")
    }

    VinData(
      vinCode = TestVin,
      mysqlData = Map.empty,
      rawStorageData = Map(
        EventType.AUTOCODE_MILEAGE -> List(autocodeTechInspections),
        EventType.ADAPERIO_MILEAGE -> List(adaperioTechInspections),
        EventType.CHECKBURO_MILEAGE -> List(checkburoTechInspections)
      ),
      offers = VosOffers.Empty
    )
  }
}
