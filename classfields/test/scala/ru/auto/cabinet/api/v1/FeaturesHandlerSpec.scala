package ru.auto.cabinet.api.v1

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes._
import org.scalatest.OptionValues
import org.scalatest.flatspec.AnyFlatSpec
import ru.yandex.vertis.feature.impl.BasicFeatureTypes.BooleanFeatureType
import ru.yandex.vertis.feature.impl.{
  BasicFeatureTypes,
  InMemoryFeatureRegistry
}
import spray.json._

class FeaturesHandlerSpec
    extends AnyFlatSpec
    with HandlerSpecTemplate
    with SprayJsonSupport
    with OptionValues {

  private val featureRegistry =
    new InMemoryFeatureRegistry(BasicFeatureTypes)
  // different features for different tests to avoid test clash
  featureRegistry.register("immutable", initialValue = false)
  featureRegistry.register("mutable", initialValue = false)
  private val route = new FeaturesHandler(featureRegistry).route

  "/" should "get initial value of feature" in {
    Get("/features") ~> route ~> check {
      val features = responseAs[JsArray].elements.map(_.asJsObject.fields)
      val feature =
        features.find(_("name").convertTo[String] == "immutable").value
      feature("type").convertTo[String] shouldBe "boolean"
      feature("value").convertTo[String] shouldBe "false"
    }
  }

  "/" should "set and then get feature" in {
    Put("/features/mutable")
      .withEntity("true") ~> route ~> check {
      status shouldBe OK
    }
    Get("/features") ~> route ~> check {
      val features = responseAs[JsArray].elements.map(_.asJsObject.fields)
      val feature =
        features.find(_("name").convertTo[String] == "mutable").value
      feature("type").convertTo[String] shouldBe "boolean"
      feature("value").convertTo[String] shouldBe "true"
    }
  }

}
