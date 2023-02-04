package ru.yandex.vertis.moisha.impl.autoru_auction.v15

import ru.yandex.vertis.moisha.test.BaseSpec
import ru.yandex.vertis.ops.test.TestOperationalSupport

class CallDailyPolicyCalculatorSpec extends BaseSpec {

  private val moscow = 1
  private val spb = 10174
  private val other = 3333

  val calculator = new CallDailyPolicyCalculator(TestOperationalSupport)

  val usual = BigDecimal(1)
  val medium = BigDecimal(1.2)
  val high = BigDecimal(1.35)

  "v15.CallDailyPolicy" should {

    "return high cost ratio: AUDI, Moscow" in {
      calculator.getCallCostRatio(hasPriorityPlacement = true, moscow, offerMark = Some("AUDI"), dealerMarks = Set()) shouldBe high
    }

    "return high cost ratio by offer mark in priority, Moscow" in {
      val notHighCostMarks = Set("KIA", "HYUNDAI")
      calculator.getCallCostRatio(
        hasPriorityPlacement = true,
        moscow,
        offerMark = Some("AUDI"),
        dealerMarks = notHighCostMarks
      ) shouldBe high
    }

    "return high cost ratio by offer mark in priority, Spb" in {
      val notHighCostMarks = Set("KIA", "HYUNDAI")
      calculator.getCallCostRatio(
        hasPriorityPlacement = true,
        spb,
        offerMark = Some("JEEP"),
        dealerMarks = notHighCostMarks
      ) shouldBe high
    }

    "return high cost ratio by dealer mark, Moscow" in {
      val highCostMark = "MERCEDES"
      calculator.getCallCostRatio(
        hasPriorityPlacement = true,
        moscow,
        offerMark = None,
        dealerMarks = Set(highCostMark)
      ) shouldBe high
    }

    "return high cost ratio by dealer mark, Spb" in {
      val highCostMark = "MERCEDES"
      calculator.getCallCostRatio(hasPriorityPlacement = true, spb, offerMark = None, dealerMarks = Set(highCostMark)) shouldBe high
    }

    "return high cost ratio by most expensive dealer mark, Moscow" in {
      val lowCostMark = "JEEP"
      val highCostMark = "AUDI"
      calculator.getCallCostRatio(
        hasPriorityPlacement = true,
        moscow,
        offerMark = None,
        dealerMarks = Set(lowCostMark, highCostMark)
      ) shouldBe high
    }

    "return high cost ratio by most expensive dealer mark, Spb" in {
      val lowCostMark = "AUDI"
      val highCostMark = "CADILLAC"
      calculator.getCallCostRatio(
        hasPriorityPlacement = true,
        spb,
        offerMark = None,
        dealerMarks = Set(lowCostMark, highCostMark)
      ) shouldBe high
    }

    "return usual high priority ratio by offer mark, Moscow" in {
      val lowCostMark = "OPEL"
      calculator.getCallCostRatio(
        hasPriorityPlacement = true,
        moscow,
        offerMark = Some(lowCostMark),
        dealerMarks = Set()
      ) shouldBe medium
    }

    "return usual high priority ratio by offer mark, Spb" in {
      val lowCostMark = "BMW"
      calculator.getCallCostRatio(hasPriorityPlacement = true, spb, offerMark = Some(lowCostMark), dealerMarks = Set()) shouldBe medium
    }

    "return usual high priority ratio by dealer marks, Moscow" in {
      val lowCostMark = "OPEL"
      calculator.getCallCostRatio(hasPriorityPlacement = true, moscow, offerMark = None, dealerMarks = Set(lowCostMark)) shouldBe medium
    }

    "return usual high priority ratio by dealer marks, Spb" in {
      val lowCostMark = "BMW"
      calculator.getCallCostRatio(hasPriorityPlacement = true, spb, offerMark = None, dealerMarks = Set(lowCostMark)) shouldBe medium
    }

    "return 1.0 cost ratio without high priority, for expensive mark. By the offerMark in Moscow" in {
      calculator.getCallCostRatio(hasPriorityPlacement = false, moscow, offerMark = Some("AUDI"), dealerMarks = Set()) shouldBe usual
    }

    "return 1.0 cost ratio without high priority, for expensive mark. By the offerMark in Spb" in {
      calculator.getCallCostRatio(hasPriorityPlacement = false, spb, offerMark = Some("JEEP"), dealerMarks = Set()) shouldBe usual
    }

    "return 1.0 cost ratio without high priority, for expensive mark. By the dealerMarks in Moscow" in {
      calculator.getCallCostRatio(hasPriorityPlacement = false, moscow, offerMark = None, dealerMarks = Set("AUDI")) shouldBe usual
    }

    "return 1.0 cost ratio without high priority, for expensive mark. By the dealerMarks in Spb" in {
      calculator.getCallCostRatio(hasPriorityPlacement = false, spb, offerMark = None, dealerMarks = Set("JEEP")) shouldBe usual
    }

    "high priority, not in Moscow or Spb" in {
      calculator.getCallCostRatio(
        hasPriorityPlacement = true,
        other,
        offerMark = Some("AUDI"),
        dealerMarks = Set("JEEP")
      ) shouldBe medium
    }

    "usual priority, not in Moscow or Spb" in {
      calculator.getCallCostRatio(
        hasPriorityPlacement = false,
        other,
        offerMark = Some("AUDI"),
        dealerMarks = Set("JEEP")
      ) shouldBe usual
    }

    "usual priority, for empty offerMark and dealersMarks" in {
      calculator.getCallCostRatio(
        hasPriorityPlacement = false,
        other,
        offerMark = None,
        dealerMarks = Set()
      ) shouldBe usual
    }

    "correctly round money to upper" in {
      calculator.toUpper(BigDecimal(1000.1)) shouldBe 1100
    }

    "stay exact" in {
      calculator.toUpper(BigDecimal(900)) shouldBe 900
    }

  }

}
