package ru.yandex.vertis.zio_baker.zio.kafka

import zio.clock.Clock
import zio.duration.Duration
import zio.{Ref, Task, UIO, URIO, ZIO}

trait Testable[R, K, V] extends KafkaConsumer.Service[K, V] {

  def complete: UIO[Option[R]]

  def runAwait(time: Duration): URIO[Clock, Option[R]] = for {
    f <- run().fork
    res <- await(time)
    _ <- f.interrupt
  } yield res

  def await(time: Duration): URIO[Clock, Option[R]] =
    complete.repeatWhile(_.isEmpty).race(ZIO.none.delay(time))
}

object Testable {

  trait Default[R, K, V] extends Testable[Default.Context[R], K, V] {
    def context: Ref[Default.Context[R]]
  }

  object Default {
    case class Context[R](items: R)

    trait Single[K, V] extends SingleKafkaConsumer[K, V] with Default[Seq[V], K, V] {

      abstract override def process(items: Seq[V]): Task[Unit] = for {
        res <- super.process(items)
        _ <- context.update(ctx => ctx.copy(items = ctx.items ++ items))
      } yield res
    }

    trait Grouped[K, V, GroupKey]
      extends GroupedKafkaConsumer[K, V, GroupKey]
      with Default[Map[GroupKey, Seq[V]], K, V] {

      abstract override def process(items: Seq[V]): Task[Unit] = for {
        res <- super.process(items)
        groups = items.groupBy(groupKey)
        _ <- context.update { ctx =>
          val m = ctx.items.toSeq ++ groups.toSeq
          val g = m.groupBy(_._1)
          val map = g.view.mapValues(_.flatMap(_._2)).toMap
          ctx.copy(items = map)
        }
      } yield res
    }
  }
}
