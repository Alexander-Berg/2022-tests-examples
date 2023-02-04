package ru.yandex.vertis.moderation.searcher.core.saas.clauses.transform

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.searcher.core.saas.clauses.ops._
import ru.yandex.vertis.moderation.searcher.core.saas.clauses._
import ru.yandex.vertis.moderation.searcher.core.saas.document.{AutoruFields, CommonFields}

@RunWith(classOf[JUnitRunner])
class UberTransformerSpec extends SpecBase {

  private val updateDateMin = CommonFields.UpdateDate >= 1000
  private val updateDateMax = CommonFields.UpdateDate >= 9999

  private val createDateMin = CommonFields.CreateDate >= 1000
  private val createDateMax = CommonFields.CreateDate <= 9999

  private val reason1 = CommonFields.Reason === "reason1"
  private val reason2 = CommonFields.Reason === "reason2"

  private val visibility1 = CommonFields.ContextVisibility === "VISIBLE"
  private val visibility2 = CommonFields.ContextVisibility === "DELETED"

  private val provenOwner1 = AutoruFields.ProvenOwnerVerdict === "PROVEN"
  private val provenOwner2 = AutoruFields.ProvenOwnerVerdict === "FAILED"

  "UberTransformer" should {
    "case 1" in {
      val src =
        and(
          updateDateMin,
          updateDateMax,
          or(provenOwner1, provenOwner2),
          or(visibility1, visibility2),
          and(reason1, reason2)
        )

      val actual = UberTransformer(src).asInstanceOf[LogicalClause]

      actual.clauses should contain allElementsOf Iterable(updateDateMin, updateDateMax, reason1, reason2)
      actual.clauses should contain(or(visibility1, visibility2))
      actual.clauses should contain(or(provenOwner1, provenOwner2)) // TODO merge with previous OR
    }

    "case 2" in {
      val src =
        and(
          updateDateMin,
          updateDateMax,
          !provenOwner1,
          or(visibility1, visibility2),
          and(reason1, reason2)
        )

      val actual = UberTransformer(src).asInstanceOf[AndNotClause]

      val leftAnd = actual.left.asInstanceOf[AndClause]
      leftAnd.clauses should contain allElementsOf Iterable(updateDateMin, updateDateMax, reason1, reason2)
      leftAnd.clauses should contain(or(visibility1, visibility2))
      actual.right shouldBe provenOwner1
    }

    "case 3" in {
      val src = or(!provenOwner1, provenOwner2)

      val actual = UberTransformer(src).asInstanceOf[OrClause]
      actual shouldBe OrClause(Set(AndNotClause(SelectAll, provenOwner1), provenOwner2))
    }

    "case 4" in {
      val src = and(createDateMin, createDateMax, !provenOwner1, provenOwner2)
      val actual = UberTransformer(src).asInstanceOf[AndNotClause]
      actual shouldBe AndNotClause(and(createDateMin, createDateMax, provenOwner2), provenOwner1)
    }
  }

  private def or(clauses: Clause*): Clause = OrClause(clauses.toIterable)

  private def and(clauses: Clause*): Clause = AndClause(clauses.toIterable)
}
