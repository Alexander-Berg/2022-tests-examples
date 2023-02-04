package ru.yandex.vertis.punisher.dao

import org.scalatest.time.{Millis, Seconds, Span}
import ru.yandex.vertis.punisher.BaseSpec

trait OfferSimilarityDaoSpec extends BaseSpec {

  protected def dao: OfferSimilarityDao[F]

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(3600, Seconds), interval = Span(1000, Millis))
}
