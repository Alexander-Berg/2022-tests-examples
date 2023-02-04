package ru.yandex.auto.garage.consumers.kafka.vos.updates

import auto.carfax.common.utils.tracing.Traced
import com.google.protobuf.util.Timestamps
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{times, verify}
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.ApiOfferModel._
import ru.auto.api.CommonModel.RecallReason
import ru.auto.api.DiffLogModel.OfferChangeEvent
import ru.auto.api.vin.garage.GarageApiModel
import ru.auto.api.vin.garage.GarageApiModel.CardTypeInfo.CardType
import ru.yandex.auto.garage.consumers.kafka.vos.VosProcessorTestUtils._
import ru.yandex.auto.garage.consumers.kafka.vos.updates.VosOffersEventProcessor._
import ru.yandex.auto.garage.converters.cards.PublicToInternalCardConverter
import ru.yandex.auto.garage.dao.CardsService
import ru.yandex.auto.garage.dao.CardsService.{Insert, Skip, Update}
import ru.yandex.auto.garage.managers.{CardBuilder, OfferInfo}
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.GarageCard.Status
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.Meta.CardTypeInfo
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.Meta.CardTypeInfo.{ChangeEvent, ChangeTypeSource}
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.{GarageCard, Meta}
import ru.yandex.auto.vin.decoder.model.{AutoruUser, LicensePlate, VinCode}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters.IterableHasAsJava

class VosOffersEventProcessorTest extends AnyWordSpecLike with MockitoSupport with BeforeAndAfter with Matchers {
  implicit val t = Traced.empty
  implicit val m = TestOperationalSupport

  private val cardsService = mock[CardsService]
  private val cardBuilder = mock[CardBuilder]
  private val converter = mock[PublicToInternalCardConverter]
  private val processor = new VosOffersEventProcessor(cardsService, cardBuilder, converter, Feature("", _ => true))

  "transformIfEventIsSuitable" should {
    "return None" when {
      "category is not CARS" in {
        val diff = {
          val builder = SuitableDiff.toBuilder
          builder.getNewOfferBuilder.setCategory(Category.MOTO)
          builder.build()
        }
        processor.transformIfEventIsSuitable(diff) shouldBe None
      }
      "section is not USED" in {
        val diff = {
          val builder = SuitableDiff.toBuilder
          builder.getNewOfferBuilder.setSection(Section.NEW)
          builder.build()
        }
        processor.transformIfEventIsSuitable(diff) shouldBe None
      }
      "offer from dealer" in {
        val diff = {
          val builder = SuitableDiff.toBuilder
          builder.getNewOfferBuilder.setSellerType(SellerType.COMMERCIAL)
          builder.build()
        }
        processor.transformIfEventIsSuitable(diff) shouldBe None
      }
      "offer without vin" in {
        val diff = {
          val builder = SuitableDiff.toBuilder
          builder.getNewOfferBuilder.getDocumentsBuilder.clearVin()
          builder.build()
        }
        processor.transformIfEventIsSuitable(diff) shouldBe None
      }
    }
    "return DiffInfo" when {
      "suitable diff event" in {
        val optDiffInfo = processor.transformIfEventIsSuitable(SuitableDiff)

        optDiffInfo.nonEmpty shouldBe true
        val diffInfo = optDiffInfo.get

        diffInfo.user shouldBe AutoruUser.apply(123)
        diffInfo.vin shouldBe TestVin
        diffInfo.offerId shouldBe "123-abc"
        diffInfo.optOldOffer shouldBe Some(OfferInfo(SuitableDiff.getOldOffer))
        diffInfo.newOffer shouldBe OfferInfo(SuitableDiff.getNewOffer)
      }
    }
  }

