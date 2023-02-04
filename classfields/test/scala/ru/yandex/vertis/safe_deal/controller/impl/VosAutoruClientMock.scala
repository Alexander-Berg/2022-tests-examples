package ru.yandex.vertis.safe_deal.controller.impl

import ru.auto.api.api_offer_model.{Category, Offer, OfferStatus}
import ru.yandex.vertis.zio_baker.model.OfferId
import ru.yandex.vertis.zio_baker.zio.client.vos.VosAutoruClient
import ru.yandex.vertis.zio_baker.zio.httpclient.RequestSupport
import ru.yandex.vertis.zio_baker.zio.httpclient.config.HttpClientConfig
import zio._

class VosAutoruClientMock() extends VosAutoruClient.Service with RequestSupport {

  override protected def httpClientConfig: HttpClientConfig =
    throw new UnsupportedOperationException("Calling a stub method")

  override def offer(category: Category, offerId: OfferId, includeRemoved: Boolean): Task[Offer] =
    Task.succeed(
      Offer.defaultInstance
        .withUserRef("user:1337")
        .withTags(Seq("allowed_for_safe_deal"))
        .withStatus(OfferStatus.ACTIVE)
    )
}
