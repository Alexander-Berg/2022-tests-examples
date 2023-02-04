package ru.yandex.vertis.punisher.dao

import org.scalatest.time.{Millis, Seconds, Span}
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.punisher.model.Offer

trait OffersDaoSpec[T <: Offer] extends BaseSpec {

  protected def dao: OffersDao[F, T]

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(900, Seconds), interval = Span(1000, Millis))
}
