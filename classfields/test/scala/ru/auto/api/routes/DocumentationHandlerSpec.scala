package ru.auto.api.routes

import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.swagger.util.Yaml
import ru.auto.api.BaseSpec

import scala.jdk.CollectionConverters._

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 25.02.17
  */
class DocumentationHandlerSpec extends BaseSpec with ScalatestRouteTest {

  private val handler = new DocumentationHandler

  private val route: Route = pathPrefix("docs") {
    handler.route
  }

  "/docs/docs.yaml" should {
    "respond with swagger-yaml" in {
      Get("/docs/docs.yaml") ~> route ~>
        check {
          status shouldBe StatusCodes.OK

          contentType shouldBe ContentTypes.`text/plain(UTF-8)`
          val jsonNode = Yaml.mapper().reader().readTree(responseAs[String])
          jsonNode.get("swagger").asText() shouldBe "2.0"
          val fields = jsonNode.fieldNames().asScala.toList
          (fields should contain).allOf("basePath", "info", "tags", "paths", "definitions")
        }
    }
  }

  "generated doc" should {
    "not include stackTrace model" in {
      handler.swagger.getDefinitions.asScala.keys shouldNot contain("StackTraceElement")
    }
  }
}
