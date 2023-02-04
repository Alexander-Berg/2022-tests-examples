package ru.yandex.vos2.autoru.catalog.cars

import org.scalatest.OptionValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
  * Created by andrey on 8/30/17.
  */
class DictionariesTest extends AnyFunSuite with OptionValues with Matchers {
  test("Body types") {
    Dictionaries.bodyTypes("HATCHBACK_5_DOORS").ruName shouldBe "Хэтчбек 5 дв."
  }

  test("new gear_type ALL_WHEEL_DRIVE") {
    assert(Dictionaries.gearTypes.byId("3").value.code == "ALL_WHEEL_DRIVE")
    assert(Dictionaries.gearTypes.byId("7").value.code == "ALL_WHEEL_DRIVE")
    assert(Dictionaries.gearTypes.byId("182").value.code == "ALL_WHEEL_DRIVE")
    assert(Dictionaries.gearTypes.byId("183").value.code == "ALL_WHEEL_DRIVE")
    assert(Dictionaries.gearTypes.byId("1074").value.code == "ALL_WHEEL_DRIVE")

    assert(Dictionaries.gearTypes.byCode("ALL_PART").value.code == "ALL_WHEEL_DRIVE")
    assert(Dictionaries.gearTypes.byCode("ALL_FULL").value.code == "ALL_WHEEL_DRIVE")
    assert(Dictionaries.gearTypes.byCode("ALL").value.code == "ALL_WHEEL_DRIVE")
    assert(Dictionaries.gearTypes.byCode("ALL_WHEEL_DRIVE").value.code == "ALL_WHEEL_DRIVE")

    assert(Dictionaries.gearTypes.byCode("ALL_PART").value.id.value == "183")
    assert(Dictionaries.gearTypes.byCode("ALL_FULL").value.id.value == "182")
    assert(Dictionaries.gearTypes.byCode("ALL").value.id.value == "1074")
  }

  test("new gear_type FORWARD_CONTROL") {
    assert(Dictionaries.gearTypes.byId("2").value.code == "FORWARD_CONTROL")
    assert(Dictionaries.gearTypes.byId("180").value.code == "FORWARD_CONTROL")

    assert(Dictionaries.gearTypes.byCode("FRONT").value.code == "FORWARD_CONTROL")
    assert(Dictionaries.gearTypes.byCode("FORWARD_CONTROL").value.code == "FORWARD_CONTROL")
  }

  test("new gear_type REAR_DRIVE") {
    assert(Dictionaries.gearTypes.byId("1").value.code == "REAR_DRIVE")
    assert(Dictionaries.gearTypes.byId("181").value.code == "REAR_DRIVE")

    assert(Dictionaries.gearTypes.byCode("REAR").value.code == "REAR_DRIVE")
    assert(Dictionaries.gearTypes.byCode("REAR_DRIVE").value.code == "REAR_DRIVE")
  }

  test("PP -> AUTOMATIC") {
    assert(Dictionaries.transmissions.byId("1418").value.code == "AUTOMATIC")
    assert(Dictionaries.transmissions.byCode("PP").value.id.value == "1418")
    assert(Dictionaries.transmissions.byCode("PP").value.code == "AUTOMATIC")

    assert(Dictionaries.transmissions.byId("1414").value.code == "AUTOMATIC")
    assert(Dictionaries.transmissions.byCode("AUTOMATIC").value.id.value == "1414")
    assert(Dictionaries.transmissions.byCode("AUTOMATIC").value.code == "AUTOMATIC")
  }

  test("new transmission ROBOT") {
    assert(Dictionaries.transmissions.byId("1415").value.code == "ROBOT")
    assert(Dictionaries.transmissions.byId("1416").value.code == "ROBOT")
    assert(Dictionaries.transmissions.byId("1419").value.code == "ROBOT")
    assert(Dictionaries.transmissions.byId("1429").value.code == "ROBOT")

    assert(Dictionaries.transmissions.byCode("ROBOT_2CLUTCH").value.code == "ROBOT")
    assert(Dictionaries.transmissions.byCode("ROBOT_1CLUTCH").value.code == "ROBOT")
    assert(Dictionaries.transmissions.byCode("ROBOT_SEQ").value.code == "ROBOT")
    assert(Dictionaries.transmissions.byCode("ROBOT").value.code == "ROBOT")
  }

  test("condition EXCELLENT") {
    assert(Dictionaries.conditions.byCode("EXCELLENT").nonEmpty)
  }

  test("engine feeding 164") {
    assert(Dictionaries.engineFeedings.byCode("Carburetor").value.id.value == "1482")
    assert(Dictionaries.engineFeedings.byId("164").value.code == "Carburetor")
    assert(Dictionaries.engineFeedings.byId("1482").value.code == "Carburetor")
  }

  test("equipments") {
    val term = Dictionaries.equipments.byCode("12-inch-wheels")
    assert(term.nonEmpty)
    assert(term.value.id.value == "11513841")
  }

  test("equipments trip-computer") {

    val term1 = Dictionaries.equipments.byCode("trip-computer")
    val term2 = Dictionaries.equipments.byCode("computer")

    assert(term1.value.id.value == "8882742")
    assert(term1.value.id.value == term2.value.id.value)
  }

  test("equipments engine-start") {

    val term1 = Dictionaries.equipments.byCode("engine-start")
    val term2 = Dictionaries.equipments.byCode("remote-engine-start")

    assert(term1.value.id.value == "11634297")
    assert(term1.value.id.value == term2.value.id.value)
  }
}
