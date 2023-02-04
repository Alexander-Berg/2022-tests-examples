package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import com.google.protobuf.Timestamp
import org.mockito.Mockito.{times, verify, verifyNoMoreInteractions}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.ResponseModel.{ResponseStatus, SuccessResponse}
import ru.auto.panoramas.InteriorModel.{InteriorPanorama => InternalPanorama}
import ru.auto.panoramas.PanoramasModel
import ru.yandex.auto.core.AutoSchemaVersions
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.AutoruModel.AutoruOffer.{ExternalPanorama, InteriorPanorama}
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.getNow
import ru.yandex.vos2.services.panoramas.HttpPanoramasClient

import scala.concurrent.duration.DurationInt

class BypassPanoramaWorkerYdbTest extends AnyWordSpec with Matchers with MockitoSupport {

  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {

    val successResponse = SuccessResponse.newBuilder().setStatus(ResponseStatus.SUCCESS).build()

    val externalPanoramaBuilder = ExternalPanorama
      .newBuilder()
      .setId("extId1")
      .setStatus(ExternalPanorama.Status.PROCESSING)
      .setPublished(false)
      .setUpdateAt(buildTimestamp(getNow - 1.hour.toMillis))

    val interiorPanoramaBuilder = InteriorPanorama
      .newBuilder()
      .setPanorama(
        InternalPanorama
          .newBuilder()
          .setId("intId1")
          .setStatus(PanoramasModel.Status.PROCESSING)
          .setPublished(false)
          .build()
      )
      .setUpdateAt(buildTimestamp(getNow - 1.hour.toSeconds))
      .setPublished(false)

    val offerBuilder = TestUtils
      .createOffer()
      .setOfferAutoru(
        AutoruOffer
          .newBuilder()
          .setCategory(Category.CARS)
          .setVersion(AutoSchemaVersions.CAR_AD_VERSION)
          .addExternalPanorama(externalPanoramaBuilder.build())
          .addInteriorPanorama(interiorPanoramaBuilder.build())
      )

    val mockedPanoramasClient: HttpPanoramasClient = mock[HttpPanoramasClient]

    val worker = new BypassPanoramaWorkerYdb(
      mockedPanoramasClient,
      TestOperationalSupport
    ) with YdbWorkerTestImpl
  }

  "Offer with panoramas in status COMPLETED shouldn't be processed" in new Fixture {

    val offer = offerBuilder
      .setOfferAutoru(
        offerBuilder.getOfferAutoruBuilder
          .setExternalPanorama(
            0,
            externalPanoramaBuilder
              .setStatus(ExternalPanorama.Status.COMPLETED)
              .setPublished(true)
              .build()
          )
          .setInteriorPanorama(
            0,
            interiorPanoramaBuilder
              .setPanorama(
                interiorPanoramaBuilder.getPanoramaBuilder
                  .setStatus(PanoramasModel.Status.COMPLETED)
                  .setPublished(true)
                  .build()
              )
              .setPublished(true)
              .build()
          )
          .build()
      )
      .build()

    val result = worker.shouldProcess(offer, None)
    assert(!result.shouldProcess)
  }

  "Offer with panoramas in status PROCESSING or AWAITING_PROCESSING less than 3 hours shouldn't be processed" in new Fixture {

    val offer = offerBuilder
      .setOfferAutoru(
        offerBuilder.getOfferAutoruBuilder
          .setExternalPanorama(
            0,
            externalPanoramaBuilder
              .setStatus(ExternalPanorama.Status.PROCESSING)
              .setUpdateAt(buildTimestamp(getNow - 1.hour.toMillis))
              .setPublished(false)
              .build()
          )
          .setInteriorPanorama(
            0,
            interiorPanoramaBuilder
              .setPanorama(
                interiorPanoramaBuilder.getPanoramaBuilder
                  .setStatus(PanoramasModel.Status.AWAITING_PROCESSING)
                  .setPublished(false)
                  .build()
              )
              .setUpdateAt(buildTimestamp(getNow - 2.hour.toMillis))
              .setPublished(false)
              .build()
          )
          .build()
      )
      .build()

    val result = worker.shouldProcess(offer, None)
    assert(!result.shouldProcess)
  }

  "Offer without panoramas shouldn't be processed" in new Fixture {

    val offer = offerBuilder
      .setOfferAutoru(
        offerBuilder.getOfferAutoruBuilder
          .removeExternalPanorama(0)
          .removeInteriorPanorama(0)
          .build()
      )
      .build()

    val result = worker.shouldProcess(offer, None)
    assert(!result.shouldProcess)
  }

