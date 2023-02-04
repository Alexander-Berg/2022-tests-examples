package util

import java.io.InputStream

import io.gatling.commons.util.Arrays
import io.gatling.core.feeder.{CloseableFeeder, Feeder, Record}

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 2019-07-24
  */
abstract class BatchedSeparatedValuesFeeder[T](is: => InputStream, streamer: InputStream => Feeder[T])
  extends CloseableFeeder[T] {

  private var currentIs: InputStream = is
  protected var feeder: Feeder[T] = streamer(currentIs)

  protected def resetStream(): Unit = {
    currentIs.close()
    currentIs = is
    feeder = streamer(currentIs)
  }

  override def close(): Unit = currentIs.close()
}

class RandomBatchedSeparatedValuesFeeder[T](is: => InputStream, streamer: InputStream => Feeder[T], bufferSize: Int)
  extends BatchedSeparatedValuesFeeder[T](is, streamer) {

  private val buffer = new Array[Record[T]](bufferSize)
  private var index = 0
  refill()

  private def refill(): Unit = {
    var fill = 0
    while (fill < bufferSize) {
      if (!feeder.hasNext) {
        resetStream()
      }
      buffer(fill) = feeder.next()
      fill += 1
    }
    Arrays.shuffle(buffer)
  }

  override def hasNext: Boolean = true

  override def next(): Record[T] =
    if (index < bufferSize) {
      val record = buffer(index)
      index += 1
      record
    } else {
      refill()
      index = 1
      buffer(0)
    }
}
