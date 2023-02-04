package ru.yandex.auto.garage.consumers.kafka.vos.recall

import auto.carfax.common.clients.passport.PassportClient
import auto.carfax.common.clients.vos.VosClient
import auto.carfax.common.utils.tracing.Traced
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.api.vin.garage.GarageApiModel.Card
import ru.yandex.auto.garage.consumers.kafka.vos.VosProcessorTestUtils._
import ru.yandex.auto.garage.converters.cards.PublicToInternalCardConverter
import ru.yandex.auto.garage.dao.CardsService
import ru.yandex.auto.garage.dao.CardsService.{Insert, Skip}
import ru.yandex.auto.garage.managers.{CardBuilder, OfferInfo}
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.GarageCard
import ru.yandex.auto.vin.decoder.model.{AutoruUser, VinCode}
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VosOffersRecallEventProcessorTest extends AnyWordSpecLike with MockitoSupport with BeforeAndAfter with Matchers {

  implicit val t = Traced.empty
  implicit val m = TestOperationalSupport

  private val cardsService = mock[CardsService]
  private val cardBuilder = mock[CardBuilder]
  private val converter = mock[PublicToInternalCardConverter]
  private val passportClient = mock[PassportClient]
  private val vosClient = mock[VosClient]

  private val processor =
    new VosOffersRecallEventProcessor(cardsService, passportClient, vosClient, cardBuilder, converter)

  private val user = AutoruUser(123)

  "offer suitable" should {
    "return false" when {
      "category MOTO" in {
        processor.isOfferSuitable(buildOfferInfo(Category.MOTO)) shouldBe false
      }
      "section NEW" in {
        processor.isOfferSuitable(buildOfferInfo(section = Section.NEW)) shouldBe false
      }
      "vin is empty" in {
        processor.isOfferSuitable(buildOfferInfo(optVin = None)) shouldBe false
      }
    }
    "return true" when {
      "category = CARS and section = USED" in {
        processor.isOfferSuitable(buildOfferInfo()) shouldBe true
      }
    }
  }

  "prepare event data" should {
    val offer = buildOffer()
    "return None" when {
      "cant find user in passport" in {
        when(passportClient.userSearch(?)(?)).thenReturn(Future.successful(None))
        when(vosClient.getOffer(?)(?)).thenReturn(Future.successful(Some(offer)))
        val result = processor.prepareEventData("123", "123-abc").await

        result shouldBe None
      }
      "cant find offer in vos" in {
        when(passportClient.userSearch(?)(?)).thenReturn(Future.successful(Some(user)))
        when(vosClient.getOffer(?)(?)).thenReturn(Future.successful(None))
        val result = processor.prepareEventData("123", "123-abc").await

        result shouldBe None
      }
    }
    "return prepared data" when {
      "successfully find user and offer" in {
        when(passportClient.userSearch(?)(?)).thenReturn(Future.successful(Some(user)))
        when(vosClient.getOffer(?)(?)).thenReturn(Future.successful(Some(offer)))
        val result = processor.prepareEventData("123", "123-abc").await

        result shouldBe Some(user -> OfferInfo(offer))
      }
    }
  }

  "create card function" should {
    val vin = VinCode("SALGA2BE8LA405000")
    val user = AutoruUser(123)
    "return Skip" when {
      "card already exists" in {
        val existed = buildRow(vin = Some(vin))
        processor.createCardFunction(user, List(existed), buildOfferInfo(optVin = Some(vin))) shouldBe Skip(())
      }
    }
    "return insert" when {
      "card does not exists" in {
        val existed = buildRow(vin = Some(vin))
        val otherVin = VinCode("SALGA2BE8LA405123")
        when(cardBuilder.buildGarageCardFromOffer(?, ?, ?, ?)(?)).thenReturn(Card.newBuilder().build())
        when(converter.convertNewCard(?, ?, ?, ?)).thenReturn(GarageCard.newBuilder().build())
        processor
          .createCardFunction(user, List(existed), buildOfferInfo(optVin = Some(otherVin)))
          .isInstanceOf[Insert[Unit]] shouldBe true
      }
    }
  }

}
