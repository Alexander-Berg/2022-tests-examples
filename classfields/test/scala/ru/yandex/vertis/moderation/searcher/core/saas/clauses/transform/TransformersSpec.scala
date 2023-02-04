package ru.yandex.vertis.moderation.searcher.core.saas.clauses.transform

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.searcher.core.saas.clauses.ops._
import ru.yandex.vertis.moderation.searcher.core.saas.clauses._
import ru.yandex.vertis.moderation.searcher.core.saas.document.{AutoruFields, CommonFields}

@RunWith(classOf[JUnitRunner])
class TransformersSpec extends SpecBase {

  private val comparison1 = CommonFields.CreateDate > 0
  private val comparison2 = CommonFields.UpdateDate >= 0
  private val equality1 = CommonFields.Reason === "reason"
  private val equality2 = CommonFields.ContextVisibility === "VISIBLE"
  private val equality3 = CommonFields.SignalKey === "SignalKey"
  private val equality4 = CommonFields.MetaIsQuota === 0
  private val notEquality1 = CommonFields.ContextVisibility =/= "VISIBLE"

  private val createDateLowerBound = CommonFields.CreateDate >= 1234567
  private val createDateUpperBound = CommonFields.CreateDate <= 9876543

  "FlattenNested" should {
    "does not touch simple clauses" in {
      val actual = FlattenNested(comparison1)
      (actual should be).theSameInstanceAs(comparison1)
    }

    "does not touch nested ORs of enclosing AND" in {
      val src = comparison1 && (equality1 || equality2)
      val actual = FlattenNested(src)
      (actual should be).theSameInstanceAs(src)
    }

    "does not touch nested ANDs of enclosing OR" in {
      val src = comparison1 || (equality1 && equality2)
      val actual = FlattenNested(src)
      (actual should be).theSameInstanceAs(src)
    }

    "does not change flat AND-NOT clauses" in {
      val src = AndNotClause(comparison1, equality1)
      val actual = FlattenNested(src)
      actual shouldBe src
    }

    "flattens nested ANDs" in {
      val src = comparison1 && (equality1 && equality2)
      val actual = FlattenNested(src)
      actual shouldBe and(comparison1, equality1, equality2)
    }

    "flattens nested ORs" in {
      val src = comparison1 || (equality1 || equality2)
      val actual = FlattenNested(src)
      actual shouldBe or(comparison1, equality1, equality2)
    }

    "flattens one levels of nesting ANDs" in {
      val src = comparison1 && (equality1 && (equality2 && comparison2))
      val actual = FlattenNested(src)
      actual shouldBe and(comparison1, equality1, equality2 && comparison2)
    }

    "flattens one level of nesting ORs" in {
      val src = comparison1 || (equality1 || (equality2 || comparison2))
      val actual = FlattenNested(src)
      actual shouldBe or(comparison1, equality1, equality2 || comparison2)
    }

    "flattens clauses inside AND-NOT" in {
      val src = AndNotClause(comparison1 && equality1 && equality2, equality1 || equality2 || comparison2)
      val actual = FlattenNested(src)
      actual shouldBe AndNotClause(and(comparison1, equality1, equality2), or(equality1, equality2, comparison2))
    }
  }

  "RemoveDuplicates" should {
    "does not touch simple clauses" in {
      (RemoveDuplicates(comparison1) should be).theSameInstanceAs(comparison1)
    }

    "removes duplicates in AND" in {
      val src = and(comparison1, equality1, comparison1, equality2, equality1, comparison1)
      val actual = RemoveDuplicates(src)
      val cast = actual.asInstanceOf[LogicalClause]
      cast shouldBe an[AndClause]
      cast.clauses should contain theSameElementsAs Seq(comparison1, equality1, equality2)
    }

    "removes duplicates in OR" in {
      val src = or(comparison1, equality1, comparison1, equality2, equality1, comparison1)
      val actual = RemoveDuplicates(src)
      val cast = actual.asInstanceOf[LogicalClause]
      cast shouldBe an[OrClause]
      cast.clauses should contain theSameElementsAs Seq(comparison1, equality1, equality2)
    }
  }

  "LiftAndNot" should {
    "does not touch AND" in {
      LiftAndNot(comparison1 && equality1) shouldBe comparison1 && equality1
    }

    "does not touch OR" in {
      LiftAndNot(comparison1 && equality1) shouldBe comparison1 && equality1
    }

    "converts AND with inverted equality clauses into AND-NOT" in {
      val actual = LiftAndNot(and(equality1, !equality2, equality3, !equality4))
      actual shouldBe AndNotClause(equality1 && equality3, equality2 || equality4)
    }

    "replaces with AND-NOT al clauses with NotEq within OR" in {
      val actual = LiftAndNot(or(equality1, !equality2, equality3, !equality4))
      actual shouldBe or(equality1, AndNotClause(SelectAll, equality2), equality3, AndNotClause(SelectAll, equality4))
    }

    "converts single value clause with NotEq operator into AND-NOT" in {
      LiftAndNot(!equality1) shouldBe AndNotClause(SelectAll, equality1)
    }
  }

  "UnwrapLogical" should {
    "does not change AND with two more nested elements" in {
      val src = and(comparison1, equality1)
      UnwrapLogical(src) shouldBe src
    }

    "does not change OR with two more nested elements" in {
      val src = or(comparison1, equality1)
      UnwrapLogical(src) shouldBe src
    }

    "unwraps AND with single nested clause" in {
      UnwrapLogical(and(comparison1)) shouldBe comparison1
    }

    "unwraps OR with single nested clause" in {
      UnwrapLogical(or(comparison1)) shouldBe comparison1
    }
  }

