package vsmoney.auction_auto_strategy.scheduler.test

import vsmoney.auction_auto_strategy.model.auction.{CriteriaContext, Criterion, CriterionKey, CriterionValue}
import vsmoney.auction_auto_strategy.scheduler.service.impl.AutoruCallContext
import vsmoney.auction_auto_strategy.scheduler.service.impl.AutoruCallContext.NotAutoruContext
import zio.test.environment.TestEnvironment
import zio.test._
import zio.test.Assertion._

object AutoruCallContextSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("AutoruCallContext fromCriteraContext")(
      testM("should create call context from auction context") {
        val result = AutoruCallContext.fromCriteraContext(testContext)
        val expected = AutoruCallContext(testRegion, testMark, testModel)
        assertM(result)(equalTo(expected))
      },
      testM("should fail with NotAutoruContext if context dont have proper criterion") {
        val result = AutoruCallContext.fromCriteraContext(testContext.copy(criteria = testContext.criteria.tail))
        assertM(result.run)(fails(isSubtype[NotAutoruContext](anything)))
      }
    )
  }

  private val testRegion = 1
  private val testMark = "BMW"
  private val testModel = "X3"

  private val testContext = CriteriaContext(
    List(
      Criterion(CriterionKey("region_id"), CriterionValue(testRegion.toString)),
      Criterion(CriterionKey("mark"), CriterionValue(testMark)),
      Criterion(CriterionKey("model"), CriterionValue(testModel))
    )
  )

}
