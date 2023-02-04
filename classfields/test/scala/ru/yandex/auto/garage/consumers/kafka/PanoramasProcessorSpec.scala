package ru.yandex.auto.garage.consumers.kafka

import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.panoramas.CommonModel.PanoramaType
import ru.auto.panoramas.InteriorModel.InteriorPanorama
import ru.auto.panoramas.KafkaEventsModel
import ru.auto.panoramas.KafkaEventsModel.{PanoramaEvent, PoiCountChanged}
import ru.auto.panoramas.PanoramasModel.Panorama
import ru.yandex.auto.garage.dao.CardsService
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.GarageCard
import ru.yandex.auto.vin.decoder.utils.RequestInfo
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext

class PanoramasProcessorSpec extends AnyWordSpecLike with MockitoSupport {

  private val cardsService = mock[CardsService]
  implicit private val ec: ExecutionContext = ExecutionContext.global
  implicit private val r: RequestInfo = RequestInfo.Empty
  private val defaultCard = GarageCard.newBuilder().build()

  private val processor = new PanoramasProcessor(cardsService)

  "PanoramasProcessor" should {
    "process event with unknown payloadCase" in {
      val event = PanoramaEvent.newBuilder().setPanoramaId("123").build()
      assert(processor.processCard(defaultCard, event).isEmpty)
    }

    "process event with unknown panorama type" in {
      val event = PanoramaEvent
        .newBuilder()
        .setPanoramaId("123")
        .setPanoramaType(PanoramaType.UNKNOWN_PANORAMA)
        .build()
      assert(processor.processCard(defaultCard, event).isEmpty)
    }

    "process exterior panorama" in {
      val event = PanoramaEvent
        .newBuilder()
        .setPanoramaId("123")
        .setPanoramaType(PanoramaType.EXTERIOR_PANORAMA)
        .setPanoramaProcessed(
          KafkaEventsModel.PanoramaProcessed
            .newBuilder()
            .setExteriorPanorama(
              Panorama
                .newBuilder()
                .setId("123")
            )
        )
        .build()
      val newCard = processor
        .processCard(defaultCard, event)

      assert(
        newCard
          .map(_._1.getVehicleInfo.getExteriorPanorama.getPanorama.getId)
          .contains("123")
      )
      assert(
        newCard
          .map(_._1.getVehicleInfo.getInteriorPanorama.getPanorama.getId)
          .contains("")
      )
    }

    "process interior panorama" in {
      val event = PanoramaEvent
        .newBuilder()
        .setPanoramaId("123")
        .setPanoramaType(PanoramaType.INTERIOR_PANORAMA)
        .setPanoramaProcessed(
          KafkaEventsModel.PanoramaProcessed
            .newBuilder()
            .setInteriorPanorama(
              InteriorPanorama
                .newBuilder()
                .setId("123")
            )
        )
        .build()
      val newCard = processor
        .processCard(defaultCard, event)

      assert(
        newCard
          .map(_._1.getVehicleInfo.getInteriorPanorama.getPanorama.getId)
          .contains("123")
      )
      assert(
        newCard
          .map(_._1.getVehicleInfo.getExteriorPanorama.getPanorama.getId)
          .contains("")
      )
    }

    "process exterior poi count change" in {
      val event = PanoramaEvent
        .newBuilder()
        .setPanoramaId("123")
        .setPanoramaType(PanoramaType.EXTERIOR_PANORAMA)
        .setPanoramaProcessed(
          KafkaEventsModel.PanoramaProcessed
            .newBuilder()
            .setExteriorPanorama(
              Panorama
                .newBuilder()
                .setId("123")
            )
        )
        .build()
      val withPanorama = processor
        .processCard(defaultCard, event)

      val poiEvent = PanoramaEvent
        .newBuilder()
        .setPanoramaId("123")
        .setPanoramaType(PanoramaType.EXTERIOR_PANORAMA)
        .setPoiCountChanged(PoiCountChanged.newBuilder().setPoiCount(10))
        .build()

      val newCard = processor
        .processCard(withPanorama.head._1, poiEvent)

      assert(
        newCard
          .map(_._1.getVehicleInfo.getExteriorPanorama.getPanorama.getId)
          .contains("123")
      )
      assert(
        newCard
          .map(_._1.getVehicleInfo.getExteriorPanorama.getPoiCount)
          .contains(10)
      )

    }

    "process interior poi count change" in {
      val event = PanoramaEvent
        .newBuilder()
        .setPanoramaId("123")
        .setPanoramaType(PanoramaType.INTERIOR_PANORAMA)
        .setPanoramaProcessed(
          KafkaEventsModel.PanoramaProcessed
            .newBuilder()
            .setInteriorPanorama(
              InteriorPanorama
                .newBuilder()
                .setId("123")
            )
        )
        .build()
      val withPanorama = processor
        .processCard(defaultCard, event)

      val poiEvent = PanoramaEvent
        .newBuilder()
        .setPanoramaId("123")
        .setPanoramaType(PanoramaType.INTERIOR_PANORAMA)
        .setPoiCountChanged(PoiCountChanged.newBuilder().setPoiCount(10))
        .build()

      val newCard = processor
        .processCard(withPanorama.head._1, poiEvent)

      assert(
        newCard
          .map(_._1.getVehicleInfo.getInteriorPanorama.getPanorama.getId)
          .contains("123")
      )
      assert(
        newCard
          .map(_._1.getVehicleInfo.getInteriorPanorama.getPoiCount)
          .contains(10)
      )
    }
  }

}
