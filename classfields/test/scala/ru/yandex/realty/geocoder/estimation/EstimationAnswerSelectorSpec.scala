package ru.yandex.realty.geocoder.estimation

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.SpecBase
import ru.yandex.realty.context.ProviderAdapter
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.geocoder.client.{Kind, Precision}
import ru.yandex.realty.geocoder.{GeoObjectStub, GeoObjectStubAccessor}
import ru.yandex.realty.graph.util.{Cut, NameSplitter}
import ru.yandex.realty.model.gen.location.GeoObjectGenerator
import ru.yandex.realty.model.raw.RawLocationExt

import scala.collection.JavaConverters._

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 08.02.16
  */
@RunWith(classOf[JUnitRunner])
class EstimationAnswerSelectorSpec extends SpecBase with RegionGraphTestComponents {

  "EstimationAnswerSelector " should {
    val nameSplitter = new NameSplitter(java.util.Collections.emptyList[Cut])
    val nameSplitterProvider: Provider[NameSplitter] = ProviderAdapter.create(nameSplitter)

    val estim = new EstimationAnswerSelector
    estim.setRegionGraphProvider(regionGraphProvider)
    val districtEstimator: DistrictEstimator = new DistrictEstimator
    districtEstimator.setNameSplitterProvider(nameSplitterProvider)
    districtEstimator.setRegionGraphProvider(regionGraphProvider)
    val sublocalityEstimator: SublocalityEstimator = new SublocalityEstimator
    sublocalityEstimator.setNameSplitterProvider(nameSplitterProvider)
    sublocalityEstimator.setRegionGraphProvider(regionGraphProvider)
    val estimators =
      Seq(districtEstimator, sublocalityEstimator, new ManualPointEstimator, new GeoTypePrecisionEstimator)
    estim.setEstimators(estimators.asJava)

    "choose valid geoObject " in {
      val geoObjectStub = GeoObjectStub(
        textOpt = Some(
          "Россия, Московская область, Раменский район, городское поселение Ильинский, посёлок городского типа Ильинский, улица 8 Марта, 1"
        ),
        precisionOpt = Some(Precision.EXACT),
        kindOpt = Some(Kind.HOUSE),
        pointOpt = Some("38.102097 55.613961"),
        envelopeLowerOpt = Some("38.093868 55.609304"),
        envelopeUpperOpt = Some("38.110325 55.618618"),
        countryNameOpt = Some("Россия"),
        addressLineOpt = Some(
          "Московская область, Раменский район, городское поселение Ильинский, посёлок городского типа Ильинский, улица 8 Марта, 1"
        ),
        administrativeAreaNameOpt = Some("Московская область"),
        subAdministrativeAreaNameOpt = Some("Раменский район"),
        localityNameOpt = Some("посёлок городского типа Ильинский"),
        thoroughfareNameOpt = Some("улица 8 Марта"),
        thoroughfarePredirectionOpt = Some(""),
        premiseNumberOpt = Some("1"),
        `typeOpt` = Some(""),
        geoidOpt = Some("213"),
        premiseNameOpt = Some(""),
        dependentLocalityNameOpt = Some(""),
        countryCodeOpt = Some("RU"),
        accuracyOpt = Some("1"),
        nameOpt = Some("улица 8 Марта, 1"),
        descriptionOpt = Some(
          "посёлок городского типа Ильинский, городское поселение Ильинский, Раменский район, Московская область, Россия"
        ),
        componentsOpt = Some(GeoObjectGenerator.generateMoscowComponents())
      )

      val rawLocation = new RawLocationExt()
      rawLocation.setAddress(
        "Россия, Московская область, Раменский район, городское поселение Ильинский, посёлок городского типа Ильинский, улица 8 Марта, 1"
      )
      rawLocation.setLatitude(55.623427f)
      rawLocation.setLongitude(38.112463f)

      val answer = estim.selectBest[GeoObjectStub](
        Seq(geoObjectStub).asJava,
        rawLocation,
        GeoObjectStubAccessor
      )

      answer shouldBe geoObjectStub
    }

    "do not fall with invalid geoobject " in {
      val geoObjectStub = GeoObjectStub(
        textOpt = Some(
          "Россия, Московская область, Московский аэропорт Домодедово им. М. В. Ломоносова"
        ),
        precisionOpt = Some(Precision.EXACT),
        kindOpt = Some(Kind.AIRPORT),
        pointOpt = Some("55.417867 37.893027")
      )
      val rawLocation = new RawLocationExt()
      rawLocation.setAddress(
        "Россия, Московская область, Домодедова 99афыв"
      )
      rawLocation.setLatitude(55.417867f)
      rawLocation.setLongitude(37.893027f)
      val answer = estim.selectBest[GeoObjectStub](
        Seq(geoObjectStub).asJava,
        rawLocation,
        GeoObjectStubAccessor
      )
      answer shouldBe null
    }
  }

}
