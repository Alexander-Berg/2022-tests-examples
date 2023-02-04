package ru.yandex.vos2.autoru.multiposting

import com.google.protobuf.Timestamp
import io.prometheus.client.{Collector, CollectorRegistry}
import org.scalatest._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.Multiposting.{Classified => ApiClassified}
import ru.auto.api.ApiOfferModel.{Category, OfferStatus}
import ru.auto.multiposting.EventModel.ClassifiedUpdateEvent
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.prometheus.PrometheusRegistry
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.AutoruModel.AutoruOffer.CarDocuments
import ru.yandex.vos2.BasicsModel.CompositeStatus
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.OfferModel.Multiposting.{Classified => VosClassified}
import ru.yandex.vos2.OfferModel.{OfferFlag, OfferService, Offer => VosOffer}
import ru.yandex.vos2.autoru.multiposting.email.BannedOffer
import ru.yandex.vos2.dao.offers.OfferDao
import ru.yandex.vos2.services.sender.SenderClient

import java.util.concurrent.TimeUnit
import java.util.function.Predicate
import scala.concurrent.ExecutionContext.Implicits.global

class MultipostingSyncClassifiedsProcessorSpec
  extends AnyWordSpec
  with MockitoSupport
  with Matchers
  with TestOperationalSupport {

  val offerDao: OfferDao = mock[OfferDao]

  val metrics: PrometheusRegistry = new PrometheusRegistry {
    override def asCollectorRegistry(): CollectorRegistry = ???
    override def register[C <: Collector](c: C): C = c
    override def unregister(predicate: Predicate[Collector]): Unit = ???
  }

  val senderClient: SenderClient = mock[SenderClient]

  val processor =
    new MultipostingSyncClassifiedsProcessor(() => offerDao, metrics, senderClient, autoruFeedEmail = null)

  "getEventIfItBannedAndFromAvito" should {
    "get value from AVITO changed to BANNED" in {
      val eventClassified: ApiClassified =
        ApiClassified
          .newBuilder()
          .setName(ApiClassified.ClassifiedName.AVITO)
          .setUrl("classified-url")
          .setStatus(OfferStatus.BANNED)
          .setDetailedStatus("detailed-banned-status")
          .addErrors {
            ApiClassified.DetailedInformation
              .newBuilder()
              .setTitle("error")
              .setDescription("fatal-error")
          }
          .addWarnings {
            ApiClassified.DetailedInformation
              .newBuilder()
              .setTitle("warning")
              .setDescription("notice")
              .setManual("manual-url")
          }
          .addWarnings {
            ApiClassified.DetailedInformation
              .newBuilder()
              .setTitle("another-warning")
              .setDescription("notice 2")
          }
          .build()

      val event: ClassifiedUpdateEvent =
        ClassifiedUpdateEvent
          .newBuilder()
          .setVin("ABC")
          .setClientId(0L)
          .setClassified(eventClassified)
          .build()

      val documents = CarDocuments
        .newBuilder()
        .setVin("ABC")
        .build()

      val autoruOffer = AutoruOffer
        .newBuilder()
        .setDocuments(documents)
        .setVersion(1)
        .setCategory(Category.CARS)
        .build()

      val offer = VosOffer
        .newBuilder()
        .setOfferID("111-fff")
        .addFlag(OfferFlag.OF_INACTIVE)
        .setOfferService(OfferService.OFFER_AUTO)
        .setOfferAutoru(autoruOffer)
        .setTimestampUpdate(0L)
        .setUserRef("a_123")
        .build()

      val eventMap = Map(
        "ABC" -> event
      )

      val expected = BannedOffer(
        offerId = "111-fff",
        vin = "ABC",
        clientId = 0L,
        url = "classified-url",
        classified = ApiClassified.ClassifiedName.AVITO.name(),
        detailedStatus = "detailed-banned-status",
        description = "error\nfatal-error\nwarning\nnotice\nanother-warning\nnotice 2"
      )

      processor.listOfBannedOffer(Seq(offer), eventMap) shouldBe Seq(expected)
    }

    "get value from AVITO changed to BANNED from ACTIVE" in {
      val eventClassified: ApiClassified =
        ApiClassified
          .newBuilder()
          .setName(ApiClassified.ClassifiedName.AVITO)
          .setUrl("classified-url")
          .setStatus(OfferStatus.BANNED)
          .setDetailedStatus("detailed-banned-status")
          .addErrors {
            ApiClassified.DetailedInformation
              .newBuilder()
              .setTitle("error")
              .setDescription("fatal-error")
          }
          .addWarnings {
            ApiClassified.DetailedInformation
              .newBuilder()
              .setTitle("warning")
              .setDescription("notice")
              .setManual("manual-url")
          }
          .addWarnings {
            ApiClassified.DetailedInformation
              .newBuilder()
              .setTitle("another-warning")
              .setDescription("notice 2")
          }
          .build()

      val event: ClassifiedUpdateEvent =
        ClassifiedUpdateEvent
          .newBuilder()
          .setVin("ABC")
          .setClientId(0L)
          .setClassified(eventClassified)
          .build()

      val documents = CarDocuments
        .newBuilder()
        .setVin("ABC")
        .build()

      val autoruOffer = AutoruOffer
        .newBuilder()
        .setDocuments(documents)
        .setVersion(1)
        .setCategory(Category.CARS)
        .build()

      val offer = VosOffer
        .newBuilder()
        .setOfferID("111-fff")
        .addFlag(OfferFlag.OF_INACTIVE)
        .setOfferService(OfferService.OFFER_AUTO)
        .setOfferAutoru(autoruOffer)
        .setTimestampUpdate(0L)
        .setUserRef("a_123")
        .setMultiposting {
          OfferModel.Multiposting
            .newBuilder()
            .addClassifieds {
              VosClassified
                .newBuilder()
                .setName(VosClassified.ClassifiedName.AVITO)
                .setStatus(CompositeStatus.CS_ACTIVE)
            }
        }
        .build()

      val eventMap = Map(
        "ABC" -> event
      )

      val expected = BannedOffer(
        offerId = "111-fff",
        vin = "ABC",
        clientId = 0L,
        url = "classified-url",
        classified = ApiClassified.ClassifiedName.AVITO.name(),
        detailedStatus = "detailed-banned-status",
        description = "error\nfatal-error\nwarning\nnotice\nanother-warning\nnotice 2"
      )

      processor.listOfBannedOffer(Seq(offer), eventMap) shouldBe Seq(expected)
    }

    "get None from AUTORU changed to BANNED" in {
      val eventClassified: ApiClassified =
        ApiClassified
          .newBuilder()
          .setName(ApiClassified.ClassifiedName.AUTORU)
          .setUrl("classified-url")
          .setStatus(OfferStatus.BANNED)
          .setDetailedStatus("detailed-banned-status")
          .addErrors {
            ApiClassified.DetailedInformation
              .newBuilder()
              .setTitle("error")
              .setDescription("fatal-error")
          }
          .addWarnings {
            ApiClassified.DetailedInformation
              .newBuilder()
              .setTitle("warning")
              .setDescription("notice")
              .setManual("manual-url")
          }
          .addWarnings {
            ApiClassified.DetailedInformation
              .newBuilder()
              .setTitle("another-warning")
              .setDescription("notice 2")
          }
          .build()

      val event: ClassifiedUpdateEvent =
        ClassifiedUpdateEvent
          .newBuilder()
          .setVin("ABC")
          .setClassified(eventClassified)
          .build()

      val documents = CarDocuments
        .newBuilder()
        .setVin("ABC")
        .build()

      val autoruOffer = AutoruOffer
        .newBuilder()
        .setDocuments(documents)
        .setVersion(1)
        .setCategory(Category.CARS)
        .build()

      val offer = VosOffer
        .newBuilder()
        .addFlag(OfferFlag.OF_INACTIVE)
        .setOfferService(OfferService.OFFER_AUTO)
        .setOfferAutoru(autoruOffer)
        .setTimestampUpdate(0L)
        .setUserRef("a_123")
        .build()

      val eventMap = Map(
        "ABC" -> event
      )

      processor.listOfBannedOffer(Seq(offer), eventMap) shouldBe List.empty[BannedOffer]
    }

    "get None from AVITO changed to ACTIVE" in {
      val eventClassified: ApiClassified =
        ApiClassified
          .newBuilder()
          .setName(ApiClassified.ClassifiedName.AVITO)
          .setUrl("classified-url")
          .setStatus(OfferStatus.ACTIVE)
          .setDetailedStatus("detailed-banned-status")
          .addErrors {
            ApiClassified.DetailedInformation
              .newBuilder()
              .setTitle("error")
              .setDescription("fatal-error")
          }
          .addWarnings {
            ApiClassified.DetailedInformation
              .newBuilder()
              .setTitle("warning")
              .setDescription("notice")
              .setManual("manual-url")
          }
          .addWarnings {
            ApiClassified.DetailedInformation
              .newBuilder()
              .setTitle("another-warning")
              .setDescription("notice 2")
          }
          .build()

      val event: ClassifiedUpdateEvent =
        ClassifiedUpdateEvent
          .newBuilder()
          .setVin("ABC")
          .setClassified(eventClassified)
          .build()

      val documents = CarDocuments
        .newBuilder()
        .setVin("ABC")
        .build()

      val autoruOffer = AutoruOffer
        .newBuilder()
        .setDocuments(documents)
        .setVersion(1)
        .setCategory(Category.CARS)
        .build()

      val offer = VosOffer
        .newBuilder()
        .addFlag(OfferFlag.OF_INACTIVE)
        .setOfferService(OfferService.OFFER_AUTO)
        .setOfferAutoru(autoruOffer)
        .setTimestampUpdate(0L)
        .setUserRef("a_123")
        .build()

      val eventMap = Map(
        "ABC" -> event
      )

      processor.listOfBannedOffer(Seq(offer), eventMap) shouldBe List.empty[BannedOffer]
    }
    "get None from AVITO status not changed" in {
      val eventClassified: ApiClassified =
        ApiClassified
          .newBuilder()
          .setName(ApiClassified.ClassifiedName.AVITO)
          .setUrl("classified-url")
          .setStatus(OfferStatus.BANNED)
          .setDetailedStatus("detailed-banned-status")
          .addErrors {
            ApiClassified.DetailedInformation
              .newBuilder()
              .setTitle("error")
              .setDescription("fatal-error")
          }
          .addWarnings {
            ApiClassified.DetailedInformation
              .newBuilder()
              .setTitle("warning")
              .setDescription("notice")
              .setManual("manual-url")
          }
          .addWarnings {
            ApiClassified.DetailedInformation
              .newBuilder()
              .setTitle("another-warning")
              .setDescription("notice 2")
          }
          .build()

      val event: ClassifiedUpdateEvent =
        ClassifiedUpdateEvent
          .newBuilder()
          .setVin("ABC")
          .setClassified(eventClassified)
          .build()

      val documents = CarDocuments
        .newBuilder()
        .setVin("ABC")
        .build()

      val autoruOffer = AutoruOffer
        .newBuilder()
        .setDocuments(documents)
        .setVersion(1)
        .setCategory(Category.CARS)
        .build()

      val offer = VosOffer
        .newBuilder()
        .addFlag(OfferFlag.OF_MIGRATED)
        .setOfferService(OfferService.OFFER_AUTO)
        .setOfferAutoru(autoruOffer)
        .setTimestampUpdate(0L)
        .setUserRef("a_123")
        .setMultiposting {
          OfferModel.Multiposting
            .newBuilder()
            .addClassifieds {
              VosClassified
                .newBuilder()
                .setName(VosClassified.ClassifiedName.AVITO)
                .setStatus(CompositeStatus.CS_BANNED)
            }
        }
        .build()

      val eventMap = Map(
        "ABC" -> event
      )

      processor.listOfBannedOffer(Seq(offer), eventMap) shouldBe List.empty[BannedOffer]
    }
  }

  "processClassified" should {
    "patch classified data only on filled fields [first sync]" in {
      val actualClassified: VosClassified.Builder =
        VosClassified
          .newBuilder()
          .setName(VosClassified.ClassifiedName.AUTORU)
          .setEnabled(true)
          .setCreateDate(123L)
          .setExpireDate(321L)
          .setStatus(CompositeStatus.CS_INACTIVE)

      val eventClassified: ApiClassified =
        ApiClassified
          .newBuilder()
          .setName(ApiClassified.ClassifiedName.AUTORU)
          .setExpireDate(432L)
          .setStartDate(222L)
          .setId("classified-offerId")
          .setUrl("classified-url")
          .setStatus(OfferStatus.BANNED)
          .setDetailedStatus("detailed-banned-status")
          .addErrors {
            ApiClassified.DetailedInformation
              .newBuilder()
              .setTitle("error")
              .setDescription("fatal-error")
          }
          .addWarnings {
            ApiClassified.DetailedInformation
              .newBuilder()
              .setTitle("warning")
              .setDescription("notice")
              .setManual("manual-url")
          }
          .addWarnings {
            ApiClassified.DetailedInformation
              .newBuilder()
              .setTitle("another-warning")
              .setDescription("notice 2")
          }
          .addServicePrices {
            ApiClassified.ServicePrice
              .newBuilder()
              .setService("x10")
              .setPrice(200L)
          }
          .addServicePrices {
            ApiClassified.ServicePrice
              .newBuilder()
              .setService("x5")
              .setPrice(50L)
          }
          .build()

      val event: ClassifiedUpdateEvent =
        ClassifiedUpdateEvent
          .newBuilder()
          .setVin("ABC")
          .setClassified(eventClassified)
          .build()

      val expected: VosClassified =
        VosClassified
          .newBuilder()
          .setName(VosClassified.ClassifiedName.AUTORU)
          .setEnabled(true)
          .setCreateDate(123L)
          .setExpireDate(432L)
          .setStartDate(222L)
          .setId("classified-offerId")
          .setUrl("classified-url")
          .setStatus(CompositeStatus.CS_BANNED)
          .setDetailedStatus("detailed-banned-status")
          .addErrors {
            VosClassified.DetailedInformation
              .newBuilder()
              .setTitle("error")
              .setDescription("fatal-error")
          }
          .addWarnings {
            VosClassified.DetailedInformation
              .newBuilder()
              .setTitle("warning")
              .setDescription("notice")
              .setManual("manual-url")
          }
          .addWarnings {
            VosClassified.DetailedInformation
              .newBuilder()
              .setTitle("another-warning")
              .setDescription("notice 2")
          }
          .addServicePrices {
            VosClassified.ServicePrice
              .newBuilder()
              .setService("x10")
              .setPrice(200L)
          }
          .addServicePrices {
            VosClassified.ServicePrice
              .newBuilder()
              .setService("x5")
              .setPrice(50L)
          }
          .build()

      processor.processClassified(actualClassified, event).build() shouldBe expected
    }

    "patch classified data only on filled fields [not first sync]" in {
      val actualClassified: VosClassified.Builder =
        VosClassified
          .newBuilder()
          .setName(VosClassified.ClassifiedName.AUTORU)
          .setEnabled(true)
          .setCreateDate(123L)
          .setStartDate(222L)
          .setExpireDate(321L)
          .setStatus(CompositeStatus.CS_INACTIVE)
          .setId("classified-offerId")
          .setUrl("classified-url")

      val eventClassified: ApiClassified =
        ApiClassified
          .newBuilder()
          .setName(ApiClassified.ClassifiedName.AUTORU)
          .setStartDate(555L)
          .build()

      val event: ClassifiedUpdateEvent =
        ClassifiedUpdateEvent
          .newBuilder()
          .setVin("ABC")
          .setClassified(eventClassified)
          .build()

      val expected: VosClassified =
        VosClassified
          .newBuilder()
          .setName(VosClassified.ClassifiedName.AUTORU)
          .setEnabled(true)
          .setCreateDate(123L)
          .setExpireDate(321L)
          .setStartDate(555L)
          .setId("classified-offerId")
          .setUrl("classified-url")
          .setStatus(CompositeStatus.CS_INACTIVE)
          .build()

      processor.processClassified(actualClassified, event).build() shouldBe expected
    }

    "patch classified with only filled services" in {
      val actualClassified: VosClassified.Builder =
        VosClassified
          .newBuilder()
          .setName(VosClassified.ClassifiedName.AUTORU)
          .setEnabled(true)
          .setCreateDate(111L)
          .setExpireDate(123L)
          .setStatus(CompositeStatus.CS_INACTIVE)
          .addServices {
            VosClassified.Service
              .newBuilder()
              .setService("inactiveService")
              .setCreateDate(123L)
              .setExpireDate(321L)
              .setIsActive(false)
          }
          .addServices {
            VosClassified.Service
              .newBuilder()
              .setService("activeService-1")
              .setCreateDate(234L)
              .setExpireDate(432L)
              .setIsActive(true)
          }
          .addServices {
            VosClassified.Service
              .newBuilder()
              .setService("activeService-2")
              .setCreateDate(456L)
              .setExpireDate(654L)
              .setIsActive(true)
          }

      val eventClassified: ApiClassified =
        ApiClassified
          .newBuilder()
          .setName(ApiClassified.ClassifiedName.AUTORU)
          .setStatus(OfferStatus.ACTIVE)
          .setStartDate(555L)
          .setId("classified-offerId")
          .addServices {
            ApiClassified.Service
              .newBuilder()
              .setService("activeService-1")
              .setStartDate(777L)
              .setExpireDate(999L)
          }
          .build()

      val eventTs = Timestamp
        .newBuilder()
        .setNanos(TimeUnit.MILLISECONDS.toNanos(333L).toInt)
        .build()

      val event: ClassifiedUpdateEvent =
        ClassifiedUpdateEvent
          .newBuilder()
          .setTimestamp(eventTs)
          .setVin("ABC")
          .setClassified(eventClassified)
          .build()

      val expected: VosClassified =
        VosClassified
          .newBuilder()
          .setName(VosClassified.ClassifiedName.AUTORU)
          .setEnabled(true)
          .setCreateDate(111L)
          .setExpireDate(123L)
          .setStartDate(555L)
          .setId("classified-offerId")
          .setStatus(CompositeStatus.CS_ACTIVE)
          .addServices {
            VosClassified.Service
              .newBuilder()
              .setService("inactiveService")
              .setCreateDate(123L)
              .setExpireDate(321L)
              .setIsActive(false)
          }
          .addServices {
            VosClassified.Service
              .newBuilder()
              .setService("activeService-1")
              .setCreateDate(234L)
              .setStartDate(777L)
              .setExpireDate(999L)
              .setIsActive(true)
          }
          .addServices {
            VosClassified.Service
              .newBuilder()
              .setService("activeService-2")
              .setCreateDate(456L)
              .setExpireDate(654L)
              .setIsActive(true)
          }
          .build()

      processor.processClassified(actualClassified, event).build() shouldBe expected
    }

    "patch classified with only filled services, ignore new services" in {
      val actualClassified: VosClassified.Builder =
        VosClassified
          .newBuilder()
          .setName(VosClassified.ClassifiedName.AUTORU)
          .setEnabled(true)
          .setExpireDate(123L)
          .setStatus(CompositeStatus.CS_INACTIVE)
          .addServices {
            VosClassified.Service
              .newBuilder()
              .setService("service")
              .setCreateDate(111L)
              .setExpireDate(332L)
              .setIsActive(true)
          }
          .addServices {
            VosClassified.Service
              .newBuilder()
              .setService("service")
              .setCreateDate(333L)
              .setExpireDate(444L)
              .setIsActive(true)
          }

      val eventClassified: ApiClassified =
        ApiClassified
          .newBuilder()
          .setName(ApiClassified.ClassifiedName.AUTORU)
          .setStatus(OfferStatus.ACTIVE)
          .setStartDate(555L)
          .setId("classified-offerId")
          .addServices {
            ApiClassified.Service
              .newBuilder()
              .setService("service")
              .setStartDate(777L)
          }
          .build()

      val eventTs = Timestamp
        .newBuilder()
        .setNanos(TimeUnit.MILLISECONDS.toNanos(200L).toInt)
        .build()

      val event: ClassifiedUpdateEvent =
        ClassifiedUpdateEvent
          .newBuilder()
          .setTimestamp(eventTs)
          .setVin("ABC")
          .setClassified(eventClassified)
          .build()

      val expected: VosClassified =
        VosClassified
          .newBuilder()
          .setName(VosClassified.ClassifiedName.AUTORU)
          .setEnabled(true)
          .setExpireDate(123L)
          .setStartDate(555L)
          .setId("classified-offerId")
          .setStatus(CompositeStatus.CS_ACTIVE)
          .addServices {
            VosClassified.Service
              .newBuilder()
              .setService("service")
              .setCreateDate(111L)
              .setStartDate(777L)
              .setExpireDate(332L)
              .setIsActive(true)
          }
          .addServices {
            VosClassified.Service
              .newBuilder()
              .setService("service")
              .setCreateDate(333L)
              .setExpireDate(444L)
              .setIsActive(true)
          }
          .build()

      processor.processClassified(actualClassified, event).build() shouldBe expected
    }

    "update repeated values without duplication" in {
      val actualClassified: VosClassified.Builder =
        VosClassified
          .newBuilder()
          .setName(VosClassified.ClassifiedName.AVITO)
          .setEnabled(true)
          .addServicePrices {
            VosClassified.ServicePrice
              .newBuilder()
              .setService("x10_1")
              .setPrice(111L)
          }
          .addServicePrices {
            VosClassified.ServicePrice
              .newBuilder()
              .setService("x2_7")
              .setPrice(222L)
          }
          .addWarnings {
            VosClassified.DetailedInformation
              .newBuilder()
              .setDescription("some-warning")
          }
          .addErrors {
            VosClassified.DetailedInformation
              .newBuilder()
              .setDescription("old-error")
          }

      val eventClassified: ApiClassified =
        ApiClassified
          .newBuilder()
          .setName(ApiClassified.ClassifiedName.AVITO)
          .setId("classified-offerId")
          .addServicePrices {
            ApiClassified.ServicePrice
              .newBuilder()
              .setService("x10_1")
              .setPrice(112L)
          }
          .addServicePrices {
            ApiClassified.ServicePrice
              .newBuilder()
              .setService("x2_7")
              .setPrice(223L)
          }
          .addServicePrices {
            ApiClassified.ServicePrice
              .newBuilder()
              .setService("x7_1")
              .setPrice(333L)
          }
          .addErrors {
            ApiClassified.DetailedInformation
              .newBuilder()
              .setDescription("new-error")
          }
          .build()

      val eventTs = Timestamp
        .newBuilder()
        .setNanos(TimeUnit.MILLISECONDS.toNanos(200L).toInt)
        .build()

      val event: ClassifiedUpdateEvent =
        ClassifiedUpdateEvent
          .newBuilder()
          .setTimestamp(eventTs)
          .setVin("ABC")
          .setClassified(eventClassified)
          .build()

      val expected: VosClassified =
        VosClassified
          .newBuilder()
          .setName(VosClassified.ClassifiedName.AVITO)
          .setEnabled(true)
          .setId("classified-offerId")
          .addServicePrices {
            VosClassified.ServicePrice
              .newBuilder()
              .setService("x10_1")
              .setPrice(112L)
          }
          .addServicePrices {
            VosClassified.ServicePrice
              .newBuilder()
              .setService("x2_7")
              .setPrice(223L)
          }
          .addServicePrices {
            VosClassified.ServicePrice
              .newBuilder()
              .setService("x7_1")
              .setPrice(333L)
          }
          .addErrors {
            VosClassified.DetailedInformation
              .newBuilder()
              .setDescription("new-error")
          }
          .build()

      processor.processClassified(actualClassified, event).build() shouldBe expected

    }

  }

  "filterRecords" should {
    def event(clientId: Long, vin: String, ts: Long): ClassifiedUpdateEvent = {
      ClassifiedUpdateEvent
        .newBuilder()
        .setClientId(clientId)
        .setVin(vin)
        .setTimestamp(Timestamp.newBuilder().setSeconds(ts).build())
        .build()
    }

    "return all records on empty feature value" in {
      val eventA = event(clientId = 123L, vin = "VIN-1", ts = 1L)
      val eventB = event(clientId = 321L, vin = "VIN-2", ts = 2L)

      val filter = ""

      processor.filterRecords(Seq(eventA, eventB), filter) should {
        contain theSameElementsAs Set(eventA, eventB)
      }
    }

    "return filtered records on VIN filter feature value" in {
      val eventA = event(clientId = 123L, vin = "VIN-3", ts = 1L)
      val eventB = event(clientId = 321L, vin = "VIN-4", ts = 2L)
      val eventC = event(clientId = 321L, vin = "VIN-5", ts = 2L)
      val eventD = event(clientId = 222L, vin = "VIN-6", ts = 2L)

      val filter = "vin:VIN-4,VIN-5"

      processor.filterRecords(Seq(eventA, eventB, eventC, eventD), filter) should {
        contain theSameElementsAs Set(eventB, eventC)
      }
    }

    "return filtered records on CLIENT filter feature value" in {
      val eventA = event(clientId = 123L, vin = "VIN-7", ts = 1L)
      val eventB = event(clientId = 321L, vin = "VIN-8", ts = 2L)
      val eventC = event(clientId = 321L, vin = "VIN-9", ts = 2L)
      val eventD = event(clientId = 222L, vin = "VIN-10", ts = 2L)

      val filter = "client:222,123"

      processor.filterRecords(Seq(eventA, eventB, eventC, eventD), filter) should {
        contain theSameElementsAs Set(eventA, eventD)
      }
    }

    "return nothing on invalid feature value" in {
      val eventA = event(clientId = 123L, vin = "VIN-11", ts = 1L)
      val eventB = event(clientId = 321L, vin = "VIN-12", ts = 2L)
      val eventC = event(clientId = 321L, vin = "VIN-13", ts = 2L)
      val eventD = event(clientId = 222L, vin = "VIN-14", ts = 2L)

      val filter = "clientd:123"

      processor.filterRecords(Seq(eventA, eventB, eventC, eventD), filter).size shouldBe 0
    }
  }

}
