package ru.yandex.vertis.moderation.searcher.core.saas.clauses

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.searcher.core.saas.clauses.ops._
import ru.yandex.vertis.moderation.searcher.core.saas.document.CommonFields

@RunWith(classOf[JUnitRunner])
class ClauseInversionSpec extends SpecBase {
  private val eq = CommonFields.CreateDate === 0
  private val notEq = CommonFields.CreateDate =/= 0

  private val gt = CommonFields.CreateDate > 0
  private val gte = CommonFields.CreateDate >= 0
  private val lt = CommonFields.CreateDate < 0
  private val lte = CommonFields.CreateDate <= 0

  private case class TestCase(description: String, clause: Clause, expected: Clause)

  "ValueClause.not" should {
    val testCases =
      Seq(
        TestCase("inverts ===", eq, notEq),
        TestCase("inverts =/=", notEq, eq),
        TestCase("inverts >", gt, lte),
        TestCase("inverts >=", gte, lt),
        TestCase("inverts <", lt, gte),
        TestCase("inverts <=", lte, gt)
      )

    testCases.foreach { case TestCase(description, clause, expected) =>
      description in {
        clause.not shouldBe expected
      }
    }
  }

  "LogicalClause.not" should {
    val testCases =
      Seq(
        TestCase("inverts OR", eq || gt, !eq && !gt),
        TestCase("inverts AND", eq && gt, !eq || !gt),
        TestCase("inverts AND-NOT", AndNotClause(eq, gt), !eq || gt)
      )

    testCases.foreach { case TestCase(description, clause, expected) =>
      description in {
        clause.not shouldBe expected
      }
    }
  }
}
