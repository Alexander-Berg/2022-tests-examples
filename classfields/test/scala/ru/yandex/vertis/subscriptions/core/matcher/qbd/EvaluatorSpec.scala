package ru.yandex.vertis.subscriptions.core.matcher.qbd

import DSL._
import Query.Builder._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}

/** Specs on [[ru.yandex.vertis.subscriptions.core.matcher.qbd.Evaluator]].
  */
@RunWith(classOf[JUnitRunner])
class EvaluatorSpec extends WordSpec with Matchers {

  "Evaluator" should {
    "evaluate true on single point query" in {
      val p = point("mark", "FORD")
      val t = term(p)
      Evaluator.evaluate(t, p) should be(true)
    }

    "evaluate true on multi point query" in {
      val p = point("mark", "FORD")
      val t = term(points("mark", "AUDI", "TOYOTA", "FORD"))
      Evaluator.evaluate(t, p) should be(true)
    }

    "evaluate true on multiple points" in {
      val p1 = point("mark", "FORD")
      val p2 = point("mark", "AUDI")
      val t1 = term(p1)
      val t2 = term(p2)
      Evaluator.evaluate(t2, p1, p2) should be(true)
    }

    "not evaluate true on multi point query" in {
      val p = point("mark", "FORD")
      val t = term(points("mark", "AUDI", "TOYOTA"))
      Evaluator.evaluate(t, p) should be(false)
    }

    "evaluate range queries" in {
      val r = term(range("price", 200, 300))
      Evaluator.evaluate(r, point("price", "250")) should be(true)
      Evaluator.evaluate(r, point("price", "350")) should be(false)
      Evaluator.evaluate(r, point("price", "200")) should be(true)
      Evaluator.evaluate(r, point("key", "200")) should be(false)
      Evaluator.evaluate(r, range("price", 100, 250)) should be(true)
      Evaluator.evaluate(r, range("price", 40, 100)) should be(false)
      Evaluator.evaluate(r, range("key", 200, 300)) should be(false)
    }

    "evaluate correctly on complex query" in {
      val q = and(
        or(
          and(term(point("mark", "FORD")), term(point("model", "FOCUS"))),
          and(term(point("mark", "AUDI")), term(point("model", "A5")))
        ),
        term(openRangeTo("price", 300000)),
        term(points("rid", "213", "2", "10000")),
        or(
          term(point("transmission", "ROBOT")),
          term(point("transmission", "AUTO"))
        )
      )

      Evaluator.evaluate(
        q,
        point("mark", "FORD"),
        point("model", "FOCUS"),
        point("rid", "10000"),
        point("transmission", "ROBOT"),
        point("price", "250000")
      ) should be(true)

      Evaluator.evaluate(
        q,
        point("mark", "FORD"),
        point("model", "FOCUS"),
        point("rid", "213"),
        point("transmission", "AUTO"),
        range("price", 200000, 400000)
      ) should be(true)

      Evaluator.evaluate(
        q,
        point("mark", "FORD"),
        point("model", "A5"),
        point("rid", "10000"),
        point("transmission", "ROBOT"),
        point("price", "300000")
      ) should be(false)

      Evaluator.evaluate(
        q,
        point("mark", "FORD"),
        point("mark", "AUDI"),
        point("rid", "10000"),
        point("transmission", "ROBOT")
      ) should be(false)
    }

    "evaluate query with distances" in {
      val q = term(distance("foo", "bar", 10))
      Evaluator.evaluate(q, distance("foo", "bar", 5)) should be(true)
      Evaluator.evaluate(q, distance("foo", "bar", 10)) should be(true)
      Evaluator.evaluate(q, distance("foo", "bar", 15)) should be(false)
      Evaluator.evaluate(q, distance("foo", "baz", 1)) should be(false)
      Evaluator.evaluate(q, distance("aaz", "bar", 1)) should be(false)
    }

    "evaluate query with many distances" in {
      val q = or(term(distance("foot", "1", 10)), term(distance("foot", "2", 15)), term(distance("transport", "2", 5)))
      Evaluator.evaluate(q, distance("foot", "1", 5)) should be(true)
      Evaluator.evaluate(q, distance("foot", "1", 10)) should be(true)
      Evaluator.evaluate(q, distance("foot", "1", 15)) should be(false)
      Evaluator.evaluate(q, distance("transport", "1", 1)) should be(false)
      Evaluator.evaluate(q, distance("transport", "2", 3)) should be(true)
      Evaluator.evaluate(q, distance("transport", "2", 10)) should be(false)
      Evaluator.evaluate(q, distance("transport", "2", 10), distance("foot", "1", 5)) should be(true)
    }

    "evaluate real world example" in {
      val q = and(
        term(point("all_region_codes", "1")),
        term(point("cluster_head", "1")),
        term(point("is_picture", "1")),
        term(point("state", "USED")),
        term(point("custom_house_state", "CLEARED_BY_CUSTOMS"))
      )

      Evaluator.evaluate(
        q,
        point("acceleration", "9.1"),
        point("all_region_codes", "213"),
        point("all_region_codes", "1"),
        point("all_region_codes", "3"),
        point("all_region_codes", "225"),
        point("cluster_head", "1"),
        point("is_picture", "1"),
        point("state", "USED"),
        point("custom_house_state", "CLEARED_BY_CUSTOMS")
      ) should be(true)
    }
  }

}
