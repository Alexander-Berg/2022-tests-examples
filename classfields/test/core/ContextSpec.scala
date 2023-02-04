package ru.yandex.vertis.billing.howmuch.model.core

import billing.common_model.Project
import billing.howmuch.model.testkit.mkRequestContext
import cats.data.NonEmptyList
import common.time.Interval
import common.zio.testkit.failsWith
import billing.howmuch.model.testkit._
import ru.yandex.vertis.billing.howmuch.model.core.Context.{
  FallbackForbidden,
  TimedSchemaNotFound,
  UnmatchedWithTimedSchema,
  ValueAfterFallback
}
import ru.yandex.vertis.billing.howmuch.model.core.criteria._
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

import java.time.Instant

object ContextSpec extends DefaultRunnableSpec {

  private val testMatrixId = MatrixId(Project.AUTORU, "call")
  private val unknownMatrixId = MatrixId(Project.AUTORU, "blabla")
  private val testInterval = Interval(Instant.ofEpochSecond(5), Some(Instant.ofEpochSecond(100)))
  private val insideTestInterval = Instant.ofEpochSecond(50)
  private val outsideTestInterval = Instant.ofEpochSecond(5000)

  override def spec: ZSpec[TestEnvironment, Any] = suite("ContextSpec")(
    // tests for parseContext: RuleContext -> Context
    testM("return Context if RuleContext contains the same criteria keys as a context schema") {
      val schema = mkSchema(testMatrixId, testInterval, List("mark", "model"), allowFallback = false)
      val ruleContext = mkRuleContext("mark" -> "AUDI", "model" -> "TT")
      val expected = mkContext("mark" -> "AUDI", "model" -> "TT")
      val result = Context.parseContext(schema, testMatrixId, ruleContext, insideTestInterval)
      assertM(result)(equalTo(expected))
    },
    testM("throw if RuleContext doesn't contain all required criteria keys") {
      val schema = mkSchema(testMatrixId, testInterval, List("mark", "model"), allowFallback = false)
      val ruleContext = mkRuleContext("mark" -> "AUDI")
      val result = Context.parseContext(schema, testMatrixId, ruleContext, insideTestInterval).run
      assertM(result)(failsWith[UnmatchedWithTimedSchema])
    },
    // эта логика может потенциально измениться в будущем
    testM("throw if RuleContext contains all required criteria keys but in the wrong order") {
      val schema = mkSchema(testMatrixId, testInterval, List("mark", "model"), allowFallback = false)
      val ruleContext = mkRuleContext("model" -> "TT", "mark" -> "AUDI")
      val result = Context.parseContext(schema, testMatrixId, ruleContext, insideTestInterval).run
      assertM(result)(failsWith[UnmatchedWithTimedSchema])
    },
    // эта логика может потенциально измениться в будущем
    testM("throw if RuleContext contains too many criteria keys") {
      val schema = mkSchema(testMatrixId, testInterval, List("mark", "model"), allowFallback = false)
      val ruleContext = mkRuleContext("mark" -> "TT", "model" -> "AUDI", "region_id" -> "10174")
      val result = Context.parseContext(schema, testMatrixId, ruleContext, insideTestInterval).run
      assertM(result)(failsWith[UnmatchedWithTimedSchema])
    },
    testM("throw if RuleContext contains fallback, but fallbacks aren't allowed by schema") {
      val schema = mkSchema(testMatrixId, testInterval, List("mark", "model"), allowFallback = false)
      val ruleContext = mkRuleContext("mark" -> "AUDI", "model" -> "*")
      val result = Context.parseContext(schema, testMatrixId, ruleContext, insideTestInterval).run
      assertM(result)(failsWith[FallbackForbidden])
    },
    testM("return Context if RuleContext contains fallback for just last criteria, and fallbacks are allowed") {
      val schema = mkSchema(testMatrixId, testInterval, List("mark", "model"), allowFallback = true)
      val ruleContext = mkRuleContext("mark" -> "AUDI", "model" -> "*")
      val expected = mkContext("mark" -> "AUDI", "model" -> "*")
      val result = Context.parseContext(schema, testMatrixId, ruleContext, insideTestInterval)
      assertM(result)(equalTo(expected))
    },
    testM("return Context if RuleContext contains fallback for every criteria, and fallbacks are allowed") {
      val schema = mkSchema(testMatrixId, testInterval, List("mark", "model"), allowFallback = true)
      val ruleContext = mkRuleContext("mark" -> "*", "model" -> "*")
      val expected = mkContext("mark" -> "*", "model" -> "*")
      val result = Context.parseContext(schema, testMatrixId, ruleContext, insideTestInterval)
      assertM(result)(equalTo(expected))
    },
    testM("throw if a value is given after a fallback") {
      val schema = mkSchema(testMatrixId, testInterval, List("mark", "model"), allowFallback = true)
      val ruleContext = mkRuleContext("mark" -> "*", "model" -> "TT")
      val result = Context.parseContext(schema, testMatrixId, ruleContext, insideTestInterval).run
      assertM(result)(failsWith[ValueAfterFallback.type])
    },
    testM("throw if schema isn't active at the rule creation request moment") {
      val schema = mkSchema(testMatrixId, testInterval, List("mark", "model"), allowFallback = false)
      val ruleContext = mkRuleContext("mark" -> "AUDI", "model" -> "TT")
      val result = Context.parseContext(schema, testMatrixId, ruleContext, outsideTestInterval).run
      assertM(result)(failsWith[TimedSchemaNotFound])
    },
    testM("throw if matrixId is unknown") {
      val schema = mkSchema(testMatrixId, testInterval, List("mark", "model"), allowFallback = false)
      val ruleContext = mkRuleContext("mark" -> "AUDI", "model" -> "TT")
      val result = Context.parseContext(schema, unknownMatrixId, ruleContext, insideTestInterval).run
      assertM(result)(failsWith[TimedSchemaNotFound])
    },
    // tests for parseContexts: RequestContext -> List[Context]
    testM("return Context if RequestContext contains all schema criteria keys and fallbacks aren't allowed") {
      val schema = mkSchema(testMatrixId, testInterval, List("mark", "model"), allowFallback = false)
      val requestContext = mkRequestContext("mark" -> "AUDI", "model" -> "TT")
      val expected = mkContext("mark" -> "AUDI", "model" -> "TT")
      val result = Context.parseContexts(schema, testMatrixId, requestContext, insideTestInterval)
      assertM(result)(equalTo(NonEmptyList.one(expected)))
    },
    testM("throw if RequestContext doesn't contain all required criteria keys") {
      val schema = mkSchema(testMatrixId, testInterval, List("mark", "model"), allowFallback = false)
      val requestContext = mkRequestContext("mark" -> "AUDI")
      val result = Context.parseContexts(schema, testMatrixId, requestContext, insideTestInterval).run
      assertM(result)(failsWith[UnmatchedWithTimedSchema])
    },
    // эта логика может потенциально измениться в будущем
    testM("throw if RequestContext contains all required criteria keys but in the wrong order") {
      val schema = mkSchema(testMatrixId, testInterval, List("mark", "model"), allowFallback = false)
      val requestContext = mkRequestContext("model" -> "TT", "mark" -> "AUDI")
      val result = Context.parseContexts(schema, testMatrixId, requestContext, insideTestInterval).run
      assertM(result)(failsWith[UnmatchedWithTimedSchema])
    },
    testM("return all possible Contexts from more concrete to more wide if fallbacks are allowed") {
      val schema = mkSchema(testMatrixId, testInterval, List("mark", "model"), allowFallback = true)
      val requestContext = mkRequestContext("mark" -> "AUDI", "model" -> "TT")
      val concrete = mkContext("mark" -> "AUDI", "model" -> "TT")
      val wider = mkContext("mark" -> "AUDI", "model" -> "*")
      val widest = mkContext("mark" -> "*", "model" -> "*")
      val expected = NonEmptyList.of(concrete, wider, widest)
      val result = Context.parseContexts(schema, testMatrixId, requestContext, insideTestInterval)
      assertM(result)(equalTo(expected))
    },
    testM("throw if schema isn't active at the get price request moment") {
      val schema = mkSchema(testMatrixId, testInterval, List("mark", "model"), allowFallback = false)
      val requestContext = mkRequestContext("mark" -> "AUDI", "model" -> "TT")
      val result = Context.parseContexts(schema, testMatrixId, requestContext, outsideTestInterval).run
      assertM(result)(failsWith[TimedSchemaNotFound])
    },
    testM("throw if matrixId is unknown") {
      val schema = mkSchema(testMatrixId, testInterval, List("mark", "model"), allowFallback = false)
      val requestContext = mkRequestContext("mark" -> "AUDI", "model" -> "TT")
      val result = Context.parseContexts(schema, unknownMatrixId, requestContext, insideTestInterval).run
      assertM(result)(failsWith[TimedSchemaNotFound])
    }
  )

  private def mkSchema(matrixId: MatrixId, interval: Interval, criteriaKeys: List[String], allowFallback: Boolean) =
    Schema(
      List(
        TimedSchema(
          matrixId,
          interval,
          ContextSchema(criteriaKeys.map(CriteriaKey)),
          allowFallback
        )
      ),
      historyUploadAllowed = Set()
    )

  private def mkContext(criteriaList: (String, String)*) = {
    val (values, fallbacks) = criteriaList.toList.span { case (_, value) => value != "*" }
    Context(
      values.map { case (key, value) => Criteria(CriteriaKey(key), CriteriaValue(value)) },
      fallbacks.map { case (key, fallback) =>
        require(fallback == "*", s"Unexpected non-fallback value after fallback: $fallback")
        CriteriaFallback(CriteriaKey(key))
      }
    )
  }
}
