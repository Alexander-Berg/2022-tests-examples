package ru.yandex.vertis.story.api.managers.offer

import org.mockito.Mockito.{times, verify}
import ru.auto.api.ApiOfferModel._
import ru.yandex.vertis.baker.util.TracedUtils
import ru.yandex.vertis.baker.util.test.http.BaseSpec
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.story.api.clients.searcher.SearcherClient
import ru.yandex.vertis.story.api.util.{BasicGenerators, TestOffer}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.util.concurrent.Threads
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eeq}
import ru.yandex.vertis.story.api.model.StoryType

import scala.concurrent.{ExecutionContext, Future}

class OffersManagerSpec extends BaseSpec with MockitoSupport with ProducerProvider {

  abstract class Fixture {
    implicit val trace: Traced = TracedUtils.empty
    implicit val ec: ExecutionContext = Threads.SameThreadEc

    val searcherClient: SearcherClient = mock[SearcherClient]
    val userId: String = BasicGenerators.readableString.next

    val offerCarWithoutDescription: Offer = TestOffer.createTestOffer("user:" ++ userId, Category.CARS, "")
    val offerMotoWithoutDescription: Offer = TestOffer.createTestOffer("user:" ++ userId, Category.MOTO, "")
    val offerTrucksWithoutDescription: Offer = TestOffer.createTestOffer("user:" ++ userId, Category.TRUCKS, "")

    val offerCarWithDescription: Offer = TestOffer.createTestOffer(
      "user:" ++ userId,
      Category.CARS,
      "" +
        "test test test test test test test test test test test test test test test test test test test test test test test test test test "
    )

    when(searcherClient.getOfferByUserId(eeq(Category.CARS), eeq(userId), ?, ?)(?))
      .thenReturn(Future.successful(Some(offerCarWithoutDescription)))
    when(searcherClient.getOfferByUserId(eeq(Category.MOTO), eeq(userId), ?, ?)(?))
      .thenReturn(Future.successful(Some(offerMotoWithoutDescription)))
    when(searcherClient.getOfferByUserId(eeq(Category.TRUCKS), eeq(userId), ?, ?)(?))
      .thenReturn(Future.successful(Some(offerMotoWithoutDescription)))
    when(searcherClient.getOfferById(?, ?)(?)).thenReturn(Future.successful(Some(offerCarWithoutDescription)))
    when(searcherClient.getRandomOffer(eeq(Category.CARS), ?, ?, ?)(?))
      .thenReturn(Future.successful(Some(offerCarWithDescription)))
    when(searcherClient.getRandomOffer(eeq(Category.MOTO), ?, ?, ?)(?))
      .thenReturn(Future.successful(Some(offerMotoWithoutDescription)))
    when(searcherClient.getRandomOffer(eeq(Category.TRUCKS), ?, ?, ?)(?))
      .thenReturn(Future.successful(Some(offerMotoWithoutDescription)))

    val offersManager = new OffersManager(searcherClient)

  }

  "OffersManager.getOffersByUserId" should {

    "return offerCar" in new Fixture() {
      val offersByCategory: Future[OffersByCategory] = offersManager.getOffersByUserId(
        categories = Set(Category.CARS, Category.MOTO, Category.TRUCKS),
        userId = userId,
        serviceType = None,
        tags = None
      )

      verify(searcherClient, times(3)).getOfferByUserId(?, ?, ?, ?)(?)
      offersByCategory.map(_.car shouldBe offerCarWithoutDescription)
      offersByCategory.map(_.moto shouldBe None)
    }

    "return offerMoto" in new Fixture() {
      val offersByCategory: Future[OffersByCategory] = offersManager.getOffersByUserId(
        categories = Set(Category.MOTO, Category.TRUCKS),
        userId = userId,
        serviceType = None,
        tags = None
      )

      verify(searcherClient, times(2)).getOfferByUserId(?, ?, ?, ?)(?)
      offersByCategory.map(_.car shouldBe None)
      offersByCategory.map(_.moto shouldBe offerMotoWithoutDescription)
    }

  }

  "OffersManager.getRandomOffersByCategory" should {

    "return offer by user id" in new Fixture() {
      val offersByCategory: Future[OffersByCategory] = offersManager.getRandomOffersByCategory(
        rId = None,
        excludeOfferId = Set.empty,
        category = Set(Category.CARS, Category.MOTO, Category.TRUCKS)
      )

      verify(searcherClient, times(3)).getRandomOffer(?, ?, ?, ?)(?)
      offersByCategory.map(_.car shouldBe offerCarWithDescription)
      offersByCategory.map(_.moto shouldBe None)
    }

  }

  "OffersManager.getOfferId" should {

    "return offer by offer id" in new Fixture() {
      val offer: Future[Option[Offer]] = offersManager.getOfferId(
        category = Category.CARS,
        offerId = offerCarWithoutDescription.getId
      )
      verify(searcherClient, times(1)).getOfferById(?, ?)(?)
      offer.map(_ shouldBe offerCarWithoutDescription)
    }

  }

  "OffersManager.getTitleAndText" should {

    "return title, price, textMileage, textInfoObject, textTransmission" in new Fixture() {

      val storyContent =
        offersManager.getTitleAndText(offerCarWithoutDescription, StoryType.Offer)

      val mileage: Int = offerCarWithoutDescription.getState.getMileage
      val year: Int = offerCarWithoutDescription.getDocuments.getYear

      storyContent.price shouldBe Some(offerCarWithoutDescription.getPriceInfo.getPrice.toInt + "\u00A0" + "₽")
      storyContent.mileage shouldBe Some(s"Пробег $mileage км")
      storyContent.info shouldBe "0.0 л. / 0 л.с. /  Бензин"
      storyContent.transmission shouldBe Some("Механика, 3+ владельца")
      storyContent.title shouldBe s"mark model $year"
    }
  }
}
