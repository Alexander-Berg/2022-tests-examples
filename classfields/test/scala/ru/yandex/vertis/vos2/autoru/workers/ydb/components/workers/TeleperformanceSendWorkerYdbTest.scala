package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.vin.VinResolutionEnums.{ResolutionPart, Status}
import ru.auto.api.vin.VinResolutionModel.ResolutionEntry
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.Condition.EXCELLENT
import ru.yandex.vos2.AutoruModel.AutoruOffer.TeleperformanceEvent.EventResult
import ru.yandex.vos2.AutoruModel.AutoruOffer.{Phone, Price, TeleperformanceEvent}
import ru.yandex.vos2.BasicsModel.Currency
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag}
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.services.teleperformance.TeleperformanceClient
import ru.yandex.vos2.getNow

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.DurationInt
import scala.util.Success

class TeleperformanceSendWorkerYdbTest extends AnyWordSpec with Matchers with MockitoSupport with InitTestDbs {

  initDbs()

  implicit val traced: Traced = Traced.empty

  private val client = mock[TeleperformanceClient]
  when(client.sendOffer(?)(?)).thenReturn(Success(EventResult.SUCCESS))

  abstract private class Fixture {
    val offer: Offer

    val worker = new TeleperformanceSendWorkerYdb(
      client,
      components.redemptionFilters,
      components.regionTree
    ) with YdbWorkerTestImpl
  }

  private val TestOfferID = "123-abc"

