package vertis.palma.utils

import java.util

import io.opentracing.{Span, SpanContext}
import io.opentracing.tag.Tag

/** Non-final io.opentracing.noop.NoopSpan.INSTANCE
  */
class NoopSpan extends Span {
  override def context(): SpanContext = NoopSpanContext

  override def setTag(key: String, value: String): Span = this

  override def setTag(key: String, value: Boolean): Span = this

  override def setTag(key: String, value: Number): Span = this

  override def setTag[T](tag: Tag[T], value: T): Span = this

  override def log(fields: util.Map[String, _]): Span = this

  override def log(timestampMicroseconds: Long, fields: util.Map[String, _]): Span = this

  override def log(event: String): Span = this

  override def log(timestampMicroseconds: Long, event: String): Span = this

  override def setBaggageItem(key: String, value: String): Span = this

  override def getBaggageItem(key: String): String = ""

  override def setOperationName(operationName: String): Span = this

  override def finish(): Unit = {}

  override def finish(finishMicros: Long): Unit = {}
}
