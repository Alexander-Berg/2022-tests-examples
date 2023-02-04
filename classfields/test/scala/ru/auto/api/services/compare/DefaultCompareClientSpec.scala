package ru.auto.api.services.compare

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.api.util.StringUtils._

/**
  * Created by ndmelentev on 22.05.17.
  */
class DefaultCompareClientSpec extends HttpClientSpec with MockedHttpClient with ScalaCheckPropertyChecks {

  val compareClient = new DefaultCompareClient(http)

  "CompareClient" should {
    "get users catalog cards" in {
      forAll(PrivateUserRefGen) { (user) =>
        val uid = user.uid
        val category = Category.CARS

        http.expectUrl(GET, url"/favorites/2.0/autoru%3Acar_compare/uid%3A$uid")

        http.respondWithJson(
          OK,
          s"""{"service": "autoru",
                            "version": "2.0",
                            "user": "uid:$uid",
                            "entities": {
                            "car_compare": [{"entity_id": "1_2_3",
                            "create_timestamp": 1495546513695,
                            "update_timestamp": 1495546513695},
                            {"entity_id": "4_5_6",
                            "create_timestamp": 1495546513696,
                            "update_timestamp": 1495546513696}]}}"""
        )

        val compareResult = compareClient.getCatalogCards(category, user).futureValue.map(_.entityId)
        compareResult(0) shouldBe "1_2_3"
        compareResult(1) shouldBe "4_5_6"
      }
    }

    "post catalog card to users compare list" in {
      forAll(PrivateUserRefGen) { (user) =>
        val uid = user.uid
        val category = Category.CARS
        val catalogCardId = "1_2_3"
        val updateTimestamp = 1606139791142L

        http.expectUrl(POST, url"/favorites/2.0/autoru%3Acar_compare/uid%3A$uid/$catalogCardId")
        http.respondWithJson(OK, s"""{"status": "OK"}""")

        compareClient.addCatalogCard(category, user, catalogCardId, updateTimestamp).futureValue
      }
    }

    "upsert catalog card to users compare list" in {
      forAll(PrivateUserRefGen) { (user) =>
        val uid = user.uid
        val category = Category.CARS
        val catalogCardId = "1_2_3"
        val updateTimestamp = 1606139791142L

        http.expectUrl(PUT, url"/favorites/2.0/autoru%3Acar_compare/uid%3A$uid/$catalogCardId")
        http.respondWithJson(OK, s"""{"status": "OK"}""")

        compareClient.upsertCatalogCard(category, user, catalogCardId, updateTimestamp).futureValue
      }
    }

    "delete catalog card from users compare list" in {
      forAll(PrivateUserRefGen) { (user) =>
        val uid = user.uid
        val category = Category.CARS
        val catalogCardId = "1_2_3"

        http.expectUrl(DELETE, url"/favorites/2.0/autoru%3Acar_compare/uid%3A$uid/$catalogCardId")
        http.respondWithJson(OK, s"""{"status": "OK"}""")

        compareClient.deleteCatalogCard(category, user, catalogCardId).futureValue
      }
    }

    "move catalog cards to another user" in {
      forAll(PrivateUserRefGen, PrivateUserRefGen) { (user1, user2) =>
        val uid1 = user1.uid
        val uid2 = user2.uid

        http.expectUrl(
          POST,
          url"/favorites/2.0/autoru%3Acar_compare%2Cmoto_compare%2Ccommercial_compare/uid%3A$uid1/move/uid%3A$uid2"
        )
        http.respondWithJson(OK, s"""{"status": "OK"}""")

        compareClient.moveCatalogCards(user1, user2).futureValue
      }
    }

  }
}
