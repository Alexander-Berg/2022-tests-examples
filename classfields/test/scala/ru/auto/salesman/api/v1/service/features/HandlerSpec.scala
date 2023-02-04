package ru.auto.salesman.api.v1.service.features

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.joda.time.DateTime
import ru.auto.salesman.api.v1.HandlerBaseSpec
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.test.BaseSpec
import ru.yandex.vertis.feature.impl.InMemoryFeatureRegistry
import ru.yandex.vertis.feature.model.FeatureRegistry
import spray.json.{DefaultJsonProtocol, JsArray}
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.auto.salesman.api.v1.SalesmanApiUtils._
import ru.auto.salesman.util.CustomFeatureTypes
import ru.auto.salesman.util.CustomFeatureTypes._

class HandlerSpec
    extends HandlerBaseSpec
    with BaseSpec
    with Directives
    with ScalatestRouteTest
    with SprayJsonSupport
    with DefaultJsonProtocol {

  override def featureRegistry: FeatureRegistry = {
    val r = new InMemoryFeatureRegistry(CustomFeatureTypes)
    // different features for different tests to avoid test clash
    r.register("immutable", initialValue = false)
    r.register("mutable", initialValue = false)
    r.register("mutableDate", initialValue = new DateTime())
    r
  }

  "Features handler" should {

    "get initial value of feature" in {
      Get("/api/1.x/service/autoru/features") ~> Route.seal(route) ~> check {
        val features = responseAs[JsArray].elements.map(_.asJsObject.fields)
        val feature =
          features.find(_("name").convertTo[String] == "immutable").value
        feature("type").convertTo[String] shouldBe "boolean"
        feature("value").convertTo[String] shouldBe "false"
      }
    }

    "set and then get feature" in {
      Put("/api/1.x/service/autoru/features/mutable")
        .withSalesmanTestHeader()
        .withEntity(ContentTypes.`application/json`, "true") ~> Route.seal(
        route
      ) ~> check {
        status shouldBe StatusCodes.OK
      }
      Get("/api/1.x/service/autoru/features")
        .withSalesmanTestHeader() ~> Route.seal(route) ~> check {
        val features = responseAs[JsArray].elements.map(_.asJsObject.fields)
        val feature =
          features.find(_("name").convertTo[String] == "mutable").value
        feature("type").convertTo[String] shouldBe "boolean"
        feature("value").convertTo[String] shouldBe "true"
      }
    }

    "require salesman-user header for PUT" in {
      Put("/api/1.x/service/autoru/features/mutable")
        .withEntity(ContentTypes.`application/json`, "true") ~> Route.seal(
        route
      ) ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[
          String
        ] shouldBe "Request is missing required HTTP header 'X-Salesman-User'"
      }
    }

    /*
     * When was resolved wrong unmarshaller(proto) for entity(as[String]), part of string after ":" was trimmed
     */
    val dt = "2019-06-03T18:45:34.754+03:00"
    "use full feature value" in {
      Put("/api/1.x/service/autoru/features/mutableDate")
        .withSalesmanTestHeader()
        .withEntity(ContentTypes.`application/json`, dt) ~> Route.seal(
        route
      ) ~> check {
        status shouldBe StatusCodes.OK
      }
      Get("/api/1.x/service/autoru/features")
        .withSalesmanTestHeader() ~> Route.seal(route) ~> check {
        val features = responseAs[JsArray].elements.map(_.asJsObject.fields)
        val feature =
          features.find(_("name").convertTo[String] == "mutableDate").value
        feature("type").convertTo[String] shouldBe "custom_dateTime"
        feature("value").convertTo[String] shouldBe dt
      }
    }
  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
