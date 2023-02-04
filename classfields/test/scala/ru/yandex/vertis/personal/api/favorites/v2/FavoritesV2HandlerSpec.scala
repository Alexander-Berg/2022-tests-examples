package ru.yandex.vertis.personal.api.favorites.v2

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import org.joda.time.DateTime
import ru.yandex.vertis.personal.ServiceRegistry
import ru.yandex.vertis.personal.api.favorites.backend.MockFavoritesBackend
import ru.yandex.vertis.personal.api.favorites.v2.model.{Entity, GetResponse}
import ru.yandex.vertis.personal.favorites.FavoritesBackend2
import ru.yandex.vertis.personal.model.favorites.FavoriteItem
import ru.yandex.vertis.personal.model.{Domains, Services, SingleRef, UserRef, WildcardRef}
import ru.yandex.vertis.personal.util.HandlerSpec
import spray.json.{enrichString, DefaultJsonProtocol, JsValue}

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 29.06.16
  */
class FavoritesV2HandlerSpec extends HandlerSpec with SprayJsonSupport with DefaultJsonProtocol {
  val service = Services.Autoru
  val searches = new MockFavoritesBackend(service, Domains.Searches)
  val cards = new MockFavoritesBackend(service, Domains.Cards)

  val registry = new ServiceRegistry[FavoritesBackend2]
  registry.register(searches)
  registry.register(cards)
  val backend = new FavoritesV2Backend(registry)

  val searchesDomain = SingleRef(Services.Autoru, Domains.Searches)
  val cardsDomain = SingleRef(Services.Autoru, Domains.Cards)
  val user = UserRef("uid:1")

  val item1 = FavoriteItem(
    "item1",
    DateTime.parse("2016-05-20T19:00:00.000Z"),
    DateTime.parse("2016-05-20T19:21:00.000Z"),
    Some("{\"a\": \"b\"}")
  )

  val item2 = FavoriteItem(
    "item2",
    DateTime.parse("2016-06-20T19:00:00.000Z"),
    DateTime.parse("2016-06-20T19:21:00.000Z"),
    Some("")
  )
  searches.create(searchesDomain, user, item1).futureValue
  cards.create(cardsDomain, user, item2).futureValue

  val wildcardDomain = WildcardRef(Services.Autoru)

  val searchesRoute = sealRoute(
    new FavoritesV2Handler(searchesDomain, user, backend).routes
  )

  val cardsRoute = sealRoute(
    new FavoritesV2Handler(cardsDomain, user, backend).routes
  )

  val wildcardRoute = sealRoute(
    new FavoritesV2Handler(wildcardDomain, user, backend).routes
  )

  "FavoritesV2Handler" should {
    "get user's favorites for single domain" in {
      val expectedStr =
        """{
          |  "service":"autoru",
          |  "version":"2.0",
          |  "user":"uid:1",
          |  "entities": {
          |    "searches": [
          |      {
          |        "entity_id":"item1",
          |        "create_timestamp":1463770800000,
          |        "update_timestamp":1463772060000,
          |        "payload":"{\"a\": \"b\"}"
          |      }
          |    ]
          |  }
          |}"""

      val expected = expectedStr.stripMargin.parseJson.compactPrint

      Get() ~> searchesRoute ~> check {
        status shouldBe StatusCodes.OK
        responseAs[JsValue].compactPrint shouldBe expected
      }
    }

    "get user's favorites for many domains" in {
      val expectedStr =
        """{
          |  "service":"autoru",
          |  "version":"2.0",
          |  "user":"uid:1",
          |  "entities":{
          |    "searches":[
          |      {"entity_id":"item1","create_timestamp":1463770800000,"update_timestamp":1463772060000,
          |      "payload":"{\"a\": \"b\"}"}
          |    ],
          |    "cards":[
          |      {"entity_id":"item2","create_timestamp":1466449200000,"update_timestamp":1466450460000,
          |      "payload":""}
          |    ]
          |  }
          |}"""

      val expected = expectedStr.stripMargin.parseJson.compactPrint

      Get() ~> wildcardRoute ~> check {
        status shouldBe StatusCodes.OK
        responseAs[JsValue].compactPrint shouldBe expected
      }
    }

    "put many user's favorites" in {
      val rawJson = """[{
                      |  "entity_id":"123456",
                      |  "payload":"payload123456"
                      |  },{
                      |  "entity_id":"654321"
                      |  }
                      |]""".stripMargin
      Put()
        .withEntity(ContentTypes.`application/json`, rawJson) ~> searchesRoute ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[GetResponse]
        response.entities.get("searches") match {
          case Some(entities) =>
            entities.exists(_.entity_id == "123456") shouldBe true
            entities.exists(_.entity_id == "654321") shouldBe true
          case None => fail("Expected 'searches' domain")
        }
      }
    }