  "find existed card" should {
    val notFoundVin = VinCode("SALWA2FF8FA505566")
    "return None" when {
      "there are no cards with offer id or vin in any state" in {
        val row = buildRow(vin = Some(TestVin), offerId = Some("123-abc"))
        processor.findExistedCard(List(row), "456-def", notFoundVin) shouldBe None
      }
    }
    "return card and prefer card".that {
      "active" in {
        val row1 = buildRow(id = 1, vin = Some(TestVin), status = GarageCard.Status.DELETED)
        val row2 = buildRow(id = 2, vin = Some(TestVin), status = GarageCard.Status.ACTIVE)

        processor.findExistedCard(List(row1, row2), "456-def", TestVin) shouldBe Some(row2)
      }
      "active and contains offer id" in {
        val row1 = buildRow(id = 1, vin = Some(TestVin), offerId = Some("123-abc"))
        val row2 = buildRow(id = 2, vin = Some(TestVin), offerId = Some("456-def"))

        processor.findExistedCard(List(row1, row2), "123-abc", notFoundVin) shouldBe Some(row1)
      }
      "active and contains vin" in {
        val row1 =
          buildRow(id = 1, vin = Some(notFoundVin), offerId = Some("123-abc"), status = GarageCard.Status.DELETED)
        val row2 = buildRow(id = 2, vin = Some(TestVin))

        processor.findExistedCard(List(row1, row2), "123-abc", TestVin) shouldBe Some(row2)
      }
      "deleted and contains offer id" in {
        val row1 =
          buildRow(id = 1, vin = Some(notFoundVin), offerId = Some("123-abc"), status = GarageCard.Status.DELETED)
        val row2 = buildRow(id = 2, vin = Some(notFoundVin))

        processor.findExistedCard(List(row1, row2), "123-abc", TestVin) shouldBe Some(row1)
      }
    }
  }

