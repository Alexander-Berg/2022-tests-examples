package ru.yandex.vertis.moisha.impl.autoru.example

import org.joda.time.DateTime
import ru.yandex.vertis.moisha.impl.autoru.model.{AutoRuContext, AutoRuOffer}

import scala.language.implicitConversions

/**
  * Pure implementation of supposed model
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
object PureModelImplementation {

  /** Single good with price */
  case class GoodImpl(good: String, cost: String, price: Long)

  /** Product - a named set of goods with total price */
  case class ProductImpl(product: String, goods: Set[GoodImpl], total: Long)

  /** AutoRu offer */
  case class OfferImpl(
      price: Long,
      creationTs: DateTime,
      transport: String,
      category: String,
      mark: Option[String] = None)

  /** Util function converting Moisha [[AutoRuOffer]] to [[OfferImpl]] */
  implicit def fromAutoRuOffer(autoRuOffer: AutoRuOffer): OfferImpl =
    OfferImpl(
      autoRuOffer.price,
      autoRuOffer.creationTs,
      autoRuOffer.transport.toString,
      autoRuOffer.category.toString,
      autoRuOffer.mark
    )

  /** AutoRu context */
  case class ContextImpl(clientRegionId: Long, clientCityId: Option[Long], offerPlacementDay: Option[Int])

  /** Util function converting Moisha [[AutoRuContext]] to [[ContextImpl]] */
  implicit def fromAutoRuContext(autoRuContext: AutoRuContext): ContextImpl =
    ContextImpl(autoRuContext.clientRegionId, autoRuContext.clientCityId, autoRuContext.offerPlacementDay)

  /** Point - estimated product in some time interval */
  case class PointImpl(policy: String, from: DateTime, to: DateTime, product: ProductImpl)

  /** Request - for simple marshalling */
  case class RequestImpl(offer: OfferImpl, context: ContextImpl, product: String, from: DateTime, to: DateTime)

  /** Response - original request and a set of points */
  case class ResponseImpl(request: RequestImpl, points: Set[PointImpl])
}
