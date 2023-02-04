package ru.yandex.vertis.moderation.api

import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.model.headers.Location
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.api.v1.service.instance.InstanceHandler
import ru.yandex.vertis.moderation.util.HandlerSpecBase
import ru.yandex.vertis.moderation.view.ViewCompanion.MarshallingContext
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class ApiHandlerSpec extends HandlerSpecBase {

  override def basePath: String = ""

  import akka.http.scaladsl.model.StatusCodes.{NotFound, OK, SeeOther}

  "/ping" should {

    "return OK" in {
      Get(url("/ping")) ~> route ~> check {
        status shouldBe OK
      }
    }
  }

  "/swagger/" should {

    "return swagger html" in {
      Get(url("/swagger/")) ~> route ~> check {
        status shouldBe OK
        contentType shouldBe ContentTypes.`text/html(UTF-8)`
      }
    }

    "redirect from root" in {
      Get(url("/")) ~> route ~> check {
        status shouldBe SeeOther
        header(Location.name) shouldBe Some(Location("swagger/"))
      }
    }
  }

  "wrong path" should {

    "return 404" in {
      Get(url("/something-wrong")) ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  "instance" should {
    "reject mordaApproved without source" in {
      val entity =
        """
          |{
          |"mordaApproved": {
          | "potentiallyMordaApproved" : false,
          | "yanApproved": true
          |}
          |}
          |""".stripMargin

      Put("/api/1.x/REALTY/instance/yandex_uid_(23123)/123123/metadata")
        .withEntity(ContentTypes.`application/json`, entity) ~> route ~> check {

        status shouldBe BadRequest

        entityAs[String] should ===(InstanceHandler.MissingSourceMessage)
      }
    }

    "accept mordaApproved with source" in {
      val entity =
        """
          |{
          |"mordaApproved": {
          | "potentiallyMordaApproved" : false,
          | "yanApproved": true,
          | "updateSource": {
          |  "type": "automatic",
          |  "application": "HOBO",
          |  "tag": "tag"
          | }
          |}
          |}
          |""".stripMargin

      Put("/api/1.x/REALTY/instance/yandex_uid_(23123)/123123/metadata")
        .withEntity(ContentTypes.`application/json`, entity) ~> route ~> check {

        status shouldBe NotFound //Service fails but request is correctly passed to it
      }
    }

  }

  implicit override def marshallingContext: MarshallingContext = MarshallingContext(ServiceGen.next)
}
