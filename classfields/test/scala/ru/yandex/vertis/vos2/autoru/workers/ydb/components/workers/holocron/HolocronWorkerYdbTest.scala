package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.holocron

import com.google.protobuf.util.Timestamps
import org.apache.kafka.common.errors.RecordTooLargeException
import org.joda.time.{DateTime, DateTimeUtils}
import org.mockito.ArgumentMatchers.{eq => eeq}
import org.mockito.Mockito.{verify, verifyNoMoreInteractions}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.ApiOfferModel.Category.{CARS, MOTO, TRUCKS}
import ru.vertis.holocron.common.{Action, HoloOffer}
import ru.yandex.vertis.baker.components.workdistribution.WorkDistributionData
import ru.yandex.vertis.baker.components.workersfactory.workers.WorkersFactory
import ru.yandex.vertis.broker.client.simple.BrokerClient
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.tracing.{EndpointConfig, LocalTracingSupport, Traced, TracingSupport}
import ru.yandex.vertis.validation.MessageValidator
import ru.yandex.vertis.validation.model.{Invalid, MissingRequiredField, Valid}
import ru.yandex.vertis.ydb.skypper.YdbWrapper
import ru.yandex.vos2.BasicsModel.CompositeStatus
import ru.yandex.vos2.OfferModel.{HolocronEvent, Offer, OfferFlag}
import ru.yandex.vos2.autoru.dao.offers.AutoruOfferDao
import ru.yandex.vos2.autoru.dao.offers.holocron.HolocronUtils
import ru.yandex.vos2.autoru.dao.offers.holocron.converters.broker.cars.HoloFullCarOfferConverter
import ru.yandex.vos2.autoru.dao.offers.holocron.converters.cars.{HolocronCarsConverter, HolocronExtendedCarsConverter}
import ru.yandex.vos2.autoru.dao.offers.holocron.sender.HolocronSender
import ru.yandex.vos2.autoru.model.TestUtils.createOffer
import ru.yandex.vos2.autoru.utils.time.TimeService
import ru.yandex.vos2.commonfeatures.{FeatureRegistryFactory, FeaturesManager}
import ru.yandex.vos2.{OfferID, OfferModel}
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicReference

