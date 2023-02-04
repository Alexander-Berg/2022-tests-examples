package ru.auto.api.managers.garage.enricher

import org.scalatest.prop.TableDrivenPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.managers.garage.enricher.SearchRequestHelper.FuzzyRound
import ru.auto.api.search.SearchModel.SearchRequestParameters
import ru.auto.api.ui.UiModel.{BodyTypeGroup, EngineGroup}
import ru.auto.api.vin.garage.GarageApiModel.Card
import ru.auto.api.CarsModel.Car.{GearType, Transmission}
import ru.auto.catalog.model.api.ApiModel.{ConfigurationCard, RawCatalog, TechParamCard}
import ru.yandex.vertis.protobuf.ProtoMacro.opt

import scala.jdk.CollectionConverters.CollectionHasAsScala

class SearchRequestHelperSpec extends BaseSpec with TableDrivenPropertyChecks {

  "fuzzyRound" should {

    def fuzzyRound(number: BigDecimal, precision: Int, digits: Option[Int]): BigDecimal =
      digits
        .map(d => number.fuzzyRound(precision, d))
        .getOrElse(number.fuzzyRound(precision))

    "match expected results for integer inputs" in new FuzzyRoundData {
      forAll(fuzzyRoundIntExamples) { (num, precision, digits, result) =>
        val actual = fuzzyRound(BigDecimal(num), precision, digits)
        val expected = BigDecimal(result)
        actual shouldBe expected
      }
    }

    "match expected results for decimal inputs" in new FuzzyRoundData {
      forAll(fuzzyRoundDecimalExamples) { (num, precision, digits, result) =>
        val actual = fuzzyRound(BigDecimal(num), precision, digits)
        val expected = BigDecimal(result)
        actual shouldBe expected
      }
    }
  }

  "enrichSearchRequestParameters" should {

    def doEnrich(card: Card, catalogCard: RawCatalog): SearchRequestParameters =
      SearchRequestHelper.enrichSearchRequestParameters(
        SearchRequestParameters.getDefaultInstance,
        card,
        catalogCard
      )

    "enrich all technical parameters" in new SearchRequestData {
      val card: Card = generateCard()
      val catalogCard: RawCatalog = generateCatalogCard(card)

      val result: SearchRequestParameters = doEnrich(card, catalogCard)

      private val carsParams = result.getCarsParams
      carsParams.getBodyTypeGroupList.asScala.toList shouldBe List(BodyTypeGroup.ALLROAD_5_DOORS)
      carsParams.getTransmissionList.asScala.toList shouldBe List(Transmission.AUTOMATIC)
      carsParams.getEngineGroupList.asScala.toList shouldBe List(EngineGroup.DIESEL)
      carsParams.getGearTypeList.asScala.toList shouldBe List(GearType.ALL_WHEEL_DRIVE)
      result.getDisplacementFrom shouldBe 2000
      result.getDisplacementTo shouldBe 2000
      result.getPowerFrom shouldBe 190
      result.getPowerTo shouldBe 190
    }

    "ignore invalid tech params" in new SearchRequestData {
      val card: Card = generateCard()
      val catalogCard: RawCatalog = generateCatalogCard(
        card,
        transmission = "unknown_transmission",
        engineType = "unknown_engine_type",
        gearType = "unknown_gear_type"
      )

      val result: SearchRequestParameters = doEnrich(card, catalogCard)

      private val carsParams = result.getCarsParams
      carsParams.getBodyTypeGroupList.asScala.toList shouldBe List(BodyTypeGroup.ALLROAD_5_DOORS)
      result.getDisplacementFrom shouldBe 2000
      result.getDisplacementTo shouldBe 2000
      result.getPowerFrom shouldBe 190
      result.getPowerTo shouldBe 190

      carsParams.getTransmissionList.asScala shouldBe empty
      carsParams.getEngineGroupList.asScala shouldBe empty
      carsParams.getGearTypeList.asScala shouldBe empty
    }

    "round displacement up" in new SearchRequestData {
      val card: Card = generateCard()
      val catalogCard: RawCatalog = generateCatalogCard(card, displacement = 1548)

      val result: SearchRequestParameters = doEnrich(card, catalogCard)

      result.getDisplacementFrom shouldBe 1600
      result.getDisplacementTo shouldBe 1600
    }

    "round displacement down" in new SearchRequestData {
      val card: Card = generateCard()
      val catalogCard: RawCatalog = generateCatalogCard(card, displacement = 1542)

      val result: SearchRequestParameters = doEnrich(card, catalogCard)

      result.getDisplacementFrom shouldBe 1500
      result.getDisplacementTo shouldBe 1500
    }
  }

  //noinspection TypeAnnotation
  trait FuzzyRoundData {

