package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.joda.time.DateTime
import org.mockito.Mockito.{reset, verify}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.VinDecoderWorkerYdb.{getStateStr, NextUpdateData}
import ru.yandex.vos2.AutoruModel.AutoruOffer.VinDecoderCheck.VinDecoderResolution
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.services.vindecoder.VinDecoderClient

import scala.util.hashing.MurmurHash3
import scala.util.{Failure, Success}

class VinDecoderWorkerYdbTest extends AnyWordSpec with Matchers with MockitoSupport {

  implicit val traced: Traced = Traced.empty

  private val testVin = "abc-222"
  private val testVinHash = MurmurHash3.stringHash(testVin)
  private val testMark = "AUDI"
  private val testModel = "A4"

  abstract private class Fixture {

    val offer = {
      val offer = TestUtils.createOffer()
      offer.getOfferAutoruBuilder
        .setCategory(Category.CARS)
        .getDocumentsBuilder
        .setVin(testVin)
      offer.getOfferAutoruBuilder.getCarInfoBuilder
        .setMark(testMark)
        .setModel(testModel)
      offer
    }
    val client = mockStrict[VinDecoderClient]

    val worker = new VinDecoderWorkerYdb(
      client
    ) with YdbWorkerTestImpl
  }

  "ignore offer with category != CARS" in new Fixture {
    offer.getOfferAutoruBuilder
      .setCategory(Category.TRUCKS)
    assert(!worker.shouldProcess(offer.build(), None).shouldProcess)

  }

  "ignore offer without vin" in new Fixture {

    offer.getOfferAutoruBuilder.getDocumentsBuilder.clearVin()
    assert(!worker.shouldProcess(offer.build(), None).shouldProcess)

  }

  "skip offer if version and vin not changed since last check" in new Fixture {

    offer.getOfferAutoruBuilder.getVinDecoderCheckBuilder
      .setVersion(VinDecoderWorkerYdb.currentVersion)
      .setVinHash(testVinHash)

    val nextCheckData = NextUpdateData(Some(new DateTime().plusDays(1)), true)

    assert(!worker.shouldProcess(offer.build(), Some(getStateStr(nextCheckData))).shouldProcess)

  }

  "process offer if version was changed" in new Fixture {

    offer.getOfferAutoruBuilder.getVinDecoderCheckBuilder
      .setVersion(VinDecoderWorkerYdb.currentVersion - 1)
      .setVinHash(testVinHash)

    assert(worker.shouldProcess(offer.build(), None).shouldProcess)
  }

  "process offer if vin code was changed" in new Fixture {

    offer.getOfferAutoruBuilder.getVinDecoderCheckBuilder
      .setVersion(VinDecoderWorkerYdb.currentVersion)
      .setVinHash(testVinHash + 1)

    assert(worker.shouldProcess(offer.build(), None).shouldProcess)
  }

  "send mark, model and vin to vin-decoder on offer processing and save resolution" in new Fixture {
    VinDecoderResolution.values().toSeq.map { resolution =>
      reset(client)
      when(client.check(?, ?, ?)(?)).thenReturn(Success(resolution))
      when(client.getRawEssentialsReport(?)(?)).thenReturn(Success(None))

      val result = worker.process(offer.build(), None)
      val resultOffer = result.updateOfferFunc.get(offer.build()).getOfferAutoru

      result.nextCheck shouldBe None
      resultOffer.hasVinDecoderCheck shouldBe true
      resultOffer.getVinDecoderCheck.getCheckTime shouldBe System.currentTimeMillis() +- 1000
      resultOffer.getVinDecoderCheck.getVersion shouldBe VinDecoderWorkerYdb.currentVersion
      resultOffer.getVinDecoderCheck.getVinHash shouldBe testVinHash
      resultOffer.getVinDecoderCheck.getResolution shouldBe resolution

      verify(client).check(testVin, testMark, testModel)
    }
  }

  "reschedule offer if vin-decoder has failed" in new Fixture {
    when(client.check(?, ?, ?)(?)).thenReturn(Failure(new RuntimeException))
    when(client.getRawEssentialsReport(?)(?)).thenReturn(Success(None))

    val result = worker.process(offer.build(), None)

    result.nextCheck.nonEmpty shouldBe true
    result.updateOfferFunc.isEmpty shouldBe true
    verify(client).check(testVin, testMark, testModel)
  }
}
