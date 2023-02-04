package ru.yandex.vos2.autoru.vin_decoder

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.auto.vin.decoder.events.Events.EssentialsReportEvent
import ru.yandex.vos2.AutoruModel.AutoruOffer.{Equipment, EquipmentMeta, EquipmentSource}
import ru.yandex.vos2.OfferModel.{Offer, OfferService}
import ru.yandex.vos2.autoru.vin_decoder.impl.VinDecoderDeciderImpl

import java.time.Instant
import scala.jdk.CollectionConverters._

class VinDecoderDeciderSpec extends AnyWordSpec with Matchers {

  private val someVin = "1HD1DDV13YY648395"

  "VinDecoderDecider" should {
    "enrich equipments" in {
      val equipmentsMap =
        Map("new1" -> true, "new2" -> false, "halogen" -> true, "electro-window-back" -> true).view
          .mapValues(Boolean.box)
          .toMap
      val event = {
        val b = EssentialsReportEvent.newBuilder.setVin(someVin)
        b.getReportBuilder.getVehicleBuilder.getCarInfoBuilder.putAllEquipment(equipmentsMap.asJava)
        b.build
      }
      val offerEquipments = Seq("source1" -> true, "source2" -> false, "electro-window-all" -> true)
      val offer = {
        val b = Offer.newBuilder
          .setTimestampUpdate(Instant.now.toEpochMilli)
          .setUserRef("ac_26352")
          .setOfferService(OfferService.OFFER_AUTO)
        b.getOfferAutoruBuilder
          .setVersion(1)
          .setCategory(Category.CARS)
          .getCarInfoBuilder
          .addAllEquipment(toOfferModel(offerEquipments).asJava)
        b.build
      }
      val expectedEquipments = Seq(
        "source1" -> true,
        "source2" -> false,
        "electro-window-all" -> true,
        "new1" -> true
      )
      val expectedOffer = {
        val equipmentMetaMap = Map(
          "new1" -> EquipmentMeta.newBuilder.setSource(EquipmentSource.VIN_DECODER).build
        )
        val b = offer.toBuilder
        val carInfoBuilder = b.getOfferAutoruBuilder.getCarInfoBuilder
        carInfoBuilder.clearEquipment
          .addAllEquipment(toOfferModel(expectedEquipments).asJava)
        carInfoBuilder.getEquipmentsMetaBuilder
          .setDescriptionParsed(false)
          .putAllEquipmentMeta(equipmentMetaMap.asJava)
        b.build
      }
      val actualDecision = VinDecoderDeciderImpl.decide(offer, event)
      val actual = actualDecision.getUpdate.map(_.toBuilder.clearTimestampAnyUpdate.build)
      val expected = Some(expectedOffer)
      actual shouldEqual expected
      actualDecision.isEmpty shouldEqual false
    }

    "do not enrich equipments if already exists" in {
      val equipmentsMap =
        Map("some-equipment" -> true, "electro-window-all" -> true).view.mapValues(Boolean.box).toMap
      val event = {
        val b = EssentialsReportEvent.newBuilder.setVin(someVin)
        b.getReportBuilder.getVehicleBuilder.getCarInfoBuilder.putAllEquipment(equipmentsMap.asJava)
        b.build
      }
      val offerEquipments =
        Seq("some-equipment" -> false, "electro-window-front" -> true, "electro-window-back" -> true)
      val offer = {
        val b = Offer.newBuilder
          .setTimestampUpdate(Instant.now.toEpochMilli)
          .setUserRef("ac_26352")
          .setOfferService(OfferService.OFFER_AUTO)
        b.getOfferAutoruBuilder
          .setVersion(1)
          .setCategory(Category.CARS)
          .getCarInfoBuilder
          .addAllEquipment(toOfferModel(offerEquipments).asJava)
        b.build
      }
      val actual = VinDecoderDeciderImpl.decide(offer, event)
      actual.isEmpty shouldEqual true
    }
  }

  private def toOfferModel(equipments: Seq[(String, Boolean)]): Iterable[Equipment] = {
    equipments.map {
      case (equipmentName, equipmentValue) =>
        Equipment.newBuilder.setName(equipmentName).setEquipped(equipmentValue).build
    }
  }
}
