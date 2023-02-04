package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import _root_.vertis.vasgen.RawDocument
import _root_.vertis.vasgen.grpc.{Result, WriteResponse}
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.ydb.skypper.YdbWrapper
import ru.yandex.vos2.BasicsModel.CompositeStatus
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag}
import ru.yandex.vos2._
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.model.TestUtils.createPhotoExt
import ru.yandex.vos2.autoru.services.idx.IdxConfigSelector
import ru.yandex.vos2.commonfeatures.FeaturesManager
import ru.yandex.vos2.model.ModelUtils.RichOfferBuilder
import ru.yandex.vos2.services.idx.sync.IdxClient

import scala.concurrent.duration._
import scala.util.Success

class IdxWorkerYdbTest extends AnyWordSpec with Matchers with InitTestDbs with BeforeAndAfter {
  implicit val traced: Traced = Traced.empty

  var now = DateTime.now()

  before {
    now = DateTime.now()
  }

  val idxConfigSelector = {
    val carClient: IdxClient[OfferID] = mock(classOf[IdxClient[OfferID]])
    when(carClient.submit(any(), any())).thenReturn(Success(ru.yandex.vos2.services.idx.sync.Ok))
    when(carClient.remove(any())).thenReturn(Success(ru.yandex.vos2.services.idx.sync.Ok))

    val truckClient: IdxClient[OfferID] = carClient
    val motoClient: IdxClient[OfferID] = carClient

    new IdxConfigSelector(
      components.idxRequestBuilder,
      components.trucksIdxRequestBuilder,
      components.motoIdxRequestBuilder,
      carClient,
      truckClient,
      motoClient,
      components.featuresManager
    )
  }

  abstract private class Fixture {

    import ru.yandex.vertis.baker.lifecycle.Application
    val mockedApplication = mock(classOf[Application])
    import ru.yandex.vertis.baker.env.{DefaultEnvProvider, Env}
    when(mockedApplication.env).thenReturn(new Env(DefaultEnvProvider))

    val worker = new IdxWorkerYdb(
      idxConfigSelector,
      (document: RawDocument) =>
        Success(
          WriteResponse
            .newBuilder()
            .addResults(Result.newBuilder().setStatus(Result.Status.ACCEPTED).setPk(document.getPk))
            .build()
        ),
      components.offerFormConverter
    ) with YdbWorkerTestImpl {
      override val ydb: YdbWrapper = components.skypper
      override def features: FeaturesManager = components.featuresManager
    }
  }

  "push recently revoked only if not banned and of cars category" in new Fixture {

    // без региона - не шлем
    checkShouldSend(shouldSend = false, worker = worker) { b =>
      b.getOfferAutoruBuilder.getSellerBuilder.getPlaceBuilder.clearGeobaseId()
    }

    // с регионом, активное - шлем
    checkShouldSend(shouldSend = true, worker = worker) { b => }

    // с регионом, не активное давно удаленное - не шлем
    checkShouldSend(shouldSend = false, worker = worker) { b =>
      b.getOfferAutoruBuilder.getRecallInfoBuilder.setRecallTimestamp(now.minusHours(25).getMillis)
      b.putFlag(OfferFlag.OF_DELETED)
    }

    // с регионом, не активное недавно удаленное, категория CARS - шлем
    checkShouldSend(shouldSend = true, newDelay = Some(1.day.minus(5.hours).toMillis), worker = worker) { b =>
      b.getOfferAutoruBuilder.getRecallInfoBuilder.setRecallTimestamp(now.minusHours(5).getMillis)
      b.getOfferAutoruBuilder.setCategory(Category.CARS)
      b.putFlag(OfferFlag.OF_DELETED)
    }

    // с регионом, не активное недавно удаленное, категория не CARS - не шлем
    checkShouldSend(shouldSend = false, worker = worker) { b =>
      b.getOfferAutoruBuilder.getRecallInfoBuilder.setRecallTimestamp(now.minusHours(5).getMillis)
      b.getOfferAutoruBuilder.setCategory(Category.TRUCKS)
      b.putFlag(OfferFlag.OF_DELETED)
    }

    // с регионом, не активное недавно удаленное, категория CARS, забаненное - не шлем
    checkShouldSend(shouldSend = false, worker = worker) { b =>
      b.getOfferAutoruBuilder.getRecallInfoBuilder.setRecallTimestamp(now.minusHours(5).getMillis)
      b.getOfferAutoruBuilder.setCategory(Category.CARS)
      b.putFlag(OfferFlag.OF_BANNED)
    }
  }

  "push recently banned" in new Fixture {
    // фича не включена - не шлем
    val nextDay5Hours = now.plusDays(1).withHourOfDay(5).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0)
    components.featureRegistry.updateFeature(components.featuresManager.SendBannedOffers.name, false)

    checkShouldSend(shouldSend = false, worker = worker) { b =>
      b.putFlag(OfferFlag.OF_BANNED)
      b.addStatusHistoryBuilder()
        .setComment("test")
        .setOfferStatus(CompositeStatus.CS_BANNED)
        .setTimestamp(now.getMillis)
    }
    // фича включена - шлем
    components.featureRegistry.updateFeature(components.featuresManager.SendBannedOffers.name, true)

