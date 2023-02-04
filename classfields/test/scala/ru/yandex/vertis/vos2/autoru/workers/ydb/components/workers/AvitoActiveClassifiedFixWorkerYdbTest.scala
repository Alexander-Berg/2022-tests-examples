package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.BasicsModel.CompositeStatus
import ru.yandex.vos2.OfferModel.Multiposting.Classified.ClassifiedName
import ru.yandex.vos2.autoru.model.AutoruModelUtils._
import ru.yandex.vos2.autoru.model.TestUtils.{createClassified, createMultiposting, createOffer}
import ru.yandex.vos2.autoru.services.cabinet.CabinetClient

import scala.util.Success
import org.junit.runner.RunWith
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AvitoActiveClassifiedFixWorkerYdbTest extends AnyWordSpec with MockitoSupport with Matchers {
  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {
    val cabinetClient: CabinetClient = mock[CabinetClient]

    val worker: AvitoActiveClassifiedFixWorker = new AvitoActiveClassifiedFixWorker(
      cabinetClient
    ) with YdbWorkerTestImpl
  }

  "Avito Classified actualize status" should {
    "process offer" in new Fixture {
      val classifiedBuilder = createClassified(ClassifiedName.AVITO)
      val multipostingBuilder = createMultiposting()
        .addClassifieds(classifiedBuilder)
      val offerBuilder = createOffer(dealer = true)
        .clearMultiposting()
        .setMultiposting(multipostingBuilder)

      when(cabinetClient.isMultipostingEnabled(?)(?)).thenReturn {
        Success(true)
      }

      assert(worker.shouldProcess(offerBuilder.build(), None).shouldProcess)
    }
    "not process offer" in new Fixture {
      val classifiedBuilder = createClassified(ClassifiedName.AVITO)
        .setId("1")
      val multipostingBuilder = createMultiposting()
        .addClassifieds(classifiedBuilder)
      val offerBuilder = createOffer()
        .setMultiposting(multipostingBuilder)

      assert(!worker.shouldProcess(offerBuilder.build(), None).shouldProcess)
    }
    "return processed offer" in new Fixture {
      val classifiedBuilder = createClassified(ClassifiedName.AVITO)
      val multipostingBuilder = createMultiposting()
        .addClassifieds(classifiedBuilder)
      val offerBuilder = createOffer()
        .setMultiposting(multipostingBuilder)
        .setUserRef("ac_123")

      when(cabinetClient.isMultipostingEnabled(?)(?)).thenReturn {
        Success(true)
      }

      val offer = offerBuilder.build()
      val processedOffer = worker.process(offer, None).updateOfferFunc.get(offer)
      val processedAvitoClassified = processedOffer.getMultiposting.findClassified(ClassifiedName.AVITO)

      processedAvitoClassified.map(_.getStatus) shouldEqual Some(CompositeStatus.CS_NEED_ACTIVATION)
    }
  }
}
