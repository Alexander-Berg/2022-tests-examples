package infra.profiler_collector.converter.test

import infra.profiler_collector.converter.JfrParser
import infra.profiler_collector.converter.test.TestData._
import zio.test._

object JfrParserSpec extends DefaultRunnableSpec {

  def spec =
    suite("JfrParser")(
      testM("parse sample report") {
        for {
          samples <- JfrParser.parse(testProfileResults, profilingMode, "test-service", Map("a" -> "b")).runCollect
          identical = samples.map(_.toString) == TestData.expectedSamples
          _ = if (!identical) {
            println("!!! expected_samples.txt !!!")
            println(samples.mkString("\n"))
          }
        } yield {
          assertTrue(identical)
        }
      }
    )
}
