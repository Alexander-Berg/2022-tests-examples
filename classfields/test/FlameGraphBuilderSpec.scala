package infra.profiler_collector.converter.test

import infra.profiler_collector.converter.test.TestData._
import infra.profiler_collector.converter.{FlameGraphBuilder, JfrParser}
import zio.test._

import java.nio.file.{Files, Paths}

object FlameGraphBuilderSpec extends DefaultRunnableSpec {

  private def trimLines(str: String) = str.linesIterator.map(_.trim).mkString("\n")

  def testFlameGraph(mode: String, expectedFileName: String, lines: Boolean = false, reverse: Boolean = false) = {
    for {
      samples <- JfrParser.parse(testProfileResults, profilingMode, "test-service", Map()).runCollect
      traces = samples.filter(_.mode == mode).map(_.asTraceSample)
      html = FlameGraphBuilder.buildFlameGraph(traces, title = "Flame Graph", lines, reverse).value
      expectedHtml = Files.readString(
        Paths.get(
          runfiles.rlocation(s"verticals_backend/infra/profiler_collector/converter/test/$expectedFileName")
        )
      )
      htmlIdentical = trimLines(html) == trimLines(expectedHtml)
      _ = if (!htmlIdentical) {
        println(s"!!! $expectedFileName !!!")
        println(html)
      }
    } yield {
      assertTrue(htmlIdentical)
    }
  }

  def spec =
    suite("FameGraphBuilder")(
      testM("cpu flame graph") {
        testFlameGraph("itimer", "expected_flame.html")
      },
      testM("cpu flame graph with line numbers") {
        testFlameGraph("itimer", "expected_flame_lines.html", lines = true)
      },
      testM("cpu flame graph reversed") {
        testFlameGraph("itimer", "expected_flame_reverse.html", reverse = true)
      },
      testM("alloc flame graph") {
        testFlameGraph("alloc", "expected_flame_alloc.html")
      },
      testM("lock flame graph") {
        testFlameGraph("lock", "expected_flame_locks.html")
      }
    )

}