  "buildAction" should {
    val user = AutoruUser.apply(123)
    "return Skip" when {
      "there are no existed card and status no changed to active" in {
        processor.buildAction(
          user = user,
          optExistedCard = None,
          diffInfo = DiffInfo(
            user,
            optOldOffer = Some(buildOfferInfo(optVin = Some(TestVin))),
            newOffer = buildOfferInfo(optVin = Some(TestVin))
          )
        ) shouldBe Skip(SkipNotExistCardLabel)
      }
      "there are existed card in deleted status" in {
        processor.buildAction(
          user = user,
          optExistedCard = Some(buildRow(status = GarageCard.Status.DELETED)),
          diffInfo = DiffInfo(
            user,
            optOldOffer = Some(buildOfferInfo(optVin = Some(TestVin), status = OfferStatus.DRAFT)),
            newOffer = buildOfferInfo(optVin = Some(TestVin))
          )
        ) shouldBe Skip(SkipUpdateDeletedCardLabel)
      }
    }
    "return insert" when {
      "there are not existed card and status changed to active" in {
        val internalCard = GarageCard.newBuilder().build()
        when(cardBuilder.buildGarageCardFromOffer(?, ?, ?, ?)(?))
          .thenReturn(GarageApiModel.Card.newBuilder().build())
        when(converter.convertNewCard(?, ?, ?, ?)).thenReturn(internalCard)
        val res = processor.buildAction(
          user = user,
          optExistedCard = None,
          diffInfo = DiffInfo(
            user,
            optOldOffer = Some(buildOfferInfo(optVin = Some(TestVin), status = OfferStatus.DRAFT)),
            newOffer = buildOfferInfo(optVin = Some(TestVin))
          )
        )

        res.isInstanceOf[Insert[String]] shouldBe true
        res.value shouldBe InsertActiveLabel
        res.asInstanceOf[Insert[String]].card.card shouldBe internalCard
      }

      "a car from an offer has been sold" in {
        val internalCard = GarageCard.newBuilder().build()
        when(cardBuilder.buildGarageCardFromOffer(?, ?, ?, ?)(?))
          .thenReturn(GarageApiModel.Card.newBuilder().build())
        when(converter.convertNewCard(?, ?, ?, ?)).thenReturn(internalCard)

        val recallInfo = RecallInfo
          .newBuilder()
          .setReason(RecallReason.SOLD_ON_AUTORU)
          .setRecallTimestamp(SoldTimestamp)
          .build()
        val res = processor.buildAction(
          user = user,
          optExistedCard = None,
          diffInfo = DiffInfo(
            user,
            optOldOffer = Some(buildOfferInfo(optVin = Some(TestVin), status = OfferStatus.ACTIVE)),
            newOffer = buildOfferInfo(optVin = Some(TestVin), optRecallInfo = Some(recallInfo))
          )
        )

        verify(cardBuilder, times(1))
          .buildGarageCardFromOffer(
            ?,
            ArgumentMatchers.eq(CardType.EX_CAR),
            ?,
            ?
          )(?)
        res.isInstanceOf[Insert[String]] shouldBe true
        res.value shouldBe InsertSoldLabel
        res.asInstanceOf[Insert[String]].card.card shouldBe internalCard
      }
    }
    "return update" when {
      "there are existed card and status is active" in {
        val res = processor.buildAction(
          user = user,
          optExistedCard = Some(buildRow(2, vin = Some(TestVin))),
          DiffInfo(
            user,
            optOldOffer = Some(buildOfferInfo(status = OfferStatus.ACTIVE, optVin = Some(TestVin))),
            newOffer = buildOfferInfo(status = OfferStatus.BANNED, optVin = Some(TestVin))
          )
        )

        res.isInstanceOf[Update[String]] shouldBe true
        val update = res.asInstanceOf[Update[String]]

        update.id shouldBe 2
        update.value shouldBe UpdateActiveLabel

        // TODO check update content
        /*update.cardUpdate._1.getOfferInfo shouldBe offerInfo

        val updatedOfferWithoutOfferInfo = {
          val builder = update.cardUpdate._1.toBuilder
          builder.clearOfferInfo()
          builder.build()
        }
        updatedOfferWithoutOfferInfo shouldBe existed.card*/
      }

      "car with active card has been sold" in {
        val recallInfo = RecallInfo
          .newBuilder()
          .setReason(RecallReason.SOLD_SOMEWHERE)
          .setRecallTimestamp(SoldTimestamp)
          .build()
        val res = processor.buildAction(
          user = user,
          optExistedCard = Some(
            buildRow(
              2,
              vin = Some(TestVin),
              card = GarageCard
                .newBuilder()
                .setMeta(
                  Meta
                    .newBuilder()
                    .setStatus(Status.ACTIVE)
                    .setCardTypeInfo(
                      CardTypeInfo
                        .newBuilder()
                        .setCurrentState(
                          CardTypeInfo.State
                            .newBuilder()
                            .setCardType(CardType.CURRENT_CAR)
                        )
                    )
                )
                .build()
            )
          ),
          DiffInfo(
            user,
            optOldOffer = Some(buildOfferInfo(status = OfferStatus.ACTIVE, optVin = Some(TestVin))),
            newOffer =
              buildOfferInfo(status = OfferStatus.BANNED, optVin = Some(TestVin), optRecallInfo = Some(recallInfo))
          )
        )

        res.isInstanceOf[Update[String]] shouldBe true
        val update = res.asInstanceOf[Update[String]]

        update.id shouldBe 2
        update.value shouldBe UpdateSoldLabel
        update.cardUpdate._1.getMeta.getCardTypeInfo.getCurrentState.getCardType shouldBe CardType.EX_CAR
        update.cardUpdate._1.getVehicleInfo.getDocuments.getSaleDate shouldBe Timestamps.fromMillis(SoldTimestamp)
        update.cardUpdate._1.getMeta.getCardTypeInfo.getCurrentState.getSource shouldBe ChangeTypeSource.OFFER
        update.cardUpdate._1.getMeta.getCardTypeInfo.getHistoryCount shouldBe 1

      }

      "car with deleted card has been sold" in {
        val recallInfo = RecallInfo
          .newBuilder()
          .setReason(RecallReason.SOLD_SOMEWHERE)
          .build()
        val res = processor.buildAction(
          user = user,
          optExistedCard = Some(
            buildRow(
              2,
              vin = Some(TestVin),
              status = Status.DELETED,
              card = GarageCard
                .newBuilder()
                .setMeta(
                  Meta
                    .newBuilder()
                    .setStatus(Status.DELETED)
                    .setCardTypeInfo(
                      CardTypeInfo
                        .newBuilder()
                        .setCurrentState(
                          CardTypeInfo.State
                            .newBuilder()
                            .setCardType(CardType.CURRENT_CAR)
                        )
                    )
                )
                .build()
            )
          ),
          DiffInfo(
            user,
            optOldOffer = Some(buildOfferInfo(status = OfferStatus.ACTIVE, optVin = Some(TestVin))),
            newOffer =
              buildOfferInfo(status = OfferStatus.BANNED, optVin = Some(TestVin), optRecallInfo = Some(recallInfo))
          )
        )

        res.isInstanceOf[Update[String]] shouldBe true
        val update = res.asInstanceOf[Update[String]]

        update.id shouldBe 2
        update.value shouldBe UpdateSoldLabel
        update.cardUpdate._1.getMeta.getCardTypeInfo.getCurrentState.getCardType shouldBe CardType.EX_CAR
        update.cardUpdate._1.getMeta.getCardTypeInfo.getCurrentState.getSource shouldBe ChangeTypeSource.OFFER
        update.cardUpdate._1.getMeta.getStatus shouldBe Status.ACTIVE
        update.cardUpdate._1.getMeta.getCardTypeInfo.getHistoryCount shouldBe 1

      }
    }
    "not perform card type change" when {
      "card is already in the EX status" in {
        val card = GarageCard
          .newBuilder()
          .setMeta(
            Meta
              .newBuilder()
              .setStatus(Status.ACTIVE)
              .setCardTypeInfo(
                CardTypeInfo
                  .newBuilder()
                  .setCurrentState(
                    CardTypeInfo.State
                      .newBuilder()
                      .setCardType(CardType.EX_CAR)
                      .setSource(ChangeTypeSource.MANUAL)
                  )
                  .addAllHistory(
                    List(
                      ChangeEvent
                        .newBuilder()
                        .build()
                    ).asJava
                  )
              )
          )
          .build()
        val recallInfo = RecallInfo
          .newBuilder()
          .setReason(RecallReason.SOLD_SOMEWHERE)
          .build()
        val res = processor.buildAction(
          user = user,
          optExistedCard = Some(buildRow(2, vin = Some(TestVin), cardType = CardType.EX_CAR, card = card)),
          DiffInfo(
            user,
            optOldOffer = Some(buildOfferInfo(status = OfferStatus.ACTIVE, optVin = Some(TestVin))),
            newOffer =
              buildOfferInfo(status = OfferStatus.BANNED, optVin = Some(TestVin), optRecallInfo = Some(recallInfo))
          )
        )
        val update = res.asInstanceOf[Update[String]]
        update.cardUpdate._1.getMeta.getCardTypeInfo.getCurrentState.getCardType shouldBe CardType.EX_CAR
        update.cardUpdate._1.getMeta.getCardTypeInfo.getCurrentState.getSource shouldBe ChangeTypeSource.MANUAL
        update.cardUpdate._1.getMeta.getCardTypeInfo.getHistoryCount shouldBe 1
      }
    }
  }

  private lazy val SuitableOffer = {
    val builder = Offer
      .newBuilder()
      .setId("123-abc")
      .setUserRef("user:123")
      .setCategory(Category.CARS)
      .setSellerType(SellerType.PRIVATE)
      .setSection(Section.USED)
      .setStatus(OfferStatus.ACTIVE)

    builder.getDocumentsBuilder.setVin(TestVin.toString).setLicensePlate(TestLp.toString)
    builder.getPriceInfoBuilder.setPrice(1000000)

    builder.build()
  }

  private lazy val TestVin = VinCode("SALWA2FF8FA505567")
  private lazy val TestLp = LicensePlate("Т234ТТ82")

  private lazy val SuitableDiff = {
    OfferChangeEvent.newBuilder().setNewOffer(SuitableOffer).setOldOffer(SuitableOffer).build()
  }

  private lazy val SoldTimestamp = 1647602953566L
}