    // забаненные объявления держим до пяти утра следующего дня, потом удаляем
    checkShouldSend(shouldSend = true, newDelay = Some(nextDay5Hours.getMillis - now.getMillis), worker = worker) { b =>
      b.putFlag(OfferFlag.OF_BANNED)
      b.addStatusHistoryBuilder()
        .setComment("test")
        .setOfferStatus(CompositeStatus.CS_BANNED)
        .setTimestamp(now.getMillis)
    }

    // забаненное больше суток назад - не шлем
    checkShouldSend(shouldSend = false, worker = worker) { b =>
      b.putFlag(OfferFlag.OF_BANNED)
      b.addStatusHistoryBuilder()
        .setComment("test")
        .setOfferStatus(CompositeStatus.CS_BANNED)
        .setTimestamp(now.minusDays(2).withHourOfDay(4).getMillis)
    }

    // забаненное неизвестно когда - не шлем
    checkShouldSend(shouldSend = false, worker = worker) { b =>
      b.putFlag(OfferFlag.OF_BANNED)
    }
    components.featureRegistry.updateFeature(components.featuresManager.SendBannedOffers.name, false)
  }

  "drafts are not pushing or removing from indexers" in new Fixture {
    val b = TestUtils.createOffer()
    b.addFlag(OfferFlag.OF_DRAFT)
    assert(!worker.shouldProcess(b.build(), None).shouldProcess)
  }

  "not push non-blurred photos if feature is enabled" in new Fixture {
    components.featureRegistry.updateFeature(components.featuresManager.BlurLicenseNumberRequired.name, false)

    checkShouldSend(shouldSend = true, worker = worker) { b =>
      b.getOfferAutoruBuilder
        .addPhoto(createPhotoExt("0", "0", blur = false))
    }

    components.featureRegistry.updateFeature(components.featuresManager.BlurLicenseNumberRequired.name, true)

    checkShouldSend(shouldSend = false, worker = worker) { b =>
      b.getOfferAutoruBuilder
        .addPhoto(createPhotoExt("0", "0", blur = false))
    }
  }

  "push regardless of feature if all photos are blurred" in new Fixture {
    components.featureRegistry.updateFeature(components.featuresManager.BlurLicenseNumberRequired.name, false)

    checkShouldSend(shouldSend = true, worker = worker) { b =>
      b.getOfferAutoruBuilder
        .addPhoto(createPhotoExt("0", "0", blur = true))
    }

    components.featureRegistry.updateFeature(components.featuresManager.BlurLicenseNumberRequired.name, true)

    checkShouldSend(shouldSend = true, worker = worker) { b =>
      b.getOfferAutoruBuilder
        .addPhoto(createPhotoExt("0", "0", blur = true))
    }
  }

  "not push non-blurred photos for call-center regardless of feature" in new Fixture {
    components.featureRegistry.updateFeature(components.featuresManager.BlurLicenseNumberRequired.name, false)

    checkShouldSend(shouldSend = false, worker = worker) { b =>
      b.getOfferAutoruBuilder.getSourceInfoBuilder.setIsCallcenter(true)
      b.getOfferAutoruBuilder
        .addPhoto(createPhotoExt("0", "0", blur = false))
    }

    components.featureRegistry.updateFeature(components.featuresManager.BlurLicenseNumberRequired.name, true)

    checkShouldSend(shouldSend = false, worker = worker) { b =>
      b.getOfferAutoruBuilder.getSourceInfoBuilder.setIsCallcenter(true)
      b.getOfferAutoruBuilder
        .addPhoto(createPhotoExt("0", "0", blur = false))
    }
  }

  "push dealer offers without blur regardless of feature" in new Fixture {
    components.featureRegistry.updateFeature(components.featuresManager.BlurLicenseNumberRequired.name, false)

    checkShouldSend(shouldSend = true, dealer = true, worker = worker) { b =>
      b.getOfferAutoruBuilder
        .addPhoto(createPhotoExt("0", "0", blur = false))
    }

    components.featureRegistry.updateFeature(components.featuresManager.BlurLicenseNumberRequired.name, true)

    checkShouldSend(shouldSend = true, dealer = true, worker = worker) { b =>
      b.getOfferAutoruBuilder
        .addPhoto(createPhotoExt("0", "0", blur = false))
    }
  }

  "push multiposting offer" in new Fixture {

    checkShouldSend(shouldSend = true, dealer = true, worker = worker) { b =>
      b.clearFlag()
        .addFlag(OfferFlag.OF_EXPIRED)
        .setMultiposting(OfferModel.Multiposting.newBuilder().setStatus(CompositeStatus.CS_ACTIVE))
    }
  }

  def checkShouldSend(
      shouldSend: Boolean,
      newDelay: Option[Long] = None,
      dealer: Boolean = false,
      worker: IdxWorkerYdb
  )(func: OfferModel.Offer.Builder => Any): Unit = {
    val offerBuilder = TestUtils.createOffer(dealer = dealer)
    offerBuilder.getOfferAutoruBuilder.getSellerBuilder.getPlaceBuilder.setGeobaseId(217)
    func(offerBuilder)
    val offer: Offer = offerBuilder.build()
    assert(worker.shouldSendOffer(offer, now.getMillis) == shouldSend)
    newDelay match {
      case Some(d) =>
        worker.process(offer, None).nextCheck.get.getMillis shouldBe new DateTime().plus(d).getMillis +- 2500
      case None =>
        assert(worker.process(offer, None).nextCheck.isEmpty)
    }
  }
}
