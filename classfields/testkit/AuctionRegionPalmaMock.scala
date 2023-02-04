package auto.dealers.trade_in_notifier.storage.testkit

import common.palma.Palma
import common.palma.testkit.MockPalma
import ru.auto.trade_in_notifier.palma.proto.region_palma_model.{Open_for, TradeInRegion}
import zio.{Has, ZLayer}

object AuctionRegionPalmaMock {

  def layer: ZLayer[Any, Palma.PalmaError, Has[Palma.Service]] = ZLayer.fromEffect {
    for {
      palma <- MockPalma.make
      _ <- palma
        .create(
          TradeInRegion(
            regionIds = Seq(1, 2, 3, 4, 5, 6, 7),
            opened = Open_for.AUCTION
          )
        )
    } yield palma
  }
}