  "FixInvertedSelectAll" should {
    "does not touch value clauses" in {
      (FixInvertedSelectAll(comparison1) should be).theSameInstanceAs(comparison1)
    }

    "does not change AND without sought clause" in {
      val src = and(equality1, comparison1 && equality2, comparison2)
      FixInvertedSelectAll(src) shouldBe src
    }

    "does not change OR without sought clause" in {
      val src = or(equality1, comparison1 || equality2, comparison2)
      FixInvertedSelectAll(src) shouldBe src
    }

    "does not change AND-NOT without sought clause" in {
      val src = AndNotClause(equality1, comparison1)
      FixInvertedSelectAll(src) shouldBe src
    }

    "fixes AND clause" in {
      val src = and(equality1, InvertedSelectAll, comparison2 && InvertedSelectAll)
      FixInvertedSelectAll(src) shouldBe and(equality1, SelectAll, comparison2 && SelectAll)
    }

    "fixes OR clause" in {
      val src = or(equality1, InvertedSelectAll, comparison2 || InvertedSelectAll)
      FixInvertedSelectAll(src) shouldBe or(equality1, SelectAll, comparison2 || SelectAll)
    }

    "fixes AND-NOT clause" in {
      val src = AndNotClause(equality1 && InvertedSelectAll, comparison2 || InvertedSelectAll)
      FixInvertedSelectAll(src) shouldBe AndNotClause(equality1 && SelectAll, comparison2 || SelectAll)
    }
  }

  "CollapseSelectAllClauses" should {
    "does not touch value clause" in {
      (CollapseSelectAllClauses(equality1) should be).theSameInstanceAs(equality1)
    }

    "does not change AND clause without SelectAll" in {
      val src = and(equality1, equality2, createDateLowerBound, createDateUpperBound)
      CollapseSelectAllClauses(src) shouldBe src
    }

    "does not change AND clause with SelectAll but without lower bound clause" in {
      val src = and(equality1, SelectAll, createDateUpperBound)
      CollapseSelectAllClauses(src) shouldBe src
    }

    "changes AND clause with SelectAll and with lower bound clause" in {
      val src = and(equality1, SelectAll, createDateLowerBound)
      val actual = CollapseSelectAllClauses(src).asInstanceOf[AndClause]
      actual.clauses should contain theSameElementsAs Seq(equality1, createDateLowerBound)
    }

    "does not change OR clause without SelectAll" in {
      val src = or(equality1, equality2, createDateLowerBound, createDateUpperBound)
      CollapseSelectAllClauses(src) shouldBe src
    }

    "does not change OR clause with SelectAll but without lower bound clause" in {
      val src = or(equality1, SelectAll, createDateUpperBound)
      CollapseSelectAllClauses(src) shouldBe src
    }

    "changes OR clause with SelectAll and with lower bound clause" in {
      val src = or(equality1, SelectAll, createDateLowerBound)
      val actual = CollapseSelectAllClauses(src).asInstanceOf[OrClause]
      actual.clauses should contain theSameElementsAs Seq(equality1, createDateLowerBound)
    }
  }

  "NormalizeTextTransformer" should {
    val normalizer = new NormalizeTextTransformer(Service.AUTORU, considerLatinAsCyrillic = false)

    val descAutoruEq1 = AutoruFields.Description === "Очень классная МАШИНКА на продажу!"
    val descAutoruEq2 = AutoruFields.Description === "Срочно покупай!!111"
    val descAutoruEq1Normalized = AutoruFields.Description === " очень классная машинка на продажу "
    val descAutoruEq2Normalized = AutoruFields.Description === " срочно покупай 111 "
    val descAutoruNotEq1 = AutoruFields.Description === "Не покупай, плиз"
    val descAutoruNotEq2 = AutoruFields.Description === "Не смей покупать?!"
    val descAutoruNotEq1Normalized = AutoruFields.Description === " не покупай плиз "
    val descAutoruNotEq2Normalized = AutoruFields.Description === " не смей покупать "

    "does not touch Value clause with Gte, Gt, Lte, Lt operator and AND, AND_NOT, OR clauses" in {
      val src = or(comparison1, and(comparison2, equality4))
      normalizer(src) shouldBe src
    }

    "does not touch Value clause with Eq operator and not 'actualZonedFields'" in {
      val src = equality2
      normalizer(src) shouldBe src
    }

    "does not touch Value with NotEq operator and not 'actualZonedFields'" in {
      val src = notEquality1
      normalizer(src) shouldBe src
    }

    "touch Value with Eq operator and 'actualZonedFields'" in {
      val src = and(descAutoruEq1, descAutoruEq2)
      normalizer(src) shouldBe and(descAutoruEq1Normalized, descAutoruEq2Normalized)
    }

    "touch Value with NotEq operator and 'actualZonedFields'" in {
      val src = or(descAutoruNotEq1, descAutoruNotEq2)
      normalizer(src) shouldBe or(descAutoruNotEq1Normalized, descAutoruNotEq2Normalized)
    }
  }

  private def or(clauses: Clause*): Clause = OrClause(clauses.toIterable)

  private def and(clauses: Clause*): Clause = AndClause(clauses.toIterable)
}
