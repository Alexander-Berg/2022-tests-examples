package ru.yandex.vertis.general.wizard.api.services

import ru.yandex.vertis.general.wizard.core.service.OfferStatsService
import ru.yandex.vertis.general.wizard.model.Listing
import zio.Task

case class TestOffersStatService(private val inner: Map[Listing, Int]) extends OfferStatsService.Service {

  override def getCount(listing: Listing): Task[Int] =
    Task.succeed(inner.getOrElse(listing, 0))
}

object TestOffersStatService {

  def simple(pair: (Listing, Int)): OfferStatsService.Service =
    TestOffersStatService(Map(pair))
}
