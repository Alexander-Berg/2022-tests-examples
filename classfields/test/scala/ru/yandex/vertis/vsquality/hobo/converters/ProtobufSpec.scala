package ru.yandex.vertis.vsquality.hobo.converters

import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators._
import ru.yandex.vertis.vsquality.hobo.util.SpecBase
import org.scalacheck.Prop.forAll
import org.scalatestplus.scalacheck.Checkers.check
import scala.annotation.nowarn

/**
  * Specs on [[Protobuf]]
  *
  * @author semkagtn
  */
@nowarn("cat=deprecation")
class ProtobufSpec extends SpecBase {

  import Protobuf._

  "TaskSource conversion" should {

    "be correct" in {
      check(forAll(TaskSourceGen) { x =>
        x == fromMessage(toMessage(x))
      })
    }
  }

  "FullTask conversion" should {

    "be correct" in {
      check(forAll(FullTaskGen) { x =>
        x == fromMessage(toMessage(x))
      })
    }
  }

  "UserSource conversion" should {
    "be correct" in {
      check(forAll(UserSourceGen) { x =>
        x == fromMessage(toMessage(x))
      })
    }
  }

  "User conversion" should {

    "be correct" in {
      check(forAll(UserGen) { x =>
        x == fromMessage(toMessage(x))
      })
    }
  }

  "PhoneCall" should {

    "bre correct" in {
      check(forAll(PhoneCallGen) { x =>
        x == fromMessage(toMessage(x))
      })
    }
  }

  "SalaryStatistics" should {

    "be correct" in {
      check(forAll(SalaryStatisticsGen) { x =>
        x == fromMessage(toMessage(x))
      })
    }
  }

  "SummarySalaryStatistics" should {

    "be correct" in {
      check(forAll(SummarySalaryStatisticsGen) { x =>
        x == fromMessage(toMessage(x))
      })
    }
  }
}
