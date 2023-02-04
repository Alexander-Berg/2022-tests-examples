package common.zio.ops.tracing.testkit

import common.zio.ops.tracing.Tracing
import io.opentracing.{Span, Tracer}
import io.opentracing.noop.NoopTracerFactory
import zio.{FiberRef, Has, UIO, ULayer, ZIO, ZLayer}
import io.opentracing.mock.MockTracer
import zio.clock.Clock

object TestTracing {

  val noOp: ULayer[Has[Tracing.Service]] = ZLayer.succeed(new Tracing.Service {
    override val tracer: Tracer = NoopTracerFactory.create()

    override def currentSpan: UIO[Option[Span]] = ZIO.none

    override def setSpan(span: Option[Span]): UIO[Unit] = ZIO.unit

    override def newSpan(operationName: String, parent: Option[Span]): UIO[Span] = ZIO.succeed(tracer.activeSpan())

    override def finish(span: Span): UIO[Unit] = ZIO.unit

    override def log(msg: String): ZIO[Any, Nothing, Unit] = ZIO.unit

    override def log(fields: Map[String, _]): ZIO[Any, Nothing, Unit] = ZIO.unit

    override def newSpan(uberId: String, operationName: String): UIO[Span] = ZIO.succeed(tracer.activeSpan())
  })

  val mock: ZLayer[Clock, Nothing, Has[MockTracer] with Has[Tracing.Service]] = {
    ZLayer.succeed(new MockTracer) ++ Clock.any >>>
      (for {
        ref <- FiberRef.make(Option.empty[Span])
        tracer <- ZIO.service[MockTracer]
        clock <- ZIO.service[Clock.Service]
      } yield new Tracing.Live(tracer, ref, clock): Tracing.Service).toLayer.passthrough
  }
}
