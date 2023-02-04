package ru.yandex.auto.vin.decoder.utils.sources

import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.cache.AudatexDealersCache
import ru.yandex.auto.vin.decoder.manager.vin.{FinesUpdateManager, TechInspectionUpdateManager}
import ru.yandex.auto.vin.decoder.model.data_provider.{FinesDataProvider, TechInspectionDataProvider}
import ru.yandex.auto.vin.decoder.model.{AudatexDealers, MockedFeatures, VinCode}
import ru.yandex.auto.vin.decoder.partners.audatex.Audatex.AudatexPartner
import ru.yandex.auto.vin.decoder.partners.checkburo.CheckburoUpdateManager
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.utils.features.TrafficDistributionFeature.ProviderWeights
import ru.yandex.auto.vin.decoder.utils.features.{
  FinesTrafficDistributionFeature,
  TechInspectionProvidersTrafficDistributionFeature
}
import ru.yandex.auto.vin.decoder.utils.sources.PartnerProgressManagerTest._
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.util.akka.http.protobuf.Protobuf.fromJson

import scala.io.Source

class PartnerProgressManagerTest extends AnyFunSuite with MockitoSupport with MockedFeatures {

  private val vin = VinCode("WBAUE11040E238571")

  when(features.AudatexAudaHistory).thenReturn(enabledFeature)
  when(features.AudatexFraudCheck).thenReturn(enabledFeature)
  when(features.AutocodeTaxi).thenReturn(enabledFeature)
  when(features.AutocodeFines).thenReturn(enabledFeature)
  when(features.Avtonomer).thenReturn(enabledFeature)
  when(features.Migalki).thenReturn(enabledFeature)
  when(features.BMW).thenReturn(enabledFeature)
  when(features.Carprice).thenReturn(enabledFeature)
  when(features.Filter).thenReturn(enabledFeature)
  when(features.FitService).thenReturn(enabledFeature)
  when(features.JLR).thenReturn(enabledFeature)
  when(features.Mazda).thenReturn(enabledFeature)
  when(features.Migtorg).thenReturn(enabledFeature)
  when(features.UremontMisc).thenReturn(enabledFeature)
  when(features.Mitsubishi).thenReturn(enabledFeature)
  when(features.Nissan).thenReturn(enabledFeature)
  when(features.RusAuto).thenReturn(enabledFeature)
  when(features.Suzuki).thenReturn(enabledFeature)
  when(features.SuzukiVehicle).thenReturn(enabledFeature)
  when(features.Wilgood).thenReturn(enabledFeature)
  when(features.AutocodeIdentifiersByVIN).thenReturn(enabledFeature)
  when(features.EnableMegaParserRsaVinWorker).thenReturn(enabledFeature)
  when(features.EnableMegaParserRsaLPWorker).thenReturn(enabledFeature)

  when(features.TechInspectionProvidersTrafficDistributionForDealers).thenReturn(
    Feature[ProviderWeights[TechInspectionDataProvider]](
      TechInspectionProvidersTrafficDistributionFeature.nameForDealers,
      _ =>
        Map(
          TechInspectionDataProvider.AUTOCODE -> 0,
          TechInspectionDataProvider.ADAPERIO -> 100
        )
    )
  )

  when(features.FinesForUsersTrafficDistribution).thenReturn(
    Feature[ProviderWeights[FinesDataProvider]](
      FinesTrafficDistributionFeature.nameForUsers,
      _ =>
        Map(
          FinesDataProvider.AUTOCODE -> 50,
          FinesDataProvider.CHECKBURO -> 50
        )
    )
  )

  when(features.FinesForDealersTrafficDistribution).thenReturn(
    Feature[ProviderWeights[FinesDataProvider]](
      FinesTrafficDistributionFeature.nameForDealers,
      _ =>
        Map(
          FinesDataProvider.AUTOCODE -> 0,
          FinesDataProvider.CHECKBURO -> 100
        )
    )
  )

  when(features.TechInspectionProvidersTrafficDistributionForUsers).thenReturn(
    Feature[ProviderWeights[TechInspectionDataProvider]](
      TechInspectionProvidersTrafficDistributionFeature.nameForUsers,
      _ =>
        Map(
          TechInspectionDataProvider.AUTOCODE -> 50,
          TechInspectionDataProvider.ADAPERIO -> 50
        )
    )
  )
  when(features.CheckburoFines).thenReturn(enabledFeature)

  val audatexDealersCache = mock[AudatexDealersCache]

  when(audatexDealersCache.get).thenReturn(AudatexDealers(List.empty[AudatexPartner]))

  val partnerProgressManager = new PartnerProgressManager(
    new TechInspectionUpdateManager(new CheckburoUpdateManager, features),
    new FinesUpdateManager(new CheckburoUpdateManager, features),
    audatexDealersCache,
    features
  )

  test("not ready count") {
    val state = fromJson(CompoundState.getDefaultInstance, notReadyStateJson)
    val counter = partnerProgressManager.buildReportProgress(vin, Some(state), None, Some("MAZDA"), None).fullProgress

    assert(counter.total == 96)
    assert(counter.ready == 92)
  }

  test("not ready count, when autocode is ready but fresher mp requests are not") {
    val state = fromJson(CompoundState.getDefaultInstance, autocodeReadyAndFresherShNotReadyStateJson)
    val counter = partnerProgressManager.buildReportProgress(vin, Some(state), None, Some("MAZDA"), None).fullProgress

    assert(counter.total == 96)
    assert(counter.ready == 92)
  }

  test("ready count, when mp requests are ready and autocode main is not ready") {
    val state = fromJson(CompoundState.getDefaultInstance, autocodeNotReadyAndFresherShReadyStateJson)
    val counter = partnerProgressManager.buildReportProgress(vin, Some(state), None, Some("MAZDA"), None).fullProgress

    assert(counter.total == 96)
    assert(counter.ready == 96)
  }

  test("ready count") {
    val state = fromJson(CompoundState.getDefaultInstance, readyStateJson)
    val counter = partnerProgressManager.buildReportProgress(vin, Some(state), None, Some("MAZDA"), None).fullProgress

    assert(counter.total == 96)
    assert(counter.ready == 96)
  }

  test("Empty blocks should not lead to non-ready sources") {
    val state = fromJson(CompoundState.getDefaultInstance, havingEmptyBlocksJson)
    val counter = partnerProgressManager.buildReportProgress(vin, Some(state), None, None, None).fullProgress

    assert(counter.total == 97)
    assert(counter.ready == 96)
  }

}

object PartnerProgressManagerTest {

  private val havingEmptyBlocksJson: String = load("/source_counter/response_with_empty_blocks.json")

  private val notReadyStateJson: String = load("/source_counter/not_ready_state.json")

  private val readyStateJson: String = load("/source_counter/ready_state.json")

  private val autocodeReadyAndFresherShNotReadyStateJson: String = load("/source_counter/complex_state_1.json")

  private val autocodeNotReadyAndFresherShReadyStateJson: String = load("/source_counter/complex_state_2.json")

  private def load(fileName: String) = {
    val source = Source.fromInputStream(getClass.getResourceAsStream(fileName))
    val lines = source.getLines().mkString
    source.close()
    lines
  }
}
