package ru.yandex.vertis.subscriptions.core.matcher.qbd

import DSL._
import org.junit.runner.RunWith
import org.scalacheck.Test.Parameters
import org.scalacheck.{Gen, Prop}
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.Checkers
import org.scalatest.{Matchers, WordSpec}
import Query.Builder._

/** Specs on [[ru.yandex.vertis.subscriptions.core.matcher.qbd.CandidatesTree]]
  */
@RunWith(classOf[JUnitRunner])
class CandidatesTreeSpec extends WordSpec with Matchers with Checkers {

  val priorities = List("key", "key1", "key2", "key3", "key4", "price", "mark", "model", "transmission", "rid")

  val tree = CandidatesTree[String](priorities)

  "CandidatesTree" should {
    "be empty before add any query" in {
      tree.valuesCount should be(0)
    }

    "be non-empty after add some query" in {
      val newTree = tree.add(term(point("key", "value")), "foo")
      newTree.valuesCount should be(1)
    }

    "be correct remove binding" in {
      val query = term(point("key", "value"))
      val newTree = tree.add(query, "foo")
      newTree.valuesCount should be(1)
      newTree.remove(query, "foo") should be(empty)
    }

    "find nothing in empty tree" in {
      tree.find(point("key", "value")) should be(empty)
    }

    "find nothing by empty terms" in {
      val p = point("key", "value")
      val newTree = tree.add(term(p), "foo")
      newTree.find() should be(empty)
    }

    "find single point query" in {
      val p = point("key", "value")
      val newTree = tree.add(term(p), "foo")
      newTree.find(p) should be(Set("foo"))
    }

    "find multi point query" in {
      val newTree = tree.add(term(points("key", "value1", "value2", "value3")), "foo")

      newTree.find(point("key", "value2")) should be(Set("foo"))
      newTree.find(point("key", "not-exists-value")) should be(empty)
    }

    "find mixed single and multi point" in {
      val newTree =
        tree.add(term(points("key", "value1", "value2", "value2")), "foo").add(term(point("key", "value")), "bar")

      newTree.find(point("key", "value2")) should be(Set("foo"))
      newTree.find(point("key", "value")) should be(Set("bar"))
    }

    "correct handle OR queries with different terms" in {
      val newTree = tree.add(
        or(
          term(point("key1", "value1")),
          term(point("key2", "value2")),
          term(point("key3", "value3")),
          term(point("key4", "value4"))
        ),
        "foo"
      )
      newTree.valuesCount should be(4)

      newTree.find(point("key1", "value1")) should be(Set("foo"))
      newTree.find(point("key2", "value2")) should be(Set("foo"))
    }

    "correct handle OR queries with same terms" in {
      val newTree = tree.add(
        or(
          term(point("key", "value1")),
          term(point("key", "value2")),
          term(point("key", "value3"))
        ),
        "foo"
      )

      newTree.valuesCount should be(3)
      newTree.find(point("key", "value1")) should be(Set("foo"))
      newTree.find(point("key", "value2")) should be(Set("foo"))
      newTree.find(point("key", "value3")) should be(Set("foo"))
      newTree.find(point("key", "not-exists")) should be(empty)
      newTree.find(point("not-exists", "value1")) should be(empty)
    }

    "correct handle AND queries" in {
      val newTree = tree.add(
        and(
          term(point("key1", "value1")),
          term(point("key2", "value2")),
          term(point("key3", "value3")),
          term(point("key4", "value4"))
        ),
        "foo"
      )
      newTree.valuesCount should be(1)

      newTree.find(point("key1", "value1")) should be(empty)
      newTree.find(point("key2", "value2")) should be(empty)
      newTree.find(point("key1", "value2")) should be(empty)

      newTree.find(
        point("key1", "value1"),
        point("key2", "value2"),
        point("key3", "value3")
      ) should be(empty)

      newTree.find(
        point("key1", "value1"),
        point("key2", "value2"),
        point("key3", "value3"),
        point("key4", "value4")
      ) should be(Set("foo"))
    }

    "correct handle range queries" in {
      val newTree = tree.add(
        term(range("price", 100, 200)),
        "foo"
      )
      newTree.find(point("price", "150")) should be(Set("foo"))
      newTree.find(openRangeFrom("price", 150)) should be(Set("foo"))
      newTree.find(openRangeFrom("price", 300)) should be(empty)
      newTree.find(openRangeTo("price", 99)) should be(empty)
      newTree.find(openRangeTo("price", 300)) should be(Set("foo"))
      newTree.find(points("price", "50", "150")) should be(Set("foo"))
    }

    "handle sly query" in {
      val newTree = tree.add(
        and(
          term(point("mark", "oU")),
          or(
            term(point("key", "u")),
            term(range("price", 2007, 2007))
          )
        ),
        "foo"
      )

      newTree.find(
        range("price", 2007, 2007),
        point("mark", "oU"),
        point("key", "u")
      ) should be(Set("foo"))

      newTree.valuesCount should be(2)
    }

    "find all items" in {
      val p1 = point("key1", "value1")
      val p2 = point("key2", "value2")
      val p3 = point("key3", "value3")
      var newTree = tree
      newTree = newTree.add(and(term(p1), term(p2), term(p3)), "foo")
      newTree = newTree.add(and(term(p2), term(p3)), "bar")
      newTree = newTree.add(term(p3), "baz")

      newTree.valuesCount should be(3)
      newTree.find(p1, p2, p3) should be(Set("foo", "bar", "baz"))
      newTree.find(p2, p3) should be(Set("bar", "baz"))

      val callback = new CollectorCallback[String]
      newTree.find(Iterable(p1, p2, p3), callback)
      callback.getCollected.toSeq.sorted should be(Seq("bar", "baz", "foo"))
    }

    "handle combine query" in {
      val newTree = tree
        .add(
          and(
            or(
              and(term(point("mark", "FORD")), term(point("model", "FOCUS"))),
              and(term(point("mark", "AUDI")), term(point("model", "A5")))
            ),
            term(points("rid", "213", "2", "10000")),
            or(
              term(point("transmission", "ROBOT")),
              term(point("transmission", "AUTO"))
            )
          ),
          "foo"
        )
        .add(
          and(
            or(
              and(term(point("mark", "FORD")), term(point("model", "MONDEO"))),
              and(term(point("mark", "AUDI")), term(point("model", "A5")))
            ),
            term(point("rid", "213")),
            or(
              term(point("transmission", "MT")),
              term(point("transmission", "AUTO"))
            )
          ),
          "bar"
        )

      newTree.valuesCount should be(16)

      newTree.find(
        point("rid", "1"),
        point("rid", "3"),
        point("rid", "4"),
        point("rid", "2"),
        point("mark", "FORD"),
        point("model", "FOCUS"),
        point("transmission", "ROBOT")
      ) should be(Set("foo"))

      newTree.find(
        point("rid", "2"),
        point("mark", "FORD"),
        point("model", "FOCUS"),
        point("transmission", "MT")
      ) should be(empty)
    }

    "find candidate in real world example" in {
      val q1 = and(
        term(point("all_region_codes", "1")),
        term(point("state", "USED"))
      )
      val q2 = and(
        or(
          term(point("all_region_codes", "1")),
          term(point("all_region_codes", "10832"))
        ),
        or(
          term(point("state", "USED")),
          term(point("state", "NEW"))
        )
      )
      val newTree = CandidatesTree[String](List("all_region_codes", "state"))
        .add(q1, "bar")
        .add(q2, "foo")

      val terms = List(
        point("all_region_codes", "213"),
        point("all_region_codes", "1"),
        point("all_region_codes", "3"),
        point("all_region_codes", "225"),
        point("state", "USED")
      )

      newTree.find(terms) should be(Set("foo", "bar"))

      newTree.remove(q1, "bar").find(terms) should be(Set("foo"))
      newTree.remove(q2, "foo").find(terms) should be(Set("bar"))

      newTree.path(_ == "foo") should be(Some(Path(Seq(point("all_region_codes", "1"), point("state", "USED")))))
    }

    "found all added queries" in {
      import Generators.genQuery
      import Generators.genTerms

      val genName: Gen[String] = Gen.oneOf(Gen.oneOf(priorities), Gen.oneOf("foo", "bar", "baz"))

      var i = 0
      var newTree = tree

      check(
        Prop.forAll(Gen.resize(3, genTerms(genName))) { terms =>
          Prop.forAll(genQuery(terms, 3)) { query =>
            i += 1
            val value = i.toString
            newTree = newTree.add(query, value)
            val usedTerms = query.terms
            usedTerms.isEmpty || newTree.find(usedTerms).contains(value)
          }
        },
        Parameters.defaultVerbose.withMinSuccessfulTests(300)
      )
    }
  }

}
