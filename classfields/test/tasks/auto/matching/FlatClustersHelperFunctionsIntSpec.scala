package vertis.doppel.controller.tasks.auto.matching

import common.zio.doobie.logging.LogDoobieQueries.slf4jLogHandler
import doobie.implicits._
import doobie.util.fragment.Fragment
import org.scalatest.Assertion
import vertis.doppel.controller.tasks.auto.matching.FlatClustersHelperFunctions._
import vertis.doppel.controller.yql.Parts
import vertis.doppel.controller.yql.Parts.ColumnFragment
import vertis.doppel.controller.tasks.auto.matching.FlatClustersHelperFunctionsIntSpec._
import vertis.yql.container.RealYqlTest
import vertis.yt.zio.wrappers.YtZio
import vertis.zio.test.ZioSpecBase.TestBody
import vertis.zio.yql.YqlClient
import zio._

/** @author kusaeva
  */
class FlatClustersHelperFunctionsIntSpec extends RealYqlTest {

  "HelperFunctions.сloseNum" should {
    "return true if both are null" in testEnv {
      testCloseNum(None, None, 0.2, true)
    }
    "return true if one is null and other is zero" in testEnv {
      testCloseNum(Some(0), None, 0.2, true)
    }
    "return true if both are zero" in testEnv {
      testCloseNum(None, None, 0.2, true)
    }
    "return false if one is zero" in testEnv {
      testCloseNum(None, Some(100), 0.2, false)
    }
    "return true for close numbers" in testEnv {
      testCloseNum(Some(100), Some(110), 0.2, true)
    }
    "return false for different numbers" in testEnv {
      testCloseNum(Some(100), Some(150), 0.2, false)
    }
  }

  "HelperFunctions.vinValid" should {
    "return false for null value" in testEnv {
      testVinValid(None, false)
    }
    "return true when length > than min vin length" in testEnv {
      testVinValid(Some("QWERTYUIOP12345678"), true)
    }
    "return false when length < than specified" in testEnv {
      testVinValid(Some("QWERTYUIOP123456".take(vinLength)), false)
    }
    "return false for empty value" in testEnv {
      testVinValid(Some(""), false)
    }
    "return false if value contains '*'" in testEnv {
      testVinValid(Some("**ERTYUIOP1234567"), false)
    }
    "return true if value has correct length and doesn't contain '*'" in testEnv {
      testVinValid(Some("QWERTYUIOP1234567".take(vinLength + 1)), true)
    }
  }

  private def testCloseNum(
      num1: Option[Int],
      num2: Option[Int],
      percent: Double,
      expected: Boolean): TestEnv => ZIO[Any, Throwable, Assertion] = {

    def testColumn(v: Option[Int]): ColumnFragment =
      v.map(IntColumn)
        .getOrElse(NullColumn)

    testFunc(
      closeNum((testColumn(num1), testColumn(num2)), percent),
      expected
    )
  }

  private def testVinValid(vin: Option[String], expected: Boolean): TestEnv => ZIO[Any, Throwable, Assertion] = {

    def testColumn(v: Option[String]): ColumnFragment =
      v.map(StringColumn)
        .getOrElse(NullColumn)

    testFunc(
      vinValid(testColumn(vin)),
      expected
    )
  }

  private def testFunc(fragment: Fragment, expected: Boolean): TestEnv => Task[Assertion] = { env: TestEnv =>
    val query =
      sql"""|${Parts.combine(FlatClustersHelperFunctions.list: _*)}
            |SELECT $fragment;
            |""".stripMargin.query[Boolean].unique

    env.yql.executeQuery(query).map(_ shouldBe expected)
  }

  private def testEnv(f: TestEnv => TestBody): Unit = ioTest {
    val env = for {
      yt <- ytZio
      yql <- YqlClient.make(yqlConfig)
    } yield TestEnv(yt, yql)
    env.use { env =>
      f(env)
    }
  }
}

object FlatClustersHelperFunctionsIntSpec {

  case class StringColumn(value: String) extends ColumnFragment {
    def f: Fragment = fr0"$value"
  }

  case class IntColumn(value: Int) extends ColumnFragment {
    def f: Fragment = fr0"$value"
  }

  case object NullColumn extends ColumnFragment {
    def f: Fragment = fr0"NULL"
  }

  case class TestEnv(yt: YtZio, yql: YqlClient)

}
