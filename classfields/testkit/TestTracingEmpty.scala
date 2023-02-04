package ru.yandex.vertis.general.search.testkit

import io.opentracing.{Span, Tracer}
import common.zio.ops.tracing.Tracing
import zio.{Has, UIO, ULayer, ZIO, ZLayer}

object TestTracingEmpty {

  val pseudoEmpty: ULayer[Has[Tracing.Service]] = ZLayer.succeed(new Tracing.Service {
    override def tracer: Tracer = ???

    override def currentSpan: UIO[Option[Span]] = ZIO.none

    override def setSpan(span: Option[Span]): UIO[Unit] = ???

    override def newSpan(operationName: String, parent: Option[Span]): UIO[Span] = ???

    override def finish(span: Span): UIO[Unit] = ???

    override def log(msg: String): ZIO[Any, Nothing, Unit] = ZIO.unit

    override def log(fields: Map[String, _]): ZIO[Any, Nothing, Unit] = ???
  })

}
