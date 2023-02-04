package ru.auto.api.model.bunker.shark

import org.scalatest.funsuite.AnyFunSuite

import java.io.ByteArrayInputStream

class CalculatorStaticTest extends AnyFunSuite {
  test("parse CalculatorStatic from json") {
    val in = new ByteArrayInputStream("""[{
                                        |"fullName": "/auto_ru/shark/calculator_static",
                                        |"content": {
                                        |	"amountRange": {
                                        |		"from": 50000,
                                        |		"to": 7000000
                                        |	},
                                        |	"interestRateRange": {
                                        |		"from": 3.9,
                                        |		"to": 3.9
                                        |	},
                                        |	"termMonthsRange": {
                                        |		"from": 12,
                                        |		"to": 84
                                        |	},
                                        |	"minInitialFeeRate": 0
                                        |}
                                        |}]""".stripMargin.getBytes("UTF-8"))
    val actual = CalculatorStatic.parse(in)
    val calculatorStatic = CalculatorStatic(
      amountRange = AmountRange(from = 50000L, to = 7000000L),
      interestRateRange = InterestRateRange(from = 3.9f, to = 3.9f),
      termMonthsRange = TermMonthsRange(from = 12, to = 84),
      minInitialFeeRate = 0f
    )
    val expected = CalculatorStaticContainer(data = Some(calculatorStatic))
    assert(actual == expected)
  }

  test("parse CalculatorStatic from empty json") {
    val in = new ByteArrayInputStream("""[{
                                        |"fullName": "/auto_ru/shark/calculator_static",
                                        |"content": {}
                                        |}]""".stripMargin.getBytes("UTF-8"))
    val actual = CalculatorStatic.parse(in)
    val expected = CalculatorStaticContainer(data = None)
    assert(actual == expected)
  }
}
