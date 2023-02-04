package ru.yandex.realty.generators

import org.scalacheck.Gen
import ru.yandex.common.util.currency.Currency
import ru.yandex.realty.model.locale.RealtyLocale
import ru.yandex.realty.render.RenderParams

object BnbSearcherGenerators {

  val renderParamsGen: Gen[RenderParams] = for {
    locale <- Gen.oneOf(RealtyLocale.values())
  } yield {
    new RenderParams(
      currency = Some(Currency.RUR),
      selectedRegion = None,
      locale = locale,
      requestedMetroGeoIds = Set.empty,
      requestedMetroTransport = None,
      requestedTimeToMetro = None,
      companyId = None,
      showMortgage = None,
      from = None,
      bucket = None,
      expFlags = Set.empty,
      showOnLanding = None,
      commutePolygon = None,
      streetAddress = Iterable.empty,
      bigbInfo = None
    )
  }

}
