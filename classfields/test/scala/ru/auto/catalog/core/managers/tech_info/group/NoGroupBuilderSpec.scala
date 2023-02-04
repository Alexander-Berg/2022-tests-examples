package ru.auto.catalog.core.managers.tech_info.group

import ru.auto.api.BreadcrumbsModel.TechParamsEntity
import ru.auto.catalog.core.managers.tech_info.HumanInfoManager.TechInfoHolder
import ru.auto.catalog.core.testkit._

class NoGroupBuilderSpec extends AbstractGroupBuilderSpec {
  private val builder = new NoGroupBuilder(verbaCars)

  trait mock {
    val mechanical = ("MECHANICAL", "Механика")
    val electro = ("ELECTRO", "Электро")
    val gasoline = ("GASOLINE", "Бензин")
    val displacement = (1351, "1.4")
    val power = 110
    val powerKvt = "215"

    private val electroTechParams = TechParamsEntity
      .newBuilder()
      .setEngineType(electro._1)
      .setPower(power)
      .setPowerKvt(powerKvt)
      .build()

    private val techParams = TechParamsEntity
      .newBuilder()
      .setEngineType(gasoline._1)
      .setPower(power)
      .setPowerKvt(powerKvt)
      .setTransmission(mechanical._1)
      .setDisplacement(displacement._1)
      .build()

    private val withoutKvtTechParams = TechParamsEntity
      .newBuilder()
      .setEngineType(gasoline._1)
      .setPower(power)
      .setTransmission(mechanical._1)
      .setDisplacement(displacement._1)
      .build()

    val techInfoHolder = mock[TechInfoHolder]
    when(techInfoHolder.techParams).thenReturn(techParams)

    val techInfoHolderForElectro = mock[TechInfoHolder]
    when(techInfoHolderForElectro.techParams).thenReturn(electroTechParams)

    val techInfoHolderForNonKvt = mock[TechInfoHolder]
    when(techInfoHolderForNonKvt.techParams).thenReturn(withoutKvtTechParams)
  }

  "NoGroupBuilder" should {
    "return full result" in new mock {
      private val result = builder.build(techInfoHolder)

      checkValue(result, "engine_type", gasoline._2)
      checkValue(result, "power", power.toString)
      checkEmpty(result, "power_kvt")
      checkValue(result, "transmission", mechanical._2)
      checkValue(result, "displacement", displacement._2)
    }

    "not return specific result for Electro engine type" in new mock {
      private val result = builder.build(techInfoHolderForElectro)

      checkValue(result, "engine_type", electro._2)
      checkValue(result, "power", power.toString)
      checkEmpty(result, "power_kvt")

      checkEmpty(result, "displacement")
    }

    "not return power_kvt" in new mock {
      private val result = builder.build(techInfoHolderForNonKvt)

      checkValue(result, "engine_type", gasoline._2)
      checkValue(result, "power", power.toString)
      checkEmpty(result, "power_kvt")
    }
  }
}
