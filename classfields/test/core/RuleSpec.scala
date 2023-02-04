package ru.yandex.vertis.billing.howmuch.model.core

import billing.common_model.Project.AUTORU
import common.time.Interval
import ru.yandex.vertis.billing.common.money.Kopecks
import ru.yandex.vertis.billing.howmuch.model.core.Source.{BillingSync, UserRequest}
import ru.yandex.vertis.billing.howmuch.model.core.criteria._
import zio.ZIO.{effectTotal, foreachPar}
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

import java.time.Instant

object RuleSpec extends DefaultRunnableSpec {

  private val testKey =
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

  private val testFrom = Instant.parse("2021-03-26T11:40:00Z")

  private val testInterval = Interval(testFrom, to = None)

  private val testRule = Rule(testKey, testInterval, Kopecks(20000), Source.StartrekTicket("VSMONEY-2750"))

  override def spec: ZSpec[TestEnvironment, Any] = suite("RuleSpec")(
    test("rule id is a stable sha256") {
      assert(testRule.id)(equalTo("a93272b7cef294270e8930d6472c1f9031960ac632d073a0c2574754e7eefc1a"))
    },
    testM("rule id is calculated in a thread-safe way") {
      val forceNewRuleIdCalculation = effectTotal(testRule.copy().id)
      val bigList = List.fill(1000)(())
      val calculateRuleIdInMultipleThreads = foreachPar(bigList)(_ => forceNewRuleIdCalculation)
      assertM(calculateRuleIdInMultipleThreads)(
        hasSameElementsDistinct(List("a93272b7cef294270e8930d6472c1f9031960ac632d073a0c2574754e7eefc1a"))
      )
    },
    test("rule id doesn't change after setting to") {
      val copied = testRule.copy(interval = testRule.interval.copy(to = Some(Instant.parse("2021-03-27T02:15:00Z"))))
      assert(copied.id)(equalTo("a93272b7cef294270e8930d6472c1f9031960ac632d073a0c2574754e7eefc1a"))
    },
    test("rule id is a stable sha256 for given user request too") {
      val copied = testRule.copy(source = UserRequest("e2c229fcd9aadfc938b6eb33c1c7418b"))
      assert(copied.id)(equalTo("867bbfe8f5868144b9cb09d576a280de16d9c9a3f4089ed120c93898b69a0022"))
    },
    test("rule id is a stable sha256 for given billing sync request too") {
      val copied = testRule.copy(source = BillingSync("e2c229fcd9aadfc938b6eb33c1c7418b"))
      assert(copied.id)(equalTo("867bbfe8f5868144b9cb09d576a280de16d9c9a3f4089ed120c93898b69a0022"))
    }
  )
}
