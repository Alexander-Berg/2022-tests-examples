package ru.yandex.vertis.feedprocessor.autoru.scheduler.parser

import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._
import ru.yandex.vertis.feedprocessor.autoru.model.{Generators, TaskContext}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.CarExternalOffer
import ru.yandex.vertis.feedprocessor.autoru.scheduler.util.AutoruGenerators

class AutoruExternalOfferBuilderSpec extends WordSpecBase {

  private val offerBuilder = {
    val task = tasksGen(serviceInfoGen = carServiceInfoGen(Section.NEW)).next
    new AutoruExternalOfferBuilder[CarExternalOffer](TaskContext(task)) {
      override def result(
          category: Category,
          section: Section,
          position: Int
        )(implicit tc: TaskContext): CarExternalOffer = {
        AutoruGenerators.carExternalOfferGen(task).next
      }
    }
  }

  "prepareVin" should {
    "replace cyrillic symbols" in {
      val vin = "0 1 2 3 4 5 6 7 8 9 А В С D Е F G Н J К L М N Р R S Т U V W Х Y Z"
      val expected = "0 1 2 3 4 5 6 7 8 9 A B C D E F G H J K L M N P R S T U V W X Y Z"

      offerBuilder.prepareVin(vin) shouldBe expected
    }
  }

}
