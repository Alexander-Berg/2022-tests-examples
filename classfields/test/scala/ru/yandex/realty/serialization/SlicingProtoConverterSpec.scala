package ru.yandex.realty.serialization

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.util.{Fragment, Range}

@RunWith(classOf[JUnitRunner])
class SlicingProtoConverterSpec extends SpecBase with PropertyChecks {

  "SlicingProtoConverter" when {
    "buildSlicing from Fragment" should {

      "maintain preconditions for reasonable case (offset + limit < total)" in {
        val total = 9
        val limit = 3
        val offset = 2
        require(offset + limit < total, "Testing [offset + limit < total] case")

        val result = SlicingProtoConverter.buildSlicing(Fragment(offset, limit), total)

        val slice = Fragment(result.getFragment.getOffset, result.getFragment.getLimit)
        val again = SlicingProtoConverter.buildSlicing(slice, total)

        again should be(result)
      }

      "maintain preconditions for case (offset + limit > total)" in {
        val total = 19
        val limit = 8
        val offset = 1 + total - limit
        require(offset + limit > total, "Testing [offset + limit > total] case")

        val result = SlicingProtoConverter.buildSlicing(Fragment(offset, limit), total)

        val slice = Fragment(result.getFragment.getOffset, result.getFragment.getLimit)
        val again = SlicingProtoConverter.buildSlicing(slice, total)

        again should be(result)
        again.getFragment.getOffset should be < total
        again.getFragment.getLimit should be >= 1

        again.getFragment.getOffset + again.getFragment.getLimit should be <= total
      }

      "maintain preconditions for degenerate case (offset > total)" in {
        val total = 7
        val limit = 3
        val offset = total + 2
        require(offset > total, "Testing [offset > total] case")

        val result = SlicingProtoConverter.buildSlicing(Fragment(offset, limit), total)

        val slice = Fragment(result.getFragment.getOffset, result.getFragment.getLimit)
        val again = SlicingProtoConverter.buildSlicing(slice, total)

        again should be(result)
        again.getFragment.getOffset should be > total
        again.getFragment.getLimit shouldBe (1)
      }

      "maintain preconditions for specific case encountered (total = 14, offset = 10, limit = 10)" in {
        val total = 14
        val limit = 10
        val offset = 10

        val result = SlicingProtoConverter.buildSlicing(Fragment(offset, limit), total)
        val expected = SlicingProtoConverter.buildSlicing(Fragment(offset, 4), total)

        result shouldBe (expected)
      }

    }

    "buildSlicing from Range" should {

      "maintain preconditions for reasonable case (from < total, from < to <= total + 1)" in {
        val total = 11
        val from = 2
        val to = 12
        require(from < total && from < to && to <= total + 1, "Testing [from < total, from < to <= total + 1] case")

        val result = SlicingProtoConverter.buildSlicing(Range(from, to), total)

        val slice = Range(result.getRange.getFrom, result.getRange.getTo)
        val again = SlicingProtoConverter.buildSlicing(slice, total)

        again should be(result)
      }

      "maintain preconditions for case (from < total, from < to > total + 1)" in {
        val total = 26
        val from = 7
        val to = 3 + total
        require(from < total && from < to && to > total + 1, "Testing [from < total, from < to > total + 1] case")

        val result = SlicingProtoConverter.buildSlicing(Range(from, to), total)

        val slice = Range(result.getRange.getFrom, result.getRange.getTo)
        val again = SlicingProtoConverter.buildSlicing(slice, total)

        again should be(result)
        again.getRange.getFrom should be < total
        again.getRange.getTo should be > again.getRange.getFrom
        again.getRange.getTo should be <= total
      }

      "maintain preconditions for degenerate case (from > total)" in {
        val total = 12
        val from = total + 2
        val to = from + 3
        require(from > total, "Testing [from > total] case")

        val result = SlicingProtoConverter.buildSlicing(Range(from, to), total)

        val slice = Range(result.getRange.getFrom, result.getRange.getTo)
        val again = SlicingProtoConverter.buildSlicing(slice, total)

        again should be(result)
        again.getRange.getFrom should be > total
        again.getRange.getTo should be > again.getRange.getFrom
      }

      "maintain preconditions for specific case encountered (total = 14, from = 10, to = 20)" in {
        val total = 14
        val from = 10
        val to = 20

        val result = SlicingProtoConverter.buildSlicing(Range(from, to), total)
        val expected = SlicingProtoConverter.buildSlicing(Range(from, 14), total)

        result shouldBe (expected)
      }

    }

  }

}
