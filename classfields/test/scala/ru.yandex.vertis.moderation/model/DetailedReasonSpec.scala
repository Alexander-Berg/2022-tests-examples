package ru.yandex.vertis.moderation.model

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.proto.Model.Reason

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class DetailedReasonSpec extends SpecBase {

  "DetailedReason.fromReason" should {

    case class TestCase(reason: Reason)

    val testCases: Seq[TestCase] = Reason.values.map(TestCase.apply)

    testCases.foreach { case TestCase(reason) =>
      s"return correct result for reason=$reason" in {
        val detailedReason = DetailedReason.fromReason(reason)
        detailedReason.reason shouldBe reason
      }
    }

    s"return number of detailed reasons equal to number of reasons" in {
      val allReasons = Reason.values.toSet
      val allDetailedReasons = allReasons.map(DetailedReason.fromReason)
      allReasons.size shouldBe allDetailedReasons.size
    }
  }
}
