package ru.auto.catalog.core.managers

import ru.auto.catalog.BaseSpec
import ru.auto.catalog.core.model.raw.cars.CarsCatalogWrapperBuilder
import ru.auto.catalog.core.testkit.{EmptyCarsSearchTagsInheritanceDecider, TestDataEngine}
import ru.auto.catalog.model.api.ApiModel.DescriptionParseRequest
import ru.yandex.vertis.baker.util.api.{Request, RequestImpl}

import scala.jdk.CollectionConverters._

class OptionsManagerIntSpec extends BaseSpec {
  private val carsCatalog = new CarsCatalogWrapperBuilder(EmptyCarsSearchTagsInheritanceDecider).from(TestDataEngine)
  private val manager = new OptionsManager(carsCatalog)

  implicit private val req: Request = new RequestImpl

  "Options manager" should {

    spec("диски 17")(Set("17-inch-wheels"))
    spec("17")(Set())
    spec("пер. стеклоподьемники")(Set("electro-window-front"))
    spec("Передние")(Set())
    spec("Диски 177777")(Set())
    spec("Диски 17,")(Set("17-inch-wheels"))
    spec("Бортовой компьютер")(Set("computer"))
    spec("Возможно бронирование авто")(Set())

    def spec(description: String)(expectedOptions: Set[String]): Unit = {
      s"parse $expectedOptions from '$description'" in {
        val withDisks = DescriptionParseRequest
          .newBuilder()
          .setDescription(description)
          .build()
        val res1 = manager.parseDescriptionToOptions(withDisks)

        expectedOptions should contain theSameElementsAs res1.getOptionsList.asScala.map(_.getValue.getCode).toSet
      }
    }
  }
}
