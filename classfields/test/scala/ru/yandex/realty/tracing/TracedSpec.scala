package ru.yandex.realty.tracing

import io.jaegertracing.Configuration.{CodecConfiguration, Propagation}
import io.jaegertracing.internal.samplers.ConstSampler
import io.jaegertracing.Configuration
import io.jaegertracing.internal.JaegerSpan
import io.jaegertracing.spi.Reporter
import io.opentracing.Tracer
import ru.yandex.realty.SpecBase

import scala.collection.JavaConverters._

class TracedSpec extends SpecBase {

  val tracer: Tracer = {
    Configuration
      .fromEnv("realty-test")
      .withCodec(
        // Порядок важен. Первыми парсим B3, потому что (только) их генерят старые сервисы, а балансер энвоя
        // этого не понимает и генерит новый егеревый айдишник
        new CodecConfiguration().withPropagation(Propagation.B3).withPropagation(Propagation.JAEGER)
      )
      .getTracerBuilder
      .withSampler(new ConstSampler(true))
      .withExpandExceptionLogs()
      .withReporter(new Reporter {
        override def report(span: JaegerSpan): Unit = {
          val res = List(
            s"parent span id: ${span.context().getParentId}",
            s"span id: ${span.context().getSpanId}",
            s"tags: ${span.getTags.asScala}",
            s"branch: ${span.getBaggageItem(Traced.branchNameKey)}"
          )
          println(res.mkString("", "\n", "\n"))
        }

        override def close(): Unit = ()
      })
      .withTraceId128Bit
      .withTag("hostname", "realty-test.yandex-team.ru")
      .build()
  }

  val branch = "feature/REALTYBACK-0000-branch-name"

  "SpanTraced" when {
    "subTraced" should {
      "set branch" in {
        val span = tracer.buildSpan("test-operation").start()
        span.setBaggageItem(Traced.branchNameKey, branch)
        val traced = Traced.wrap(tracer, span)
        traced.branchName.get should be(branch)
        traced.finish()
      }
      "keep branch" in {
        val span = tracer
          .buildSpan("test-operation")
          .start()

        span.setBaggageItem(Traced.branchNameKey, branch)

        val traced = Traced.wrap(tracer, span)
        traced.branchName.get should be(branch)
        traced.withSubTrace("sub-test-operation") { t =>
          t.branchName.get should be(branch)
        }
        traced.finish()
      }
      "branch different for spans" in {
        val span1 = tracer.buildSpan("test-operation").start()
        val span2 = tracer.buildSpan("test-operation").start()
        val span3 = tracer.buildSpan("test-operation").start()
        val traced1 = Traced.wrap(tracer, span1, Some(branch))
        val traced2 = Traced.wrap(tracer, span2)
        val traced3 = Traced.wrap(tracer, span3, Some("branch3"))

        traced1.branchName.get should be(branch)
        traced2.branchName should be(None)
        traced3.branchName.get should be("branch3")

        traced1.finish()
        traced2.finish()
        traced3.finish()
      }
    }
  }
}
