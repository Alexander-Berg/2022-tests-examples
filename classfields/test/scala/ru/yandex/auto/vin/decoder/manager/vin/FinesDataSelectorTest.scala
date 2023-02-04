package ru.yandex.auto.vin.decoder.manager.vin

import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.manager.offer.VosOffers
import ru.yandex.auto.vin.decoder.manager.vin.autocode.AutocodeDataSelector
import ru.yandex.auto.vin.decoder.manager.vin.checkburo.CheckburoDataSelector
import ru.yandex.auto.vin.decoder.model.data_provider.FinesDataProvider
import ru.yandex.auto.vin.decoder.model.{MockedFeatures, Sts, VinCode}
import ru.yandex.auto.vin.decoder.proto.VinHistory
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import ru.yandex.auto.vin.decoder.utils.features.FinesTrafficDistributionFeature
import ru.yandex.auto.vin.decoder.utils.features.TrafficDistributionFeature.ProviderWeights
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager.Prepared
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

class FinesDataSelectorTest extends AnyWordSpecLike with MockitoSupport with MockedFeatures {

  private val TestVin: VinCode = VinCode("WP1ZZZ92ZGLA80455")

  val selector = new FinesDataSelector(new AutocodeDataSelector, new CheckburoDataSelector, features)

  def sampleFines = VinInfoHistory
    .newBuilder()

  def buildFines(sts: String, et: EventType) = {
    sampleFines
      .setGroupId(sts)
      .setEventType(et)
      .addFines(VinHistory.Fine.newBuilder())
      .build()

  }

  "FinesDataSelector" should {
    "convert fines from both providers, keeping only most relevant data" in {
      val fines = Map(
        EventType.CHECKBURO_FINES -> List(
          Prepared
            .simulate(buildFines("3", EventType.CHECKBURO_FINES), 1)
            .copy(groupId = "3")
        ),
        EventType.AUTOCODE_FINES -> List(
          Prepared.simulate(buildFines("123", EventType.AUTOCODE_FINES), 2).copy(groupId = "123"),
          Prepared.simulate(buildFines("234", EventType.AUTOCODE_FINES), 3).copy(groupId = "234"),
          Prepared.simulate(buildFines("234", EventType.AUTOCODE_FINES), 4).copy(groupId = "234")
        )
      )
      val data = VinData(
        vinCode = TestVin,
        mysqlData = Map.empty,
        rawStorageData = fines,
        offers = VosOffers.Empty
      )
      val combinedFines = selector.getFines(data, isForDealer = false)
      assert(combinedFines.size == 3)
      assert(Set(Sts("123"), Sts("234"), Sts("3")) == combinedFines.keySet)
      val initiallyDuplicatingFine = combinedFines
        .collect { case (sts, prepared) if sts == Sts("234") => prepared.timestampUpdate }
      assert(initiallyDuplicatingFine.size == 1)
      assert(initiallyDuplicatingFine.lastOption.get == 4)
    }

    "convert fines only from checkburo if report is requested by dealer with disabled dealer feature" in {
      val fines = Map(
        EventType.CHECKBURO_FINES -> List(
          Prepared
            .simulate(buildFines("3", EventType.CHECKBURO_FINES), 1)
            .copy(groupId = "3")
        ),
        EventType.AUTOCODE_FINES -> List(
          Prepared.simulate(buildFines("123", EventType.AUTOCODE_FINES), 2).copy(groupId = "123"),
          Prepared.simulate(buildFines("234", EventType.AUTOCODE_FINES), 3).copy(groupId = "234")
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
      val data = VinData(
        vinCode = TestVin,
        mysqlData = Map.empty,
        rawStorageData = fines,
        offers = VosOffers.Empty
      )
      val combinedFines = selector.getFines(data, isForDealer = true)
      assert(combinedFines.size == 1)
      assert(combinedFines.head._1.sts == "3")
    }
  }
}
