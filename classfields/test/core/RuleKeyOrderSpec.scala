package ru.yandex.vertis.billing.howmuch.model.core

import billing.common_model.Project.{AUTORU, REALTY}
import ru.yandex.vertis.billing.howmuch.model.core.criteria.{Criteria, CriteriaFallback, CriteriaKey, CriteriaValue}
import ru.yandex.vertis.billing.howmuch.model.core.RuleKey.ruleKeyCatsOrder
import zio.test.{DefaultRunnableSpec, ZSpec}
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

object RuleKeyOrderSpec extends DefaultRunnableSpec {

  val testKey: RuleKey =
    RuleKey(
      MatrixId(AUTORU, "call"),
      Context(
        List(
          Criteria(CriteriaKey("region_id"), CriteriaValue("10174")),
          Criteria(CriteriaKey("mark"), CriteriaValue("AUDI"))
        ),
        List(CriteriaFallback(CriteriaKey("model")))
      )
    )

  override def spec: ZSpec[TestEnvironment, Any] = suite("RuleKeyOrderSpec")(
    test("comparison takes matrix project id into account") {
      val anotherTestKey = testKey.copy(matrixId = testKey.matrixId.copy(project = REALTY))

      assert(ruleKeyCatsOrder.neqv(testKey, anotherTestKey))(isTrue)
    },
    test("comparison takes matrix id into account") {
      val anotherTestKey = testKey.copy(matrixId = testKey.matrixId.copy(matrixId = "another_call"))

      assert(ruleKeyCatsOrder.neqv(testKey, anotherTestKey))(isTrue)
    },
    test("comparison takes context into account") {
      val anotherTestKey = testKey.copy(context = testKey.context.copy(List(), List()))

      assert(ruleKeyCatsOrder.neqv(testKey, anotherTestKey))(isTrue)
    },
    test("comparison considers same rules equivalent") {
      val anotherTestKey = testKey.copy()

      assert(ruleKeyCatsOrder.eqv(testKey, anotherTestKey))(isTrue)
    }
  )
}
