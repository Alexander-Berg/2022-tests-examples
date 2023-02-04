package ru.yandex.realty.geocoder.estimation

import java.util
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.geocoder.{GeoObjectStub, GeoObjectStubAccessor}
import ru.yandex.realty.model.gen.location.GeoObjectGenerator
import ru.yandex.realty.model.raw.RawLocationExt
import ru.yandex.vertis.generators.ProducerProvider._

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class GeoTypePrecisionEstimatorSpec extends SpecBase {

  val estimator = new GeoTypePrecisionEstimator()

  "GeoTypePrecisionEstimator " should {
    val exactObject = GeoObjectStub(componentsOpt = Some(GeoObjectGenerator.exactComponentsGen().next))
    val inexactObject = GeoObjectStub(componentsOpt = Some(GeoObjectGenerator.inexactComponentsGen().next))
    "choose more precision geoObject" in {
      val rawLocation = new RawLocationExt
      rawLocation.setAddress("Россия, проспект Просвещения, 43")
      rawLocation.setLatitude(60.04536f)
      rawLocation.setLongitude(30.3638f)
      val objects = Seq(
        exactObject,
        inexactObject
      )
      val results: util.Map[GeoObjectStub, Integer] =
        estimator.estimate(objects.asJava, rawLocation, GeoObjectStubAccessor)
      results.get(exactObject) < results.get(inexactObject) shouldBe true
    }

    "return null if manual point is not defined " in {
      val rawLocation = new RawLocationExt
      rawLocation.setAddress("Россия, проспект Просвещения, 43")
      val objects = Seq(
        exactObject,
        inexactObject
      )
      estimator.estimate(objects.asJava, rawLocation, GeoObjectStubAccessor) shouldBe null
    }
  }
}
