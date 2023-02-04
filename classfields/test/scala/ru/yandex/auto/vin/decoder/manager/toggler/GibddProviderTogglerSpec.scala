package ru.yandex.auto.vin.decoder.manager.toggler

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.vin.decoder.model.data_provider.GibddDataProvider
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.scheduler.models.{FromZeroDelay, WatchingStateUpdate}
import ru.yandex.auto.vin.decoder.utils.features.TrafficDistributionFeature.ProviderWeights
import ru.yandex.auto.vin.decoder.utils.random.WeightedSelector
import ru.yandex.vertis.feature.model.{Feature, FeatureType}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.language.implicitConversions

class GibddProviderTogglerSpec extends AnyWordSpecLike with MockitoSupport with Matchers {

  import GibddProviderTogglerSpec._
  private val feature = mock[Feature[ProviderWeights[GibddDataProvider]]]
  private val random: Map[_, Int] => Int = mock[Map[_, Int] => Int]
  private val weightedSelector = new WeightedSelector(random)
  private val toggler = new ProviderToggler(feature, weightedSelector)

  "GibddProviderToggler" should {

    "return autocode state if none other state present and weight > 0" in {
      when(feature.value).thenReturn("""{ "ADAPERIO": 25, "AUTOCODE": 25, "SCRAPING_HUB": 50 }""")
      when(random.apply(?)).thenReturn(24)
      val autocodeUpd = GibddDataProvider.AUTOCODE -> Some(compoundStateUpdate)
      val shUpd = GibddDataProvider.SCRAPING_HUB -> None
      val adaperioUpd = GibddDataProvider.ADAPERIO -> None
      val got = toggler.toggle(Map(adaperioUpd, autocodeUpd, shUpd))
      got.get shouldBe GibddDataProvider.AUTOCODE
    }

    "return autocode state if other weights are 0 even if non-empty updates from other providers" in {
      when(feature.value).thenReturn("""{ "ADAPERIO": 0, "AUTOCODE": 100, "SCRAPING_HUB": 0 }""")
      when(random.apply(?)).thenReturn(0)
      val autocodeUpd = GibddDataProvider.AUTOCODE -> Some(compoundStateUpdate)
      val shUpd = GibddDataProvider.SCRAPING_HUB -> Some(compoundStateUpdate)
      val adaperioUpd = GibddDataProvider.ADAPERIO -> Some(compoundStateUpdate)
      val got = toggler.toggle(Map(adaperioUpd, autocodeUpd, shUpd))
      got.get shouldBe GibddDataProvider.AUTOCODE
    }

    "return None state if nothing to update (even if weights > 0)" in {
      when(feature.value).thenReturn("""{ "ADAPERIO": 0, "AUTOCODE": 100, "SCRAPING_HUB": 0 }""")
      val autocodeUpd = GibddDataProvider.AUTOCODE -> None
      val shUpd = GibddDataProvider.SCRAPING_HUB -> None
      val adaperioUpd = GibddDataProvider.ADAPERIO -> None
      val got = toggler.toggle(Map(adaperioUpd, autocodeUpd, shUpd))
      got shouldBe None
    }

    "return None if autocode weight > 0, but cannot update over autocode (other weights are 0)" in {
      when(feature.value).thenReturn("""{ "ADAPERIO": 0, "AUTOCODE": 100, "SCRAPING_HUB": 0 }""")
      val autocodeUpd = GibddDataProvider.AUTOCODE -> None
      val shUpd = GibddDataProvider.SCRAPING_HUB -> Some(compoundStateUpdate)
      val adaperioUpd = GibddDataProvider.ADAPERIO -> Some(compoundStateUpdate)
      val got = toggler.toggle(Map(adaperioUpd, autocodeUpd, shUpd))
      got shouldBe None
    }

    "throw an ZeroProviderWeightsException then all configured weights of available providers are 0" in {
      when(feature.value).thenReturn("""{ "ADAPERIO": 100, "AUTOCODE": 0, "SCRAPING_HUB": 0 }""")
      val autocodeUpd = GibddDataProvider.AUTOCODE -> Some(compoundStateUpdate)
      val shUpd = GibddDataProvider.SCRAPING_HUB -> Some(compoundStateUpdate)
      assertThrows[ZeroProviderWeightsException](toggler.toggle(Map(autocodeUpd, shUpd)))
    }

    "return SCRAPING_HUB in lower case" in {
      when(feature.value).thenReturn("""{ "ADAPERIO": 10, "AUTOCODE": 20, "SCRAPING_HUB": 30 }""")
      when(random.apply(?)).thenReturn(30)
      val autocodeUpd = GibddDataProvider.AUTOCODE -> Some(compoundStateUpdate)
      val shUpd = GibddDataProvider.SCRAPING_HUB -> Some(compoundStateUpdate)
      val adaperioUpd = GibddDataProvider.ADAPERIO -> Some(compoundStateUpdate)
      val got = toggler.toggle(Map(adaperioUpd, autocodeUpd, shUpd))
      got.get shouldBe GibddDataProvider.SCRAPING_HUB
    }

    "return SCRAPING_HUB in upper case" in {
      when(feature.value).thenReturn("""{ "ADAPERIO": 10, "AUTOCODE": 20, "SCRAPING_HUB": 30 }""")
      when(random.apply(?)).thenReturn(59)
      val autocodeUpd = GibddDataProvider.AUTOCODE -> Some(compoundStateUpdate)
      val shUpd = GibddDataProvider.SCRAPING_HUB -> Some(compoundStateUpdate)
      val adaperioUpd = GibddDataProvider.ADAPERIO -> Some(compoundStateUpdate)
      val got = toggler.toggle(Map(adaperioUpd, autocodeUpd, shUpd))
      got.get shouldBe GibddDataProvider.SCRAPING_HUB
    }

    "return ADAPERIO in lower case" in {
      when(feature.value).thenReturn("""{ "ADAPERIO": 10, "AUTOCODE": 20, "SCRAPING_HUB": 30 }""")
      when(random.apply(?)).thenReturn(0)
      val autocodeUpd = GibddDataProvider.AUTOCODE -> Some(compoundStateUpdate)
      val shUpd = GibddDataProvider.SCRAPING_HUB -> Some(compoundStateUpdate)
      val adaperioUpd = GibddDataProvider.ADAPERIO -> Some(compoundStateUpdate)
      val got = toggler.toggle(Map(adaperioUpd, autocodeUpd, shUpd))
      got.get shouldBe GibddDataProvider.ADAPERIO
    }

    "return ADAPERIO in upper case" in {
      when(feature.value).thenReturn("""{ "ADAPERIO": 10, "AUTOCODE": 20, "SCRAPING_HUB": 30 }""")
      when(random.apply(?)).thenReturn(9)
      val autocodeUpd = GibddDataProvider.AUTOCODE -> Some(compoundStateUpdate)
      val shUpd = GibddDataProvider.SCRAPING_HUB -> Some(compoundStateUpdate)
      val adaperioUpd = GibddDataProvider.ADAPERIO -> Some(compoundStateUpdate)
      val got = toggler.toggle(Map(adaperioUpd, autocodeUpd, shUpd))
      got.get shouldBe GibddDataProvider.ADAPERIO
    }

    "return AUTOCODE in lower case" in {
      when(feature.value).thenReturn("""{ "ADAPERIO": 10, "AUTOCODE": 20, "SCRAPING_HUB": 30 }""")
      when(random.apply(?)).thenReturn(10)
      val autocodeUpd = GibddDataProvider.AUTOCODE -> Some(compoundStateUpdate)
      val shUpd = GibddDataProvider.SCRAPING_HUB -> Some(compoundStateUpdate)
      val adaperioUpd = GibddDataProvider.ADAPERIO -> Some(compoundStateUpdate)
      val got = toggler.toggle(Map(adaperioUpd, autocodeUpd, shUpd))
      got.get shouldBe GibddDataProvider.AUTOCODE
    }

    "return AUTOCODE in upper case" in {
      when(feature.value).thenReturn("""{ "ADAPERIO": 10, "AUTOCODE": 20, "SCRAPING_HUB": 30 }""")
      when(random.apply(?)).thenReturn(29)
      val autocodeUpd = GibddDataProvider.AUTOCODE -> Some(compoundStateUpdate)
      val shUpd = GibddDataProvider.SCRAPING_HUB -> Some(compoundStateUpdate)
      val adaperioUpd = GibddDataProvider.ADAPERIO -> Some(compoundStateUpdate)
      val got = toggler.toggle(Map(adaperioUpd, autocodeUpd, shUpd))
      got.get shouldBe GibddDataProvider.AUTOCODE
    }
  }
}

object GibddProviderTogglerSpec {

  import ru.yandex.auto.vin.decoder.utils.features.GibddProvidersTrafficDistributionFeature._

  private def compoundStateUpdate = new WatchingStateUpdate(CompoundState.newBuilder().build(), FromZeroDelay())

  implicit private def string2FractionContainer(json: String): ProviderWeights[GibddDataProvider] = {
    val des = implicitly[FeatureType[ProviderWeights[GibddDataProvider]]]
    des.serDe.deserialize(json).get
  }
}