    "put one user's favorite" in {
      Put(s"/123456", "payload123456") ~> searchesRoute ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[GetResponse]
        response.entities.get("searches") match {
          case Some(entities) =>
            entities.exists(_.entity_id == "123456") shouldBe true
          case None => fail("Expected 'searches' domain")
        }
      }
    }

    "updates user's favorites" in {
      Put(s"/item3") ~> searchesRoute ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[GetResponse]
        response.entities.get("searches") match {
          case Some(entities) =>
            entities.exists(_.entity_id == "item3") should be(true)
          case None => fail("Expected 'searches' domain")
        }
      }
    }

    "copy only 'search' domain" in {
      val dest = UserRef("uid:2")
      Post(s"/copy/$dest") ~> searchesRoute ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[GetResponse]
        response.user shouldBe dest.toPlain
        response.entities.get("searches") match {
          case Some(entities) if entities.nonEmpty =>
          case _ => fail(s"Expected 'searches' domain, there is no this one")
        }
        response.entities.get("cards") match {
          case Some(entities) if entities.isEmpty =>
          case None =>
          case other =>
            fail(
              s"Expected empty 'cards' domain after move only 'searches', but got $other"
            )
        }
      }
    }

    "copy all domains" in {
      val dest = UserRef("uid:3")
      Post(s"/copy/$dest") ~> wildcardRoute ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[GetResponse]
        response.user shouldBe dest.toPlain
        response.entities.get("searches") match {
          case Some(entities) if entities.nonEmpty =>
          case _ => fail("Expected 'searches' domain, there is no this one")
        }
        response.entities.get("cards") match {
          case Some(entities) if entities.nonEmpty =>
          case _ => fail("Expected 'cards' domain, there is no this one")
        }
      }
    }

    "deletes particular user's favorites" in {
      Delete(s"/item3") ~> searchesRoute ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[GetResponse]
        response.entities.get("searches") match {
          case Some(entities) =>
            entities.exists(_.entity_id == "item3") should be(false)
          case None => fail("Expected 'searches' domain")
        }
      }
    }

    "deletes entire user's favorites domain in case of force=true" in {
      Delete("?force=true") ~> searchesRoute ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[GetResponse]
        response.entities.get("searches") match {
          case None =>
          case Some(entities) if entities.isEmpty =>
          case Some(_) =>
            fail(
              "Expected no searches or empty items after delete entire domain"
            )
        }
        cards.getAll(cardsDomain, user).futureValue shouldNot be(empty)
      }
    }

    "deletes all user's favorites domains in case of force=true" in {
      Delete("?force=true") ~> wildcardRoute ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[GetResponse]
        response.entities should be(empty)
        searches.getAll(searchesDomain, user).futureValue shouldBe empty
        cards.getAll(cardsDomain, user).futureValue shouldBe empty
      }
    }

    "not delete entire user's favorites in case of no force=true" in {
      Delete() ~> searchesRoute ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "move single domain" in {
      searches.create(searchesDomain, user, item1)
      cards.create(cardsDomain, user, item2)

      val dest = UserRef("uid:4")
      Post(s"/move/$dest") ~> searchesRoute ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[GetResponse]
        response.user shouldBe dest.toPlain
        response.entities.get("searches") match {
          case Some(entities) if entities.nonEmpty =>
          case _ => fail("Expected 'searches' domain, there is no this one")
        }
        response.entities.get("cards") match {
          case None =>
          case Some(entities) if entities.isEmpty =>
          case _ =>
            fail(
              "Expected no 'cards' domain after move only 'searches', but got this"
            )
        }
        // after move source domain should be empty
        searches.getAll(searchesDomain, user).futureValue shouldBe empty
      }
    }

    "move all domains" in {
      searches.create(searchesDomain, user, item1)
      cards.create(cardsDomain, user, item2)

      val dest = UserRef("uid:5")
      Post(s"/move/$dest") ~> wildcardRoute ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[GetResponse]
        response.user shouldBe dest.toPlain
        response.entities.get("searches") match {
          case Some(entities) if entities.nonEmpty =>
          case _ => fail("Expected 'searches' domain, there is no this one")
        }
        response.entities.get("cards") match {
          case Some(entities) if entities.nonEmpty =>
          case _ => fail("Expected 'cards' domain, there is no this one")
        }
        // after move source should be empty
        searches.getAll(searchesDomain, user).futureValue shouldBe empty
        cards.getAll(cardsDomain, user).futureValue shouldBe empty
      }
    }

    "preserve destination data in case of move" in {
      searches.create(searchesDomain, user, item1)
      cards.create(cardsDomain, user, item2)

      val dest = UserRef("uid:6")
      val destItem = FavoriteItem(
        "item3",
        DateTime.parse("2016-05-20T19:00:00.000Z"),
        DateTime.parse("2016-05-20T19:21:00.000Z"),
        None
      )
      searches.create(searchesDomain, dest, destItem)

      Post(s"/move/$dest") ~> wildcardRoute ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[GetResponse]
        response.user shouldBe dest.toPlain
        response.entities.get("searches") match {
          case Some(entities) if entities.contains(Entity.fromFavoriteItem(destItem)) =>
          case Some(entities) =>
            fail(s"Unexpected entities after move: $entities")
          case _ => fail("Expected 'searches' domain, there is no this one")
        }
      }
    }
  }
}
