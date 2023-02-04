package ru.yandex.vertis.general.wizard.scheduler.services

import ru.yandex.vertis.general.wizard.model.{OfferFilter, StockOffer}
import ru.yandex.vertis.general.wizard.scheduler.feed.PinType
import ru.yandex.vertis.general.wizard.scheduler.services.FeedUrlBuilder.FeedUrlBuilder

import zio.test.mock
import zio.test.mock.Mock
import zio.{Has, URLayer, ZLayer}

/** @author a-pashinin
  */
object FeedUrlBuilderMock extends Mock[FeedUrlBuilder] {

  object Build extends Effect[(StockOffer, Seq[OfferFilter], PinType.Value), Throwable, Option[String]]

  override val compose: URLayer[Has[mock.Proxy], FeedUrlBuilder] =
    ZLayer.fromService { proxy => (offer: StockOffer, urlFilters: Seq[OfferFilter], pinType: PinType.Value) =>
      proxy(Build, offer, urlFilters, pinType)
    }
}