  "Offer with panoramas in status PROCESSING more than 3 hours should be processed" in new Fixture {

    val offer = offerBuilder
      .setOfferAutoru(
        offerBuilder.getOfferAutoruBuilder
          .setExternalPanorama(
            0,
            externalPanoramaBuilder
              .setStatus(ExternalPanorama.Status.PROCESSING)
              .setUpdateAt(buildTimestamp(getNow - 4.hour.toMillis))
              .setPublished(false)
              .build()
          )
          .setInteriorPanorama(
            0,
            interiorPanoramaBuilder
              .setPanorama(
                interiorPanoramaBuilder.getPanoramaBuilder
                  .setStatus(PanoramasModel.Status.PROCESSING)
                  .setPublished(false)
                  .build()
              )
              .setUpdateAt(buildTimestamp(getNow - 5.hour.toMillis))
              .setPublished(false)
              .build()
          )
          .build()
      )
      .build()

    when(mockedPanoramasClient.bypassExteriorPanorama(Set("extId1"))).thenReturn(Option(successResponse))
    when(mockedPanoramasClient.bypassInteriorPanorama(Set("intId1"))).thenReturn(Option(successResponse))

    val shouldProcessResult = worker.shouldProcess(offer, None)
    assert(shouldProcessResult.shouldProcess)

    val processResult = worker.process(offer, None)
    assert(processResult.nextCheck.isDefined)

    verify(mockedPanoramasClient, times(1)).bypassExteriorPanorama(Set("extId1"))
    verify(mockedPanoramasClient, times(1)).bypassInteriorPanorama(Set("intId1"))
    verifyNoMoreInteractions(mockedPanoramasClient)
  }

  "Offer with panoramas in status AWAITING_PROCESSING more than 3 hours should be processed" in new Fixture {

    val offer = offerBuilder
      .setOfferAutoru(
        offerBuilder.getOfferAutoruBuilder
          .setExternalPanorama(
            0,
            externalPanoramaBuilder
              .setStatus(ExternalPanorama.Status.AWAITING_PROCESSING)
              .setUpdateAt(buildTimestamp(getNow - 4.hour.toMillis))
              .setPublished(false)
              .build()
          )
          .setInteriorPanorama(
            0,
            interiorPanoramaBuilder
              .setPanorama(
                interiorPanoramaBuilder.getPanoramaBuilder
                  .setStatus(PanoramasModel.Status.AWAITING_PROCESSING)
                  .setPublished(false)
                  .build()
              )
              .setUpdateAt(buildTimestamp(getNow - 5.hour.toMillis))
              .setPublished(false)
              .build()
          )
          .build()
      )
      .build()

    when(mockedPanoramasClient.bypassExteriorPanorama(Set("extId1"))).thenReturn(Option(successResponse))
    when(mockedPanoramasClient.bypassInteriorPanorama(Set("intId1"))).thenReturn(Option(successResponse))

    val shouldProcessResult = worker.shouldProcess(offer, None)
    assert(shouldProcessResult.shouldProcess)

    val processResult = worker.process(offer, None)
    assert(processResult.nextCheck.isDefined)

    verify(mockedPanoramasClient, times(1)).bypassExteriorPanorama(Set("extId1"))
    verify(mockedPanoramasClient, times(1)).bypassInteriorPanorama(Set("intId1"))
    verifyNoMoreInteractions(mockedPanoramasClient)
  }

  "Offer with only external panoramas in status PROCESSING more than 3 hours should be processed" in new Fixture {

    val offer = offerBuilder
      .setOfferAutoru(
        offerBuilder.getOfferAutoruBuilder
          .setExternalPanorama(
            0,
            externalPanoramaBuilder
              .setStatus(ExternalPanorama.Status.AWAITING_PROCESSING)
              .setUpdateAt(buildTimestamp(getNow - 4.hour.toMillis))
              .setPublished(false)
              .build()
          )
          .removeInteriorPanorama(0)
          .build()
      )
      .build()

    when(mockedPanoramasClient.bypassExteriorPanorama(Set("extId1"))).thenReturn(Option(successResponse))

    val shouldProcessResult = worker.shouldProcess(offer, None)
    assert(shouldProcessResult.shouldProcess)

    val processResult = worker.process(offer, None)
    assert(processResult.nextCheck.isDefined)

    verify(mockedPanoramasClient, times(1)).bypassExteriorPanorama(Set("extId1"))
    verify(mockedPanoramasClient, times(0)).bypassInteriorPanorama(Set("intId1"))
    verifyNoMoreInteractions(mockedPanoramasClient)
  }

  "Offer with only interior panoramas in status PROCESSING more than 3 hours should be processed" in new Fixture {

    val offer = offerBuilder
      .setOfferAutoru(
        offerBuilder.getOfferAutoruBuilder
          .removeExternalPanorama(0)
          .setInteriorPanorama(
            0,
            interiorPanoramaBuilder
              .setPanorama(
                interiorPanoramaBuilder.getPanoramaBuilder
                  .setStatus(PanoramasModel.Status.AWAITING_PROCESSING)
                  .setPublished(false)
                  .build()
              )
              .setUpdateAt(buildTimestamp(getNow - 5.hour.toMillis))
              .setPublished(false)
              .build()
          )
          .build()
      )
      .build()

    when(mockedPanoramasClient.bypassInteriorPanorama(Set("intId1"))).thenReturn(Option(successResponse))

    val shouldProcessResult = worker.shouldProcess(offer, None)
    assert(shouldProcessResult.shouldProcess)

    val processResult = worker.process(offer, None)
    assert(processResult.nextCheck.isDefined)

    verify(mockedPanoramasClient, times(0)).bypassExteriorPanorama(Set("extId1"))
    verify(mockedPanoramasClient, times(1)).bypassInteriorPanorama(Set("intId1"))
    verifyNoMoreInteractions(mockedPanoramasClient)
  }

  private def buildTimestamp(millis: Long): Timestamp = {
    Timestamp.newBuilder().setSeconds(millis / 1000).build()
  }
}
