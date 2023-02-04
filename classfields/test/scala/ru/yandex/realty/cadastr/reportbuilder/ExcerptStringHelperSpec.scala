package ru.yandex.realty.cadastr.reportbuilder

import org.junit.runner.RunWith
import org.scalatest.WordSpecLike
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.cadastr.backend.ExcerptStringHelper

@RunWith(classOf[JUnitRunner])
class ExcerptStringHelperSpec extends WordSpecLike {

  "ExcerptStringHelper" should {

    "disguise flat number in public reports" in {
      val addresses = Seq(
        "Москва, Тверская д1, кв 155",
        "Москва, улица Мира, кв. 2",
        "Калининград, улица Пушкина, дом 8, кв. 43ё",
        "Москва, Липовая аллея, д. 123, Кв. 2",
        "Воронеж, улица Квартальная, д7, кв 12",
        "Тамбов, Ананасовая ул, дом 10, кв.91",
        "Сочи, улица имени квадрилиона, дом 10, кв 117",
        "Саратов, улица Строителей, дом 24, кв15"
      )

      val disguised = Seq(
        "Москва, Тверская д1, кв *",
        "Москва, улица Мира, кв. *",
        "Калининград, улица Пушкина, дом 8, кв. *",
        "Москва, Липовая аллея, д. 123, Кв. *",
        "Воронеж, улица Квартальная, д7, кв *",
        "Тамбов, Ананасовая ул, дом 10, кв.*",
        "Сочи, улица имени квадрилиона, дом 10, кв *",
        "Саратов, улица Строителей, дом 24, кв*"
      )

      val cut = Seq(
        "Москва, Тверская д1",
        "Москва, улица Мира",
        "Калининград, улица Пушкина, дом 8",
        "Москва, Липовая аллея, д. 123",
        "Воронеж, улица Квартальная, д7",
        "Тамбов, Ананасовая ул, дом 10",
        "Сочи, улица имени квадрилиона, дом 10",
        "Саратов, улица Строителей, дом 24"
      )

      val disguisedResult = addresses.map(ExcerptStringHelper.disguiseAddress)
      assert(disguisedResult == disguised)

      val cutResult = addresses.map(ExcerptStringHelper.cutApartmentNumberFromAddress)
      assert(cutResult == cut)
    }
  }
}
