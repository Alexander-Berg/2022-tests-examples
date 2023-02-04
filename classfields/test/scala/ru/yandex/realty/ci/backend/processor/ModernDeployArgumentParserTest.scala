package ru.yandex.realty.ci.backend.processor

import org.scalatest.FunSuite
import ru.yandex.realty.ci.api.RealtyCiApi.DeployParams
import ru.yandex.realty.ci.backend.processor.ModernDeployArgumentParser.parse

class ModernDeployArgumentParserTest extends FunSuite {
  val paramsBuilder = DeployParams.newBuilder()

  test("get error") {
    val args = List("-i", "REALTYBACK-5271")
    assertThrows[IllegalArgumentException] {
      parse(args, paramsBuilder)
    }
  }

  test("parse single service") {
    val args = List("-i", "REALTYBACK-5271", "realty3-api")
    val params = parse(args, paramsBuilder)
    assert(params.getServiceNameCount == 1)
  }

  test("parse multiple services") {
    val args = List("-i", "REALTYBACK-5271", "realty3-api", "realty-rent-api")
    val params = parse(args, paramsBuilder)
    assert(params.getServiceNameCount == 2)
  }
}
