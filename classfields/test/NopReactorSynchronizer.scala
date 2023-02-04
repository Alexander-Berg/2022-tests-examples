package vertis.yt.zio.reactor

import vertis.yt.zio.reactor.ReactorSynchronizer.PublishResult
import zio.{RIO, UIO, ZIO}

/**
  */
object NopReactorSynchronizer {

  def create[T]: ReactorSynchronizer[Any, T] = {
    new ReactorSynchronizer[Any, T] {
      override def publish(instances: Seq[T], filter: T => Boolean): RIO[Any, PublishResult[T]] = UIO(
        PublishResult.empty
      )
      override def publishLater(instances: Seq[T]): RIO[Any, Unit] = ZIO.unit
    }
  }

}
