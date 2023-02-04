package ru.yandex.vos2.autoru.utils.converters.formoffer

import org.junit.runner.RunWith
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.AutoruModel.AutoruOffer.EquipmentSource
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.utils.FormTestUtils
import ru.yandex.vos2.autoru.utils.booking.impl.EmptyDefaultBookingAllowedDeciderImpl

import java.time.Instant
import scala.jdk.CollectionConverters._

@RunWith(classOf[JUnitRunner])
class FormOfferEquipmentsConverterTest extends AnyWordSpec with InitTestDbs {
  initDbs()

  private val formTestUtils = new FormTestUtils(components)
  import formTestUtils._

  private val formOfferConverter: FormOfferConverter =
    new FormOfferConverter(
      components.carsCatalog,
      components.recognizedLpUtils,
      EmptyDefaultBookingAllowedDeciderImpl,
      components.featuresManager
    )

  final private case class TestCase(description: String,
                                    previousEquipments: Map[String, Boolean],
                                    offerMeta: Map[String, EquipmentSource],
                                    formEquipment: Map[String, Boolean],
                                    expectedEquipments: Map[String, Boolean])

  private val testCases: Seq[TestCase] = Seq(
    TestCase(
      description = "Add new equipment",
      previousEquipments = Map(
        "some-existing-enabled-equipment" -> true,
        "some-existing-disabled-equipment-for-override" -> false
      ),
      offerMeta = Map.empty,
      formEquipment = Map(
        "new-enabled-equipment" -> true,
        "new-disabled-equipment" -> false,
        "some-existing-disabled-equipment-for-override" -> true
      ),
      expectedEquipments = Map(
        "some-existing-enabled-equipment" -> false,
        "some-existing-disabled-equipment-for-override" -> true,
        "new-enabled-equipment" -> true,
        "new-disabled-equipment" -> false
      )
    ),
    TestCase(
      description = "Disable options not present in the form",
      previousEquipments = Map(
        "enabled-vin-decoder-equipment" -> true,
        "disabled-vin-decoder-equipment" -> false,
        "enabled-description-parsing-equipment" -> true,
        "disabled-description-parsing-equipment" -> false,
        "enabled-equipment" -> true,
        "disabled-equipment" -> false
      ),
      offerMeta = {
        import EquipmentSource._
        Map(
          "enabled-vin-decoder-equipment" -> VIN_DECODER,
          "disabled-vin-decoder-equipment" -> VIN_DECODER,
          "enabled-description-parsing-equipment" -> DESCRIPTION_PARSING,
          "disabled-description-parsing-equipment" -> DESCRIPTION_PARSING
        )
      },
      formEquipment = Map("new-enabled-equipment" -> true, "new-disabled-equipment" -> false),
      expectedEquipments = Map(
        "enabled-vin-decoder-equipment" -> false,
        "disabled-vin-decoder-equipment" -> false,
        "enabled-description-parsing-equipment" -> false,
        "disabled-description-parsing-equipment" -> false,
        "enabled-equipment" -> false,
        "disabled-equipment" -> false,
        "new-enabled-equipment" -> true,
        "new-disabled-equipment" -> false
      )
    ),
    TestCase(
      description = "Restore from meta",
      previousEquipments = Map("existing-disabled-vin-decoder-equipment" -> false),
      offerMeta = {
        import EquipmentSource._
        Map(
          "existing-disabled-vin-decoder-equipment" -> VIN_DECODER,
          "description-parsing-equipment-for-restore" -> DESCRIPTION_PARSING
        )
      },
      formEquipment = Map.empty,
      expectedEquipments = Map(
        "existing-disabled-vin-decoder-equipment" -> false,
        "description-parsing-equipment-for-restore" -> true
      )
    )
  )

  private def now(): Long = Instant.now.getEpochSecond

  "CarInfoConverter.convertExistingOffer" when {
    testCases.foreach {
      case TestCase(description, previousEquipments, offerMeta, formEquipment, expectedEquipments) =>
        description in {
          val form = {
            val builder = salonOfferForm.toBuilder
            val carInfoBuilder = builder.getCarInfoBuilder
            carInfoBuilder.clearEquipment
            formEquipment.foreach {
              case (name, enabled) =>
                carInfoBuilder.putEquipment(name, enabled)
            }
            builder.build
          }
          val curOffer = {
            val builder = curPrivateProto.toBuilder
            val carInfoBuilder = builder.getOfferAutoruBuilder.getCarInfoBuilder
            carInfoBuilder.clearEquipment
            previousEquipments.foreach {
              case (name, enabled) =>
                carInfoBuilder.addEquipment(AutoruOffer.Equipment.newBuilder.setName(name).setEquipped(enabled))
            }
            carInfoBuilder.clearEquipmentsMeta
            offerMeta.foreach {
              case (name, equipmentSource) =>
                val equipmentMeta = AutoruOffer.EquipmentMeta.newBuilder.setSource(equipmentSource).build
                carInfoBuilder.getEquipmentsMetaBuilder.putEquipmentMeta(name, equipmentMeta).setDescriptionParsed(true)
            }
            builder.build
          }
          val result = formOfferConverter.convertExistingOffer(
            form = form,
            curOffer = curOffer,
            optDraft = None,
            ad = salonAd,
            now = now()
          )
          val actualEquipments = result.getOfferAutoru.getCarInfo.getEquipmentList.asScala.map { equipment =>
            equipment.getName -> equipment.getEquipped
          }.toMap
          assert(actualEquipments == expectedEquipments)
        }
    }
  }
}
