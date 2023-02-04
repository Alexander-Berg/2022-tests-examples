package ru.yandex.auto.vin.decoder.partners.carsharing

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.yandex.auto.vin.decoder.partners.carsharing.model.MarkModel

class MarkModelTest extends AnyFunSuite with Matchers {

  test("test contains") {
    val seq = Seq(
      MarkModel("Toyota", "COrolla"),
      MarkModel("Nissan", "Tiida"),
      MarkModel("BMW", "3")
    )
    MarkModel("toyota", "Corolla").equalsIgnoreCase(seq.head) shouldBe true
    MarkModel("toyota", "Corolla").equalsIgnoreCase(seq.tail.head) shouldBe false

  }

}
