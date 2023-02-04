package vertis.palma.utils

import java.{lang, util}

import scala.jdk.CollectionConverters._
import io.opentracing.SpanContext

/** Non-final io.opentracing.noop.NoopSpanContext
  */
class NoopSpanContext extends SpanContext {
  override def toTraceId: String = ""

  override def toSpanId: String = ""

  override def baggageItems(): lang.Iterable[util.Map.Entry[String, String]] = Iterable.empty.asJava

  override def toString: String = "NoopSpanContext"
}

object NoopSpanContext extends NoopSpanContext
