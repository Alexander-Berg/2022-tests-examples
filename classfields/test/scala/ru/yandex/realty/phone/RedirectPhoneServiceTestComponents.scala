package ru.yandex.realty.phone

import ru.yandex.realty.application.ng.ExecutionContextProvider
import ru.yandex.realty.phone.RedirectPhoneStrategy.{oneStepRedirectPhoneStrategy, prioritizeLocalPhoneStrategy}
import ru.yandex.realty.telepony.TeleponyClientMockComponents
import ru.yandex.realty.tracing.Traced

trait RedirectPhoneServiceTestComponents extends TeleponyClientMockComponents {
  this: ExecutionContextProvider =>

  implicit val traced: Traced = Traced.empty

  lazy val redirectPhoneProvider = new TeleponyRedirectPhoneProvider(teleponyClient)

  lazy val redirectPhoneStrategyProvider =
    new RedirectPhoneStrategyProvider(
      Seq(
        oneStepRedirectPhoneStrategy(redirectPhoneProvider),
        prioritizeLocalPhoneStrategy(redirectPhoneProvider)
      )
    )
  lazy val redirectPhoneService = new RedirectPhoneService(redirectPhoneStrategyProvider, teleponyClient)

}
