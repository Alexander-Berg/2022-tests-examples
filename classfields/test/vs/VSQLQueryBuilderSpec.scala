package ru.yandex.auto.search.api.test.vs

import ru.yandex.auto.search.api.vs.{Eq, VSQLQuery}
import zio.test.environment.TestEnvironment
import zio.test.{assertTrue, DefaultRunnableSpec, ZSpec}

object VSQLQueryBuilderSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    test("build query without condition") {
      val query = VSQLQuery.buildSelect(VSQLQuery.ALL_FIELDS, domain = "auto", condition = None)

      assertTrue(query.exists(_ == "SELECT * FROM auto"))
    }

    test("build query with pk= condition") {
      val c = Eq("PK", 1)
      val query = VSQLQuery.buildSelect(VSQLQuery.ALL_FIELDS, domain = "auto", Some(c))
      assertTrue(query.exists(_ == "SELECT * FROM auto WHERE PK = 1"))
    }

    test("wrap condition value in quotes") {
      val c = Eq("PK", "1-abc")
      val query = VSQLQuery.buildSelect(VSQLQuery.ALL_FIELDS, domain = "auto", Some(c))
      assertTrue(query.exists(_ == "SELECT * FROM auto WHERE PK = \"1-abc\""))
    }

    test("should return error if passed empty fields") {
      val query = VSQLQuery.buildSelect(fields = Seq(), domain = "auto", condition = None)
      assertTrue(query.isLeft)
    }

    test("should return error if passed empty domain") {
      val query = VSQLQuery.buildSelect(VSQLQuery.ALL_FIELDS, domain = "", condition = None)
      assertTrue(query.isLeft)
    }

  }
}
