package ru.auto.catalog.core.managers.tech_info.group

import ru.auto.api.BreadcrumbsModel.TechParamsEntity
import ru.auto.catalog.core.managers.tech_info.HumanInfoManager.TechInfoHolder
import ru.auto.catalog.core.testkit._

class EngineGroupBuilderSpec extends AbstractGroupBuilderSpec {
  private val builder = new EngineGroupBuilder(verbaCars)

  trait mock {
    val electro = ("ELECTRO", "Электро")
    val gasoline = ("GASOLINE", "Бензин")
    val engineOrder = ("Front transverse engine", "переднее, поперечное")
    val feeding = ("None", "нет")
    val engineFeeding = ("Multipoint fuel injection", "распределенный впрыск (многоточечный)")
    val moment = (115.0f, "115")
    val diameter = "80x67.2"
    val displacement = 1351
    val cylindersValue = 4
    val cylindersOrder = "OPPOSITE"
    val valves = 2
    val compression = 9.5f

    private val techParams = TechParamsEntity
      .newBuilder()
      .setEngineType(gasoline._1)
      .setEngineOrder(engineOrder._1)
      .setFeeding(feeding._1)
      .setEngineFeeding(engineFeeding._1)
      .setMoment(moment._1)
      .setDiameter(diameter)
      .setDisplacement(displacement)
      .setValves(valves)
      .setCompression(compression)
      .build()

    val techInfoHolder = mock[TechInfoHolder]
    when(techInfoHolder.techParams).thenReturn(techParams)

    private val electroTechParams = TechParamsEntity
      .newBuilder()
      .setEngineType(electro._1)
      .setEngineOrder(engineOrder._1)
      .setFeeding(feeding._1)
      .setValves(valves)
      .build()

    val techInfoHolderForElectro = mock[TechInfoHolder]
    when(techInfoHolderForElectro.techParams).thenReturn(electroTechParams)
  }

  "EngineGroupBuilder" should {
    "return full result" in new mock {
      private val result = builder.build(techInfoHolder)

      checkValue(result, "engine_type", gasoline._2)
      checkValue(result, "engine_order", engineOrder._2)
      checkValue(result, "feeding", feeding._2)
      checkValue(result, "engine_feeding", engineFeeding._2)
      checkValue(result, "moment", moment._2)
      checkValue(result, "diameter", diameter)
      checkValue(result, "displacement", displacement.toString)
      checkValue(result, "valves", valves.toString)
      checkValue(result, "compression", compression.toString)
    }

    "return specific result for Electro engine type" in new mock {
      private val result = builder.build(techInfoHolderForElectro)

      checkValue(result, "engine_type", electro._2)
      checkValue(result, "engine_order", engineOrder._2)
      checkValue(result, "feeding", feeding._2)

      checkEmpty(result, "engine_feeding")
      checkEmpty(result, "moment")
      checkEmpty(result, "diameter")
      checkEmpty(result, "displacement")
      checkEmpty(result, "valves")
      checkEmpty(result, "compression")
    }
  }
}
