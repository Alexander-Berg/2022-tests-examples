package vertis.zio.stream

import common.zio.logging.Logging
import vertis.zio.util.ZioFiberEnrichers.NamedFiberZio
import zio.stream.{ZSink, ZStream, ZTransducer}
import zio._
import zio.duration.durationInt

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
object StreamsTest {

  // streams from the queue via an 'impossible batching'
  // it must pull by time, otherwise it'll stall, the weight condition is never met
  def stream[A](q: Queue[A], empty: A, batching: (A, A) => A): RIO[ZEnv with Logging.Logging, Option[A]] = {
    val sink = ZSink.head[A]
    val stream = ZStream.fromQueueWithShutdown(q, 1).tap(in => Logging.info(s"Got $in"))
    val batched = stream.aggregateAsyncWithin(
      impossibleBatching[A, A](empty, batching),
      Schedule
        .spaced(100.millis)
        .tapOutput(i => Logging.debug(s"Time to batch $i"))
    )
    batched
      .run(sink)
      .withFiberName("streaming_fiber")
      .onExit(_ => Logging.info("Done streaming"))
  }

  def impossibleBatching[A, S](accInitial: S, combine: (S, A) => S): ZTransducer[Any, Nothing, A, S] =
    ZTransducer.foldWeighted[A, S](accInitial)((_, _) => 0, 100)(combine)
}
