package ru.yandex.vertis.clustering.utils.features

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.TableFor2
import org.scalatest.prop.Tables.Table
import ru.yandex.vertis.clustering.BaseSpec

@RunWith(classOf[JUnitRunner])
class FeatureHelpersTest extends BaseSpec {

  "FeatureHelpers" should {
    import ru.yandex.vertis.clustering.utils.features.FeatureHelpersTest._

    "cleanPhone" in {
      forAll(PhoneTestCases) { (input, expectedOutput) =>
        FeatureHelpers.cleanPhone(input) should equal(expectedOutput)
      }
    }
  }

}

object FeatureHelpersTest {

  val PhoneTestCases: TableFor2[String, String] = Table(
    ("input", "output"),
    ("+79200000000", "79200000000"),
    ("+7(920)000-00-00", "79200000000"),
    (" +7 920 000-00-00 ", "79200000000"),
    ("+89200000000", "79200000000"),
    ("+8(920)000-00-00", "79200000000"),
    (" +8 920 000-00-00 ", "79200000000"),
    ("89200000000", "79200000000"),
    ("8(920)000-00-00", "79200000000"),
    (" 8 920 000-00-00 ", "79200000000"),
    ("+1 (555) 13-14-15", "1555131415"),
    ("(this is not a phone-number)", "thisisnotaphonenumber")
  )
}
