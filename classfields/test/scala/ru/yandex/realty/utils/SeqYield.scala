package ru.yandex.realty.utils

import ru.yandex.inside.yt.kosher.operations.Yield
import zio._

import scala.collection.mutable.ArrayBuffer

final class SeqYield[T] extends Seq[(Int, T)] with Yield[T] {

  private val buffer = ArrayBuffer.empty[(Int, T)]

  override def `yield`(index: Int, value: T): Unit = buffer.append((index, value))

  override def close(): Unit = ()

  override def length: Int = buffer.size

  override def apply(idx: Int): (Int, T) = buffer(idx)

  override def iterator: Iterator[(Int, T)] = buffer.toIterator
}

object SeqYield {
  def empty[T: Tag]: ULayer[Has[SeqYield[T]]] = ZLayer.succeed(new SeqYield[T])
}
