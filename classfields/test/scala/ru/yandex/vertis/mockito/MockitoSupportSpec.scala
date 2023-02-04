package ru.yandex.vertis.mockito

import org.mockito.{Answers, Mockito}
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.mockito.MockitoSupportSpec.{DefaultInt, DefaultString, Param, TestTrait}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

/**
 * Runnable specs on [[MockitoSupport]]
 *
 * @author alex-kovalenko
 */
class MockitoSupportSpec
  extends Matchers
    with WordSpecLike
    with MockitoSupport {

  "MockitoSupport" should {
    "create mocks" in {
      val t1: TestTrait = mock[TestTrait]
      intercept[NotMockedException] {
        t1.unit(1)
      }

      val t2: TestTrait = mock[TestTrait](Answers.RETURNS_DEFAULTS)
      t2.int(1) shouldBe 0
      t2.multiParamDefault(1)() shouldBe null

      val t3: TestTrait =
        mock[TestTrait](
          Mockito.withSettings()
            .defaultAnswer(Answers.RETURNS_DEFAULTS))
      t3.int(1) shouldBe 0
    }

    "mock with Mockito.when style" when {
      val m = mock[TestTrait]
      "noArg method" in {
        when(m.noArg).thenThrow(new IllegalArgumentException)
        intercept[IllegalArgumentException] {
          m.noArg
        }
      }

      "one arg method" in {
        when(m.int(?)).thenReturn(1)
        when(m.int(4)).thenReturn(4)
        m.int(2) shouldBe 1
        m.int(4) shouldBe 4
      }

      "two args method" in {
        when(m.intString(?, ?)).thenReturn("")
        when(m.intString(eq(2), ?)).thenReturn("2")
        m.intString(1, "a") shouldBe ""
        m.intString(2, "b") shouldBe "2"
      }

      "object arg method" in {
        when(m.param(?)).thenReturn("")
        when(m.param(Param(2))).thenReturn("2")
        m.param(Param(1)) shouldBe ""
        m.param(Param(2)) shouldBe "2"
      }

      "method with one default argument" in {
        when(m.default(DefaultInt)).thenReturn(-1)
        when(m.default(1)).thenReturn(1)

        m.default(DefaultInt) shouldBe -1
        m.default(1) shouldBe 1
        intercept[NotMockedException] {
          m.default(2)
        }
        intercept[NotMockedException] {
          m.default()
        }
      }

      "method with two arguments one default" in {
        when(m.defaultOneOfTwo(?, ?)).thenReturn("? ?")
        when(m.defaultOneOfTwo(eq(1), ?)).thenReturn("1 ?")
        when(m.defaultOneOfTwo(?, eq(1))).thenReturn("? 1")
        when(m.defaultOneOfTwo(1, 1)).thenReturn("1")

        m.defaultOneOfTwo(0) shouldBe "? ?"
        m.defaultOneOfTwo(0, DefaultInt) shouldBe "? ?"
        m.defaultOneOfTwo(1) shouldBe "1 ?"
        m.defaultOneOfTwo(1, DefaultInt) shouldBe "1 ?"
        m.defaultOneOfTwo(0, 1) shouldBe "? 1"
        m.defaultOneOfTwo(1, 1) shouldBe "1"
      }

      "method with two default arguments" in {
        when(m.allDefault()).thenReturn("def def")
        m.allDefault() shouldBe "def def"
        intercept[NotMockedException] {
          m.allDefault(DefaultInt, DefaultInt)
        }

        when(m.allDefault(?, ?)).thenReturn("? ?")
        m.allDefault(DefaultInt, DefaultInt) shouldBe "? ?"
        m.allDefault(1, 1) shouldBe "? ?"

        when(m.allDefault()).thenReturn("def def")
        m.allDefault() shouldBe "def def"
        m.allDefault(1, 1) shouldBe "? ?"
      }

      "method with multiple parameter lists" in {
        when(m.multiIntString(?)(?)).thenReturn("")
        when(m.multiIntString(eq(1))(?)).thenReturn("1 ?")
        when(m.multiIntString(?)(eq("1"))).thenReturn("? 1")
        when(m.multiIntString(1)("1")).thenReturn("1")

        m.multiIntString(0)("0") shouldBe ""
        m.multiIntString(1)("0") shouldBe "1 ?"
        m.multiIntString(0)("1") shouldBe "? 1"
        m.multiIntString(1)("1") shouldBe "1"
      }

      "multi param with default" in {
        when(m.multiParamDefault(?)(?)).thenReturn("? ?")
        when(m.multiParamDefault(eq(1))(?)).thenReturn("1 ?")
        when(m.multiParamDefault(?)(eq("1"))).thenReturn("? 1")
        when(m.multiParamDefault(1)("1")).thenReturn("1")

        m.multiParamDefault(0)() shouldBe "? ?"
        m.multiParamDefault(0)(DefaultString) shouldBe "? ?"
        m.multiParamDefault(0)("a") shouldBe "? ?"
        m.multiParamDefault(1)() shouldBe "1 ?"
        m.multiParamDefault(1)("") shouldBe "1 ?"
        m.multiParamDefault(1)("a") shouldBe "1 ?"
        m.multiParamDefault(0)("1") shouldBe "? 1"
        m.multiParamDefault(1)("1") shouldBe "1"
      }

      "multi param implicit" in {
        implicit val p = Param(1)

        when(m.multiParamImplicit(?)(?)).thenReturn("? ?")
        when(m.multiParamImplicit(?)(eq(p))).thenReturn("? p")
        when(m.multiParamImplicit(eq(1))(?)).thenReturn("1 ?")
        when(m.multiParamImplicit(eq(1))(eq(p))).thenReturn("1 p")

        val p2 = Param(2)
        m.multiParamImplicit(0)(p2) shouldBe "? ?"
        m.multiParamImplicit(0) shouldBe "? p"
        m.multiParamImplicit(1)(p2) shouldBe "1 ?"
        m.multiParamImplicit(1) shouldBe "1 p"
      }

      "multi param with default implicit" in {
        when(m.multiParamDefaultImplicit(?)(?)).thenReturn("? ?")
        when(m.multiParamDefaultImplicit(?)(eq("a"))).thenReturn("? a")
        when(m.multiParamDefaultImplicit(eq(1))(?)).thenReturn("1 ?")
        when(m.multiParamDefaultImplicit(1)("a")).thenReturn("1 a")

        implicit val a = "a"
        val b = "b"
        m.multiParamDefaultImplicit(0)(b) shouldBe "? ?"
        m.multiParamDefaultImplicit(0) shouldBe "? a"
        m.multiParamDefaultImplicit(1)(b) shouldBe "1 ?"
        m.multiParamDefaultImplicit(1) shouldBe "1 a"
      }

      "method with one implicit" in {
        when(m.oneImplicit(?)).thenReturn(0)
        implicit val p = Param(1)
        m.oneImplicit shouldBe 0
      }

      "like real method" in {
        when(m.manyArguments(?, ?)(?, ?)).thenReturn(Success(()))

        import ExecutionContext.Implicits.global

        implicit val f: (Int, String) => Unit = (i, s) => ()

        m.manyArguments(1, "name") should matchPattern {
          case Success(()) =>
        }
      }
    }

    "stub with partial functions" when {
      val m = mock[TestTrait]
      "unit method" in {
        stub(m.unit _) {
          case i if i >= 0 => ()
          case _ => throw new IllegalArgumentException
        }

        m.unit(0)
        m.unit(1)
        intercept[IllegalArgumentException] {
          m.unit(-1)
        }
      }

      "no arg method" in {
        stub(m.noArg _)(1)
        m.noArg shouldBe 1
      }

      "one arg method" in {
        stub(m.int _) {
          case i if i >= 0 => i * i
        }

        m.int(2) shouldBe 4
        intercept[NotMockedException] {
          m.int(-1)
        }
      }

      "two args method" in {
        stub(m.intString _) {
          case (0, s) => s
          case (i, s) => s"$i: $s"
        }

        m.intString(0, "a") shouldBe "a"
        m.intString(1, "a") shouldBe "1: a"
      }

      "object arg method" in {
        stub(m.param _) {
          case p@Param(i) if i >= 0 => s"$p"
          case p => s"${p.copy(i = -p.i)}"
        }

        m.param(Param(1)) shouldBe "Param(1)"
        m.param(Param(-2)) shouldBe "Param(2)"
      }

      "method with one default argument" in {
        stub(m.default _) {
          case i => i * i
        }

        m.default() shouldBe 0
        m.default(2) shouldBe 4
      }

      "method with two arguments one default" in {
        stub(m.defaultOneOfTwo _) {
          case (i, j) => s"$i: $j"
        }

        m.defaultOneOfTwo(1) shouldBe "1: 0"
        m.defaultOneOfTwo(1, 2) shouldBe "1: 2"
      }

      "method with two default arguments" in {
        stub(m.allDefault _) {
          case (i, j) => s"$i: $j"
        }

        m.allDefault() shouldBe "0: 0"
        m.allDefault(i = 1) shouldBe "1: 0"
        m.allDefault(j = 1) shouldBe "0: 1"
        m.allDefault(1, 1) shouldBe "1: 1"
      }

      "method with multiple parameter lists" in {
        stub(m.multiIntString _) {
          case (i, s) => s"$i: $s"
        }

        m.multiIntString(1)("a") shouldBe "1: a"
      }

      "multi param with default" in {
        stub(m.multiParamDefault _) {
          case (i, s) => s"$i: $s"
        }
        m.multiParamDefault(1)() shouldBe "1: null"
        m.multiParamDefault(1)("a") shouldBe "1: a"
      }

      "multi param implicit" in {
        stub(m.multiParamImplicit(_: Int)(_: Param)) {
          case (i, p) => p.copy(i = i).toString
        }

        implicit val param = Param(1)
        m.multiParamImplicit(2) shouldBe "Param(2)"
        m.multiParamImplicit(3)(Param(-1)) shouldBe "Param(3)"
      }

      "multi param with default implicit" in {
        stub(m.multiParamDefaultImplicit(_: Int)(_: String)) {
          case (i, s) => s"$i: $s"
        }

        m.multiParamDefaultImplicit(1) shouldBe "1: null"

        implicit val a = "a"
        m.multiParamDefaultImplicit(1) shouldBe "1: a"
        m.multiParamDefaultImplicit(2)("b") shouldBe "2: b"
      }

      "method with one implicit" in {
        stub(m.oneImplicit(_: Param)) {
          case p => p.i
        }

        implicit val param = Param(2)
        m.oneImplicit shouldBe 2
      }

      "like real method" in {
        type Func = (Int, String) => Unit
        stub(m.manyArguments(_: Int, _:String)(_: ExecutionContext, _: Func)) {
          case (id, name, _, f) if id >= 0 => Success(f(id, name))
          case _ => Failure(new Exception("artificial"))
        }

        import ExecutionContext.Implicits.global
        implicit val fn: Func = (i, s) => ()
        m.manyArguments(1, "name1") should matchPattern {
          case Success(()) =>
        }
        m.manyArguments(-1, "name2") should matchPattern {
          case Failure(_) =>
        }
      }
    }

    "does not allow use default arguments" when {
      "create strict mock" in {
        val strictM = mockStrict[TestTrait]
        stub(strictM.default _) {
          case i => i * 2
        }

        intercept[NotMockedException] {
          strictM.default()
        }

        strictM.default(1) shouldBe 2
      }
    }
  }

}

object MockitoSupportSpec {
  case class Param(i: Int)

  val DefaultInt = -1
  val DefaultString = "empty"

  trait TestTrait {
    def unit(i: Int): Unit
    def noArg: Int
    def int(i: Int): Int
    def intString(i: Int, s: String): String
    def param(p: Param): String

    def default(i: Int = DefaultInt): Int
    def defaultOneOfTwo(i: Int, j: Int = DefaultInt): String
    def allDefault(i: Int = DefaultInt, j: Int = DefaultInt): String

    def multiIntString(i: Int)(s: String): String
    def multiParamDefault(i: Int)(s: String = DefaultString): String
    def multiParamImplicit(i: Int)(implicit p: Param): String
    def multiParamDefaultImplicit(i: Int)(implicit s: String = DefaultString): String

    def oneImplicit(implicit p: Param): Int

    def manyArguments(id: Int, name: String)
                     (implicit ec: ExecutionContext,
                       callback: (Int, String) => Unit): Try[Unit]

    def byNameParam(p: => Param): Int
  }
}
