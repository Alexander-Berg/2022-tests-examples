package ru.yandex.vertis.tracing

import brave.propagation.TraceContext
import brave.{Span, Tracer}
import org.mockito.Matchers._
import org.mockito.Mockito.{doNothing, verify, _}
import org.scalatest.Matchers._
import org.scalatest.{OptionValues, WordSpec}
import org.scalatest.mockito.MockitoSugar
import zipkin.Endpoint

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 07.03.17
  */
class TracedSpec extends WordSpec with MockitoSugar with OptionValues {
  private val calling = afterWord("calling")

  "Traced.empty" should {
    "noop on all methods" in {
      val trace = Traced.empty

      trace.named("123")
      trace.annotate("abc")
      trace.annotate("a", "bc")

      trace.context shouldBe None
      trace.requestId shouldBe ""

      trace.annotateClientSent()
      trace.annotateClientReceive()

      trace.annotateServerSent()
      trace.annotateServerReceive()

      trace.annotateError()

      trace.remoteEndpoint(null)

      trace.subTraced("abc") shouldBe trace
      trace.subTraced("abc", null) shouldBe trace

      trace.finish()
    }
  }

  private class Fixture {
    val tracer: Tracer = null // impossible to mock brave.Tracer
    val span: Span = mock[Span]
    val trace: Traced = Traced.wrap(tracer, span)
  }

  "Traced.wrap" should {
    "proxy method call to wrapped span and tracer" when calling {
      "finish()" in new Fixture {
        doNothing().when(span).finish()
        trace.finish()
        verify(span).finish()
      }
      "context" in new Fixture {
        val context: TraceContext = mock[TraceContext]
        when(span.context()).thenReturn(context)

        trace.context.value shouldBe context
        verify(span).context()
      }
      "requestId" in new Fixture {
        val context: TraceContext = mock[TraceContext]
        when(span.context()).thenReturn(context)
        when(context.traceIdString()).thenReturn("test-id")

        trace.requestId shouldBe "test-id"
        verify(span).context()
        verify(context).traceIdString()
      }
      "named(name)" in new Fixture {
        when(span.name(any[String])).thenReturn(span)
        trace.named("test")
        verify(span).name("test")
      }
      "annotate(tag)" in new Fixture {
        when(span.annotate(any[String])).thenReturn(span)
        trace.annotate("tag")
        verify(span).annotate("tag")
      }
      "annotate(tag, value)" in new Fixture {
        when(span.tag(any[String], any[String])).thenReturn(span)
        trace.annotate("tag2", "value")
        verify(span).tag("tag2", "value")
      }

      // think about testing subTraced

      "remoteEndpoint" in new Fixture {
        val endpoint: Endpoint = Endpoint.create("service", 0)
        when(span.remoteEndpoint(any[Endpoint])).thenReturn(span)
        trace.remoteEndpoint(endpoint)
        verify(span).remoteEndpoint(endpoint)
      }
      "toString" in new Fixture {
        val context: TraceContext = mock[TraceContext]
        when(span.context()).thenReturn(context)
        when(context.traceIdString()).thenReturn("test-id")

        trace.toString shouldBe "[test-id]"
        verify(span).context()
        verify(context).traceIdString()
      }
    }
  }

  "Traced.Wrapper" should {
    "be available implicitly" in {
      case class W(trace: Traced) extends Traced.Wrapper
      def method()(implicit trace: Traced) = trace

      val trace = mock[Traced]
      implicit val wrapper: W = W(trace)

      method() shouldBe trace
    }
  }
}