  "Teleperformance YDB" should {

    "don't process if not all need data" in new Fixture {
      val offerBuilder: Offer.Builder = createOffer()
      offerBuilder.getOfferAutoruBuilder.getSellerBuilder.clearUserName()
      val offer: Offer = offerBuilder.build()
      val res = worker.process(offer, None)
      assert(res.updateOfferFunc.isEmpty)
    }

    "set status new" in new Fixture {
      val offerBuilder = createOffer()
      val offer = offerBuilder.build()

      val res = worker.process(offer, None)

      val updatedOffer = res.updateOfferFunc.get(offer)
      assert(updatedOffer.getOfferAutoru.getTeleperformanceEventHistoryCount == 1)
      assert(
        updatedOffer.getOfferAutoru.getTeleperformanceEventHistoryList.get(0).getEventType
          == TeleperformanceEvent.EventType.NEW
      )
    }

    "vin changed" in new Fixture {
      val offerBuilder: Offer.Builder = createOffer()
      offerBuilder.getOfferAutoruBuilder.getPredictPriceBuilder.getAutoruBuilder.setFrom(502000)
      addHistoryElem(offerBuilder, TeleperformanceEvent.EventType.NEW, "01ab", offerBuilder.getOfferAutoru.getPrice)
      val offer = offerBuilder.build()

      val res = worker.process(offer, None)

      val updatedOffer = res.updateOfferFunc.get(offer)
      assert(updatedOffer.getOfferAutoru.getTeleperformanceEventHistoryCount == 2)
      assert(
        updatedOffer.getOfferAutoru.getTeleperformanceEventHistory(1).getEventType
          == TeleperformanceEvent.EventType.VIN_CHANGE
      )
    }

    "stage" should {
      "not processed" when {
        "not all need data" in new Fixture() {
          val offerBuilder: Offer.Builder = createOffer()
          offerBuilder.getOfferAutoruBuilder.getSellerBuilder.clearUserName()
          val offer: Offer = offerBuilder.build()

          worker.shouldProcess(offer, None).shouldProcess shouldEqual false
        }

        "not allow redemption criterias" in new Fixture() {
          val offerBuilder: Offer.Builder = createOffer()
          offerBuilder.getOfferAutoruBuilder.getPriceBuilder.setPrice(10000000)
          val offer: Offer = offerBuilder.build()

          worker.shouldProcess(offer, None).shouldProcess shouldEqual false

        }

        "predict price is not ready" in new Fixture() {
          val offerBuilder = createOffer()
          offerBuilder.getOfferAutoruBuilder.clearPredictPrice()
          val offer = offerBuilder.build()

          worker.shouldProcess(offer, None).shouldProcess shouldEqual false

        }

        "offer is banned, but last event is cancel" in new Fixture() {
          val offerBuilder: Offer.Builder = createOffer()
          addHistoryElem(
            offerBuilder,
            TeleperformanceEvent.EventType.CANCEL,
            offerBuilder.getOfferAutoru.getDocuments.getVin,
            offerBuilder.getOfferAutoru.getPrice
          )
          offerBuilder.addFlag(OfferFlag.OF_BANNED)
          val offer = offerBuilder.build()

          worker.shouldProcess(offer, None).shouldProcess shouldEqual false

        }

        "offer is banned, but event history is empty" in new Fixture() {
          val offerBuilder: Offer.Builder = createOffer()
          offerBuilder.addFlag(OfferFlag.OF_BANNED)
          val offer = offerBuilder.build()

          worker.shouldProcess(offer, None).shouldProcess shouldEqual false
        }

        "user is reseller, but last event is cancel" in new Fixture() {
          val offerBuilder: Offer.Builder = createOffer()
          addHistoryElem(
            offerBuilder,
            TeleperformanceEvent.EventType.CANCEL,
            offerBuilder.getOfferAutoru.getDocuments.getVin,
            offerBuilder.getOfferAutoru.getPrice
          )
          offerBuilder.getOfferAutoruBuilder.setReseller(true)
          val offer = offerBuilder.build()

          worker.shouldProcess(offer, None).shouldProcess shouldEqual false
        }

        "old car (old than 10 years)" in new Fixture() {
          val offerBuilder: Offer.Builder = createOffer()
          offerBuilder.getOfferAutoruBuilder.getEssentialsBuilder.setYear(2007)
          val offer = offerBuilder.build()

          worker.shouldProcess(offer, None).shouldProcess shouldEqual false

        }

        "user name is empty" in new Fixture() {
          val offerBuilder: Offer.Builder = createOffer()
          offerBuilder.getOfferAutoruBuilder.getSellerBuilder.setUserName("")
          val offer = offerBuilder.build()

          worker.shouldProcess(offer, None).shouldProcess shouldEqual false

        }

        "vin restriction" in new Fixture {
          val offerBuilder = createOffer()
          offerBuilder.getOfferAutoruBuilder.getVinResolutionBuilder.getResolutionBuilder.getEntriesBuilderList.asScala
            .find(_.getPart == ResolutionPart.RP_RESTRICTED)
            .get
            .setStatus(Status.ERROR)
          val offer = offerBuilder.build()

          worker.shouldProcess(offer, None).shouldProcess shouldEqual false

        }

        "pledge" in new Fixture {
          val offerBuilder = createOffer()
          offerBuilder.getOfferAutoruBuilder.getVinResolutionBuilder.getResolutionBuilder.getEntriesBuilderList.asScala
            .find(_.getPart == ResolutionPart.RP_PLEDGE)
            .get
            .setStatus(Status.ERROR)
          val offer = offerBuilder.build()

          worker.shouldProcess(offer, None).shouldProcess shouldEqual false

        }
        "last event is cancel" in new Fixture {
          val offerBuilder = createOffer()
          addHistoryElem(
            offerBuilder,
            TeleperformanceEvent.EventType.PRICE_CHANGE,
            offerBuilder.getOfferAutoru.getDocuments.getVin,
            offerBuilder.getOfferAutoruBuilder.getPriceBuilder.setPrice(100.0).build()
          )
          addHistoryElem(
            offerBuilder,
            TeleperformanceEvent.EventType.CANCEL,
            offerBuilder.getOfferAutoru.getDocuments.getVin,
            offerBuilder.getOfferAutoru.getPrice
          )
          val offer = offerBuilder.build()

          worker.shouldProcess(offer, None).shouldProcess shouldEqual false

        }
        "phone in black list" in new Fixture {
          val offerBuilder = createOffer()
          offerBuilder.getOfferAutoruBuilder.getSellerBuilder.getPhoneBuilder(0).setNumber("79161196421")
          val offer = offerBuilder.build()

          worker.shouldProcess(offer, None).shouldProcess shouldEqual false

        }
        "not allowed mark" in new Fixture {
          val offerBuilder = createOffer()
          offerBuilder.getOfferAutoruBuilder.getCarInfoBuilder.setMark("LAMBORGHINI")
          val offer = offerBuilder.build()

          worker.shouldProcess(offer, None).shouldProcess shouldEqual false

        }
      }

      "processed" when {
        "new offer was created" in new Fixture() {
          val offer: Offer = createOffer().build()

          worker.shouldProcess(offer, None).shouldProcess shouldEqual true

        }

        "old offer with empty event history" in new Fixture() {
          val offerBuilder: Offer.Builder = createOffer(getNow - 2.days.toMillis)
          val offer = offerBuilder.build()

          worker.shouldProcess(offer, None).shouldProcess shouldEqual true

        }

        "vin changed" in new Fixture() {
          val offerBuilder: Offer.Builder = createOffer()
          offerBuilder.getOfferAutoruBuilder.getPredictPriceBuilder.getAutoruBuilder.setFrom(502000)
          addHistoryElem(offerBuilder, TeleperformanceEvent.EventType.NEW, "01ab", offerBuilder.getOfferAutoru.getPrice)
          val offer = offerBuilder.build()
          worker.shouldProcess(offer, None).shouldProcess shouldEqual true

        }

        "price changed" in new Fixture() {
          val offerBuilder: Offer.Builder = createOffer()
          val priceBuilder = offerBuilder.getOfferAutoruBuilder.getPriceBuilder.clone()
          priceBuilder.setPrice(850000)
          addHistoryElem(
            offerBuilder,
            TeleperformanceEvent.EventType.NEW,
            offerBuilder.getOfferAutoru.getDocuments.getVin,
            priceBuilder.build()
          )
          val offer = offerBuilder.build()
          worker.shouldProcess(offer, None).shouldProcess shouldEqual true

        }

        "offer is banned" in new Fixture() {
          val offerBuilder: Offer.Builder = createOffer()
          addHistoryElem(
            offerBuilder,
            TeleperformanceEvent.EventType.NEW,
            offerBuilder.getOfferAutoru.getDocuments.getVin,
            offerBuilder.getOfferAutoru.getPrice
          )
          offerBuilder.addFlag(OfferFlag.OF_BANNED)
          val offer = offerBuilder.build()

          worker.shouldProcess(offer, None).shouldProcess shouldEqual true

        }

        "user is reseller" in new Fixture() {
          val offerBuilder: Offer.Builder = createOffer()
          addHistoryElem(
            offerBuilder,
            TeleperformanceEvent.EventType.NEW,
            offerBuilder.getOfferAutoru.getDocuments.getVin,
            offerBuilder.getOfferAutoru.getPrice
          )

          offerBuilder.getOfferAutoruBuilder.setReseller(true)
          val offer = offerBuilder.build()

          worker.shouldProcess(offer, None).shouldProcess shouldEqual true

        }

        "not allow to old criterias, but allow to new" in new Fixture {
          val offerBuilder: Offer.Builder = createOffer()
          offerBuilder.getOfferAutoruBuilder.getEssentialsBuilder.setYear(2015)
          offerBuilder.getOfferAutoruBuilder.getPriceBuilder.setPrice(1700000)
          offerBuilder.getOfferAutoruBuilder.getStateBuilder.setMileage(190000)
          offerBuilder.getOfferAutoruBuilder.getCarInfoBuilder.setHorsePower(290)
          offerBuilder.getOfferAutoruBuilder.getCarInfoBuilder.setMark("HUMMER")
          val offer = offerBuilder.build()

          worker.shouldProcess(offer, None).shouldProcess shouldEqual true

        }
      }
    }
  }