    val fuzzyRoundIntExamples = Table(
      ("num", "precision", "digits", "result"),
      ("0", 0, Some(0), "0"),
      ("0", 0, Some(1), "0"),
      ("0", 0, Some(2), "0"),
      ("0", 0, Some(3), "0"),
      ("0", 0, None, "0"),
      ("0", 1, Some(0), "0"),
      ("0", 1, Some(1), "0"),
      ("0", 1, Some(2), "0"),
      ("0", 1, Some(3), "0"),
      ("0", 1, None, "0"),
      ("0", 2, Some(0), "0"),
      ("0", 2, Some(1), "0"),
      ("0", 2, Some(2), "0"),
      ("0", 2, Some(3), "0"),
      ("0", 2, None, "0"),
      ("1", 0, Some(0), "1"),
      ("1", 0, Some(1), "1"),
      ("1", 0, Some(2), "1"),
      ("1", 0, Some(3), "1"),
      ("1", 0, None, "1"),
      ("1", 1, Some(0), "1"),
      ("1", 1, Some(1), "1"),
      ("1", 1, Some(2), "1"),
      ("1", 1, Some(3), "1"),
      ("1", 1, None, "1"),
      ("1", 2, Some(0), "1"),
      ("1", 2, Some(1), "1"),
      ("1", 2, Some(2), "1"),
      ("1", 2, Some(3), "1"),
      ("1", 2, None, "1"),
      ("9999", 0, Some(0), "9999"),
      ("9999", 0, Some(1), "9999"),
      ("9999", 0, Some(2), "9999"),
      ("9999", 0, Some(3), "9999"),
      ("9999", 0, None, "9999"),
      ("9999", 1, Some(0), "9999"),
      ("9999", 1, Some(1), "9999"),
      ("9999", 1, Some(2), "9999"),
      ("9999", 1, Some(3), "9999"),
      ("9999", 1, None, "9999"),
      ("9999", 2, Some(0), "9999"),
      ("9999", 2, Some(1), "9999"),
      ("9999", 2, Some(2), "9999"),
      ("9999", 2, Some(3), "9999"),
      ("9999", 2, None, "9999")
    )

    val fuzzyRoundDecimalExamples = Table(
      ("num", "precision", "digits", "result"),
      ("0.1545", 0, Some(0), "0"),
      ("0.1545", 0, Some(1), "0"),
      ("0.1545", 0, Some(2), "0"),
      ("0.1545", 0, Some(3), "0"),
      ("0.1545", 0, None, "0"),
      ("0.1545", 1, Some(0), "0"),
      ("0.1545", 1, Some(1), "0.2"),
      ("0.1545", 1, Some(2), "0.2"),
      ("0.1545", 1, Some(3), "0.2"),
      ("0.1545", 1, None, "0.2"),
      ("0.1545", 2, Some(0), "0"),
      ("0.1545", 2, Some(1), "0.2"),
      ("0.1545", 2, Some(2), "0.15"),
      ("0.1545", 2, Some(3), "0.16"),
      ("0.1545", 2, None, "0.15"),
      ("1.5454", 0, Some(0), "2"),
      ("1.5454", 0, Some(1), "2"),
      ("1.5454", 0, Some(2), "2"),
      ("1.5454", 0, Some(3), "2"),
      ("1.5454", 0, None, "2"),
      ("1.5454", 1, Some(0), "2"),
      ("1.5454", 1, Some(1), "1.5"),
      ("1.5454", 1, Some(2), "1.6"),
      ("1.5454", 1, Some(3), "1.6"),
      ("1.5454", 1, None, "1.6"),
      ("1.5454", 2, Some(0), "2"),
      ("1.5454", 2, Some(1), "1.5"),
      ("1.5454", 2, Some(2), "1.55"),
      ("1.5454", 2, Some(3), "1.55"),
      ("1.5454", 2, None, "1.55")
    )
  }

  trait SearchRequestData {

    def generateCard(): Card = {
      val builder = Card
        .newBuilder()
        .setId("123456789")

      builder.getVehicleInfoBuilder.getCarInfoBuilder
        .setBodyType("ALLROAD_5_DOORS")
        .setMark("BMW")
        .setModel("X3")
        .setSuperGenId(21029610)
        .setConfigurationId(21029647)

      builder.getVehicleInfoBuilder.getCarInfoBuilder.setTechParamId(21029738)

      builder.build()
    }

    def generateCatalogCard(
        card: Card,
        bodyTypeGroup: BodyTypeGroup = BodyTypeGroup.ALLROAD_5_DOORS,
        transmission: String = "AUTOMATIC",
        engineType: String = "DIESEL",
        gearType: String = "ALL_WHEEL_DRIVE",
        displacement: Int = 1995,
        power: Int = 190
    ): RawCatalog = {
      val builder = RawCatalog.newBuilder()

      opt(card.getVehicleInfo.getCarInfo.getConfigurationId).foreach { configurationId =>
        val card = ConfigurationCard.newBuilder()
        card.getEntityBuilder.getConfigurationBuilder.setBodyTypeGroup(bodyTypeGroup)
        builder.putConfiguration(configurationId.toString, card.build)
      }

      opt(card.getVehicleInfo.getCarInfo.getTechParamId).foreach { techParamId =>
        val card = TechParamCard.newBuilder()
        card.getEntityBuilder.getTechParamsBuilder.setTransmission(transmission)
        card.getEntityBuilder.getTechParamsBuilder.setEngineType(engineType)
        card.getEntityBuilder.getTechParamsBuilder.setGearType(gearType)
        card.getEntityBuilder.getTechParamsBuilder.setDisplacement(displacement)
        card.getEntityBuilder.getTechParamsBuilder.setPower(power)
        builder.putTechParam(techParamId.toString, card.build)
      }

      builder.build()
    }
  }
}
