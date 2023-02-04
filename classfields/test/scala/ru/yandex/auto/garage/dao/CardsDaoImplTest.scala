package ru.yandex.auto.garage.dao

import auto.carfax.common.utils.tracing.Traced
import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.garage.GarageApiModel
import ru.auto.api.vin.garage.GarageApiModel.CardTypeInfo.CardType
import ru.auto.api.vin.garage.RequestModel.GetListingRequest.{Filters, Sorting}
import ru.yandex.auto.garage.dao.cards.{CardsDaoImpl, GarageCardsTable}
import ru.yandex.auto.garage.exceptions.CardNotFound
import ru.yandex.auto.garage.testkit.GaragePgDatabaseContainer
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.GarageCard
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.Meta.CardTypeInfo
import ru.yandex.auto.vin.decoder.model.VinCode
import auto.carfax.common.storages.pg.PostgresProfile.api._
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture

import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters._

class CardsDaoImplTest extends AnyFunSuite with GaragePgDatabaseContainer with BeforeAndAfter {

  lazy val dao = new CardsDaoImpl(database)
  implicit val t = Traced.empty

  before {
    val query = GarageCardsTable.cards.delete
    database.runWrite(query)
  }

  test("update not existing card") {
    intercept[CardNotFound] {
      dao
        .lockAndUpdate(1L, "user:123", System.currentTimeMillis()) { _ =>
          Some(GarageCard.newBuilder().build()) -> ()
        }
        .await
    }
  }

  test("crud") {
    val userId = "user:123"
    val vin = VinCode("X7LBSRBYNBH480080")
    val updatedVin = VinCode("A7LBSRBYNBH480080")
    val card = {
      val builder = GarageCard.newBuilder()
      builder.getSourceBuilder.setUserId(userId)
      builder.getMetaBuilder
        .setCreated(Timestamps.fromMillis(System.currentTimeMillis()))
        .setStatus(GarageCard.Status.ACTIVE)
      builder.getVehicleInfoBuilder.getDocumentsBuilder.setVin(vin.toString)
      builder.build()
    }
    val updatedCard = {
      val builder = card.toBuilder
      builder.getVehicleInfoBuilder.getDocumentsBuilder.setVin(updatedVin.toString)
      builder.build()
    }

    val emptyResult = dao.getCard(userId, vin, onMaster = true).await

    val inserted = dao.insertCard(card, System.currentTimeMillis()).await

    val afterInsertResult = dao.getCard(userId, vin, onMaster = true).await
    val afterInsertByIdResult = dao.getCard(inserted.id, onMaster = true).await

    val updated = dao
      .lockAndUpdate(inserted.id, userId, System.currentTimeMillis())(_ => Some(updatedCard) -> ())
      .await

    val afterUpdateResult = dao.getCard(userId, updatedVin, onMaster = true).await
    val afterUpdateByIdResult = dao.getCard(inserted.id, onMaster = true).await

    assert(emptyResult.isEmpty)

    assert(inserted.card == card)
    assert(afterInsertResult.get.card == card)
    assert(afterInsertByIdResult.get.card == card)

    assert(updated._1.card == updatedCard)
    assert(afterUpdateResult.get.card == updatedCard)
    assert(afterUpdateByIdResult.get.card == updatedCard)
  }

  test("get user cards") {
    val userId = "user:123"

    val vins = List(
      VinCode("XW8ZZZ4H5DG046202"),
      VinCode("XW8ZZZ4H5DG046203"),
      VinCode("WAUZZZ4BZWN039853"),
      VinCode("WUAZZZ4G7FN902006"),
      VinCode("WUAZZZ4G7FN902007")
    )

    val secondUserId = "user:456"
    val secondUserVins = List(
      VinCode("WAUZZZ8T3CA006712")
    )

    secondUserVins.foreach(vin => {
      val card = buildCard(secondUserId, vin)
      dao.insertCard(card, System.currentTimeMillis()).await
    })

    val filters = Filters.newBuilder().addStatus(GarageApiModel.Card.Status.ACTIVE).build()
    val sorting = Sorting.CREATION_DATE

    val emptyResult = dao.getUserCards(userId, 2, 0, filters, sorting, true).await

    vins.foreach(vin => {
      val card = buildCard(userId, vin)
      dao.insertCard(card, System.currentTimeMillis()).await
    })

    val firstPage = dao.getUserCards(userId, 2, 0, filters, sorting, true).await
    val secondPage = dao.getUserCards(userId, 2, 2, filters, sorting, true).await
    val thirdPage = dao.getUserCards(userId, 2, 4, filters, sorting, true).await

    assert(emptyResult.isEmpty)
    assert(firstPage.map(_.card.getVehicleInfo.getDocuments.getVin) == vins.takeRight(2).map(_.toString).reverse)
    assert(
      secondPage.map(_.card.getVehicleInfo.getDocuments.getVin) == vins.takeRight(4).take(2).map(_.toString).reverse
    )
    assert(thirdPage.map(_.card.getVehicleInfo.getDocuments.getVin) == vins.take(1).map(_.toString))
  }

  test("get user cards with valid ordering even with paging") {
    val userId = "user:123"

    val vin1 = VinCode("XW8ZZZ4H5DG046202")
    val vin2 = VinCode("WUAZZZ4G7FN902006")
    val vin3 = VinCode("WUAZZZ4G7FN902008")

    val now = System.currentTimeMillis()
    val currentCarCard1 = buildCard(userId, vin1, CardType.CURRENT_CAR, now + 100)
    dao.insertCard(currentCarCard1, now).await
    val currentCarCard2 = buildCard(userId, vin2, CardType.CURRENT_CAR, now + 200)
    dao.insertCard(currentCarCard2, now).await
    val dreamCarCard = buildCard(userId, vin3, CardType.DREAM_CAR, now + 300)
    dao.insertCard(dreamCarCard, System.currentTimeMillis()).await

    val filters = Filters
      .newBuilder()
      .addStatus(GarageApiModel.Card.Status.ACTIVE)
      .addAllCardType(Seq(CardType.CURRENT_CAR, CardType.DREAM_CAR).asJava)
      .build()
    val sorting = Sorting.CREATION_DATE

    val page = dao.getUserCards(userId, 1, 0, filters, sorting, true).await

    assert(
      page.map(_.card.getVehicleInfo.getDocuments.getVin).head == currentCarCard2.getVehicleInfo.getDocuments.getVin
    )
  }

  private def buildCard(
      userId: String,
      vin: VinCode,
      cardType: CardType = CardType.CURRENT_CAR,
      createdMs: Long = System.currentTimeMillis()) = {
    val builder = GarageCard.newBuilder()
    builder.getSourceBuilder.setUserId(userId)
    builder.getMetaBuilder
      .setCreated(Timestamps.fromMillis(createdMs))
      .setStatus(GarageCard.Status.ACTIVE)
      .setCardTypeInfo(
        CardTypeInfo.newBuilder().setCurrentState(CardTypeInfo.State.newBuilder().setCardType(cardType))
      )
    builder.getVehicleInfoBuilder.getDocumentsBuilder.setVin(vin.toString)
    builder.build()
  }

}