import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.YdbWorkerTestImpl

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class HolocronWorkerYdbTest extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty

  val moment = new DateTime(2020, 5, 6, 0, 0, 0, 0)

  abstract private class Fixture {
    val featuresRegistry = FeatureRegistryFactory.inMemory()
    val featuresManager = new FeaturesManager(featuresRegistry)
    featuresRegistry.updateFeature(
      featuresManager.SendToHolocronYdb.name,
      featuresManager.SendToHolocronYdb.value.copy(generation = 2)
    )

    val timeService = mock[TimeService]
    when(timeService.getNow).thenReturn(moment)

    val daoMocked = mock[AutoruOfferDao]
    val ydbMocked = mock[YdbWrapper]

    val holocronExtendedConverter: HolocronExtendedCarsConverter = mock[HolocronExtendedCarsConverter]
    val holocronConverter: HolocronCarsConverter = mock[HolocronCarsConverter]

    val holocronValidator: MessageValidator = mock[MessageValidator]
    val holocronSender: HolocronSender = mock[HolocronSender]
    val brokerClient: BrokerClient = mock[BrokerClient]

    val worker = new HolocronWorkerYdb(
      Map(
        CARS -> holocronExtendedConverter,
        TRUCKS -> holocronExtendedConverter,
        MOTO -> holocronExtendedConverter
      ),
      holocronValidator,
      holocronSender,
      brokerClient,
      featuresManager.SendToHolocronYdb,
      featuresManager.DelaySendOldToHolocron,
      featuresManager.SendHolocronToBroker,
      featuresManager.StrictHolocronValidation,
      timeService,
      maxLastEventsSize = 3
    ) with YdbWorkerTestImpl

    val simpleWorker = new HolocronWorkerYdb(
      Map(
        CARS -> holocronConverter,
        TRUCKS -> holocronConverter,
        MOTO -> holocronConverter
      ),
      holocronValidator,
      holocronSender,
      brokerClient,
      featuresManager.SendToHolocronYdb,
      featuresManager.DelaySendOldToHolocron,
      featuresManager.SendHolocronToBroker,
      featuresManager.StrictHolocronValidation,
      timeService
    ) with YdbWorkerTestImpl

  }
  "Holocronstage" should {
    "not process" when {
      "draft" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        offer.addFlag(OfferModel.OfferFlag.OF_DRAFT)
        worker.shouldProcess(offer.build(), None).shouldProcess shouldBe false
      }

      "unknown category" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        offer.getOfferAutoruBuilder.setCategory(Category.CATEGORY_UNKNOWN)
        worker.shouldProcess(offer.build(), None).shouldProcess shouldBe false
      }
    }
    "process simple holo" when {
      "cars offer" in new Fixture {
        val offer: Offer.Builder = createOffer(category = Category.CARS)
        simpleWorker.shouldProcess(offer.build(), None).shouldProcess shouldBe true
      }

      "trucks offer" in new Fixture {
        val offer: Offer.Builder = createOffer(category = Category.TRUCKS)
        simpleWorker.shouldProcess(offer.build(), None).shouldProcess shouldBe true
      }

      "moto offer" in new Fixture {
        val offer: Offer.Builder = createOffer(category = Category.MOTO)
        simpleWorker.shouldProcess(offer.build(), None).shouldProcess shouldBe true
      }
    }
    "not process extended holo without simple" when {
      "cars offer" in new Fixture {
        val offer: Offer.Builder = createOffer(category = Category.CARS)
        worker.shouldProcess(offer.build(), None).shouldProcess shouldBe false
      }

      "trucks offer" in new Fixture {
        val offer: Offer.Builder = createOffer(category = Category.TRUCKS)
        worker.shouldProcess(offer.build(), None).shouldProcess shouldBe false
      }

      "moto offer" in new Fixture {
        val offer: Offer.Builder = createOffer(category = Category.MOTO)
        worker.shouldProcess(offer.build(), None).shouldProcess shouldBe false
      }
    }

    "process extended holo after simple" when {
      "cars offer" in new Fixture {
        val offer: Offer.Builder = createOffer(category = Category.CARS)
        simpleWorker.shouldProcess(offer.build(), None).shouldProcess shouldBe true
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        when(holocronConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Invalid(Seq(MissingRequiredField("field"))))

        val newOffer = simpleWorker.process(offer.build(), None).updateOfferFunc.get(offer.build())
        worker.shouldProcess(newOffer, None).shouldProcess shouldBe true
      }

      "trucks offer" in new Fixture {
        val offer: Offer.Builder = createOffer(category = Category.TRUCKS)
        simpleWorker.shouldProcess(offer.build(), None).shouldProcess shouldBe true
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        when(holocronConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Invalid(Seq(MissingRequiredField("field"))))

        val newOffer = simpleWorker.process(offer.build(), None).updateOfferFunc.get(offer.build())
        worker.shouldProcess(newOffer, None).shouldProcess shouldBe true
      }

      "moto offer" in new Fixture {
        val offer: Offer.Builder = createOffer(category = Category.MOTO)
        simpleWorker.shouldProcess(offer.build(), None).shouldProcess shouldBe true
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        when(holocronConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Invalid(Seq(MissingRequiredField("field"))))

        val newOffer = simpleWorker.process(offer.build(), None).updateOfferFunc.get(offer.build())
        worker.shouldProcess(newOffer, None).shouldProcess shouldBe true
      }
    }

    "not trying to send" when {
      "last events not corrupted but time is the same" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        offer.addFlag(OfferFlag.OF_INACTIVE)
        offer.getHolocronStatusBuilder
          .setLastSentChangeVersion(2)
          .setLastSentHoloTimestamp(Timestamps.fromSeconds(1))
          .addLastEvents(
            HolocronEvent.newBuilder().setWasSent(true).setChangeVersion(1).setHoloTimestamp(Timestamps.fromSeconds(1))
          )
          .addLastEvents(
            HolocronEvent.newBuilder().setWasSent(true).setChangeVersion(2).setHoloTimestamp(Timestamps.fromSeconds(1))
          )
        when(holocronValidator.validate(?)).thenReturn(Valid)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(
          offer.getOfferID,
          action = Action.DEACTIVATE,
          holoTimestamp = moment,
          changeVersion = offer.getHolocronStatus.getLastSentChangeVersion + 1
        )
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        val result = worker.process(offer.build(), None)
        result.updateOfferFunc shouldBe None

        offer.getHolocronStatus.getLastEventsCount shouldBe 2
        offer.getHolocronStatus.getLastEvents(1).getChangeVersion shouldBe 2
        offer.getHolocronStatus.getLastSentChangeVersion shouldBe 2
        offer.getHolocronStatus.getLastSentHoloTimestamp shouldBe Timestamps.fromSeconds(1)
        verifyNoMoreInteractions(holocronSender)
        verifyNoMoreInteractions(brokerClient)

      }

      "old valid event and feature is enabled" in new Fixture {
        featuresRegistry.updateFeature(
          featuresManager.DelaySendOldToHolocron.name,
          featuresManager.DelaySendOldToHolocron.value.copy(value = true)
        )
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID, holoTimestamp = moment.minusDays(31))
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Valid)
        val result = worker.process(offer.build(), None)

        result.nextCheck.nonEmpty shouldBe true
        offer.hasHolocronStatus shouldBe false
        offer.hasSimpleHolocronStatus shouldBe false
        result.updateOfferFunc shouldBe None

        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
        verifyNoMoreInteractions(holocronSender)
        verifyNoMoreInteractions(brokerClient)
        featuresRegistry.updateFeature(
          featuresManager.DelaySendOldToHolocron.name,
          featuresManager.DelaySendOldToHolocron.value.copy(value = false)
        )
      }

      "old invalid event and feature is enabled" in new Fixture {
        featuresRegistry.updateFeature(
          featuresManager.DelaySendOldToHolocron.name,
          featuresManager.DelaySendOldToHolocron.value.copy(value = true)
        )
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer =
          randomCarsFullHoloOffer(offer.getOfferID, action = Action.DEACTIVATE, holoTimestamp = moment.minusDays(31))
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Invalid(Seq(MissingRequiredField("field"))))
        val result = worker.process(offer.build(), None)

        result.nextCheck.nonEmpty shouldBe true
        result.updateOfferFunc shouldBe None

        offer.hasHolocronStatus shouldBe false
        offer.hasSimpleHolocronStatus shouldBe false
        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
        verifyNoMoreInteractions(holocronSender)
        verifyNoMoreInteractions(brokerClient)
        featuresRegistry.updateFeature(
          featuresManager.DelaySendOldToHolocron.name,
          featuresManager.DelaySendOldToHolocron.value.copy(value = false)
        )
      }

      "no last sent status? all events are wasSent=false" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        val hash: String = HolocronUtils.getHash(holoOffer)
        offer.getHolocronStatusBuilder
          .addLastEvents(
            HolocronEvent
              .newBuilder()
              .setChangeVersion(1)
              .setHolocronHash(hash)
              .setOfferStatus(CompositeStatus.CS_ACTIVE)
              .setHoloTimestamp(Timestamps.fromSeconds(1))
          )
          .addLastEvents(
            HolocronEvent
              .newBuilder()
              .setChangeVersion(2)
              .setHolocronHash(hash)
              .setOfferStatus(CompositeStatus.CS_ACTIVE)
              .setHoloTimestamp(Timestamps.fromSeconds(2))
          )
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        val result = worker.process(offer.build(), None)

        result.nextCheck shouldBe None
        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verifyNoMoreInteractions(holocronValidator)
      }

      "feature generation is zero in last event" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        val hash: String = HolocronUtils.getHash(holoOffer)
        offer.getHolocronStatusBuilder
          .setLastSentHolocronHash(hash)
          .addLastEventsBuilder()
          .setHolocronHash(hash)
          .setWasSent(true)
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        val result = worker.process(offer.build(), None)

        result.nextCheck shouldBe None
        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verifyNoMoreInteractions(holocronValidator)
        verifyNoMoreInteractions(holocronSender)
        verifyNoMoreInteractions(brokerClient)
      }

      "feature generation changed, last event wasSent=true" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        val hash: String = HolocronUtils.getHash(holoOffer)
        offer.getHolocronStatusBuilder
          .setLastSentHolocronHash(hash)
          .addLastEventsBuilder()
          .setHolocronHash(hash)
          .setFeatureGeneration(1)
          .setWasSent(true)
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        val result = worker.process(offer.build(), None)

        result.nextCheck shouldBe None
        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verifyNoMoreInteractions(holocronValidator)
        verifyNoMoreInteractions(holocronSender)
        verifyNoMoreInteractions(brokerClient)
      }

      "first offer status is inactive" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        offer.addFlag(OfferFlag.OF_INACTIVE)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID, action = Action.DEACTIVATE)
        val holocronHash: String = HolocronUtils.getHash(holoOffer)
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        val result = worker.process(offer.build(), None)

        result.nextCheck shouldBe None
        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verifyNoMoreInteractions(holocronValidator)
        verifyNoMoreInteractions(holocronSender)
        verifyNoMoreInteractions(brokerClient)
      }

      "both inactive and last events exist" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        offer.addFlag(OfferFlag.OF_INACTIVE)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        val holocronHash: String = HolocronUtils.getHash(holoOffer)
        offer.getHolocronStatusBuilder
          .setLastSentOfferStatus(CompositeStatus.CS_INACTIVE)
          .addLastEventsBuilder()
          .setOfferStatus(CompositeStatus.CS_INACTIVE)
          .setWasSent(true)
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        val result = worker.process(offer.build(), None)

        result.nextCheck shouldBe None

        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verifyNoMoreInteractions(holocronValidator)
        verifyNoMoreInteractions(holocronSender)
        verifyNoMoreInteractions(brokerClient)
      }

      "skip resend DEACTIVATE event if last events exist" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        val hash: String = HolocronUtils.getHash(holoOffer)
        offer.getHolocronStatusBuilder
          .setLastSentHolocronHash(hash)
          .setLastSentOfferStatus(CompositeStatus.CS_INACTIVE)
          .addLastEventsBuilder()
          .setOfferStatus(CompositeStatus.CS_INACTIVE)
          .setHolocronHash(hash)
          .setFeatureGeneration(1)
          .setWasSent(true)
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        val result = worker.process(offer.build(), None)

        result.nextCheck shouldBe None

        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verifyNoMoreInteractions(holocronValidator)
        verifyNoMoreInteractions(holocronSender)
        verifyNoMoreInteractions(brokerClient)
      }

      "last event is fine" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        val hash: String = HolocronUtils.getHash(holoOffer)
        offer.getHolocronStatusBuilder
          .setLastSentHolocronHash(hash)
          .addLastEventsBuilder()
          .setHolocronHash(hash)
          .setWasSent(true)
          .setFeatureGeneration(2)
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        val result = worker.process(offer.build(), None)

        result.nextCheck shouldBe None

        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verifyNoMoreInteractions(holocronValidator)
        verifyNoMoreInteractions(holocronSender)
        verifyNoMoreInteractions(brokerClient)
      }

      "last event has validation error" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        val hash: String = HolocronUtils.getHash(holoOffer)
        offer.getHolocronStatusBuilder
          .addLastEventsBuilder()
          .setHolocronHash(hash)
          .setFeatureGeneration(2)
          .setValidationError("error")
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        val result = worker.process(offer.build(), None)

        result.nextCheck shouldBe None

        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verifyNoMoreInteractions(holocronValidator)
        verifyNoMoreInteractions(holocronSender)
        verifyNoMoreInteractions(brokerClient)
      }

      "holo offer is the same except timestamps, changeVersion and rawAuto" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)

        val holoOfferBuilder: HoloOffer.Builder = HoloOffer.newBuilder()
        holoOfferBuilder.setChangeVersion(16)
        holoOfferBuilder.getTimestampBuilder.setSeconds(100500)
        holoOfferBuilder.getVosCarBuilder.getUpdatedBuilder.setSeconds(100500)
        val holoOffer1: HoloOffer = holoOfferBuilder.build()

        val holocronHash: String = HolocronUtils.getHash(holoOffer1)
        offer.getHolocronStatusBuilder
          .setLastSentChangeVersion(15)
          .setLastSentHolocronHash(holocronHash)
          .addLastEventsBuilder()
          .setHolocronHash(holocronHash)
          .setChangeVersion(15)
          .setWasSent(true)

        holoOfferBuilder.setChangeVersion(17)
        holoOfferBuilder.getTimestampBuilder.setSeconds(100501)
        holoOfferBuilder.getVosCarBuilder.getUpdatedBuilder.setSeconds(100501)
        holoOfferBuilder.getRawAutoBuilder.setDescription("x")
        val holoOffer2: HoloOffer = holoOfferBuilder.build()
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer2)

        val result = worker.process(offer.build(), None)

        result.nextCheck shouldBe None

        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verifyNoMoreInteractions(holocronValidator)
        verifyNoMoreInteractions(holocronSender)
        verifyNoMoreInteractions(brokerClient)
        result.updateOfferFunc shouldBe None

        offer.getHolocronStatus.getLastSentChangeVersion shouldBe 15
        offer.getHolocronStatus.hasLastSentOfferStatus shouldBe false
      }

      "holocron hash is the same" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        val holocronHash: String = HolocronUtils.getHash(holoOffer)
        offer.getHolocronStatusBuilder
          .setLastSentChangeVersion(15)
          .setLastSentHolocronHash(holocronHash)
          .addLastEventsBuilder()
          .setChangeVersion(15)
          .setHolocronHash(holocronHash)
          .setWasSent(true)
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        val result = worker.process(offer.build(), None)

        result.nextCheck shouldBe None
        result.updateOfferFunc shouldBe None

        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verifyNoMoreInteractions(holocronValidator)
        verifyNoMoreInteractions(holocronSender)
        verifyNoMoreInteractions(brokerClient)

        offer.getHolocronStatus.getLastSentChangeVersion shouldBe 15
        offer.getHolocronStatus.hasLastSentOfferStatus shouldBe false
      }

      "holo offer is the same except changeVersion" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer1: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        val holoOffer2: HoloOffer = holoOffer1.toBuilder.setChangeVersion(16).build()
        val holocronHash: String = HolocronUtils.getHash(holoOffer1)
        offer.getHolocronStatusBuilder
          .setLastSentChangeVersion(15)
          .setLastSentHolocronHash(holocronHash)
          .addLastEventsBuilder()
          .setChangeVersion(15)
          .setHolocronHash(holocronHash)
          .setWasSent(true)
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer2)
        val result = worker.process(offer.build(), None)

        result.nextCheck shouldBe None

        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verifyNoMoreInteractions(holocronValidator)
        verifyNoMoreInteractions(holocronSender)
        verifyNoMoreInteractions(brokerClient)
        result.updateOfferFunc shouldBe None

        offer.getHolocronStatus.getLastSentChangeVersion shouldBe 15
      }
    }

    "update simple holocron status" when {
      "simple holocron converters in use" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        when(holocronConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Invalid(Seq(MissingRequiredField("field"))))
        val result = simpleWorker.process(offer.build(), None)
        verify(holocronConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
        verifyNoMoreInteractions(holocronSender)
        verifyNoMoreInteractions(brokerClient)

        val newOffer = result.updateOfferFunc.get(offer.build())

        newOffer.hasHolocronStatus shouldBe false
        result.nextCheck shouldBe None

        val holoStatus: OfferModel.HolocronStatus = newOffer.getSimpleHolocronStatus
        holoStatus.hasLastSentOfferStatus shouldBe false
        holoStatus.hasLastSentChangeVersion shouldBe false
        holoStatus.hasLastSentMoment shouldBe false
        holoStatus.hasLastSentHoloTimestamp shouldBe false
        holoStatus.getLastEventsCount shouldBe 1
        val event: OfferModel.HolocronEvent = holoStatus.getLastEvents(0)
        event.getOfferStatus shouldBe CompositeStatus.CS_ACTIVE
        event.getChangeVersion shouldBe 1
        event.getAction shouldBe Action.ACTIVATE
        event.getMoment.getSeconds shouldBe moment.getMillis / 1000
        event.getHoloTimestamp.getSeconds shouldBe moment.minusHours(1).getMillis / 1000
        event.getWasSent shouldBe false
        event.getSendError shouldBe ""
        event.getValidationError shouldBe "MissingRequiredField(field)"
        event.getFeatureGeneration shouldBe 2
      }
    }

    "validate" when {
      "converter returned Some and need to send" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Invalid(Seq(MissingRequiredField("field"))))
        val result = worker.process(offer.build(), None)

        result.nextCheck shouldBe None

        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
      }
    }

    "try to send" when {
      "has last sent status but all events are wasSent=false" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        val hash: String = HolocronUtils.getHash(holoOffer)
        offer.getHolocronStatusBuilder
          .setLastSentChangeVersion(2)
          .setLastSentHolocronHash(hash)
          .setLastSentOfferStatus(CompositeStatus.CS_ACTIVE)
          .setLastSentHoloTimestamp(Timestamps.fromSeconds(2))
          .addLastEvents(
            HolocronEvent
              .newBuilder()
              .setChangeVersion(1)
              .setHolocronHash(hash)
              .setOfferStatus(CompositeStatus.CS_ACTIVE)
              .setHoloTimestamp(Timestamps.fromSeconds(1))
          )
          .addLastEvents(
            HolocronEvent
              .newBuilder()
              .setChangeVersion(2)
              .setHolocronHash(hash)
              .setOfferStatus(CompositeStatus.CS_ACTIVE)
              .setHoloTimestamp(Timestamps.fromSeconds(2))
          )
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Invalid(Seq(MissingRequiredField("field"))))
        val result = worker.process(offer.build(), None)

        result.nextCheck shouldBe None

        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
      }

      "resend DEACTIVATE event" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        val holocronHash: String = HolocronUtils.getHash(holoOffer)
        offer.getHolocronStatusBuilder
          .setLastSentHolocronHash(holocronHash)
          .setLastSentOfferStatus(CompositeStatus.CS_INACTIVE)
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Invalid(Seq(MissingRequiredField("field"))))
        val result = worker.process(offer.build(), None)

        result.nextCheck shouldBe None

        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
      }

      "no holocron status exist" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Invalid(Seq(MissingRequiredField("field"))))
        val result = worker.process(offer.build(), None)

        result.nextCheck shouldBe None

        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
      }

      "hash changed" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        val holocronHash: String = HolocronUtils.getHash(holoOffer)
        offer.getHolocronStatusBuilder.setLastSentHolocronHash(holocronHash + "old")
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Invalid(Seq(MissingRequiredField("field"))))
        val result = worker.process(offer.build(), None)

        result.nextCheck shouldBe None

        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
      }

      "feature generation changed" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        val hash: String = HolocronUtils.getHash(holoOffer)
        offer.getHolocronStatusBuilder.addLastEventsBuilder().setHolocronHash(hash).setFeatureGeneration(1)
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Invalid(Seq(MissingRequiredField("field"))))
        val result = worker.process(offer.build(), None)

        result.nextCheck shouldBe None

        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
      }

      "last event has send error" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        val hash: String = HolocronUtils.getHash(holoOffer)
        offer.getHolocronStatusBuilder
          .addLastEventsBuilder()
          .setHolocronHash(hash)
          .setFeatureGeneration(2)
          .setSendError("error")
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Invalid(Seq(MissingRequiredField("field"))))
        val result = worker.process(offer.build(), None)

        result.nextCheck shouldBe None

        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
      }

      "last event hash changed" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        val hash: String = HolocronUtils.getHash(holoOffer)
        offer.getHolocronStatusBuilder
          .addLastEventsBuilder()
          .setHolocronHash(hash + "old")
          .setFeatureGeneration(2)
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Invalid(Seq(MissingRequiredField("field"))))
        val result = worker.process(offer.build(), None)

        result.nextCheck shouldBe None

        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
      }

      "last events corrupted" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        offer.addFlag(OfferFlag.OF_INACTIVE)
        offer.getHolocronStatusBuilder
          .setLastSentChangeVersion(2)
          .setLastSentHoloTimestamp(Timestamps.fromSeconds(1))
          .addLastEvents(
            HolocronEvent.newBuilder().setWasSent(true).setChangeVersion(1).setHoloTimestamp(Timestamps.fromSeconds(2))
          )
          .addLastEvents(
            HolocronEvent.newBuilder().setWasSent(true).setChangeVersion(2).setHoloTimestamp(Timestamps.fromSeconds(1))
          )
        when(holocronValidator.validate(?)).thenReturn(Valid)
        when(holocronSender.send(?)).thenReturn(Success(()))
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(
          offer.getOfferID,
          action = Action.DEACTIVATE,
          holoTimestamp = moment,
          changeVersion = offer.getHolocronStatus.getLastSentChangeVersion + 1
        )
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        val result = worker.process(offer.build(), None)
        val newOffer = result.updateOfferFunc.get(offer.build())

        newOffer.getHolocronStatus.getLastEventsCount shouldBe 3
        newOffer.getHolocronStatus.getLastEvents(2).getChangeVersion shouldBe 3
        newOffer.getHolocronStatus.getLastSentChangeVersion shouldBe 3
        newOffer.getHolocronStatus.getLastSentHoloTimestamp shouldBe Timestamps.fromMillis(moment.getMillis)
        verify(holocronSender).send(?)
        verifyNoMoreInteractions(brokerClient)
        val result2 = worker.process(newOffer, None)
        result2.updateOfferFunc shouldBe None

        verifyNoMoreInteractions(holocronSender)
      }

      "last events corrupted: no last events" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        offer.addFlag(OfferFlag.OF_INACTIVE)
        offer.getHolocronStatusBuilder
          .setLastSentChangeVersion(2)
          .setLastSentHoloTimestamp(Timestamps.fromSeconds(1))
        when(holocronValidator.validate(?)).thenReturn(Valid)
        when(holocronSender.send(?)).thenReturn(Success(()))
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(
          offer.getOfferID,
          action = Action.DEACTIVATE,
          holoTimestamp = moment,
          changeVersion = offer.getHolocronStatus.getLastSentChangeVersion + 1
        )
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        val result = worker.process(offer.build(), None)
        val newOffer = result.updateOfferFunc.get(offer.build())

        newOffer.getHolocronStatus.getLastEventsCount shouldBe 1
        newOffer.getHolocronStatus.getLastEvents(0).getChangeVersion shouldBe 3
        newOffer.getHolocronStatus.getLastSentChangeVersion shouldBe 3
        newOffer.getHolocronStatus.getLastSentHoloTimestamp shouldBe Timestamps.fromMillis(moment.getMillis)
        verify(holocronSender).send(?)
        verifyNoMoreInteractions(brokerClient)
        val result2 = worker.process(newOffer, None)
        verifyNoMoreInteractions(holocronSender)
        result2.updateOfferFunc shouldBe None

      }
    }

    "not send" when {
      "validator returned Valid but failed to send" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Valid)
        when(holocronSender.send(?)).thenReturn(Failure(new RuntimeException("error")))
        val result = worker.process(offer.build(), None)
        val newOffer = result.updateOfferFunc.get(offer.build())

        result.nextCheck.nonEmpty shouldBe true
        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
        verify(holocronSender).send(eeq(holoOffer))
        verifyNoMoreInteractions(brokerClient)
        newOffer.hasSimpleHolocronStatus shouldBe false
        val holoStatus: OfferModel.HolocronStatus = newOffer.getHolocronStatus
        holoStatus.hasLastSentOfferStatus shouldBe false
        holoStatus.hasLastSentChangeVersion shouldBe false
        holoStatus.hasLastSentMoment shouldBe false
        holoStatus.hasLastSentHoloTimestamp shouldBe false
        holoStatus.getLastEventsCount shouldBe 1
        val event: OfferModel.HolocronEvent = holoStatus.getLastEvents(0)
        event.getOfferStatus shouldBe CompositeStatus.CS_ACTIVE
        event.getChangeVersion shouldBe 1
        event.getAction shouldBe Action.ACTIVATE
        event.getMoment.getSeconds shouldBe moment.getMillis / 1000
        event.getHoloTimestamp.getSeconds shouldBe moment.minusHours(1).getMillis / 1000
        event.getWasSent shouldBe false
        event.getSendError shouldBe "error"
        event.getValidationError shouldBe ""
        event.getFeatureGeneration shouldBe 2
      }
      "validator returned Valid but failed to send with RecordTooLargeException" in new Fixture {
        val message = "record too large"
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Valid)
        when(holocronSender.send(?))
          .thenReturn(Failure(new ExecutionException(new RecordTooLargeException(message))))
        val result = worker.process(offer.build(), None)
        val newOffer = result.updateOfferFunc.get(offer.build())

        result.nextCheck shouldBe None

        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
        verify(holocronSender).send(eeq(holoOffer))
        verifyNoMoreInteractions(brokerClient)
        newOffer.hasSimpleHolocronStatus shouldBe false
        val holoStatus: OfferModel.HolocronStatus = newOffer.getHolocronStatus
        holoStatus.hasLastSentOfferStatus shouldBe false
        holoStatus.hasLastSentChangeVersion shouldBe false
        holoStatus.hasLastSentMoment shouldBe false
        holoStatus.hasLastSentHoloTimestamp shouldBe false
        holoStatus.getLastEventsCount shouldBe 1
        val event: OfferModel.HolocronEvent = holoStatus.getLastEvents(0)
        event.getOfferStatus shouldBe CompositeStatus.CS_ACTIVE
        event.getChangeVersion shouldBe 1
        event.getAction shouldBe Action.ACTIVATE
        event.getMoment.getSeconds shouldBe moment.getMillis / 1000
        event.getHoloTimestamp.getSeconds shouldBe moment.minusHours(1).getMillis / 1000
        event.getWasSent shouldBe false
        event.getSendError shouldBe ""
        event.getValidationError shouldBe "org.apache.kafka.common.errors.RecordTooLargeException: record too large"
        event.getFeatureGeneration shouldBe 2
      }
      "validator returned Invalid" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Invalid(Seq(MissingRequiredField("field"))))
        val result = worker.process(offer.build(), None)
        val newOffer = result.updateOfferFunc.get(offer.build())

        result.nextCheck shouldBe None

        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
        verifyNoMoreInteractions(holocronSender)
        verifyNoMoreInteractions(brokerClient)
        newOffer.hasSimpleHolocronStatus shouldBe false
        val holoStatus: OfferModel.HolocronStatus = newOffer.getHolocronStatus
        holoStatus.hasLastSentOfferStatus shouldBe false
        holoStatus.hasLastSentChangeVersion shouldBe false
        holoStatus.hasLastSentMoment shouldBe false
        holoStatus.hasLastSentHoloTimestamp shouldBe false
        holoStatus.getLastEventsCount shouldBe 1
        val event: OfferModel.HolocronEvent = holoStatus.getLastEvents(0)
        event.getOfferStatus shouldBe CompositeStatus.CS_ACTIVE
        event.getChangeVersion shouldBe 1
        event.getAction shouldBe Action.ACTIVATE
        event.getMoment.getSeconds shouldBe moment.getMillis / 1000
        event.getHoloTimestamp.getSeconds shouldBe moment.minusHours(1).getMillis / 1000
        event.getWasSent shouldBe false
        event.getSendError shouldBe ""
        event.getValidationError shouldBe "MissingRequiredField(field)"
        event.getFeatureGeneration shouldBe 2
      }
      "validator returned Invalid, action is DEACTIVATE, failed to send" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        offer.addFlag(OfferFlag.OF_INACTIVE)
        offer.getHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_INACTIVE)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID, action = Action.DEACTIVATE)
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Invalid(Seq(MissingRequiredField("field"))))
        when(holocronSender.send(?)).thenReturn(Failure(new RuntimeException("error")))
        val result = worker.process(offer.build(), None)
        val newOffer = result.updateOfferFunc.get(offer.build())

        result.nextCheck.nonEmpty shouldBe true
        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
        verify(holocronSender).send(eeq(holoOffer))
        verifyNoMoreInteractions(brokerClient)
        newOffer.hasSimpleHolocronStatus shouldBe false
        val holoStatus: OfferModel.HolocronStatus = newOffer.getHolocronStatus
        holoStatus.hasLastSentOfferStatus shouldBe true
        holoStatus.hasLastSentChangeVersion shouldBe false
        holoStatus.hasLastSentMoment shouldBe false
        holoStatus.hasLastSentHoloTimestamp shouldBe false
        holoStatus.getLastEventsCount shouldBe 1
        val event: OfferModel.HolocronEvent = holoStatus.getLastEvents(0)
        event.getOfferStatus shouldBe CompositeStatus.CS_INACTIVE
        event.getChangeVersion shouldBe 1
        event.getAction shouldBe Action.DEACTIVATE
        event.getMoment.getSeconds shouldBe moment.getMillis / 1000
        event.getHoloTimestamp.getSeconds shouldBe moment.minusHours(1).getMillis / 1000
        event.getWasSent shouldBe false
        event.getSendError shouldBe "error"
        event.getValidationError shouldBe "MissingRequiredField(field)"
        event.getFeatureGeneration shouldBe 2
      }
      "validator returned Invalid, action is DEACTIVATE, failed to send with RecordTooLargeException" in new Fixture {
        val message = "record too large"
        val offer: Offer.Builder = createOffer(moment.getMillis)
        offer.addFlag(OfferFlag.OF_INACTIVE)
        offer.getHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_INACTIVE)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID, action = Action.DEACTIVATE)
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Invalid(Seq(MissingRequiredField("field"))))
        when(holocronSender.send(?))
          .thenReturn(Failure(new ExecutionException(new RecordTooLargeException(message))))
        val result = worker.process(offer.build(), None)

        result.nextCheck shouldBe None

        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
        verify(holocronSender).send(eeq(holoOffer))
        verifyNoMoreInteractions(brokerClient)
        val newOffer = result.updateOfferFunc.get(offer.build())

        newOffer.hasSimpleHolocronStatus shouldBe false
        val holoStatus: OfferModel.HolocronStatus = newOffer.getHolocronStatus
        holoStatus.hasLastSentOfferStatus shouldBe true
        holoStatus.hasLastSentChangeVersion shouldBe false
        holoStatus.hasLastSentMoment shouldBe false
        holoStatus.hasLastSentHoloTimestamp shouldBe false
        holoStatus.getLastEventsCount shouldBe 1
        val event: OfferModel.HolocronEvent = holoStatus.getLastEvents(0)
        event.getOfferStatus shouldBe CompositeStatus.CS_INACTIVE
        event.getChangeVersion shouldBe 1
        event.getAction shouldBe Action.DEACTIVATE
        event.getMoment.getSeconds shouldBe moment.getMillis / 1000
        event.getHoloTimestamp.getSeconds shouldBe moment.minusHours(1).getMillis / 1000
        event.getWasSent shouldBe false
        event.getSendError shouldBe ""
        event.getValidationError shouldBe "org.apache.kafka.common.errors.RecordTooLargeException: record too large"
        event.getFeatureGeneration shouldBe 2
      }
    }

    "send to broker" when {
      "old valid event, DelaySendOldToHolocron feature is disabled, and sending to broker is enabled" in new Fixture {
        featuresRegistry.updateFeature(
          featuresManager.SendHolocronToBroker.name,
          featuresManager.SendHolocronToBroker.value.copy(value = true)
        )
        DateTimeUtils.setCurrentMillisFixed(System.currentTimeMillis())
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID, holoTimestamp = moment.minusDays(31))
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Valid)
        when(holocronSender.send(?)).thenReturn(Success(()))
        when(brokerClient.send(?)(?)).thenReturn(Future.unit)
        val result = worker.process(offer.build(), None)

        result.nextCheck shouldBe None
        val newOffer = result.updateOfferFunc.get(offer.build())

        newOffer.hasHolocronStatus shouldBe true
        newOffer.hasSimpleHolocronStatus shouldBe false
        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
        verify(holocronSender).send(eeq(holoOffer))
        verify(brokerClient).send(eeq(HoloFullCarOfferConverter.convert(holoOffer, offer.build())))(?)
        DateTimeUtils.setCurrentMillisSystem()
        featuresRegistry.updateFeature(
          featuresManager.SendHolocronToBroker.name,
          featuresManager.SendHolocronToBroker.value.copy(value = false)
        )
      }
    }

    "send" when {
      "old valid event and DelaySendOldToHolocron feature is disabled" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID, holoTimestamp = moment.minusDays(31))
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Valid)
        when(holocronSender.send(?)).thenReturn(Success(()))
        val result = worker.process(offer.build(), None)

        result.nextCheck shouldBe None
        val newOffer = result.updateOfferFunc.get(offer.build())

        newOffer.hasHolocronStatus shouldBe true
        newOffer.hasSimpleHolocronStatus shouldBe false
        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
        verify(holocronSender).send(eeq(holoOffer))
        verifyNoMoreInteractions(brokerClient)
      }

      "old invalid event and DelaySendOldToHolocron feature is disabled" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer =
          randomCarsFullHoloOffer(offer.getOfferID, action = Action.DEACTIVATE, holoTimestamp = moment.minusDays(31))
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Invalid(Seq(MissingRequiredField("field"))))
        when(holocronSender.send(?)).thenReturn(Success(()))
        val result = worker.process(offer.build(), None)

        result.nextCheck shouldBe None
        val newOffer = result.updateOfferFunc.get(offer.build())

        newOffer.hasHolocronStatus shouldBe true
        newOffer.hasSimpleHolocronStatus shouldBe false
        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
        verify(holocronSender).send(eeq(holoOffer))
        verifyNoMoreInteractions(brokerClient)
      }

      "new valid event and DelaySendOldToHolocron feature is enabled" in new Fixture {
        featuresRegistry.updateFeature(
          featuresManager.DelaySendOldToHolocron.name,
          featuresManager.DelaySendOldToHolocron.value.copy(value = true)
        )
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID, holoTimestamp = moment.minusDays(29))
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Valid)
        when(holocronSender.send(?)).thenReturn(Success(()))
        val result = worker.process(offer.build(), None)

        result.nextCheck shouldBe None
        val newOffer = result.updateOfferFunc.get(offer.build())

        newOffer.hasHolocronStatus shouldBe true
        newOffer.hasSimpleHolocronStatus shouldBe false
        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
        verify(holocronSender).send(eeq(holoOffer))
        verifyNoMoreInteractions(brokerClient)
        featuresRegistry.updateFeature(
          featuresManager.DelaySendOldToHolocron.name,
          featuresManager.DelaySendOldToHolocron.value.copy(value = false)
        )
      }

      "new invalid event and DelaySendOldToHolocron feature is enabled" in new Fixture {
        featuresRegistry.updateFeature(
          featuresManager.DelaySendOldToHolocron.name,
          featuresManager.DelaySendOldToHolocron.value.copy(value = true)
        )
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer =
          randomCarsFullHoloOffer(offer.getOfferID, action = Action.DEACTIVATE, holoTimestamp = moment.minusDays(29))
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Invalid(Seq(MissingRequiredField("field"))))
        when(holocronSender.send(?)).thenReturn(Success(()))
        val result = worker.process(offer.build(), None)

        result.nextCheck shouldBe None
        val newOffer = result.updateOfferFunc.get(offer.build())

        newOffer.hasHolocronStatus shouldBe true
        newOffer.hasSimpleHolocronStatus shouldBe false
        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
        verify(holocronSender).send(eeq(holoOffer))
        verifyNoMoreInteractions(brokerClient)
        featuresRegistry.updateFeature(
          featuresManager.DelaySendOldToHolocron.name,
          featuresManager.DelaySendOldToHolocron.value.copy(value = false)
        )
      }

      "validator returned Valid" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Valid)
        when(holocronSender.send(?)).thenReturn(Success(()))
        val result = worker.process(offer.build(), None)

        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
        verify(holocronSender).send(eeq(holoOffer))
        verifyNoMoreInteractions(brokerClient)
        val newOffer = result.updateOfferFunc.get(offer.build())

        newOffer.hasSimpleHolocronStatus shouldBe false
        val holoStatus: OfferModel.HolocronStatus = newOffer.getHolocronStatus
        holoStatus.getLastSentOfferStatus shouldBe CompositeStatus.CS_ACTIVE
        holoStatus.getLastSentChangeVersion shouldBe 1
        holoStatus.getLastSentMoment shouldBe Timestamps.fromMillis(moment.getMillis)
        holoStatus.getLastSentHoloTimestamp shouldBe Timestamps.fromMillis(moment.minusHours(1).getMillis)
        holoStatus.getLastEventsCount shouldBe 1
        val event: OfferModel.HolocronEvent = holoStatus.getLastEvents(0)
        event.getOfferStatus shouldBe CompositeStatus.CS_ACTIVE
        event.getChangeVersion shouldBe 1
        event.getAction shouldBe Action.ACTIVATE
        event.getMoment.getSeconds shouldBe moment.getMillis / 1000
        event.getHoloTimestamp.getSeconds shouldBe moment.minusHours(1).getMillis / 1000
        event.getWasSent shouldBe true
        event.getSendError shouldBe ""
        event.getValidationError shouldBe ""
        event.getFeatureGeneration shouldBe 2
      }
      "validator returned Invalid but action is DEACTIVATE" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        offer.addFlag(OfferFlag.OF_INACTIVE)
        offer.getHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_INACTIVE)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID, action = Action.DEACTIVATE)
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Invalid(Seq(MissingRequiredField("field"))))
        when(holocronSender.send(?)).thenReturn(Success(()))
        val result = worker.process(offer.build(), None)

        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
        verify(holocronSender).send(eeq(holoOffer))
        verifyNoMoreInteractions(brokerClient)
        val newOffer = result.updateOfferFunc.get(offer.build())

        newOffer.hasSimpleHolocronStatus shouldBe false
        val holoStatus: OfferModel.HolocronStatus = newOffer.getHolocronStatus
        holoStatus.getLastSentOfferStatus shouldBe CompositeStatus.CS_INACTIVE
        holoStatus.getLastSentChangeVersion shouldBe 1
        holoStatus.getLastSentMoment shouldBe Timestamps.fromMillis(moment.getMillis)
        holoStatus.getLastSentHoloTimestamp shouldBe Timestamps.fromMillis(moment.minusHours(1).getMillis)
        holoStatus.getLastEventsCount shouldBe 1
        val event: OfferModel.HolocronEvent = holoStatus.getLastEvents(0)
        event.getOfferStatus shouldBe CompositeStatus.CS_INACTIVE
        event.getChangeVersion shouldBe 1
        event.getAction shouldBe Action.DEACTIVATE
        event.getMoment.getSeconds shouldBe moment.getMillis / 1000
        event.getHoloTimestamp.getSeconds shouldBe moment.minusHours(1).getMillis / 1000
        event.getWasSent shouldBe true
        event.getSendError shouldBe ""
        event.getValidationError shouldBe "MissingRequiredField(field)"
        event.getFeatureGeneration shouldBe 2
      }

      "validator returned Invalid but strict validation is disabled" in new Fixture {
        featuresRegistry.updateFeature(featuresManager.StrictHolocronValidation.name, false)
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Invalid(Seq(MissingRequiredField("field"))))
        when(holocronSender.send(?)).thenReturn(Success(()))
        val result = worker.process(offer.build(), None)
        result.nextCheck shouldBe None

        verify(holocronExtendedConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
        verify(holocronSender).send(eeq(holoOffer))
        verifyNoMoreInteractions(brokerClient)
        val newOffer = result.updateOfferFunc.get(offer.build())

        newOffer.hasSimpleHolocronStatus shouldBe false
        val holoStatus: OfferModel.HolocronStatus = newOffer.getHolocronStatus
        holoStatus.getLastSentOfferStatus shouldBe CompositeStatus.CS_ACTIVE
        holoStatus.getLastSentChangeVersion shouldBe 1
        holoStatus.getLastSentMoment shouldBe Timestamps.fromMillis(moment.getMillis)
        holoStatus.getLastSentHoloTimestamp shouldBe Timestamps.fromMillis(moment.minusHours(1).getMillis)
        holoStatus.getLastEventsCount shouldBe 1
        val event: OfferModel.HolocronEvent = holoStatus.getLastEvents(0)
        event.getOfferStatus shouldBe CompositeStatus.CS_ACTIVE
        event.getChangeVersion shouldBe 1
        event.getAction shouldBe Action.ACTIVATE
        event.getMoment.getSeconds shouldBe moment.getMillis / 1000
        event.getHoloTimestamp.getSeconds shouldBe moment.minusHours(1).getMillis / 1000
        event.getWasSent shouldBe true
        event.getSendError shouldBe ""
        event.getValidationError shouldBe "MissingRequiredField(field)"
        event.getFeatureGeneration shouldBe 2
        featuresRegistry.updateFeature(featuresManager.StrictHolocronValidation.name, true)
      }
    }

    "keep only maxLastEventsSize last events" in new Fixture {
      var offer: Offer.Builder = createOffer(moment.getMillis)
      when(holocronValidator.validate(?)).thenReturn(Valid)
      when(holocronSender.send(?)).thenReturn(Success(()))
      for (_ <- 1 to 5) {
        val holoOffer: HoloOffer =
          randomCarsFullHoloOffer(
            offer.getOfferID,
            changeVersion = offer.getHolocronStatus.getLastSentChangeVersion + 1,
            holoTimestamp =
              new DateTime(offer.getHolocronStatus.getLastSentHoloTimestamp.getSeconds * 1000).plusSeconds(1),
            vin = "vin" + (offer.getHolocronStatus.getLastSentChangeVersion + 1)
          )
        when(holocronExtendedConverter.convert(?, ?)).thenReturn(holoOffer)
        val result = worker.process(offer.build(), None)
        val newOffer = result.updateOfferFunc.get(offer.build())

        offer = newOffer.toBuilder
      }
      offer.getHolocronStatus.getLastEventsCount shouldBe 3
      offer.getHolocronStatus.getLastEvents(0).getChangeVersion shouldBe 3
      offer.getHolocronStatus.getLastEvents(1).getChangeVersion shouldBe 4
      offer.getHolocronStatus.getLastEvents(2).getChangeVersion shouldBe 5
    }

    "not try to send simple" when {
      "extended holocron last event hash is the same and simple not exist" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsHoloOffer(offer.getOfferID)
        val hash: String = HolocronUtils.getHash(holoOffer)
        offer.getHolocronStatusBuilder
          .addLastEventsBuilder()
          .setHolocronHash(hash)
          .setFeatureGeneration(2)
        when(holocronConverter.convert(?, ?)).thenReturn(holoOffer)
        val result = simpleWorker.process(offer.build(), None)
        result.nextCheck shouldBe None

        verify(holocronConverter).convert(eeq(offer.build()), eeq(moment))
        verifyNoMoreInteractions(holocronValidator)
        verifyNoMoreInteractions(holocronSender)
        verifyNoMoreInteractions(brokerClient)
      }

      "extended holocron hash is the same and simple not exist" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        offer.getHolocronStatusBuilder.setLastSentChangeVersion(15)
        val holoOffer: HoloOffer = randomCarsHoloOffer(offer.getOfferID)
        val holocronHash: String = HolocronUtils.getHash(holoOffer)
        offer.getHolocronStatusBuilder.setLastSentHolocronHash(holocronHash)
        when(holocronConverter.convert(?, ?)).thenReturn(holoOffer)
        val result = simpleWorker.process(offer.build(), None)

        result.nextCheck shouldBe None
        result.updateOfferFunc shouldBe None

        verify(holocronConverter).convert(eeq(offer.build()), eeq(moment))
        verifyNoMoreInteractions(holocronValidator)
        verifyNoMoreInteractions(holocronSender)
        verifyNoMoreInteractions(brokerClient)
        offer.getHolocronStatus.hasLastSentOfferStatus shouldBe false
      }

      "simple holocron last event hash is the same" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsHoloOffer(offer.getOfferID)
        val hash: String = HolocronUtils.getHash(holoOffer)
        offer.getSimpleHolocronStatusBuilder
          .setLastSentHolocronHash(hash)
          .addLastEventsBuilder()
          .setHolocronHash(hash)
          .setWasSent(true)
          .setFeatureGeneration(2)
        when(holocronConverter.convert(?, ?)).thenReturn(holoOffer)
        val result = simpleWorker.process(offer.build(), None)
        result.nextCheck shouldBe None

        verify(holocronConverter).convert(eeq(offer.build()), eeq(moment))
        verifyNoMoreInteractions(holocronValidator)
        verifyNoMoreInteractions(holocronSender)
        verifyNoMoreInteractions(brokerClient)
      }

      "simple holocron hash is the same" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsHoloOffer(offer.getOfferID)
        val holocronHash: String = HolocronUtils.getHash(holoOffer)
        offer.getSimpleHolocronStatusBuilder
          .setLastSentHolocronHash(holocronHash)
          .setLastSentChangeVersion(15)
          .addLastEventsBuilder()
          .setHolocronHash(holocronHash)
          .setChangeVersion(15)
          .setWasSent(true)
        when(holocronConverter.convert(?, ?)).thenReturn(holoOffer)
        val result = simpleWorker.process(offer.build(), None)

        result.nextCheck shouldBe None
        result.updateOfferFunc shouldBe None

        verify(holocronConverter).convert(eeq(offer.build()), eeq(moment))
        verifyNoMoreInteractions(holocronValidator)
        verifyNoMoreInteractions(holocronSender)
        verifyNoMoreInteractions(brokerClient)

        offer.getSimpleHolocronStatus.getLastSentChangeVersion shouldBe 15
        offer.getSimpleHolocronStatus.hasLastSentOfferStatus shouldBe false
      }
    }

    "try to send simple" when {
      "no holocron status exist" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsHoloOffer(offer.getOfferID)
        when(holocronConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Invalid(Seq(MissingRequiredField("field"))))
        val result = simpleWorker.process(offer.build(), None)
        result.nextCheck shouldBe None

        verify(holocronConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
      }

      "hash changed" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsHoloOffer(offer.getOfferID)
        val holocronHash: String = HolocronUtils.getHash(holoOffer)
        offer.getSimpleHolocronStatusBuilder.setLastSentHolocronHash(holocronHash + "old")
        when(holocronConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Invalid(Seq(MissingRequiredField("field"))))
        val result = simpleWorker.process(offer.build(), None)
        result.nextCheck shouldBe None

        verify(holocronConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
      }

      "last event hash changed" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsHoloOffer(offer.getOfferID)
        val hash: String = HolocronUtils.getHash(holoOffer)
        offer.getSimpleHolocronStatusBuilder
          .addLastEventsBuilder()
          .setHolocronHash(hash + "old")
          .setFeatureGeneration(2)
        when(holocronConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Invalid(Seq(MissingRequiredField("field"))))
        val result = simpleWorker.process(offer.build(), None)
        result.nextCheck shouldBe None

        verify(holocronConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
      }

      "extended hash changed and simple not exist" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsHoloOffer(offer.getOfferID)
        val holocronHash: String = HolocronUtils.getHash(holoOffer)
        offer.getHolocronStatusBuilder.setLastSentHolocronHash(holocronHash + "old")
        when(holocronConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Invalid(Seq(MissingRequiredField("field"))))
        val result = simpleWorker.process(offer.build(), None)
        result.nextCheck shouldBe None

        verify(holocronConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
      }

      "extended last event hash changed and simple not exist" in new Fixture {
        val offer: Offer.Builder = createOffer(moment.getMillis)
        val holoOffer: HoloOffer = randomCarsFullHoloOffer(offer.getOfferID)
        val hash: String = HolocronUtils.getHash(holoOffer)
        offer.getHolocronStatusBuilder
          .addLastEventsBuilder()
          .setHolocronHash(hash + "old")
          .setFeatureGeneration(2)
        when(holocronConverter.convert(?, ?)).thenReturn(holoOffer)
        when(holocronValidator.validate(?)).thenReturn(Invalid(Seq(MissingRequiredField("field"))))
        val result = simpleWorker.process(offer.build(), None)
        result.nextCheck shouldBe None

        verify(holocronConverter).convert(eeq(offer.build()), eeq(moment))
        verify(holocronValidator).validate(eeq(holoOffer))
      }
    }
  }

  private def randomCarsFullHoloOffer(id: OfferID,
                                      action: Action = Action.ACTIVATE,
                                      changeVersion: Int = 1,
                                      holoTimestamp: DateTime = moment.minusHours(1),
                                      vin: String = "vin") = {
    val b = HoloOffer.newBuilder()
    b.setAction(action)
    b.setChangeVersion(changeVersion)
    b.setTimestamp(Timestamps.fromMillis(holoTimestamp.getMillis))
    b.getVosCarBuilder.setId(id)
    b.getVosCarBuilder.setVin(vin)
    b.build()
  }

  private def randomCarsHoloOffer(id: OfferID,
                                  action: Action = Action.ACTIVATE,
                                  changeVersion: Int = 1,
                                  holoTimestamp: DateTime = moment.minusHours(1)) = {
    val b = HoloOffer.newBuilder()
    b.setAction(action)
    b.setChangeVersion(changeVersion)
    b.setTimestamp(Timestamps.fromMillis(holoTimestamp.getMillis))
    b.getCarBuilder.setId(id)
    b.build()
  }
}
