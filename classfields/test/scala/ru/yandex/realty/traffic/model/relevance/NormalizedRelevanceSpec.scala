package ru.yandex.realty.traffic.model.relevance

import org.junit.runner.RunWith
import ru.yandex.realty.traffic.model.relevance.NormalizedRelevance.NormalizedRelevance
import zio.test._
import zio.test.junit._
import eu.timepit.refined.auto._

@RunWith(classOf[ZTestJUnitRunner])
class NormalizedRelevanceSpec extends JUnitRunnableSpec {

  final private case class FitIntTestCase(
    value: Int,
    expected: NormalizedRelevance,
    min: Int = Int.MinValue,
    max: Int = Int.MaxValue
  )

  private val FitIntSpecs =
    Seq(
      FitIntTestCase(0, 0.5, min = Int.MinValue + 1), // math.abs(int.min) === int.max + 1
      FitIntTestCase(Int.MaxValue, 1.0),
      FitIntTestCase(Int.MinValue, 0.0),
      FitIntTestCase(0, 0.0, 0, 10),
      FitIntTestCase(5, 0.5, 0, 10),
      FitIntTestCase(10, 1.0, 0, 10)
    )

  private def fitIntSpec(testCase: FitIntTestCase) =
    test(s"should correctly fit int ${testCase.value} with bounds [${testCase.min}, ${testCase.max}]") {
      assertTrue(NormalizedRelevance.fitInt(testCase.value, testCase.min, testCase.max) == testCase.expected)
    }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    val tests =
      FitIntSpecs.map(fitIntSpec)

    suite("NormalizedRelevance")(
      tests: _*
    )
  }
}
