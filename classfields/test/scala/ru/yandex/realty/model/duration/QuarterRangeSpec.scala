package ru.yandex.realty.model.duration

import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.time.QuarterUnit

@RunWith(classOf[JUnitRunner])
class QuarterRangeSpec extends SpecBase with Matchers {

  "FinishQuarterRange" should {
    "allow constructing an empty one (left = right, span is empty)" in {
      val fq = QuarterUnit(2017, 4)
      val fqr = QuarterRange(fq, fq)
      fqr.span should be(0)
    }

    "reject constructing an invalid one (left > right)" in {
      val fq1 = QuarterUnit(2017, 4)
      val fq2 = QuarterUnit(2018, 1)
      fq1 should be < fq2

      val valid = QuarterRange(fq1, fq2)
      valid.span should be(1)

      an[IllegalArgumentException] should be thrownBy QuarterRange(fq2, fq1)
    }

    "not contain any FinishQuarter less than the left bound (inclusive)" in {
      val fq1 = QuarterUnit(2016, 1)
      val fq2 = QuarterUnit(2017, 1)
      fq1 should be < fq2

      val fqr = QuarterRange(fq1, fq2)
      fqr should contain(fq1)
      fqr should not contain (fq1 - 1)
    }

    "not contain any FinishQuarter no less than the right bound (exclusive)" in {
      val fq1 = QuarterUnit(2016, 1)
      val fq2 = QuarterUnit(2017, 1)
      fq1 should be < fq2

      val fqr = QuarterRange(fq1, fq2)
      fqr should contain(fq2 - 1)
      fqr should not contain fq2
    }

    "allow for partial comparisons (precedes)" in {
      val finished = QuarterUnit(2017, 4)
      val offer = QuarterUnit(2018, 1)

      val verbaFqr = QuarterRange(finished - 1, finished + 1)
      val offerFqr = QuarterRange(offer - 1, offer + 1)

      verbaFqr.precedes(offerFqr) should be(false)
      offerFqr.precedes(verbaFqr) should be(false)
      verbaFqr.intersects(offerFqr) should be(true)
      offerFqr.intersects(verbaFqr) should be(true)

      val q1_q3_2016 = QuarterRange(QuarterUnit(2016, 1), QuarterUnit(2016, 3) + 1)
      val q2_q4_2017 = QuarterRange(QuarterUnit(2017, 2), QuarterUnit(2017, 4) + 1)
      q2_q4_2017.precedes(q1_q3_2016) should be(false)
      q1_q3_2016.precedes(q2_q4_2017) should be(true)
      q2_q4_2017.intersects(q1_q3_2016) should be(false)
      q1_q3_2016.intersects(q2_q4_2017) should be(false)
    }

    "provide the intersects(that: FinishQuarterRange) method correct" when {
      val case1this = QuarterRange(QuarterUnit(2015, 1), QuarterUnit(2018, 1))
      val case1that = QuarterRange(QuarterUnit(2016, 2), QuarterUnit(2017, 1))

      "'this' is wider than 'that' ('that' is contained in 'this')" in {
        case1this.span should be > case1that.span
        case1this.intersects(case1that) should be(true)
      }

      "'that' is wider than 'this' ('this' is contained in 'that')" in {
        val case2this = case1that
        val case2that = case1this
        case2this.span should be < case2that.span
        case2this.intersects(case2that) should be(true)
      }

      val case3this = QuarterRange(QuarterUnit(2012, 2), QuarterUnit(2013, 3))
      val case3that = QuarterRange(QuarterUnit(2013, 3), QuarterUnit(2014, 3))

      "'this' is leftward to 'that' (adjacent, no intersection)" in {
        case3this.precedes(case3that) should be(true)
        case3that.precedes(case3this) should be(false)
        case3this.intersects(case3that) should be(false)
      }

      "'that' is leftward to 'this' (adjacent, no intersection)" in {
        val case4this = case3that
        val case4that = case3this
        case4this.precedes(case4that) should be(false)
        case4that.precedes(case4this) should be(true)
        case4this.intersects(case4that) should be(false)
      }

      "'this' coincides with 'that' (equals-wise, not the same instance)" in {
        val case5this = QuarterRange(QuarterUnit(2016, 2), QuarterUnit(2017, 1))
        val case5that = QuarterRange(QuarterUnit(2016, 2), QuarterUnit(2017, 1))
        case5this.precedes(case5that) should be(false)
        case5that.precedes(case5this) should be(false)
        case5this.intersects(case5that) should be(true)
        case5that.intersects(case5this) should be(true)
      }

      val case6this = QuarterRange(QuarterUnit(2012, 2), QuarterUnit(2013, 1))
      val case6that = QuarterRange(QuarterUnit(2013, 4), QuarterUnit(2014, 3))

      "'this' is leftward to 'that' (a hole exists in between, no intersection)" in {
        case6this.precedes(case6that) should be(true)
        case6that.precedes(case6this) should be(false)
        case6this.intersects(case6that) should be(false)
      }

      "'that' is leftward to 'this' (a hole exists in between, no intersection)" in {
        val case7this = case6that
        val case7that = case6this
        case7this.precedes(case7that) should be(false)
        case7that.precedes(case7this) should be(true)
        case7this.intersects(case7that) should be(false)
      }
    }
  }

}
