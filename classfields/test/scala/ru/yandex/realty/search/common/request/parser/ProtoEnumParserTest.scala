package ru.yandex.realty.search.common.request.parser

import org.scalatest.FunSuite
import ru.yandex.realty.building.model.BuildingEpoch

class ProtoEnumParserTest extends FunSuite {

  test("testParse") {
    val parser = ProtoEnumParser.getInstanceForType(classOf[BuildingEpoch])
    assert(parser.parse("BUILDING_EPOCH_KHRUSHCHEV") == BuildingEpoch.BUILDING_EPOCH_KHRUSHCHEV)
    assert(parser.parse("KHRUSHCHEV") == BuildingEpoch.BUILDING_EPOCH_KHRUSHCHEV)
    assert(parser.parse("BREZHNEV") == BuildingEpoch.BUILDING_EPOCH_BREZHNEV)
  }

  test("testCache") {
    val parser = ProtoEnumParser.getInstanceForType(classOf[BuildingEpoch])
    assert(parser != null)
    assert(parser == ProtoEnumParser.getInstanceForType(classOf[BuildingEpoch]))
  }
}