  def createOffer(now: Long = getNow, dealer: Boolean = false, regionId: Long = 1): Offer.Builder = {

    def createVinEntry(part: ResolutionPart, status: Status): ResolutionEntry = {
      ResolutionEntry.newBuilder().setPart(part).setStatus(status).build()
    }

    val offerBuilder = TestUtils.createOffer(now, dealer)
    offerBuilder.setOfferID(TestOfferID)
    val autoruOfferBuilder = offerBuilder.getOfferAutoruBuilder
    autoruOfferBuilder.getSellerBuilder.getPlaceBuilder.setGeobaseId(regionId)
    autoruOfferBuilder.getCarInfoBuilder.setHorsePower(190)
    autoruOfferBuilder.getPriceBuilder
      .setCreated(1L)
      .setCurrency(Currency.RUB)
      .setPrice(500000)
      .setPriceRub(500000)
      .setCurrency(Currency.RUB)
    autoruOfferBuilder.getDocumentsBuilder.setIsPtsOriginal(true)
    autoruOfferBuilder.getEssentialsBuilder.setYear(2014)
    autoruOfferBuilder.getStateBuilder.setMileage(10000)
    autoruOfferBuilder.getStateBuilder.setCondition(EXCELLENT)
    autoruOfferBuilder.getOwnershipBuilder.setPtsOwnersCount(1)
    autoruOfferBuilder.getSellerBuilder.setUserName("seller")
    autoruOfferBuilder.getSellerBuilder.addPhone(
      Phone
        .newBuilder()
        .setNumber("79998887766")
    )
    autoruOfferBuilder.getDocumentsBuilder.setVin("WVWZZZ7MZ9V008918")
    autoruOfferBuilder.getPredictPriceBuilder.getAutoruBuilder.setCurrency(Currency.RUB)
    autoruOfferBuilder.getPredictPriceBuilder.getAutoruBuilder.setFrom(450000)
    autoruOfferBuilder.getPredictPriceBuilder.getAutoruBuilder.setTo(600000)
    autoruOfferBuilder.getPredictPriceBuilder.setVersion(1)
    autoruOfferBuilder.getVinResolutionBuilder
      .setVersion(1)
      .getResolutionBuilder
      .setVersion(1)
      .addAllEntries(
        Seq(
          createVinEntry(ResolutionPart.RP_PLEDGE, Status.OK),
          createVinEntry(ResolutionPart.RP_RESTRICTED, Status.OK)
        ).asJava
      )

    offerBuilder
  }

  private def addHistoryElem(offerBuilder: Offer.Builder,
                             eventType: TeleperformanceEvent.EventType,
                             vin: String,
                             price: Price): Unit = {
    val item = TeleperformanceEvent.newBuilder()
    item.setEventType(eventType)
    item.setVin(vin)
    item.setPrice(price)
    item.setTimestampCreate(getNow)
    offerBuilder.getOfferAutoruBuilder.addTeleperformanceEventHistory(item)
  }

}
