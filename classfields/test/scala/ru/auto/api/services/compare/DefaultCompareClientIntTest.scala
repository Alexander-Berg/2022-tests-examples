package ru.auto.api.services.compare

import org.scalatest.Ignore
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.exceptions.UserAlreadyHasThisCatalogCard
import ru.auto.api.http.HttpClientConfig
import ru.auto.api.model.UserRef
import ru.auto.api.services.HttpClientSuite

/**
  * Created by ndmelentev on 22.05.17.
  */
@Ignore
class DefaultCompareClientIntTest extends HttpClientSuite {

  override protected def config: HttpClientConfig =
    HttpClientConfig.apply("http", "personal-api-01-sas.test.vertis.yandex.net", 36900)

  val compareClient = new DefaultCompareClient(http)

  test("get catalog cards of user") {
    val catalogCardId = "1_2_3"
    val user = UserRef.user(20871601L)
    val domain = Category.CARS
    val updateTimestamp = 1606139791142L

    compareClient.upsertCatalogCard(domain, user, catalogCardId, updateTimestamp).futureValue

    compareClient.getCatalogCards(domain, user).futureValue.map(_.entityId) should contain(catalogCardId)

    compareClient.deleteCatalogCard(domain, user, catalogCardId)
  }

  test("get catalog cards of user when there are none") {
    val user = UserRef.user(20871602L)
    val domain = Category.CARS

    val catalogCards = compareClient.getCatalogCards(domain, user).futureValue.map(_.entityId)
    for (catalogCard <- catalogCards) {
      compareClient.deleteCatalogCard(domain, user, catalogCard).futureValue
    }

    compareClient.getCatalogCards(domain, user).futureValue.size shouldBe 0
  }

  test("add catalog card to user") {
    pending
    val catalogCardId = "1_2_3"
    val user = UserRef.user(20871602L)
    val domain = Category.CARS
    val updateTimestamp = 1606139791142L

    // remove favorite offer from user
    compareClient.deleteCatalogCard(domain, user, catalogCardId).futureValue

    // add once -> no throwable
    compareClient.addCatalogCard(domain, user, catalogCardId, updateTimestamp).futureValue

    // add twice -> throws exception
    compareClient
      .addCatalogCard(domain, user, catalogCardId, updateTimestamp)
      .failed
      .futureValue shouldBe an[UserAlreadyHasThisCatalogCard]

    compareClient.deleteCatalogCard(domain, user, catalogCardId).futureValue
  }

  test("upsert catalog card to user") {
    val catalogCardId = "1_2_3"
    val user = UserRef.user(20871601L)
    val domain = Category.CARS
    val updateTimestamp = 1606139791142L

    compareClient.deleteCatalogCard(domain, user, catalogCardId).futureValue
    compareClient.getCatalogCards(domain, user).futureValue shouldNot contain(catalogCardId)

    // add once -> no exception
    compareClient.upsertCatalogCard(domain, user, catalogCardId, updateTimestamp).futureValue
    compareClient.getCatalogCards(domain, user).futureValue should contain(catalogCardId)

    // add twice -> no exception
    compareClient.upsertCatalogCard(domain, user, catalogCardId, updateTimestamp).futureValue
    compareClient.getCatalogCards(domain, user).futureValue should contain(catalogCardId)

    compareClient.deleteCatalogCard(domain, user, catalogCardId).futureValue
  }

  test("move catalog cards to another user") {
    pending
    val user1 = UserRef.user(20871601L)
    val user2 = UserRef.user(20871602L)
    val domain = Category.CARS
    val catalogCardId = "1_2_3"
    val updateTimestamp = 1606139791142L

    val catalogCards1 = compareClient.getCatalogCards(domain, user1).futureValue.map(_.entityId)
    for (catalogCard <- catalogCards1) {
      compareClient.deleteCatalogCard(domain, user1, catalogCard).futureValue
    }
    compareClient.addCatalogCard(domain, user2, catalogCardId, updateTimestamp).futureValue

    val catalogCards2 = compareClient.getCatalogCards(domain, user1).futureValue.map(_.entityId)
    for (catalogCard <- catalogCards2) {
      compareClient.deleteCatalogCard(domain, user2, catalogCard).futureValue
    }

    compareClient.moveCatalogCards(user1, user2).futureValue
    compareClient.getCatalogCards(domain, user1).futureValue.size should be(0)
    compareClient.getCatalogCards(domain, user2).futureValue should contain(catalogCardId)

    compareClient.moveCatalogCards(user2, user1).futureValue
    compareClient.getCatalogCards(domain, user2).futureValue.size should be(0)
    compareClient.getCatalogCards(domain, user1).futureValue should contain(catalogCardId)
  }

  test("delete catalog card from user") {
    val catalogCardId = "1_2_3"
    val user = UserRef.user(20871601L)
    val domain = Category.CARS
    val updateTimestamp = 1606139791142L

    compareClient.upsertCatalogCard(domain, user, catalogCardId, updateTimestamp).futureValue
    compareClient.deleteCatalogCard(domain, user, catalogCardId).futureValue
    compareClient.getCatalogCards(domain, user).futureValue shouldNot contain(catalogCardId)
  }

}
