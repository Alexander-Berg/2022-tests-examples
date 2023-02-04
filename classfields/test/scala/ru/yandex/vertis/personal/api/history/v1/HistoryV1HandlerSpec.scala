package ru.yandex.vertis.personal.api.history.v1

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import ru.yandex.vertis.personal.ServiceRegistry
import ru.yandex.vertis.personal.api.history.backend.MockHistoryBackend
import ru.yandex.vertis.personal.api.history.v1.model.{Entity, EntityResponse, GetResponse}
import ru.yandex.vertis.personal.api.model.SuccessResponse
import ru.yandex.vertis.personal.history.HistoryBackend
import ru.yandex.vertis.personal.model.{Domains, Services, SingleRef, UserRef}
import ru.yandex.vertis.personal.util.HandlerSpec
import spray.json.{DefaultJsonProtocol, JsValue}

class HistoryV1HandlerSpec extends HandlerSpec with SprayJsonSupport with DefaultJsonProtocol {
  val version = "1.0"
  val service: Services.Value = Services.Autoru
  val domain: Domains.Value = Domains.CarOffers
  val carOffersDomain = SingleRef(service, domain)
  val firstUser = UserRef("autoruuid:123")
  val secondUser = UserRef("uid:321")
  val historyBackend = new MockHistoryBackend(service, domain)

  val registry = new ServiceRegistry[HistoryBackend]
  registry.register(historyBackend)
  val backend = new HistoryV1Backend(registry)

  val firstEntityId = "0"
  val firstEntityPayload = "test"
  val secondEntityId = "1"
  val secondEntityPayload = "test"

  def route(user: UserRef): Route = {
    sealRoute(new HistoryV1Handler(carOffersDomain, user, backend).routes)
  }

  val firstUserRoute: Route = route(firstUser)
  val secondUserRoute: Route = route(secondUser)

  private def getEntities(route: Route): Seq[Entity] = {
    Get() ~> firstUserRoute ~> check {
      status shouldBe StatusCodes.OK
      val entitiesMap = responseAs[GetResponse].entities
      entitiesMap.getOrElse(domain.toString, Seq.empty)
    }
  }

  private def getEntity(route: Route): Entity = {
    val entities = getEntities(route)
    entities.size shouldBe 1
    entities.head
  }

  private def putEntity(route: Route, id: String, payload: String): EntityResponse = {
    Put(s"/$id").withEntity(payload) ~> firstUserRoute ~> check {
      status shouldBe StatusCodes.OK
      responseAs[EntityResponse]
    }
  }

  "HistoryV1Handler" should {
    "create user's history" in {
      putEntity(firstUserRoute, firstEntityId, firstEntityPayload) match {
        case EntityResponse(
            "OK",
            Entity(`firstEntityId`, _, 1, Some(`firstEntityPayload`))
            ) =>
          ()
        case other =>
          fail(s"Unexpected $other")
      }
    }
    "correctly update entity" when {
      "repeat put" in {
        val entity = getEntity(firstUserRoute) match {
          case e @ Entity(`firstEntityId`, _, 1, Some(`firstEntityPayload`)) =>
            e
          case other =>
            fail(s"Unexpected $other")
        }
        putEntity(firstUserRoute, firstEntityId, firstEntityPayload) match {
          case EntityResponse(
              "OK",
              e @ Entity(`firstEntityId`, ts, count, Some(`firstEntityPayload`))
              ) =>
            if (entity.visit_timestamp < ts && count > entity.add_count) {
              ()
            } else {
              fail(s"Unexpected entity $e")
            }
          case other =>
            fail(s"Unexpected response $other")
        }
      }
      "call update" in {
        val entity = getEntity(firstUserRoute)
        val changedEntityPayload = s"changed_${entity.payload.get}"
        Patch(s"/$firstEntityId")
          .withEntity(changedEntityPayload) ~> firstUserRoute ~> check {
          status shouldBe StatusCodes.OK
          responseAs[JsValue] shouldBe SuccessResponse.InnerJson
        }
        val expectedEntity = entity.copy(payload = Some(changedEntityPayload))
        getEntity(firstUserRoute) shouldBe expectedEntity
      }
    }
    "move user's items" in {
      val entities = getEntities(firstUserRoute)
      Post("/move/uid:321") ~> firstUserRoute ~> check {
        status shouldBe StatusCodes.OK
        responseAs[JsValue] shouldBe SuccessResponse.InnerJson
      }
      Get() ~> secondUserRoute ~> check {
        status shouldBe StatusCodes.OK
        val entitiesMap = Map(domain.toString -> entities)
        val expected =
          GetResponse(service.toString, "1.0", secondUser.toString, entitiesMap)
        responseAs[GetResponse] shouldBe expected
      }
    }
    "delete one element" in {
      putEntity(firstUserRoute, firstEntityId, firstEntityPayload)
      val entity = getEntity(firstUserRoute)
      putEntity(firstUserRoute, secondEntityId, secondEntityPayload) match {
        case EntityResponse(
            "OK",
            Entity(`secondEntityId`, _, 1, Some(`secondEntityPayload`))
            ) =>
          ()
        case other =>
          fail(s"Unexpected $other")
      }
      getEntities(firstUserRoute).size shouldBe 2
      Delete(s"/$secondEntityId") ~> firstUserRoute ~> check {
        status shouldBe StatusCodes.OK
        responseAs[JsValue] shouldBe SuccessResponse.InnerJson
      }
      getEntities(firstUserRoute) shouldBe Seq(entity)
    }
    "clear entities" in {
      putEntity(firstUserRoute, secondEntityId, secondEntityPayload)
      getEntities(firstUserRoute).size shouldBe 2
      Delete() ~> firstUserRoute ~> check {
        status shouldBe StatusCodes.OK
        responseAs[JsValue] shouldBe SuccessResponse.InnerJson
      }
      getEntities(firstUserRoute).size shouldBe 0
    }
    "forget user" in {
      putEntity(firstUserRoute, firstEntityId, firstEntityPayload)
      putEntity(firstUserRoute, secondEntityId, secondEntityPayload)
      getEntities(firstUserRoute).size shouldBe 2
      Delete("/forget") ~> firstUserRoute ~> check {
        status shouldBe StatusCodes.OK
        responseAs[JsValue] shouldBe SuccessResponse.InnerJson
      }
      getEntities(firstUserRoute).size shouldBe 0
    }
  }
}
