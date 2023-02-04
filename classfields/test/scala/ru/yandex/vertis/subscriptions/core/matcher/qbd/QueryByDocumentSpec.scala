package ru.yandex.vertis.subscriptions.core.matcher.qbd

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vertis.subscriptions.core.matcher.qbd.DSL._
import ru.yandex.vertis.subscriptions.core.matcher.qbd.Query.Builder._

/**
  * Specs on [[ru.yandex.vertis.subscriptions.core.matcher.qbd.QueryByDocument]]
  */
@RunWith(classOf[JUnitRunner])
class QueryByDocumentSpec extends WordSpec with Matchers {

  val qbd = new QueryByDocument[String](List("mark", "model", "rid", "transmission"))

  "QueryByDocument" should {
    "add and remove bindings" in {
      val query = and(
        or(
          and(term(point("mark", "FORD")), term(point("model", "FOCUS"))),
          and(term(point("mark", "AUDI")), term(point("model", "A5")))
        ),
        term(points("rid", "213", "2", "10000")),
        or(
          term(point("transmission", "ROBOT")),
          term(point("transmission", "AUTO"))
        )
      )
      val nonEmpty = qbd.add(query, "foo")

      nonEmpty.size should be(1)
      nonEmpty.remove(query, "bar").size should be(1)
      nonEmpty.remove(query, "foo").size should be(0)
    }

    "find query by documents" in {
      val newQbd = qbd
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

      newQbd.find(
        point("mark", "FORD"),
        point("model", "FOCUS"),
        point("rid", "213"),
        point("transmission", "AUTO")
      ) should be(Set("foo"))

      newQbd.find(
        point("mark", "FORD"),
        point("model", "FOCUS"),
        point("rid", "10000"),
        point("transmission", "ROBOT")
      ) should be(Set("foo"))

      newQbd.find(
        point("mark", "FORD"),
        point("model", "A5"),
        point("rid", "10000"),
        point("transmission", "ROBOT")
      ) should be(empty)

      newQbd.find(
        point("mark", "FORD"),
        point("model", "MONDEO"),
        point("rid", "10000"),
        point("transmission", "AUTO")
      ) should be(empty)

      newQbd.find(
        point("mark", "FORD"),
        point("model", "MONDEO"),
        point("rid", "10000"),
        point("transmission", "AUTO")
      ) should be(empty)
    }
  }
}
