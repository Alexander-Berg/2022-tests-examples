package ru.yandex.vertis.subscriptions.core.matcher.qbd

import DSL._
import Generators._
import Query.Builder._
import QueryNormalizerSpec._
import org.junit.runner.RunWith
import org.scalacheck.Test.Parameters
import org.scalacheck.{Gen, Prop}
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.Checkers
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vertis.subscriptions.core.matcher.qbd.QueryNormalizer.Var

/** Specs on [[ru.yandex.vertis.subscriptions.core.matcher.qbd.QueryNormalizer]]
  */
@RunWith(classOf[JUnitRunner])
class QueryNormalizerSpec extends WordSpec with Matchers with Checkers {

  "QueryNormalizer" should {

    "apply De'Morgan rules in simple case" in {
      val mark = point("mark", "FORD")
      val model = point("model", "FOCUS")
      val year = point("year", "2005")
      val query = NotQuery(and(term(mark), term(model), term(year)))
      QueryNormalizer.deMorgan(query) should be(
        or(NotQuery(term(mark)), NotQuery(term(model)), NotQuery(term(year)))
      )
    }

    "De'Morgan should produce equivalent formula without complex query negations" in {
      check(
        Prop.forAll(Gen.resize(4, genTerms)) { terms =>
          Prop.forAll(genQuery(terms, 4)) { query =>
            val deMorganed = QueryNormalizer.deMorgan(query)
            val equal = Evaluator.evaluate(query, terms) ==
              Evaluator.evaluate(deMorganed, terms)
            equal && negateOnlyAtTerms(deMorganed)
          }
        },
        Parameters.defaultVerbose
      )
    }

    "normalize single term query" in {
      val query = term(point("mark", "FORD"))
      QueryNormalizer.getDNF(query) should be(Set(Set(Var(point("mark", "FORD")))))
    }

    "normalize simple AND query" in {
      val mark = point("mark", "FORD")
      val model = point("model", "FOCUS")
      val query = and(term(mark), term(model))
      QueryNormalizer.getDNF(query) should be(Set(Set(Var(mark), Var(model))))
    }

    "normalize simple OR query" in {
      val mark1 = point("mark", "FORD")
      val mark2 = point("mark", "OPEL")
      val query = or(term(mark1), term(mark2))
      QueryNormalizer.getDNF(query) should be(Set(Set(Var(mark1)), Set(Var(mark2))))
    }

    "normalize combined query 1" in {
      val mark1 = point("mark", "FORD")
      val model1 = point("model", "FOCUS")
      val mark2 = point("mark", "NISSAN")
      val model2 = point("model", "ALMERA")
      val query = and(or(term(mark1), term(mark2)), or(term(model1), term(model2)))
      QueryNormalizer.getDNF(query) should be(
        Set(
          Set(Var(mark1), Var(model1)),
          Set(Var(mark2), Var(model2)),
          Set(Var(mark1), Var(model2)),
          Set(Var(mark2), Var(model1))
        )
      )
    }

    "normalize combined query 2" in {
      val mark1 = point("mark", "FORD")
      val model1 = point("model", "FOCUS")
      val mark2 = point("mark", "NISSAN")
      val model2 = point("model", "ALMERA")
      val query = or(and(term(mark1), term(model1)), and(term(mark2), term(model2)))
      QueryNormalizer.getDNF(query) should be(
        Set(
          Set(Var(mark1), Var(model1)),
          Set(Var(mark2), Var(model2))
        )
      )
    }

    "normalize strange queries2" in {
      val query = and(
        term(point("mark", "oU")),
        term(point("mark", "oU")),
        or(
          and(
            term(point("mark", "oU")),
            term(point("mark", "oU")),
            term(point("key", "u"))
          ),
          term(range("price", 2007, 2007))
        )
      )
      QueryNormalizer.getDNF(query) should not be (empty)
    }

    "not modify any DNF query" in {
      check(
        Prop.forAll(genDNF) { dnf =>
          {
            val normalized = QueryNormalizer.getDNF(getQuery(dnf))
            normalized == dnf
          }
        },
        Parameters.defaultVerbose
      )
    }

    "always produce equivalent DNF formula" in {
      check(
        Prop.forAll(Gen.resize(4, genTerms)) { terms =>
          Prop.forAll(genQuery(terms, 3)) { query =>
            val normalized = getQuery(QueryNormalizer.getDNF(query))
            Evaluator.evaluate(query, terms) ==
              Evaluator.evaluate(normalized, terms)
          }
        },
        Parameters.defaultVerbose
      )
    }
  }

}

object QueryNormalizerSpec {

  /** Creates [[ru.yandex.vertis.subscriptions.core.matcher.qbd.Query]]
    * that corresponds to given DNF */
  def getQuery(dnf: QueryNormalizer.DNF) = {
    val queries = for {
      disjunct <- dnf
      queries = for {
        variable <- disjunct
        t = term(variable.term)
        query = if (variable.negated) NotQuery(t) else t
      } yield query
      d = and(queries)
    } yield d
    or(queries)
  }

  def negateOnlyAtTerms(query: Query): Boolean = {
    query match {
      case TermQuery(_) => true
      case NotQuery(TermQuery(_)) => true
      case NotQuery(_) => false
      case AndQuery(queries) => queries.forall(q => negateOnlyAtTerms(q))
      case OrQuery(queries) => queries.forall(q => negateOnlyAtTerms(q))
    }
  }
}
