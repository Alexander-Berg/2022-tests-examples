package vs.registry.producer

import bootstrap.tracing.$
import vs.core.distribution.ShardDistributionNotifier
import vs.core.events.Publisher
import zio.mock.*
import zio.*

object DistributionMock extends Mock[ShardDistributionNotifier] {

  override val compose: URLayer[Proxy, ShardDistributionNotifier] = ZLayer
    .fromZIO {
      for {
        proxy <- ZIO.service[Proxy]
      } yield new ShardDistributionNotifier {
        override def subscribe(
          subscription: Seq[Int] => RIO[$, Unit],
        ): RIO[$, Publisher.Subscription] = proxy(Subscribe, subscription)
      }
    }

  object Subscribe
      extends Effect[
        Seq[Int] => RIO[$, Unit],
        Throwable,
        Publisher.Subscription,
      ]

}
