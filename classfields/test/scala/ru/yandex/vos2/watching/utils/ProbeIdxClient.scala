package ru.yandex.vos2.watching.utils

import ru.yandex.realty.proto.offer.OfferHiddenReason
import ru.yandex.realty.proto.offer.vos.OfferIdx.IdxRequest
import ru.yandex.realty.proto.unified.vos.offer.Publishing.ShowStatus
import ru.yandex.vos2.OfferID
import ru.yandex.vos2.services.idx.sync.{IdxClient, IdxResponse, Ok}

import scala.concurrent.Future

/**
  * @author Ilya Gerasimov (747mmhg@yandex-team.ru)
  */
class ProbeIdxClient extends IdxClient[OfferID] {

  var sentRequest: IdxRequest = _
  var removed = false

  def clean(): Unit = {
    this.sentRequest = null
    this.removed = false
  }

  override def submit(offerId: OfferID, bytes: Array[Byte]): Future[IdxResponse] = {
    sentRequest = IdxRequest.parseFrom(bytes)
    Future.successful(Ok)
  }

  override def remove(offerId: OfferID): Future[IdxResponse] = {
    removed = true
    Future.successful(Ok)
  }
}

object ProbeIdxClient {

  def hasHiddenReason(req: IdxRequest): Boolean = {
    req.getCreate.getOffer.getContent.getHiddenReason != OfferHiddenReason.OFFER_HIDDEN_REASON_UNKNOWN
  }

  def hasPublishedShowStatus(req: IdxRequest): Boolean = {
    req.getCreate.getOffer.getContent.getShowStatus == ShowStatus.PUBLISHED
  }
}
