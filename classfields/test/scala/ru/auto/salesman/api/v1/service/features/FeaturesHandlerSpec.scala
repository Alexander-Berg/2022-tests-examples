package ru.auto.salesman.api.v1.service.features

import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.StatusCodes._
import ru.auto.salesman.api.RoutingSpec
import ru.auto.salesman.api.SalesmanApiUtils.SalesmanHttpRequest
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.feature.impl.{BasicFeatureTypes, InMemoryFeatureRegistry}
import ru.yandex.vertis.feature.model.FeatureRegistry
import spray.json.JsArray

class FeaturesHandlerSpec extends RoutingSpec {

  val featureRegistry: FeatureRegistry =
    new InMemoryFeatureRegistry(BasicFeatureTypes)
  // different features for different tests to avoid test clash
  featureRegistry.register("immutable", initialValue = false)
  featureRegistry.register("mutable", initialValue = false)
  private val route = new FeaturesHandler(featureRegistry).route

  "Features handler" should {

    "get initial value of feature" in {
      Get("/") ~> seal(route) ~> check {
        val features = responseAs[JsArray].elements.map(_.asJsObject.fields)
        val feature =
          features.find(_("name").convertTo[String] == "immutable").value
        feature("type").convertTo[String] shouldBe "boolean"
        feature("value").convertTo[String] shouldBe "false"
      }
    }

    "set and then get feature" in {
      Put("/mutable")
        .withSalesmanTestHeader()
        .withEntity(ContentTypes.`application/json`, "true") ~> seal(
        route
      ) ~> check {
        status shouldBe OK
      }
      Get("/").withSalesmanTestHeader() ~> seal(route) ~> check {
        val features = responseAs[JsArray].elements.map(_.asJsObject.fields)
        val feature =
          features.find(_("name").convertTo[String] == "mutable").value
        feature("type").convertTo[String] shouldBe "boolean"
        feature("value").convertTo[String] shouldBe "true"
      }
    }

    "require salesman-user header for PUT" in {
      Put("/mutable").withEntity(
        ContentTypes.`application/json`,
        "true"
      ) ~> seal(route) ~> check {
        status shouldBe BadRequest
        responseAs[
          String
        ] shouldBe "Request is missing required HTTP header 'X-Salesman-User'"
      }
    }
  }
}
